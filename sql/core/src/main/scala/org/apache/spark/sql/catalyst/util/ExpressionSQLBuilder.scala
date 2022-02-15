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

package org.apache.spark.sql.catalyst.util

import org.apache.spark.sql.catalyst.expressions.{Attribute, BinaryOperator, CaseWhen, EqualTo, Expression, IsNotNull, IsNull, Literal, Not}
import org.apache.spark.sql.connector.expressions.{Expression => V2Expression, FieldReference, GeneralSQLExpression, LiteralValue}

/**
 * The builder to generate V2 expressions from catalyst expressions.
 */
class ExpressionSQLBuilder(e: Expression) {

  def build(): Option[V2Expression] = {
    val expression = generateExpression(e)
    expression.foreach {
      case generated: GeneralSQLExpression =>
        generateSQL(e).foreach(sql => generated.setSql(sql))
      case _ =>
    }
    expression
  }

  private def generateExpression(expr: Expression): Option[V2Expression] = expr match {
    case Literal(value, dataType) => Some(LiteralValue(value, dataType))
    case attr: Attribute => Some(FieldReference.column(quoteIfNeeded(attr.name)))
    case IsNull(col) => generateExpression(col)
      .map(c => new GeneralSQLExpression("IS NULL", Array[V2Expression](c)))
    case IsNotNull(col) => generateExpression(col)
      .map(c => new GeneralSQLExpression("IS NOT NULL", Array[V2Expression](c)))
    case b: BinaryOperator =>
      val left = generateExpression(b.left)
      val right = generateExpression(b.right)
      if (left.isDefined && right.isDefined) {
        Some(new GeneralSQLExpression(b.sqlOperator, Array[V2Expression](left.get, right.get)))
      } else {
        None
      }
    case Not(eq: EqualTo) =>
      val left = generateExpression(eq.left)
      val right = generateExpression(eq.right)
      if (left.isDefined && right.isDefined) {
        Some(new GeneralSQLExpression("!=", Array[V2Expression](left.get, right.get)))
      } else {
        None
      }
    case Not(child) =>
      generateExpression(child).map(v => new GeneralSQLExpression("NOT", Array[V2Expression](v)))
    case CaseWhen(branches, elseValue) =>
      val conditions = branches.map(_._1).flatMap(generateExpression)
      val values = branches.map(_._2).flatMap(generateExpression)
      if (conditions.length == branches.length && values.length == branches.length) {
        val branchExpressions = conditions.zip(values).map { case (c, v) =>
          new GeneralSQLExpression(Array[V2Expression](c, v))
        }
        val branchExpression = new GeneralSQLExpression(branchExpressions.toArray[V2Expression])
        if (elseValue.isDefined) {
          elseValue.flatMap(generateExpression).map { v =>
            new GeneralSQLExpression("CASE WHEN", Array[V2Expression](branchExpression, v))
          }
        } else {
          Some(new GeneralSQLExpression("CASE WHEN", Array[V2Expression](branchExpression)))
        }
      } else {
        None
      }
    // TODO supports other expressions
    case _ => None
  }

  private def generateSQL(expr: Expression): Option[String] = expr match {
    case Literal(value, dataType) => Some(LiteralValue(value, dataType).toString)
    case a: Attribute => Some(quoteIfNeeded(a.name))
    case IsNull(col) => generateSQL(col).map(c => s"$c IS NULL")
    case IsNotNull(col) => generateSQL(col).map(c => s"$c IS NOT NULL")
    case b: BinaryOperator =>
      val l = generateSQL(b.left)
      val r = generateSQL(b.right)
      if (l.isDefined && r.isDefined) {
        Some(s"(${l.get}) ${b.sqlOperator} (${r.get})")
      } else {
        None
      }
    case Not(EqualTo(left, right)) =>
      val l = generateSQL(left)
      val r = generateSQL(right)
      if (l.isDefined && r.isDefined) {
        Some(s"${l.get} != ${r.get}")
      } else {
        None
      }
    case Not(child) => generateSQL(child).map(v => s"NOT ($v)")
    case CaseWhen(branches, elseValue) =>
      val conditionsSQL = branches.map(_._1).flatMap(generateSQL)
      val valuesSQL = branches.map(_._2).flatMap(generateSQL)
      if (conditionsSQL.length == branches.length && valuesSQL.length == branches.length) {
        val branchSQL =
          conditionsSQL.zip(valuesSQL).map { case (c, v) => s" WHEN $c THEN $v" }.mkString
        if (elseValue.isDefined) {
          elseValue.flatMap(generateSQL).map(v => s"CASE$branchSQL ELSE $v END")
        } else {
          Some(s"CASE$branchSQL END")
        }
      } else {
        None
      }
    // TODO supports other expressions
    case _ => None
  }
}
