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
 * Generates byte code that produces a [[MutableRow]] object that can update itself based on a new
 * input [[Row]] for a fixed set of [[Expression Expressions]].
 */
object GenerateMutableProjection extends CodeGenerator {
  import scala.reflect.runtime.{universe => ru}
  import scala.reflect.runtime.universe._

  // TODO: Should be weak references... bounded in size.
  val projectionCache = new collection.mutable.HashMap[Seq[Expression], () => MutableProjection]

  def apply(expressions: Seq[Expression], inputSchema: Seq[Attribute]): (() => MutableProjection) =
    apply(expressions.map(BindReferences.bindReference(_, inputSchema)))

  // TODO: Safe to fire up multiple instances of the compiler?
  def apply(expressions: Seq[Expression]): () => MutableProjection =
    globalLock.synchronized {
      val cleanedExpressions = expressions.map(ExpressionCanonicalizer(_))
      projectionCache.getOrElseUpdate(cleanedExpressions, createProjection(cleanedExpressions))
    }

  val mutableRowName = newTermName("mutableRow")

  def createProjection(expressions: Seq[Expression]): (() => MutableProjection) = {
    val projectionCode = expressions.zipWithIndex.flatMap { case (e, i) =>
      val evaluationCode = expressionEvaluator(e)

      evaluationCode.code :+
        q"""
        if(${evaluationCode.nullTerm})
          mutableRow.setNullAt($i)
        else
          ${setColumn(mutableRowName, e.dataType, i, evaluationCode.primitiveTerm)}
      """
    }

    val code =
      q"""
      () => { new $mutableProjectionType {

        private[this] var $mutableRowName: $mutableRowType =
          new $genericMutableRowType(${expressions.size})

        def target(row: $mutableRowType): $mutableProjectionType = {
          $mutableRowName = row
          this
        }

        /* Provide immutable access to the last projected row. */
        def currentValue: $rowType = mutableRow

        def apply(i: $rowType): $rowType = {
          ..$projectionCode
          mutableRow
        }
      } }
    """

    log.debug(s"code for ${expressions.mkString(",")}:\n$code")
    toolBox.eval(code).asInstanceOf[() => MutableProjection]
  }
}
