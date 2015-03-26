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

package org.apache.spark.scheduler.cluster

import scala.concurrent.{Future, ExecutionContext}

import _root_.akka.actor.{Actor, ActorRef, Props}
import _root_.akka.remote.{DisassociatedEvent, RemotingLifecycleEvent}

import org.apache.spark.{Logging, SparkContext}
import org.apache.spark.rpc._
import org.apache.spark.scheduler.cluster.CoarseGrainedClusterMessages._
import org.apache.spark.scheduler.TaskSchedulerImpl
import org.apache.spark.ui.JettyUtils
import org.apache.spark.util.{AkkaUtils, Utils}

import scala.util.control.NonFatal

/**
 * Abstract Yarn scheduler backend that contains common logic
 * between the client and cluster Yarn scheduler backends.
 */
private[spark] abstract class YarnSchedulerBackend(
    scheduler: TaskSchedulerImpl,
    sc: SparkContext)
  extends CoarseGrainedSchedulerBackend(scheduler, sc.env.rpcEnv) {

  if (conf.getOption("spark.scheduler.minRegisteredResourcesRatio").isEmpty) {
    minRegisteredRatio = 0.8
  }

  protected var totalExpectedExecutors = 0

  private val yarnSchedulerActor: RpcEndpointRef =
    rpcEnv.setupEndpoint(YarnSchedulerBackend.ACTOR_NAME, new YarnSchedulerActor(rpcEnv))

  private implicit val askTimeout = AkkaUtils.askTimeout(sc.conf)

  /**
   * Request executors from the ApplicationMaster by specifying the total number desired.
   * This includes executors already pending or running.
   */
  override def doRequestTotalExecutors(requestedTotal: Int): Boolean = {
    yarnSchedulerActor.askWithReply[Boolean](RequestExecutors(requestedTotal))
  }

  /**
   * Request that the ApplicationMaster kill the specified executors.
   */
  override def doKillExecutors(executorIds: Seq[String]): Boolean = {
    yarnSchedulerActor.askWithReply[Boolean](KillExecutors(executorIds))
  }

  override def sufficientResourcesRegistered(): Boolean = {
    totalRegisteredExecutors.get() >= totalExpectedExecutors * minRegisteredRatio
  }

  /**
   * Add filters to the SparkUI.
   */
  private def addWebUIFilter(
      filterName: String,
      filterParams: Map[String, String],
      proxyBase: String): Unit = {
    if (proxyBase != null && proxyBase.nonEmpty) {
      System.setProperty("spark.ui.proxyBase", proxyBase)
    }

    val hasFilter =
      filterName != null && filterName.nonEmpty &&
      filterParams != null && filterParams.nonEmpty
    if (hasFilter) {
      logInfo(s"Add WebUI Filter. $filterName, $filterParams, $proxyBase")
      conf.set("spark.ui.filters", filterName)
      filterParams.foreach { case (k, v) => conf.set(s"spark.$filterName.param.$k", v) }
      scheduler.sc.ui.foreach { ui => JettyUtils.addFilters(ui.getHandlers, conf) }
    }
  }

  /**
   * An actor that communicates with the ApplicationMaster.
   */
  private class YarnSchedulerActor(override val rpcEnv: RpcEnv) extends RpcEndpoint with Logging {
    @volatile private var amActor: Option[RpcEndpointRef] = None

    implicit val askAmActorExecutor = ExecutionContext.fromExecutor(
      Utils.newDaemonCachedThreadPool("yarn-scheduler-ask-am-executor"))

    override def receive: PartialFunction[Any, Unit] = {
      case RegisterClusterManager(am) =>
        logInfo(s"ApplicationMaster registered as $am")
        amActor = Some(am)

      case AddWebUIFilter(filterName, filterParams, proxyBase) =>
        addWebUIFilter(filterName, filterParams, proxyBase)

    }

    override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
      case RegisterClusterManager(am) =>
        logInfo(s"ApplicationMaster registered as $am")
        amActor = Some(am)

      case r: RequestExecutors =>
        amActor match {
          case Some(actor) =>
            Future {
              context.reply(actor.askWithReply[Boolean](r))
            } onFailure {
              case NonFatal(e) => logError(s"Sending $r to AM was unsuccessful", e)
            }
          case None =>
            logWarning("Attempted to request executors before the AM has registered!")
            context.reply(false)
        }

      case k: KillExecutors =>
        amActor match {
          case Some(actor) =>
            Future {
              context.reply(actor.askWithReply[Boolean](k))
            } onFailure {
              case NonFatal(e) => logError(s"Sending $k to AM was unsuccessful", e)
            }
          case None =>
            logWarning("Attempted to kill executors before the AM has registered!")
            context.reply(false)
        }

    }

    override def onDisconnected(remoteAddress: RpcAddress): Unit = {
      if (amActor.exists(_.address == remoteAddress)) {
        logWarning(s"ApplicationMaster has disassociated: $remoteAddress")
      }
    }
  }
}

private[spark] object YarnSchedulerBackend {
  val ACTOR_NAME = "YarnScheduler"
}
