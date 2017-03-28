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

package org.apache.spark.scheduler

import java.io.{File, FileOutputStream, InputStream, IOException}
import java.net.URI

import scala.collection.mutable
import scala.io.Source

import org.apache.hadoop.fs.Path
import org.json4s.jackson.JsonMethods._
import org.scalatest.BeforeAndAfter

import org.apache.spark._
import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.spark.internal.Logging
import org.apache.spark.io._
import org.apache.spark.util.{JsonProtocol, Utils}

/**
 * Test whether EventLoggingListener logs events properly.
 *
 * This tests whether EventLoggingListener actually log files with expected name patterns while
 * logging events, whether the parsing of the file names is correct, and whether the logged events
 * can be read and deserialized into actual SparkListenerEvents.
 */
class EventLoggingListenerSuite extends SparkFunSuite with LocalSparkContext with BeforeAndAfter
  with Logging {
  import EventLoggingListenerSuite._

  private val fileSystem = Utils.getHadoopFileSystem("/",
    SparkHadoopUtil.get.newConfiguration(new SparkConf()))
  private var testDir: File = _
  private var testDirPath: Path = _

  before {
    testDir = Utils.createTempDir()
    testDir.deleteOnExit()
    testDirPath = new Path(testDir.getAbsolutePath())
  }

  after {
    Utils.deleteRecursively(testDir)
  }

  test("Verify log file exist") {
    // Verify logging directory exists
    val conf = getLoggingConf(testDirPath)
    val eventLogger = new EventLoggingListener("test", None, testDirPath.toUri(), conf)
    eventLogger.start()

    val logPath = new Path(eventLogger.logPath + EventLoggingListener.IN_PROGRESS)
    assert(fileSystem.exists(logPath))
    val logStatus = fileSystem.getFileStatus(logPath)
    assert(!logStatus.isDirectory)

    // Verify log is renamed after stop()
    eventLogger.stop()
    assert(!fileSystem.getFileStatus(new Path(eventLogger.logPath)).isDirectory)
  }

  test("Basic event logging") {
    testEventLogging()
  }

  test("Basic event logging with compression") {
    CompressionCodec.ALL_COMPRESSION_CODECS.foreach { codec =>
      testEventLogging(compressionCodec = Some(CompressionCodec.getShortName(codec)))
    }
  }

  test("End-to-end event logging") {
    testApplicationEventLogging()
  }

  test("End-to-end event logging with compression") {
    CompressionCodec.ALL_COMPRESSION_CODECS.foreach { codec =>
      testApplicationEventLogging(compressionCodec = Some(CompressionCodec.getShortName(codec)))
    }
  }

  test("Event logging with password redaction") {
    val key = "spark.executorEnv.HADOOP_CREDSTORE_PASSWORD"
    val secretPassword = "secret_password"
    val conf = getLoggingConf(testDirPath, None)
      .set(key, secretPassword)
    val eventLogger = new EventLoggingListener("test", None, testDirPath.toUri(), conf)
    val envDetails = SparkEnv.environmentDetails(conf, "FIFO", Seq.empty, Seq.empty)
    val event = SparkListenerEnvironmentUpdate(envDetails)
    val redactedProps = eventLogger.redactEvent(event).environmentDetails("Spark Properties").toMap
    assert(redactedProps(key) == "*********(redacted)")
  }

  test("Log overwriting") {
    val logUri = EventLoggingListener.getLogPath(testDir.toURI, "test", None)
    val logPath = new URI(logUri).getPath
    // Create file before writing the event log
    new FileOutputStream(new File(logPath)).close()
    // Expected IOException, since we haven't enabled log overwrite.
    intercept[IOException] { testEventLogging() }
    // Try again, but enable overwriting.
    testEventLogging(extraConf = Map("spark.eventLog.overwrite" -> "true"))
  }

  test("Event log name") {
    val baseDirUri = Utils.resolveURI("/base-dir")
    // without compression
    assert(s"${baseDirUri.toString}/app1" === EventLoggingListener.getLogPath(
      baseDirUri, "app1", None))
    // with compression
    assert(s"${baseDirUri.toString}/app1.lzf" ===
      EventLoggingListener.getLogPath(baseDirUri, "app1", None, Some("lzf")))
    // illegal characters in app ID
    assert(s"${baseDirUri.toString}/a-fine-mind_dollar_bills__1" ===
      EventLoggingListener.getLogPath(baseDirUri,
        "a fine:mind$dollar{bills}.1", None))
    // illegal characters in app ID with compression
    assert(s"${baseDirUri.toString}/a-fine-mind_dollar_bills__1.lz4" ===
      EventLoggingListener.getLogPath(baseDirUri,
        "a fine:mind$dollar{bills}.1", None, Some("lz4")))
  }

  test("test event logger logging executor metrics") {
    import org.apache.spark.scheduler.cluster._
    import org.apache.spark.ui.memory._
    val conf = EventLoggingListenerSuite.getLoggingConf(testDirPath)
    val eventLogger = new EventLoggingListener("test-memListener", None, testDirPath.toUri(), conf)
    val execId = "exec-1"
    val hostName = "host-1"
    val port: Option[Int] = Some(80)

    eventLogger.start()
    eventLogger.onExecutorAdded(SparkListenerExecutorAdded(
      0L, execId, new ExecutorInfo(hostName, 1, Map.empty)))

    // stage 1 and stage 2 submitted
    eventLogger.onStageSubmitted(MemoryListenerSuite.createStageStartEvent(1))
    eventLogger.onStageSubmitted(MemoryListenerSuite.createStageStartEvent(2))
    val execMetrics1 = MemoryListenerSuite.createExecutorMetrics(hostName, port, 1L, 20, 10)
    eventLogger.onExecutorMetricsUpdate(MemoryListenerSuite.createExecutorMetricsUpdateEvent(
      execId, execMetrics1))
    val execMetrics2 = MemoryListenerSuite.createExecutorMetrics(hostName, port, 2L, 30, 10)
    eventLogger.onExecutorMetricsUpdate(MemoryListenerSuite.createExecutorMetricsUpdateEvent(
      execId, execMetrics2))
    // stage1 completed
    eventLogger.onStageCompleted(MemoryListenerSuite.createStageEndEvent(1))
    // stage3 submitted
    eventLogger.onStageSubmitted(MemoryListenerSuite.createStageStartEvent(3))
    val execMetrics3 = MemoryListenerSuite.createExecutorMetrics(hostName, port, 3L, 30, 30)
    eventLogger.onExecutorMetricsUpdate(MemoryListenerSuite.createExecutorMetricsUpdateEvent(
      execId, execMetrics3))
    val execMetrics4 = MemoryListenerSuite.createExecutorMetrics(hostName, port, 4L, 20, 25)
    eventLogger.onExecutorMetricsUpdate(MemoryListenerSuite.createExecutorMetricsUpdateEvent(
      execId, execMetrics4))
    // stage 2 completed
    eventLogger.onStageCompleted(MemoryListenerSuite.createStageEndEvent(2))
    val execMetrics5 = MemoryListenerSuite.createExecutorMetrics(hostName, port, 5L, 15, 15)
    eventLogger.onExecutorMetricsUpdate(MemoryListenerSuite.createExecutorMetricsUpdateEvent(
      execId, execMetrics5))
    val execMetrics6 = MemoryListenerSuite.createExecutorMetrics(hostName, port, 6L, 25, 10)
    eventLogger.onExecutorMetricsUpdate(MemoryListenerSuite.createExecutorMetricsUpdateEvent(
      execId, execMetrics6))
    // stage 3 completed
    eventLogger.onStageCompleted(MemoryListenerSuite.createStageEndEvent(3))

    eventLogger.onExecutorRemoved(SparkListenerExecutorRemoved(7L, execId, ""))

    // Totally there are 15 logged events, including:
    // 2 events of executor Added/Removed
    // 6 events of stage Submitted/Completed
    // 7 events of executorMetrics update (3 combined metrics and 4 original metrics)
    assert(eventLogger.loggedEvents.size === 15)
    eventLogger.stop()

    val logData = EventLoggingListener.openEventLog(new Path(eventLogger.logPath), fileSystem)
    val lines = readLines(logData)
    Utils.tryWithSafeFinally {
      // totally there are 16 lines, including SparkListenerLogStart event and 15 other events
      assert(lines.size === 16)

      val listenerBus = new LiveListenerBus
      val memoryListener = new MemoryListener
      listenerBus.addListener(memoryListener)

      val sparkEvents: Seq[SparkListenerEvent] = lines.map { line =>
        val event = JsonProtocol.sparkEventFromJson(parse(line))
        listenerBus.postToAll(event)
        event
      }

      // Make sure there always an original {{SparkListenerExecutorMetricsUpdate}} event updated
      // before each stage complete.
      val latestMetricsStage1 = getLatestExecutorMetricsBeforeStageEnd(sparkEvents, 1).get
      val latestMetricsStage2 = getLatestExecutorMetricsBeforeStageEnd(sparkEvents, 2).get
      val latestMetricsStage3 = getLatestExecutorMetricsBeforeStageEnd(sparkEvents, 3).get
      assertMetrics(execId, (hostName, 2L, 30, 10), latestMetricsStage1)
      assertMetrics(execId, (hostName, 4L, 20, 25), latestMetricsStage2)
      assertMetrics(execId, (hostName, 6L, 25, 10), latestMetricsStage3)

      // Following is an integration test with [[org.apache.spark.ui.memory.MemoryListener]], make
      // sure the events logged in history file can work correctly.
      val mapForStage1 = memoryListener.completedStagesToMem((1, 0))
      val transMetrics1 = mapForStage1(execId).transportInfo.get
      MemoryListenerSuite.assertTransMetrics(
        30, 10, MemTime(30, 2L), MemTime(10, 2L), transMetrics1)
      val mapForStage2 = memoryListener.completedStagesToMem((2, 0))
      val transMetrics2 = mapForStage2(execId).transportInfo.get
      MemoryListenerSuite.assertTransMetrics(
        20, 25, MemTime(30, 3L), MemTime(30, 3L), transMetrics2)
      val mapForStage3 = memoryListener.completedStagesToMem((3, 0))
      val transMetrics3 = mapForStage3(execId).transportInfo.get
      MemoryListenerSuite.assertTransMetrics(
        25, 10, MemTime(30, 3L), MemTime(30, 3L), transMetrics3)
    } {
      logData.close()
    }

    def getLatestExecutorMetricsBeforeStageEnd(
        events: Seq[SparkListenerEvent],
        stageId: Int): Option[SparkListenerExecutorMetricsUpdate] = {
      val itr = events.iterator
      var latestMetrics: Option[SparkListenerExecutorMetricsUpdate] = None
      var isStageSubmitted: Boolean = false
      while(itr.hasNext) {
        val event = itr.next()
        event match {
          case ss: SparkListenerStageSubmitted if ss.stageInfo.stageId == stageId =>
            isStageSubmitted = true
          case sc: SparkListenerStageCompleted if sc.stageInfo.stageId == stageId =>
            return latestMetrics
          case emu: SparkListenerExecutorMetricsUpdate if isStageSubmitted =>
            latestMetrics = Some(emu)
          case _ => // Do nothing for other events
        }
      }
      latestMetrics
    }

    def assertMetrics(
        execId: String,
        metricsDetails: (String, Long, Long, Long),
        event: SparkListenerExecutorMetricsUpdate): Unit = {
      val execMetrics = event.executorMetrics
      assert(execId === event.execId)
      assert(metricsDetails._1 === execMetrics.hostname)
      assert(metricsDetails._2 === execMetrics.transportMetrics.timeStamp)
      assert(metricsDetails._3 === execMetrics.transportMetrics.onHeapSize)
      assert(metricsDetails._4 === execMetrics.transportMetrics.offHeapSize)
    }
  }

  /* ----------------- *
   * Actual test logic *
   * ----------------- */

  import EventLoggingListenerSuite._

  /**
   * Test basic event logging functionality.
   *
   * This creates two simple events, posts them to the EventLoggingListener, and verifies that
   * exactly these two events are logged in the expected file.
   */
  private def testEventLogging(
      compressionCodec: Option[String] = None,
      extraConf: Map[String, String] = Map()) {
    val conf = getLoggingConf(testDirPath, compressionCodec)
    extraConf.foreach { case (k, v) => conf.set(k, v) }
    val logName = compressionCodec.map("test-" + _).getOrElse("test")
    val eventLogger = new EventLoggingListener(logName, None, testDirPath.toUri(), conf)
    val listenerBus = new LiveListenerBus(sc)
    val applicationStart = SparkListenerApplicationStart("Greatest App (N)ever", None,
      125L, "Mickey", None)
    val applicationEnd = SparkListenerApplicationEnd(1000L)

    // A comprehensive test on JSON de/serialization of all events is in JsonProtocolSuite
    eventLogger.start()
    listenerBus.start()
    listenerBus.addListener(eventLogger)
    listenerBus.postToAll(applicationStart)
    listenerBus.postToAll(applicationEnd)
    eventLogger.stop()

    // Verify file contains exactly the two events logged
    val logData = EventLoggingListener.openEventLog(new Path(eventLogger.logPath), fileSystem)
    Utils.tryWithSafeFinally {
      val lines = readLines(logData)
      val logStart = SparkListenerLogStart(SPARK_VERSION)
      assert(lines.size === 3)
      assert(lines(0).contains("SparkListenerLogStart"))
      assert(lines(1).contains("SparkListenerApplicationStart"))
      assert(lines(2).contains("SparkListenerApplicationEnd"))
      assert(JsonProtocol.sparkEventFromJson(parse(lines(0))) === logStart)
      assert(JsonProtocol.sparkEventFromJson(parse(lines(1))) === applicationStart)
      assert(JsonProtocol.sparkEventFromJson(parse(lines(2))) === applicationEnd)
    } {
      logData.close()
    }
  }

  /**
   * Test end-to-end event logging functionality in an application.
   * This runs a simple Spark job and asserts that the expected events are logged when expected.
   */
  private def testApplicationEventLogging(compressionCodec: Option[String] = None) {
    // Set defaultFS to something that would cause an exception, to make sure we don't run
    // into SPARK-6688.
    val conf = getLoggingConf(testDirPath, compressionCodec)
      .set("spark.hadoop.fs.defaultFS", "unsupported://example.com")
    sc = new SparkContext("local-cluster[2,2,1024]", "test", conf)
    assert(sc.eventLogger.isDefined)
    val eventLogger = sc.eventLogger.get
    val eventLogPath = eventLogger.logPath
    val expectedLogDir = testDir.toURI()
    assert(eventLogPath === EventLoggingListener.getLogPath(
      expectedLogDir, sc.applicationId, None, compressionCodec.map(CompressionCodec.getShortName)))

    // Begin listening for events that trigger asserts
    val eventExistenceListener = new EventExistenceListener(eventLogger)
    sc.addSparkListener(eventExistenceListener)

    // Trigger asserts for whether the expected events are actually logged
    sc.parallelize(1 to 10000).count()
    sc.stop()

    // Ensure all asserts have actually been triggered
    eventExistenceListener.assertAllCallbacksInvoked()

    // Make sure expected events exist in the log file.
    val logData = EventLoggingListener.openEventLog(new Path(eventLogger.logPath), fileSystem)
    val eventSet = mutable.Set(
      SparkListenerApplicationStart,
      SparkListenerBlockManagerAdded,
      SparkListenerExecutorAdded,
      SparkListenerEnvironmentUpdate,
      SparkListenerJobStart,
      SparkListenerJobEnd,
      SparkListenerStageSubmitted,
      SparkListenerStageCompleted,
      SparkListenerTaskStart,
      SparkListenerTaskEnd,
      SparkListenerApplicationEnd).map(Utils.getFormattedClassName)
    Utils.tryWithSafeFinally {
      val logStart = SparkListenerLogStart(SPARK_VERSION)
      val lines = readLines(logData)
      lines.foreach { line =>
        eventSet.foreach { event =>
          if (line.contains(event)) {
            val parsedEvent = JsonProtocol.sparkEventFromJson(parse(line))
            val eventType = Utils.getFormattedClassName(parsedEvent)
            if (eventType == event) {
              eventSet.remove(event)
            }
          }
        }
      }
      assert(JsonProtocol.sparkEventFromJson(parse(lines(0))) === logStart)
      assert(eventSet.isEmpty, "The following events are missing: " + eventSet.toSeq)
    } {
      logData.close()
    }
  }

  private def readLines(in: InputStream): Seq[String] = {
    Source.fromInputStream(in).getLines().toSeq
  }

  /**
   * A listener that asserts certain events are logged by the given EventLoggingListener.
   * This is necessary because events are posted asynchronously in a different thread.
   */
  private class EventExistenceListener(eventLogger: EventLoggingListener) extends SparkListener {
    var jobStarted = false
    var jobEnded = false
    var appEnded = false

    override def onJobStart(jobStart: SparkListenerJobStart) {
      jobStarted = true
    }

    override def onJobEnd(jobEnd: SparkListenerJobEnd) {
      jobEnded = true
    }

    override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd) {
      appEnded = true
    }

    def assertAllCallbacksInvoked() {
      assert(jobStarted, "JobStart callback not invoked!")
      assert(jobEnded, "JobEnd callback not invoked!")
      assert(appEnded, "ApplicationEnd callback not invoked!")
    }
  }

}


object EventLoggingListenerSuite {

  /** Get a SparkConf with event logging enabled. */
  def getLoggingConf(logDir: Path, compressionCodec: Option[String] = None): SparkConf = {
    val conf = new SparkConf
    conf.set("spark.eventLog.enabled", "true")
    conf.set("spark.eventLog.testing", "true")
    conf.set("spark.eventLog.dir", logDir.toUri.toString)
    compressionCodec.foreach { codec =>
      conf.set("spark.eventLog.compress", "true")
      conf.set("spark.io.compression.codec", codec)
    }
    conf
  }

  def getUniqueApplicationId: String = "test-" + System.currentTimeMillis
}
