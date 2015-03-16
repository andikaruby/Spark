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
package org.apache.spark.status.api.v1

import java.util.Date
import javax.ws.rs._
import javax.ws.rs.core.MediaType

import org.apache.spark.JobExecutionStatus
import org.apache.spark.ui.jobs.JobProgressListener
import org.apache.spark.ui.jobs.UIData.JobUIData

@Produces(Array(MediaType.APPLICATION_JSON))
class AllJobsResource(uiRoot: UIRoot) {

  @GET
  def jobsList(
    @PathParam("appId") appId: String,
    @QueryParam("status") statuses: java.util.List[JobExecutionStatus]
  ): Seq[JobData] = {
    uiRoot.withSparkUI(appId) { ui =>
      val statusToJobs = ui.jobProgressListener.synchronized {
        Seq(
          JobExecutionStatus.RUNNING -> ui.jobProgressListener.activeJobs.values.toSeq,
          JobExecutionStatus.SUCCEEDED -> ui.jobProgressListener.completedJobs.toSeq,
          JobExecutionStatus.FAILED -> ui.jobProgressListener.failedJobs.reverse.toSeq
        )
      }
      val adjStatuses: java.util.List[JobExecutionStatus] = {
        if (statuses.isEmpty) {
          java.util.Arrays.asList(JobExecutionStatus.values(): _*)
        }
        else {
          statuses
        }
      }
      for {
        (status, jobs) <- statusToJobs
        job <- jobs if adjStatuses.contains(status)
      } yield {
        AllJobsResource.convertJobData(job, ui.jobProgressListener, false)
      }
    }
  }

}

object AllJobsResource {
  def convertJobData(
    job: JobUIData,
    listener: JobProgressListener,
    includeStageDetails: Boolean
  ): JobData = {
    listener.synchronized {
      val lastStageInfo = listener.stageIdToInfo.get(job.stageIds.max)
      val lastStageData = lastStageInfo.flatMap { s =>
        listener.stageIdToData.get((s.stageId, s.attemptId))
      }
      val lastStageName = lastStageInfo.map { _.name }.getOrElse("(Unknown Stage Name)")
      val lastStageDescription = lastStageData.flatMap { _.description }
      JobData(
        jobId = job.jobId,
        name = lastStageName,
        description = lastStageDescription,
        submissionTime = job.submissionTime.map{new Date(_)},
        completionTime = job.completionTime.map{new Date(_)},
        stageIds = job.stageIds,
        jobGroup = job.jobGroup,
        status = job.status,
        numTasks = job.numTasks,
        numActiveTasks = job.numActiveTasks,
        numCompletedTasks = job.numCompletedTasks,
        numSkippedTasks = job.numCompletedTasks,
        numFailedTasks = job.numFailedTasks,
        numActiveStages = job.numActiveStages,
        numCompletedStages = job.completedStageIndices.size,
        numSkippedStages = job.numSkippedStages,
        numFailedStages = job.numFailedStages
      )
    }
  }
}
