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
package org.apache.spark.deploy.yarn

import scala.collection.mutable

import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.util.{Clock, SystemClock}

private[spark] class FailureWithinTimeIntervalTracker(sparkConf: SparkConf) extends Logging {

  private var clock: Clock = new SystemClock

  private val executorFailuresValidityInterval =
    sparkConf.get(config.EXECUTOR_ATTEMPT_FAILURE_VALIDITY_INTERVAL_MS).getOrElse(-1L)

  // Queue to store the timestamp of failed executors for each host
  private val failedExecutorsTimeStampsPerHost = mutable.Map[String, mutable.Queue[Long]]()

  private val sumFailedExecutorsTimeStamps = new mutable.Queue[Long]()

  private def getNumFailuresWithinValidityInterval(
      failedExecutorsTimeStampsForHost: mutable.Queue[Long],
      endTime: Long): Int = {
    while (executorFailuresValidityInterval > 0
      && failedExecutorsTimeStampsForHost.nonEmpty
      && failedExecutorsTimeStampsForHost.head < endTime - executorFailuresValidityInterval) {
      failedExecutorsTimeStampsForHost.dequeue()
    }
    failedExecutorsTimeStampsForHost.size
  }

  /**
   * Use a different clock. This is mainly used for testing.
   */
  def setClock(newClock: Clock): Unit = {
    clock = newClock
  }

  def getSumExecutorsFailed: Int = synchronized {
    getNumFailuresWithinValidityInterval(sumFailedExecutorsTimeStamps, clock.getTimeMillis())
  }

  def registerFailureOnHost(hostname: String): Unit = synchronized {
    val timeMillis = clock.getTimeMillis()
    sumFailedExecutorsTimeStamps.enqueue(timeMillis)
    val failedExecutorsOnHost =
      failedExecutorsTimeStampsPerHost.getOrElse(hostname, {
        val failureOnHost = mutable.Queue[Long]()
        failedExecutorsTimeStampsPerHost.put(hostname, failureOnHost)
        failureOnHost
      })
    failedExecutorsOnHost.enqueue(timeMillis)  }

  def registerSumExecutorFailure(): Unit = synchronized {
    val timeMillis = clock.getTimeMillis()
    sumFailedExecutorsTimeStamps.enqueue(timeMillis)
  }

  def getNumExecutorFailuresOnHost(hostname: String): Int =
    failedExecutorsTimeStampsPerHost.get(hostname).map { failedExecutorsOnHost =>
      getNumFailuresWithinValidityInterval(failedExecutorsOnHost, clock.getTimeMillis())
    }.getOrElse(0)

}
