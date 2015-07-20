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

package org.apache.spark.sql.hive.execution

import org.apache.spark.sql.hive.test.TestHive
import org.apache.spark.sql.test.SQLTestUtils
import org.apache.spark.sql.{SQLConf, AnalysisException, QueryTest, Row}
import org.scalatest.BeforeAndAfterAll
import test.org.apache.spark.sql.hive.aggregate2.MyDoubleSum

class Aggregate2Suite extends QueryTest with SQLTestUtils with BeforeAndAfterAll {

  override val sqlContext = TestHive
  import sqlContext.implicits._

  var originalUseAggregate2: Boolean = _

  override def beforeAll(): Unit = {
    originalUseAggregate2 = sqlContext.conf.useSqlAggregate2
    sqlContext.sql("set spark.sql.useAggregate2=true")
    val data1 = Seq[(Integer, Integer)](
      (1, 10),
      (null, -60),
      (1, 20),
      (1, 30),
      (2, 0),
      (null, -10),
      (2, -1),
      (2, null),
      (2, null),
      (null, 100),
      (3, null),
      (null, null),
      (3, null)).toDF("key", "value")
    data1.write.saveAsTable("agg1")

    val data2 = Seq[(Integer, Integer)](
      (1, 10),
      (null, -60),
      (1, 30),
      (1, 30),
      (2, 1),
      (null, -10),
      (2, -1),
      (2, 1),
      (2, null),
      (null, 100),
      (3, null),
      (null, null),
      (3, null)).toDF("key", "value")
    data2.write.saveAsTable("agg2")

    // Register a UDAF
    val javaUDAF = new MyDoubleSum
    sqlContext.udaf.register("mydoublesum", javaUDAF)
  }

  override def afterAll(): Unit = {
    sqlContext.sql("DROP TABLE IF EXISTS agg1")
    sqlContext.sql("DROP TABLE IF EXISTS agg2")
    sqlContext.sql(s"set spark.sql.useAggregate2=$originalUseAggregate2")
  }

  test("only do grouping") {
    checkAnswer(
      sqlContext.sql(
        """
          |SELECT key
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(1) :: Row(2) :: Row(3) :: Row(null) :: Nil)
  }

  test("test average2 no key in output") {
    checkAnswer(
      sqlContext.sql(
        """
          |SELECT avg(value)
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(-0.5) :: Row(20.0) :: Row(null) :: Row(10.0) :: Nil)
  }

  test("test average2") {
    checkAnswer(
      sqlContext.sql(
        """
          |SELECT key, avg(value)
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(1, 20.0) :: Row(2, -0.5) :: Row(3, null) :: Row(null, 10.0) :: Nil)

    checkAnswer(
      sqlContext.sql(
        """
          |SELECT avg(value), key
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(20.0, 1) :: Row(-0.5, 2) :: Row(null, 3) :: Row(10.0, null) :: Nil)

    checkAnswer(
      sqlContext.sql(
        """
          |SELECT avg(value) + 1.5, key + 10
          |FROM agg1
          |GROUP BY key + 10
        """.stripMargin),
      Row(21.5, 11) :: Row(1.0, 12) :: Row(null, 13) :: Row(11.5, null) :: Nil)

    checkAnswer(
      sqlContext.sql(
        """
          |SELECT avg(value) FROM agg1
        """.stripMargin),
      Row(11.125) :: Nil)

    checkAnswer(
      sqlContext.sql(
        """
          |SELECT avg(null)
        """.stripMargin),
      Row(null) :: Nil)
  }

  test("udaf") {
    checkAnswer(
      sqlContext.sql(
        """
          |SELECT
          |  key,
          |  mydoublesum(cast(value as double) + 1.5 * key),
          |  avg(value - key),
          |  mydoublesum(cast(value as double) - 1.5 * key),
          |  avg(value)
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(1, 64.5, 19.0, 55.5, 20.0) ::
        Row(2, 5.0, -2.5, -7.0, -0.5) ::
        Row(3, null, null, null, null) ::
        Row(null, null, null, null, 10.0) :: Nil)
  }

  test("non-AlgebraicAggregate aggreguate function") {
    checkAnswer(
      sqlContext.sql(
        """
          |SELECT mydoublesum(cast(value as double)), key
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(60.0, 1) :: Row(-1.0, 2) :: Row(null, 3) :: Row(30.0, null) :: Nil)

    checkAnswer(
      sqlContext.sql(
        """
          |SELECT mydoublesum(cast(value as double)) FROM agg1
        """.stripMargin),
      Row(89.0) :: Nil)

    checkAnswer(
      sqlContext.sql(
        """
          |SELECT mydoublesum(null)
        """.stripMargin),
      Row(null) :: Nil)
  }

  test("non-AlgebraicAggregate and AlgebraicAggregate aggreguate function") {
    checkAnswer(
      sqlContext.sql(
        """
          |SELECT mydoublesum(cast(value as double)), key, avg(value)
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(60.0, 1, 20.0) ::
        Row(-1.0, 2, -0.5) ::
        Row(null, 3, null) ::
        Row(30.0, null, 10.0) :: Nil)

    checkAnswer(
      sqlContext.sql(
        """
          |SELECT
          |  mydoublesum(cast(value as double) + 1.5 * key),
          |  avg(value - key),
          |  key,
          |  mydoublesum(cast(value as double) - 1.5 * key),
          |  avg(value)
          |FROM agg1
          |GROUP BY key
        """.stripMargin),
      Row(64.5, 19.0, 1, 55.5, 20.0) ::
        Row(5.0, -2.5, 2, -7.0, -0.5) ::
        Row(null, null, 3, null, null) ::
        Row(null, null, null, null, 10.0) :: Nil)
  }

  test("Cannot use AggregateExpression1 and AggregateExpressions2 together") {
    Seq(true, false).foreach { useAggregate2 =>
      sqlContext.sql(s"set spark.sql.useAggregate2=$useAggregate2")
      val errorMessage = intercept[AnalysisException] {
        sqlContext.sql(
          """
            |SELECT
            |  key,
            |  sum(cast(value as double) + 1.5 * key),
            |  mydoublesum(value)
            |FROM agg1
            |GROUP BY key
          """.stripMargin).collect()
      }.getMessage
      val expectedErrorMessage =
        s"${SQLConf.USE_SQL_AGGREGATE2.key} is ${if (useAggregate2) "enabled" else "disabled"}. " +
          s"Please ${if (useAggregate2) "disable" else "enable"} it to use"
      assert(errorMessage.contains(expectedErrorMessage))
    }

    sqlContext.sql(s"set spark.sql.useAggregate2=true")
  }

  test("single distinct column sets") {
    sqlContext.sql(
      """
        |SELECT avg(distinct value) FROM agg2
      """.stripMargin).explain(true)

    sqlContext.sql(
      """
        |SELECT avg(distinct value) FROM agg2
      """.stripMargin).collect.foreach(println)

    // TODO: add both distinct agg non-distinct agg in the same query.
  }
}
