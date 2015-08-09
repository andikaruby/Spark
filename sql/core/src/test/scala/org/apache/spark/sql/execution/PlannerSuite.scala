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

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.TestData._
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.joins.{BroadcastHashJoin, ShuffledHashJoin}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.test.{SQLTestUtils, TestSQLContext}
import org.apache.spark.sql.test.TestSQLContext._
import org.apache.spark.sql.test.TestSQLContext.implicits._
import org.apache.spark.sql.test.TestSQLContext.planner._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SQLContext, Row, SQLConf, execution}


class PlannerSuite extends SparkFunSuite with SQLTestUtils {

  override def sqlContext: SQLContext = TestSQLContext

  private def testPartialAggregationPlan(query: LogicalPlan): Unit = {
    val plannedOption = HashAggregation(query).headOption.orElse(Aggregation(query).headOption)
    val planned =
      plannedOption.getOrElse(
        fail(s"Could query play aggregation query $query. Is it an aggregation query?"))
    val aggregations = planned.collect { case n if n.nodeName contains "Aggregate" => n }

    // For the new aggregation code path, there will be three aggregate operator for
    // distinct aggregations.
    assert(
      aggregations.size == 2 || aggregations.size == 3,
      s"The plan of query $query does not have partial aggregations.")
  }

  test("unions are collapsed") {
    val query = testData.unionAll(testData).unionAll(testData).logicalPlan
    val planned = BasicOperators(query).head
    val logicalUnions = query collect { case u: logical.Union => u }
    val physicalUnions = planned collect { case u: execution.Union => u }

    assert(logicalUnions.size === 2)
    assert(physicalUnions.size === 1)
  }

  test("count is partially aggregated") {
    val query = testData.groupBy('value).agg(count('key)).queryExecution.analyzed
    testPartialAggregationPlan(query)
  }

  test("count distinct is partially aggregated") {
    val query = testData.groupBy('value).agg(countDistinct('key)).queryExecution.analyzed
    testPartialAggregationPlan(query)
  }

  test("mixed aggregates are partially aggregated") {
    val query =
      testData.groupBy('value).agg(count('value), countDistinct('key)).queryExecution.analyzed
    testPartialAggregationPlan(query)
  }

  test("sizeInBytes estimation of limit operator for broadcast hash join optimization") {
    def checkPlan(fieldTypes: Seq[DataType], newThreshold: Int): Unit = {
      setConf(SQLConf.AUTO_BROADCASTJOIN_THRESHOLD, newThreshold)
      val fields = fieldTypes.zipWithIndex.map {
        case (dataType, index) => StructField(s"c${index}", dataType, true)
      } :+ StructField("key", IntegerType, true)
      val schema = StructType(fields)
      val row = Row.fromSeq(Seq.fill(fields.size)(null))
      val rowRDD = org.apache.spark.sql.test.TestSQLContext.sparkContext.parallelize(row :: Nil)
      createDataFrame(rowRDD, schema).registerTempTable("testLimit")

      val planned = sql(
        """
          |SELECT l.a, l.b
          |FROM testData2 l JOIN (SELECT * FROM testLimit LIMIT 1) r ON (l.a = r.key)
        """.stripMargin).queryExecution.executedPlan

      val broadcastHashJoins = planned.collect { case join: BroadcastHashJoin => join }
      val shuffledHashJoins = planned.collect { case join: ShuffledHashJoin => join }

      assert(broadcastHashJoins.size === 1, "Should use broadcast hash join")
      assert(shuffledHashJoins.isEmpty, "Should not use shuffled hash join")

      dropTempTable("testLimit")
    }

    val origThreshold = conf.autoBroadcastJoinThreshold

    val simpleTypes =
      NullType ::
      BooleanType ::
      ByteType ::
      ShortType ::
      IntegerType ::
      LongType ::
      FloatType ::
      DoubleType ::
      DecimalType(10, 5) ::
      DecimalType.SYSTEM_DEFAULT ::
      DateType ::
      TimestampType ::
      StringType ::
      BinaryType :: Nil

    checkPlan(simpleTypes, newThreshold = 16434)

    val complexTypes =
      ArrayType(DoubleType, true) ::
      ArrayType(StringType, false) ::
      MapType(IntegerType, StringType, true) ::
      MapType(IntegerType, ArrayType(DoubleType), false) ::
      StructType(Seq(
        StructField("a", IntegerType, nullable = true),
        StructField("b", ArrayType(DoubleType), nullable = false),
        StructField("c", DoubleType, nullable = false))) :: Nil

    checkPlan(complexTypes, newThreshold = 901617)

    setConf(SQLConf.AUTO_BROADCASTJOIN_THRESHOLD, origThreshold)
  }

  test("InMemoryRelation statistics propagation") {
    val origThreshold = conf.autoBroadcastJoinThreshold
    setConf(SQLConf.AUTO_BROADCASTJOIN_THRESHOLD, 81920)

    testData.limit(3).registerTempTable("tiny")
    sql("CACHE TABLE tiny")

    val a = testData.as("a")
    val b = table("tiny").as("b")
    val planned = a.join(b, $"a.key" === $"b.key").queryExecution.executedPlan

    val broadcastHashJoins = planned.collect { case join: BroadcastHashJoin => join }
    val shuffledHashJoins = planned.collect { case join: ShuffledHashJoin => join }

    assert(broadcastHashJoins.size === 1, "Should use broadcast hash join")
    assert(shuffledHashJoins.isEmpty, "Should not use shuffled hash join")

    setConf(SQLConf.AUTO_BROADCASTJOIN_THRESHOLD, origThreshold)
  }

  test("efficient limit -> project -> sort") {
    val query = testData.sort('key).select('value).limit(2).logicalPlan
    val planned = planner.TakeOrderedAndProject(query)
    assert(planned.head.isInstanceOf[execution.TakeOrderedAndProject])
  }

  test("PartitioningCollection") {
    withTempTable("normal", "small", "tiny") {
      testData.registerTempTable("normal")
      testData.limit(10).registerTempTable("small")
      testData.limit(3).registerTempTable("tiny")

      // Disable broadcast join
      withSQLConf(SQLConf.AUTO_BROADCASTJOIN_THRESHOLD.key -> "-1") {
        val joins = Array("JOIN", "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "FULL OUTER JOIN")
        var i = 0
        while (i < joins.length) {
          var j = 0
          while (j < joins.length) {
            val firstJoin: String = joins(i)
            val secondJoin: String = joins(j)

            {
              val numExchanges: Int = sql(
                s"""
                |SELECT *
                |FROM
                |  normal $firstJoin small ON (normal.key = small.key)
                |  $secondJoin tiny ON (small.key = tiny.key)
              """.stripMargin
              ).queryExecution.executedPlan.collect {
                case exchange: Exchange => exchange
              }.length
              assert(numExchanges === 3)
            }

            {
              val numExchanges: Int = sql(
                s"""
                |SELECT *
                |FROM
                |  normal $firstJoin small ON (normal.key = small.key)
                |  $secondJoin tiny ON (normal.key = tiny.key)
              """.stripMargin
              ).queryExecution.executedPlan.collect {
                case exchange: Exchange => exchange
              }.length
              assert(numExchanges === 3)
            }

            j += 1
          }
          i += 1
        }

        {
          val numExchanges: Int = sql(
            s"""
                |SELECT small.key, count(*)
                |FROM
                |  normal JOIN small ON (normal.key = small.key)
                |  JOIN tiny ON (small.key = tiny.key)
                |GROUP BY
                |  small.key
              """.stripMargin
          ).queryExecution.executedPlan.collect {
            case exchange: Exchange => exchange
          }.length
          assert(numExchanges === 3)
        }

        {
          val numExchanges: Int = sql(
            s"""
                |SELECT normal.key, count(*)
                |FROM
                |  normal LEFT OUTER JOIN small ON (normal.key = small.key)
                |  JOIN tiny ON (small.key = tiny.key)
                |GROUP BY
                |  normal.key
              """.stripMargin
          ).queryExecution.executedPlan.collect {
            case exchange: Exchange => exchange
          }.length
          assert(numExchanges === 3)
        }

        {
          val numExchanges: Int = sql(
            s"""
                |SELECT small.key, count(*)
                |FROM
                |  normal LEFT OUTER JOIN small ON (normal.key = small.key)
                |  JOIN tiny ON (small.key = tiny.key)
                |GROUP BY
                |  small.key
              """.stripMargin
          ).queryExecution.executedPlan.collect {
            case exchange: Exchange => exchange
          }.length
          assert(numExchanges === 4)
        }
      }
    }
  }
}
