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

package org.apache.spark.sql.catalyst.expressions

import java.util.Locale

import org.apache.spark.sql.catalyst.analysis.TypeCheckResult
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, CodeGenerator, ExprCode}
import org.apache.spark.sql.catalyst.expressions.codegen.Block.BlockHelper
import org.apache.spark.sql.catalyst.util.ToNumberParser
import org.apache.spark.sql.types.{AbstractDataType, DataType, Decimal, DecimalType, StringType}
import org.apache.spark.unsafe.types.UTF8String

/**
 * A function that converts strings to decimal values, returning an exception if the input string
 * fails to match the format string.
 */
@ExpressionDescription(
  usage = """
     _FUNC_(expr, fmt) - Convert string 'expr' to a number based on the string format 'fmt'.
       Throws an exception if the conversion fails. The format can consist of the following
       characters, case insensitive:
         '0' or '9': Specifies an expected digit between 0 and 9. A sequence of 0 or 9 in the format
           string matches a sequence of digits in the input string. If the 0/9 sequence starts with
           0 and is before the decimal point, it can only match a digit sequence of the same size.
           Otherwise, if the sequence starts with 9 or is after the decimal point, it can match a
           digit sequence that has the same or smaller size.
         '.' or 'D': Specifies the position of the decimal point (optional, only allowed once).
         ',' or 'G': Specifies the position of the grouping (thousands) separator (,). There must be
           a 0 or 9 to the left and right of each grouping separator. 'expr' must match the
           grouping separator relevant for the size of the number.
         '$': Specifies the location of the $ currency sign. This character may only be specified
           once.
         'S' or 'MI': Specifies the position of a '-' or '+' sign (optional, only allowed once at
           the beginning or end of the format string). Note that 'S' allows '-' but 'MI' does not.
         'PR': Only allowed at the end of the format string; specifies that 'expr' indicates a
           negative number with wrapping angled brackets.
           ('<1>').
  """,
  examples = """
    Examples:
      > SELECT _FUNC_('454', '999');
       454
      > SELECT _FUNC_('454.00', '000.00');
       454.00
      > SELECT _FUNC_('12,454', '99,999');
       12454
      > SELECT _FUNC_('$78.12', '$99.99');
       78.12
      > SELECT _FUNC_('12,454.8-', '99,999.9S');
       -12454.8
  """,
  since = "3.3.0",
  group = "string_funcs")
case class ToNumber(left: Expression, right: Expression)
  extends BinaryExpression with ImplicitCastInputTypes with NullIntolerant {
  private lazy val numberFormat = right.eval().toString.toUpperCase(Locale.ROOT)
  private lazy val numberFormatter = new ToNumberParser(numberFormat, true)

  override def dataType: DataType = numberFormatter.parsedDecimalType
  override def inputTypes: Seq[DataType] = Seq(StringType, StringType)
  override def checkInputDataTypes(): TypeCheckResult = {
    val inputTypeCheck = super.checkInputDataTypes()
    if (inputTypeCheck.isSuccess) {
      if (right.foldable) {
        numberFormatter.check()
      } else {
        TypeCheckResult.TypeCheckFailure(s"Format expression must be foldable, but got $right")
      }
    } else {
      inputTypeCheck
    }
  }
  override def prettyName: String = "to_number"
  override def nullSafeEval(string: Any, format: Any): Any = {
    val input = string.asInstanceOf[UTF8String]
    numberFormatter.parse(input)
  }
  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    val builder =
      ctx.addReferenceObj("builder", numberFormatter, classOf[ToNumberParser].getName)
    val eval = left.genCode(ctx)
    ev.copy(code =
      code"""
        |${eval.code}
        |boolean ${ev.isNull} = ${eval.isNull};
        |${CodeGenerator.javaType(dataType)} ${ev.value} = ${CodeGenerator.defaultValue(dataType)};
        |if (!${ev.isNull}) {
        |  ${ev.value} = $builder.parse(${eval.value});
        |}
      """.stripMargin)
  }
  override protected def withNewChildrenInternal(
      newLeft: Expression, newRight: Expression): ToNumber =
    copy(left = newLeft, right = newRight)
}

/**
 * A function that converts strings to decimal values, returning NULL if the input string fails to
 * match the format string.
 */
@ExpressionDescription(
  usage = """
     _FUNC_(expr, fmt) - Convert string 'expr' to a number based on the string format `fmt`.
       Returns NULL if the string 'expr' does not match the expected format. The format follows the
       same semantics as the to_number function.
  """,
  examples = """
    Examples:
      > SELECT _FUNC_('454', '999');
       454
      > SELECT _FUNC_('454.00', '000.00');
       454.00
      > SELECT _FUNC_('12,454', '99,999');
       12454
      > SELECT _FUNC_('$78.12', '$99.99');
       78.12
      > SELECT _FUNC_('12,454.8-', '99,999.9S');
       -12454.8
  """,
  since = "3.3.0",
  group = "string_funcs")
case class TryToNumber(left: Expression, right: Expression)
  extends BinaryExpression with ImplicitCastInputTypes with NullIntolerant {
  private lazy val numberFormat = right.eval().toString.toUpperCase(Locale.ROOT)
  private lazy val numberFormatter = new ToNumberParser(numberFormat, false)

  override def dataType: DataType = numberFormatter.parsedDecimalType
  override def inputTypes: Seq[DataType] = Seq(StringType, StringType)
  override def nullable: Boolean = true
  override def checkInputDataTypes(): TypeCheckResult = ToNumber(left, right).checkInputDataTypes()
  override def prettyName: String = "try_to_number"
  override def nullSafeEval(string: Any, format: Any): Any = {
    val input = string.asInstanceOf[UTF8String]
    numberFormatter.parse(input)
  }
  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    val builder =
      ctx.addReferenceObj("builder", numberFormatter, classOf[ToNumberParser].getName)
    val eval = left.genCode(ctx)
    ev.copy(code =
      code"""
        |${eval.code}
        |boolean ${ev.isNull} = ${eval.isNull};
        |${CodeGenerator.javaType(dataType)} ${ev.value} = ${CodeGenerator.defaultValue(dataType)};
        |if (!${ev.isNull}) {
        |  ${ev.value} = $builder.parse(${eval.value});
        |}
      """.stripMargin)
  }
  override protected def withNewChildrenInternal(
      newLeft: Expression,
      newRight: Expression): TryToNumber =
    copy(left = newLeft, right = newRight)
}

/**
 * A function that converts decimal values to strings, returning NULL if the decimal value fails to
 * match the format string.
 */
@ExpressionDescription(
  usage = """
    _FUNC_(numberExpr, formatExpr) - Convert `numberExpr` to a string based on the `formatExpr`.
      Throws an exception if the conversion fails. The format can consist of the following
      characters, case insensitive:
        '0' or '9': Specifies an expected digit between 0 and 9. A sequence of 0 or 9 in the format
          string matches a sequence of digits in the input value, generating a result string of the
          same length as the corresponding sequence in the format string. The result string is
          left-padded with zeros if the 0/9 sequence comprises more digits than the matching part of
          the decimal value, starts with 0, and is before the decimal point. Otherwise, it is
          padded with spaces.
        '.' or 'D': Specifies the position of the decimal point (optional, only allowed once).
        ',' or 'G': Specifies the position of the grouping (thousands) separator (,). There must be
          a 0 or 9 to the left and right of each grouping separator.
        '$': Specifies the location of the $ currency sign. This character may only be specified
          once.
        'S' or 'MI': Specifies the position of a '-' or '+' sign (optional, only allowed once at
          the beginning or end of the format string). Note that 'S' prints '+' for positive values
          but 'MI' prints a space.
        'PR': Only allowed at the end of the format string; specifies that the result string will be
          wrapped by angle brackets if the input value is negative.
          ('<1>').
  """,
  examples = """
    Examples:
      > SELECT _FUNC_(454, '999');
       454
      > SELECT _FUNC_(454.00, '000D00');
       454.00
      > SELECT _FUNC_(12454, '99G999');
       12,454
      > SELECT _FUNC_(78.12, '$99.99');
       $78.12
      > SELECT _FUNC_(-12454.8, '99G999D9S');
       12,454.8-
  """,
  since = "3.4.0",
  group = "string_funcs")
case class ToCharacter(left: Expression, right: Expression)
  extends BinaryExpression with ImplicitCastInputTypes with NullIntolerant {
  private lazy val numberFormat = right.eval().toString.toUpperCase(Locale.ROOT)
  private lazy val numberFormatter = new ToNumberParser(numberFormat, true)

  override def dataType: DataType = StringType
  override def inputTypes: Seq[AbstractDataType] = Seq(DecimalType, StringType)
  override def checkInputDataTypes(): TypeCheckResult = {
    val inputTypeCheck = super.checkInputDataTypes()
    if (inputTypeCheck.isSuccess) {
      if (right.foldable) {
        numberFormatter.check()
      } else {
        TypeCheckResult.TypeCheckFailure(s"Format expression must be foldable, but got $right")
      }
    } else {
      inputTypeCheck
    }
  }
  override def prettyName: String = "to_char"
  override def nullSafeEval(decimal: Any, format: Any): Any = {
    val input = decimal.asInstanceOf[Decimal]
    numberFormatter.format(input)
  }
  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    val builder =
      ctx.addReferenceObj("builder", numberFormatter, classOf[ToNumberParser].getName)
    val eval = left.genCode(ctx)
    val result =
      code"""
         |${eval.code}
         |boolean ${ev.isNull} = ${eval.isNull};
         |${CodeGenerator.javaType(dataType)} ${ev.value} = ${CodeGenerator.defaultValue(dataType)};
         |if (!${ev.isNull}) {
         |  ${ev.value} = $builder.format(${eval.value});
         |}
      """
    val stripped = result.stripMargin
    ev.copy(code = stripped)
  }
  override protected def withNewChildrenInternal(
      newLeft: Expression, newRight: Expression): ToCharacter =
    copy(left = newLeft, right = newRight)
}

/**
 * A function that converts decimal values to strings, returning NULL if the decimal value fails to
 * match the format string.
 */
@ExpressionDescription(
  usage = """

    _FUNC_(numberExpr, formatExpr) - Convert `numberExpr` to a string based on the `formatExpr`.
      Returns NULL if the conversion fails. The format follows the same semantics as the
      to_char function.
  """,
  examples = """
    Examples:
      > SELECT _FUNC_(454, '999');
       454
      > SELECT _FUNC_(454.00, '000D00');
       454.00
      > SELECT _FUNC_(12454, '99G999');
       12,454
      > SELECT _FUNC_(78.12, '$99.99');
       $78.12
      > SELECT _FUNC_(-12454.8, '99G999D9S');
       12,454.8-
  """,
  since = "3.4.0",
  group = "string_funcs")
case class TryToCharacter(left: Expression, right: Expression)
  extends BinaryExpression with ImplicitCastInputTypes with NullIntolerant {
  private lazy val numberFormat = right.eval().toString.toUpperCase(Locale.ROOT)
  private lazy val numberFormatter = new ToNumberParser(numberFormat, false)

  override def dataType: DataType = StringType
  override def inputTypes: Seq[AbstractDataType] = Seq(DecimalType, StringType)
  override def nullable: Boolean = true
  override def checkInputDataTypes(): TypeCheckResult =
    ToCharacter(left, right).checkInputDataTypes()
  override def prettyName: String = "try_to_char"
  override def nullSafeEval(decimal: Any, format: Any): Any = {
    val input = decimal.asInstanceOf[Decimal]
    numberFormatter.format(input)
  }
  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    val builder =
      ctx.addReferenceObj("builder", numberFormatter, classOf[ToNumberParser].getName)
    val eval = left.genCode(ctx)
    ev.copy(code =
      code"""
         |${eval.code}
         |boolean ${ev.isNull} = ${eval.isNull};
         |${CodeGenerator.javaType(dataType)} ${ev.value} = ${CodeGenerator.defaultValue(dataType)};
         |if (!${ev.isNull}) {
         |  UTF8String result = $builder.format(${eval.value});
         |  if (result == null) {
         |    ${ev.isNull} = true;
         |    ${ev.value} = null;
         |  } else {
         |    ${ev.isNull} = false;
         |    ${ev.value} = result;
         |  }
         |}
      """.stripMargin)
  }

  override protected def withNewChildrenInternal(
      newLeft: Expression,
      newRight: Expression): TryToCharacter =
    copy(left = newLeft, right = newRight)
}
