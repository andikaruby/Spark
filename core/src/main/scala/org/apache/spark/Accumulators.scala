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

package org.apache.spark

import java.io.{ObjectInputStream, Serializable}

import scala.collection.generic.Growable
import scala.collection.Map
import scala.collection.mutable
import scala.ref.WeakReference
import scala.reflect.ClassTag

import org.apache.spark.serializer.JavaSerializer
import org.apache.spark.util.Utils

/**
 * A data type that can be accumulated, ie has an commutative and associative "add" operation,
 * but where the result type, `R`, may be different from the element type being added, `T`.
 *
 * You must define how to add data, and how to merge two of these together.  For some data types,
 * such as a counter, these might be the same operation. In that case, you can use the simpler
 * [[org.apache.spark.Accumulator]]. They won't always be the same, though -- e.g., imagine you are
 * accumulating a set. You will add items to the set, and you will union two sets together.
 *
 * TODO: document thread-safety.
 *
 * @param initialValue initial value of accumulator
 * @param param helper object defining how to add elements of type `R` and `T`
 * @param name human-readable name for use in Spark's web UI
 * @param internal if this [[Accumulable]] is internal. Internal [[Accumulable]]s will be reported
 *                 to the driver via heartbeats. For internal [[Accumulable]]s, `R` must be
 *                 thread safe so that they can be reported correctly.
 * @tparam R the full accumulated data (result type)
 * @tparam T partial data that can be added in
 */
class Accumulable[R, T] private[spark] (
    initialValue: R,
    param: AccumulableParam[R, T],
    val name: Option[String],
    internal: Boolean)
  extends Serializable {

  private[spark] def this(
      @transient initialValue: R, param: AccumulableParam[R, T], internal: Boolean) = {
    this(initialValue, param, None, internal)
  }

  def this(@transient initialValue: R, param: AccumulableParam[R, T], name: Option[String]) =
    this(initialValue, param, name, false)

  def this(@transient initialValue: R, param: AccumulableParam[R, T]) =
    this(initialValue, param, None)

  val id: Long = Accumulators.newId()

  @volatile private var value_ : R = initialValue
  val zero = param.zero(initialValue)  // Zero value to be passed to workers

  Accumulators.register(this)

  /**
   * If this [[Accumulable]] is internal. Internal [[Accumulable]]s will be reported to the driver
   * via heartbeats. For internal [[Accumulable]]s, `R` must be thread safe so that they can be
   * reported correctly.
   */
  private[spark] def isInternal: Boolean = internal

  /**
   * Add more data to this accumulator / accumulable
   * @param term the data to add
   */
  def += (term: T): Unit = { value_ = param.addAccumulator(value_, term) }

  /**
   * Add more data to this accumulator / accumulable
   * @param term the data to add
   */
  def add(term: T): Unit = { value_ = param.addAccumulator(value_, term) }

  /**
   * Merge two accumulable objects together
   *
   * Normally, a user will not want to use this version, but will instead call `+=`.
   * @param term the other `R` that will get merged with this
   */
  def ++= (term: R): Unit = { value_ = param.addInPlace(value_, term) }

  /**
   * Merge two accumulable objects together
   *
   * Normally, a user will not want to use this version, but will instead call `add`.
   * @param term the other `R` that will get merged with this
   */
  def merge(term: R): Unit = { value_ = param.addInPlace(value_, term) }

  /**
   * Access the accumulator's current value; only allowed on master.
   */
  def value: R = value_

  /**
   * Get the current value of this accumulator from within a task.
   *
   * This is NOT the global value of the accumulator.  To get the global value after a
   * completed operation on the dataset, call `value`.
   *
   * The typical use of this method is to directly mutate the local value, eg., to add
   * an element to a Set.
   *
   * TODO: probably don't need this.
   */
  def localValue: R = value_

  /**
   * Set the accumulator's value.
   */
  def value_= (newValue: R): Unit = {
    value_ = newValue
  }

  /**
   * Set the accumulator's value.
   */
  def setValue(newValue: R): Unit = {
    this.value = newValue
  }

  // Called by Java when deserializing an object
  private def readObject(in: ObjectInputStream): Unit = Utils.tryOrIOException {
    in.defaultReadObject()
    // Automatically register the accumulator when it is deserialized with the task closure.
    //
    // Note internal accumulators sent with task are deserialized before the TaskContext is created
    // and are registered in the TaskContext constructor. Other internal accumulators, such SQL
    // metrics, still need to register here.
    val taskContext = TaskContext.get()
    if (taskContext != null) {
      taskContext.registerAccumulator(this)
    }
  }

  override def toString: String = if (value_ == null) "null" else value_.toString
}

/**
 * Helper object defining how to accumulate values of a particular type. An implicit
 * AccumulableParam needs to be available when you create [[Accumulable]]s of a specific type.
 *
 * @tparam R the full accumulated data (result type)
 * @tparam T partial data that can be added in
 */
trait AccumulableParam[R, T] extends Serializable {
  /**
   * Add additional data to the accumulator value. Is allowed to modify and return `r`
   * for efficiency (to avoid allocating objects).
   *
   * @param r the current value of the accumulator
   * @param t the data to be added to the accumulator
   * @return the new value of the accumulator
   */
  def addAccumulator(r: R, t: T): R

  /**
   * Merge two accumulated values together. Is allowed to modify and return the first value
   * for efficiency (to avoid allocating objects).
   *
   * @param r1 one set of accumulated data
   * @param r2 another set of accumulated data
   * @return both data sets merged together
   */
  def addInPlace(r1: R, r2: R): R

  /**
   * Return the "zero" (identity) value for an accumulator type, given its initial value. For
   * example, if R was a vector of N dimensions, this would return a vector of N zeroes.
   */
  def zero(initialValue: R): R
}

private[spark] class
GrowableAccumulableParam[R <% Growable[T] with TraversableOnce[T] with Serializable: ClassTag, T]
  extends AccumulableParam[R, T] {

  def addAccumulator(growable: R, elem: T): R = {
    growable += elem
    growable
  }

  def addInPlace(t1: R, t2: R): R = {
    t1 ++= t2
    t1
  }

  def zero(initialValue: R): R = {
    // We need to clone initialValue, but it's hard to specify that R should also be Cloneable.
    // Instead we'll serialize it to a buffer and load it back.
    val ser = new JavaSerializer(new SparkConf(false)).newInstance()
    val copy = ser.deserialize[R](ser.serialize(initialValue))
    copy.clear()   // In case it contained stuff
    copy
  }
}

/**
 * A simpler value of [[Accumulable]] where the result type being accumulated is the same
 * as the types of elements being merged, i.e. variables that are only "added" to through an
 * associative operation and can therefore be efficiently supported in parallel. They can be used
 * to implement counters (as in MapReduce) or sums. Spark natively supports accumulators of numeric
 * value types, and programmers can add support for new types.
 *
 * An accumulator is created from an initial value `v` by calling [[SparkContext#accumulator]].
 * Tasks running on the cluster can then add to it using the [[Accumulable#+=]] operator.
 * However, they cannot read its value. Only the driver program can read the accumulator's value,
 * using its value method.
 *
 * The interpreter session below shows an accumulator being used to add up the elements of an array:
 *
 * {{{
 * scala> val accum = sc.accumulator(0)
 * accum: spark.Accumulator[Int] = 0
 *
 * scala> sc.parallelize(Array(1, 2, 3, 4)).foreach(x => accum += x)
 * ...
 * 10/09/29 18:41:08 INFO SparkContext: Tasks finished in 0.317106 s
 *
 * scala> accum.value
 * res2: Int = 10
 * }}}
 *
 * @param initialValue initial value of accumulator
 * @param param helper object defining how to add elements of type `T`
 * @tparam T result type
 */
class Accumulator[T] private[spark] (
    @transient private[spark] val initialValue: T,
    param: AccumulatorParam[T],
    name: Option[String],
    internal: Boolean)
  extends Accumulable[T, T](initialValue, param, name, internal) {

  def this(initialValue: T, param: AccumulatorParam[T], name: Option[String]) = {
    this(initialValue, param, name, false)
  }

  def this(initialValue: T, param: AccumulatorParam[T]) = {
    this(initialValue, param, None, false)
  }
}

/**
 * A simpler version of [[org.apache.spark.AccumulableParam]] where the only data type you can add
 * in is the same type as the accumulated value. An implicit AccumulatorParam object needs to be
 * available when you create Accumulators of a specific type.
 *
 * @tparam T type of value to accumulate
 */
trait AccumulatorParam[T] extends AccumulableParam[T, T] {
  def addAccumulator(t1: T, t2: T): T = {
    addInPlace(t1, t2)
  }
}

object AccumulatorParam {

  // The following implicit objects were in SparkContext before 1.2 and users had to
  // `import SparkContext._` to enable them. Now we move them here to make the compiler find
  // them automatically. However, as there are duplicate codes in SparkContext for backward
  // compatibility, please update them accordingly if you modify the following implicit objects.

  implicit object DoubleAccumulatorParam extends AccumulatorParam[Double] {
    def addInPlace(t1: Double, t2: Double): Double = t1 + t2
    def zero(initialValue: Double): Double = 0.0
  }

  implicit object IntAccumulatorParam extends AccumulatorParam[Int] {
    def addInPlace(t1: Int, t2: Int): Int = t1 + t2
    def zero(initialValue: Int): Int = 0
  }

  implicit object LongAccumulatorParam extends AccumulatorParam[Long] {
    def addInPlace(t1: Long, t2: Long): Long = t1 + t2
    def zero(initialValue: Long): Long = 0L
  }

  implicit object FloatAccumulatorParam extends AccumulatorParam[Float] {
    def addInPlace(t1: Float, t2: Float): Float = t1 + t2
    def zero(initialValue: Float): Float = 0f
  }

  // TODO: Add AccumulatorParams for other types, e.g. lists and strings
}

// TODO: The multi-thread support in accumulators is kind of lame; check
// if there's a more intuitive way of doing it right
private[spark] object Accumulators extends Logging {
  /**
   * This global map holds the original accumulator objects that are created on the driver.
   * It keeps weak references to these objects so that accumulators can be garbage-collected
   * once the RDDs and user-code that reference them are cleaned up.
   */
  val originals = mutable.Map[Long, WeakReference[Accumulable[_, _]]]()

  private var lastId: Long = 0

  def newId(): Long = synchronized {
    lastId += 1
    lastId
  }

  def register(a: Accumulable[_, _]): Unit = synchronized {
    originals(a.id) = new WeakReference[Accumulable[_, _]](a)
  }

  def remove(accId: Long) {
    synchronized {
      originals.remove(accId)
    }
  }

  // Add values to the original accumulators with some given IDs
  def add(values: Map[Long, Any]): Unit = synchronized {
    for ((id, value) <- values) {
      if (originals.contains(id)) {
        // Since we are now storing weak references, we must check whether the underlying data
        // is valid.
        originals(id).get match {
          case Some(accum) => accum.asInstanceOf[Accumulable[Any, Any]] ++= value
          case None =>
            throw new IllegalAccessError("Attempted to access garbage collected Accumulator.")
        }
      } else {
        logWarning(s"Ignoring accumulator update for unknown accumulator id $id")
      }
    }
  }

}

private[spark] object InternalAccumulator {

  // Names of internal task level metrics
  val EXECUTOR_DESERIALIZE_TIME = "metrics.executorDeserializeTime"
  val EXECUTOR_RUN_TIME = "metrics.executorRunTime"
  val RESULT_SIZE = "metrics.resultSize"
  val JVM_GC_TIME = "metrics.jvmGCTime"
  val RESULT_SERIALIZATION_TIME = "metrics.resultSerializationTime"
  val MEMORY_BYTES_SPILLED = "metrics.memoryBytesSpilled"
  val DISK_BYTES_SPILLED = "metrics.diskBytesSpilled"
  val PEAK_EXECUTION_MEMORY = "metrics.peakExecutionMemory"
  val TEST_ACCUM = "metrics.testAccumulator"

  // Names of shuffle read metrics
  object shuffleRead {
    val REMOTE_BLOCKS_FETCHED = "metrics.shuffle.read.remoteBlocksFetched"
    val LOCAL_BLOCKS_FETCHED = "metrics.shuffle.read.localBlocksFetched"
    val REMOTE_BYTES_READ = "metrics.shuffle.read.remoteBytesRead"
    val LOCAL_BYTES_READ = "metrics.shuffle.read.localBytesRead"
    val FETCH_WAIT_TIME = "metrics.shuffle.read.fetchWaitTime"
    val RECORDS_READ = "metrics.shuffle.read.recordsRead"
  }

  // Names of shuffle write metrics
  object shuffleWrite {
    val BYTES_WRITTEN = "metrics.shuffle.write.shuffleBytesWritten"
    val RECORDS_WRITTEN = "metrics.shuffle.write.shuffleRecordsWritten"
    val WRITE_TIME = "metrics.shuffle.write.shuffleWriteTime"
  }

  // Names of output metrics
  object output {
    val BYTES_WRITTEN = "metrics.output.bytesWritten"
    val RECORDS_WRITTEN = "metrics.output.recordsWritten"
  }

  // Names of input metrics
  object input {
    val BYTES_READ = "metrics.input.bytesRead"
    val RECORDS_READ = "metrics.input.recordsRead"
  }

  /**
   * Create a new internal Long accumulator with the specified name.
   */
  private def newMetric(name: String): Accumulator[Long] = {
    new Accumulator(0L, AccumulatorParam.LongAccumulatorParam, Some(name), internal = true)
  }

  /**
   * Accumulators for tracking internal metrics.
   * Note: this method does not register accumulators for cleanup.
   */
  def create(): Seq[Accumulator[Long]] = {
    val metricNames = Seq[String](
      EXECUTOR_DESERIALIZE_TIME,
      EXECUTOR_RUN_TIME,
      RESULT_SIZE,
      JVM_GC_TIME,
      RESULT_SERIALIZATION_TIME,
      MEMORY_BYTES_SPILLED,
      DISK_BYTES_SPILLED,
      PEAK_EXECUTION_MEMORY) ++
      sys.props.get("spark.testing").map(_ => TEST_ACCUM).toSeq
    metricNames.map(newMetric) ++
      createShuffleReadAccums() ++
      createShuffleWriteAccums() ++
      createInputAccums() ++
      createOutputAccums()
  }

  /**
   * Accumulators for tracking shuffle read metrics.
   * Note: this method does not register accumulators for cleanup.
   */
  def createShuffleReadAccums(): Seq[Accumulator[Long]] = {
    Seq[String](
      shuffleRead.REMOTE_BLOCKS_FETCHED,
      shuffleRead.LOCAL_BLOCKS_FETCHED,
      shuffleRead.REMOTE_BYTES_READ,
      shuffleRead.LOCAL_BYTES_READ,
      shuffleRead.FETCH_WAIT_TIME,
      shuffleRead.RECORDS_READ).map(newMetric)
  }

  /**
   * Accumulators for tracking shuffle write metrics.
   * Note: this method does not register accumulators for cleanup.
   */
  def createShuffleWriteAccums(): Seq[Accumulator[Long]] = {
    Seq[String](
      shuffleWrite.BYTES_WRITTEN,
      shuffleWrite.RECORDS_WRITTEN,
      shuffleWrite.WRITE_TIME).map(newMetric)
  }

  /**
   * Accumulators for tracking input metrics.
   * Note: this method does not register accumulators for cleanup.
   */
  def createInputAccums(): Seq[Accumulator[Long]] = {
    Seq[String](input.BYTES_READ, input.RECORDS_READ).map(newMetric)
  }

  /**
   * Accumulators for tracking output metrics.
   * Note: this method does not register accumulators for cleanup.
   */
  private def createOutputAccums(): Seq[Accumulator[Long]] = {
    Seq[String](output.BYTES_WRITTEN, output.RECORDS_WRITTEN).map(newMetric)
  }

  /**
   * Accumulators for tracking internal metrics.
   *
   * These accumulators are created with the stage such that all tasks in the stage will
   * add to the same set of accumulators. We do this to report the distribution of accumulator
   * values across all tasks within each stage.
   */
  def create(sc: SparkContext): Seq[Accumulator[Long]] = {
    val accums = create()
    accums.foreach { accum =>
      sc.cleaner.foreach(_.registerAccumulatorForCleanup(accum))
    }
    accums
  }
}
