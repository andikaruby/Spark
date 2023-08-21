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
package org.apache.spark.sql

import org.apache.spark.sql.test.RemoteSparkSession

class StubbingTestSuite extends RemoteSparkSession {
  private def eval[T](f: => T): T = f

  test("capture of to-be stubbed class") {
    val session = spark
    import session.implicits._
    val result = spark
      .range(10)
      .map(n => n + 1)
      .collectResult()
    eval {
      assert(result.length == 10)
    }
    result.close()
  }
}

class ActualStub(sparkSession: SparkSession) {}
