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

package org.apache.spark.sql.streaming

import scala.collection.mutable.ArrayBuffer

import org.apache.spark.sql._
import org.apache.spark.sql.execution.streaming._
import org.apache.spark.sql.sources.StreamSourceProvider
import org.apache.spark.sql.test.SharedSQLContext
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.apache.spark.util.ManualClock

class StreamSuite extends StreamTest {

  import testImplicits._

  test("map with recovery") {
    val inputData = MemoryStream[Int]
    val mapped = inputData.toDS().map(_ + 1)

    testStream(mapped)(
      AddData(inputData, 1, 2, 3),
      StartStream(),
      CheckAnswer(2, 3, 4),
      StopStream,
      AddData(inputData, 4, 5, 6),
      StartStream(),
      CheckAnswer(2, 3, 4, 5, 6, 7))
  }

  test("join") {
    // Make a table and ensure it will be broadcast.
    val smallTable = Seq((1, "one"), (2, "two"), (4, "four")).toDF("number", "word")

    // Join the input stream with a table.
    val inputData = MemoryStream[Int]
    val joined = inputData.toDS().toDF().join(smallTable, $"value" === $"number")

    testStream(joined)(
      AddData(inputData, 1, 2, 3),
      CheckAnswer(Row(1, 1, "one"), Row(2, 2, "two")),
      AddData(inputData, 4),
      CheckAnswer(Row(1, 1, "one"), Row(2, 2, "two"), Row(4, 4, "four")))
  }

  test("union two streams") {
    val inputData1 = MemoryStream[Int]
    val inputData2 = MemoryStream[Int]

    val unioned = inputData1.toDS().union(inputData2.toDS())

    testStream(unioned)(
      AddData(inputData1, 1, 3, 5),
      CheckAnswer(1, 3, 5),
      AddData(inputData2, 2, 4, 6),
      CheckAnswer(1, 2, 3, 4, 5, 6),
      StopStream,
      AddData(inputData1, 7),
      StartStream(),
      AddData(inputData2, 8),
      CheckAnswer(1, 2, 3, 4, 5, 6, 7, 8))
  }

  test("sql queries") {
    val inputData = MemoryStream[Int]
    inputData.toDF().createOrReplaceTempView("stream")
    val evens = sql("SELECT * FROM stream WHERE value % 2 = 0")

    testStream(evens)(
      AddData(inputData, 1, 2, 3, 4),
      CheckAnswer(2, 4))
  }

  test("DataFrame reuse") {
    def assertDF(df: DataFrame) {
      withTempDir { outputDir =>
        withTempDir { checkpointDir =>
          val query = df.writeStream.format("parquet")
            .option("checkpointLocation", checkpointDir.getAbsolutePath)
            .start(outputDir.getAbsolutePath)
          try {
            query.processAllAvailable()
            val outputDf = spark.read.parquet(outputDir.getAbsolutePath).as[Long]
            checkDataset[Long](outputDf, (0L to 10L).toArray: _*)
          } finally {
            query.stop()
          }
        }
      }
    }

    val df = spark.readStream.format(classOf[FakeDefaultSource].getName).load()
    assertDF(df)
    assertDF(df)
  }

  test("unsupported queries") {
    val streamInput = MemoryStream[Int]
    val batchInput = Seq(1, 2, 3).toDS()

    def assertError(expectedMsgs: Seq[String])(body: => Unit): Unit = {
      val e = intercept[AnalysisException] {
        body
      }
      expectedMsgs.foreach { s => assert(e.getMessage.contains(s)) }
    }

    // Running streaming plan as a batch query
    assertError("start" :: Nil) {
      streamInput.toDS.map { i => i }.count()
    }

    // Running non-streaming plan with as a streaming query
    assertError("without streaming sources" :: "start" :: Nil) {
      val ds = batchInput.map { i => i }
      testStream(ds)()
    }

    // Running streaming plan that cannot be incrementalized
    assertError("not supported" :: "streaming" :: Nil) {
      val ds = streamInput.toDS.map { i => i }.sort()
      testStream(ds)()
    }
  }

  test("minimize delay between batch construction and execution") {

    // For each batch, we would retrieve new data's offsets and log them before we run the execution
    // This checks whether the key of the offset log is the expected batch id
    def CheckOffsetLogLatestBatchId(expectedId: Int): AssertOnQuery =
      AssertOnQuery(_.offsetLog.getLatest().get._1 == expectedId,
        s"offsetLog's latest should be $expectedId")

    // For each batch, we would log the state change during the execution
    // This checks whether the key of the state change log is the expected batch id
    def CheckIncrementalExecutionCurrentBatchId(expectedId: Int): AssertOnQuery =
      AssertOnQuery(_.lastExecution.asInstanceOf[IncrementalExecution].currentBatchId == expectedId,
        s"lastExecution's currentBatchId should be $expectedId")

    // For each batch, we would log the sink change after the execution
    // This checks whether the key of the sink change log is the expected batch id
    def CheckSinkLatestBatchId(expectedId: Int): AssertOnQuery =
      AssertOnQuery(_.sink.asInstanceOf[MemorySink].latestBatchId.get == expectedId,
        s"sink's lastBatchId should be $expectedId")

    val inputData = MemoryStream[Int]
    testStream(inputData.toDS())(
      StartStream(ProcessingTime("10 seconds"), new ManualClock),

      /* -- batch 0 ----------------------- */
      // Add some data in batch 0
      AddData(inputData, 1, 2, 3),
      AdvanceManualClock(10 * 1000), // 10 seconds

      /* -- batch 1 ----------------------- */
      // Check the results of batch 0
      CheckAnswer(1, 2, 3),
      CheckIncrementalExecutionCurrentBatchId(0),
      CheckOffsetLogLatestBatchId(0),
      CheckSinkLatestBatchId(0),
      // Add some data in batch 1
      AddData(inputData, 4, 5, 6),
      AdvanceManualClock(10 * 1000),

      /* -- batch _ ----------------------- */
      // Check the results of batch 1
      CheckAnswer(1, 2, 3, 4, 5, 6),
      CheckIncrementalExecutionCurrentBatchId(1),
      CheckOffsetLogLatestBatchId(1),
      CheckSinkLatestBatchId(1),

      AdvanceManualClock(10 * 1000),
      AdvanceManualClock(10 * 1000),
      AdvanceManualClock(10 * 1000),

      /* -- batch __ ---------------------- */
      // Check the results of batch 1 again; this is to make sure that, when there's no new data,
      // the currentId does not get logged (e.g. as 2) even if the clock has advanced many times
      CheckAnswer(1, 2, 3, 4, 5, 6),
      CheckIncrementalExecutionCurrentBatchId(1),
      CheckOffsetLogLatestBatchId(1),
      CheckSinkLatestBatchId(1),

      /* Stop then restart the Stream  */
      StopStream,
      StartStream(ProcessingTime("10 seconds"), new ManualClock),

      /* -- batch 1 rerun ----------------- */
      // this batch 1 would re-run because the latest batch id logged in offset log is 1
      AdvanceManualClock(10 * 1000),

      /* -- batch 2 ----------------------- */
      // Check the results of batch 1
      CheckAnswer(1, 2, 3, 4, 5, 6),
      CheckIncrementalExecutionCurrentBatchId(1),
      CheckOffsetLogLatestBatchId(1),
      CheckSinkLatestBatchId(1),
      // Add some data in batch 2
      AddData(inputData, 7, 8, 9),
      AdvanceManualClock(10 * 1000),

      /* -- batch 3 ----------------------- */
      // Check the results of batch 2
      CheckAnswer(1, 2, 3, 4, 5, 6, 7, 8, 9),
      CheckIncrementalExecutionCurrentBatchId(2),
      CheckOffsetLogLatestBatchId(2),
      CheckSinkLatestBatchId(2))
  }

  test("insert an extraStrategy") {
    try {
      spark.experimental.extraStrategies = TestStrategy :: Nil

      val inputData = MemoryStream[(String, Int)]
      val df = inputData.toDS().map(_._1).toDF("a")

      testStream(df)(
        AddData(inputData, ("so slow", 1)),
        CheckAnswer("so fast"))
    } finally {
      spark.experimental.extraStrategies = Nil
    }
  }

  test("output mode API in Scala") {
    val o1 = OutputMode.Append
    assert(o1 === InternalOutputModes.Append)
    val o2 = OutputMode.Complete
    assert(o2 === InternalOutputModes.Complete)
  }

  test("explain") {
    val inputData = MemoryStream[String]
    val df = inputData.toDS().map(_ + "foo")
    // Test `explain` not throwing errors
    df.explain()
    val q = df.writeStream.queryName("memory_explain").format("memory").start()
      .asInstanceOf[StreamExecution]
    try {
      assert("No physical plan. Waiting for data." === q.explainInternal(false))
      assert("No physical plan. Waiting for data." === q.explainInternal(true))

      inputData.addData("abc")
      q.processAllAvailable()

      val explainWithoutExtended = q.explainInternal(false)
      // `extended = false` only displays the physical plan.
      assert("LocalRelation".r.findAllMatchIn(explainWithoutExtended).size === 0)
      assert("LocalTableScan".r.findAllMatchIn(explainWithoutExtended).size === 1)

      val explainWithExtended = q.explainInternal(true)
      // `extended = true` displays 3 logical plans (Parsed/Optimized/Optimized) and 1 physical
      // plan.
      assert("LocalRelation".r.findAllMatchIn(explainWithExtended).size === 3)
      assert("LocalTableScan".r.findAllMatchIn(explainWithExtended).size === 1)
    } finally {
      q.stop()
    }
  }


  // Instrument MemoryStream to collect timings.
  class MemoryStreamWithTiming(id : Int) extends MemoryStream[String](id, sqlContext) {
    val pollTimes = new ArrayBuffer[Long]
    override def getOffset: Option[Offset] = synchronized {
      pollTimes.append(System.currentTimeMillis())
      super.getOffset
    }

    def pollIntervals : Array[Long] = {
      pollTimes.sliding(2).map( w => w(1) - w(0) ).toArray
    }
  }

  test("SPARK-17386: Polling rate should decrease if there is no data") {

    // Shared SQL context, so save and restore the config values we change
    val origMinDelay = sqlContext.getConf("spark.sql.streaming.minPollingDelay")
    val origMaxDelay = sqlContext.getConf("spark.sql.streaming.maxPollingDelay")

    // Isolate this test case from changes to the default values of the polling
    // delay parameters.
    sqlContext.setConf("spark.sql.streaming.minPollingDelay", "10")
    sqlContext.setConf("spark.sql.streaming.maxPollingDelay", "200")

    {
      val inputData = new MemoryStreamWithTiming(42)
      val df = inputData.toDS()



      testStream(df)(
        AddData(inputData, "hello"),
        CheckAnswer("hello"),
        Assert({
          Thread.sleep(2000); true
        })
      )

      // Observed polling interval should converge to about 200 msec.
      val badIntervals = inputData.pollIntervals.drop(10).
        filter( x => (x > 250 || x < 150))
      if (badIntervals.size > 0) {
        fail(s"Unexpected intervals between polls: ${badIntervals.mkString(", ")}")
      }
    }


    sqlContext.setConf("spark.sql.streaming.minPollingDelay", origMinDelay)
    sqlContext.setConf("spark.sql.streaming.maxPollingDelay", origMaxDelay)
  }

  // This test is ignored by default because it is somewhat timing-dependent
  ignore("SPARK-17386: Polling rate should increase if data comes quickly") {
    // Shared SQL context, so save and restore the config values we change
    val origMinDelay = sqlContext.getConf("spark.sql.streaming.minPollingDelay")
    val origMaxDelay = sqlContext.getConf("spark.sql.streaming.maxPollingDelay")

    // The current version of the scheduler has trouble getting below 50 msec
    // when there's data to process, so do everything in slow motion.
    val targetMinDelayMsec = 100;
    sqlContext.setConf("spark.sql.streaming.minPollingDelay", targetMinDelayMsec.toString())
    sqlContext.setConf("spark.sql.streaming.maxPollingDelay", (20 * targetMinDelayMsec).toString())

    {
      val inputData = new MemoryStreamWithTiming(42)
      val df = inputData.toDS().toDF()
      val sink = new MemorySink(df.schema, OutputMode.Append)
      val metadataRoot = org.apache.spark.util.Utils
        .createTempDir(namePrefix = "streaming.metadata")
        .getCanonicalPath

      val stream = spark
        .streams
        .startQuery(
          None,
          Some(metadataRoot),
          df,
          sink,
          OutputMode.Append)
        .asInstanceOf[StreamExecution]

      val targetDelays = new ArrayBuffer[Long]

      val endTimeMsec = System.currentTimeMillis() + (20 * targetMinDelayMsec)
      while (System.currentTimeMillis() < endTimeMsec) {
        inputData.addData("a")

        // ignore result, but record current polling delay
        targetDelays.append(stream.curPollingDelayMs)

        Thread.sleep(10)
      }
      stream.stop()

      val averageDelay = targetDelays.sum / targetDelays.size

      if (averageDelay < targetMinDelayMsec) {
        fail(s"Average delay of $averageDelay msec too small " +
          s"(should be > $targetMinDelayMsec)")
      }
      if (averageDelay > targetMinDelayMsec * 1.5) {
        fail(s"Average delay of $averageDelay msec too small " +
          s"(should be < ${targetMinDelayMsec * 1.5})")
      }

      val pollDelays = inputData.pollIntervals.toList
      val averagePollDelay = pollDelays.sum / pollDelays.size
      if (averagePollDelay > targetMinDelayMsec) {
        fail(s"Average delay of $averageDelay msec too large " +
          s"(should be < $targetMinDelayMsec)")
      }
    }

    sqlContext.setConf("spark.sql.streaming.minPollingDelay", origMinDelay)
    sqlContext.setConf("spark.sql.streaming.maxPollingDelay", origMaxDelay)
  }

  // This test is ignored by default because it is somewhat timing-dependent
  ignore("SPARK-17386: Polling rate should converge to rate of constant data arrival") {

    // Shared SQL context, so save and restore the config values we change
    val origMinDelay = sqlContext.getConf("spark.sql.streaming.minPollingDelay")
    val origMaxDelay = sqlContext.getConf("spark.sql.streaming.maxPollingDelay")

    // The current version of the scheduler has trouble getting below 50 msec
    // when there's data to process, so do everything in slow motion.
    val targetMinDelayMsec = 50;
    val actualBatchDelayMsec = 5 * targetMinDelayMsec
    sqlContext.setConf("spark.sql.streaming.minPollingDelay", targetMinDelayMsec.toString())
    sqlContext.setConf("spark.sql.streaming.maxPollingDelay", (20 * targetMinDelayMsec).toString())

    {
      val inputData = new MemoryStreamWithTiming(42)
      val df = inputData.toDS().toDF()
      val sink = new MemorySink(df.schema, OutputMode.Append)
      val metadataRoot = org.apache.spark.util.Utils
        .createTempDir(namePrefix = "streaming.metadata")
        .getCanonicalPath

      val stream = spark
        .streams
        .startQuery(
          None,
          Some(metadataRoot),
          df,
          sink,
          OutputMode.Append)
        .asInstanceOf[StreamExecution]

      val targetDelays = new ArrayBuffer[Long]

      val endTimeMsec = System.currentTimeMillis() + (100 * targetMinDelayMsec)
      while (System.currentTimeMillis() < endTimeMsec) {
        inputData.addData("a")

        // ignore result, but record current polling delay
        targetDelays.append(stream.curPollingDelayMs)

        Thread.sleep(actualBatchDelayMsec)
      }
      stream.stop()

      val averageTargetDelay = targetDelays.sum / targetDelays.size

      // Target polling rate should converge to within a factor of 2 of the
      // actual data rate
      if (averageTargetDelay > 2 * actualBatchDelayMsec
          || averageTargetDelay < actualBatchDelayMsec / 2 ) {
        fail(s"Average target delay of $averageTargetDelay msec outside" +
          s"of expected range (${actualBatchDelayMsec / 2}, " +
          s"${actualBatchDelayMsec * 2})")
      }

      val pollDelays = inputData.pollIntervals.toList
      val averagePollDelay = pollDelays.sum / pollDelays.size

      // Actual polling rate should be about half the target rate, since the
      // scheduler always polls a second time after successfully reading data.
      if (averagePollDelay > actualBatchDelayMsec
        || averageTargetDelay < actualBatchDelayMsec / 4 ) {
        fail(s"Average observed delay of $averagePollDelay msec outside" +
          s"of expected range (${actualBatchDelayMsec / 4},  " +
          s"${actualBatchDelayMsec})")
      }
    }

    sqlContext.setConf("spark.sql.streaming.minPollingDelay", origMinDelay)
    sqlContext.setConf("spark.sql.streaming.maxPollingDelay", origMaxDelay)
  }

}

/**
 * A fake StreamSourceProvider thats creates a fake Source that cannot be reused.
 */
class FakeDefaultSource extends StreamSourceProvider {

  private val fakeSchema = StructType(StructField("a", IntegerType) :: Nil)

  override def sourceSchema(
      spark: SQLContext,
      schema: Option[StructType],
      providerName: String,
      parameters: Map[String, String]): (String, StructType) = ("fakeSource", fakeSchema)

  override def createSource(
      spark: SQLContext,
      metadataPath: String,
      schema: Option[StructType],
      providerName: String,
      parameters: Map[String, String]): Source = {
    // Create a fake Source that emits 0 to 10.
    new Source {
      private var offset = -1L

      override def schema: StructType = StructType(StructField("a", IntegerType) :: Nil)

      override def getOffset: Option[Offset] = {
        if (offset >= 10) {
          None
        } else {
          offset += 1
          Some(LongOffset(offset))
        }
      }

      override def getBatch(start: Option[Offset], end: Offset): DataFrame = {
        val startOffset = start.map(_.asInstanceOf[LongOffset].offset).getOrElse(-1L) + 1
        spark.range(startOffset, end.asInstanceOf[LongOffset].offset + 1).toDF("a")
      }

      override def stop() {}
    }
  }
}
