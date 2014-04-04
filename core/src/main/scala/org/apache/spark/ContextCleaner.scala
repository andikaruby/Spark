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

import java.lang.ref.{ReferenceQueue, WeakReference}

import scala.collection.mutable.{ArrayBuffer, SynchronizedBuffer}

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

/**
 * Classes that represent cleaning tasks.
 */
private sealed trait CleanupTask
private case class CleanRDD(rddId: Int) extends CleanupTask
private case class CleanShuffle(shuffleId: Int) extends CleanupTask
private case class CleanBroadcast(broadcastId: Long) extends CleanupTask

/**
 * A WeakReference associated with a CleanupTask.
 *
 * When the referent object becomes only weakly reachable, the corresponding
 * CleanupTaskWeakReference is automatically added to the given reference queue.
 */
private class CleanupTaskWeakReference(
    val task: CleanupTask,
    referent: AnyRef,
    referenceQueue: ReferenceQueue[AnyRef])
  extends WeakReference(referent, referenceQueue)

/**
 * An asynchronous cleaner for RDD, shuffle, and broadcast state.
 *
 * This maintains a weak reference for each RDD, ShuffleDependency, and Broadcast of interest,
 * to be processed when the associated object goes out of scope of the application. Actual
 * cleanup is performed in a separate daemon thread.
 */
private[spark] class ContextCleaner(sc: SparkContext) extends Logging {

  private val referenceBuffer = new ArrayBuffer[CleanupTaskWeakReference]
    with SynchronizedBuffer[CleanupTaskWeakReference]

  private val referenceQueue = new ReferenceQueue[AnyRef]

  private val listeners = new ArrayBuffer[CleanerListener]
    with SynchronizedBuffer[CleanerListener]

  private val cleaningThread = new Thread() { override def run() { keepCleaning() }}

  @volatile private var stopped = false

  /** Attach a listener object to get information of when objects are cleaned. */
  def attachListener(listener: CleanerListener) {
    listeners += listener
  }

  /** Start the cleaner. */
  def start() {
    cleaningThread.setDaemon(true)
    cleaningThread.setName("ContextCleaner")
    cleaningThread.start()
  }

  /** Stop the cleaner. */
  def stop() {
    stopped = true
  }

  /** Register a RDD for cleanup when it is garbage collected. */
  def registerRDDForCleanup(rdd: RDD[_]) {
    registerForCleanup(rdd, CleanRDD(rdd.id))
  }

  /** Register a ShuffleDependency for cleanup when it is garbage collected. */
  def registerShuffleForCleanup(shuffleDependency: ShuffleDependency[_, _]) {
    registerForCleanup(shuffleDependency, CleanShuffle(shuffleDependency.shuffleId))
  }

  /** Register a Broadcast for cleanup when it is garbage collected. */
  def registerBroadcastForCleanup[T](broadcast: Broadcast[T]) {
    registerForCleanup(broadcast, CleanBroadcast(broadcast.id))
  }

  /** Register an object for cleanup. */
  private def registerForCleanup(objectForCleanup: AnyRef, task: CleanupTask) {
    referenceBuffer += new CleanupTaskWeakReference(task, objectForCleanup, referenceQueue)
  }

  /** Keep cleaning RDD, shuffle, and broadcast state. */
  private def keepCleaning() {
    while (!stopped) {
      try {
        val reference = Option(referenceQueue.remove(ContextCleaner.REF_QUEUE_POLL_TIMEOUT))
          .map(_.asInstanceOf[CleanupTaskWeakReference])
        reference.map(_.task).foreach { task =>
          logDebug("Got cleaning task " + task)
          referenceBuffer -= reference.get
          task match {
            case CleanRDD(rddId) => doCleanupRDD(rddId)
            case CleanShuffle(shuffleId) => doCleanupShuffle(shuffleId)
            case CleanBroadcast(broadcastId) => doCleanupBroadcast(broadcastId)
          }
        }
      } catch {
        case t: Throwable => logError("Error in cleaning thread", t)
      }
    }
  }

  /** Perform RDD cleanup. */
  private def doCleanupRDD(rddId: Int) {
    try {
      logDebug("Cleaning RDD " + rddId)
      sc.unpersistRDD(rddId, blocking = false)
      listeners.foreach(_.rddCleaned(rddId))
      logInfo("Cleaned RDD " + rddId)
    } catch {
      case t: Throwable => logError("Error cleaning RDD " + rddId, t)
    }
  }

  /** Perform shuffle cleanup. */
  private def doCleanupShuffle(shuffleId: Int) {
    try {
      logDebug("Cleaning shuffle " + shuffleId)
      mapOutputTrackerMaster.unregisterShuffle(shuffleId)
      blockManagerMaster.removeShuffle(shuffleId)
      listeners.foreach(_.shuffleCleaned(shuffleId))
      logInfo("Cleaned shuffle " + shuffleId)
    } catch {
      case t: Throwable => logError("Error cleaning shuffle " + shuffleId, t)
    }
  }

  /** Perform broadcast cleanup. */
  private def doCleanupBroadcast(broadcastId: Long) {
    try {
      logDebug("Cleaning broadcast " + broadcastId)
      broadcastManager.unbroadcast(broadcastId, removeFromDriver = true)
      listeners.foreach(_.broadcastCleaned(broadcastId))
      logInfo("Cleaned broadcast " + broadcastId)
    } catch {
      case t: Throwable => logError("Error cleaning broadcast " + broadcastId, t)
    }
  }

  private def blockManagerMaster = sc.env.blockManager.master
  private def broadcastManager = sc.env.broadcastManager
  private def mapOutputTrackerMaster = sc.env.mapOutputTracker.asInstanceOf[MapOutputTrackerMaster]

  // Used for testing

  def cleanupRDD(rdd: RDD[_]) {
    doCleanupRDD(rdd.id)
  }

  def cleanupShuffle(shuffleDependency: ShuffleDependency[_, _]) {
    doCleanupShuffle(shuffleDependency.shuffleId)
  }

  def cleanupBroadcast[T](broadcast: Broadcast[T]) {
    doCleanupBroadcast(broadcast.id)
  }
}

private object ContextCleaner {
  private val REF_QUEUE_POLL_TIMEOUT = 100
}

/**
 * Listener class used for testing when any item has been cleaned by the Cleaner class.
 */
private[spark] trait CleanerListener {
  def rddCleaned(rddId: Int)
  def shuffleCleaned(shuffleId: Int)
  def broadcastCleaned(broadcastId: Long)
}
