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

package org.apache.spark.sql.execution.datasources

import org.apache.spark.SparkFunSuite
import org.apache.spark.api.python.PythonEvalType
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types._

class DataSourceUtilsSuite extends SparkFunSuite {
  test("shouldPushFilter should return false if expression contains Unevaluable expression") {
    val unevaluableExpression = PythonUDF("pyUDF", null,
      IntegerType, Seq.empty, PythonEvalType.SQL_BATCHED_UDF, true)

    val result = DataSourceUtils.shouldPushFilter(unevaluableExpression)

    assert(result === false)
  }

  test("shouldPushFilter should return true when not containing Unevaluable expression") {
    val expression = EqualTo(Literal("a"), Literal("b"))

    val result = DataSourceUtils.shouldPushFilter(expression)

    assert(result === true)
  }
}
