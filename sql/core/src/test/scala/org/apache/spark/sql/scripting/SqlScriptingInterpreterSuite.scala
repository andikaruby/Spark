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

package org.apache.spark.sql.scripting

import org.apache.spark.sql.{AnalysisException, QueryTest, Row}
import org.apache.spark.sql.test.SharedSparkSession

/**
 * SQL Scripting interpreter tests.
 * Output from the parser is provided to the interpreter.
 * Output from the interpreter (iterator over executable statements) is then checked - statements
 *   are executed and output DataFrames are compared with expected outputs.
 */
class SqlScriptingInterpreterSuite extends QueryTest with SharedSparkSession {
  // Helpers
  private def verifySqlScriptResult(sqlText: String, expected: Seq[Array[Row]]): Unit = {
    val interpreter = SqlScriptingInterpreter(spark)
    val compoundBody = spark.sessionState.sqlParser.parseScript(sqlText)
    val result = interpreter.execute(compoundBody).toSeq
    assert(result.length == expected.length)
    result.zip(expected).foreach {
      case (actualAnswer, expectedAnswer) =>
        assert(actualAnswer.sameElements(expectedAnswer))
    }
  }

  // Tests
  test("select 1") {
    verifySqlScriptResult("SELECT 1;", Seq(Array(Row(1))))
  }

  test("select 1; select 2;") {
    val sqlScript =
      """
        |BEGIN
        |SELECT 1;
        |SELECT 2;
        |END
        |""".stripMargin
    val expected = Seq(
      Array(Row(1)),
      Array(Row(2))
    )
    verifySqlScriptResult(sqlScript, expected)
  }

  test("multi statement - simple") {
    withTable("t") {
      val sqlScript =
        """
          |BEGIN
          |CREATE TABLE t (a INT, b STRING, c DOUBLE) USING parquet;
          |INSERT INTO t VALUES (1, 'a', 1.0);
          |SELECT a, b FROM t WHERE a = 12;
          |SELECT a FROM t;
          |END
          |""".stripMargin
      val expected = Seq(
        Array.empty[Row], // create table
        Array.empty[Row], // insert
        Array.empty[Row], // select with filter
        Array(Row(1)) // select
      )
      verifySqlScriptResult(sqlScript, expected)
    }
  }

  test("multi statement - count") {
    withTable("t") {
      val sqlScript =
        """
          |BEGIN
          |CREATE TABLE t (a INT, b STRING, c DOUBLE) USING parquet;
          |INSERT INTO t VALUES (1, 'a', 1.0);
          |INSERT INTO t VALUES (1, 'a', 1.0);
          |SELECT
          | CASE WHEN COUNT(*) > 10 THEN true
          | ELSE false
          | END AS MoreThanTen
          |FROM t;
          |END
          |""".stripMargin
      val expected = Seq(
        Array.empty[Row], // create table
        Array.empty[Row], // insert #1
        Array.empty[Row], // insert #2
        Array(Row(false)) // select
      )
      verifySqlScriptResult(sqlScript, expected)
    }
  }

  test("session vars - set and read") {
    val sqlScript =
      """
        |BEGIN
        |DECLARE var = 1;
        |SET VAR var = var + 1;
        |SELECT var;
        |END
        |""".stripMargin
    val expected = Seq(
      Array.empty[Row], // declare var
      Array.empty[Row], // set var
      Array(Row(2)), // select
    )
    verifySqlScriptResult(sqlScript, expected)
  }

  test("session vars - set and read scoped") {
    val sqlScript =
      """
        |BEGIN
        | BEGIN
        |   DECLARE var = 1;
        |   SELECT var;
        | END;
        | BEGIN
        |   DECLARE var = 2;
        |   SELECT var;
        | END;
        | BEGIN
        |   DECLARE var = 3;
        |   SET VAR var = var + 1;
        |   SELECT var;
        | END;
        |END
        |""".stripMargin
    val expected = Seq(
      Array.empty[Row], // declare var
      Array(Row(1)), // select
      Array.empty[Row], // declare var
      Array(Row(2)), // select
      Array.empty[Row], // declare var
      Array.empty[Row], // set var
      Array(Row(4)), // select
    )
    verifySqlScriptResult(sqlScript, expected)
  }

  test("session vars - var out of scope") {
    val varName: String = "testVarName"
    val e = intercept[AnalysisException] {
      val sqlScript =
        s"""
          |BEGIN
          | BEGIN
          |   DECLARE $varName = 1;
          |   SELECT $varName;
          | END;
          | SELECT $varName;
          |END
          |""".stripMargin
      verifySqlScriptResult(sqlScript, Seq.empty)
    }
    checkError(
      exception = e,
      errorClass = "UNRESOLVED_COLUMN.WITHOUT_SUGGESTION",
      sqlState = "42703",
      parameters = Map("objectName" -> s"`$varName`"),
      context = ExpectedContext(
        fragment = s"$varName",
        start = 79,
        stop = 89)
    )
  }

  test("session vars - drop var statement") {
    val sqlScript =
      """
        |BEGIN
        |DECLARE var = 1;
        |SET VAR var = var + 1;
        |SELECT var;
        |DROP TEMPORARY VARIABLE var;
        |END
        |""".stripMargin
    val expected = Seq(
      Array.empty[Row], // declare var
      Array.empty[Row], // set var
      Array(Row(2)), // select
      Array.empty[Row], // drop var - explicit
    )
    verifySqlScriptResult(sqlScript, expected)
  }
}
