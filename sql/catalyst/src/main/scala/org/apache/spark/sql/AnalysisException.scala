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

import org.apache.spark.SparkError
import org.apache.spark.annotation.Stable
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan

/**
 * Thrown when a query fails to analyze, usually because the query itself is invalid.
 *
 * @since 1.3.0
 */
@Stable
class AnalysisException protected[sql] (
    val message: String,
    val line: Option[Int] = None,
    val startPosition: Option[Int] = None,
    // Some plans fail to serialize due to bugs in scala collections.
    @transient val plan: Option[LogicalPlan] = None,
    val cause: Option[Throwable] = None,
    val errorClass: Option[String] = None,
    val messageParameters: Seq[String] = Seq.empty)
  extends Exception(message, cause.orNull) with SparkError with Serializable {

  def this(errorClass: String, messageParameters: Seq[String], cause: Option[Throwable]) =
    this(
      SparkError.getMessage(errorClass, messageParameters),
      errorClass = Some(errorClass),
      messageParameters = messageParameters,
      cause = cause)

  def this(errorClass: String, messageParameters: Seq[String]) =
    this(errorClass = errorClass, messageParameters = messageParameters, cause = None)

  def withPosition(line: Option[Int], startPosition: Option[Int]): AnalysisException = {
    val newException = new AnalysisException(
      message, line, startPosition, plan, cause, errorClass, messageParameters)
    newException.setStackTrace(getStackTrace)
    newException
  }

  override def getMessage: String = {
    val planAnnotation = Option(plan).flatten.map(p => s";\n$p").getOrElse("")
    getSimpleMessage + planAnnotation
  }

  // Outputs an exception without the logical plan.
  // For testing only
  def getSimpleMessage: String = if (line.isDefined || startPosition.isDefined) {
    val lineAnnotation = line.map(l => s" line $l").getOrElse("")
    val positionAnnotation = startPosition.map(p => s" pos $p").getOrElse("")
    s"$message;$lineAnnotation$positionAnnotation"
  } else {
    message
  }
}
