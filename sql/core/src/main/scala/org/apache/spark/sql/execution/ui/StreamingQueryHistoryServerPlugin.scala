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

package org.apache.spark.sql.execution.ui

import org.apache.spark.SparkConf
import org.apache.spark.internal.config.Status.ASYNC_TRACKING_ENABLED
import org.apache.spark.scheduler.SparkListener
import org.apache.spark.sql.execution.streaming.StreamingQueryListenerBus
import org.apache.spark.sql.streaming.ui.{StreamingQueryStatusListener, StreamingQueryTab}
import org.apache.spark.status.{AppHistoryServerPlugin, ElementTrackingStore}
import org.apache.spark.ui.SparkUI

class StreamingQueryHistoryServerPlugin extends AppHistoryServerPlugin {

  override def createListeners(conf: SparkConf, store: ElementTrackingStore): Seq[SparkListener] = {
    val listenerBus = new StreamingQueryListenerBus(None, live = false)
    listenerBus.addListener(new StreamingQueryStatusListener(conf, store))
    Seq(listenerBus)
  }

  override def setupUI(ui: SparkUI): Unit = {
    val replayConf = ui.conf.clone().set(ASYNC_TRACKING_ENABLED, false)
    val trackingStore = new ElementTrackingStore(ui.store.store, replayConf)
    val streamingQueryStatusStore = new StreamingQueryStatusStore(trackingStore)
    new StreamingQueryTab(streamingQueryStatusStore, ui)
  }

  override def displayOrder: Int = 1
}
