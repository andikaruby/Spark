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

package org.apache.spark.streaming.dstream


import scala.deprecated
import scala.collection.mutable.HashMap
import scala.reflect.ClassTag

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}

import org.apache.spark.{Logging, SparkException}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.MetadataCleaner
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.scheduler.Job

/**
 * A Discretized Stream (DStream), the basic abstraction in Spark Streaming, is a continuous
 * sequence of RDDs (of the same type) representing a continuous stream of data (see
 * org.apache.spark.rdd.RDD in the Spark core documentation for more details on RDDs).
 * DStreams can either be created from live data (such as, data from TCP sockets, Kafka, Flume,
 * etc.) using a [[org.apache.spark.streaming.StreamingContext]] or it can be generated by
 * transforming existing DStreams using operations such as `map`,
 * `window` and `reduceByKeyAndWindow`. While a Spark Streaming program is running, each DStream
 * periodically generates a RDD, either from live data or by transforming the RDD generated by a
 * parent DStream.
 *
 * This class contains the basic operations available on all DStreams, such as `map`, `filter` and
 * `window`. In addition, [[org.apache.spark.streaming.dstream.PairDStreamFunctions]] contains
 * operations available only on DStreams of key-value pairs, such as `groupByKeyAndWindow` and
 * `join`. These operations are automatically available on any DStream of pairs
 * (e.g., DStream[(Int, Int)] through implicit conversions when
 * `org.apache.spark.streaming.StreamingContext._` is imported.
 *
 * DStreams internally is characterized by a few basic properties:
 *  - A list of other DStreams that the DStream depends on
 *  - A time interval at which the DStream generates an RDD
 *  - A function that is used to generate an RDD after each time interval
 */

abstract class DStream[T: ClassTag] (
    @transient private[streaming] var ssc: StreamingContext
  ) extends Serializable with Logging {

  // =======================================================================
  // Methods that should be implemented by subclasses of DStream
  // =======================================================================

  /** Time interval after which the DStream generates a RDD */
  def slideDuration: Duration

  /** List of parent DStreams on which this DStream depends on */
  def dependencies: List[DStream[_]]

  /** Method that generates a RDD for the given time */
  def compute (validTime: Time): Option[RDD[T]]

  // =======================================================================
  // Methods and fields available on all DStreams
  // =======================================================================

  // RDDs generated, marked as private[streaming] so that testsuites can access it
  @transient
  private[streaming] var generatedRDDs = new HashMap[Time, RDD[T]] ()

  // Time zero for the DStream
  private[streaming] var zeroTime: Time = null

  // Duration for which the DStream will remember each RDD created
  private[streaming] var rememberDuration: Duration = null

  // Storage level of the RDDs in the stream
  private[streaming] var storageLevel: StorageLevel = StorageLevel.NONE

  // Checkpoint details
  private[streaming] val mustCheckpoint = false
  private[streaming] var checkpointDuration: Duration = null
  private[streaming] val checkpointData = new DStreamCheckpointData(this)

  // Reference to whole DStream graph
  private[streaming] var graph: DStreamGraph = null

  private[streaming] def isInitialized = (zeroTime != null)

  // Duration for which the DStream requires its parent DStream to remember each RDD created
  private[streaming] def parentRememberDuration = rememberDuration

  /** Return the StreamingContext associated with this DStream */
  def context = ssc

  /** Persist the RDDs of this DStream with the given storage level */
  def persist(level: StorageLevel): DStream[T] = {
    if (this.isInitialized) {
      throw new UnsupportedOperationException(
        "Cannot change storage level of an DStream after streaming context has started")
    }
    this.storageLevel = level
    this
  }

  /** Persist RDDs of this DStream with the default storage level (MEMORY_ONLY_SER) */
  def persist(): DStream[T] = persist(StorageLevel.MEMORY_ONLY_SER)

  /** Persist RDDs of this DStream with the default storage level (MEMORY_ONLY_SER) */
  def cache(): DStream[T] = persist()

  /**
   * Enable periodic checkpointing of RDDs of this DStream
   * @param interval Time interval after which generated RDD will be checkpointed
   */
  def checkpoint(interval: Duration): DStream[T] = {
    if (isInitialized) {
      throw new UnsupportedOperationException(
        "Cannot change checkpoint interval of an DStream after streaming context has started")
    }
    persist()
    checkpointDuration = interval
    this
  }

  /**
   * Initialize the DStream by setting the "zero" time, based on which
   * the validity of future times is calculated. This method also recursively initializes
   * its parent DStreams.
   */
  private[streaming] def initialize(time: Time) {
    if (zeroTime != null && zeroTime != time) {
      throw new SparkException("ZeroTime is already initialized to " + zeroTime
        + ", cannot initialize it again to " + time)
    }
    zeroTime = time

    // Set the checkpoint interval to be slideDuration or 10 seconds, which ever is larger
    if (mustCheckpoint && checkpointDuration == null) {
      checkpointDuration = slideDuration * math.ceil(Seconds(10) / slideDuration).toInt
      logInfo("Checkpoint interval automatically set to " + checkpointDuration)
    }

    // Set the minimum value of the rememberDuration if not already set
    var minRememberDuration = slideDuration
    if (checkpointDuration != null && minRememberDuration <= checkpointDuration) {
      // times 2 just to be sure that the latest checkpoint is not forgotten (#paranoia)
      minRememberDuration = checkpointDuration * 2
    }
    if (rememberDuration == null || rememberDuration < minRememberDuration) {
      rememberDuration = minRememberDuration
    }

    // Initialize the dependencies
    dependencies.foreach(_.initialize(zeroTime))
  }

  private[streaming] def validate() {
    assert(rememberDuration != null, "Remember duration is set to null")

    assert(
      !mustCheckpoint || checkpointDuration != null,
      "The checkpoint interval for " + this.getClass.getSimpleName + " has not been set." +
        " Please use DStream.checkpoint() to set the interval."
    )

    assert(
     checkpointDuration == null || context.sparkContext.checkpointDir.isDefined,
      "The checkpoint directory has not been set. Please use StreamingContext.checkpoint()" +
      " or SparkContext.checkpoint() to set the checkpoint directory."
    )

    assert(
      checkpointDuration == null || checkpointDuration >= slideDuration,
      "The checkpoint interval for " + this.getClass.getSimpleName + " has been set to " +
        checkpointDuration + " which is lower than its slide time (" + slideDuration + "). " +
        "Please set it to at least " + slideDuration + "."
    )

    assert(
      checkpointDuration == null || checkpointDuration.isMultipleOf(slideDuration),
      "The checkpoint interval for " + this.getClass.getSimpleName + " has been set to " +
        checkpointDuration + " which not a multiple of its slide time (" + slideDuration + "). " +
        "Please set it to a multiple " + slideDuration + "."
    )

    assert(
      checkpointDuration == null || storageLevel != StorageLevel.NONE,
      "" + this.getClass.getSimpleName + " has been marked for checkpointing but the storage " +
        "level has not been set to enable persisting. Please use DStream.persist() to set the " +
        "storage level to use memory for better checkpointing performance."
    )

    assert(
      checkpointDuration == null || rememberDuration > checkpointDuration,
      "The remember duration for " + this.getClass.getSimpleName + " has been set to " +
        rememberDuration + " which is not more than the checkpoint interval (" +
        checkpointDuration + "). Please set it to higher than " + checkpointDuration + "."
    )

    val metadataCleanerDelay = MetadataCleaner.getDelaySeconds(ssc.conf)
    logInfo("metadataCleanupDelay = " + metadataCleanerDelay)
    assert(
      metadataCleanerDelay < 0 || rememberDuration.milliseconds < metadataCleanerDelay * 1000,
      "It seems you are doing some DStream window operation or setting a checkpoint interval " +
        "which requires " + this.getClass.getSimpleName + " to remember generated RDDs for more " +
        "than " + rememberDuration.milliseconds / 1000 + " seconds. But Spark's metadata cleanup" +
        "delay is set to " + metadataCleanerDelay + " seconds, which is not sufficient. Please " +
        "set the Java property 'spark.cleaner.delay' to more than " +
        math.ceil(rememberDuration.milliseconds / 1000.0).toInt + " seconds."
    )

    dependencies.foreach(_.validate())

    logInfo("Slide time = " + slideDuration)
    logInfo("Storage level = " + storageLevel)
    logInfo("Checkpoint interval = " + checkpointDuration)
    logInfo("Remember duration = " + rememberDuration)
    logInfo("Initialized and validated " + this)
  }

  private[streaming] def setContext(s: StreamingContext) {
    if (ssc != null && ssc != s) {
      throw new SparkException("Context is already set in " + this + ", cannot set it again")
    }
    ssc = s
    logInfo("Set context for " + this)
    dependencies.foreach(_.setContext(ssc))
  }

  private[streaming] def setGraph(g: DStreamGraph) {
    if (graph != null && graph != g) {
      throw new SparkException("Graph is already set in " + this + ", cannot set it again")
    }
    graph = g
    dependencies.foreach(_.setGraph(graph))
  }

  private[streaming] def remember(duration: Duration) {
    if (duration != null && duration > rememberDuration) {
      rememberDuration = duration
      logInfo("Duration for remembering RDDs set to " + rememberDuration + " for " + this)
    }
    dependencies.foreach(_.remember(parentRememberDuration))
  }

  /** Checks whether the 'time' is valid wrt slideDuration for generating RDD */
  private[streaming] def isTimeValid(time: Time): Boolean = {
    if (!isInitialized) {
      throw new SparkException (this + " has not been initialized")
    } else if (time <= zeroTime || ! (time - zeroTime).isMultipleOf(slideDuration)) {
      logInfo("Time " + time + " is invalid as zeroTime is " + zeroTime +
        " and slideDuration is " + slideDuration + " and difference is " + (time - zeroTime))
      false
    } else {
      logDebug("Time " + time + " is valid")
      true
    }
  }

  /**
   * Retrieve a precomputed RDD of this DStream, or computes the RDD. This is an internal
   * method that should not be called directly.
   */
  private[streaming] def getOrCompute(time: Time): Option[RDD[T]] = {
    // If this DStream was not initialized (i.e., zeroTime not set), then do it
    // If RDD was already generated, then retrieve it from HashMap
    generatedRDDs.get(time) match {

      // If an RDD was already generated and is being reused, then
      // probably all RDDs in this DStream will be reused and hence should be cached
      case Some(oldRDD) => Some(oldRDD)

      // if RDD was not generated, and if the time is valid
      // (based on sliding time of this DStream), then generate the RDD
      case None => {
        if (isTimeValid(time)) {
          compute(time) match {
            case Some(newRDD) =>
              if (storageLevel != StorageLevel.NONE) {
                newRDD.persist(storageLevel)
                logInfo("Persisting RDD " + newRDD.id + " for time " +
                  time + " to " + storageLevel + " at time " + time)
              }
              if (checkpointDuration != null &&
                (time - zeroTime).isMultipleOf(checkpointDuration)) {
                newRDD.checkpoint()
                logInfo("Marking RDD " + newRDD.id + " for time " + time +
                  " for checkpointing at time " + time)
              }
              generatedRDDs.put(time, newRDD)
              Some(newRDD)
            case None =>
              None
          }
        } else {
          None
        }
      }
    }
  }

  /**
   * Generate a SparkStreaming job for the given time. This is an internal method that
   * should not be called directly. This default implementation creates a job
   * that materializes the corresponding RDD. Subclasses of DStream may override this
   * to generate their own jobs.
   */
  private[streaming] def generateJob(time: Time): Option[Job] = {
    getOrCompute(time) match {
      case Some(rdd) => {
        val jobFunc = () => {
          val emptyFunc = { (iterator: Iterator[T]) => {} }
          context.sparkContext.runJob(rdd, emptyFunc)
        }
        Some(new Job(time, jobFunc))
      }
      case None => None
    }
  }

  /**
   * Clear metadata that are older than `rememberDuration` of this DStream.
   * This is an internal method that should not be called directly. This default
   * implementation clears the old generated RDDs. Subclasses of DStream may override
   * this to clear their own metadata along with the generated RDDs.
   */
  private[streaming] def clearMetadata(time: Time) {
    val oldRDDs = generatedRDDs.filter(_._1 <= (time - rememberDuration))
    logDebug("Clearing references to old RDDs: [" +
      oldRDDs.map(x => s"${x._1} -> ${x._2.id}").mkString(", ") + "]")
    generatedRDDs --= oldRDDs.keys
    if (ssc.conf.getBoolean("spark.streaming.unpersist", false)) {
      logDebug("Unpersisting old RDDs: " + oldRDDs.values.map(_.id).mkString(", "))
      oldRDDs.values.foreach(_.unpersist(false))
    }
    logDebug("Cleared " + oldRDDs.size + " RDDs that were older than " +
      (time - rememberDuration) + ": " + oldRDDs.keys.mkString(", "))
    dependencies.foreach(_.clearMetadata(time))
  }

  /**
   * Refresh the list of checkpointed RDDs that will be saved along with checkpoint of
   * this stream. This is an internal method that should not be called directly. This is
   * a default implementation that saves only the file names of the checkpointed RDDs to
   * checkpointData. Subclasses of DStream (especially those of InputDStream) may override
   * this method to save custom checkpoint data.
   */
  private[streaming] def updateCheckpointData(currentTime: Time) {
    logDebug("Updating checkpoint data for time " + currentTime)
    checkpointData.update(currentTime)
    dependencies.foreach(_.updateCheckpointData(currentTime))
    logDebug("Updated checkpoint data for time " + currentTime + ": " + checkpointData)
  }

  private[streaming] def clearCheckpointData(time: Time) {
    logDebug("Clearing checkpoint data")
    checkpointData.cleanup(time)
    dependencies.foreach(_.clearCheckpointData(time))
    logDebug("Cleared checkpoint data")
  }

  /**
   * Restore the RDDs in generatedRDDs from the checkpointData. This is an internal method
   * that should not be called directly. This is a default implementation that recreates RDDs
   * from the checkpoint file names stored in checkpointData. Subclasses of DStream that
   * override the updateCheckpointData() method would also need to override this method.
   */
  private[streaming] def restoreCheckpointData() {
    // Create RDDs from the checkpoint data
    logInfo("Restoring checkpoint data")
    checkpointData.restore()
    dependencies.foreach(_.restoreCheckpointData())
    logInfo("Restored checkpoint data")
  }

  @throws(classOf[IOException])
  private def writeObject(oos: ObjectOutputStream) {
    logDebug(this.getClass().getSimpleName + ".writeObject used")
    if (graph != null) {
      graph.synchronized {
        if (graph.checkpointInProgress) {
          oos.defaultWriteObject()
        } else {
          val msg = "Object of " + this.getClass.getName + " is being serialized " +
            " possibly as a part of closure of an RDD operation. This is because " +
            " the DStream object is being referred to from within the closure. " +
            " Please rewrite the RDD operation inside this DStream to avoid this. " +
            " This has been enforced to avoid bloating of Spark tasks " +
            " with unnecessary objects."
          throw new java.io.NotSerializableException(msg)
        }
      }
    } else {
      throw new java.io.NotSerializableException(
        "Graph is unexpectedly null when DStream is being serialized.")
    }
  }

  @throws(classOf[IOException])
  private def readObject(ois: ObjectInputStream) {
    logDebug(this.getClass().getSimpleName + ".readObject used")
    ois.defaultReadObject()
    generatedRDDs = new HashMap[Time, RDD[T]] ()
  }

  // =======================================================================
  // DStream operations
  // =======================================================================

  /** Return a new DStream by applying a function to all elements of this DStream. */
  def map[U: ClassTag](mapFunc: T => U): DStream[U] = {
    new MappedDStream(this, context.sparkContext.clean(mapFunc))
  }

  /**
   * Return a new DStream by applying a function to all elements of this DStream,
   * and then flattening the results
   */
  def flatMap[U: ClassTag](flatMapFunc: T => Traversable[U]): DStream[U] = {
    new FlatMappedDStream(this, context.sparkContext.clean(flatMapFunc))
  }

  /** Return a new DStream containing only the elements that satisfy a predicate. */
  def filter(filterFunc: T => Boolean): DStream[T] = new FilteredDStream(this, filterFunc)

  /**
   * Return a new DStream in which each RDD is generated by applying glom() to each RDD of
   * this DStream. Applying glom() to an RDD coalesces all elements within each partition into
   * an array.
   */
  def glom(): DStream[Array[T]] = new GlommedDStream(this)


  /**
   * Return a new DStream with an increased or decreased level of parallelism. Each RDD in the
   * returned DStream has exactly numPartitions partitions.
   */
  def repartition(numPartitions: Int): DStream[T] = this.transform(_.repartition(numPartitions))

  /**
   * Return a new DStream in which each RDD is generated by applying mapPartitions() to each RDDs
   * of this DStream. Applying mapPartitions() to an RDD applies a function to each partition
   * of the RDD.
   */
  def mapPartitions[U: ClassTag](
      mapPartFunc: Iterator[T] => Iterator[U],
      preservePartitioning: Boolean = false
    ): DStream[U] = {
    new MapPartitionedDStream(this, context.sparkContext.clean(mapPartFunc), preservePartitioning)
  }

  /**
   * Return a new DStream in which each RDD has a single element generated by reducing each RDD
   * of this DStream.
   */
  def reduce(reduceFunc: (T, T) => T): DStream[T] =
    this.map(x => (null, x)).reduceByKey(reduceFunc, 1).map(_._2)

  /**
   * Return a new DStream in which each RDD has a single element generated by counting each RDD
   * of this DStream.
   */
  def count(): DStream[Long] = {
    this.map(_ => (null, 1L))
        .transform(_.union(context.sparkContext.makeRDD(Seq((null, 0L)), 1)))
        .reduceByKey(_ + _)
        .map(_._2)
  }

  /**
   * Return a new DStream in which each RDD contains the counts of each distinct value in
   * each RDD of this DStream. Hash partitioning is used to generate
   * the RDDs with `numPartitions` partitions (Spark's default number of partitions if
   * `numPartitions` not specified).
   */
  def countByValue(numPartitions: Int = ssc.sc.defaultParallelism): DStream[(T, Long)] =
    this.map(x => (x, 1L)).reduceByKey((x: Long, y: Long) => x + y, numPartitions)

  /**
   * Apply a function to each RDD in this DStream. This is an output operator, so
   * 'this' DStream will be registered as an output stream and therefore materialized.
   */
  @deprecated("use foreachRDD", "0.9.0")
  def foreach(foreachFunc: RDD[T] => Unit): Unit = {
    this.foreachRDD(foreachFunc)
  }

  /**
   * Apply a function to each RDD in this DStream. This is an output operator, so
   * 'this' DStream will be registered as an output stream and therefore materialized.
   */
  @deprecated("use foreachRDD", "0.9.0")
  def foreach(foreachFunc: (RDD[T], Time) => Unit): Unit = {
    this.foreachRDD(foreachFunc)
  }

  /**
   * Apply a function to each RDD in this DStream. This is an output operator, so
   * 'this' DStream will be registered as an output stream and therefore materialized.
   */
  def foreachRDD(foreachFunc: RDD[T] => Unit) {
    this.foreachRDD((r: RDD[T], t: Time) => foreachFunc(r))
  }

  /**
   * Apply a function to each RDD in this DStream. This is an output operator, so
   * 'this' DStream will be registered as an output stream and therefore materialized.
   */
  def foreachRDD(foreachFunc: (RDD[T], Time) => Unit) {
    new ForEachDStream(this, context.sparkContext.clean(foreachFunc)).register()
  }

  /**
   * Return a new DStream in which each RDD is generated by applying a function
   * on each RDD of 'this' DStream.
   */
  def transform[U: ClassTag](transformFunc: RDD[T] => RDD[U]): DStream[U] = {
    transform((r: RDD[T], t: Time) => context.sparkContext.clean(transformFunc(r)))
  }

  /**
   * Return a new DStream in which each RDD is generated by applying a function
   * on each RDD of 'this' DStream.
   */
  def transform[U: ClassTag](transformFunc: (RDD[T], Time) => RDD[U]): DStream[U] = {
    val cleanedF = context.sparkContext.clean(transformFunc)
    val realTransformFunc =  (rdds: Seq[RDD[_]], time: Time) => {
      assert(rdds.length == 1)
      cleanedF(rdds.head.asInstanceOf[RDD[T]], time)
    }
    new TransformedDStream[U](Seq(this), realTransformFunc)
  }

  /**
   * Return a new DStream in which each RDD is generated by applying a function
   * on each RDD of 'this' DStream and 'other' DStream.
   */
  def transformWith[U: ClassTag, V: ClassTag](
      other: DStream[U], transformFunc: (RDD[T], RDD[U]) => RDD[V]
    ): DStream[V] = {
    val cleanedF = ssc.sparkContext.clean(transformFunc)
    transformWith(other, (rdd1: RDD[T], rdd2: RDD[U], time: Time) => cleanedF(rdd1, rdd2))
  }

  /**
   * Return a new DStream in which each RDD is generated by applying a function
   * on each RDD of 'this' DStream and 'other' DStream.
   */
  def transformWith[U: ClassTag, V: ClassTag](
      other: DStream[U], transformFunc: (RDD[T], RDD[U], Time) => RDD[V]
    ): DStream[V] = {
    val cleanedF = ssc.sparkContext.clean(transformFunc)
    val realTransformFunc = (rdds: Seq[RDD[_]], time: Time) => {
      assert(rdds.length == 2)
      val rdd1 = rdds(0).asInstanceOf[RDD[T]]
      val rdd2 = rdds(1).asInstanceOf[RDD[U]]
      cleanedF(rdd1, rdd2, time)
    }
    new TransformedDStream[V](Seq(this, other), realTransformFunc)
  }

  /**
   * Print the first ten elements of each RDD generated in this DStream. This is an output
   * operator, so this DStream will be registered as an output stream and there materialized.
   */
  def print() {
    def foreachFunc = (rdd: RDD[T], time: Time) => {
      val first11 = rdd.take(11)
      println ("-------------------------------------------")
      println ("Time: " + time)
      println ("-------------------------------------------")
      first11.take(10).foreach(println)
      if (first11.size > 10) println("...")
      println()
    }
    new ForEachDStream(this, context.sparkContext.clean(foreachFunc)).register()
  }

  /**
   * Return a new DStream in which each RDD contains all the elements in seen in a
   * sliding window of time over this DStream. The new DStream generates RDDs with
   * the same interval as this DStream.
   * @param windowDuration width of the window; must be a multiple of this DStream's interval.
   */
  def window(windowDuration: Duration): DStream[T] = window(windowDuration, this.slideDuration)

  /**
   * Return a new DStream in which each RDD contains all the elements in seen in a
   * sliding window of time over this DStream.
   * @param windowDuration width of the window; must be a multiple of this DStream's
   *                       batching interval
   * @param slideDuration  sliding interval of the window (i.e., the interval after which
   *                       the new DStream will generate RDDs); must be a multiple of this
   *                       DStream's batching interval
   */
  def window(windowDuration: Duration, slideDuration: Duration): DStream[T] = {
    new WindowedDStream(this, windowDuration, slideDuration)
  }

  /**
   * Return a new DStream in which each RDD has a single element generated by reducing all
   * elements in a sliding window over this DStream.
   * @param reduceFunc associative reduce function
   * @param windowDuration width of the window; must be a multiple of this DStream's
   *                       batching interval
   * @param slideDuration  sliding interval of the window (i.e., the interval after which
   *                       the new DStream will generate RDDs); must be a multiple of this
   *                       DStream's batching interval
   */
  def reduceByWindow(
      reduceFunc: (T, T) => T,
      windowDuration: Duration,
      slideDuration: Duration
    ): DStream[T] = {
    this.reduce(reduceFunc).window(windowDuration, slideDuration).reduce(reduceFunc)
  }

  /**
   * Return a new DStream in which each RDD has a single element generated by reducing all
   * elements in a sliding window over this DStream. However, the reduction is done incrementally
   * using the old window's reduced value :
   *  1. reduce the new values that entered the window (e.g., adding new counts)
   *  2. "inverse reduce" the old values that left the window (e.g., subtracting old counts)
   *  This is more efficient than reduceByWindow without "inverse reduce" function.
   *  However, it is applicable to only "invertible reduce functions".
   * @param reduceFunc associative reduce function
   * @param invReduceFunc inverse reduce function
   * @param windowDuration width of the window; must be a multiple of this DStream's
   *                       batching interval
   * @param slideDuration  sliding interval of the window (i.e., the interval after which
   *                       the new DStream will generate RDDs); must be a multiple of this
   *                       DStream's batching interval
   */
  def reduceByWindow(
      reduceFunc: (T, T) => T,
      invReduceFunc: (T, T) => T,
      windowDuration: Duration,
      slideDuration: Duration
    ): DStream[T] = {
      this.map(x => (1, x))
          .reduceByKeyAndWindow(reduceFunc, invReduceFunc, windowDuration, slideDuration, 1)
          .map(_._2)
  }

  /**
   * Return a new DStream in which each RDD has a single element generated by counting the number
   * of elements in a sliding window over this DStream. Hash partitioning is used to generate
   * the RDDs with Spark's default number of partitions.
   * @param windowDuration width of the window; must be a multiple of this DStream's
   *                       batching interval
   * @param slideDuration  sliding interval of the window (i.e., the interval after which
   *                       the new DStream will generate RDDs); must be a multiple of this
   *                       DStream's batching interval
   */
  def countByWindow(windowDuration: Duration, slideDuration: Duration): DStream[Long] = {
    this.map(_ => 1L).reduceByWindow(_ + _, _ - _, windowDuration, slideDuration)
  }

  /**
   * Return a new DStream in which each RDD contains the count of distinct elements in
   * RDDs in a sliding window over this DStream. Hash partitioning is used to generate
   * the RDDs with `numPartitions` partitions (Spark's default number of partitions if
   * `numPartitions` not specified).
   * @param windowDuration width of the window; must be a multiple of this DStream's
   *                       batching interval
   * @param slideDuration  sliding interval of the window (i.e., the interval after which
   *                       the new DStream will generate RDDs); must be a multiple of this
   *                       DStream's batching interval
   * @param numPartitions  number of partitions of each RDD in the new DStream.
   */
  def countByValueAndWindow(
      windowDuration: Duration,
      slideDuration: Duration,
      numPartitions: Int = ssc.sc.defaultParallelism
    ): DStream[(T, Long)] = {

    this.map(x => (x, 1L)).reduceByKeyAndWindow(
      (x: Long, y: Long) => x + y,
      (x: Long, y: Long) => x - y,
      windowDuration,
      slideDuration,
      numPartitions,
      (x: (T, Long)) => x._2 != 0L
    )
  }

  /**
   * Return a new DStream by unifying data of another DStream with this DStream.
   * @param that Another DStream having the same slideDuration as this DStream.
   */
  def union(that: DStream[T]): DStream[T] = new UnionDStream[T](Array(this, that))

  /**
   * Return all the RDDs defined by the Interval object (both end times included)
   */
  def slice(interval: Interval): Seq[RDD[T]] = {
    slice(interval.beginTime, interval.endTime)
  }

  /**
   * Return all the RDDs between 'fromTime' to 'toTime' (both included)
   */
  def slice(fromTime: Time, toTime: Time): Seq[RDD[T]] = {
    if (!isInitialized) {
      throw new SparkException(this + " has not been initialized")
    }
    if (!(fromTime - zeroTime).isMultipleOf(slideDuration)) {
      logWarning("fromTime (" + fromTime + ") is not a multiple of slideDuration ("
        + slideDuration + ")")
    }
    if (!(toTime - zeroTime).isMultipleOf(slideDuration)) {
      logWarning("toTime (" + fromTime + ") is not a multiple of slideDuration ("
        + slideDuration + ")")
    }
    val alignedToTime = toTime.floor(slideDuration)
    val alignedFromTime = fromTime.floor(slideDuration)

    logInfo("Slicing from " + fromTime + " to " + toTime +
      " (aligned to " + alignedFromTime + " and " + alignedToTime + ")")

    alignedFromTime.to(alignedToTime, slideDuration).flatMap(time => {
      if (time >= zeroTime) getOrCompute(time) else None
    })
  }

  /**
   * Save each RDD in this DStream as a Sequence file of serialized objects.
   * The file name at each batch interval is generated based on `prefix` and
   * `suffix`: "prefix-TIME_IN_MS.suffix".
   */
  def saveAsObjectFiles(prefix: String, suffix: String = "") {
    val saveFunc = (rdd: RDD[T], time: Time) => {
      val file = rddToFileName(prefix, suffix, time)
      rdd.saveAsObjectFile(file)
    }
    this.foreachRDD(saveFunc)
  }

  /**
   * Save each RDD in this DStream as at text file, using string representation
   * of elements. The file name at each batch interval is generated based on
   * `prefix` and `suffix`: "prefix-TIME_IN_MS.suffix".
   */
  def saveAsTextFiles(prefix: String, suffix: String = "") {
    val saveFunc = (rdd: RDD[T], time: Time) => {
      val file = rddToFileName(prefix, suffix, time)
      rdd.saveAsTextFile(file)
    }
    this.foreachRDD(saveFunc)
  }

  /**
   * Register this streaming as an output stream. This would ensure that RDDs of this
   * DStream will be generated.
   */
  private[streaming] def register(): DStream[T] = {
    ssc.graph.addOutputStream(this)
    this
  }
}
