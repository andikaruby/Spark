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

package org.apache.spark.sql.hive

import java.io.IOException
import java.text.NumberFormat
import java.util.Date

import org.apache.hadoop.fs.Path
import org.apache.hadoop.hive.ql.exec.{FileSinkOperator, Utilities}
import org.apache.hadoop.hive.ql.io.{HiveFileFormatUtils, HiveOutputFormat}
import org.apache.hadoop.hive.ql.plan.FileSinkDesc
import org.apache.hadoop.mapred._
import org.apache.hadoop.io.Writable

import org.apache.spark.{SparkHadoopWriter, SerializableWritable, Logging}
import org.apache.hadoop.hive.ql.plan.TableDesc
import org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter
import org.apache.hadoop.hive.ql.io.HiveOutputFormat
import org.apache.hadoop.hive.ql.io.HivePassThroughOutputFormat
import org.apache.hadoop.util.ReflectionUtils
import org.apache.hadoop.hive.ql.exec.Utilities
import org.apache.hadoop.io.compress.CompressionCodec
import org.apache.hadoop.io.SequenceFile.CompressionType
import org.apache.hadoop.hive.ql.io.HiveFileFormatUtils

/**
 * Internal helper class that saves an RDD using a Hive OutputFormat.
 * It is based on [[SparkHadoopWriter]].
 */
private[hive] class SparkHiveHadoopWriter(
    @transient jobConf: JobConf,
    isCompressed: Boolean,
    tableDesc: TableDesc,
    compressCodec: String,
    compressType: String)
  extends Logging
  with SparkHadoopMapRedUtil
  with Serializable {

  private val now = new Date()
  private val conf = new SerializableWritable(jobConf)

  private var jobID = 0
  private var splitID = 0
  private var attemptID = 0
  private var jID: SerializableWritable[JobID] = null
  private var taID: SerializableWritable[TaskAttemptID] = null

  @transient private var writer: RecordWriter = null
  @transient private var format: HiveOutputFormat[_, _] = null
  @transient private var committer: OutputCommitter = null
  @transient private var jobContext: JobContext = null
  @transient private var taskContext: TaskAttemptContext = null

  def preSetup() {
    setIDs(0, 0, 0)
    setConfParams()

    val jCtxt = getJobContext()
    getOutputCommitter().setupJob(jCtxt)
  }


  def setup(jobid: Int, splitid: Int, attemptid: Int) {
    setIDs(jobid, splitid, attemptid)
    setConfParams()
  }

  def open() {
    val numfmt = NumberFormat.getInstance()
    numfmt.setMinimumIntegerDigits(5)
    numfmt.setGroupingUsed(false)
    val jc = conf.value

    getOutputCommitter().setupTask(getTaskContext())
    var hiveOutputFormat: HiveOutputFormat[_, _] = tableDesc.getOutputFileFormatClass.newInstance.
      asInstanceOf[HiveOutputFormat[_, _]]

    format = hiveOutputFormat

    val extension = Utilities.getFileExtension(
      jc,
      isCompressed,
      getOutputFormat())

    val outputName = "part-"  + numfmt.format(splitID) + extension
    val path = FileOutputFormat.getTaskOutputPath(jc, outputName)

    var jc_output: JobConf = jc
    if (isCompressed) {
      jc_output = new JobConf(jc)
      val codecStr: String = compressCodec
      if (codecStr != null && !(codecStr.trim == "")) {
        val codec: Class[_ <: CompressionCodec] =
          Class.forName(codecStr).asInstanceOf[Class[_ <: CompressionCodec]]
        FileOutputFormat.setOutputCompressorClass(jc_output, codec)
      }
      val typ: String = compressType
      if (typ != null && !(typ.trim == "")) {
        val style: CompressionType = CompressionType.valueOf(typ)
        SequenceFileOutputFormat.setOutputCompressionType(jc, style)
      }
    }
    writer = HiveFileFormatUtils.getRecordWriter(jc_output, hiveOutputFormat,
      jc.getOutputValueClass.asInstanceOf[Class[Writable]],
      isCompressed, tableDesc.getProperties, path, null)
  }

  def write(value: Writable) {
    if (writer != null) {
      writer.write(value)
    } else {
      throw new IOException("Writer is null, open() has not been called")
    }
  }

  def close() {
    // Seems the boolean value passed into close does not matter.
    writer.close(false)
  }

  def commit() {
    val taCtxt = getTaskContext()
    val cmtr = getOutputCommitter()
    if (cmtr.needsTaskCommit(taCtxt)) {
      try {
        cmtr.commitTask(taCtxt)
        logInfo (taID + ": Committed")
      } catch {
        case e: IOException =>
          logError("Error committing the output of task: " + taID.value, e)
          cmtr.abortTask(taCtxt)
          throw e
      }
    } else {
      logWarning ("No need to commit output of task: " + taID.value)
    }
  }

  def commitJob() {
    // always ? Or if cmtr.needsTaskCommit ?
    val cmtr = getOutputCommitter()
    cmtr.commitJob(getJobContext())
  }

  // ********* Private Functions *********

  private def getOutputFormat(): HiveOutputFormat[_,_] = {
    if (format == null) {
      format = conf.value.getOutputFormat()
        .asInstanceOf[HiveOutputFormat[_, _]]
    }
    format
  }

  private def getOutputCommitter(): OutputCommitter = {
    if (committer == null) {
      committer = conf.value.getOutputCommitter
    }
    committer
  }

  private def getJobContext(): JobContext = {
    if (jobContext == null) {
      jobContext = newJobContext(conf.value, jID.value)
    }
    jobContext
  }

  private def getTaskContext(): TaskAttemptContext = {
    if (taskContext == null) {
      taskContext =  newTaskAttemptContext(conf.value, taID.value)
    }
    taskContext
  }

  private def setIDs(jobId: Int, splitId: Int, attemptId: Int) {
    jobID = jobId
    splitID = splitId
    attemptID = attemptId

    jID = new SerializableWritable[JobID](SparkHadoopWriter.createJobID(now, jobId))
    taID = new SerializableWritable[TaskAttemptID](
      new TaskAttemptID(new TaskID(jID.value, true, splitID), attemptID))
  }

  private def setConfParams() {
    conf.value.set("mapred.job.id", jID.value.toString)
    conf.value.set("mapred.tip.id", taID.value.getTaskID.toString)
    conf.value.set("mapred.task.id", taID.value.toString)
    conf.value.setBoolean("mapred.task.is.map", true)
    conf.value.setInt("mapred.task.partition", splitID)
  }
}

private[hive] object SparkHiveHadoopWriter {
  def createPathFromString(path: String, conf: JobConf): Path = {
    if (path == null) {
      throw new IllegalArgumentException("Output path is null")
    }
    val outputPath = new Path(path)
    val fs = outputPath.getFileSystem(conf)
    if (outputPath == null || fs == null) {
      throw new IllegalArgumentException("Incorrectly formatted output path")
    }
    outputPath.makeQualified(fs)
  }
}
