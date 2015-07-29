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

package org.apache.spark.scheduler

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.util.CallSite

import scala.collection.mutable.HashSet

/**
 * A stage is a set of independent tasks all computing the same function that need to run as part
 * of a Spark job, where all the tasks have the same shuffle dependencies. Each DAG of tasks run
 * by the scheduler is split up into stages at the boundaries where shuffle occurs, and then the
 * DAGScheduler runs these stages in topological order.
 *
 * Each Stage can either be a shuffle map stage, in which case its tasks' results are input for
 * another stage, or a result stage, in which case its tasks directly compute the action that
 * initiated a job (e.g. count(), save(), etc). For shuffle map stages, we also track the nodes
 * that each output partition is on.
 *
 * Each Stage also has a firstJobId, identifying the job that first submitted the stage.  When FIFO
 * scheduling is used, this allows Stages from earlier jobs to be computed first or recovered
 * faster on failure.
 *
 * The callSite provides a location in user code which relates to the stage. For a shuffle map
 * stage, the callSite gives the user code that created the RDD being shuffled. For a result
 * stage, the callSite gives the user code that executes the associated action (e.g. count()).
 *
 * A single stage can consist of multiple attempts. In that case, the latestInfo field will
 * be updated for each attempt.
 *
 */
private[scheduler] abstract class Stage(
    val id: Int,
    val rdd: RDD[_],
    val numTasks: Int,
    val parents: List[Stage],
    val firstJobId: Int,
    val callSite: CallSite)
  extends Logging {

  val numPartitions = rdd.partitions.size

  /** Set of jobs that this stage belongs to. */
  val jobIds = new HashSet[Int]

  var pendingTasks = new HashSet[Task[_]]

  /** The ID to use for the next new attempt for this stage. */
  private var nextAttemptId: Int = 0

  val name = callSite.shortForm
  val details = callSite.longForm

  /**
   * Pointer to the [StageInfo] object for the most recent attempt. This needs to be initialized
   * here, before any attempts have actually been created, because the DAGScheduler uses this
   * StageInfo to tell SparkListeners when a job starts (which happens before any stage attempts
   * have been created).
   */
  private var _latestInfo: StageInfo = StageInfo.fromStage(this, nextAttemptId)

  /**
   * Spark is resilient to executors dying by retrying stages on FetchFailures. Here, we keep track
   * of unique stage failures (per stage attempt) triggered by fetch failures to prevent endless
   * stage retries. Specifically, per stage we wish to only record a failure when the following
   * holds:
   *
   * A) A fetch failure was observed
   * B) A failure has not yet been registered for this stage attempt. There may be multiple
   * concurrent failures for a sinlge stage since we may have multiple tasks executing at the same
   * time, one or many of which may fail. Also, even though there may only be one non-zombie stage
   * attemp, zombie stages may still have running tasks.
   */
  private val attemptsFailedFromFetch = new HashSet[Int]

  private[scheduler] def clearFailures() : Unit = {
    attemptsFailedFromFetch.clear()
  }

  /**
   * Check whether we should abort the failedStage due to multiple consecutive fetch failures.
   * 
   * This method updates the running set of failed stage attempts and returns
   * true if the number of failures exceeds the allowable number of failures.
   */
  private[scheduler] def failedOnFetchAndShouldAbort(stageAttemptId: Int): Boolean = {
    attemptsFailedFromFetch.add(stageAttemptId)
    attemptsFailedFromFetch.size >= Stage.MAX_STAGE_FAILURES
  }

  /** Creates a new attempt for this stage by creating a new StageInfo with a new attempt ID. */
  def makeNewStageAttempt(numPartitionsToCompute: Int): Unit = {
    _latestInfo = StageInfo.fromStage(this, nextAttemptId, Some(numPartitionsToCompute))
    nextAttemptId += 1
  }

  /** Returns the StageInfo for the most recent attempt for this stage. */
  def latestInfo: StageInfo = _latestInfo

  override final def hashCode(): Int = id
  override final def equals(other: Any): Boolean = other match {
    case stage: Stage => stage != null && stage.id == id
    case _ => false
  }
}

private[spark] object Stage {
  // The maximum number of times to retry a stage before aborting
  private[scheduler] val MAX_STAGE_FAILURES = 4
}
