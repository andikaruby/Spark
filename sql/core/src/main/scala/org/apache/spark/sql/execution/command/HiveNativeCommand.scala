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

package org.apache.spark.sql.execution.command

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.catalyst.expressions.AttributeReference
import org.apache.spark.sql.types.StringType

/**
 * A command that we delegate to Hive. Eventually we should remove this.
 */
case class HiveNativeCommand(sql: String) extends RunnableCommand {

  override def output: Seq[AttributeReference] =
    Seq(AttributeReference("result", StringType, nullable = false)())

  override def run(sparkSession: SparkSession): Seq[Row] = {
    logWarning(s"Native command: ${sql.trim}")
    sparkSession.sessionState.runNativeSql(sql).map(Row(_))
  }
}
