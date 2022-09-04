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

package org.apache.spark.sql.sparkconnect.service

import java.util
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._

import com.google.common.base.Ticker
import com.google.common.cache.CacheBuilder
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.stub.StreamObserver

import org.apache.spark.{SparkContext, SparkEnv}
import org.apache.spark.annotation.Experimental;
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.connect.proto
import org.apache.spark.connect.proto.{
  AnalyzeResponse,
  Request,
  Response,
  SparkConnectServiceGrpc
}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.execution.ExtendedMode
import org.apache.spark.sql.sparkconnect.planner.SparkConnectPlanner

/**
 * The SparkConnectService Implementation.
 *
 * This class implements the service stub from the generated code of GRPC.
 *
 * @param debug delegates debug behavior to the handlers.
 */
@Experimental
class SparkConnectService(debug: Boolean)
    extends SparkConnectServiceGrpc.SparkConnectServiceImplBase {
  override def executePlan(request: Request, responseObserver: StreamObserver[Response]): Unit =
    new SparkConnectStreamHandler(responseObserver).handle(request)

  override def analyzePlan(
      request: Request,
      responseObserver: StreamObserver[AnalyzeResponse]): Unit = {
    val session =
      SparkConnectService.getOrCreateIsolatedSession(request.getUserContext.getUserId).session

    val logicalPlan = request.getPlan.getOpTypeCase match {
      case proto.Plan.OpTypeCase.ROOT =>
        SparkConnectPlanner(request.getPlan.getRoot, session).transform()
      case _ =>
        responseObserver.onError(
          new UnsupportedOperationException(
            s"${request.getPlan.getOpTypeCase} not supported for analysis."))
        return
    }
    val ds = Dataset.ofRows(session, logicalPlan)
    val explainString = ds.queryExecution.explainString(ExtendedMode)

    val resp = proto.AnalyzeResponse
      .newBuilder()
      .setExplainString(explainString)
      .setClientId(request.getClientId)

    resp.addAllColumnTypes(ds.schema.fields.map(_.dataType.sql).toSeq.asJava)
    resp.addAllColumnNames(ds.schema.fields.map(_.name).toSeq.asJava)
    responseObserver.onNext(resp.build())
    responseObserver.onCompleted()
  }
}

/**
 * Trivial object used for referring to SparkSessions in the SessionCache.
 *
 * @param userId
 * @param session
 */
@Experimental
case class SessionHolder(userId: String, session: SparkSession) {}

/**
 * Satic instance of the SparkConnectService.
 *
 * Used to start the overall SparkConnect service and provides global state to manage the different
 * SparkSession from different users connecting to the cluster.
 */
@Experimental
object SparkConnectService {

  // Type alias for the SessionCacheKey. Right now this is a String but allows us to switch to a
  // different or complex type easily.
  type SessionCacheKey = String;

  var server: Server = _

  private val userSessionMapping =
    cacheBuilder(100, 3600).build[SessionCacheKey, SessionHolder]()

  // Simple builder for creating the cache of Sessions.
  private def cacheBuilder(cacheSize: Int, timeoutSeconds: Int): CacheBuilder[Object, Object] = {
    var cacheBuilder = CacheBuilder.newBuilder().ticker(Ticker.systemTicker())
    if (cacheSize >= 0) {
      cacheBuilder = cacheBuilder.maximumSize(cacheSize)
    }
    if (timeoutSeconds >= 0) {
      cacheBuilder.expireAfterAccess(timeoutSeconds, TimeUnit.SECONDS)
    }
    cacheBuilder
  }

  /**
   * Based on the `key` find or create a new SparkSession.
   */
  def getOrCreateIsolatedSession(key: SessionCacheKey): SessionHolder = {
    userSessionMapping.get(key, () => {
      SessionHolder(key, newIsolatedSession())
    })
  }

  private def newIsolatedSession(): SparkSession = {
    SparkSession.active.newSession()
  }

  /**
   * Starts the GRPC Serivce.
   *
   * TODO(martin.grund) Make port number configurable.
   */
  def startGRPCService(): Unit = {
    val debugMode = SparkEnv.get.conf.getBoolean("spark.connect.grpc.debug.enabled", true)
    val port = 15002
    val sb = NettyServerBuilder
      .forPort(port)
      .addService(new SparkConnectService(debugMode))

    // If debug mode is configured, load the ProtoReflection service so that tools like
    // grpcurl can introspect the API for debugging.
    if (debugMode) {
      sb.addService(ProtoReflectionService.newInstance())
    }
    server = sb.build
    server.start()
  }

  // Starts the service
  def start(): Unit = {
    startGRPCService()
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdownNow()
    }
  }
}

/**
 * This is the main entry point for Spark Connect.
 *
 * To decouple the build of Spark Connect and it's dependencies from the core of Spark, we implement
 * it as a Driver Plugin. To enable Spark Connect, simply make sure that the appropriate JAR is
 * available in the CLASSPATH and the driver plugin is configured to load this class.
 */
@Experimental
class SparkConnectPlugin extends SparkPlugin {

  /**
   * Return the plugin's driver-side component.
   *
   * @return The driver-side component, or null if one is not needed.
   */
  override def driverPlugin(): DriverPlugin = new DriverPlugin {

    override def init(
        sc: SparkContext,
        pluginContext: PluginContext): util.Map[String, String] = {
      SparkConnectService.start()
      Map.empty[String, String].asJava
    }

    override def shutdown(): Unit = {
      SparkConnectService.stop()
    }
  }

  override def executorPlugin(): ExecutorPlugin = null
}
