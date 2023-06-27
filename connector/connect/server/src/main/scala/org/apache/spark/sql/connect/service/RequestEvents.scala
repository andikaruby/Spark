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

package org.apache.spark.sql.connect.service

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.protobuf.Message

import org.apache.spark.connect.proto
import org.apache.spark.scheduler.{LiveListenerBus, SparkListenerEvent}
import org.apache.spark.sql.catalyst.{QueryPlanningTracker, QueryPlanningTrackerCallback}
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan}
import org.apache.spark.sql.connect.common.ProtoUtils
import org.apache.spark.util.{Clock, Utils}

object RequestEvents {
  // TODO: Make this configurable
  val MAX_STATEMENT_TEXT_SIZE = 65535
}

/**
 * Post request Connect events to @link org.apache.spark.scheduler.LiveListenerBus.
 *
 * @param planHolder:
 *   Request for which the events are generated.
 * @param clock:
 *   Source of time for unit tests.
 */
case class RequestEvents(planHolder: ExecutePlanHolder, clock: Clock)
  extends QueryPlanningTrackerCallback {

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationStarted.
   */
  def postStarted(): Unit = {
    val request = planHolder.request
    val plan: Message =
      request.getPlan.getOpTypeCase match {
        case proto.Plan.OpTypeCase.COMMAND => request.getPlan.getCommand
        case proto.Plan.OpTypeCase.ROOT => request.getPlan.getRoot
        case _ =>
          throw new UnsupportedOperationException(
            s"${request.getPlan.getOpTypeCase} not supported.")
      }

    listenerBus.post(
      SparkListenerConnectOperationStarted(
        jobTag,
        operationId,
        clock.getTimeMillis(),
        request.getSessionId,
        request.getUserContext.getUserId,
        request.getUserContext.getUserName,
        Utils.redact(
          sessionHolder.session.sessionState.conf.stringRedactionPattern,
          ProtoUtils.abbreviate(plan, RequestEvents.MAX_STATEMENT_TEXT_SIZE).toString),
        request.getClientType))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationAnalyzed.
   *
   * @param analyzedPlan
   *   The analyzed plan generated by the Connect request plan. None when the request does not
   *   generate a plan.
   */
  def postAnalyzed(analyzedPlan: Option[LogicalPlan] = None): Unit = {
    val event =
      SparkListenerConnectOperationAnalyzed(jobTag, operationId, clock.getTimeMillis())
    event.analyzedPlan = analyzedPlan
    listenerBus.post(event)
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationReadyForExecution.
   *
   */
  def postReadyForExecution(): Unit = {
    listenerBus.post(SparkListenerConnectOperationReadyForExecution(
      jobTag,
      operationId,
      clock.getTimeMillis()))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationCanceled.
   */
  def postCanceled(): Unit = {
    listenerBus
      .post(SparkListenerConnectOperationCanceled(jobTag, operationId, clock.getTimeMillis()))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationFailed.
   *
   * @param errorMessage
   *   The message of the error thrown during the request.
   */
  def postFailed(errorMessage: String): Unit = {
    listenerBus.post(
      SparkListenerConnectOperationFailed(
        jobTag,
        operationId,
        clock.getTimeMillis(),
        errorMessage))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationAnalyzed &
   * @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationReadyForExecution
   *
   * @param analyzedPlan
   *   The analyzed plan generated by the Connect request plan. None when the request does not
   *   generate a plan.
   */
  def postAnalyzedAndReadyForExecution(analyzedPlan: Option[LogicalPlan] = None): Unit = {
    postAnalyzed(analyzedPlan)
    postReadyForExecution()
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationAnalyzed,
   * @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationReadyForExecution &
   * @link
   *   org.apache.spark.sql.connect.service.SparkListenerConnectOperationFinished.
   *
   * @param analyzedPlan
   *   The analyzed plan generated by the Connect request plan. None when the request does not
   *   generate a plan.
   */
  def postAnalyzedAndFinished(analyzedPlan: Option[LogicalPlan] = None): Unit = {
    postAnalyzed(analyzedPlan)
    postReadyForExecution()
    postFinished()
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationFinished.
   */
  def postFinished(): Unit = {
    listenerBus
      .post(SparkListenerConnectOperationFinished(jobTag, operationId, clock.getTimeMillis()))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationClosed.
   */
  def postClosed(): Unit = {
    listenerBus
      .post(SparkListenerConnectOperationClosed(jobTag, operationId, clock.getTimeMillis()))
  }

  def analyzed(tracker: QueryPlanningTracker, analyzedPlan: LogicalPlan): Unit = {
    postAnalyzed(Some(analyzedPlan))
  }

  def readyForExecution(tracker: QueryPlanningTracker): Unit = postReadyForExecution

  private def operationId(): String = {
    planHolder.operationId
  }

  private def jobTag(): String = {
    planHolder.jobTag
  }

  private def listenerBus(): LiveListenerBus = {
    sessionHolder.session.sparkContext.listenerBus
  }

  private def sessionHolder(): SessionHolder = {
    planHolder.sessionHolder
  }
}

/**
 * Event sent after reception of a Connect request (i.e. not queued),
 * but prior any analysis or execution.
 *
 * @param jobTag:
 *   Opaque Spark jobTag (@link org.apache.spark.SparkContext.setJobGroup) assigned by Connect
 *   during a request. Designed to be unique across sessions and requests.
 * @param operationId:
 *   36 characters UUID assigned by Connect during a request.
 * @param eventTime:
 *   The time in ms when the event was generated.
 * @param sessionId:
 *   ID assigned by the client or Connect the operation was executed on.
 * @param userId:
 *   Opaque userId set in the Connect request.
 * @param userName:
 *   Opaque userName set in the Connect request.
 * @param statementText:
 *   The connect request plan converted to text.
 * @param clientType:
 *   The clientType set in the Connect request.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationStarted(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    sessionId: String,
    userId: String,
    userName: String,
    statementText: String,
    clientType: String,
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent

/**
 * The event is sent after a Connect request has been analyzed
 * (@link org.apache.spark.sql.catalyst.QueryPlanningTracker.ANALYSIS).
 *
 * @param jobTag:
 *   Opaque Spark jobTag (@link org.apache.spark.SparkContext.addJobTag) assigned by Connect
 *   during a request. Designed to be unique across sessions and requests.
 * @param operationId:
 *   36 characters UUID assigned by Connect during a request.
 * @param eventTime:
 *   The time in ms when the event was generated.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationAnalyzed(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent {

  /**
   * Analyzed Spark plan generated by the Connect request. None when the Connect request does not
   * generate a Spark plan.
   */
  @JsonIgnore var analyzedPlan: Option[LogicalPlan] = None
}

/**
 * The event is sent after a Connect request is ready for execution. For eager
 * commands this is after @link
 * org.apache.spark.sql.catalyst.QueryPlanningTracker.ANALYSIS. For other requests it is after
 * \@link org.apache.spark.sql.catalyst.QueryPlanningTracker.PLANNING
 *
 * @param jobTag:
 *   Opaque Spark jobTag (@link org.apache.spark.SparkContext.addJobTag) assigned by Connect
 *   during a request. Designed to be unique across sessions and requests.
 * @param operationId:
 *   36 characters UUID assigned by Connect during a request.
 * @param eventTime:
 *   The time in ms when the event was generated.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationReadyForExecution(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent

/**
 * Event sent after a Connect request has been canceled.
 *
 * @param jobTag:
 *   Opaque Spark jobTag (@link org.apache.spark.SparkContext.addJobTag) assigned by Connect
 *   during a request. Designed to be unique across sessions and requests.
 * @param operationId:
 *   36 characters UUID assigned by Connect during a request.
 * @param eventTime:
 *   The time in ms when the event was generated.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationCanceled(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent

/**
 * Event sent after a Connect request has failed.
 *
 * @param jobTag:
 *   Opaque Spark jobTag (@link org.apache.spark.SparkContext.addJobTag) assigned by Connect
 *   during a request. Designed to be unique across sessions and requests.
 * @param operationId:
 *   36 characters UUID assigned by Connect during a request.
 * @param eventTime:
 *   The time in ms when the event was generated.
 * @param errorMessage:
 *   The message of the error thrown during the request.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationFailed(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    errorMessage: String,
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent

/**
 * Event sent after a Connect request has finished executing, but prior results have been sent to
 * client.
 *
 * @param jobTag:
 *   Opaque Spark jobTag (@link org.apache.spark.SparkContext.addJobTag) assigned by Connect
 *   during a request. Designed to be unique across sessions and requests.
 * @param operationId:
 *   36 characters UUID assigned by Connect during a request.
 * @param eventTime:
 *   The time in ms when the event was generated.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationFinished(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent

/**
 * Event sent after a Connect request has finished executing and results have been sent to client.
 *
 * @param jobTag:
 *   Opaque Spark jobTag (@link org.apache.spark.SparkContext.addJobTag) assigned by Connect
 *   during a request. Designed to be unique across sessions and requests.
 * @param operationId:
 *   36 characters UUID assigned by Connect during a request.
 * @param eventTime:
 *   The time in ms when the event was generated.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationClosed(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent
