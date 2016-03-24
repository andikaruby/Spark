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

package org.apache.spark.sql.catalyst.plans.logical

import org.apache.spark.sql.catalyst.expressions.{Attribute, AttributeSet, Expression}

/**
 * Transforms the input by forking and running the specified script.
 *
 * @param input the set of expression that should be passed to the script.
 * @param script the command that should be executed.
 * @param outputBeforeConstraints the attributes that are produced by the script.
 * @param ioschema the input and output schema applied in the execution of the script.
 */
case class ScriptTransformation(
    input: Seq[Expression],
    script: String,
    outputBeforeConstraints: Seq[Attribute],
    child: LogicalPlan,
    ioschema: ScriptInputOutputSchema) extends UnaryNode {
  override def references: AttributeSet = AttributeSet(input.flatMap(_.references))
}

/**
 * A placeholder for implementation specific input and output properties when passing data
 * to a script. For example, in Hive this would specify which SerDes to use.
 */
trait ScriptInputOutputSchema
