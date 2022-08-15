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

import java.io.{InputStreamReader, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import scala.util.control.NonFatal

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileAlreadyExistsException, FSDataInputStream, Path}

import org.apache.spark.internal.Logging
import org.apache.spark.sql.errors.QueryExecutionErrors
import org.apache.spark.sql.execution.streaming.CheckpointFileManager.CancellableFSDataOutputStream
import org.apache.spark.util.JacksonUtils
import org.apache.spark.util.Utils

/**
 * Contains metadata associated with a [[org.apache.spark.sql.streaming.StreamingQuery]].
 * This information is written in the checkpoint location the first time a query is started
 * and recovered every time the query is restarted.
 *
 * @param id  unique id of the [[org.apache.spark.sql.streaming.StreamingQuery]]
 *            that needs to be persisted across restarts
 */
case class StreamMetadata(id: String) {
  def json: String = JacksonUtils.writeValueAsString(json)
}

object StreamMetadata extends Logging {

  /** Read the metadata from file if it exists */
  def read(metadataFile: Path, hadoopConf: Configuration): Option[StreamMetadata] = {
    val fileManager = CheckpointFileManager.create(metadataFile.getParent, hadoopConf)

    if (fileManager.exists(metadataFile)) {
      var input: FSDataInputStream = null
      try {
        input = fileManager.open(metadataFile)
        val metadata =
          Utils.tryWithResource(new InputStreamReader(input, StandardCharsets.UTF_8)) { reader =>
            JacksonUtils.readValue(reader, classOf[StreamMetadata])
          }
        Some(metadata)
      } catch {
        case NonFatal(e) =>
          logError(s"Error reading stream metadata from $metadataFile", e)
          throw e
      } finally {
        IOUtils.closeQuietly(input)
      }
    } else None
  }

  /** Write metadata to file */
  def write(
      metadata: StreamMetadata,
      metadataFile: Path,
      hadoopConf: Configuration): Unit = {
    var output: CancellableFSDataOutputStream = null
    try {
      val fileManager = CheckpointFileManager.create(metadataFile.getParent, hadoopConf)
      output = fileManager.createAtomic(metadataFile, overwriteIfPossible = false)
      val writer = new OutputStreamWriter(output)
      JacksonUtils.writeValue(writer, metadata)
      writer.close()
    } catch {
      case e: FileAlreadyExistsException =>
        if (output != null) {
          output.cancel()
        }
        throw QueryExecutionErrors.multiStreamingQueriesUsingPathConcurrentlyError(
          metadataFile.getName, e)
      case e: Throwable =>
        if (output != null) {
          output.cancel()
        }
        logError(s"Error writing stream metadata $metadata to $metadataFile", e)
        throw e
    }
  }
}
