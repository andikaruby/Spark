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

package org.apache.spark.sql.connect.client

import scala.language.existentials

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import java.net.URI

import org.apache.spark.connect.proto
import org.apache.spark.sql.connect.common.config.ConnectCommon

/**
 * Conceptually the remote spark session that communicates with the server.
 */
class SparkConnectClient(
    private val userContext: proto.UserContext,
    private val channel: ManagedChannel) {

  private[this] val stub = proto.SparkConnectServiceGrpc.newBlockingStub(channel)

  /**
   * Placeholder method.
   * @return
   *   User ID.
   */
  def userId: String = userContext.getUserId()

  /**
   * Dispatch the [[proto.AnalyzePlanRequest]] to the Spark Connect server.
   * @return
   *   A [[proto.AnalyzePlanResponse]] from the Spark Connect server.
   */
  def analyze(request: proto.AnalyzePlanRequest): proto.AnalyzePlanResponse =
    stub.analyzePlan(request)

  /**
   * Shutdown the client's connection to the server.
   */
  def shutdown(): Unit = {
    channel.shutdownNow()
  }
}

object SparkConnectClient {
  def builder(): Builder = new Builder()

  /**
   * This is a helper class that is used to create a GRPC channel based on either a set host and
   * port or a NameResolver-compliant URI connection string.
   */
  class Builder() {
    private val userContextBuilder = proto.UserContext.newBuilder()
    private var _host: String = "localhost"
    private var _port: Int = ConnectCommon.CONNECT_GRPC_BINDING_PORT

    def userId(id: String): Builder = {
      userContextBuilder.setUserId(id)
      this
    }

    def host(host: String): Builder = {
      require(host != null)
      _host = host
      this
    }

    def port(port: Int): Builder = {
      _port = port
      this
    }

    object URIParams {
      val PARAM_USER_ID = "user_id"
      val PARAM_USE_SSL = "use_ssl"
      val PARAM_TOKEN = "token"
    }

    private def verifyURI(uri: URI): Unit = {
      if (uri.getScheme != "sc") {
        throw new IllegalArgumentException("Scheme for connection URI must be 'sc'.")
      }
      // Java URI considers everything after the authority segment as "path" until the ? (query)/# (fragment)
      // components as shown in the regex [scheme:][//authority][path][?query][#fragment].
      // However, with the Spark Connect definition, configuration parameter are passed in the style of the HTTP URL
      // Path Parameter Syntax (e.g sc://hostname:port/;param1=value;param2=value).
      // Thus, we manualy parse the "java path" to get the "correct path" and configuration parameters.
      val pathAndParams = uri.getPath.split(';')
      if (pathAndParams.nonEmpty && (pathAndParams(0) != "/" && pathAndParams(0) != "")) {
        throw new IllegalArgumentException(
          s"Path component for connection URI must be empty: " +
            s"${pathAndParams(0)}")
      }
      if (uri.getHost.isEmpty) {
        throw new IllegalArgumentException(s"Host for connection URI must be defined.")
      }
    }

    private def parseURIParams(uri: URI): Unit = {
      val params = uri.getPath.split(';').drop(1).filter(_ != "")
      params.foreach { kv =>
        val (key, value) = {
          val arr = kv.split('=')
          if (arr.length != 2) {
            throw new IllegalArgumentException(
              s"Parameter $kv is not a valid parameter" +
                s" key-value pair")
          }
          (arr(0), arr(1))
        }
        if (key == URIParams.PARAM_USER_ID) {
          userContextBuilder.setUserId(value)
        }
        // TODO(SPARK-41917): Support SSL and Auth tokens.
        if (key == URIParams.PARAM_USE_SSL) {
          throw new UnsupportedOperationException("SSL is currently not supported.")
        }
        if (key == URIParams.PARAM_TOKEN) {
          throw new UnsupportedOperationException("Auth tokens are currently not supported.")
        }
      }
    }

    /**
     * Creates the channel with a target connection string, per the documentation of Spark
     * Connect.
     *
     * Note: The connection string, if used, will override any previous host/port settings.
     */
    def connectionString(connectionString: String): Builder = {
      // TODO: Support ssl/bearer token params
      val uri = new URI(connectionString)
      verifyURI(uri)
      parseURIParams(uri)
      _host = uri.getHost
      _port = uri.getPort
      this
    }

    def build(): SparkConnectClient = {
      val channelBuilder = ManagedChannelBuilder.forAddress(_host, _port).usePlaintext()
      new SparkConnectClient(userContextBuilder.build(), channelBuilder.build())
    }
  }
}
