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
import org.apache.spark.sql.catalyst.ExtendedAnalysisException
import org.apache.spark.sql.catalyst.expressions.{Collation, ExpressionEvalHelper, Literal, StringRepeat, StringReplace}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types.StringType

class CollationStringExpressionsSuite extends QueryTest
  with SharedSparkSession with ExpressionEvalHelper {

  case class CollationTestCase[R](s1: String, s2: String, collation: String, expectedResult: R)
  case class CollationTestFail[R](s1: String, s2: String, collation: String)

  test("Support ConcatWs string expression with Collation") {
    // Supported collations
    val checks = Seq(
      CollationTestCase("Spark", "SQL", "UTF8_BINARY", "Spark SQL")
    )
    checks.foreach(ct => {
      checkAnswer(sql(s"SELECT concat_ws(collate(' ', '${ct.collation}'), " +
        s"collate('${ct.s1}', '${ct.collation}'), collate('${ct.s2}', '${ct.collation}'))"),
        Row(ct.expectedResult))
    })
    // Unsupported collations
    val fails = Seq(
      CollationTestCase("ABC", "%b%", "UTF8_BINARY_LCASE", false),
      CollationTestCase("ABC", "%B%", "UNICODE", true),
      CollationTestCase("ABC", "%b%", "UNICODE_CI", false)
    )
    fails.foreach(ct => {
      val expr = s"concat_ws(collate(' ', '${ct.collation}'), " +
        s"collate('${ct.s1}', '${ct.collation}'), collate('${ct.s2}', '${ct.collation}'))"
      checkError(
        exception = intercept[ExtendedAnalysisException] {
          sql(s"SELECT $expr")
        },
        errorClass = "DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE",
        sqlState = "42K09",
        parameters = Map(
          "sqlExpr" -> s"\"concat_ws(collate( ), collate(${ct.s1}), collate(${ct.s2}))\"",
          "paramIndex" -> "first",
          "inputSql" -> s"\"collate( )\"",
          "inputType" -> s"\"STRING COLLATE ${ct.collation}\"",
          "requiredType" -> "\"STRING\""
        ),
        context = ExpectedContext(
          fragment = s"$expr",
          start = 7,
          stop = 73 + 3 * ct.collation.length
        )
      )
    })
  }

  test("REPLACE check result on explicitly collated strings") {
    def testReplace(source: String, search: String, replace: String,
        collationId: Integer, expected: String): Unit = {
      val sourceLiteral = Literal.create(source, StringType(collationId))
      val searchLiteral = Literal.create(search, StringType(collationId))
      val replaceLiteral = Literal.create(replace, StringType(collationId))

      checkEvaluation(StringReplace(sourceLiteral, searchLiteral, replaceLiteral), expected)
    }

    // scalastyle:off
    // UTF8_BINARY
    testReplace("r世eplace", "pl", "123", 0, "r世e123ace")
    testReplace("replace", "pl", "", 0, "reace")
    testReplace("repl世ace", "Pl", "", 0, "repl世ace")
    testReplace("replace", "", "123", 0, "replace")
    testReplace("abcabc", "b", "12", 0, "a12ca12c")
    testReplace("abcdabcd", "bc", "", 0, "adad")
    // UTF8_BINARY_LCASE
    testReplace("r世eplace", "pl", "xx", 1, "r世exxace")
    testReplace("repl世ace", "PL", "AB", 1, "reAB世ace")
    testReplace("Replace", "", "123", 1, "Replace")
    testReplace("re世place", "世", "x", 1, "rexplace")
    testReplace("abcaBc", "B", "12", 1, "a12ca12c")
    testReplace("AbcdabCd", "Bc", "", 1, "Adad")
    // UNICODE
    testReplace("re世place", "plx", "123", 2, "re世place")
    testReplace("世Replace", "re", "", 2, "世Replace")
    testReplace("replace世", "", "123", 2, "replace世")
    testReplace("aBc世abc", "b", "12", 2, "aBc世a12c")
    testReplace("abcdabcd", "bc", "", 2, "adad")
    // UNICODE_CI
    testReplace("replace", "plx", "123", 3, "replace")
    testReplace("Replace", "re", "", 3, "place")
    testReplace("replace", "", "123", 3, "replace")
    testReplace("aBc世abc", "b", "12", 3, "a12c世a12c")
    testReplace("a世Bcdabcd", "bC", "", 3, "a世dad")
    // scalastyle:on
  }
  
  test("REPEAT check output type on explicitly collated string") {
    def testRepeat(expected: String, collationId: Int, input: String, n: Int): Unit = {
      val s = Literal.create(input, StringType(collationId))

      checkEvaluation(Collation(StringRepeat(s, Literal.create(n))).replacement, expected)
    }

    testRepeat("UTF8_BINARY", 0, "abc", 2)
    testRepeat("UTF8_BINARY_LCASE", 1, "abc", 2)
    testRepeat("UNICODE", 2, "abc", 2)
    testRepeat("UNICODE_CI", 3, "abc", 2)
  }

  // TODO: Add more tests for other string expressions

}

class CollationStringExpressionsANSISuite extends CollationRegexpExpressionsSuite {
  override protected def sparkConf: SparkConf =
    super.sparkConf.set(SQLConf.ANSI_ENABLED, true)
}
