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

package org.apache.spark.sql.catalyst.expressions.aggregate

import org.apache.spark.sql.catalyst.analysis.TypeCheckResult
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.util.TypeUtils
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types._

@ExpressionDescription(
  usage = "_FUNC_(expr) - Returns the sum calculated from values of a group.",
  examples = """
    Examples:
      > SELECT _FUNC_(col) FROM VALUES (5), (10), (15) AS tab(col);
       30
      > SELECT _FUNC_(col) FROM VALUES (NULL), (10), (15) AS tab(col);
       25
      > SELECT _FUNC_(col) FROM VALUES (NULL), (NULL) AS tab(col);
       NULL
  """,
  group = "agg_funcs",
  since = "1.0.0")
case class Sum(child: Expression) extends DeclarativeAggregate with ImplicitCastInputTypes {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = resultType

  override def inputTypes: Seq[AbstractDataType] = Seq(NumericType)

  override def checkInputDataTypes(): TypeCheckResult =
    TypeUtils.checkForNumericExpr(child.dataType, "function sum")

  private lazy val resultType = child.dataType match {
    case DecimalType.Fixed(precision, scale) =>
      DecimalType.bounded(precision + 10, scale)
    case _: IntegralType => LongType
    case _ => DoubleType
  }

  private lazy val sumDataType = resultType

  private lazy val sum = AttributeReference("sum", sumDataType)()

  private lazy val isEmptyOrNulls = AttributeReference("isEmptyOrNulls", BooleanType, false)()

  private lazy val zero = Literal.default(sumDataType)

  override lazy val aggBufferAttributes = sum :: isEmptyOrNulls :: Nil

  override lazy val initialValues: Seq[Expression] = Seq(
    /* sum = */  zero,
    /* isEmptyOrNulls = */ Literal.create(true, BooleanType)
  )

  /**
   * For decimal types and when child is nullable:
   * isEmptyOrNulls flag is a boolean to represent if there are no rows or if all rows that
   * have been seen are null.  This will be used to identify if the end result of sum in
   * evaluateExpression should be null or not.
   *
   * Update of the isEmptyOrNulls flag:
   * If this flag is false, then keep it as is.
   * If this flag is true, then check if the incoming value is null and if it is null, keep it
   * as true else update it to false.
   * Once this flag is switched to false, it will remain false.
   *
   * The update of the sum is as follows:
   * If sum is null, then we have a case of overflow, so keep sum as is.
   * If sum is not null, and the incoming value is not null, then perform the addition along
   * with the overflow checking. Note, that if overflow occurs, then sum will be null here.
   * If the new incoming value is null, we will keep the sum in buffer as is and skip this
   * incoming null
   */
  override lazy val updateExpressions: Seq[Expression] = {
    if (child.nullable) {
      resultType match {
        case d: DecimalType =>
          Seq(
            /* sum */
            If(IsNull(sum), sum,
              If(IsNotNull(child.cast(sumDataType)),
                CheckOverflow(sum + child.cast(sumDataType), d, true), sum)),
            /* isEmptyOrNulls */
            If(isEmptyOrNulls, IsNull(child.cast(sumDataType)), isEmptyOrNulls)
          )
        case _ =>
          Seq(
            coalesce(sum + child.cast(sumDataType), sum),
            If(isEmptyOrNulls, IsNull(child.cast(sumDataType)), isEmptyOrNulls)
          )
      }
    } else {
      resultType match {
        case d: DecimalType =>
          Seq(
            /* sum */
            If(IsNull(sum), sum, CheckOverflow(sum + child.cast(sumDataType), d, true)),
            /* isEmptyOrNulls */
            false
          )
        case _ => Seq(sum + child.cast(sumDataType), false)
      }
    }
  }

  /**
   * For decimal type:
   * update of the sum is as follows:
   * Check if either portion of the left.sum or right.sum has overflowed
   * If it has, then the sum value will remain null.
   * If it did not have overflow, then add the sum.left and sum.right and check for overflow.
   *
   * isEmptyOrNulls:  Set to false if either one of the left or right is set to false. This
   * means we have seen atleast a row that was not null.
   * If the value from bufferLeft and bufferRight are both true, then this will be true.
   */
  override lazy val mergeExpressions: Seq[Expression] = {
    resultType match {
      case d: DecimalType =>
        Seq(
          /* sum = */
          If(And(IsNull(sum.left), EqualTo(isEmptyOrNulls.left, false)) ||
            And(IsNull(sum.right), EqualTo(isEmptyOrNulls.right, false)),
              Literal.create(null, resultType),
              CheckOverflow(sum.left + sum.right, d, true)),
          /* isEmptyOrNulls = */
          And(isEmptyOrNulls.left, isEmptyOrNulls.right)
          )
      case _ =>
        Seq(
          coalesce(sum.left + sum.right, sum.left),
          And(isEmptyOrNulls.left, isEmptyOrNulls.right)
        )
    }
  }

  /**
   * If the isEmptyOrNulls is true, then it means either there are no rows, or all the rows were
   * null, so the result will be null.
   * If the isEmptyOrNulls is false, then if sum is null that means an overflow has happened.
   * So now, if ansi is enabled, then throw exception, if not then return null.
   * If sum is not null, then return the sum.
   */
  override lazy val evaluateExpression: Expression = resultType match {
    case d: DecimalType =>
      If(EqualTo(isEmptyOrNulls, true),
        Literal.create(null, sumDataType),
        If(And(SQLConf.get.ansiEnabled, IsNull(sum)),
          OverflowException(resultType, "Arithmetic Operation overflow"), sum))
    case _ => If(EqualTo(isEmptyOrNulls, true), Literal.create(null, resultType), sum)
  }

}
