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

package org.apache.spark.sql.sparkconnect.command

import com.google.common.collect.{Lists, Maps}
import scala.collection.JavaConverters._

import org.apache.spark.annotation.Experimental;
import org.apache.spark.api.python.{PythonEvalType, SimplePythonFunction}
import org.apache.spark.connect.{proto => proto}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.python.UserDefinedPythonFunction
import org.apache.spark.sql.types.StringType

@Experimental
case class SparkConnectCommandPlanner(session: SparkSession, command: proto.Command) {

  def process(): Unit = {
    command.getCommandTypeCase match {
      case proto.Command.CommandTypeCase.CREATE_FUNCTION =>
        handleCreateScalarFunction(command.getCreateFunction)
      case _ => throw new UnsupportedOperationException(s"${command} not supported.")
    }
  }

  // This is a helper function that registers a new Python function in the
  // [[SparkSession]].
  def handleCreateScalarFunction(cf: proto.CreateScalarFunction): Unit = {
    val function = SimplePythonFunction(
      cf.getSerializedFunction.toByteArray,
      Maps.newHashMap(),
      Lists.newArrayList(),
      "python3",
      "3.9",
      Lists.newArrayList(),
      null)

    val udf = UserDefinedPythonFunction(
      cf.getPartsList.asScala.head,
      function,
      StringType,
      PythonEvalType.SQL_BATCHED_UDF,
      udfDeterministic = false)

    session.udf.registerPython(cf.getPartsList.asScala.head, udf)

  }

}
