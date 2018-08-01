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

import java.util.{Timer, TimerTask}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable.ArrayBuffer

import org.apache.spark.internal.Logging
import org.apache.spark.rpc.{RpcCallContext, RpcEnv, ThreadSafeRpcEndpoint}
import org.apache.spark.scheduler.{LiveListenerBus, SparkListener, SparkListenerStageCompleted}

/**
 * A coordinator that handles all global sync requests from BarrierTaskContext. Each global sync
 * request is generated by `BarrierTaskContext.barrier()`, and identified by
 * stageId + stageAttemptId + barrierEpoch. Reply all the blocking global sync requests upon
 * received all the requests for a group of `barrier()` calls. If the coordinator doesn't collect
 * enough global sync requests within a configured time, fail all the requests due to timeout.
 */
private[spark] class BarrierCoordinator(
    timeout: Int,
    listenerBus: LiveListenerBus,
    override val rpcEnv: RpcEnv) extends ThreadSafeRpcEndpoint with Logging {

  private val timer = new Timer("BarrierCoordinator barrier epoch increment timer")

  private val listener = new SparkListener {
    override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = {
      val stageInfo = stageCompleted.stageInfo
      // Remove internal data from a finished stage attempt.
      cleanupSyncRequests(stageInfo.stageId, stageInfo.attemptNumber)
      barrierEpochByStageIdAndAttempt.remove((stageInfo.stageId, stageInfo.attemptNumber))
    }
  }

  // Epoch counter for each barrier (stage, attempt).
  private val barrierEpochByStageIdAndAttempt = new ConcurrentHashMap[(Int, Int), AtomicInteger]

  // Remember all the blocking global sync requests for each barrier (stage, attempt).
  private val syncRequestsByStageIdAndAttempt =
    new ConcurrentHashMap[(Int, Int), ArrayBuffer[RpcCallContext]]

  override def onStart(): Unit = {
    super.onStart()
    listenerBus.addToStatusQueue(listener)
  }

  /**
   * Get the array of [[RpcCallContext]]s that correspond to a barrier sync request from a stage
   * attempt.
   */
  private def getOrInitSyncRequests(
      stageId: Int,
      stageAttemptId: Int,
      numTasks: Int = 0): ArrayBuffer[RpcCallContext] = {
    val requests = syncRequestsByStageIdAndAttempt.putIfAbsent((stageId, stageAttemptId),
      new ArrayBuffer[RpcCallContext](numTasks))
    if (requests == null) {
      syncRequestsByStageIdAndAttempt.get((stageId, stageAttemptId))
    } else {
      requests
    }
  }

  /**
   * Clean up the array of [[RpcCallContext]]s that correspond to a barrier sync request from a
   * stage attempt.
   */
  private def cleanupSyncRequests(stageId: Int, stageAttemptId: Int): Unit = {
    val requests = syncRequestsByStageIdAndAttempt.remove((stageId, stageAttemptId))
    if (requests != null) {
      requests.clear()
    }
    logInfo(s"Removed all the pending barrier sync requests from Stage $stageId (Attempt " +
      s"$stageAttemptId).")
  }

  /**
   * Get the barrier epoch that correspond to a barrier sync request from a stage attempt.
   */
  private def getOrInitBarrierEpoch(stageId: Int, stageAttemptId: Int): AtomicInteger = {
    val barrierEpoch = barrierEpochByStageIdAndAttempt.putIfAbsent((stageId, stageAttemptId),
      new AtomicInteger(0))
    if (barrierEpoch == null) {
      barrierEpochByStageIdAndAttempt.get((stageId, stageAttemptId))
    } else {
      barrierEpoch
    }
  }

  /**
   * Send failure to all the blocking barrier sync requests from a stage attempt with proper
   * failure message.
   */
  private def failAllSyncRequests(
      syncRequests: ArrayBuffer[RpcCallContext],
      message: String): Unit = {
    syncRequests.foreach(_.sendFailure(new SparkException(message)))
  }

  /**
   * Finish all the blocking barrier sync requests from a stage attempt successfully if we
   * have received all the sync requests.
   */
  private def maybeFinishAllSyncRequests(
      syncRequests: ArrayBuffer[RpcCallContext],
      numTasks: Int): Boolean = {
    if (syncRequests.size == numTasks) {
      syncRequests.foreach(_.reply(()))
      return true
    }

    false
  }


  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case RequestToSync(numTasks, stageId, stageAttemptId, taskAttemptId, barrierEpoch) =>
      // Check the barrier epoch, to see which barrier() call we are processing.
      val currentBarrierEpoch = getOrInitBarrierEpoch(stageId, stageAttemptId)
      logInfo(s"Current barrier epoch for Stage $stageId (Attempt $stageAttemptId) is" +
        s"$currentBarrierEpoch.")
      if (currentBarrierEpoch.get() != barrierEpoch) {
        context.sendFailure(new SparkException(s"The request to sync of Stage $stageId (Attempt " +
          s"$stageAttemptId) with barrier epoch $barrierEpoch has already finished. Maybe task " +
          s"$taskAttemptId is not properly killed."))
      } else {
        val syncRequests = getOrInitSyncRequests(stageId, stageAttemptId)
        // If this is the first sync message received for a barrier() call, init a timer to ensure
        // we may timeout for the sync.
        if (syncRequests.isEmpty) {
          timer.schedule(new TimerTask {
            override def run(): Unit = {
              // Timeout for current barrier() call, fail all the sync requests.
              val requests = getOrInitSyncRequests(stageId, stageAttemptId)
              failAllSyncRequests(requests, "The coordinator didn't get all barrier sync " +
                s"requests for barrier epoch $barrierEpoch from Stage $stageId (Attempt " +
                s"$stageAttemptId) within ${timeout}s.")
              cleanupSyncRequests(stageId, stageAttemptId)
              // The global sync fails so the stage is expected to retry another attempt, all sync
              // messages come from current stage attempt shall fail.
              currentBarrierEpoch.set(-1)
            }
          }, timeout * 1000)
        }

        syncRequests += context
        logInfo(s"Barrier sync epoch $barrierEpoch from Stage $stageId (Attempt " +
          s"$stageAttemptId) received update from Task $taskAttemptId, current progress: " +
          s"${syncRequests.size}/$numTasks.")
        if (maybeFinishAllSyncRequests(syncRequests, numTasks)) {
          // Finished current barrier() call successfully, clean up internal data and increase the
          // barrier epoch.
          logInfo(s"Barrier sync epoch $barrierEpoch from Stage $stageId (Attempt " +
            s"$stageAttemptId) received all updates from tasks, finished successfully.")
          cleanupSyncRequests(stageId, stageAttemptId)
          currentBarrierEpoch.incrementAndGet()
        }
      }
  }

  override def onStop(): Unit = timer.cancel()
}

private[spark] sealed trait BarrierCoordinatorMessage extends Serializable

/**
 * A global sync request message from BarrierTaskContext, by `barrier()` call. Each request is
 * identified by stageId + stageAttemptId + barrierEpoch.
 *
 * @param numTasks The number of global sync requests the BarrierCoordinator shall receive
 * @param stageId ID of current stage
 * @param stageAttemptId ID of current stage attempt
 * @param taskAttemptId Unique ID of current task
 * @param barrierEpoch ID of the `barrier()` call, a task may consists multiple `barrier()` calls.
 */
private[spark] case class RequestToSync(
    numTasks: Int,
    stageId: Int,
    stageAttemptId: Int,
    taskAttemptId: Long,
    barrierEpoch: Int) extends BarrierCoordinatorMessage
