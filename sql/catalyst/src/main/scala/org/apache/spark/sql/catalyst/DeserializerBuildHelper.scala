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

package org.apache.spark.sql.catalyst

import org.apache.spark.sql.catalyst.analysis.UnresolvedExtractValue
import org.apache.spark.sql.catalyst.expressions.{Expression, GetStructField, UpCast}
import org.apache.spark.sql.catalyst.expressions.objects.{AssertNotNull, Invoke, StaticInvoke}
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.types._

private[spark] object DeserializerBuildHelper {
  /** Returns the current path with a sub-field extracted. */
  def addToPath(
      path: Expression,
      part: String,
      dataType: DataType,
      walkedTypePath: Seq[String]): Expression = {
    val newPath = UnresolvedExtractValue(path, expressions.Literal(part))
    upCastToExpectedType(newPath, dataType, walkedTypePath)
  }

  /** Returns the current path with a field at ordinal extracted. */
  def addToPathOrdinal(
      path: Expression,
      ordinal: Int,
      dataType: DataType,
      walkedTypePath: Seq[String]): Expression = {
    val newPath = GetStructField(path, ordinal)
    upCastToExpectedType(newPath, dataType, walkedTypePath)
  }

  def expressionWithNullSafety(
      expr: Expression,
      nullable: Boolean,
      walkedTypePath: Seq[String]): Expression = {
    if (nullable) {
      expr
    } else {
      AssertNotNull(expr, walkedTypePath)
    }
  }

  def deserializerForWithNullSafety(
      expr: Expression,
      dataType: DataType,
      nullable: Boolean,
      walkedTypePath: Seq[String],
      funcForCreatingNewExpr: (Expression, Seq[String]) => Expression): Expression = {
    val newExpr = funcForCreatingNewExpr(expr, walkedTypePath)
    expressionWithNullSafety(newExpr, nullable, walkedTypePath)
  }

  def deserializerForWithNullSafetyAndUpcast(
      expr: Expression,
      dataType: DataType,
      nullable: Boolean,
      walkedTypePath: Seq[String],
      funcForCreatingNewExpr: (Expression, Seq[String]) => Expression): Expression = {
    val casted = upCastToExpectedType(expr, dataType, walkedTypePath)
    deserializerForWithNullSafety(casted, dataType, nullable, walkedTypePath,
      funcForCreatingNewExpr)
  }

  def createDeserializerForTypesSupportValueOf(
      path: Expression,
      clazz: Class[_]): Expression = {
    StaticInvoke(
      clazz,
      ObjectType(clazz),
      "valueOf",
      path :: Nil,
      returnNullable = false)
  }

  def createDeserializerForString(path: Expression, returnNullable: Boolean): Expression = {
    Invoke(path, "toString", ObjectType(classOf[java.lang.String]),
      returnNullable = returnNullable)
  }

  def createDeserializerForSqlDate(path: Expression): Expression = {
    StaticInvoke(
      DateTimeUtils.getClass,
      ObjectType(classOf[java.sql.Date]),
      "toJavaDate",
      path :: Nil,
      returnNullable = false)
  }

  def createDeserializerForSqlTimestamp(path: Expression): Expression = {
    StaticInvoke(
      DateTimeUtils.getClass,
      ObjectType(classOf[java.sql.Timestamp]),
      "toJavaTimestamp",
      path :: Nil,
      returnNullable = false)
  }

  def createDeserializerForJavaBigDecimal(
      path: Expression,
      returnNullable: Boolean): Expression = {
    Invoke(path, "toJavaBigDecimal", ObjectType(classOf[java.math.BigDecimal]),
      returnNullable = returnNullable)
  }

  def createDeserializerForScalaBigDecimal(
      path: Expression,
      returnNullable: Boolean): Expression = {
    Invoke(path, "toBigDecimal", ObjectType(classOf[BigDecimal]), returnNullable = returnNullable)
  }

  def createDeserializerForJavaBigInteger(
      path: Expression,
      returnNullable: Boolean): Expression = {
    Invoke(path, "toJavaBigInteger", ObjectType(classOf[java.math.BigInteger]),
      returnNullable = returnNullable)
  }

  def createDeserializerForScalaBigInt(path: Expression): Expression = {
    Invoke(path, "toScalaBigInt", ObjectType(classOf[scala.math.BigInt]),
      returnNullable = false)
  }

  /**
   * When we build the `deserializer` for an encoder, we set up a lot of "unresolved" stuff
   * and lost the required data type, which may lead to runtime error if the real type doesn't
   * match the encoder's schema.
   * For example, we build an encoder for `case class Data(a: Int, b: String)` and the real type
   * is [a: int, b: long], then we will hit runtime error and say that we can't construct class
   * `Data` with int and long, because we lost the information that `b` should be a string.
   *
   * This method help us "remember" the required data type by adding a `UpCast`. Note that we
   * only need to do this for leaf nodes.
   */
  private def upCastToExpectedType(
      expr: Expression,
      expected: DataType,
      walkedTypePath: Seq[String]): Expression = expected match {
    case _: StructType => expr
    case _: ArrayType => expr
    case _: MapType => expr
    case _ => UpCast(expr, expected, walkedTypePath)
  }
}
