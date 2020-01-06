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

package org.apache.spark.deploy.history

import scala.collection.mutable

import org.apache.spark.deploy.history.EventFilter.FilterStatistics
import org.apache.spark.internal.Logging
import org.apache.spark.scheduler._

/**
 * This class tracks both live jobs and live executors, and pass the list to the
 * [[BasicEventFilter]] to help BasicEventFilter to reject finished jobs (+ stages/tasks/RDDs)
 * and dead executors.
 */
private[spark] class BasicEventFilterBuilder extends SparkListener with EventFilterBuilder {
  private val _liveJobToStages = new mutable.HashMap[Int, Set[Int]]
  private val _stageToTasks = new mutable.HashMap[Int, mutable.Set[Long]]
  private val _stageToRDDs = new mutable.HashMap[Int, Set[Int]]
  private val _liveExecutors = new mutable.HashSet[String]

  private var totalJobs: Long = 0L
  private var totalStages: Long = 0L
  private var totalTasks: Long = 0L

  def liveJobs: Set[Int] = _liveJobToStages.keySet.toSet
  def liveStages: Set[Int] = _stageToRDDs.keySet.toSet
  def liveTasks: Set[Long] = _stageToTasks.values.flatten.toSet
  def liveRDDs: Set[Int] = _stageToRDDs.values.flatten.toSet
  def liveExecutors: Set[String] = _liveExecutors.toSet

  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    totalJobs += 1
    jobStart.stageIds.foreach { stageId =>
      if (_stageToRDDs.get(stageId).isEmpty) {
        // stage submit event is not received yet
        totalStages += 1
        _stageToRDDs.put(stageId, Set.empty[Int])
      }
    }
    _liveJobToStages += jobStart.jobId -> jobStart.stageIds.toSet
  }

  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = {
    val stages = _liveJobToStages.getOrElse(jobEnd.jobId, Seq.empty[Int])
    _liveJobToStages -= jobEnd.jobId
    // This might leave some stages and tasks if job end event comes earlier than stage submitted
    // or task start event; it's not accurate but safer than dropping wrong events which cannot be
    // restored.
    _stageToTasks --= stages
    _stageToRDDs --= stages
  }

  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit = {
    val stageId = stageSubmitted.stageInfo.stageId
    if (_stageToRDDs.get(stageId).isEmpty) {
      // job start event is not received yet
      totalStages += 1
    }
    _stageToRDDs.put(stageId, stageSubmitted.stageInfo.rddInfos.map(_.id).toSet)
  }

  override def onTaskStart(taskStart: SparkListenerTaskStart): Unit = {
    totalTasks += 1
    val curTasks = _stageToTasks.getOrElseUpdate(taskStart.stageId,
      mutable.HashSet[Long]())
    curTasks += taskStart.taskInfo.taskId
  }

  override def onExecutorAdded(executorAdded: SparkListenerExecutorAdded): Unit = {
    _liveExecutors += executorAdded.executorId
  }

  override def onExecutorRemoved(executorRemoved: SparkListenerExecutorRemoved): Unit = {
    _liveExecutors -= executorRemoved.executorId
  }

  override def createFilter(): EventFilter = {
    val stats = FilterStatistics(totalJobs, liveJobs.size, totalStages,
      liveStages.size, totalTasks, liveTasks.size)

    new BasicEventFilter(stats, liveJobs, liveStages, liveTasks, liveRDDs, liveExecutors)
  }
}

/**
 * This class provides the functionality to reject events which are related to the finished
 * jobs based on the given information. This class only deals with job related events, and provides
 * a PartialFunction which returns false for rejected events for finished jobs, returns true
 * otherwise.
 */
private[spark] abstract class JobEventFilter(
    stats: Option[FilterStatistics],
    liveJobs: Set[Int],
    liveStages: Set[Int],
    liveTasks: Set[Long],
    liveRDDs: Set[Int]) extends EventFilter with Logging {

  logDebug(s"jobs : $liveJobs")
  logDebug(s"stages : $liveStages")
  logDebug(s"tasks : $liveTasks")
  logDebug(s"RDDs : $liveRDDs")

  override def statistics(): Option[FilterStatistics] = stats

  protected val acceptFnForJobEvents: PartialFunction[SparkListenerEvent, Boolean] = {
    case e: SparkListenerStageCompleted =>
      liveStages.contains(e.stageInfo.stageId)
    case e: SparkListenerStageSubmitted =>
      liveStages.contains(e.stageInfo.stageId)
    case e: SparkListenerTaskStart =>
      liveTasks.contains(e.taskInfo.taskId)
    case e: SparkListenerTaskGettingResult =>
      liveTasks.contains(e.taskInfo.taskId)
    case e: SparkListenerTaskEnd =>
      liveTasks.contains(e.taskInfo.taskId)
    case e: SparkListenerJobStart =>
      liveJobs.contains(e.jobId)
    case e: SparkListenerJobEnd =>
      liveJobs.contains(e.jobId)
    case e: SparkListenerUnpersistRDD =>
      liveRDDs.contains(e.rddId)
    case e: SparkListenerExecutorMetricsUpdate =>
      e.accumUpdates.exists { case (taskId, stageId, _, _) =>
        liveTasks.contains(taskId) || liveStages.contains(stageId)
      }
    case e: SparkListenerSpeculativeTaskSubmitted =>
      liveStages.contains(e.stageId)
  }
}

/**
 * This class rejects events which are related to the finished jobs or dead executors,
 * based on the given information. The events which are not related to the job and executor
 * will be considered as "Don't mind".
 */
private[spark] class BasicEventFilter(
    _stats: FilterStatistics,
    _liveJobs: Set[Int],
    _liveStages: Set[Int],
    _liveTasks: Set[Long],
    _liveRDDs: Set[Int],
    liveExecutors: Set[String])
  extends JobEventFilter(
    Some(_stats),
    _liveJobs,
    _liveStages,
    _liveTasks,
    _liveRDDs) with Logging {

  logDebug(s"live executors : $liveExecutors")

  private val _acceptFn: PartialFunction[SparkListenerEvent, Boolean] = {
    case e: SparkListenerExecutorAdded => liveExecutors.contains(e.executorId)
    case e: SparkListenerExecutorRemoved => liveExecutors.contains(e.executorId)
    case e: SparkListenerExecutorBlacklisted => liveExecutors.contains(e.executorId)
    case e: SparkListenerExecutorUnblacklisted => liveExecutors.contains(e.executorId)
    case e: SparkListenerStageExecutorMetrics => liveExecutors.contains(e.execId)
    case _: SparkListenerBlockManagerAdded => true
    case _: SparkListenerBlockManagerRemoved => true
  }

  override def acceptFn(): PartialFunction[SparkListenerEvent, Boolean] = {
    _acceptFn.orElse(acceptFnForJobEvents)
  }
}
