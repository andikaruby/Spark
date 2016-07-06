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

package org.apache.spark.deploy

import javax.annotation.concurrent.ThreadSafe

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.metrics.source.Source
import org.apache.spark.network.shuffle.ExternalShuffleBlockHandler

/**
 * Provides metrics source for external shuffle service
 */
@ThreadSafe
private class ExternalShuffleServiceSource
(blockHandler: ExternalShuffleBlockHandler) extends Source {
  override val metricRegistry = new MetricRegistry()
  override val sourceName = "shuffleService"

  metricRegistry.registerAll(blockHandler.getAllMetrics)

  metricRegistry.register(MetricRegistry.name("registeredExecutorsSize"),
    new Gauge[Long] {
      override def getValue: Long = blockHandler.getRegisteredExecutorsSize
    })

  metricRegistry.register(MetricRegistry.name("totalShuffleRequests"),
    new Gauge[Long] {
      override def getValue: Long = blockHandler.getTotalShuffleRequests
    })
}

