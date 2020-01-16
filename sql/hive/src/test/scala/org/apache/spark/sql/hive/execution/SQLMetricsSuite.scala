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

package org.apache.spark.sql.hive.execution

import org.apache.spark.sql.execution.metric.SQLMetricsTestUtils
import org.apache.spark.sql.hive.test.TestHiveSingleton
import org.apache.spark.sql.internal.SQLConf

class SQLMetricsSuite extends SQLMetricsTestUtils with TestHiveSingleton {

  var originalValue: String = _
  // With AQE on/off, the metric info is different.
  override def beforeAll(): Unit = {
    super.beforeAll()
    originalValue = spark.conf.get(SQLConf.ADAPTIVE_EXECUTION_ENABLED.key)
    spark.conf.set(SQLConf.ADAPTIVE_EXECUTION_ENABLED.key, "false")
  }

  override def afterAll(): Unit = {
    spark.conf.set(SQLConf.ADAPTIVE_EXECUTION_ENABLED.key, originalValue)
    super.afterAll()
  }

  test("writing data out metrics: hive") {
    testMetricsNonDynamicPartition("hive", "t1")
  }

  test("writing data out metrics dynamic partition: hive") {
    withSQLConf(("hive.exec.dynamic.partition.mode", "nonstrict")) {
      testMetricsDynamicPartition("hive", "hive", "t1")
    }
  }
}
