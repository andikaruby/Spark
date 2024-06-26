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
import org.sparkproject.connect.protobuf.Message

import org.apache.spark.connect.proto
import org.apache.spark.scheduler.SparkListenerEvent
import org.apache.spark.sql.catalyst.{QueryPlanningTracker, QueryPlanningTrackerCallback}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.connect.common.ProtoUtils
import org.apache.spark.util.{Clock, Utils}

object ExecuteEventsManager {
  // TODO: Make this configurable
  val MAX_STATEMENT_TEXT_SIZE = 65535
}

sealed abstract class ExecuteStatus(value: Int)

object ExecuteStatus {
  case object Pending extends ExecuteStatus(0)
  case object Started extends ExecuteStatus(1)
  case object Analyzed extends ExecuteStatus(2)
  case object ReadyForExecution extends ExecuteStatus(3)
  case object Finished extends ExecuteStatus(4)
  case object Failed extends ExecuteStatus(5)
  case object Canceled extends ExecuteStatus(6)
  case object Closed extends ExecuteStatus(7)
}

/**
 * Post request Connect events to @link org.apache.spark.scheduler.LiveListenerBus.
 *
 * @param executeHolder:
 *   Request for which the events are generated.
 * @param clock:
 *   Source of time for unit tests.
 */
case class ExecuteEventsManager(executeHolder: ExecuteHolder, clock: Clock) {

  private def operationId = executeHolder.operationId

  private def jobTag = executeHolder.jobTag

  private def sparkSessionTags = executeHolder.sparkSessionTags

  private def listenerBus = sessionHolder.session.sparkContext.listenerBus

  private def sessionHolder = executeHolder.sessionHolder

  private def sessionId = executeHolder.request.getSessionId

  private def sessionStatus = sessionHolder.eventManager.status

  private var _status: ExecuteStatus = ExecuteStatus.Pending

  private var error = Option.empty[Boolean]

  private var canceled = Option.empty[Boolean]

  private var producedRowCount = Option.empty[Long]

  /**
   * @return
   *   Last event posted by the Connect request
   */
  private[connect] def status: ExecuteStatus = _status

  /**
   * @return
   *   True when the Connect request has posted @link
   *   org.apache.spark.sql.connect.service.SparkListenerConnectOperationCanceled
   */
  private[connect] def hasCanceled: Option[Boolean] = canceled

  /**
   * @return
   *   True when the Connect request has posted @link
   *   org.apache.spark.sql.connect.service.SparkListenerConnectOperationFailed
   */
  private[connect] def hasError: Option[Boolean] = error

  /**
   * @return
   *   How many rows the Connect request has produced @link
   *   org.apache.spark.sql.connect.service.SparkListenerConnectOperationFinished
   */
  private[connect] def getProducedRowCount: Option[Long] = producedRowCount

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationStarted.
   */
  def postStarted(): Unit = {
    assertStatus(List(ExecuteStatus.Pending), ExecuteStatus.Started)
    val request = executeHolder.request
    val plan: Message =
      request.getPlan.getOpTypeCase match {
        case proto.Plan.OpTypeCase.COMMAND => request.getPlan.getCommand
        case proto.Plan.OpTypeCase.ROOT => request.getPlan.getRoot
        case _ =>
          throw new UnsupportedOperationException(
            s"${request.getPlan.getOpTypeCase} not supported.")
      }

    val event = SparkListenerConnectOperationStarted(
      jobTag,
      operationId,
      clock.getTimeMillis(),
      sessionId,
      request.getUserContext.getUserId,
      request.getUserContext.getUserName,
      Utils.redact(
        sessionHolder.session.sessionState.conf.stringRedactionPattern,
        ProtoUtils.abbreviate(plan, ExecuteEventsManager.MAX_STATEMENT_TEXT_SIZE).toString),
      sparkSessionTags)
    event.planRequest = Some(request)
    listenerBus.post(event)
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationAnalyzed.
   *
   * @param analyzedPlan
   *   The analyzed plan generated by the Connect request plan. None when the request does not
   *   generate a plan.
   */
  def postAnalyzed(analyzedPlan: Option[LogicalPlan] = None): Unit = {
    assertStatus(List(ExecuteStatus.Started, ExecuteStatus.Analyzed), ExecuteStatus.Analyzed)
    val event =
      SparkListenerConnectOperationAnalyzed(jobTag, operationId, clock.getTimeMillis())
    event.analyzedPlan = analyzedPlan
    listenerBus.post(event)
  }

  /**
   * Post @link
   * org.apache.spark.sql.connect.service.SparkListenerConnectOperationReadyForExecution.
   */
  def postReadyForExecution(): Unit = {
    assertStatus(List(ExecuteStatus.Analyzed), ExecuteStatus.ReadyForExecution)
    listenerBus.post(
      SparkListenerConnectOperationReadyForExecution(jobTag, operationId, clock.getTimeMillis()))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationCanceled.
   */
  def postCanceled(): Unit = {
    assertStatus(
      List(
        ExecuteStatus.Started,
        ExecuteStatus.Analyzed,
        ExecuteStatus.ReadyForExecution,
        ExecuteStatus.Finished,
        ExecuteStatus.Failed),
      ExecuteStatus.Canceled)
    canceled = Some(true)
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
    assertStatus(
      List(
        ExecuteStatus.Started,
        ExecuteStatus.Analyzed,
        ExecuteStatus.ReadyForExecution,
        ExecuteStatus.Finished),
      ExecuteStatus.Failed)
    error = Some(true)
    listenerBus.post(
      SparkListenerConnectOperationFailed(
        jobTag,
        operationId,
        clock.getTimeMillis(),
        errorMessage))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationFinished.
   * @param producedRowsCountOpt
   *   Number of rows that are returned to the user. None is expected when the operation does not
   *   return any rows.
   */
  def postFinished(
      producedRowsCountOpt: Option[Long] = None,
      extraTags: Map[String, String] = Map.empty): Unit = {
    assertStatus(
      List(ExecuteStatus.Started, ExecuteStatus.ReadyForExecution),
      ExecuteStatus.Finished)
    producedRowCount = producedRowsCountOpt

    listenerBus
      .post(
        SparkListenerConnectOperationFinished(
          jobTag,
          operationId,
          clock.getTimeMillis(),
          producedRowCount,
          extraTags))
  }

  /**
   * Post @link org.apache.spark.sql.connect.service.SparkListenerConnectOperationClosed.
   */
  def postClosed(): Unit = {
    assertStatus(
      List(ExecuteStatus.Finished, ExecuteStatus.Failed, ExecuteStatus.Canceled),
      ExecuteStatus.Closed)
    listenerBus
      .post(SparkListenerConnectOperationClosed(jobTag, operationId, clock.getTimeMillis()))
  }

  /**
   * @return
   *   \@link A org.apache.spark.sql.catalyst.QueryPlanningTracker that calls postAnalyzed &
   *   postReadyForExecution after analysis & prior execution.
   */
  def createQueryPlanningTracker(): QueryPlanningTracker = {
    new QueryPlanningTracker(Some(new QueryPlanningTrackerCallback {
      def analyzed(tracker: QueryPlanningTracker, analyzedPlan: LogicalPlan): Unit = {
        postAnalyzed(Some(analyzedPlan))
      }

      def readyForExecution(tracker: QueryPlanningTracker): Unit = postReadyForExecution()
    }))
  }

  private[connect] def status_(executeStatus: ExecuteStatus): Unit = {
    _status = executeStatus
  }

  private def assertStatus(
      validStatuses: List[ExecuteStatus],
      eventStatus: ExecuteStatus): Unit = {
    if (validStatuses.find(s => s == status).isEmpty) {
      throw new IllegalStateException(s"""
        operationId: $operationId with status ${status}
        is not within statuses $validStatuses for event $eventStatus
        """)
    }
    if (sessionHolder.eventManager.status != SessionStatus.Started) {
      throw new IllegalStateException(s"""
        sessionId: $sessionId with status $sessionStatus
        is not Started for event $eventStatus
        """)
    }
    _status = eventStatus
  }
}

/**
 * Event sent after reception of a Connect request (i.e. not queued), but prior any analysis or
 * execution.
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
 * @param sparkSessionTags:
 *   Extra tags set by the user (via SparkSession.addTag).
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
    sparkSessionTags: Set[String],
    extraTags: Map[String, String] = Map.empty)
    extends SparkListenerEvent {

  /**
   * The Connect request. None if the operation is not of type @link proto.ExecutePlanRequest
   */
  @JsonIgnore var planRequest: Option[proto.ExecutePlanRequest] = None
}

/**
 * The event is sent after a Connect request has been analyzed (@link
 * org.apache.spark.sql.catalyst.QueryPlanningTracker.ANALYSIS).
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
 * The event is sent after a Connect request is ready for execution. For eager commands this is
 * after @link org.apache.spark.sql.catalyst.QueryPlanningTracker.ANALYSIS. For other requests it
 * is after \@link org.apache.spark.sql.catalyst.QueryPlanningTracker.PLANNING
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
 * @param producedRowCount:
 *   Number of rows that are returned to the user. None is expected when the operation does not
 *   return any rows.
 * @param extraTags:
 *   Additional metadata during the request.
 */
case class SparkListenerConnectOperationFinished(
    jobTag: String,
    operationId: String,
    eventTime: Long,
    producedRowCount: Option[Long] = None,
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
