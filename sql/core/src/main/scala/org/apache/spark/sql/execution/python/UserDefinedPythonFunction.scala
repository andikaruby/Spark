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

package org.apache.spark.sql.execution.python

import org.apache.spark.api.python.PythonFunction
import org.apache.spark.sql.Column
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.types.DataType

/**
 * A user-defined Python function. This is used by the Python API.
 */
case class UserDefinedPythonFunction(
    name: String,
    func: PythonFunction,
    dataType: DataType,
    pythonEvalType: Int) {

  private var _nullable: Boolean = true

  /**
   * Returns true when the UDF can return a nullable value.
   */
  def nullable: Boolean = _nullable

  def builder(e: Seq[Expression]): PythonUDF = {
<<<<<<< HEAD
    PythonUDF(name, func, dataType, e, pythonEvalType)
=======
    PythonUDF(name, func, dataType, e, pythonEvalType, _nullable)
  }

  /** Returns a [[Column]] that will evaluate to calling this UDF with the given input. */
  def apply(exprs: Column*): Column = {
    val udf = builder(exprs.map(_.expr))
    Column(udf)
  }

  private def copyAll(): UserDefinedPythonFunction = {
    val udf = copy()
    udf._nullable = _nullable
    udf
  }

  /**
   * Updates UserDefinedFunction with a given nullability.
   */
  def withNullability(nullable: Boolean): UserDefinedPythonFunction = {
    if (nullable == _nullable) {
      this
    } else {
      val udf = copyAll()
      udf._nullable = nullable
      udf
    }
  }
}
