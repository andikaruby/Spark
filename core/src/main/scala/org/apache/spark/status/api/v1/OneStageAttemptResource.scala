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

import javax.ws.rs.core.MediaType
import javax.ws.rs.{GET, PathParam, Produces}

import org.apache.spark.SparkException

@Produces(Array(MediaType.APPLICATION_JSON))
private[v1] class OneStageAttemptResource(uiRoot: UIRoot) {

  @GET
  def stageData(
    @PathParam("appId") appId: String,
    @PathParam("stageId") stageId: Int,
    @PathParam("attemptId") attemptId: Int
  ): StageData = {
    uiRoot.withSparkUI(appId) { ui =>
      val listener = ui.stagesTab.listener
      val stageAndStatus = AllStagesResource.stagesAndStatus(ui)
      val oneStage = stageAndStatus.flatMap { case (status, stages) =>
        val matched = stages.find { stage =>
          stage.stageId == stageId && stage.attemptId == attemptId
        }
        matched.map { status -> _ }
      }.headOption
      oneStage match {
        case Some((status, stageInfo)) =>
          val stageUiData = listener.synchronized {
            listener.stageIdToData.get((stageInfo.stageId, stageInfo.attemptId)).
              getOrElse(throw new SparkException("failed to get full stage data for stage: " +
              stageInfo.stageId + ":" + stageInfo.attemptId)
              )
          }
          AllStagesResource.stageUiToStageData(status, stageInfo, stageUiData,
            includeDetails = true)
        case None =>
          val stageAttempts = stageAndStatus.flatMap{ case (status, stages) =>
            stages.filter { _.stageId == stageId }.map{_.attemptId}
          }
          if (stageAttempts.isEmpty) {
            throw new NotFoundException(s"unknown stage: $stageId")
          } else {
            throw new NotFoundException(s"unknown attempt for stage $stageId.  " +
              s"Found attempts: ${stageAttempts.mkString("[", ",", "]")}")
          }
      }

    }
  }
}
