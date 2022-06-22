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

package org.apache.spark.sql.errors

import org.apache.spark.sql.{AnalysisException, ClassData, IntegratedUDFTestUtils, QueryTest}
import org.apache.spark.sql.functions.{grouping, grouping_id, sum}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSparkSession

case class StringLongClass(a: String, b: Long)

case class StringIntClass(a: String, b: Int)

case class ComplexClass(a: Long, b: StringLongClass)

case class ArrayClass(arr: Seq[StringIntClass])

class QueryCompilationErrorsSuite extends QueryTest with SharedSparkSession {
  import testImplicits._

  test("CANNOT_UP_CAST_DATATYPE: invalid upcast data type") {
    val msg1 = intercept[AnalysisException] {
      sql("select 'value1' as a, 1L as b").as[StringIntClass]
    }.message
    assert(msg1 ===
      s"""
         |Cannot up cast b from "BIGINT" to "INT".
         |The type path of the target object is:
         |- field (class: "scala.Int", name: "b")
         |- root class: "org.apache.spark.sql.errors.StringIntClass"
         |You can either add an explicit cast to the input data or choose a higher precision type
       """.stripMargin.trim + " of the field in the target object")

    val msg2 = intercept[AnalysisException] {
      sql("select 1L as a," +
        " named_struct('a', 'value1', 'b', cast(1.0 as decimal(38,18))) as b")
        .as[ComplexClass]
    }.message
    assert(msg2 ===
      s"""
         |Cannot up cast b.`b` from "DECIMAL(38,18)" to "BIGINT".
         |The type path of the target object is:
         |- field (class: "scala.Long", name: "b")
         |- field (class: "org.apache.spark.sql.errors.StringLongClass", name: "b")
         |- root class: "org.apache.spark.sql.errors.ComplexClass"
         |You can either add an explicit cast to the input data or choose a higher precision type
       """.stripMargin.trim + " of the field in the target object")
  }

  test("UNSUPPORTED_GROUPING_EXPRESSION: filter with grouping/grouping_Id expression") {
    val df = Seq(
      (536361, "85123A", 2, 17850),
      (536362, "85123B", 4, 17850),
      (536363, "86123A", 6, 17851)
    ).toDF("InvoiceNo", "StockCode", "Quantity", "CustomerID")
    Seq("grouping", "grouping_id").foreach { grouping =>
      val errMsg = intercept[AnalysisException] {
        df.groupBy("CustomerId").agg(Map("Quantity" -> "max"))
          .filter(s"$grouping(CustomerId)=17850")
      }
      assert(errMsg.message ===
        "grouping()/grouping_id() can only be used with GroupingSets/Cube/Rollup")
      assert(errMsg.errorClass === Some("UNSUPPORTED_GROUPING_EXPRESSION"))
    }
  }

  test("UNSUPPORTED_GROUPING_EXPRESSION: Sort with grouping/grouping_Id expression") {
    val df = Seq(
      (536361, "85123A", 2, 17850),
      (536362, "85123B", 4, 17850),
      (536363, "86123A", 6, 17851)
    ).toDF("InvoiceNo", "StockCode", "Quantity", "CustomerID")
    Seq(grouping("CustomerId"), grouping_id("CustomerId")).foreach { grouping =>
      val errMsg = intercept[AnalysisException] {
        df.groupBy("CustomerId").agg(Map("Quantity" -> "max")).
          sort(grouping)
      }
      assert(errMsg.errorClass === Some("UNSUPPORTED_GROUPING_EXPRESSION"))
      assert(errMsg.message ===
        "grouping()/grouping_id() can only be used with GroupingSets/Cube/Rollup")
    }
  }

  test("INVALID_PARAMETER_VALUE: the argument_index of string format is invalid") {
    withSQLConf(SQLConf.ALLOW_ZERO_INDEX_IN_FORMAT_STRING.key -> "false") {
      val e = intercept[AnalysisException] {
        sql("select format_string('%0$s', 'Hello')")
      }
      assert(e.errorClass === Some("INVALID_PARAMETER_VALUE"))
      assert(e.message === "The value of parameter(s) 'strfmt' in `format_string` is invalid: " +
        "expects %1$, %2$ and so on, but got %0$.")
    }
  }

  test("CANNOT_USE_MIXTURE: Using aggregate function with grouped aggregate pandas UDF") {
    import IntegratedUDFTestUtils._
    assume(shouldTestGroupedAggPandasUDFs)

    val df = Seq(
      (536361, "85123A", 2, 17850),
      (536362, "85123B", 4, 17850),
      (536363, "86123A", 6, 17851)
    ).toDF("InvoiceNo", "StockCode", "Quantity", "CustomerID")
    val e = intercept[AnalysisException] {
      val pandasTestUDF = TestGroupedAggPandasUDF(name = "pandas_udf")
      df.groupBy("CustomerId")
        .agg(pandasTestUDF(df("Quantity")), sum(df("Quantity"))).collect()
    }

    assert(e.errorClass === Some("CANNOT_USE_MIXTURE"))
    assert(e.message ===
      "Cannot use a mixture of aggregate function and group aggregate pandas UDF")
  }

  test("UNSUPPORTED_FEATURE: Using Python UDF with unsupported join condition") {
    import IntegratedUDFTestUtils._

    val df1 = Seq(
      (536361, "85123A", 2, 17850),
      (536362, "85123B", 4, 17850),
      (536363, "86123A", 6, 17851)
    ).toDF("InvoiceNo", "StockCode", "Quantity", "CustomerID")
    val df2 = Seq(
      ("Bob", 17850),
      ("Alice", 17850),
      ("Tom", 17851)
    ).toDF("CustomerName", "CustomerID")

    val e = intercept[AnalysisException] {
      val pythonTestUDF = TestPythonUDF(name = "python_udf")
      df1.join(
        df2, pythonTestUDF(df1("CustomerID") === df2("CustomerID")), "leftouter").collect()
    }

    assert(e.errorClass === Some("UNSUPPORTED_FEATURE"))
    assert(e.getSqlState === "0A000")
    assert(e.message ===
      "The feature is not supported: " +
      "Using PythonUDF in join condition of join type LEFT OUTER is not supported.")
  }

  test("UNSUPPORTED_FEATURE: Using pandas UDF aggregate expression with pivot") {
    import IntegratedUDFTestUtils._
    assume(shouldTestGroupedAggPandasUDFs)

    val df = Seq(
      (536361, "85123A", 2, 17850),
      (536362, "85123B", 4, 17850),
      (536363, "86123A", 6, 17851)
    ).toDF("InvoiceNo", "StockCode", "Quantity", "CustomerID")

    val e = intercept[AnalysisException] {
      val pandasTestUDF = TestGroupedAggPandasUDF(name = "pandas_udf")
      df.groupBy(df("CustomerID")).pivot(df("CustomerID")).agg(pandasTestUDF(df("Quantity")))
    }

    assert(e.errorClass === Some("UNSUPPORTED_FEATURE"))
    assert(e.getSqlState === "0A000")
    assert(e.message ===
      "The feature is not supported: " +
      "Pandas UDF aggregate expressions don't support pivot.")
  }

  test("UNSUPPORTED_DESERIALIZER: data type mismatch") {
    val e = intercept[AnalysisException] {
      sql("select 1 as arr").as[ArrayClass]
    }
    assert(e.errorClass === Some("UNSUPPORTED_DESERIALIZER"))
    assert(e.message ===
      """The deserializer is not supported: need a(n) "ARRAY" field but got "INT".""")
  }

  test("UNSUPPORTED_DESERIALIZER:" +
    "the real number of fields doesn't match encoder schema") {
    val ds = Seq(ClassData("a", 1), ClassData("b", 2)).toDS()

    val e1 = intercept[AnalysisException] {
      ds.as[(String, Int, Long)]
    }
    assert(e1.errorClass === Some("UNSUPPORTED_DESERIALIZER"))
    assert(e1.message ===
      "The deserializer is not supported: try to map \"STRUCT<a: STRING, b: INT>\" " +
      "to Tuple3, but failed as the number of fields does not line up.")

    val e2 = intercept[AnalysisException] {
      ds.as[Tuple1[String]]
    }
    assert(e2.errorClass === Some("UNSUPPORTED_DESERIALIZER"))
    assert(e2.message ===
      "The deserializer is not supported: try to map \"STRUCT<a: STRING, b: INT>\" " +
      "to Tuple1, but failed as the number of fields does not line up.")
  }

  test("UNSUPPORTED_GENERATOR: " +
    "generators are not supported when it's nested in expressions") {
    val e = intercept[AnalysisException](
      sql("""select explode(Array(1, 2, 3)) + 1""").collect()
    )
    assert(e.errorClass === Some("UNSUPPORTED_GENERATOR"))
    assert(e.message ===
      """The generator is not supported: """ +
      """nested in expressions "(explode(array(1, 2, 3)) + 1)"""")
  }

  test("UNSUPPORTED_GENERATOR: only one generator allowed") {
    val e = intercept[AnalysisException](
      sql("""select explode(Array(1, 2, 3)), explode(Array(1, 2, 3))""").collect()
    )
    assert(e.errorClass === Some("UNSUPPORTED_GENERATOR"))
    assert(e.message ===
      "The generator is not supported: only one generator allowed per select clause " +
      """but found 2: "explode(array(1, 2, 3))", "explode(array(1, 2, 3))"""")
  }

  test("UNSUPPORTED_GENERATOR: generators are not supported outside the SELECT clause") {
    val e = intercept[AnalysisException](
      sql("""select 1 from t order by explode(Array(1, 2, 3))""").collect()
    )
    assert(e.errorClass === Some("UNSUPPORTED_GENERATOR"))
    assert(e.message ===
      "The generator is not supported: outside the SELECT clause, found: " +
      "'Sort [explode(array(1, 2, 3)) ASC NULLS FIRST], true")
  }

  test("UNSUPPORTED_GENERATOR: not a generator") {
    val e = intercept[AnalysisException](
      sql(
        """
          |SELECT explodedvalue.*
          |FROM VALUES array(1, 2, 3) AS (value)
          |LATERAL VIEW array_contains(value, 1) AS explodedvalue""".stripMargin).collect()
    )
    assert(e.errorClass === Some("UNSUPPORTED_GENERATOR"))
    assert(e.message ===
      """The generator is not supported: `array_contains` is expected to be a generator. """ +
      "However, its class is org.apache.spark.sql.catalyst.expressions.ArrayContains, " +
      "which is not a generator.")
  }
}
