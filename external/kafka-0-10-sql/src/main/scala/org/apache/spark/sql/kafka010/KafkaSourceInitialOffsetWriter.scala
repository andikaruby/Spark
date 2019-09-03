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

package org.apache.spark.sql.kafka010

import java.io._
import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.streaming.{HDFSMetadataLog, SerializedOffset}

/** A version of [[HDFSMetadataLog]] specialized for saving the initial offsets. */
private[kafka010] class KafkaSourceInitialOffsetWriter(
    sparkSession: SparkSession,
    metadataPath: String)
  extends HDFSMetadataLog[KafkaSourceOffset](sparkSession, metadataPath) {

  val VERSION = 1

  override def serialize(metadata: KafkaSourceOffset, out: OutputStream): Unit = {
    out.write(0) // A zero byte is written to support Spark 2.1.0 (SPARK-19517)
    val writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))
    writer.write(s"v$VERSION\n")
    writer.write(metadata.json)
    writer.flush
  }

  override def deserialize(in: InputStream): KafkaSourceOffset = {
    in.read() // A zero byte is read to support Spark 2.1.0 (SPARK-19517)
    val content = IOUtils.toString(new InputStreamReader(in, StandardCharsets.UTF_8))
    // HDFSMetadataLog guarantees that it never creates a partial file.
    require(content.nonEmpty)
    if (content(0) == 'v') {
      val indexOfNewLine = content.indexOf("\n")
      if (indexOfNewLine > 0) {
        validateVersion(content.substring(0, indexOfNewLine), VERSION)
        KafkaSourceOffset(SerializedOffset(content.substring(indexOfNewLine + 1)))
      } else {
        throw new IllegalStateException(
          "Log file was malformed: failed to detect the log file version line.")
      }
    } else {
      // The log was generated by Spark 2.1.0
      KafkaSourceOffset(SerializedOffset(content))
    }
  }
}
