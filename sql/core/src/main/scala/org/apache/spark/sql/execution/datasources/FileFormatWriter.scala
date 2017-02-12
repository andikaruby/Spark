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

import java.util.{Date, UUID}

import scala.collection.mutable

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl

import org.apache.spark._
import org.apache.spark.internal.Logging
import org.apache.spark.internal.io.{FileCommitProtocol, SparkHadoopWriterUtils}
import org.apache.spark.internal.io.FileCommitProtocol.TaskCommitMessage
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.catalog.{BucketSpec, ExternalCatalogUtils}
import org.apache.spark.sql.catalyst.catalog.CatalogTypes.TablePartitionSpec
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.physical.HashPartitioning
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.execution.{QueryExecution, SortExec, SQLExecution}
import org.apache.spark.sql.types.{StringType, StructType}
import org.apache.spark.util.{SerializableConfiguration, Utils}


/** A helper object for writing FileFormat data out to a location. */
object FileFormatWriter extends Logging {

  /**
   * Max number of files a single task writes out due to file size. In most cases the number of
   * files written should be very small. This is just a safe guard to protect some really bad
   * settings, e.g. maxRecordsPerFile = 1.
   */
  private val MAX_FILE_COUNTER = 1000 * 1000

  /** Describes how output files should be placed in the filesystem. */
  case class OutputSpec(
    outputPath: String, customPartitionLocations: Map[TablePartitionSpec, String])

  /** A shared job description for all the write tasks. */
  private class WriteJobDescription(
      val uuid: String,  // prevent collision between different (appending) write jobs
      val serializableHadoopConf: SerializableConfiguration,
      val outputWriterFactory: OutputWriterFactory,
      val allColumns: Seq[Attribute],
      val dataColumns: Seq[Attribute],
      val partitionColumns: Seq[Attribute],
      val bucketColumns: Seq[Attribute],
      val numBuckets: Int,
      val path: String,
      val customPartitionLocations: Map[TablePartitionSpec, String],
      val maxRecordsPerFile: Long)
    extends Serializable {

    assert(AttributeSet(allColumns) == AttributeSet(partitionColumns ++ dataColumns),
      s"""
         |All columns: ${allColumns.mkString(", ")}
         |Partition columns: ${partitionColumns.mkString(", ")}
         |Data columns: ${dataColumns.mkString(", ")}
       """.stripMargin)
  }

  /**
   * Basic work flow of this command is:
   * 1. Driver side setup, including output committer initialization and data source specific
   *    preparation work for the write job to be issued.
   * 2. Issues a write job consists of one or more executor side tasks, each of which writes all
   *    rows within an RDD partition.
   * 3. If no exception is thrown in a task, commits that task, otherwise aborts that task;  If any
   *    exception is thrown during task commitment, also aborts that task.
   * 4. If all tasks are committed, commit the job, otherwise aborts the job;  If any exception is
   *    thrown during job commitment, also aborts the job.
   */
  def write(
      sparkSession: SparkSession,
      queryExecution: QueryExecution,
      fileFormat: FileFormat,
      committer: FileCommitProtocol,
      outputSpec: OutputSpec,
      hadoopConf: Configuration,
      partitionColumns: Seq[Attribute],
      bucketSpec: Option[BucketSpec],
      refreshFunction: (Seq[TablePartitionSpec]) => Unit,
      options: Map[String, String]): Unit = {

    val job = Job.getInstance(hadoopConf)
    job.setOutputKeyClass(classOf[Void])
    job.setOutputValueClass(classOf[InternalRow])
    FileOutputFormat.setOutputPath(job, new Path(outputSpec.outputPath))

    val allColumns = queryExecution.logical.output
    val partitionSet = AttributeSet(partitionColumns)
    val dataColumns = queryExecution.logical.output.filterNot(partitionSet.contains)
    val bucketColumns = bucketSpec.toSeq.flatMap {
      spec => spec.bucketColumnNames.map(c => allColumns.find(_.name == c).get)
    }
    val sortColumns = bucketSpec.toSeq.flatMap {
      spec => spec.sortColumnNames.map(c => allColumns.find(_.name == c).get)
    }

    // Note: prepareWrite has side effect. It sets "job".
    val outputWriterFactory =
      fileFormat.prepareWrite(sparkSession, job, options, dataColumns.toStructType)

    val description = new WriteJobDescription(
      uuid = UUID.randomUUID().toString,
      serializableHadoopConf = new SerializableConfiguration(job.getConfiguration),
      outputWriterFactory = outputWriterFactory,
      allColumns = queryExecution.logical.output,
      dataColumns = dataColumns,
      partitionColumns = partitionColumns,
      bucketColumns = bucketColumns,
      numBuckets = bucketSpec.map(_.numBuckets).getOrElse(0),
      path = outputSpec.outputPath,
      customPartitionLocations = outputSpec.customPartitionLocations,
      maxRecordsPerFile = options.get("maxRecordsPerFile").map(_.toLong)
        .getOrElse(sparkSession.sessionState.conf.maxRecordsPerFile)
    )

    SQLExecution.withNewExecutionId(sparkSession, queryExecution) {
      // This call shouldn't be put into the `try` block below because it only initializes and
      // prepares the job, any exception thrown from here shouldn't cause abortJob() to be called.
      committer.setupJob(job)

      val bucketIdExpression = bucketSpec.map { spec =>
        // Use `HashPartitioning.partitionIdExpression` as our bucket id expression, so that we can
        // guarantee the data distribution is same between shuffle and bucketed data source, which
        // enables us to only shuffle one side when join a bucketed table and a normal one.
        HashPartitioning(bucketColumns, spec.numBuckets).partitionIdExpression
      }
      // We should first sort by partition columns, then bucket id, and finally sorting columns.
      val requiredOrdering = (partitionColumns ++ bucketIdExpression ++ sortColumns)
        .map(SortOrder(_, Ascending))
      val actualOrdering = queryExecution.executedPlan.outputOrdering
      // We can still avoid the sort if the required ordering is [partCol] and the actual ordering
      // is [partCol, anotherCol].
      val rdd = if (requiredOrdering == actualOrdering.take(requiredOrdering.length)) {
        queryExecution.toRdd
      } else {
        SortExec(requiredOrdering, global = false, queryExecution.executedPlan).execute()
      }

      try {
        val ret = sparkSession.sparkContext.runJob(rdd,
          (taskContext: TaskContext, iter: Iterator[InternalRow]) => {
            executeTask(
              description = description,
              sparkStageId = taskContext.stageId(),
              sparkPartitionId = taskContext.partitionId(),
              sparkAttemptNumber = taskContext.attemptNumber(),
              committer,
              iterator = iter)
          })

        val commitMsgs = ret.map(_._1)
        val updatedPartitions = ret.flatMap(_._2).distinct.map(PartitioningUtils.parsePathFragment)

        committer.commitJob(job, commitMsgs)
        logInfo(s"Job ${job.getJobID} committed.")
        refreshFunction(updatedPartitions)
      } catch { case cause: Throwable =>
        logError(s"Aborting job ${job.getJobID}.", cause)
        committer.abortJob(job)
        throw new SparkException("Job aborted.", cause)
      }
    }
  }

  /** Writes data out in a single Spark task. */
  private def executeTask(
      description: WriteJobDescription,
      sparkStageId: Int,
      sparkPartitionId: Int,
      sparkAttemptNumber: Int,
      committer: FileCommitProtocol,
      iterator: Iterator[InternalRow]): (TaskCommitMessage, Set[String]) = {

    val jobId = SparkHadoopWriterUtils.createJobID(new Date, sparkStageId)
    val taskId = new TaskID(jobId, TaskType.MAP, sparkPartitionId)
    val taskAttemptId = new TaskAttemptID(taskId, sparkAttemptNumber)

    // Set up the attempt context required to use in the output committer.
    val taskAttemptContext: TaskAttemptContext = {
      // Set up the configuration object
      val hadoopConf = description.serializableHadoopConf.value
      hadoopConf.set("mapred.job.id", jobId.toString)
      hadoopConf.set("mapred.tip.id", taskAttemptId.getTaskID.toString)
      hadoopConf.set("mapred.task.id", taskAttemptId.toString)
      hadoopConf.setBoolean("mapred.task.is.map", true)
      hadoopConf.setInt("mapred.task.partition", 0)

      new TaskAttemptContextImpl(hadoopConf, taskAttemptId)
    }

    committer.setupTask(taskAttemptContext)

    val writeTask =
      if (description.partitionColumns.isEmpty && description.numBuckets == 0) {
        new SingleDirectoryWriteTask(description, taskAttemptContext, committer)
      } else {
        new DynamicPartitionWriteTask(description, taskAttemptContext, committer)
      }

    try {
      Utils.tryWithSafeFinallyAndFailureCallbacks(block = {
        // Execute the task to write rows out and commit the task.
        val outputPartitions = writeTask.execute(iterator)
        writeTask.releaseResources()
        (committer.commitTask(taskAttemptContext), outputPartitions)
      })(catchBlock = {
        // If there is an error, release resource and then abort the task
        try {
          writeTask.releaseResources()
        } finally {
          committer.abortTask(taskAttemptContext)
          logError(s"Job $jobId aborted.")
        }
      })
    } catch {
      case t: Throwable =>
        throw new SparkException("Task failed while writing rows", t)
    }
  }

  /**
   * A simple trait for writing out data in a single Spark task, without any concerns about how
   * to commit or abort tasks. Exceptions thrown by the implementation of this trait will
   * automatically trigger task aborts.
   */
  private trait ExecuteWriteTask {
    /**
     * Writes data out to files, and then returns the list of partition strings written out.
     * The list of partitions is sent back to the driver and used to update the catalog.
     */
    def execute(iterator: Iterator[InternalRow]): Set[String]
    def releaseResources(): Unit
  }

  /** Writes data to a single directory (used for non-dynamic-partition writes). */
  private class SingleDirectoryWriteTask(
      description: WriteJobDescription,
      taskAttemptContext: TaskAttemptContext,
      committer: FileCommitProtocol) extends ExecuteWriteTask {

    private[this] var currentWriter: OutputWriter = _

    private def newOutputWriter(fileCounter: Int): Unit = {
      val ext = description.outputWriterFactory.getFileExtension(taskAttemptContext)
      val tmpFilePath = committer.newTaskTempFile(
        taskAttemptContext,
        None,
        f"-c$fileCounter%03d" + ext)

      currentWriter = description.outputWriterFactory.newInstance(
        path = tmpFilePath,
        dataSchema = description.dataColumns.toStructType,
        context = taskAttemptContext)
    }

    override def execute(iter: Iterator[InternalRow]): Set[String] = {
      var fileCounter = 0
      var recordsInFile: Long = 0L
      newOutputWriter(fileCounter)
      while (iter.hasNext) {
        if (description.maxRecordsPerFile > 0 && recordsInFile >= description.maxRecordsPerFile) {
          fileCounter += 1
          assert(fileCounter < MAX_FILE_COUNTER,
            s"File counter $fileCounter is beyond max value $MAX_FILE_COUNTER")

          recordsInFile = 0
          releaseResources()
          newOutputWriter(fileCounter)
        }

        val internalRow = iter.next()
        currentWriter.write(internalRow)
        recordsInFile += 1
      }
      releaseResources()
      Set.empty
    }

    override def releaseResources(): Unit = {
      if (currentWriter != null) {
        currentWriter.close()
        currentWriter = null
      }
    }
  }

  /**
   * Writes data to using dynamic partition writes, meaning this single function can write to
   * multiple directories (partitions) or files (bucketing).
   */
  private class DynamicPartitionWriteTask(
      desc: WriteJobDescription,
      taskAttemptContext: TaskAttemptContext,
      committer: FileCommitProtocol) extends ExecuteWriteTask {

    // currentWriter is initialized whenever we see a new key
    private var currentWriter: OutputWriter = _

    private def bucketIdExpression: Option[Expression] = if (desc.numBuckets > 0) {
      // Use `HashPartitioning.partitionIdExpression` as our bucket id expression, so that we can
      // guarantee the data distribution is same between shuffle and bucketed data source, which
      // enables us to only shuffle one side when join a bucketed table and a normal one.
      Some(HashPartitioning(desc.bucketColumns, desc.numBuckets).partitionIdExpression)
    } else {
      None
    }

    /** Expressions that given partition columns build a path string like: col1=val/col2=val/... */
    private def partitionPathExpression: Seq[Expression] = {
      desc.partitionColumns.zipWithIndex.flatMap { case (c, i) =>
        // TODO: use correct timezone for partition values.
        val escaped = ScalaUDF(
          ExternalCatalogUtils.escapePathName _,
          StringType,
          Seq(Cast(c, StringType, Option(DateTimeUtils.defaultTimeZone().getID))),
          Seq(StringType))
        val str = If(IsNull(c), Literal(ExternalCatalogUtils.DEFAULT_PARTITION_NAME), escaped)
        val partitionName = Literal(c.name + "=") :: str :: Nil
        if (i == 0) partitionName else Literal(Path.SEPARATOR) :: partitionName
      }
    }

    /**
     * Open and returns a new OutputWriter given a partition key and optional bucket id.
     * If bucket id is specified, we will append it to the end of the file name, but before the
     * file extension, e.g. part-r-00009-ea518ad4-455a-4431-b471-d24e03814677-00002.gz.parquet
     *
     * @param partColsAndBucketId a row consisting of partition columns and a bucket id for the
     *                            current row.
     * @param getPartitionPath a function that projects the partition values into a path string.
     * @param fileCounter the number of files that have been written in the past for this specific
     *                    partition. This is used to limit the max number of records written for a
     *                    single file. The value should start from 0.
     * @param updatedPartitions the set of updated partition paths, we should add the new partition
     *                          path of this writer to it.
     */
    private def newOutputWriter(
        partColsAndBucketId: InternalRow,
        getPartitionPath: UnsafeProjection,
        fileCounter: Int,
        updatedPartitions: mutable.Set[String]): Unit = {
      val partDir = if (desc.partitionColumns.isEmpty) {
        None
      } else {
        Option(getPartitionPath(partColsAndBucketId).getString(0))
      }
      partDir.foreach(updatedPartitions.add)

      // If the bucket spec is defined, the bucket column is right after the partition columns
      val bucketId = if (desc.numBuckets > 0) {
        BucketingUtils.bucketIdToString(partColsAndBucketId.getInt(desc.partitionColumns.length))
      } else {
        ""
      }

      // This must be in a form that matches our bucketing format. See BucketingUtils.
      val ext = f"$bucketId.c$fileCounter%03d" +
        desc.outputWriterFactory.getFileExtension(taskAttemptContext)

      val customPath = partDir match {
        case Some(dir) =>
          desc.customPartitionLocations.get(PartitioningUtils.parsePathFragment(dir))
        case _ =>
          None
      }
      val path = if (customPath.isDefined) {
        committer.newTaskTempFileAbsPath(taskAttemptContext, customPath.get, ext)
      } else {
        committer.newTaskTempFile(taskAttemptContext, partDir, ext)
      }

      currentWriter = desc.outputWriterFactory.newInstance(
        path = path,
        dataSchema = desc.dataColumns.toStructType,
        context = taskAttemptContext)
    }

    override def execute(iter: Iterator[InternalRow]): Set[String] = {
      val getPartitionColsAndBucketId = UnsafeProjection.create(
        desc.partitionColumns ++ bucketIdExpression, desc.allColumns)

      // Generates the partition path given the row generated by `getPartitionColsAndBucketId`.
      val getPartPath = UnsafeProjection.create(
        Seq(Concat(partitionPathExpression)), desc.partitionColumns)

      // Returns the data columns to be written given an input row
      val getOutputRow = UnsafeProjection.create(desc.dataColumns, desc.allColumns)

      // If anything below fails, we should abort the task.
      var recordsInFile: Long = 0L
      var fileCounter = 0
      var currentPartColsAndBucketId: UnsafeRow = null
      val updatedPartitions = mutable.Set[String]()
      for (row <- iter) {
        val nextPartColsAndBucketId = getPartitionColsAndBucketId(row)
        if (currentPartColsAndBucketId != nextPartColsAndBucketId) {
          // See a new partition or bucket - write to a new partition dir (or a new bucket file).
          currentPartColsAndBucketId = nextPartColsAndBucketId.copy()
          logDebug(s"Writing partition: $currentPartColsAndBucketId")

          recordsInFile = 0
          fileCounter = 0

          releaseResources()
          newOutputWriter(currentPartColsAndBucketId, getPartPath, fileCounter, updatedPartitions)
        } else if (desc.maxRecordsPerFile > 0 &&
            recordsInFile >= desc.maxRecordsPerFile) {
          // Exceeded the threshold in terms of the number of records per file.
          // Create a new file by increasing the file counter.
          recordsInFile = 0
          fileCounter += 1
          assert(fileCounter < MAX_FILE_COUNTER,
            s"File counter $fileCounter is beyond max value $MAX_FILE_COUNTER")

          releaseResources()
          newOutputWriter(currentPartColsAndBucketId, getPartPath, fileCounter, updatedPartitions)
        }

        currentWriter.write(getOutputRow(row))
        recordsInFile += 1
      }
      releaseResources()
      updatedPartitions.toSet
    }

    override def releaseResources(): Unit = {
      if (currentWriter != null) {
        currentWriter.close()
        currentWriter = null
      }
    }
  }
}
