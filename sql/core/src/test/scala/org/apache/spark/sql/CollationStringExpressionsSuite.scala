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

import scala.collection.immutable.Seq

import org.apache.spark.SparkConf
import org.apache.spark.sql.catalyst.expressions.ExpressionEvalHelper
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types.{ArrayType, BinaryType, BooleanType, DataType, IntegerType, StringType}

class CollationStringExpressionsSuite
  extends QueryTest
  with SharedSparkSession
  with ExpressionEvalHelper {

  test("Support ConcatWs string expression with collation") {
    // Supported collations
    case class ConcatWsTestCase[R](s: String, a: Array[String], c: String, result: R)
    val testCases = Seq(
      ConcatWsTestCase(" ", Array("Spark", "SQL"), "UTF8_BINARY", "Spark SQL")
    )
    testCases.foreach(t => {
      val arrCollated = t.a.map(s => s"collate('$s', '${t.c}')").mkString(", ")
      var query = s"SELECT concat_ws(collate('${t.s}', '${t.c}'), $arrCollated)"
      // Result & data type
      checkAnswer(sql(query), Row(t.result))
      assert(sql(query).schema.fields.head.dataType.sameType(StringType(t.c)))
      // Implicit casting
      val arr = t.a.map(s => s"'$s'").mkString(", ")
      query = s"SELECT concat_ws(collate('${t.s}', '${t.c}'), $arr)"
      checkAnswer(sql(query), Row(t.result))
      assert(sql(query).schema.fields.head.dataType.sameType(StringType(t.c)))
      query = s"SELECT concat_ws('${t.s}', $arrCollated)"
      checkAnswer(sql(query), Row(t.result))
      assert(sql(query).schema.fields.head.dataType.sameType(StringType(t.c)))
    })
    // Unsupported collations
    case class ConcatWsTestFail(s: String, a: Array[String], c: String)
    val failCases = Seq(
      ConcatWsTestFail(" ", Array("ABC", "%b%"), "UTF8_BINARY_LCASE"),
      ConcatWsTestFail(" ", Array("ABC", "%B%"), "UNICODE"),
      ConcatWsTestFail(" ", Array("ABC", "%b%"), "UNICODE_CI")
    )
    failCases.foreach(t => {
      val arrCollated = t.a.map(s => s"collate('$s', '${t.c}')").mkString(", ")
      val query = s"SELECT concat_ws(collate('${t.s}', '${t.c}'), $arrCollated)"
      val unsupportedCollation = intercept[AnalysisException] { sql(query) }
      assert(unsupportedCollation.getErrorClass === "DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE")
    })
    // Collation mismatch
    val collationMismatch = intercept[AnalysisException] {
      sql("SELECT concat_ws(' ',collate('Spark', 'UTF8_BINARY_LCASE'),collate('SQL', 'UNICODE'))")
    }
    assert(collationMismatch.getErrorClass === "COLLATION_MISMATCH.EXPLICIT")
  }

  test("Support Contains string expression with collation") {
    // Supported collations
    case class ContainsTestCase[R](l: String, r: String, c: String, result: R)
    val testCases = Seq(
      ContainsTestCase("", "", "UTF8_BINARY", true),
      ContainsTestCase("abcde", "C", "UNICODE", false),
      ContainsTestCase("abcde", "FGH", "UTF8_BINARY_LCASE", false),
      ContainsTestCase("abcde", "BCD", "UNICODE_CI", true)
    )
    testCases.foreach(t => {
      val query = s"SELECT contains(collate('${t.l}','${t.c}'),collate('${t.r}','${t.c}'))"
      // Result & data type
      checkAnswer(sql(query), Row(t.result))
      assert(sql(query).schema.fields.head.dataType.sameType(BooleanType))
      // Implicit casting
      checkAnswer(sql(s"SELECT contains(collate('${t.l}','${t.c}'),'${t.r}')"), Row(t.result))
      checkAnswer(sql(s"SELECT contains('${t.l}',collate('${t.r}','${t.c}'))"), Row(t.result))
    })
    // Collation mismatch
    val collationMismatch = intercept[AnalysisException] {
      sql("SELECT contains(collate('abcde','UTF8_BINARY_LCASE'),collate('C','UNICODE_CI'))")
    }
    assert(collationMismatch.getErrorClass === "COLLATION_MISMATCH.EXPLICIT")
  }

  test("Support StartsWith string expression with collation") {
    // Supported collations
    case class StartsWithTestCase[R](l: String, r: String, c: String, result: R)
    val testCases = Seq(
      StartsWithTestCase("", "", "UTF8_BINARY", true),
      StartsWithTestCase("abcde", "A", "UNICODE", false),
      StartsWithTestCase("abcde", "FGH", "UTF8_BINARY_LCASE", false),
      StartsWithTestCase("abcde", "ABC", "UNICODE_CI", true)
    )
    testCases.foreach(t => {
      val query = s"SELECT startswith(collate('${t.l}','${t.c}'),collate('${t.r}','${t.c}'))"
      // Result & data type
      checkAnswer(sql(query), Row(t.result))
      assert(sql(query).schema.fields.head.dataType.sameType(BooleanType))
      // Implicit casting
      checkAnswer(sql(s"SELECT startswith(collate('${t.l}', '${t.c}'),'${t.r}')"), Row(t.result))
      checkAnswer(sql(s"SELECT startswith('${t.l}', collate('${t.r}', '${t.c}'))"), Row(t.result))
    })
    // Collation mismatch
    val collationMismatch = intercept[AnalysisException] {
      sql("SELECT startswith(collate('abcde', 'UTF8_BINARY_LCASE'),collate('C', 'UNICODE_CI'))")
    }
    assert(collationMismatch.getErrorClass === "COLLATION_MISMATCH.EXPLICIT")
  }

  test("Support EndsWith string expression with collation") {
    // Supported collations
    case class EndsWithTestCase[R](l: String, r: String, c: String, result: R)
    val testCases = Seq(
      EndsWithTestCase("", "", "UTF8_BINARY", true),
      EndsWithTestCase("abcde", "E", "UNICODE", false),
      EndsWithTestCase("abcde", "FGH", "UTF8_BINARY_LCASE", false),
      EndsWithTestCase("abcde", "CDE", "UNICODE_CI", true)
    )
    testCases.foreach(t => {
      val query = s"SELECT endswith(collate('${t.l}', '${t.c}'), collate('${t.r}', '${t.c}'))"
      // Result & data type
      checkAnswer(sql(query), Row(t.result))
      assert(sql(query).schema.fields.head.dataType.sameType(BooleanType))
      // Implicit casting
      checkAnswer(sql(s"SELECT endswith(collate('${t.l}', '${t.c}'),'${t.r}')"), Row(t.result))
      checkAnswer(sql(s"SELECT endswith('${t.l}', collate('${t.r}', '${t.c}'))"), Row(t.result))
    })
    // Collation mismatch
    val collationMismatch = intercept[AnalysisException] {
      sql("SELECT endswith(collate('abcde', 'UTF8_BINARY_LCASE'),collate('C', 'UNICODE_CI'))")
    }
    assert(collationMismatch.getErrorClass === "COLLATION_MISMATCH.EXPLICIT")
  }

  test("Support StringRepeat string expression with collation") {
    // Supported collations
    case class StringRepeatTestCase[R](s: String, n: Int, c: String, result: R)
    val testCases = Seq(
      StringRepeatTestCase("", 1, "UTF8_BINARY", ""),
      StringRepeatTestCase("a", 0, "UNICODE", ""),
      StringRepeatTestCase("XY", 3, "UTF8_BINARY_LCASE", "XYXYXY"),
      StringRepeatTestCase("123", 2, "UNICODE_CI", "123123")
    )
    testCases.foreach(t => {
      val query = s"SELECT repeat(collate('${t.s}', '${t.c}'), ${t.n})"
      // Result & data type
      checkAnswer(sql(query), Row(t.result))
      assert(sql(query).schema.fields.head.dataType.sameType(StringType(t.c)))
    })
  }

  test("Ascii & UnBase64 expressions with collation") {
    case class AsciiUnBase64TestCase[R](q: String, dt: DataType, r: R)
    val testCases = Seq(
      AsciiUnBase64TestCase("select ascii('a' collate utf8_binary)", IntegerType, Row(97)),
      AsciiUnBase64TestCase("select ascii('a' collate utf8_binary_lcase)", IntegerType, Row(97)),
      AsciiUnBase64TestCase("select unbase64('YQ==' collate utf8_binary)", BinaryType,
        Row(Seq(97))),
      AsciiUnBase64TestCase("select unbase64('YQ==' collate utf8_binary_lcase)", BinaryType,
        Row(Seq(97)))
    )
    testCases.foreach(t => {
      // Result & data type
      checkAnswer(sql(t.q), t.r)
      assert(sql(t.q).schema.fields.head.dataType.sameType(t.dt))
    })
  }

  test("Chr, Base64, Decode & FormatNumber expressions with collation") {
    case class DefaultCollationTestCase[R](q: String, c: String, r: R)
    val testCases = Seq(
      DefaultCollationTestCase("select chr(97)", "UTF8_BINARY", Row("a")),
      DefaultCollationTestCase("select chr(97)", "UTF8_BINARY_LCASE", Row("a")),
      DefaultCollationTestCase("select base64('a')", "UTF8_BINARY", Row("YQ==")),
      DefaultCollationTestCase("select base64('a')", "UTF8_BINARY_LCASE", Row("YQ==")),
      DefaultCollationTestCase("select decode(encode('a', 'utf-8'), 'utf-8')", "UTF8_BINARY",
        Row("a")),
      DefaultCollationTestCase("select decode(encode('a', 'utf-8'), 'utf-8')",
        "UTF8_BINARY_LCASE", Row("a")),
      DefaultCollationTestCase("select format_number(123.123, '###.###')", "UTF8_BINARY",
        Row("123.123")),
      DefaultCollationTestCase("select format_number(123.123, '###.###')", "UTF8_BINARY_LCASE",
        Row("123.123"))
    )
    testCases.foreach(t => {
      withSQLConf(SQLConf.DEFAULT_COLLATION.key -> t.c) {
        // Result & data type
        checkAnswer(sql(t.q), t.r)
        assert(sql(t.q).schema.fields.head.dataType.sameType(StringType(t.c)))
      }
    })
  }

  test("Encode, ToBinary & Sentences expressions with collation") {
    case class EncodeToBinarySentencesTestCase[R](q: String, dt: DataType, r: R)
    val testCases = Seq(
      EncodeToBinarySentencesTestCase("select encode('a' collate utf8_binary, 'utf-8')",
        BinaryType, Row(Seq(97))),
      EncodeToBinarySentencesTestCase("select encode('a' collate utf8_binary_lcase, 'utf-8')",
        BinaryType, Row(Seq(97))),
      EncodeToBinarySentencesTestCase("select to_binary('a' collate utf8_binary, 'utf-8')",
        BinaryType, Row(Seq(97))),
      EncodeToBinarySentencesTestCase("select to_binary('a' collate utf8_binary_lcase, 'utf-8')",
        BinaryType, Row(Seq(97))),
      EncodeToBinarySentencesTestCase(
        """
          |select sentences('Hello, world! Nice day.' collate utf8_binary)
          |""".stripMargin,
        ArrayType(ArrayType(StringType)), Row(Seq(Seq("Hello", "world"), Seq("Nice", "day")))),
      EncodeToBinarySentencesTestCase(
        """
          |select sentences('Hello, world! Nice day.' collate utf8_binary_lcase)
          |""".stripMargin,
        ArrayType(ArrayType(StringType("UTF8_BINARY_LCASE"))),
        Row(Seq(Seq("Hello", "world"), Seq("Nice", "day"))))
    )
    testCases.foreach(t => {
      // Result & data type
      checkAnswer(sql(t.q), t.r)
      assert(sql(t.q).schema.fields.head.dataType.sameType(t.dt))
    })
  }

  // TODO: Add more tests for other string expressions

}

class CollationStringExpressionsANSISuite extends CollationStringExpressionsSuite {
  override protected def sparkConf: SparkConf =
    super.sparkConf.set(SQLConf.ANSI_ENABLED, true)

  // TODO: If needed, add more tests for other string expressions (with ANSI mode enabled)

}
