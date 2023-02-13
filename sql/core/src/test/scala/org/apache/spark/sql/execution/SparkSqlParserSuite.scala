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

package org.apache.spark.sql.execution

import scala.collection.JavaConverters._

import org.apache.spark.SparkThrowable
import org.apache.spark.internal.config.ConfigEntry
import org.apache.spark.sql.catalyst.{FunctionIdentifier, TableIdentifier}
import org.apache.spark.sql.catalyst.analysis.{AnalysisTest, UnresolvedAlias, UnresolvedAttribute, UnresolvedFunction, UnresolvedGenerator, UnresolvedHaving, UnresolvedRelation, UnresolvedStar}
import org.apache.spark.sql.catalyst.expressions.{Ascending, AttributeReference, Concat, GreaterThan, Literal, NullsFirst, SortOrder, UnresolvedWindowExpression, UnspecifiedFrame, WindowSpecDefinition, WindowSpecReference}
import org.apache.spark.sql.catalyst.parser.ParseException
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.connector.catalog.TableCatalog
import org.apache.spark.sql.execution.command._
import org.apache.spark.sql.execution.datasources.{CreateTempViewUsing, RefreshResource}
import org.apache.spark.sql.internal.StaticSQLConf
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types.StringType

/**
 * Parser test cases for rules defined in [[SparkSqlParser]].
 *
 * See [[org.apache.spark.sql.catalyst.parser.PlanParserSuite]] for rules
 * defined in the Catalyst module.
 */
class SparkSqlParserSuite extends AnalysisTest with SharedSparkSession {
  import org.apache.spark.sql.catalyst.dsl.expressions._

  private lazy val parser = new SparkSqlParser()

  private def assertEqual(sqlCommand: String, plan: LogicalPlan): Unit = {
    comparePlans(parser.parsePlan(sqlCommand), plan)
  }

  private def parseException(sqlText: String): SparkThrowable = {
    intercept[ParseException](sql(sqlText).collect())
  }

  test("Checks if SET/RESET can parse all the configurations") {
    // Force to build static SQL configurations
    StaticSQLConf
    ConfigEntry.knownConfigs.values.asScala.foreach { config =>
      assertEqual(s"SET ${config.key}", SetCommand(Some(config.key -> None)))
      assertEqual(s"SET `${config.key}`", SetCommand(Some(config.key -> None)))

      val defaultValueStr = config.defaultValueString
      if (config.defaultValue.isDefined && defaultValueStr != null) {
        assertEqual(s"SET ${config.key}=`$defaultValueStr`",
          SetCommand(Some(config.key -> Some(defaultValueStr))))
        assertEqual(s"SET `${config.key}`=`$defaultValueStr`",
          SetCommand(Some(config.key -> Some(defaultValueStr))))

        if (!defaultValueStr.contains(";")) {
          assertEqual(s"SET ${config.key}=$defaultValueStr",
            SetCommand(Some(config.key -> Some(defaultValueStr))))
          assertEqual(s"SET `${config.key}`=$defaultValueStr",
            SetCommand(Some(config.key -> Some(defaultValueStr))))
        }
      }
      assertEqual(s"RESET ${config.key}", ResetCommand(Some(config.key)))
    }
  }

  test("SET with comment") {
    assertEqual(s"SET my_path = /a/b/*", SetCommand(Some("my_path" -> Some("/a/b/*"))))

    checkError(
      exception = parseException("SET k=`v` /*"),
      errorClass = "UNCLOSED_BRACKETED_COMMENT",
      parameters = Map.empty)

    checkError(
      exception = parseException("SET `k`=`v` /*"),
      errorClass = "UNCLOSED_BRACKETED_COMMENT",
      parameters = Map.empty)
  }

  test("Report Error for invalid usage of SET command") {
    assertEqual("SET", SetCommand(None))
    assertEqual("SET -v", SetCommand(Some("-v", None)))
    assertEqual("SET spark.sql.key", SetCommand(Some("spark.sql.key" -> None)))
    assertEqual("SET  spark.sql.key   ", SetCommand(Some("spark.sql.key" -> None)))
    assertEqual("SET spark:sql:key=false", SetCommand(Some("spark:sql:key" -> Some("false"))))
    assertEqual("SET spark:sql:key=", SetCommand(Some("spark:sql:key" -> Some(""))))
    assertEqual("SET spark:sql:key=  ", SetCommand(Some("spark:sql:key" -> Some(""))))
    assertEqual("SET spark:sql:key=-1 ", SetCommand(Some("spark:sql:key" -> Some("-1"))))
    assertEqual("SET spark:sql:key = -1", SetCommand(Some("spark:sql:key" -> Some("-1"))))
    assertEqual("SET 1.2.key=value", SetCommand(Some("1.2.key" -> Some("value"))))
    assertEqual("SET spark.sql.3=4", SetCommand(Some("spark.sql.3" -> Some("4"))))
    assertEqual("SET 1:2:key=value", SetCommand(Some("1:2:key" -> Some("value"))))
    assertEqual("SET spark:sql:3=4", SetCommand(Some("spark:sql:3" -> Some("4"))))
    assertEqual("SET 5=6", SetCommand(Some("5" -> Some("6"))))
    assertEqual("SET spark:sql:key = va l u  e ",
      SetCommand(Some("spark:sql:key" -> Some("va l u  e"))))
    assertEqual("SET `spark.sql.    key`=value",
      SetCommand(Some("spark.sql.    key" -> Some("value"))))
    assertEqual("SET `spark.sql.    key`= v  a lu e ",
      SetCommand(Some("spark.sql.    key" -> Some("v  a lu e"))))
    assertEqual("SET `spark.sql.    key`=  -1",
      SetCommand(Some("spark.sql.    key" -> Some("-1"))))
    assertEqual("SET key=", SetCommand(Some("key" -> Some(""))))

    val sql1 = "SET spark.sql.key value"
    checkError(
      exception = parseException(sql1),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql1,
        start = 0,
        stop = 22))

    val sql2 = "SET spark.sql.key   'value'"
    checkError(
      exception = parseException(sql2),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql2,
        start = 0,
        stop = 26))

    val sql3 = "SET    spark.sql.key \"value\" "
    checkError(
      exception = parseException(sql3),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = "SET    spark.sql.key \"value\"",
        start = 0,
        stop = 27))

    val sql4 = "SET spark.sql.key value1 value2"
    checkError(
      exception = parseException(sql4),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql4,
        start = 0,
        stop = 30))

    val sql5 = "SET spark.   sql.key=value"
    checkError(
      exception = parseException(sql5),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql5,
        start = 0,
        stop = 25))

    val sql6 = "SET spark   :sql:key=value"
    checkError(
      exception = parseException(sql6),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql6,
        start = 0,
        stop = 25))

    val sql7 = "SET spark .  sql.key=value"
    checkError(
      exception = parseException(sql7),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql7,
        start = 0,
        stop = 25))

    val sql8 = "SET spark.sql.   key=value"
    checkError(
      exception = parseException(sql8),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql8,
        start = 0,
        stop = 25))

    val sql9 = "SET spark.sql   :key=value"
    checkError(
      exception = parseException(sql9),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql9,
        start = 0,
        stop = 25))

    val sql10 = "SET spark.sql .  key=value"
    checkError(
      exception = parseException(sql10),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql10,
        start = 0,
        stop = 25))

    val sql11 = "SET ="
    checkError(
      exception = parseException(sql11),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql11,
        start = 0,
        stop = 4))

    val sql12 = "SET =value"
    checkError(
      exception = parseException(sql12),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql12,
        start = 0,
        stop = 9))
  }

  test("Report Error for invalid usage of RESET command") {
    assertEqual("RESET", ResetCommand(None))
    assertEqual("RESET spark.sql.key", ResetCommand(Some("spark.sql.key")))
    assertEqual("RESET  spark.sql.key  ", ResetCommand(Some("spark.sql.key")))
    assertEqual("RESET 1.2.key ", ResetCommand(Some("1.2.key")))
    assertEqual("RESET spark.sql.3", ResetCommand(Some("spark.sql.3")))
    assertEqual("RESET 1:2:key ", ResetCommand(Some("1:2:key")))
    assertEqual("RESET spark:sql:3", ResetCommand(Some("spark:sql:3")))
    assertEqual("RESET `spark.sql.    key`", ResetCommand(Some("spark.sql.    key")))

    val sql1 = "RESET spark.sql.key1 key2"
    checkError(
      exception = parseException(sql1),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql1,
        start = 0,
        stop = 24))

    val sql2 = "RESET spark.  sql.key1 key2"
    checkError(
      exception = parseException(sql2),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql2,
        start = 0,
        stop = 26))

    val sql3 = "RESET spark.sql.key1 key2 key3"
    checkError(
      exception = parseException(sql3),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql3,
        start = 0,
        stop = 29))

    val sql4 = "RESET spark:   sql:key"
    checkError(
      exception = parseException(sql4),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql4,
        start = 0,
        stop = 21))

    val sql5 = "RESET spark   .sql.key"
    checkError(
      exception = parseException(sql5),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql5,
        start = 0,
        stop = 21))

    val sql6 = "RESET spark :  sql:key"
    checkError(
      exception = parseException(sql6),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql6,
        start = 0,
        stop = 21))

    val sql7 = "RESET spark.sql:   key"
    checkError(
      exception = parseException(sql7),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql7,
        start = 0,
        stop = 21))

    val sql8 = "RESET spark.sql   .key"
    checkError(
      exception = parseException(sql8),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql8,
        start = 0,
        stop = 21))

    val sql9 = "RESET spark.sql :  key"
    checkError(
      exception = parseException(sql9),
      errorClass = "_LEGACY_ERROR_TEMP_0043",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql9,
        start = 0,
        stop = 21))
  }

  test("SPARK-33419: Semicolon handling in SET command") {
    assertEqual("SET a=1;", SetCommand(Some("a" -> Some("1"))))
    assertEqual("SET a=1;;", SetCommand(Some("a" -> Some("1"))))

    assertEqual("SET a=`1`;", SetCommand(Some("a" -> Some("1"))))
    assertEqual("SET a=`1;`", SetCommand(Some("a" -> Some("1;"))))
    assertEqual("SET a=`1;`;", SetCommand(Some("a" -> Some("1;"))))

    assertEqual("SET `a`=1;;", SetCommand(Some("a" -> Some("1"))))
    assertEqual("SET `a`=`1;`", SetCommand(Some("a" -> Some("1;"))))
    assertEqual("SET `a`=`1;`;", SetCommand(Some("a" -> Some("1;"))))

    val sql1 = "SET a=1; SELECT 1"
    checkError(
      exception = parseException(sql1),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = sql1,
        start = 0,
        stop = 16))

    val sql2 = "SET a=1;2;;"
    checkError(
      exception = parseException(sql2),
      errorClass = "INVALID_SET_SYNTAX",
      parameters = Map.empty,
      context = ExpectedContext(
        fragment = "SET a=1;2",
        start = 0,
        stop = 8))

    val sql3 = "SET a b=`1;;`"
    checkError(
      exception = parseException(sql3),
      errorClass = "INVALID_PROPERTY_KEY",
      parameters = Map("key" -> "\"a b\"", "value" -> "\"1;;\""),
      context = ExpectedContext(
        fragment = sql3,
        start = 0,
        stop = 12))

    val sql4 = "SET `a`=1;2;;"
    checkError(
      exception = parseException(sql4),
      errorClass = "INVALID_PROPERTY_VALUE",
      parameters = Map("value" -> "\"1;2;;\"", "key" -> "\"a\""),
      context = ExpectedContext(
        fragment = "SET `a`=1;2",
        start = 0,
        stop = 10))
  }

  test("refresh resource") {
    assertEqual("REFRESH prefix_path", RefreshResource("prefix_path"))
    assertEqual("REFRESH /", RefreshResource("/"))
    assertEqual("REFRESH /path///a", RefreshResource("/path///a"))
    assertEqual("REFRESH pat1h/112/_1a", RefreshResource("pat1h/112/_1a"))
    assertEqual("REFRESH pat1h/112/_1a/a-1", RefreshResource("pat1h/112/_1a/a-1"))
    assertEqual("REFRESH path-with-dash", RefreshResource("path-with-dash"))
    assertEqual("REFRESH \'path with space\'", RefreshResource("path with space"))
    assertEqual("REFRESH \"path with space 2\"", RefreshResource("path with space 2"))

    val errMsg1 =
      "REFRESH statements cannot contain ' ', '\\n', '\\r', '\\t' inside unquoted resource paths"
    val sql1 = "REFRESH a b"
    checkError(
      exception = parseException(sql1),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg1),
      context = ExpectedContext(
        fragment = sql1,
        start = 0,
        stop = 10))

    val sql2 = "REFRESH a\tb"
    checkError(
      exception = parseException(sql2),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg1),
      context = ExpectedContext(
        fragment = sql2,
        start = 0,
        stop = 10))

    val sql3 = "REFRESH a\nb"
    checkError(
      exception = parseException(sql3),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg1),
      context = ExpectedContext(
        fragment = sql3,
        start = 0,
        stop = 10))

    val sql4 = "REFRESH a\rb"
    checkError(
      exception = parseException(sql4),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg1),
      context = ExpectedContext(
        fragment = sql4,
        start = 0,
        stop = 10))

    val sql5 = "REFRESH a\r\nb"
    checkError(
      exception = parseException(sql5),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg1),
      context = ExpectedContext(
        fragment = sql5,
        start = 0,
        stop = 11))

    val sql6 = "REFRESH @ $a$"
    checkError(
      exception = parseException(sql6),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg1),
      context = ExpectedContext(
        fragment = sql6,
        start = 0,
        stop = 12))

    val errMsg2 = "Resource paths cannot be empty in REFRESH statements. Use / to match everything"
    val sql7 = "REFRESH  "
    checkError(
      exception = parseException(sql7),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg2),
      context = ExpectedContext(
        fragment = "REFRESH",
        start = 0,
        stop = 6))

    val sql8 = "REFRESH"
    checkError(
      exception = parseException(sql8),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg2),
      context = ExpectedContext(
        fragment = sql8,
        start = 0,
        stop = 6))
  }

  test("SPARK-33118 CREATE TEMPORARY TABLE with LOCATION") {
    assertEqual("CREATE TEMPORARY TABLE t USING parquet OPTIONS (path '/data/tmp/testspark1')",
      CreateTempViewUsing(TableIdentifier("t", None), None, false, false, "parquet",
        Map("path" -> "/data/tmp/testspark1")))
    assertEqual("CREATE TEMPORARY TABLE t USING parquet LOCATION '/data/tmp/testspark1'",
      CreateTempViewUsing(TableIdentifier("t", None), None, false, false, "parquet",
        Map("path" -> "/data/tmp/testspark1")))
  }

  test("describe query") {
    val query = "SELECT * FROM t"
    assertEqual("DESCRIBE QUERY " + query, DescribeQueryCommand(query, parser.parsePlan(query)))
    assertEqual("DESCRIBE " + query, DescribeQueryCommand(query, parser.parsePlan(query)))
  }

  test("query organization") {
    // Test all valid combinations of order by/sort by/distribute by/cluster by/limit/windows
    val baseSql = "select * from t"
    val basePlan =
      Project(Seq(UnresolvedStar(None)), UnresolvedRelation(TableIdentifier("t")))

    assertEqual(s"$baseSql distribute by a, b",
      RepartitionByExpression(UnresolvedAttribute("a") :: UnresolvedAttribute("b") :: Nil,
        basePlan,
        None))
    assertEqual(s"$baseSql distribute by a sort by b",
      Sort(SortOrder(UnresolvedAttribute("b"), Ascending) :: Nil,
        global = false,
        RepartitionByExpression(UnresolvedAttribute("a") :: Nil,
          basePlan,
          None)))
    assertEqual(s"$baseSql cluster by a, b",
      Sort(SortOrder(UnresolvedAttribute("a"), Ascending) ::
          SortOrder(UnresolvedAttribute("b"), Ascending) :: Nil,
        global = false,
        RepartitionByExpression(UnresolvedAttribute("a") :: UnresolvedAttribute("b") :: Nil,
          basePlan,
          None)))
  }

  test("pipeline concatenation") {
    val concat = Concat(
      Concat(UnresolvedAttribute("a") :: UnresolvedAttribute("b") :: Nil) ::
      UnresolvedAttribute("c") ::
      Nil
    )
    assertEqual(
      "SELECT a || b || c FROM t",
      Project(UnresolvedAlias(concat) :: Nil, UnresolvedRelation(TableIdentifier("t"))))
  }

  test("database and schema tokens are interchangeable") {
    assertEqual("CREATE DATABASE foo", parser.parsePlan("CREATE SCHEMA foo"))
    assertEqual("DROP DATABASE foo", parser.parsePlan("DROP SCHEMA foo"))
    assertEqual("ALTER DATABASE foo SET DBPROPERTIES ('x' = 'y')",
      parser.parsePlan("ALTER SCHEMA foo SET DBPROPERTIES ('x' = 'y')"))
    assertEqual("DESC DATABASE foo", parser.parsePlan("DESC SCHEMA foo"))
  }

  test("manage resources") {
    assertEqual("ADD FILE abc.txt", AddFilesCommand(Seq("abc.txt")))
    assertEqual("ADD FILE 'abc.txt'", AddFilesCommand(Seq("abc.txt")))
    assertEqual("ADD FILE \"/path/to/abc.txt\"", AddFilesCommand("/path/to/abc.txt"::Nil))
    assertEqual("LIST FILE abc.txt", ListFilesCommand(Array("abc.txt")))
    assertEqual("LIST FILE '/path//abc.txt'", ListFilesCommand(Array("/path//abc.txt")))
    assertEqual("LIST FILE \"/path2/abc.txt\"", ListFilesCommand(Array("/path2/abc.txt")))
    assertEqual("ADD JAR /path2/_2/abc.jar", AddJarsCommand(Seq("/path2/_2/abc.jar")))
    assertEqual("ADD JAR '/test/path_2/jar/abc.jar'",
      AddJarsCommand(Seq("/test/path_2/jar/abc.jar")))
    assertEqual("ADD JAR \"abc.jar\"", AddJarsCommand(Seq("abc.jar")))
    assertEqual("LIST JAR /path-with-dash/abc.jar",
      ListJarsCommand(Array("/path-with-dash/abc.jar")))
    assertEqual("LIST JAR 'abc.jar'", ListJarsCommand(Array("abc.jar")))
    assertEqual("LIST JAR \"abc.jar\"", ListJarsCommand(Array("abc.jar")))
    assertEqual("ADD FILE '/path with space/abc.txt'",
      AddFilesCommand(Seq("/path with space/abc.txt")))
    assertEqual("ADD JAR '/path with space/abc.jar'",
      AddJarsCommand(Seq("/path with space/abc.jar")))
  }

  test("SPARK-32608: script transform with row format delimit") {
    val rowFormat =
      """
        |  ROW FORMAT DELIMITED
        |  FIELDS TERMINATED BY ','
        |  COLLECTION ITEMS TERMINATED BY '#'
        |  MAP KEYS TERMINATED BY '@'
        |  LINES TERMINATED BY '\n'
        |  NULL DEFINED AS 'null'
      """.stripMargin

    val ioSchema =
      ScriptInputOutputSchema(
        Seq(("TOK_TABLEROWFORMATFIELD", ","),
          ("TOK_TABLEROWFORMATCOLLITEMS", "#"),
          ("TOK_TABLEROWFORMATMAPKEYS", "@"),
          ("TOK_TABLEROWFORMATNULL", "null"),
          ("TOK_TABLEROWFORMATLINES", "\n")),
        Seq(("TOK_TABLEROWFORMATFIELD", ","),
          ("TOK_TABLEROWFORMATCOLLITEMS", "#"),
          ("TOK_TABLEROWFORMATMAPKEYS", "@"),
          ("TOK_TABLEROWFORMATNULL", "null"),
          ("TOK_TABLEROWFORMATLINES", "\n")), None, None,
        List.empty, List.empty, None, None, false)

    assertEqual(
      s"""
         |SELECT TRANSFORM(a, b, c)
         |  $rowFormat
         |  USING 'cat' AS (a, b, c)
         |  $rowFormat
         |FROM testData
      """.stripMargin,
      ScriptTransformation(
        "cat",
        Seq(AttributeReference("a", StringType)(),
          AttributeReference("b", StringType)(),
          AttributeReference("c", StringType)()),
        Project(Seq($"a", $"b", $"c"),
          UnresolvedRelation(TableIdentifier("testData"))),
        ioSchema))

    assertEqual(
      s"""
         |SELECT TRANSFORM(a, sum(b), max(c))
         |  $rowFormat
         |  USING 'cat' AS (a, b, c)
         |  $rowFormat
         |FROM testData
         |GROUP BY a
         |HAVING sum(b) > 10
      """.stripMargin,
      ScriptTransformation(
        "cat",
        Seq(AttributeReference("a", StringType)(),
          AttributeReference("b", StringType)(),
          AttributeReference("c", StringType)()),
        UnresolvedHaving(
          GreaterThan(
            UnresolvedFunction("sum", Seq(UnresolvedAttribute("b")), isDistinct = false),
            Literal(10)),
          Aggregate(
            Seq($"a"),
            Seq(
              $"a",
              UnresolvedAlias(
                UnresolvedFunction("sum", Seq(UnresolvedAttribute("b")), isDistinct = false), None),
              UnresolvedAlias(
                UnresolvedFunction("max", Seq(UnresolvedAttribute("c")), isDistinct = false), None)
            ),
            UnresolvedRelation(TableIdentifier("testData")))),
        ioSchema))

    assertEqual(
      s"""
         |SELECT TRANSFORM(a, sum(b) OVER w, max(c) OVER w)
         |  $rowFormat
         |  USING 'cat' AS (a, b, c)
         |  $rowFormat
         |FROM testData
         |WINDOW w AS (PARTITION BY a ORDER BY b)
      """.stripMargin,
      ScriptTransformation(
        "cat",
        Seq(AttributeReference("a", StringType)(),
          AttributeReference("b", StringType)(),
          AttributeReference("c", StringType)()),
        WithWindowDefinition(
          Map("w" -> WindowSpecDefinition(
            Seq($"a"),
            Seq(SortOrder($"b", Ascending, NullsFirst, Seq.empty)),
            UnspecifiedFrame)),
          Project(
            Seq(
              $"a",
              UnresolvedAlias(
                UnresolvedWindowExpression(
                  UnresolvedFunction("sum", Seq(UnresolvedAttribute("b")), isDistinct = false),
                  WindowSpecReference("w")), None),
              UnresolvedAlias(
                UnresolvedWindowExpression(
                  UnresolvedFunction("max", Seq(UnresolvedAttribute("c")), isDistinct = false),
                  WindowSpecReference("w")), None)
            ),
            UnresolvedRelation(TableIdentifier("testData")))),
        ioSchema))

    assertEqual(
      s"""
         |SELECT TRANSFORM(a, sum(b), max(c))
         |  $rowFormat
         |  USING 'cat' AS (a, b, c)
         |  $rowFormat
         |FROM testData
         |LATERAL VIEW explode(array(array(1,2,3))) myTable AS myCol
         |LATERAL VIEW explode(myTable.myCol) myTable2 AS myCol2
         |GROUP BY a, myCol, myCol2
         |HAVING sum(b) > 10
      """.stripMargin,
      ScriptTransformation(
        "cat",
        Seq(AttributeReference("a", StringType)(),
          AttributeReference("b", StringType)(),
          AttributeReference("c", StringType)()),
        UnresolvedHaving(
          GreaterThan(
            UnresolvedFunction("sum", Seq(UnresolvedAttribute("b")), isDistinct = false),
            Literal(10)),
          Aggregate(
            Seq($"a", $"myCol", $"myCol2"),
            Seq(
              $"a",
              UnresolvedAlias(
                UnresolvedFunction("sum", Seq(UnresolvedAttribute("b")), isDistinct = false), None),
              UnresolvedAlias(
                UnresolvedFunction("max", Seq(UnresolvedAttribute("c")), isDistinct = false), None)
            ),
            Generate(
              UnresolvedGenerator(
                FunctionIdentifier("explode"),
                Seq(UnresolvedAttribute("myTable.myCol"))),
              Nil, false, Option("mytable2"), Seq($"myCol2"),
              Generate(
                UnresolvedGenerator(
                  FunctionIdentifier("explode"),
                  Seq(UnresolvedFunction("array",
                    Seq(
                      UnresolvedFunction("array", Seq(Literal(1), Literal(2), Literal(3)), false)),
                    false))),
                Nil, false, Option("mytable"), Seq($"myCol"),
                UnresolvedRelation(TableIdentifier("testData")))))),
        ioSchema))
  }

  test("SPARK-32607: Script Transformation ROW FORMAT DELIMITED" +
    " `TOK_TABLEROWFORMATLINES` only support '\\n'") {

    val errMsg = "LINES TERMINATED BY only supports newline '\\n' right now: @"
    // test input format TOK_TABLEROWFORMATLINES
    val sql1 =
      s"""SELECT TRANSFORM(a, b, c, d, e)
         |  ROW FORMAT DELIMITED
         |  FIELDS TERMINATED BY ','
         |  LINES TERMINATED BY '@'
         |  NULL DEFINED AS 'null'
         |  USING 'cat' AS (value)
         |  ROW FORMAT DELIMITED
         |  FIELDS TERMINATED BY '&'
         |  LINES TERMINATED BY '\n'
         |  NULL DEFINED AS 'NULL'
         |FROM v""".stripMargin
    checkError(
      exception = parseException(sql1),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg),
      context = ExpectedContext(
        fragment = sql1,
        start = 0,
        stop = 264))

    // test output format TOK_TABLEROWFORMATLINES
    val sql2 =
      s"""SELECT TRANSFORM(a, b, c, d, e)
         |  ROW FORMAT DELIMITED
         |  FIELDS TERMINATED BY ','
         |  LINES TERMINATED BY '\n'
         |  NULL DEFINED AS 'null'
         |  USING 'cat' AS (value)
         |  ROW FORMAT DELIMITED
         |  FIELDS TERMINATED BY '&'
         |  LINES TERMINATED BY '@'
         |  NULL DEFINED AS 'NULL'
         |FROM v""".stripMargin
    checkError(
      exception = parseException(sql2),
      errorClass = "_LEGACY_ERROR_TEMP_0064",
      parameters = Map("msg" -> errMsg),
      context = ExpectedContext(
        fragment = sql2,
        start = 0,
        stop = 264))
  }

  test("CLEAR CACHE") {
    assertEqual("CLEAR CACHE", ClearCacheCommand)
  }

  test("CREATE TABLE LIKE COMMAND should reject reserved properties") {
    val sql1 =
      s"CREATE TABLE target LIKE source TBLPROPERTIES (${TableCatalog.PROP_OWNER}='howdy')"
    checkError(
      exception = parseException(sql1),
      errorClass = "UNSUPPORTED_FEATURE.SET_TABLE_PROPERTY",
      parameters = Map("property" -> TableCatalog.PROP_OWNER,
        "msg" -> "it will be set to the current user"),
      context = ExpectedContext(
        fragment = sql1,
        start = 0,
        stop = 60))

    val sql2 =
      s"CREATE TABLE target LIKE source TBLPROPERTIES (${TableCatalog.PROP_PROVIDER}='howdy')"
    checkError(
      exception = parseException(sql2),
      errorClass = "UNSUPPORTED_FEATURE.SET_TABLE_PROPERTY",
      parameters = Map("property" -> TableCatalog.PROP_PROVIDER,
        "msg" -> "please use the USING clause to specify it"),
      context = ExpectedContext(
        fragment = sql2,
        start = 0,
        stop = 63))
  }
}
