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

package org.apache.spark.streaming.api.python

import java.util.{ArrayList => JArrayList}

import org.apache.spark.Partitioner
import org.apache.spark.rdd.{CoGroupedRDD, UnionRDD, PartitionerAwareUnionRDD, RDD}
import org.apache.spark.api.java._
import org.apache.spark.api.python._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Interval, Duration, Time}
import org.apache.spark.streaming.dstream._
import org.apache.spark.streaming.api.java._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag


/**
 * Interface for Python callback function with two arguments
 */
trait PythonRDDFunction {
  def call(rdd: JavaRDD[_], time: Long): JavaRDD[Array[Byte]]
}

class RDDFunction(pfunc: PythonRDDFunction) {
  def apply(rdd: Option[RDD[_]], time: Time): Option[RDD[Array[Byte]]] = {
    val jrdd = if (rdd.isDefined) {
      JavaRDD.fromRDD(rdd.get)
    } else {
      null
    }
    val r = pfunc.call(jrdd, time.milliseconds)
    if (r != null) {
      Some(r.rdd)
    } else {
      None
    }
  }
}

/**
 * Interface for Python callback function with three arguments
 */
trait PythonRDDFunction2 {
  def call(rdd: JavaRDD[_], rdd2: JavaRDD[_], time: Long): JavaRDD[Array[Byte]]
}

class RDDFunction2(pfunc: PythonRDDFunction2) {
  def apply(rdd: Option[RDD[_]], rdd2: Option[RDD[_]], time: Time): Option[RDD[Array[Byte]]] = {
    val jrdd = if (rdd.isDefined) {
      JavaRDD.fromRDD(rdd.get)
    } else {
      null
    }
    val jrdd2 = if (rdd2.isDefined) {
      JavaRDD.fromRDD(rdd2.get)
    } else {
      null
    }
    val r = pfunc.call(jrdd, jrdd2, time.milliseconds)
    if (r != null) {
      Some(r.rdd)
    } else {
      None
    }
  }
}

private[python]
abstract class PythonDStream(parent: DStream[_]) extends DStream[Array[Byte]] (parent.ssc) {

  override def dependencies = List(parent)

  override def slideDuration: Duration = parent.slideDuration

  val asJavaDStream  = JavaDStream.fromDStream(this)
}

/**
 * Transformed DStream in Python.
 *
 * If the result RDD is PythonRDD, then it will cache it as an template for future use,
 * this can reduce the Python callbacks.
 */
private[spark] class PythonTransformedDStream (parent: DStream[_], pfunc: PythonRDDFunction,
                                var reuse: Boolean = false)
  extends PythonDStream(parent) {

  val func = new RDDFunction(pfunc)
  var lastResult: PythonRDD = _

  override def compute(validTime: Time): Option[RDD[Array[Byte]]] = {
    val rdd1 = parent.getOrCompute(validTime)
    if (rdd1.isEmpty) {
      return None
    }
    if (reuse && lastResult != null) {
      Some(lastResult.copyTo(rdd1.get))
    } else {
      val r = func(rdd1, validTime)
      if (reuse && r.isDefined && lastResult == null) {
        r.get match {
          case rdd: PythonRDD =>
            if (rdd.parent(0) == rdd1) {
              // only one PythonRDD
              lastResult = rdd
            } else {
              // may have multiple stages
              reuse = false
            }
        }
      }
      r
    }
  }
}

/**
 * Transformed from two DStreams in Python.
 */
private[spark]
class PythonTransformed2DStream(parent: DStream[_], parent2: DStream[_],
                                pfunc: PythonRDDFunction2)
  extends DStream[Array[Byte]] (parent.ssc) {

  val func = new RDDFunction2(pfunc)

  override def slideDuration: Duration = parent.slideDuration

  override def dependencies = List(parent, parent2)

  override def compute(validTime: Time): Option[RDD[Array[Byte]]] = {
    func(parent.getOrCompute(validTime), parent2.getOrCompute(validTime), validTime)
  }

  val asJavaDStream  = JavaDStream.fromDStream(this)
}

/**
 * similar to StateDStream
 */
private[spark]
class PythonStateDStream(parent: DStream[Array[Byte]], preduceFunc: PythonRDDFunction2)
  extends PythonDStream(parent) {

  val reduceFunc = new RDDFunction2(preduceFunc)

  super.persist(StorageLevel.MEMORY_ONLY)
  override val mustCheckpoint = true

  override def compute(validTime: Time): Option[RDD[Array[Byte]]] = {
    val lastState = getOrCompute(validTime - slideDuration)
    val rdd = parent.getOrCompute(validTime)
    if (rdd.isDefined) {
      reduceFunc(lastState, rdd, validTime)
    } else {
      lastState
    }
  }
}

/**
 * Copied from ReducedWindowedDStream
 */
private[spark]
class PythonReducedWindowedDStream(parent: DStream[Array[Byte]],
                                   preduceFunc: PythonRDDFunction2,
                                   pinvReduceFunc: PythonRDDFunction2,
                                   _windowDuration: Duration,
                                   _slideDuration: Duration
                                   ) extends PythonStateDStream(parent, preduceFunc) {

  assert(_windowDuration.isMultipleOf(parent.slideDuration),
    "The window duration of ReducedWindowedDStream (" + _windowDuration + ") " +
      "must be multiple of the slide duration of parent DStream (" + parent.slideDuration + ")"
  )

  assert(_slideDuration.isMultipleOf(parent.slideDuration),
    "The slide duration of ReducedWindowedDStream (" + _slideDuration + ") " +
      "must be multiple of the slide duration of parent DStream (" + parent.slideDuration + ")"
  )

  val invReduceFunc = new RDDFunction2(pinvReduceFunc)

  def windowDuration: Duration = _windowDuration
  override def slideDuration: Duration = _slideDuration
  override def parentRememberDuration: Duration = rememberDuration + windowDuration

  override def compute(validTime: Time): Option[RDD[Array[Byte]]] = {
    val currentTime = validTime
    val currentWindow = new Interval(currentTime - windowDuration + parent.slideDuration,
      currentTime)
    val previousWindow = currentWindow - slideDuration

    logDebug("Window time = " + windowDuration)
    logDebug("Slide time = " + slideDuration)
    logDebug("ZeroTime = " + zeroTime)
    logDebug("Current window = " + currentWindow)
    logDebug("Previous window = " + previousWindow)

    //  _____________________________
    // |  previous window   _________|___________________
    // |___________________|       current window        |  --------------> Time
    //                     |_____________________________|
    //
    // |________ _________|          |________ _________|
    //          |                             |
    //          V                             V
    //       old RDDs                     new RDDs
    //

    // Get the RDD of the reduced value of the previous window
    val previousWindowRDD = getOrCompute(previousWindow.endTime)

    if (windowDuration > slideDuration * 5 && previousWindowRDD.isDefined) {
      // subtle the values from old RDDs
      val oldRDDs =
        parent.slice(previousWindow.beginTime, currentWindow.beginTime - parent.slideDuration)
      val subbed = if (oldRDDs.size > 0) {
        invReduceFunc(previousWindowRDD, Some(ssc.sc.union(oldRDDs)), validTime)
      } else {
        previousWindowRDD
      }

      // add the RDDs of the reduced values in "new time steps"
      val newRDDs =
        parent.slice(previousWindow.endTime, currentWindow.endTime - parent.slideDuration)

      if (newRDDs.size > 0) {
        reduceFunc(subbed, Some(ssc.sc.union(newRDDs)), validTime)
      } else {
        subbed
      }
    } else {
      // Get the RDDs of the reduced values in current window
      val currentRDDs =
        parent.slice(currentWindow.beginTime, currentWindow.endTime - parent.slideDuration)
      if (currentRDDs.size > 0) {
        reduceFunc(None, Some(ssc.sc.union(currentRDDs)), validTime)
      } else {
        None
      }
    }
  }
}

/**
 * This is used for foreachRDD() in Python
 */
class PythonForeachDStream(
    prev: DStream[Array[Byte]],
    foreachFunction: PythonRDDFunction
  ) extends ForEachDStream[Array[Byte]](
    prev,
    (rdd: RDD[Array[Byte]], time: Time) => {
      if (rdd != null) {
        foreachFunction.call(rdd, time.milliseconds)
      }
    }
  ) {

  this.register()
}


/**
 * similar to QueueInputStream
 */

class PythonDataInputStream(
    ssc_ : JavaStreamingContext,
    inputRDDs: JArrayList[JavaRDD[Array[Byte]]],
    oneAtAtime: Boolean,
    defaultRDD: JavaRDD[Array[Byte]]
  ) extends InputDStream[Array[Byte]](JavaStreamingContext.toStreamingContext(ssc_)) {

  val emptyRDD = if (defaultRDD != null) {
    Some(defaultRDD.rdd)
  } else {
    None // ssc.sparkContext.emptyRDD[Array[Byte]]
  }

  def start() {}

  def stop() {}

  def compute(validTime: Time): Option[RDD[Array[Byte]]] = {
    val index = ((validTime - zeroTime) / slideDuration - 1).toInt
    if (oneAtAtime) {
      if (index == 0) {
        val rdds = inputRDDs.toArray.map(_.asInstanceOf[JavaRDD[Array[Byte]]].rdd).toSeq
        Some(ssc.sparkContext.union(rdds))
      } else {
        emptyRDD
      }
    } else {
      if (index < inputRDDs.size()) {
        Some(inputRDDs.get(index).rdd)
      } else {
        emptyRDD
      }
    }
  }

  val asJavaDStream  = JavaDStream.fromDStream(this)
}
