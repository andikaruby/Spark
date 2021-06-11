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
package org.apache.spark.sql.execution.datasources.v2

import org.apache.hadoop.mapreduce.Job

import org.apache.spark.internal.Logging
import org.apache.spark.internal.io.{FileCommitProtocol, FileNamingProtocol}
import org.apache.spark.sql.connector.write.{BatchWrite, DataWriterFactory, PhysicalWriteInfo, WriterCommitMessage}
import org.apache.spark.sql.execution.datasources.{WriteJobDescription, WriteTaskResult}
import org.apache.spark.sql.execution.datasources.FileFormatWriter.processStats
import org.apache.spark.util.Utils

class FileBatchWrite(
    job: Job,
    description: WriteJobDescription,
    committer: FileCommitProtocol,
    namingProtocol: FileNamingProtocol)
  extends BatchWrite with Logging {
  override def commit(messages: Array[WriterCommitMessage]): Unit = {
    val results = messages.map(_.asInstanceOf[WriteTaskResult])
    logInfo(s"Start to commit write Job ${description.uuid}.")
    val (_, duration) = Utils.timeTakenMs { committer.commitJob(job, results.map(_.commitMsg)) }
    logInfo(s"Write Job ${description.uuid} committed. Elapsed time: $duration ms.")

    processStats(description.statsTrackers, results.map(_.summary.stats))
    logInfo(s"Finished processing stats for write job ${description.uuid}.")
  }

  override def useCommitCoordinator(): Boolean = false

  override def abort(messages: Array[WriterCommitMessage]): Unit = {
    committer.abortJob(job)
  }

  override def createBatchWriterFactory(info: PhysicalWriteInfo): DataWriterFactory = {
    FileWriterFactory(description, committer, namingProtocol)
  }
}

