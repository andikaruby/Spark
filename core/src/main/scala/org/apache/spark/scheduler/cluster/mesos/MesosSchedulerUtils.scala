/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.scheduler.cluster.mesos

import java.util.{List => JList}
import java.util.concurrent.CountDownLatch

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

import com.google.common.base.Splitter
import org.apache.mesos.{MesosSchedulerDriver, Protos, Scheduler, SchedulerDriver}
import org.apache.mesos.Protos._
import org.apache.mesos.protobuf.{ByteString, GeneratedMessage}

import org.apache.spark.{Logging, SparkConf, SparkContext, SparkException}
import org.apache.spark.util.Utils

/**
 * Shared trait for implementing a Mesos Scheduler. This holds common state and helper
 * methods and Mesos scheduler will use.
 */
private[mesos] trait MesosSchedulerUtils extends Logging {
  // Lock used to wait for scheduler to be registered
  private final val registerLatch = new CountDownLatch(1)

  // Driver for talking to Mesos
  protected var mesosDriver: SchedulerDriver = null

  /**
   * Creates a new MesosSchedulerDriver that communicates to the Mesos master.
    *
    * @param masterUrl The url to connect to Mesos master
   * @param scheduler the scheduler class to receive scheduler callbacks
   * @param sparkUser User to impersonate with when running tasks
   * @param appName The framework name to display on the Mesos UI
   * @param conf Spark configuration
   * @param webuiUrl The WebUI url to link from Mesos UI
   * @param checkpoint Option to checkpoint tasks for failover
   * @param failoverTimeout Duration Mesos master expect scheduler to reconnect on disconnect
   * @param frameworkId The id of the new framework
   */
  protected def createSchedulerDriver(
      masterUrl: String,
      scheduler: Scheduler,
      sparkUser: String,
      appName: String,
      conf: SparkConf,
      webuiUrl: Option[String] = None,
      checkpoint: Option[Boolean] = None,
      failoverTimeout: Option[Double] = None,
      frameworkId: Option[String] = None): SchedulerDriver = {
    val fwInfoBuilder = FrameworkInfo.newBuilder().setUser(sparkUser).setName(appName)
    val credBuilder = Credential.newBuilder()
    webuiUrl.foreach { url => fwInfoBuilder.setWebuiUrl(url) }
    checkpoint.foreach { checkpoint => fwInfoBuilder.setCheckpoint(checkpoint) }
    failoverTimeout.foreach { timeout => fwInfoBuilder.setFailoverTimeout(timeout) }
    frameworkId.foreach { id =>
      fwInfoBuilder.setId(FrameworkID.newBuilder().setValue(id).build())
    }
    conf.getOption("spark.mesos.principal").foreach { principal =>
      fwInfoBuilder.setPrincipal(principal)
      credBuilder.setPrincipal(principal)
    }
    conf.getOption("spark.mesos.secret").foreach { secret =>
      credBuilder.setSecret(ByteString.copyFromUtf8(secret))
    }
    if (credBuilder.hasSecret && !fwInfoBuilder.hasPrincipal) {
      throw new SparkException(
        "spark.mesos.principal must be configured when spark.mesos.secret is set")
    }
    conf.getOption("spark.mesos.role").foreach { role =>
      fwInfoBuilder.setRole(role)
    }
    if (credBuilder.hasPrincipal) {
      new MesosSchedulerDriver(
        scheduler, fwInfoBuilder.build(), masterUrl, credBuilder.build())
    } else {
      new MesosSchedulerDriver(scheduler, fwInfoBuilder.build(), masterUrl)
    }
  }

  /**
   * Starts the MesosSchedulerDriver and stores the current running driver to this new instance.
   * This driver is expected to not be running.
   * This method returns only after the scheduler has registered with Mesos.
   */
  def startScheduler(newDriver: SchedulerDriver): Unit = {
    synchronized {
      if (mesosDriver != null) {
        registerLatch.await()
        return
      }
      @volatile
      var error: Option[Exception] = None

      // We create a new thread that will block inside `mesosDriver.run`
      // until the scheduler exists
      new Thread(Utils.getFormattedClassName(this) + "-mesos-driver") {
        setDaemon(true)
        override def run() {
          try {
            mesosDriver = newDriver
            val ret = mesosDriver.run()
            logInfo("driver.run() returned with code " + ret)
            if (ret != null && ret.equals(Status.DRIVER_ABORTED)) {
              error = Some(new SparkException("Error starting driver, DRIVER_ABORTED"))
              markErr()
            }
          } catch {
            case e: Exception => {
              logError("driver.run() failed", e)
              error = Some(e)
              markErr()
            }
          }
        }
      }.start()

      registerLatch.await()

      // propagate any error to the calling thread. This ensures that SparkContext creation fails
      // without leaving a broken context that won't be able to schedule any tasks
      error.foreach(throw _)
    }
  }

  def getResource(res: JList[Resource], name: String): Double = {
    // A resource can have multiple values in the offer since it can either be from
    // a specific role or wildcard.
    res.asScala.filter(_.getName == name).map(_.getScalar.getValue).sum
  }

  /**
   * Transforms a range resource to a list of ranges
   *
   * @param res the mesos resource list
   * @param name the name of the resource
   * @return the list of ranges returned
   */
  protected def getRangeResource(res: JList[Resource], name: String): List[(Long, Long)] = {
    // A resource can have multiple values in the offer since it can either be from
    // a specific role or wildcard.
    res.asScala.filter(_.getName == name).flatMap(_.getRanges.getRangeList.asScala
      .map(r => (r.getBegin, r.getEnd)).toList).toList
  }

  /**
    * Signal that the scheduler has registered with Mesos.
    */
  protected def markRegistered(): Unit = {
    registerLatch.countDown()
  }

  protected def markErr(): Unit = {
    registerLatch.countDown()
  }

  def createResource(name: String, amount: Double, role: Option[String] = None): Resource = {
    val builder = Resource.newBuilder()
      .setName(name)
      .setType(Value.Type.SCALAR)
      .setScalar(Value.Scalar.newBuilder().setValue(amount).build())

    role.foreach { r => builder.setRole(r) }

    builder.build()
  }

  /**
   * Partition the existing set of resources into two groups, those remaining to be
   * scheduled and those requested to be used for a new task.
    *
    * @param resources The full list of available resources
   * @param resourceName The name of the resource to take from the available resources
   * @param amountToUse The amount of resources to take from the available resources
   * @return The remaining resources list and the used resources list.
   */
  def partitionResources(
      resources: JList[Resource],
      resourceName: String,
      amountToUse: Double): (List[Resource], List[Resource]) = {
    var remain = amountToUse
    var requestedResources = new ArrayBuffer[Resource]
    val remainingResources = resources.asScala.map {
      case r => {
        if (remain > 0 &&
          r.getType == Value.Type.SCALAR &&
          r.getScalar.getValue > 0.0 &&
          r.getName == resourceName) {
          val usage = Math.min(remain, r.getScalar.getValue)
          requestedResources += createResource(resourceName, usage, Some(r.getRole))
          remain -= usage
          createResource(resourceName, r.getScalar.getValue - usage, Some(r.getRole))
        } else {
          r
        }
      }
    }

    // Filter any resource that has depleted.
    val filteredResources =
      remainingResources.filter(r => r.getType != Value.Type.SCALAR || r.getScalar.getValue > 0.0)

    (filteredResources.toList, requestedResources.toList)
  }

  /** Helper method to get the key,value-set pair for a Mesos Attribute protobuf */
  protected def getAttribute(attr: Attribute): (String, Set[String]) = {
    (attr.getName, attr.getText.getValue.split(',').toSet)
  }


  /** Build a Mesos resource protobuf object */
  protected def createResource(resourceName: String, quantity: Double): Protos.Resource = {
    Resource.newBuilder()
      .setName(resourceName)
      .setType(Value.Type.SCALAR)
      .setScalar(Value.Scalar.newBuilder().setValue(quantity).build())
      .build()
  }

  /**
   * Converts the attributes from the resource offer into a Map of name -> Attribute Value
   * The attribute values are the mesos attribute types and they are
    *
    * @param offerAttributes
   * @return
   */
  protected def toAttributeMap(offerAttributes: JList[Attribute]): Map[String, GeneratedMessage] = {
    offerAttributes.asScala.map(attr => {
      val attrValue = attr.getType match {
        case Value.Type.SCALAR => attr.getScalar
        case Value.Type.RANGES => attr.getRanges
        case Value.Type.SET => attr.getSet
        case Value.Type.TEXT => attr.getText
      }
      (attr.getName, attrValue)
    }).toMap
  }


  /**
   * Match the requirements (if any) to the offer attributes.
   * if attribute requirements are not specified - return true
   * else if attribute is defined and no values are given, simple attribute presence is performed
   * else if attribute name and value is specified, subset match is performed on slave attributes
   */
  def matchesAttributeRequirements(
      slaveOfferConstraints: Map[String, Set[String]],
      offerAttributes: Map[String, GeneratedMessage]): Boolean = {
    slaveOfferConstraints.forall {
      // offer has the required attribute and subsumes the required values for that attribute
      case (name, requiredValues) =>
        offerAttributes.get(name) match {
          case None => false
          case Some(_) if requiredValues.isEmpty => true // empty value matches presence
          case Some(scalarValue: Value.Scalar) =>
            // check if provided values is less than equal to the offered values
            requiredValues.map(_.toDouble).exists(_ <= scalarValue.getValue)
          case Some(rangeValue: Value.Range) =>
            val offerRange = rangeValue.getBegin to rangeValue.getEnd
            // Check if there is some required value that is between the ranges specified
            // Note: We only support the ability to specify discrete values, in the future
            // we may expand it to subsume ranges specified with a XX..YY value or something
            // similar to that.
            requiredValues.map(_.toLong).exists(offerRange.contains(_))
          case Some(offeredValue: Value.Set) =>
            // check if the specified required values is a subset of offered set
            requiredValues.subsetOf(offeredValue.getItemList.asScala.toSet)
          case Some(textValue: Value.Text) =>
            // check if the specified value is equal, if multiple values are specified
            // we succeed if any of them match.
            requiredValues.contains(textValue.getValue)
        }
    }
  }

  /**
   * Parses the attributes constraints provided to spark and build a matching data struct:
   *  Map[<attribute-name>, Set[values-to-match]]
   *  The constraints are specified as ';' separated key-value pairs where keys and values
   *  are separated by ':'. The ':' implies equality (for singular values) and "is one of" for
   *  multiple values (comma separated). For example:
   *  {{{
   *  parseConstraintString("tachyon:true;zone:us-east-1a,us-east-1b")
   *  // would result in
   *  <code>
   *  Map(
   *    "tachyon" -> Set("true"),
   *    "zone":   -> Set("us-east-1a", "us-east-1b")
   *  )
   *  }}}
   *
   *  Mesos documentation: http://mesos.apache.org/documentation/attributes-resources/
   *                       https://github.com/apache/mesos/blob/master/src/common/values.cpp
   *                       https://github.com/apache/mesos/blob/master/src/common/attributes.cpp
   *
   * @param constraintsVal constaints string consisting of ';' separated key-value pairs (separated
   *                       by ':')
   * @return  Map of constraints to match resources offers.
   */
  def parseConstraintString(constraintsVal: String): Map[String, Set[String]] = {
    /*
      Based on mesos docs:
      attributes : attribute ( ";" attribute )*
      attribute : labelString ":" ( labelString | "," )+
      labelString : [a-zA-Z0-9_/.-]
    */
    val splitter = Splitter.on(';').trimResults().withKeyValueSeparator(':')
    // kv splitter
    if (constraintsVal.isEmpty) {
      Map()
    } else {
      try {
        splitter.split(constraintsVal).asScala.toMap.mapValues(v =>
          if (v == null || v.isEmpty) {
            Set[String]()
          } else {
            v.split(',').toSet
          }
        )
      } catch {
        case NonFatal(e) =>
          throw new IllegalArgumentException(s"Bad constraint string: $constraintsVal", e)
      }
    }
  }

  // These defaults copied from YARN
  private val MEMORY_OVERHEAD_FRACTION = 0.10
  private val MEMORY_OVERHEAD_MINIMUM = 384

  /**
   * Return the amount of memory to allocate to each executor, taking into account
   * container overheads.
    *
    * @param sc SparkContext to use to get `spark.mesos.executor.memoryOverhead` value
   * @return memory requirement as (0.1 * <memoryOverhead>) or MEMORY_OVERHEAD_MINIMUM
   *         (whichever is larger)
   */
  def executorMemory(sc: SparkContext): Int = {
    sc.conf.getInt("spark.mesos.executor.memoryOverhead",
      math.max(MEMORY_OVERHEAD_FRACTION * sc.executorMemory, MEMORY_OVERHEAD_MINIMUM).toInt) +
      sc.executorMemory
  }

  def setupUris(uris: String, builder: CommandInfo.Builder): Unit = {
    uris.split(",").foreach { uri =>
      builder.addUris(CommandInfo.URI.newBuilder().setValue(uri.trim()))
    }
  }

  protected def getRejectOfferDurationForUnmetConstraints(sc: SparkContext): Long = {
    sc.conf.getTimeAsSeconds("spark.mesos.rejectOfferDurationForUnmetConstraints", "120s")
  }

  /**
   * Checks executor ports if they are within some range of the offered list of ports ranges,
   *
   * @param sc the Spark Context
   * @param ports the list of ports to check
   * @param takenPorts ports already used for that slave
   * @return true if ports are within range false otherwise
   */
  protected def checkPorts(sc: SparkContext, ports: List[(Long, Long)],
                           takenPorts: List[Long] = List()): Boolean = {

    def checkIfInRange(port: Int, ps: List[(Long, Long)]): Boolean = {
      ps.exists(r => r._1 <= port & r._2 >= port)
    }

    val portsToCheck = List(sc.conf.getInt("spark.executor.port", 0),
      sc.conf.getInt("spark.blockManager.port", 0))
    val nonZeroPorts = portsToCheck.filter(_ != 0)

    // If we require a port that is taken we have to decline the offer since mesos
    // shares all port ranges on the slave
    val contained = for {port <- nonZeroPorts}
      yield {
        takenPorts.contains(port)
      }

    if (contained.contains(true)) {
      return false
    }

    val withinRange = nonZeroPorts.forall(p => checkIfInRange(p, ports))

    // make sure we have enough ports to allocate per offer
    ports.map(r => r._2 - r._1 + 1).sum >= portsToCheck.size && withinRange
  }

  /**
   * Partitions port resources.
   *
   * @param conf the spark config
   * @param ports the ports offered
   * @return resources left, port resources to be used and the list of assigned ports
   */
  def partitionPorts(
      conf: SparkConf,
      ports: List[Resource]): (List[Resource], List[Resource], List[Long]) = {

    val taskPortRanges = getRangeResourceWithRoleInfo(ports.asJava, "ports")

    val portsToCheck = List(conf.getInt("spark.executor.port", 0).toLong,
      conf.getInt("spark.blockManager.port", 0).toLong)

    val nonZeroPorts = portsToCheck.filter(_ != 0)

    // reserve non zero ports first

    val nonZeroResources = reservePorts(taskPortRanges, nonZeroPorts)

    // reserve actual port numbers for zero ports - not set by the user

    val numOfZeroPorts = portsToCheck.count(_ == 0)

    val randPorts = pickRandomPortsFromRanges(nonZeroResources._1, numOfZeroPorts)

    val zeroResources = reservePorts(nonZeroResources._1, randPorts)

    val (resourcesLeft, resourcesToBeUsed) = createResources(nonZeroResources, zeroResources)

    (resourcesLeft, resourcesToBeUsed, nonZeroPorts ++ randPorts)
  }

  private def createResources(
      nonZero: (List[PortRangeResourceInfo], List[PortRangeResourceInfo]),
      zero: (List[PortRangeResourceInfo], List[PortRangeResourceInfo]))
      : (List[Resource], List[Resource]) = {

    val resources = {
      if (nonZero._2.isEmpty) { // no user ports were defined
        (zero._1.flatMap{port => createMesosPortResource(port.value, Some(port.role))},
          zero._2.flatMap{port => createMesosPortResource(port.value, Some(port.role))})

      } else if (zero._2.isEmpty) { // no random ports were defined
        (nonZero._1.flatMap{port => createMesosPortResource(port.value, Some(port.role))},
          nonZero._2.flatMap{port => createMesosPortResource(port.value, Some(port.role))})
      }
      else {  // we have user defined and random ports defined
        val left = zero._1.flatMap{port => createMesosPortResource(port.value, Some(port.role))}

        val used = nonZero._2.flatMap{port =>
          createMesosPortResource(port.value, Some(port.role))} ++
          zero._2.flatMap{port => createMesosPortResource(port.value, Some(port.role))}

        (left, used)
      }
    }
    resources
  }

  private case class PortRangeResourceInfo(role: String, value: List[(Long, Long)])

  private def getRangeResourceWithRoleInfo(res: JList[Resource], name: String)
      : List[PortRangeResourceInfo] = {
    // A resource can have multiple values in the offer since it can either be from
    // a specific role or wildcard.
    res.asScala.filter(_.getName == name)
      .map{res => PortRangeResourceInfo(res.getRole, res.getRanges.getRangeList.asScala
        .map(r => (r.getBegin, r.getEnd)).toList) }.toList
  }

  private def reservePorts(
      availablePortRanges: List[PortRangeResourceInfo],
      wantedPorts: List[Long])
      : (List[PortRangeResourceInfo], List[PortRangeResourceInfo]) = {

    if (wantedPorts.isEmpty) { // port list is empty we didnt consume any resources
      return (availablePortRanges, List())
    }

    var tmpLeft = availablePortRanges
    val tmpRanges = for {port <- wantedPorts}
      yield {
        val ret = findPortAndSplitRange(port, tmpLeft)
        val rangeToRemove = ret._1
        val diffRanges = tmpLeft.filterNot{r => r == rangeToRemove}
        val newRangesLeft = diffRanges ++ List(ret._2).flatMap(p => p)
        tmpLeft = newRangesLeft
        ret
      }

    val rangesToRemove = tmpRanges.map(x => x._1)
    val test = availablePortRanges ++ tmpRanges.flatMap{x => x._2}
    val newRangesLeft = (availablePortRanges ++ tmpRanges.flatMap{x => x._2})
      .flatMap{r => removeRanges(r, rangesToRemove)}

    val newRanges = tmpRanges.map{r => PortRangeResourceInfo(r._1.role, List((r._3, r._3)))}

    (newRangesLeft, newRanges)
  }

  private def removeRanges(
      rangeA: PortRangeResourceInfo,
      rangesToRemove: List[PortRangeResourceInfo])
      : Option[PortRangeResourceInfo] = {

   val ranges = rangeA.value.filterNot(rangesToRemove.flatMap{_.value}.toSet)

    if (ranges.isEmpty) {
        None
      } else {
      Some(PortRangeResourceInfo(rangeA.role, ranges))
    }
  }

  private def createMesosPortResource(ranges: List[(Long, Long)],
                                      role: Option[String] = None): List[Resource] = {

    ranges.map { range =>
      val rangeValue = Value.Range.newBuilder()
      rangeValue.setBegin(range._1)
      rangeValue.setEnd(range._2)
      val builder = Resource.newBuilder()
        .setName("ports")
        .setType(Value.Type.RANGES)
        .setRanges(Value.Ranges.newBuilder().addRange(rangeValue))

      role.foreach { r => builder.setRole(r) }
      builder.build()
    }
  }

  private def pickRandomPortsFromRanges(
      ranges: List[PortRangeResourceInfo],
      numToPick: Int): List[Long] = {

    if (numToPick == 0) {
      return List()
    }

    val ports = scala.util.Random.
      shuffle(ranges.flatMap(p => p.value.flatMap(r => (r._1 to r._2).toList))
      .distinct
    )
    require(ports.size >= numToPick)
    ports.take(numToPick)
  }

  private def findPortAndSplitRange(port: Long, ranges: List[PortRangeResourceInfo])
      : (PortRangeResourceInfo, Option[PortRangeResourceInfo], Long) = {

    val rangePortInfo = ranges
      .map{p => val tmpList = List(p.value.filter(r => r._1 <= port & r._2 >= port))
        PortRangeResourceInfo(p.role, tmpList.head)}.filterNot(p => p.value.isEmpty)
      .head

    val range = rangePortInfo.value.head

    val ret = {
      if (port == range._1 && port == range._2) {
        None
      }
      else if (port == range._1 && port != range._2) {
        Some(PortRangeResourceInfo(rangePortInfo.role, List((port + 1, range._2))))
      }
      else if (port == range._2 && port != range._2) {
        Some(PortRangeResourceInfo(rangePortInfo.role, List((range._1, port - 1))))
      }
      else {
        // split range
        val splitList = List((range._1, port - 1), (port + 1, range._2))
        Some(PortRangeResourceInfo(rangePortInfo.role, splitList))
      }
    }

    (rangePortInfo, ret, port)
  }

  /**
   * Retrieves the port resources from a list of mesos offered resources
   *
   * @param resources the mesos resources to parse
   * @return the port resources only
   */
  def getPortResources(resources: List[Resource]) : (List[Resource], List[Resource]) = {
    resources.partition {r => !(r.getType == Value.Type.RANGES & r.getName == "ports")}
  }

  /**
   * Checks if a range allocated for a port was a llocated for a random port
   *
   *  @param conf spark configuration
   *  @param range the range to check
   *  @return true if was assigned for a randome port false otherwise
   */
  def isRandPortRange(conf: SparkConf, range: (Long, Long)): Boolean = {
    val nonZeroPortsToCheck = List(conf.getInt("spark.executor.port", 0).toLong,
      conf.getInt("spark.blockManager.port", 0).toLong).filter(_ != 0)
    val isInNonZero = (port: Long) => if (nonZeroPortsToCheck.isEmpty) {
      false
    } else {
      nonZeroPortsToCheck.contains(port)
    }
    range._1 == range._2 && !isInNonZero(range._1)
  }
}
