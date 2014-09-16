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

import java.io.{BufferedOutputStream, FileOutputStream, IOException, PrintWriter}
import java.net.URI

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileStatus, FileSystem, FSDataOutputStream, Path}
import org.apache.hadoop.fs.permission.FsPermission
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods._

import org.apache.spark.{Logging, SparkConf, SparkContext, SPARK_VERSION}
import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.spark.io.CompressionCodec
import org.apache.spark.util.{JsonProtocol, Utils}

/**
 * A SparkListener that logs events to persistent storage.
 *
 * Event logging is specified by the following configurable parameters:
 *   spark.eventLog.enabled - Whether event logging is enabled.
 *   spark.eventLog.compress - Whether to compress logged events
 *   spark.eventLog.overwrite - Whether to overwrite any existing files.
 *   spark.eventLog.dir - Path to the directory in which events are logged.
 *   spark.eventLog.buffer.kb - Buffer size to use when writing to output streams
 */
private[spark] class EventLoggingListener(
    appName: String,
    sparkConf: SparkConf,
    hadoopConf: Configuration)
  extends SparkListener with Logging {

  import EventLoggingListener._

  def this(appName: String, sparkConf: SparkConf) =
    this(appName, sparkConf, SparkHadoopUtil.get.newConfiguration(sparkConf))

  private val shouldCompress = sparkConf.getBoolean("spark.eventLog.compress", false)
  private val shouldOverwrite = sparkConf.getBoolean("spark.eventLog.overwrite", false)
  private val testing = sparkConf.getBoolean("spark.eventLog.testing", false)
  private val outputBufferSize = sparkConf.getInt("spark.eventLog.buffer.kb", 100) * 1024
  private val logBaseDir = sparkConf.get("spark.eventLog.dir", DEFAULT_LOG_DIR).stripSuffix("/")
  private val fileSystem = Utils.getHadoopFileSystem(new URI(logBaseDir), hadoopConf)
  private lazy val compressionCodec = CompressionCodec.createCodec(sparkConf)

  // Only defined if the file system scheme is not local
  private var hadoopDataStream: Option[FSDataOutputStream] = None

  // The Hadoop APIs have changed over time, so we use reflection to figure out
  // the correct method to use to flush a hadoop data stream. See SPARK-1518
  // for details.
  private val hadoopFlushMethod = {
    val cls = classOf[FSDataOutputStream]
    scala.util.Try(cls.getMethod("hflush")).getOrElse(cls.getMethod("sync"))
  }

  private var writer: Option[PrintWriter] = None

  // For testing. Keep track of all JSON serialized events that have been logged.
  private[scheduler] val loggedEvents = new ArrayBuffer[JValue]

  val logPath = {
      val sb = new StringBuilder()
        .append(logBaseDir)
        .append("/")
        .append(appName.replaceAll("[ :/]", "-").toLowerCase())
        .append("-")
        .append(System.currentTimeMillis())
        .append("-")
        .append(SPARK_VERSION)
      if (shouldCompress) {
        val codec =
          sparkConf.get("spark.io.compression.codec", CompressionCodec.DEFAULT_COMPRESSION_CODEC)
        sb.append("-").append(codec)
      }
      sb.toString()
    }

  /**
   * Creates the log file in the configured log directory.
   *
   * The file name contains some metadata about its contents. It follows the following
   * format:
   *
   * {{{
   *    {app name}-{timestamp}.{spark version}[.{compression codec}][.inprogress]
   * }}}
   *
   * Where:
   * - "app name" is a fs-friendly version of the application's name, in lower case
   * - "timestamp" is a timestamp generated by this logger
   * - "spark version" is the version of spark that generated the logs
   * - "compression codec" is an optional string with the name of the compression codec
   *   used to write the file
   * - ".inprogress" will be present while the log file is still being written to, and
   *   removed after the application is finished.
   */
  def start() {
    if (!fileSystem.isDirectory(new Path(logBaseDir))) {
      throw new IllegalArgumentException(s"Log directory $logBaseDir does not exist.");
    }

    val workingPath = logPath + IN_PROGRESS
    val uri = new URI(workingPath)
    val path = new Path(workingPath)
    val defaultFs = FileSystem.getDefaultUri(hadoopConf).getScheme
    val isDefaultLocal = defaultFs == null || defaultFs == "file"

    /* The Hadoop LocalFileSystem (r1.0.4) has known issues with syncing (HADOOP-7844).
     * Therefore, for local files, use FileOutputStream instead. */
    val dstream =
      if ((isDefaultLocal && uri.getScheme == null) || uri.getScheme == "file") {
        // Second parameter is whether to append
        new FileOutputStream(uri.getPath)
      } else {
        hadoopDataStream = Some(fileSystem.create(path))
        hadoopDataStream.get
      }

    fileSystem.setPermission(path, LOG_FILE_PERMISSIONS)
    val bstream = new BufferedOutputStream(dstream, outputBufferSize)
    val cstream = if (shouldCompress) compressionCodec.compressedOutputStream(bstream) else bstream
    writer = Some(new PrintWriter(cstream))

    logInfo("Logging events to %s".format(logPath))
  }

  /** Log the event as JSON. */
  private def logEvent(event: SparkListenerEvent, flushLogger: Boolean = false) {
    val eventJson = JsonProtocol.sparkEventToJson(event)
    writer.foreach(_.println(compact(render(eventJson))))
    if (flushLogger) {
      writer.foreach(_.flush())
      hadoopDataStream.foreach(hadoopFlushMethod.invoke(_))
    }
    if (testing) {
      loggedEvents += eventJson
    }
  }

  override def onStageSubmitted(event: SparkListenerStageSubmitted) =
    logEvent(event)
  override def onTaskStart(event: SparkListenerTaskStart) =
    logEvent(event)
  override def onTaskGettingResult(event: SparkListenerTaskGettingResult) =
    logEvent(event)
  override def onTaskEnd(event: SparkListenerTaskEnd) =
    logEvent(event)
  override def onEnvironmentUpdate(event: SparkListenerEnvironmentUpdate) =
    logEvent(event)
  override def onStageCompleted(event: SparkListenerStageCompleted) =
    logEvent(event, flushLogger = true)
  override def onJobStart(event: SparkListenerJobStart) =
    logEvent(event, flushLogger = true)
  override def onJobEnd(event: SparkListenerJobEnd) =
    logEvent(event, flushLogger = true)
  override def onBlockManagerAdded(event: SparkListenerBlockManagerAdded) =
    logEvent(event, flushLogger = true)
  override def onBlockManagerRemoved(event: SparkListenerBlockManagerRemoved) =
    logEvent(event, flushLogger = true)
  override def onUnpersistRDD(event: SparkListenerUnpersistRDD) =
    logEvent(event, flushLogger = true)
  override def onApplicationStart(event: SparkListenerApplicationStart) =
    logEvent(event, flushLogger = true)
  override def onApplicationEnd(event: SparkListenerApplicationEnd) =
    logEvent(event, flushLogger = true)
  // No-op because logging every update would be overkill
  override def onExecutorMetricsUpdate(event: SparkListenerExecutorMetricsUpdate) { }

  /**
   * Stop logging events.
   * In addition, create an empty special file to indicate application completion.
   */
  def stop() = {
    writer.foreach(_.close())

    val target = new Path(logPath)
    if (fileSystem.exists(target)) {
      throw new IOException("Target log file already exists (%s)".format(logPath))
    }
    fileSystem.rename(new Path(logPath + IN_PROGRESS), target)
  }

}

private[spark] object EventLoggingListener extends Logging {
  val DEFAULT_LOG_DIR = "/tmp/spark-events"
  val IN_PROGRESS = ".inprogress"
  val LOG_FILE_PERMISSIONS = FsPermission.createImmutable(Integer.parseInt("770", 8).toShort)

  // Regex for parsing log file names. See description of log file name format in start().
  val LOG_FILE_NAME_REGEX = s"(.+)-([0-9]+)-([0-9](?:\\.[0-9])*)(?:-(.+?))?(\\$IN_PROGRESS)?".r

  // A cache for compression codecs to avoid creating the same codec many times
  private val codecMap = new mutable.HashMap[String, CompressionCodec]

  /**
   * Parse the event logging information associated with the logs in the given directory.
   *
   * Specifically, this looks for event log files, the Spark version file, the compression
   * codec file (if event logs are compressed), and the application completion file (if the
   * application has run to completion).
   */
  def parseLoggingInfo(log: Path): EventLoggingInfo = {
    try {
      val LOG_FILE_NAME_REGEX(_, _, version, codecName, inprogress) = log.getName()
      val codec: Option[CompressionCodec] = if (codecName != null) {
          val conf = new SparkConf()
          conf.set("spark.io.compression.codec", codecName)
          Some(CompressionCodec.createCodec(conf))
        } else {
          None
        }
      EventLoggingInfo(log, version, codec, inprogress == null)
    } catch {
      case e: Exception =>
        logWarning("Exception in parsing logging info from file %s".format(log), e)
        null
    }
  }

}


/**
 * Information needed to process the event logs associated with an application.
 */
private[spark] case class EventLoggingInfo(
    path: Path,
    sparkVersion: String,
    compressionCodec: Option[CompressionCodec],
    applicationComplete: Boolean = false)
