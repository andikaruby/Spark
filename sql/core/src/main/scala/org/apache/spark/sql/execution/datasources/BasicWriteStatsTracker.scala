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

package org.apache.spark.sql.execution.datasources

import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

import scala.collection.mutable

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

import org.apache.spark.{SparkContext, TaskContext}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.catalog.CatalogTypes.TablePartitionSpec
import org.apache.spark.sql.execution.SQLExecution
import org.apache.spark.sql.execution.metric.{SQLMetric, SQLMetrics}
import org.apache.spark.util.SerializableConfiguration


/**
 * Simple metrics collected during an instance of [[FileFormatDataWriter]].
 * These were first introduced in https://github.com/apache/spark/pull/18159 (SPARK-20703).
 */
case class BasicWriteTaskStats(
    partitionSpecWithStats: mutable.Map[TablePartitionSpec, PartitionStats],
    totalNumFiles: Int,
    totalNumBytes: Long,
    totalNumRows: Long)
  extends WriteTaskStats

case class PartitionStats(var numFiles: Int = 0, var numBytes: Long = 0, var numRows: Long = 0) {
  def updateNumFiles(num: Int): Unit = numFiles = numFiles + num

  def updateNumBytes(size: Long): Unit = numBytes = numBytes + size

  def updateNumRows(num: Long): Unit = numRows = numRows + num

  def merge(stats: PartitionStats): Unit = {
    updateNumFiles(stats.numFiles)
    updateNumBytes(stats.numBytes)
    updateNumRows(stats.numRows)
  }
}

/**
 * Simple [[WriteTaskStatsTracker]] implementation that produces [[BasicWriteTaskStats]].
 */
class BasicWriteTaskStatsTracker(hadoopConf: Configuration)
  extends WriteTaskStatsTracker with Logging {

  private[this] val partitionsStats: mutable.Map[TablePartitionSpec, PartitionStats] =
    mutable.Map.empty
  private[this] var totalNumFiles: Int = 0
  private[this] var numSubmittedFiles: Int = 0
  private[this] var totalNumBytes: Long = 0L
  private[this] var totalNumRows: Long = 0L

  private[this] var curPartitionValue: Option[TablePartitionSpec] = None
  private[this] val submittedFiles = mutable.HashSet[String]()

  /**
   * Get the size of the file expected to have been written by a worker.
   * @param filePath path to the file
   * @return the file size or None if the file was not found.
   */
  private def getFileSize(filePath: String): Option[Long] = {
    val path = new Path(filePath)
    val fs = path.getFileSystem(hadoopConf)
    getFileSize(fs, path)
  }

  /**
   * Get the size of the file expected to have been written by a worker.
   * This supports the XAttr in HADOOP-17414 when the "magic committer" adds
   * a custom HTTP header to the a zero byte marker.
   * If the output file as returned by getFileStatus > 0 then the length if
   * returned. For zero-byte files, the (optional) Hadoop FS API getXAttr() is
   * invoked. If a parseable, non-negative length can be retrieved, this
   * is returned instead of the length.
   * @return the file size or None if the file was not found.
   */
  private [datasources] def getFileSize(fs: FileSystem, path: Path): Option[Long] = {
    // the normal file status probe.
    try {
      val len = fs.getFileStatus(path).getLen
      if (len > 0) {
        return Some(len)
      }
    } catch {
      case e: FileNotFoundException =>
        // may arise against eventually consistent object stores.
        logDebug(s"File $path is not yet visible", e)
        return None
    }

    // Output File Size is 0. Look to see if it has an attribute
    // declaring a future-file-length.
    // Failure of API call, parsing, invalid value all return the
    // 0 byte length.

    var len = 0L
    try {
      val attr = fs.getXAttr(path, BasicWriteJobStatsTracker.FILE_LENGTH_XATTR)
      if (attr != null && attr.nonEmpty) {
        val str = new String(attr, StandardCharsets.UTF_8)
        logDebug(s"File Length statistics for $path retrieved from XAttr: $str")
        // a non-empty header was found. parse to a long via the java class
        val l = java.lang.Long.parseLong(str)
        if (l > 0) {
          len = l
        } else {
          logDebug("Ignoring negative value in XAttr file length")
        }
      }
    } catch {
      case e: NumberFormatException =>
        // warn but don't dump the whole stack
        logInfo(s"Failed to parse" +
          s" ${BasicWriteJobStatsTracker.FILE_LENGTH_XATTR}:$e;" +
          s" bytes written may be under-reported");
      case e: UnsupportedOperationException =>
        // this is not unusual; ignore
        logDebug(s"XAttr not supported on path $path", e);
      case e: Exception =>
        // Something else. Log at debug and continue.
        logDebug(s"XAttr processing failure on $path", e);
    }
    Some(len)
  }

  override def newPartition(partitionValues: TablePartitionSpec): Unit = {
    curPartitionValue = Some(partitionValues)
    val origin = partitionsStats.getOrElse(partitionValues, PartitionStats())
    partitionsStats.put(partitionValues, origin)
  }

  override def newFile(filePath: String): Unit = {
    submittedFiles += filePath
    numSubmittedFiles += 1
  }

  override def closeFile(filePath: String): Unit = {
    updateFileStats(filePath)
    submittedFiles.remove(filePath)
  }

  private def updateFileStats(filePath: String): Unit = {
    getFileSize(filePath).foreach { len =>
      curPartitionValue.foreach { partitionValue =>
        val partitionStats = partitionsStats.getOrElse(partitionValue, PartitionStats())
        partitionStats.updateNumFiles(1)
        partitionStats.updateNumBytes(len)
        partitionsStats.update(partitionValue, partitionStats)
      }
      totalNumBytes += len
      totalNumFiles += 1
    }
  }

  override def newRow(filePath: String, row: InternalRow): Unit = {
    curPartitionValue.foreach { partitionValue =>
      val partitionStats = partitionsStats.getOrElse(partitionValue, PartitionStats())
      partitionStats.updateNumRows(1)
      partitionsStats.update(partitionValue, partitionStats)
    }
    totalNumRows += 1
  }

  override def getFinalStats(): WriteTaskStats = {
    submittedFiles.foreach(updateFileStats)
    submittedFiles.clear()

    // Reports bytesWritten and recordsWritten to the Spark output metrics.
    Option(TaskContext.get()).map(_.taskMetrics().outputMetrics).foreach { outputMetrics =>
      outputMetrics.setBytesWritten(totalNumBytes)
      outputMetrics.setRecordsWritten(totalNumRows)
    }

    if (numSubmittedFiles != totalNumFiles) {
      logInfo(s"Expected $numSubmittedFiles files, but only saw $totalNumFiles. " +
        "This could be due to the output format not writing empty files, " +
        "or files being not immediately visible in the filesystem.")
    }
    BasicWriteTaskStats(partitionsStats, totalNumFiles, totalNumBytes, totalNumRows)
  }
}


/**
 * Simple [[WriteJobStatsTracker]] implementation that's serializable, capable of
 * instantiating [[BasicWriteTaskStatsTracker]] on executors and processing the
 * [[BasicWriteTaskStats]] they produce by aggregating the metrics and posting them
 * as DriverMetricUpdates.
 */
class BasicWriteJobStatsTracker(
    serializableHadoopConf: SerializableConfiguration,
    @transient val metrics: Map[String, SQLMetric])
  extends WriteJobStatsTracker {

  @transient val partitionsStats: mutable.Map[TablePartitionSpec, PartitionStats] =
    mutable.Map.empty
  @transient var numFiles: Long = 0L
  @transient var totalNumBytes: Long = 0L
  @transient var totalNumOutput: Long = 0L

  override def newTaskInstance(): WriteTaskStatsTracker = {
    new BasicWriteTaskStatsTracker(serializableHadoopConf.value)
  }

  override def processStats(stats: Seq[WriteTaskStats]): Unit = {
    val sparkContext = SparkContext.getActive.get
    val basicStats = stats.map(_.asInstanceOf[BasicWriteTaskStats])

    basicStats.foreach { summary =>
      summary.partitionSpecWithStats.foreach { case (partitionValue, stats) =>
        val currentStats = partitionsStats.getOrElse(partitionValue, PartitionStats())
        currentStats.merge(stats)
        partitionsStats.put(partitionValue, currentStats)
      }
      numFiles += summary.totalNumFiles
      totalNumBytes += summary.totalNumBytes
      totalNumOutput += summary.totalNumRows
    }

    metrics(BasicWriteJobStatsTracker.NUM_FILES_KEY).add(numFiles)
    metrics(BasicWriteJobStatsTracker.NUM_OUTPUT_BYTES_KEY).add(totalNumBytes)
    metrics(BasicWriteJobStatsTracker.NUM_OUTPUT_ROWS_KEY).add(totalNumOutput)
    metrics(BasicWriteJobStatsTracker.NUM_PARTS_KEY).add(partitionsStats.keys.size)

    val executionId = sparkContext.getLocalProperty(SQLExecution.EXECUTION_ID_KEY)
    SQLMetrics.postDriverMetricUpdates(sparkContext, executionId, metrics.values.toList)
  }
}

object BasicWriteJobStatsTracker {
  private val NUM_FILES_KEY = "numFiles"
  private val NUM_OUTPUT_BYTES_KEY = "numOutputBytes"
  private val NUM_OUTPUT_ROWS_KEY = "numOutputRows"
  private val NUM_PARTS_KEY = "numParts"
  /** XAttr key of the data length header added in HADOOP-17414. */
  val FILE_LENGTH_XATTR = "header.x-hadoop-s3a-magic-data-length"

  def metrics: Map[String, SQLMetric] = {
    val sparkContext = SparkContext.getActive.get
    Map(
      NUM_FILES_KEY -> SQLMetrics.createMetric(sparkContext, "number of written files"),
      NUM_OUTPUT_BYTES_KEY -> SQLMetrics.createSizeMetric(sparkContext, "written output"),
      NUM_OUTPUT_ROWS_KEY -> SQLMetrics.createMetric(sparkContext, "number of output rows"),
      NUM_PARTS_KEY -> SQLMetrics.createMetric(sparkContext, "number of dynamic part")
    )
  }
}
