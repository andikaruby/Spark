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

package org.apache.spark.streaming.flume

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.api.java.{JavaReceiverInputDStream, JavaStreamingContext}
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import java.net.InetSocketAddress

object FlumeUtils {
  /**
   * Create a input stream from a Flume source.
   * @param ssc      StreamingContext object
   * @param hostname Hostname of the slave machine to which the flume data will be sent
   * @param port     Port of the slave machine to which the flume data will be sent
   * @param storageLevel  Storage level to use for storing the received objects
   */
  def createStream (
      ssc: StreamingContext,
      hostname: String,
      port: Int,
      storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2
    ): ReceiverInputDStream[SparkFlumeEvent] = {
    val inputStream = new FlumeInputDStream[SparkFlumeEvent](ssc, hostname, port, storageLevel)
    inputStream
  }

  /**
   * Creates a input stream from a Flume source.
   * Storage level of the data will be the default StorageLevel.MEMORY_AND_DISK_SER_2.
   * @param hostname Hostname of the slave machine to which the flume data will be sent
   * @param port     Port of the slave machine to which the flume data will be sent
   */
  def createStream(
      jssc: JavaStreamingContext,
      hostname: String,
      port: Int
    ): JavaReceiverInputDStream[SparkFlumeEvent] = {
    createStream(jssc.ssc, hostname, port)
  }

  /**
   * Creates a input stream from a Flume source.
   * @param hostname Hostname of the slave machine to which the flume data will be sent
   * @param port     Port of the slave machine to which the flume data will be sent
   * @param storageLevel  Storage level to use for storing the received objects
   */
  def createStream(
      jssc: JavaStreamingContext,
      hostname: String,
      port: Int,
      storageLevel: StorageLevel
    ): JavaReceiverInputDStream[SparkFlumeEvent] = {
    createStream(jssc.ssc, hostname, port, storageLevel)
  }

  /**
   * Creates an input stream that is to be used with the Spark Sink deployed on a Flume agent.
   * This stream will poll the sink for data and will pull events as they are available.
   * @param addresses List of InetSocketAddresses representing the hosts to connect to.
   * @param maxBatchSize The maximum number of events to be pulled from the Spark sink in a
   *                     single RPC call
   * @param parallelism Number of concurrent requests this stream should send to the sink. Note
   *                    that having a higher number of requests concurrently being pulled will
   *                    result in this stream using more threads
   * @param storageLevel Storage level to use for storing the received objects
   */
  def createPollingStream (
    ssc: StreamingContext,
    addresses: Seq[InetSocketAddress],
    maxBatchSize: Int = 100,
    parallelism: Int = 5,
    storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2
  ): ReceiverInputDStream[SparkPollingEvent] = {
    new FlumePollingInputDStream[SparkPollingEvent](ssc, addresses, maxBatchSize,
      parallelism, storageLevel)
  }

  /**
   * Creates an input stream that is to be used with the Spark Sink deployed on a Flume agent.
   * This stream will poll the sink for data and will pull events as they are available.
   * @param addresses List of InetSocketAddresses representing the hosts to connect to.
   * @param maxBatchSize The maximum number of events to be pulled from the Spark sink in a
   *                     single RPC call
   * @param parallelism Number of concurrent requests this stream should send to the sink. Note
   *                    that having a higher number of requests concurrently being pulled will
   *                    result in this stream using more threads
   * @param storageLevel Storage level to use for storing the received objects
   */
  def createJavaPollingStream (
    ssc: StreamingContext,
    addresses: Seq[InetSocketAddress],
    maxBatchSize: Int = 100,
    parallelism: Int = 5,
    storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2
  ): JavaReceiverInputDStream[SparkPollingEvent] = {
    new FlumePollingInputDStream[SparkPollingEvent](ssc, addresses, maxBatchSize,
      parallelism, storageLevel)
  }
}
