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

package org.apache.spark.sql.execution.streaming

import java.io.{File, OutputStream}
import java.util.concurrent.{CountDownLatch, Semaphore, ThreadPoolExecutor, TimeUnit}

import scala.collection.mutable.ListBuffer

import org.apache.hadoop.fs.Path
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}

import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.streaming.WriteToStream
import org.apache.spark.sql.connector.read.streaming
import org.apache.spark.sql.execution.streaming.AsyncProgressTrackingMicroBatchExecution.{ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS, ASYNC_PROGRESS_TRACKING_ENABLED, ASYNC_PROGRESS_TRACKING_OVERRIDE_SINK_SUPPORT_CHECK}
import org.apache.spark.sql.functions.{column, window}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.streaming.{StreamingQuery, StreamingQueryException, StreamTest, Trigger}
import org.apache.spark.sql.streaming.util.StreamManualClock
import org.apache.spark.util.{Clock, Utils}

class AsyncProgressTrackingMicroBatchExecutionSuite
    extends StreamTest
    with BeforeAndAfter
    with Matchers {

  import testImplicits._

  after {
    sqlContext.streams.active.foreach(_.stop())
  }

  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def waitPendingOffsetWrites(streamExecution: StreamExecution): Unit = {
    assert(streamExecution.isInstanceOf[AsyncProgressTrackingMicroBatchExecution])
    eventually(timeout(Span(5, Seconds))) {
      streamExecution
        .asInstanceOf[AsyncProgressTrackingMicroBatchExecution]
        .areWritesPendingOrInProgress() should be(false)
    }
  }

  def waitPendingPurges(streamExecution: StreamExecution): Unit = {
    assert(streamExecution.isInstanceOf[AsyncProgressTrackingMicroBatchExecution])
    eventually(timeout(Span(5, Seconds))) {
      streamExecution
        .asInstanceOf[AsyncProgressTrackingMicroBatchExecution]
        .arePendingAsyncPurge should be(false)
    }
  }

  // test the basic functionality i.e. happy path
  test("async WAL commits happy path") {
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDF()

    val tableName = "test"

    def startQuery(): StreamingQuery = {
      ds.writeStream
        .format("memory")
        .queryName(tableName)
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, true)
        .option(ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS, 0)
        .option("checkpointLocation", checkpointLocation)
        .start()
    }
    val query = startQuery()
    val expected = new ListBuffer[Row]()
    for (j <- 0 until 100) {
      for (i <- 0 until 10) {
        val v = i + (j * 10)
        inputData.addData({ v })
        expected += Row(v)
      }
      query.processAllAvailable()
    }

    checkAnswer(
      spark.table(tableName),
      expected.toSeq
    )
  }

  test("async WAL commits recovery") {
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDF()

    var index = 0
    // to synchronize producing and consuming messages so that
    // we can generate and read the desired number of batches
    var countDownLatch = new CountDownLatch(10)
    val sem = new Semaphore(1)
    val data = new ListBuffer[Int]()
    def startQuery(): StreamingQuery = {
      ds.writeStream
        .foreachBatch((ds: Dataset[Row], batchId: Long) => {
          ds.collect.foreach((row: Row) => {
            data += row.getInt(0)
          }: Unit)
          countDownLatch.countDown()
          index += 1
          sem.release()
        })
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, true)
        .option(ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS, 0)
        .option(ASYNC_PROGRESS_TRACKING_OVERRIDE_SINK_SUPPORT_CHECK, true)
        .option("checkpointLocation", checkpointLocation)
        .start()
    }
    var query = startQuery()

    for (i <- 0 until 10) {
      sem.acquire()
      inputData.addData({ i })
    }

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
    } finally {
      query.stop()
    }

    assert(index == 10)
    data should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))

    countDownLatch = new CountDownLatch(10)

    /**
     * Start the query again
     */
    query = startQuery()

    for (i <- 10 until 20) {
      sem.acquire()
      inputData.addData({ i })
    }

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
    } finally {
      query.stop()
    }

    // convert data to set to deduplicate results
    data.toSet should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19).toSet
    )
  }

  test("async WAL commits turn on and off") {
    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "0"
      )
    )(
      AddData(inputData, 0),
      StartStream(checkpointLocation = checkpointLocation),
      CheckAnswer(0),
      AddData(inputData, 1),
      CheckAnswer(0, 1),
      AddData(inputData, 2),
      CheckAnswer(0, 1, 2),
      Execute { q =>
        waitPendingOffsetWrites(q)
        // make sure we have removed all pending commits
        q.offsetLog.asInstanceOf[AsyncOffsetSeqLog].pendingAsyncOffsetWrite() should be(0)
      },
      StopStream
    )

    // offsets should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2))

    /**
     * Starting stream second time with async progress tracking turned off
     */
    testStream(ds)(
      // add new data
      AddData(inputData, 3),
      StartStream(checkpointLocation = checkpointLocation),
      CheckNewAnswer(3),
      AddData(inputData, 4),
      CheckNewAnswer(4),
      StopStream
    )

    // offsets should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4))
    // commits for batch 2, 3, 4 should be logged
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4))

    /**
     * Starting stream third time with async progress tracking turned back on
     */
    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "0"
      )
    )(
      // add new data
      AddData(inputData, 5),
      StartStream(checkpointLocation = checkpointLocation),
      // no data needs to be replayed because commit log is on previously
      CheckNewAnswer(5),
      AddData(inputData, 6),
      CheckNewAnswer(6),
      Execute { q =>
        waitPendingOffsetWrites(q)
        // make sure we have removed all pending commits
        q.offsetLog.asInstanceOf[AsyncOffsetSeqLog].pendingAsyncOffsetWrite() should be(0)
      },
      StopStream
    )

    // offsets should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6))
    // no new commits should be logged since async offset commits are enabled
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6))

    /**
     * Starting stream fourth time with async progress tracking turned off
     */
    testStream(ds)(
      // add new data
      AddData(inputData, 7),
      StartStream(checkpointLocation = checkpointLocation),
      CheckNewAnswer(7),
      AddData(inputData, 8),
      CheckNewAnswer(8),
      StopStream
    )

    // offsets should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8))
    // commits for batch 2, 3, 4, 6, 7, 8 should be logged
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8))
  }

  test("Fail with once trigger") {
    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDF()

    val e = intercept[IllegalArgumentException] {
      ds.writeStream
        .format("noop")
        .trigger(Trigger.Once())
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, true)
        .start()
    }
    e.getMessage should equal("Async progress tracking cannot be used with Once trigger")
  }

  test("Fail with available now trigger") {

    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDF()

    val e = intercept[IllegalArgumentException] {
      ds.writeStream
        .format("noop")
        .trigger(Trigger.AvailableNow())
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, true)
        .start()
    }
    e.getMessage should equal("Async progress tracking cannot be used with AvailableNow trigger")
  }

  test("switching between async wal commit enabled and trigger once") {
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDF()

    var index = 0
    var countDownLatch = new CountDownLatch(10)
    var sem = new Semaphore(1)
    val data = new ListBuffer[Int]()
    def startQuery(
        asyncProgressTracking: Boolean,
        trigger: Trigger = Trigger.ProcessingTime(0)): StreamingQuery = {

      ds.writeStream
        .trigger(trigger)
        .foreachBatch((ds: Dataset[Row], batchId: Long) => {
          ds.collect.foreach((row: Row) => {
            data += row.getInt(0)
          }: Unit)
          countDownLatch.countDown()
          index += 1
          sem.release()
        })
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, asyncProgressTracking)
        .option(ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS, 0)
        .option(ASYNC_PROGRESS_TRACKING_OVERRIDE_SINK_SUPPORT_CHECK, true)
        .option("checkpointLocation", checkpointLocation)
        .start()
    }

    /*
     start the query with async progress tracking turned on
     */
    var query = startQuery(true)

    for (i <- 0 until 10) {
      sem.acquire()
      inputData.addData({ i })
    }

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(9)
      }
    } finally {
      query.stop()
    }

    index should equal(10)
    data should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))

    // offsets should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))

    /*
     Turn off async offset commit and use trigger once
     */

    // trigger once should process batch 10
    countDownLatch = new CountDownLatch(1)
    for (i <- 10 until 20) {
      inputData.addData({ i })
    }
    query = startQuery(false, trigger = Trigger.Once())

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      // make sure the batch 10 in the commit log writes to durable storage
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(10)
      }
    } finally {
      query.stop()
    }

    data should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19))

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

    /*
     Turn on async offset commit again
     */

    countDownLatch = new CountDownLatch(10)
    sem = new Semaphore(1)
    query = startQuery(true)
    for (i <- 20 until 30) {
      sem.acquire()
      inputData.addData({ i })
    }

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(20)
      }
    } finally {
      query.stop()
    }

    data should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
        24, 25, 26, 27, 28, 29)
    )

    // 10 more batches should logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    )
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    )

    /*
     Turn off async offset commit again
     */

    for (i <- 30 until 40) {
      inputData.addData({ i })
    }

    countDownLatch = new CountDownLatch(1)
    query = startQuery(false, trigger = Trigger.Once())

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      // make sure the batch 20 in the commit log writes to durable storage
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(21)
      }
    } finally {
      query.stop()
    }

    // convert data to set to deduplicate results
    data should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
        24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39)
    )

    // batch 21 should be processed
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)
    )
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)
    )

  }

  test("switching between async wal commit enabled and available now") {
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDF()

    var index = 0
    var countDownLatch = new CountDownLatch(10)
    var sem = new Semaphore(1)
    val data = new ListBuffer[Int]()
    def startQuery(
        asyncOffsetCommitsEnabled: Boolean,
        trigger: Trigger = Trigger.ProcessingTime(0)): StreamingQuery = {
      ds.writeStream
        .trigger(trigger)
        .foreachBatch((ds: Dataset[Row], batchId: Long) => {
          ds.collect.foreach((row: Row) => {
            data += row.getInt(0)
          }: Unit)
          countDownLatch.countDown()
          index += 1
          sem.release()
        })
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, asyncOffsetCommitsEnabled)
        .option(ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS, 0)
        .option(ASYNC_PROGRESS_TRACKING_OVERRIDE_SINK_SUPPORT_CHECK, true)
        .option("checkpointLocation", checkpointLocation)
        .start()
    }

    /*
     start the query with async offset commits turned on
     */
    var query = startQuery(true)

    for (i <- 0 until 10) {
      sem.acquire()
      inputData.addData({ i })
    }

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(9)
      }
    } finally {
      query.stop()
    }

    index should equal(10)
    data should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))

    /*
     Turn off async offset commit and use trigger available now
     */

    countDownLatch = new CountDownLatch(1)
    for (i <- 10 until 20) {
      inputData.addData({ i })
    }
    query = startQuery(false, trigger = Trigger.AvailableNow())

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      // make sure the batch 10 in the commit log writes to durable storage
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(10)
      }
    } finally {
      query.stop()
    }

    data should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19))

    // since using trigger available now, the new data, i.e. batch 10, should also be processed
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

    /*
     Turn on async offset commit again
     */

    countDownLatch = new CountDownLatch(10)
    sem = new Semaphore(1)
    query = startQuery(true)
    for (i <- 20 until 30) {
      sem.acquire()
      inputData.addData({ i })
    }

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(20)
      }
    } finally {
      query.stop()
    }

    data should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
        24, 25, 26, 27, 28, 29)
    )

    // 10 more batches should logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    )
    // no additional commit log entries should be logged since async offset commit is on
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    )

    /*
     Turn off async offset commit and use trigger available now
     */

    for (i <- 30 until 40) {
      inputData.addData({ i })
    }

    countDownLatch = new CountDownLatch(1)
    query = startQuery(false, trigger = Trigger.AvailableNow())

    try {
      countDownLatch.await(streamingTimeout.toMillis, TimeUnit.MILLISECONDS)
      // make sure the batch 20 in the commit log writes to durable storage
      eventually(timeout(Span(5, Seconds))) {
        val files = getListOfFiles(checkpointLocation + "/commits")
          .filter(file => !file.isHidden)
          .map(file => file.getName.toInt)
          .sorted

        files.last should be(21)
      }
    } finally {
      query.stop()
    }

    // just reprocessing batch 20 should not more offset log entries should added
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)
    )
    // batch 20 should be added to the commit log
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)
    )
  }

  class FailAsyncProgressTrackingMicroBatchExecution(
      sparkSession: SparkSession,
      trigger: Trigger,
      triggerClock: Clock,
      extraOptions: Map[String, String],
      plan: WriteToStream)
      extends AsyncProgressTrackingMicroBatchExecution(
        sparkSession,
        trigger,
        triggerClock,
        extraOptions,
        plan
      ) {

    override val offsetLog = new FailAsyncOffsetSeqLog(
      sparkSession,
      checkpointFile("offsets"),
      asyncWritesExecutorService,
      asyncProgressTrackingCheckpointingIntervalMs
    )
  }

  class FailAsyncOffsetSeqLog(
      sparkSession: SparkSession,
      path: String,
      executorService: ThreadPoolExecutor,
      offsetCommitIntervalMs: Long)
      extends AsyncOffsetSeqLog(sparkSession, path, executorService, offsetCommitIntervalMs) {

    override def write(
        batchMetadataFile: Path,
        fn: OutputStream => Unit): Unit = {

      throw new Exception("test fail")
    }
  }

  // Tests that errors that occurred during async offset log write gets bubbled up
  // to the main stream execution thread
  test("bubble up async offset log write errors") {
    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "0"
      )
    )(
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 0),
      CheckAnswer(0),
      Execute { q =>
        waitPendingOffsetWrites(q)
        // to simulate write error by writing a file for the next offset i.e. 1.
        // This should create a conflict
        import java.io._
        val pw = new PrintWriter(new File(checkpointLocation + "/offsets/1"))
        pw.write("Hello, world")
        pw.close
      },
      AddData(inputData, 1),
      Execute {
        q =>
          eventually(timeout(Span(5, Seconds))) {
            val e = intercept[StreamingQueryException] {
              q.processAllAvailable()
            }
            e.getCause.getCause.getMessage should include(
              "Concurrent update to the log. Multiple streaming jobs detected for 1")
          }
      }
    )
  }

  // Tests that errors that occurred during async commit log write gets bubbled up
  // to the main stream execution thread
  test("bubble up async commit log write errors 1") {
    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "0"
      )
    )(
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 0),
      CheckAnswer(0),
      Execute { q =>
        waitPendingOffsetWrites(q)
        // to simulate write error
        import java.io._
        val pw = new PrintWriter(new File(checkpointLocation + "/commits/1"))
        pw.write("Hello, world")
        pw.close
      },
      AddData(inputData, 1),
      Execute {
        q =>
          eventually(timeout(Span(5, Seconds))) {
            val e = intercept[StreamingQueryException] {
              q.processAllAvailable()
            }
            e.getCause.getCause.getMessage should include(
              "Concurrent update to the log. Multiple streaming jobs detected for 1")
          }
      }
    )
  }

  // Tests that errors that occurred during async offset log write gets bubbled up
  // to the main stream execution thread
  test("bubble up async offset log write errors 2") {
    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "0"
      )
    )(
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 0),
      CheckAnswer(0),
      Execute { q =>
        waitPendingOffsetWrites(q)
        // to simulate write error
        import java.io._
        val commitDir = new File(checkpointLocation + "/offsets")
        commitDir.setReadOnly()

      },
      AddData(inputData, 1),
      Execute {
        q =>
          eventually(timeout(Span(5, Seconds))) {
            val e = intercept[StreamingQueryException] {
              q.processAllAvailable()
            }
            e.getCause.getCause.getMessage should include("Permission denied")
          }
      }
    )
  }

  // Tests that errors that occurred during async commit log write gets bubbled up
  // to the main stream execution thread
  test("bubble up async commit log write errors 2") {
    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()
    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "0"
      )
    )(
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 0),
      CheckAnswer(0),
      Execute { q =>
        waitPendingOffsetWrites(q)
        // to simulate write error
        import java.io._
        val commitDir = new File(checkpointLocation + "/commits")
        commitDir.setReadOnly()

      },
      AddData(inputData, 1),
      Execute {
        q =>
          eventually(timeout(Span(5, Seconds))) {
            val e = intercept[StreamingQueryException] {
              q.processAllAvailable()
            }
            e.getCause.getCause.getMessage should include("Permission denied")
          }
      }
    )
  }

  class MemoryStreamCapture[A: Encoder](
      id: Int,
      sqlContext: SQLContext,
      numPartitions: Option[Int] = None)
      extends MemoryStream[A](id, sqlContext, numPartitions = numPartitions) {

    val commits = new ListBuffer[streaming.Offset]()
    val commitThreads = new ListBuffer[Thread]()

    override def commit(end: streaming.Offset): Unit = {
      super.commit(end)
      commits += end
      commitThreads += Thread.currentThread()
    }
  }

  test("commit intervals happy path") {

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)

    val ds = inputData.toDF()

    val data = new ListBuffer[Int]()
    def startQuery(): StreamingQuery = {
      ds.writeStream
        .foreachBatch((ds: Dataset[Row], batchId: Long) => {
          ds.collect.foreach((row: Row) => {
            data += row.getInt(0)
          }: Unit)
        })
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, true)
        .option(ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS, 1000)
        .option(ASYNC_PROGRESS_TRACKING_OVERRIDE_SINK_SUPPORT_CHECK, true)
        .option("checkpointLocation", checkpointLocation)
        .start()
    }
    val query = startQuery()
    val expected = new ListBuffer[Int]()
    for (j <- 0 until 100) {
      for (i <- 0 until 10) {
        val v = i + (j * 10)
        inputData.addData({ v })
        expected += v
      }
      query.processAllAvailable()
    }

    eventually(timeout(Span(5, Seconds))) {
      val commitLogFiles = getListOfFiles(checkpointLocation + "/commits")
        .filter(file => !file.isHidden)
        .map(file => file.getName.toInt)
        .sorted

      val offsetLogFiles = getListOfFiles(checkpointLocation + "/offsets")
        .filter(file => !file.isHidden)
        .map(file => file.getName.toInt)
        .sorted

      offsetLogFiles should equal (commitLogFiles)
    }

    query.stop()

    data should equal(expected)

    val commitLogFiles = getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted

    val offsetLogFiles = getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted

    logInfo(s"offsetLogFiles: ${offsetLogFiles}")
    logInfo(s"commitLogFiles: ${commitLogFiles}")

    val offsetLog = new AsyncOffsetSeqLog(ds.sparkSession, checkpointLocation + "/offsets", null, 0)
    // commits received at source should match up to the ones found in the offset log
    for (i <- 0 until inputData.commits.length) {
      val offsetOnDisk: OffsetSeq = offsetLog.get(offsetLogFiles(i)).get

      val sourceCommittedOffset: streaming.Offset = inputData.commits(i)

      offsetOnDisk.offsets(0).get.json() should equal(sourceCommittedOffset.json())
    }

    // make sure that the source commits is being executed by the main stream execution thread
    inputData.commitThreads.foreach(thread => {
      thread.getName should include("stream execution thread for")
      thread.getName should include(query.id.toString)
      thread.getName should include(query.runId.toString)
    })
    commitLogFiles should equal(offsetLogFiles)
  }

  test("interval commits and recovery") {
    val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    val clock = new StreamManualClock

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
      )
    )(
      // need to to set processing time to something so manual clock will work
      StartStream(
        Trigger.ProcessingTime("1 millisecond"),
        checkpointLocation = checkpointLocation,
        triggerClock = clock
      ),
      AddData(inputData, 0),
      AdvanceManualClock(100),
      CheckNewAnswer(0),
      AddData(inputData, 1),
      AdvanceManualClock(100),
      CheckNewAnswer(1),
      AddData(inputData, 2),
      AdvanceManualClock(100),
      CheckNewAnswer(2),
      AddData(inputData, 3),
      AdvanceManualClock(800), // should trigger offset commit write to durable storage
      CheckNewAnswer(3),
      Execute { q =>
        waitPendingOffsetWrites(q)
      },
      StopStream
    )

    // batches 0 and 3 should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3))

    /**
     * restart stream
     */
    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
      )
    )(
      // add new data
      StartStream(
        Trigger.ProcessingTime("1 millisecond"),
        checkpointLocation = checkpointLocation,
        triggerClock = clock
      ),
      AddData(inputData, 4), // should persist to durable storage since first batch after restart
      AdvanceManualClock(100),
      CheckNewAnswer(4),
      AddData(inputData, 5),
      AdvanceManualClock(100),
      CheckNewAnswer(5),
      AddData(inputData, 6),
      AdvanceManualClock(100),
      CheckNewAnswer(6),
      AddData(inputData, 7),
      AdvanceManualClock(800), // should trigger offset commit write to durable storage
      CheckNewAnswer(7),
      Execute { q =>
        waitPendingOffsetWrites(q)
      },
      StopStream
    )

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 7))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 7))
  }

  test(
    "recovery when first offset is not zero and" +
    " not commit log entries"
  ) {
    val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    // create a scenario in which the offset log only contains batch 2 and commit log is empty
    testStream(ds)(
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 0),
      CheckNewAnswer(0),
      AddData(inputData, 1),
      CheckNewAnswer(1),
      AddData(inputData, 2),
      CheckNewAnswer(2),
      Execute { q =>
        q.offsetLog.purge(2)
        getListOfFiles(checkpointLocation + "/commits").foreach(file => file.delete())
      },
      StopStream
    )

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(2))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array())

    /**
     * start new stream
     */
    val inputData2 = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds2 = inputData2.toDS()
    testStream(ds2)(
      // add back old data
      AddData(inputData2, 0),
      AddData(inputData2, 1),
      AddData(inputData2, 2),
      StartStream(checkpointLocation = checkpointLocation),
      // should replay from the beginning
      CheckNewAnswer(0, 1, 2),
      AddData(inputData2, 3),
      CheckNewAnswer(3),
      AddData(inputData2, 4),
      CheckNewAnswer(4),
      AddData(inputData2, 5),
      CheckNewAnswer(5),
      StopStream
    )


    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(2, 3, 4, 5))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(2, 3, 4, 5))
  }

  test("test multiple gaps in offset and commit logs") {
    val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    // create a scenario in which the offset log only contains batch 2 and commit log is empty
    testStream(ds)(
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 0),
      CheckNewAnswer(0),
      AddData(inputData, 1),
      CheckNewAnswer(1),
      AddData(inputData, 2),
      CheckNewAnswer(2),
      AddData(inputData, 3),
      CheckNewAnswer(3),
      AddData(inputData, 4),
      CheckNewAnswer(4),
      AddData(inputData, 5),
      CheckNewAnswer(5),

      StopStream
    )

    new File(checkpointLocation + "/offsets/1").delete()
    new File(checkpointLocation + "/offsets/.1.crc").delete()
    new File(checkpointLocation + "/offsets/3").delete()
    new File(checkpointLocation + "/offsets/.3.crc").delete()
    new File(checkpointLocation + "/offsets/4").delete()
    new File(checkpointLocation + "/offsets/.4.crc").delete()
    new File(checkpointLocation + "/commits/1").delete()
    new File(checkpointLocation + "/commits/.1.crc").delete()
    new File(checkpointLocation + "/commits/.3.crc").delete()
    new File(checkpointLocation + "/commits/3").delete()
    new File(checkpointLocation + "/commits/.4.crc").delete()
    new File(checkpointLocation + "/commits/4").delete()
    new File(checkpointLocation + "/commits/.5.crc").delete()
    new File(checkpointLocation + "/commits/5").delete()


    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 2, 5))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 2))

    /**
     * start new stream
     */
    val inputData2 = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds2 = inputData2.toDS()
    testStream(ds2)(
      // add back old data
      AddData(inputData2, 0),
      AddData(inputData2, 1),
      AddData(inputData2, 2),
      AddData(inputData2, 3),
      AddData(inputData2, 4),
      AddData(inputData2, 5),
      StartStream(checkpointLocation = checkpointLocation),
      CheckNewAnswer(3, 4, 5),
      StopStream
    )

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 2, 5))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 2, 5))
  }

  test(
    "recovery when gaps in in offset and" +
    " commit log"
  ) {
    val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    // create a scenario in which the offset log only contains batch 2 and commit log is empty
    testStream(ds)(
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 0),
      CheckNewAnswer(0),
      AddData(inputData, 1),
      CheckNewAnswer(1),
      AddData(inputData, 2),
      CheckNewAnswer(2),
      StopStream
    )

    new File(checkpointLocation + "/offsets/1").delete()
    new File(checkpointLocation + "/offsets/.1.crc").delete()
    new File(checkpointLocation + "/commits/2").delete()
    new File(checkpointLocation + "/commits/.2.crc").delete()
    new File(checkpointLocation + "/commits/1").delete()
    new File(checkpointLocation + "/commits/.1.crc").delete()

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 2))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0))

    /**
     * start new stream
     */
    val inputData2 = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds2 = inputData2.toDS()
    testStream(ds2)(
      // add back old data
      AddData(inputData2, 0),
      AddData(inputData2, 1),
      AddData(inputData2, 2),
      StartStream(checkpointLocation = checkpointLocation),
      // should replay from batch 1
      CheckNewAnswer(1, 2),
      AddData(inputData2, 3),
      CheckNewAnswer(3),
      AddData(inputData2, 4),
      CheckNewAnswer(4),
      AddData(inputData2, 5),
      CheckNewAnswer(5),
      StopStream
    )

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 2, 3, 4, 5))
    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 2, 3, 4, 5))
  }

  test("recovery non-contiguous log") {
    val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    val clock = new StreamManualClock

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
      )
    )(
      // need to to set processing time to something so manual clock will work
      StartStream(
        Trigger.ProcessingTime("1 millisecond"),
        checkpointLocation = checkpointLocation,
        triggerClock = clock
      ),
      AddData(inputData, 0),
      AdvanceManualClock(100),
      CheckNewAnswer(0),
      AddData(inputData, 1),
      AdvanceManualClock(100),
      CheckNewAnswer(1),
      AddData(inputData, 2),
      AdvanceManualClock(100),
      CheckNewAnswer(2),
      AddData(inputData, 3),
      AdvanceManualClock(800), // should trigger offset commit write to durable storage
      CheckNewAnswer(3),
      Execute { q =>
        waitPendingOffsetWrites(q)
      },
      StopStream
    )

    // delete batch 3 from commit log
    new File(checkpointLocation + "/commits/3").delete()
    new File(checkpointLocation + "/commits/.3.crc").delete()
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0))

    /**
     * Turn async progress tracking off and test recovery
     */
    testStream(ds)(
      // need to to set processing time to something so manual clock will work
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 4),
      CheckNewAnswer(1, 2, 3, 4),
      AddData(inputData, 5),
      CheckNewAnswer(5),
      StopStream
    )
    // batches 0 and 3 should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5))

  }

  test("switching async progress tracking with interval commits on and off") {

    val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDS()

    val checkpointLocation = Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

    var clock = new StreamManualClock

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
      )
    )(
      // need to to set processing time to something so manual clock will work
      StartStream(
        Trigger.ProcessingTime("1 millisecond"),
        checkpointLocation = checkpointLocation,
        triggerClock = clock
      ),
      AddData(inputData, 0),
      AdvanceManualClock(100),
      CheckNewAnswer(0),
      AddData(inputData, 1),
      AdvanceManualClock(100),
      CheckNewAnswer(1),
      AddData(inputData, 2),
      AdvanceManualClock(100),
      CheckNewAnswer(2),
      AddData(inputData, 3),
      AdvanceManualClock(800), // should trigger offset commit write to durable storage
      CheckNewAnswer(3),
      Execute { q =>
        waitPendingOffsetWrites(q)
      },
      StopStream
    )

    // batches 0 and 3 should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3))

    inputData.commits.length should be(0)

    /**
     * Turn async progress tracking off
     */
    testStream(ds)(
      // need to to set processing time to something so manual clock will work
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 4),
      CheckNewAnswer(4),
      Execute { q =>
        inputData.commits.length should equal(1)
        inputData.commits(0).json should equal(LongOffset(3).json)
        inputData.commits.clear()
      },
      AddData(inputData, 5),
      CheckNewAnswer(5),
      Execute { q =>
        inputData.commits.length should equal(1)
        inputData.commits(0).json should equal(LongOffset(4).json)
        inputData.commits.clear()
      },
      StopStream
    )
    // batches 0 and 3 should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5))

    /**
     * Turn async progress tracking on
     */
    clock = new StreamManualClock

    testStream(
      ds,
      extraOptions = Map(
        ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
        ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
      )
    )(
      // need to to set processing time to something so manual clock will work
      StartStream(
        Trigger.ProcessingTime("1 millisecond"),
        checkpointLocation = checkpointLocation,
        triggerClock = clock
      ),
      AddData(inputData, 6), // will get persisted since first batch on restart
      AdvanceManualClock(100),
      CheckNewAnswer(6),
      Execute { q =>
        waitPendingOffsetWrites(q)
      },
      AddData(inputData, 7),
      AdvanceManualClock(100),
      CheckNewAnswer(7),
      Execute { q =>
        inputData.commits.length should equal(1)
        inputData.commits(0).json should equal(LongOffset(5).json)
        inputData.commits.clear()
      },
      AddData(inputData, 8),
      AdvanceManualClock(100),
      CheckNewAnswer(8),
      AddData(inputData, 9),
      AdvanceManualClock(800), //  persist offset
      CheckNewAnswer(9),
      Execute { q =>
        waitPendingOffsetWrites(q)
      },
      StopStream
    )

    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5, 6, 9))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5, 6, 9))

    // simulate batch 9 doesn't have a commit log
    new File(checkpointLocation + "/commits/9").delete()
    new File(checkpointLocation + "/commits/.9.crc").delete()

    /**
     * Turn async progress tracking off
     */
    testStream(ds)(
      // need to to set processing time to something so manual clock will work
      StartStream(checkpointLocation = checkpointLocation),
      AddData(inputData, 10),
      CheckNewAnswer(7, 8, 9, 10),
      Execute { q =>
        inputData.commits.length should equal(1)
        inputData.commits(0).json should equal(LongOffset(9).json)
        inputData.commits.clear()
      },
      AddData(inputData, 11),
      CheckNewAnswer(11),
      Execute { q =>
        inputData.commits.length should equal(1)
        inputData.commits(0).json should equal(LongOffset(10).json)
        inputData.commits.clear()
      },
      StopStream
    )
    // batches 0 and 3 should be logged
    getListOfFiles(checkpointLocation + "/offsets")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5, 6, 9, 10, 11))

    getListOfFiles(checkpointLocation + "/commits")
      .filter(file => !file.isHidden)
      .map(file => file.getName.toInt)
      .sorted should equal(Array(0, 3, 4, 5, 6, 9, 10, 11))
  }

  test("Fail on stateful pipelines") {
    val rateStream = spark.readStream
      .format("rate")
      .option("numPartitions", 1)
      .option("rowsPerSecond", 10)
      .load()
      .toDF()

    val windowedStream = rateStream
      .withWatermark("timestamp", "0 seconds")
      .groupBy(window(column("timestamp"), "10 seconds"), column("value"))
      .count()

    val e = intercept[StreamingQueryException] {
      val query = windowedStream.writeStream
        .format("noop")
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, true)
        .start()

      query.processAllAvailable()
    }
    e.getMessage should include(
      "Stateful streaming queries does not support async progress tracking at this moment."
    )
  }

  test("Fail on pipelines using unsupported sinks") {
    val inputData = new MemoryStream[Int](id = 0, sqlContext = sqlContext)
    val ds = inputData.toDF()

    val e = intercept[IllegalArgumentException] {
      ds.writeStream
        .format("parquet")
        .option(ASYNC_PROGRESS_TRACKING_ENABLED, true)
        .option(
          "checkpointLocation",
          Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath
        )
        .start("/tmp")
    }

    e.getMessage should equal("Sink FileSink[/tmp] does not support async progress tracking")
  }

  test("with log purging") {

    withSQLConf(SQLConf.MIN_BATCHES_TO_RETAIN.key -> "2", SQLConf.ASYNC_LOG_PURGE.key -> "false") {
      withTempDir { checkpointLocation =>
        val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
        val ds = inputData.toDS()

        val clock = new StreamManualClock
        testStream(
          ds,
          extraOptions = Map(
            ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
            ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
          )
        )(
          // need to to set processing time to something so manual clock will work
          StartStream(
            Trigger.ProcessingTime("1 millisecond"),
            checkpointLocation = checkpointLocation.getCanonicalPath,
            triggerClock = clock
          ),
          AddData(inputData, 0),
          AdvanceManualClock(100),
          CheckNewAnswer(0),
          AddData(inputData, 1),
          AdvanceManualClock(100),
          CheckNewAnswer(1),
          AddData(inputData, 2),
          AdvanceManualClock(100),
          CheckNewAnswer(2),
          AddData(inputData, 3),
          AdvanceManualClock(800), // should trigger offset commit write to durable storage
          CheckNewAnswer(3),
          AddData(inputData, 4),
          AdvanceManualClock(100),
          CheckNewAnswer(4),
          AddData(inputData, 5),
          AdvanceManualClock(100),
          CheckNewAnswer(5),
          AddData(inputData, 6),
          AdvanceManualClock(100),
          CheckNewAnswer(6),
          AddData(inputData, 7),
          AdvanceManualClock(800), // should trigger offset commit write to durable storage
          CheckNewAnswer(7),
          Execute { q =>
            // wait for all async log writes to finish
            waitPendingOffsetWrites(q)
          },
          // add a new row to make sure log purge has kicked in.
          // There can be a race condition in which the commit log entry for the previous batch
          // may or may not be written to disk yet before the log purge is called.
          // Adding another batch here will make sure purge is called on the correct number of
          // offset and commit log entries
          AddData(inputData, 8),
          AdvanceManualClock(100),
          CheckNewAnswer(8),
          Execute { q =>
            waitPendingOffsetWrites(q)
            getListOfFiles(checkpointLocation + "/offsets")
              .filter(file => !file.isHidden)
              .map(file => file.getName.toInt)
              .sorted should equal(Array(3, 7))
            getListOfFiles(checkpointLocation + "/commits")
              .filter(file => !file.isHidden)
              .map(file => file.getName.toInt)
              .sorted should equal(Array(3, 7))
            q.offsetLog.asInstanceOf[AsyncOffsetSeqLog].writtenToDurableStorage.size() should be(2)
            q.commitLog.asInstanceOf[AsyncCommitLog].writtenToDurableStorage.size() should be(2)
          },
          StopStream
        )

        /**
         * restart
         */
        testStream(
          ds,
          extraOptions = Map(
            ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
            ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
          )
        )(
          // need to to set processing time to something so manual clock will work
          StartStream(
            Trigger.ProcessingTime("1 millisecond"),
            checkpointLocation = checkpointLocation.getCanonicalPath,
            triggerClock = clock
          ),
          AddData(inputData, 9),
          AdvanceManualClock(100),
          CheckNewAnswer(8, 9),
          AddData(inputData, 10),
          AdvanceManualClock(100),
          CheckNewAnswer(10),
          AddData(inputData, 11),
          AdvanceManualClock(100),
          CheckNewAnswer(11),
          AddData(inputData, 12),
          AdvanceManualClock(800), // should trigger offset commit write to durable storage
          CheckNewAnswer(12),
          Execute { q =>
            // wait for all async log writes to finish
            waitPendingOffsetWrites(q)
          },
          // add a new row to make sure log purge has kicked in.
          // There can be a race condition in which the commit log entry for the previous batch
          // may or may not be written to disk yet before the log purge is called.
          // Adding another batch here will make sure purge is called on the correct number of
          // offset and commit log entries
          AddData(inputData, 13),
          AdvanceManualClock(100),
          CheckNewAnswer(13),
          Execute { q =>
            waitPendingOffsetWrites(q)
            getListOfFiles(checkpointLocation + "/offsets")
              .filter(file => !file.isHidden)
              .map(file => file.getName.toInt)
              .sorted should equal(Array(8, 12))
            getListOfFiles(checkpointLocation + "/commits")
              .filter(file => !file.isHidden)
              .map(file => file.getName.toInt)
              .sorted should equal(Array(8, 12))

            q.offsetLog.asInstanceOf[AsyncOffsetSeqLog].writtenToDurableStorage.size() should be(2)
            q.commitLog.asInstanceOf[AsyncCommitLog].writtenToDurableStorage.size() should be(2)
          },
          StopStream
        )
      }
    }
  }

  test("with async log purging") {
    withSQLConf(SQLConf.MIN_BATCHES_TO_RETAIN.key -> "2", SQLConf.ASYNC_LOG_PURGE.key -> "true") {
      withTempDir { checkpointLocation =>
        val inputData = new MemoryStreamCapture[Int](id = 0, sqlContext = sqlContext)
        val ds = inputData.toDS()

        val clock = new StreamManualClock
        testStream(
          ds,
          extraOptions = Map(
            ASYNC_PROGRESS_TRACKING_ENABLED -> "true",
            ASYNC_PROGRESS_TRACKING_CHECKPOINTING_INTERVAL_MS -> "1000"
          )
        )(
          // need to to set processing time to something so manual clock will work
          StartStream(
            Trigger.ProcessingTime("1 millisecond"),
            checkpointLocation = checkpointLocation.getCanonicalPath,
            triggerClock = clock
          ),
          AddData(inputData, 0),
          AdvanceManualClock(100),
          CheckNewAnswer(0),
          AddData(inputData, 1),
          AdvanceManualClock(100),
          CheckNewAnswer(1),
          AddData(inputData, 2),
          AdvanceManualClock(100),
          CheckNewAnswer(2),
          AddData(inputData, 3),
          AdvanceManualClock(800), // should trigger offset commit write to durable storage
          Execute { q =>
            // wait for async log writes to complete
            waitPendingOffsetWrites(q)
            eventually(timeout(Span(5, Seconds))) {
              getListOfFiles(checkpointLocation + "/offsets")
                .filter(file => !file.isHidden)
                .map(file => file.getName.toInt)
                .sorted should equal(Array(0, 3))

              getListOfFiles(checkpointLocation + "/commits")
                .filter(file => !file.isHidden)
                .map(file => file.getName.toInt)
                .sorted should equal(Array(0, 3))
            }
          },
          CheckNewAnswer(3),
          AddData(inputData, 4),
          AdvanceManualClock(100),
          CheckNewAnswer(4),
          AddData(inputData, 5),
          AdvanceManualClock(100),
          CheckNewAnswer(5),
          AddData(inputData, 6),
          AdvanceManualClock(100),
          CheckNewAnswer(6),
          AddData(inputData, 7),
          AdvanceManualClock(800), // should trigger offset commit write to durable storage
          CheckNewAnswer(7),
          Execute { q =>
            // wait for async log writes to complete
            waitPendingOffsetWrites(q)
            // can contain batches 0, 3, 7 or 3, 7
            eventually(timeout(Span(5, Seconds))) {
              getListOfFiles(checkpointLocation + "/offsets")
                .filter(file => !file.isHidden)
                .map(file => file.getName.toInt)
                .sorted should contain allElementsOf (Array(3, 7))

              // can contain batches 0, 3, 7 or 3, 7
              getListOfFiles(checkpointLocation + "/commits")
                .filter(file => !file.isHidden)
                .map(file => file.getName.toInt)
                .sorted should contain allElementsOf (Array(3, 7))
            }
          },
          AddData(inputData, 8),
          AdvanceManualClock(100),
          // the commit log entry for batch 7 may not be written yet at this point
          CheckNewAnswer(8),
          // run another batch to make sure commit log entry
          // for batch 7 has been written and purge is triggered
          AddData(inputData, 9),
          AdvanceManualClock(100),
          CheckNewAnswer(9),
          Execute { q =>
            eventually(timeout(Span(5, Seconds))) {

              // make sure all async purge tasks are done
              waitPendingPurges(q)

              waitPendingOffsetWrites(q)
              getListOfFiles(checkpointLocation + "/offsets")
                .filter(file => !file.isHidden)
                .map(file => file.getName.toInt)
                .sorted should equal(Array(3, 7))
              getListOfFiles(checkpointLocation + "/commits")
                .filter(file => !file.isHidden)
                .map(file => file.getName.toInt)
                .sorted should equal(Array(3, 7))
              q.offsetLog.asInstanceOf[AsyncOffsetSeqLog]
                .writtenToDurableStorage.size() should be(2)
              q.commitLog.asInstanceOf[AsyncCommitLog]
                .writtenToDurableStorage.size() should be(2)
            }
          },
          StopStream
        )
      }
    }
  }
}
