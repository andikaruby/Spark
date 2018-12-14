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

package org.apache.spark.sql.execution.metric

import java.io.File

import scala.util.Random

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.plans.logical.LocalRelation
import org.apache.spark.sql.execution.{FilterExec, RangeExec, SparkPlan, WholeStageCodegenExec}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSQLContext
import org.apache.spark.util.{AccumulatorContext, JsonProtocol}

class SQLMetricsSuite extends SparkFunSuite with SQLMetricsTestUtils with SharedSQLContext {
  import testImplicits._

  /**
   * Generates a `DataFrame` by filling randomly generated bytes for hash collision.
   */
  private def generateRandomBytesDF(numRows: Int = 65535): DataFrame = {
    val random = new Random()
    val manyBytes = (0 until numRows).map { _ =>
      val byteArrSize = random.nextInt(100)
      val bytes = new Array[Byte](byteArrSize)
      random.nextBytes(bytes)
      (bytes, random.nextInt(100))
    }
    manyBytes.toSeq.toDF("a", "b")
  }

  test("LocalTableScanExec computes metrics in collect and take") {
    val df1 = spark.createDataset(Seq(1, 2, 3))
    val logical = df1.queryExecution.logical
    require(logical.isInstanceOf[LocalRelation])
    df1.collect()
    val metrics1 = df1.queryExecution.executedPlan.collectLeaves().head.metrics
    assert(metrics1.contains("numOutputRows"))
    assert(metrics1("numOutputRows").value === 3)

    val df2 = spark.createDataset(Seq(1, 2, 3)).limit(2)
    df2.collect()
    val metrics2 = df2.queryExecution.executedPlan.collectLeaves().head.metrics
    assert(metrics2.contains("numOutputRows"))
    assert(metrics2("numOutputRows").value === 2)
  }

  test("Filter metrics") {
    // Assume the execution plan is
    // PhysicalRDD(nodeId = 1) -> Filter(nodeId = 0)
    val df = person.filter('age < 25)
    testSparkPlanMetrics(df, 1, Map(
      0L -> (("Filter", Map(
        "number of output rows" -> 1L))))
    )
  }

  test("WholeStageCodegen metrics") {
    // Assume the execution plan is
    // WholeStageCodegen(nodeId = 0, Range(nodeId = 2) -> Filter(nodeId = 1))
    // TODO: update metrics in generated operators
    val ds = spark.range(10).filter('id < 5)
    testSparkPlanMetrics(ds.toDF(), 1, Map.empty)
  }

  test("Aggregate metrics") {
    // Assume the execution plan is
    // ... -> HashAggregate(nodeId = 2) -> Exchange(nodeId = 1)
    // -> HashAggregate(nodeId = 0)
    val df = testData2.groupBy().count() // 2 partitions
    val expected1 = Seq(
      Map("number of output rows" -> 2L,
        "avg hash probe (min, med, max)" -> "\n(1, 1, 1)"),
      Map("number of output rows" -> 1L,
        "avg hash probe (min, med, max)" -> "\n(1, 1, 1)"))
    testSparkPlanMetrics(df, 1, Map(
      2L -> (("HashAggregate", expected1(0))),
      0L -> (("HashAggregate", expected1(1))))
    )

    // 2 partitions and each partition contains 2 keys
    val df2 = testData2.groupBy('a).count()
    val expected2 = Seq(
      Map("number of output rows" -> 4L,
        "avg hash probe (min, med, max)" -> "\n(1, 1, 1)"),
      Map("number of output rows" -> 3L,
        "avg hash probe (min, med, max)" -> "\n(1, 1, 1)"))
    testSparkPlanMetrics(df2, 1, Map(
      2L -> (("HashAggregate", expected2(0))),
      0L -> (("HashAggregate", expected2(1))))
    )
  }

  test("Aggregate metrics: track avg probe") {
    // The executed plan looks like:
    // HashAggregate(keys=[a#61], functions=[count(1)], output=[a#61, count#71L])
    // +- Exchange hashpartitioning(a#61, 5)
    //    +- HashAggregate(keys=[a#61], functions=[partial_count(1)], output=[a#61, count#76L])
    //       +- Exchange RoundRobinPartitioning(1)
    //          +- LocalTableScan [a#61]
    //
    // Assume the execution plan with node id is:
    // Wholestage disabled:
    // HashAggregate(nodeId = 0)
    //   Exchange(nodeId = 1)
    //     HashAggregate(nodeId = 2)
    //       Exchange (nodeId = 3)
    //         LocalTableScan(nodeId = 4)
    //
    // Wholestage enabled:
    // WholeStageCodegen(nodeId = 0)
    //   HashAggregate(nodeId = 1)
    //     Exchange(nodeId = 2)
    //       WholeStageCodegen(nodeId = 3)
    //         HashAggregate(nodeId = 4)
    //           Exchange(nodeId = 5)
    //             LocalTableScan(nodeId = 6)
    Seq(true, false).foreach { enableWholeStage =>
      val df = generateRandomBytesDF().repartition(1).groupBy('a).count()
      val nodeIds = if (enableWholeStage) {
        Set(4L, 1L)
      } else {
        Set(2L, 0L)
      }
      val metrics = getSparkPlanMetrics(df, 1, nodeIds, enableWholeStage).get
      nodeIds.foreach { nodeId =>
        val probes = metrics(nodeId)._2("avg hash probe (min, med, max)")
        probes.toString.stripPrefix("\n(").stripSuffix(")").split(", ").foreach { probe =>
          assert(probe.toDouble > 1.0)
        }
      }
    }
  }

  test("ObjectHashAggregate metrics") {
    // Assume the execution plan is
    // ... -> ObjectHashAggregate(nodeId = 2) -> Exchange(nodeId = 1)
    // -> ObjectHashAggregate(nodeId = 0)
    val df = testData2.groupBy().agg(collect_set('a)) // 2 partitions
    testSparkPlanMetrics(df, 1, Map(
      2L -> (("ObjectHashAggregate", Map("number of output rows" -> 2L))),
      0L -> (("ObjectHashAggregate", Map("number of output rows" -> 1L))))
    )

    // 2 partitions and each partition contains 2 keys
    val df2 = testData2.groupBy('a).agg(collect_set('a))
    testSparkPlanMetrics(df2, 1, Map(
      2L -> (("ObjectHashAggregate", Map("number of output rows" -> 4L))),
      0L -> (("ObjectHashAggregate", Map("number of output rows" -> 3L))))
    )
  }

  test("Sort metrics") {
    // Assume the execution plan is
    // WholeStageCodegen(nodeId = 0, Range(nodeId = 2) -> Sort(nodeId = 1))
    val ds = spark.range(10).sort('id)
    testSparkPlanMetrics(ds.toDF(), 2, Map.empty)
  }

  test("SortMergeJoin metrics") {
    // Because SortMergeJoin may skip different rows if the number of partitions is different, this
    // test should use the deterministic number of partitions.
    val testDataForJoin = testData2.filter('a < 2) // TestData2(1, 1) :: TestData2(1, 2)
    testDataForJoin.createOrReplaceTempView("testDataForJoin")
    withTempView("testDataForJoin") {
      // Assume the execution plan is
      // ... -> SortMergeJoin(nodeId = 1) -> TungstenProject(nodeId = 0)
      val df = spark.sql(
        "SELECT * FROM testData2 JOIN testDataForJoin ON testData2.a = testDataForJoin.a")
      testSparkPlanMetrics(df, 1, Map(
        0L -> (("SortMergeJoin", Map(
          // It's 4 because we only read 3 rows in the first partition and 1 row in the second one
          "number of output rows" -> 4L))))
      )
    }
  }

  test("SortMergeJoin(outer) metrics") {
    // Because SortMergeJoin may skip different rows if the number of partitions is different,
    // this test should use the deterministic number of partitions.
    val testDataForJoin = testData2.filter('a < 2) // TestData2(1, 1) :: TestData2(1, 2)
    testDataForJoin.createOrReplaceTempView("testDataForJoin")
    withTempView("testDataForJoin") {
      // Assume the execution plan is
      // ... -> SortMergeJoin(nodeId = 1) -> TungstenProject(nodeId = 0)
      val df = spark.sql(
        "SELECT * FROM testData2 left JOIN testDataForJoin ON testData2.a = testDataForJoin.a")
      testSparkPlanMetrics(df, 1, Map(
        0L -> (("SortMergeJoin", Map(
          // It's 4 because we only read 3 rows in the first partition and 1 row in the second one
          "number of output rows" -> 8L))))
      )

      val df2 = spark.sql(
        "SELECT * FROM testDataForJoin right JOIN testData2 ON testData2.a = testDataForJoin.a")
      testSparkPlanMetrics(df2, 1, Map(
        0L -> (("SortMergeJoin", Map(
          // It's 4 because we only read 3 rows in the first partition and 1 row in the second one
          "number of output rows" -> 8L))))
      )
    }
  }

  test("BroadcastHashJoin metrics") {
    val df1 = Seq((1, "1"), (2, "2")).toDF("key", "value")
    val df2 = Seq((1, "1"), (2, "2"), (3, "3"), (4, "4")).toDF("key", "value")
    // Assume the execution plan is
    // ... -> BroadcastHashJoin(nodeId = 1) -> TungstenProject(nodeId = 0)
    val df = df1.join(broadcast(df2), "key")
    testSparkPlanMetrics(df, 2, Map(
      1L -> (("BroadcastHashJoin", Map(
        "number of output rows" -> 2L))))
    )
  }

  test("ShuffledHashJoin metrics") {
    withSQLConf("spark.sql.autoBroadcastJoinThreshold" -> "40",
        "spark.sql.shuffle.partitions" -> "2",
        "spark.sql.join.preferSortMergeJoin" -> "false") {
      val df1 = Seq((1, "1"), (2, "2")).toDF("key", "value")
      val df2 = (1 to 10).map(i => (i, i.toString)).toSeq.toDF("key", "value")
      // Assume the execution plan is
      // ... -> ShuffledHashJoin(nodeId = 1) -> Project(nodeId = 0)
      val df = df1.join(df2, "key")
      val metrics = getSparkPlanMetrics(df, 1, Set(1L))
      testSparkPlanMetrics(df, 1, Map(
        1L -> (("ShuffledHashJoin", Map(
          "number of output rows" -> 2L))))
      )
    }
  }

  test("BroadcastHashJoin(outer) metrics") {
    val df1 = Seq((1, "a"), (1, "b"), (4, "c")).toDF("key", "value")
    val df2 = Seq((1, "a"), (1, "b"), (2, "c"), (3, "d")).toDF("key2", "value")
    // Assume the execution plan is
    // ... -> BroadcastHashJoin(nodeId = 0)
    val df = df1.join(broadcast(df2), $"key" === $"key2", "left_outer")
    testSparkPlanMetrics(df, 2, Map(
      0L -> (("BroadcastHashJoin", Map(
        "number of output rows" -> 5L))))
    )

    val df3 = df1.join(broadcast(df2), $"key" === $"key2", "right_outer")
    testSparkPlanMetrics(df3, 2, Map(
      0L -> (("BroadcastHashJoin", Map(
        "number of output rows" -> 6L))))
    )
  }

  test("BroadcastNestedLoopJoin metrics") {
    val testDataForJoin = testData2.filter('a < 2) // TestData2(1, 1) :: TestData2(1, 2)
    testDataForJoin.createOrReplaceTempView("testDataForJoin")
    withSQLConf(SQLConf.CROSS_JOINS_ENABLED.key -> "true") {
      withTempView("testDataForJoin") {
        // Assume the execution plan is
        // ... -> BroadcastNestedLoopJoin(nodeId = 1) -> TungstenProject(nodeId = 0)
        val df = spark.sql(
          "SELECT * FROM testData2 left JOIN testDataForJoin ON " +
            "testData2.a * testDataForJoin.a != testData2.a + testDataForJoin.a")
        testSparkPlanMetrics(df, 3, Map(
          1L -> (("BroadcastNestedLoopJoin", Map(
            "number of output rows" -> 12L))))
        )
      }
    }
  }

  test("BroadcastLeftSemiJoinHash metrics") {
    val df1 = Seq((1, "1"), (2, "2")).toDF("key", "value")
    val df2 = Seq((1, "1"), (2, "2"), (3, "3"), (4, "4")).toDF("key2", "value")
    // Assume the execution plan is
    // ... -> BroadcastHashJoin(nodeId = 0)
    val df = df1.join(broadcast(df2), $"key" === $"key2", "leftsemi")
    testSparkPlanMetrics(df, 2, Map(
      0L -> (("BroadcastHashJoin", Map(
        "number of output rows" -> 2L))))
    )
  }

  test("CartesianProduct metrics") {
    withSQLConf(SQLConf.CROSS_JOINS_ENABLED.key -> "true") {
      val testDataForJoin = testData2.filter('a < 2) // TestData2(1, 1) :: TestData2(1, 2)
      testDataForJoin.createOrReplaceTempView("testDataForJoin")
      withTempView("testDataForJoin") {
        // Assume the execution plan is
        // ... -> CartesianProduct(nodeId = 1) -> TungstenProject(nodeId = 0)
        val df = spark.sql(
          "SELECT * FROM testData2 JOIN testDataForJoin")
        testSparkPlanMetrics(df, 1, Map(
          0L -> (("CartesianProduct", Map("number of output rows" -> 12L))))
        )
      }
    }
  }

  test("SortMergeJoin(left-anti) metrics") {
    val anti = testData2.filter("a > 2")
    withTempView("antiData") {
      anti.createOrReplaceTempView("antiData")
      val df = spark.sql(
        "SELECT * FROM testData2 ANTI JOIN antiData ON testData2.a = antiData.a")
      testSparkPlanMetrics(df, 1, Map(
        0L -> (("SortMergeJoin", Map("number of output rows" -> 4L))))
      )
    }
  }

  test("save metrics") {
    withTempPath { file =>
      // person creates a temporary view. get the DF before listing previous execution IDs
      val data = person.select('name)
      val previousExecutionIds = currentExecutionIds()
      // Assume the execution plan is
      // PhysicalRDD(nodeId = 0)
      data.write.format("json").save(file.getAbsolutePath)
      sparkContext.listenerBus.waitUntilEmpty(10000)
      val executionIds = currentExecutionIds().diff(previousExecutionIds)
      assert(executionIds.size === 1)
      val executionId = executionIds.head
      val jobs = statusStore.execution(executionId).get.jobs
      // Use "<=" because there is a race condition that we may miss some jobs
      // TODO Change "<=" to "=" once we fix the race condition that missing the JobStarted event.
      assert(jobs.size <= 1)
      val metricValues = statusStore.executionMetrics(executionId)
      // Because "save" will create a new DataFrame internally, we cannot get the real metric id.
      // However, we still can check the value.
      assert(metricValues.values.toSeq.exists(_ === "2"))
    }
  }

  test("metrics can be loaded by history server") {
    val metric = SQLMetrics.createMetric(sparkContext, "zanzibar")
    metric += 10L
    val metricInfo = metric.toInfo(Some(metric.value), None)
    metricInfo.update match {
      case Some(v: Long) => assert(v === 10L)
      case Some(v) => fail(s"metric value was not a Long: ${v.getClass.getName}")
      case _ => fail("metric update is missing")
    }
    assert(metricInfo.metadata === Some(AccumulatorContext.SQL_ACCUM_IDENTIFIER))
    // After serializing to JSON, the original value type is lost, but we can still
    // identify that it's a SQL metric from the metadata
    val metricInfoJson = JsonProtocol.accumulableInfoToJson(metricInfo)
    val metricInfoDeser = JsonProtocol.accumulableInfoFromJson(metricInfoJson)
    metricInfoDeser.update match {
      case Some(v: String) => assert(v.toLong === 10L)
      case Some(v) => fail(s"deserialized metric value was not a string: ${v.getClass.getName}")
      case _ => fail("deserialized metric update is missing")
    }
    assert(metricInfoDeser.metadata === Some(AccumulatorContext.SQL_ACCUM_IDENTIFIER))
  }

  test("range metrics") {
    val res1 = InputOutputMetricsHelper.run(
      spark.range(30).filter(x => x % 3 == 0).toDF()
    )
    assert(res1 === (30L, 0L, 30L) :: Nil)

    val res2 = InputOutputMetricsHelper.run(
      spark.range(150).repartition(4).filter(x => x < 10).toDF()
    )
    assert(res2 === (150L, 0L, 150L) :: (0L, 150L, 10L) :: Nil)

    withTempDir { tempDir =>
      val dir = new File(tempDir, "pqS").getCanonicalPath

      spark.range(10).write.parquet(dir)
      spark.read.parquet(dir).createOrReplaceTempView("pqS")

      // The executed plan looks like:
      // Exchange RoundRobinPartitioning(2)
      // +- BroadcastNestedLoopJoin BuildLeft, Cross
      //   :- BroadcastExchange IdentityBroadcastMode
      //   :  +- Exchange RoundRobinPartitioning(3)
      //   :     +- *Range (0, 30, step=1, splits=2)
      //   +- *FileScan parquet [id#465L] Batched: true, Format: Parquet, Location: ...(ignored)
      val res3 = InputOutputMetricsHelper.run(
        spark.range(30).repartition(3).crossJoin(sql("select * from pqS")).repartition(2).toDF()
      )
      // The query above is executed in the following stages:
      //   1. range(30)                   => (30, 0, 30)
      //   2. sql("select * from pqS")    => (0, 30, 0)
      //   3. crossJoin(...) of 1. and 2. => (10, 0, 300)
      //   4. shuffle & return results    => (0, 300, 0)
      assert(res3 === (30L, 0L, 30L) :: (0L, 30L, 0L) :: (10L, 0L, 300L) :: (0L, 300L, 0L) :: Nil)
    }
  }

  test("SPARK-25278: output metrics are wrong for plans repeated in the query") {
    val name = "demo_view"
    withView(name) {
      sql(s"CREATE OR REPLACE VIEW $name AS VALUES 1,2")
      val view = spark.table(name)
      val union = view.union(view)
      testSparkPlanMetrics(union, 1, Map(
        0L -> ("Union" -> Map()),
        1L -> ("LocalTableScan" -> Map("number of output rows" -> 2L)),
        2L -> ("LocalTableScan" -> Map("number of output rows" -> 2L))))
    }
  }

  test("writing data out metrics: parquet") {
    testMetricsNonDynamicPartition("parquet", "t1")
  }

  test("writing data out metrics with dynamic partition: parquet") {
    testMetricsDynamicPartition("parquet", "parquet", "t1")
  }

  test("SPARK-25602: SparkPlan.getByteArrayRdd should not consume the input when not necessary") {
    def checkFilterAndRangeMetrics(
        df: DataFrame,
        filterNumOutputs: Int,
        rangeNumOutputs: Int): Unit = {
      var filter: FilterExec = null
      var range: RangeExec = null
      val collectFilterAndRange: SparkPlan => Unit = {
        case f: FilterExec =>
          assert(filter == null, "the query should only have one Filter")
          filter = f
        case r: RangeExec =>
          assert(range == null, "the query should only have one Range")
          range = r
        case _ =>
      }
      if (SQLConf.get.wholeStageEnabled) {
        df.queryExecution.executedPlan.foreach {
          case w: WholeStageCodegenExec =>
            w.child.foreach(collectFilterAndRange)
          case _ =>
        }
      } else {
        df.queryExecution.executedPlan.foreach(collectFilterAndRange)
      }

      assert(filter != null && range != null, "the query doesn't have Filter and Range")
      assert(filter.metrics("numOutputRows").value == filterNumOutputs)
      assert(range.metrics("numOutputRows").value == rangeNumOutputs)
    }

    val df = spark.range(0, 3000, 1, 2).toDF().filter('id % 3 === 0)
    val df2 = df.limit(2)
    Seq(true, false).foreach { wholeStageEnabled =>
      withSQLConf(SQLConf.WHOLESTAGE_CODEGEN_ENABLED.key -> wholeStageEnabled.toString) {
        df.collect()
        checkFilterAndRangeMetrics(df, filterNumOutputs = 1000, rangeNumOutputs = 3000)

        df.queryExecution.executedPlan.foreach(_.resetMetrics())
        // For each partition, we get 2 rows. Then the Filter should produce 2 rows per-partition,
        // and Range should produce 1000 rows (one batch) per-partition. Totally Filter produces
        // 4 rows, and Range produces 2000 rows.
        df.queryExecution.toRdd.mapPartitions(_.take(2)).collect()
        checkFilterAndRangeMetrics(df, filterNumOutputs = 4, rangeNumOutputs = 2000)

        // Top-most limit will call `CollectLimitExec.executeCollect`, which will only run the first
        // task, so totally the Filter produces 2 rows, and Range produces 1000 rows (one batch).
        df2.collect()
        checkFilterAndRangeMetrics(df2, filterNumOutputs = 2, rangeNumOutputs = 1000)
      }
    }
  }
}
