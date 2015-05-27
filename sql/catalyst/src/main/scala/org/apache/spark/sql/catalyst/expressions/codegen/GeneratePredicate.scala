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

package org.apache.spark.sql.catalyst.expressions.codegen

import org.apache.spark.sql.catalyst.expressions._

/**
 * Generates bytecode that evaluates a boolean [[Expression]] on a given input [[Row]].
 */
object GeneratePredicate extends CodeGenerator[Expression, (Row) => Boolean] {
  import scala.reflect.runtime.{universe => ru}

  protected def canonicalize(in: Expression): Expression = ExpressionCanonicalizer.execute(in)

  protected def bind(in: Expression, inputSchema: Seq[Attribute]): Expression =
    BindReferences.bindReference(in, inputSchema)

  protected def create(predicate: Expression): ((Row) => Boolean) = {
    val ctx = newCodeGenContext()
    val cEval = expressionEvaluator(predicate, ctx)

    val code = s"""
      (expressions: Seq[$exprType]) => {
        (i: $rowType) => {
          ${cEval.code}
          if (${cEval.nullTerm})
            false
          else
            ${cEval.primitiveTerm}
        }
      }
      """

    logWarning(s"Generated predicate '$predicate':\n$code")
    toolBox.eval(toolBox.parse(code)).asInstanceOf[(Seq[Expression]) => (Row => Boolean)](ctx.borrowed)
  }
}
