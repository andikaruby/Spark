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

package org.apache.spark.sql.execution.streaming.state

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

import scala.io.{Source => IOSource}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.streaming.HDFSMetadataLog

object StateSchemaV3File {
  val COLUMN_FAMILY_SCHEMA_VERSION = 1
}

/**
 * The StateSchemaV3File is used to write the schema of multiple column families.
 * Right now, this is primarily used for the TransformWithState operator, which supports
 * multiple column families to keep the data for multiple state variables.
 * This class is different than other HDFSMetadataLog files in that the files are written
 * to a directory with the batchId as the directory name, and the metadata is written to
 * a file with a random UUID as the name.
 * If a query fails after we have written the new State Schema file, and we restart with
 * a different schema, we want to make sure that we don't read the old schema file with
 * the same batch, so we create a new one.
 * @param hadoopConf Hadoop configuration that is used to read / write metadata files.
 * @param path Path to the directory that will be used for writing metadata.
 * @param metadataCacheEnabled Whether to cache the batches' metadata in memory.
 */
class StateSchemaV3File(
    hadoopConf: Configuration,
    path: String,
    metadataCacheEnabled: Boolean = false)
  extends HDFSMetadataLog[List[ColumnFamilySchema]](hadoopConf, path, metadataCacheEnabled) {

  val VERSION = 3

  def this(sparkSession: SparkSession, path: String) = {
    this(
      sparkSession.sessionState.newHadoopConf(),
      path,
      metadataCacheEnabled = sparkSession.sessionState.conf.getConf(
        SQLConf.STREAMING_METADATA_CACHE_ENABLED)
    )
  }

  override def deserialize(in: InputStream): List[ColumnFamilySchema] = {
    val lines = IOSource.fromInputStream(in, UTF_8.name()).getLines()

    if (!lines.hasNext) {
      throw new IllegalStateException("Incomplete log file in the offset commit log")
    }

    val version = lines.next().trim
    validateVersion(version, VERSION)

    val columnFamilySchemaVersion = lines.next().trim

    columnFamilySchemaVersion match {
      case "v1" => lines.map(ColumnFamilySchemaV1.fromJson).toList
      case _ =>
        throw new IllegalStateException(
          s"Unsupported column family schema version: $columnFamilySchemaVersion")
    }
  }

  override def serialize(schemas: List[ColumnFamilySchema], out: OutputStream): Unit = {
    out.write(s"v${VERSION}".getBytes(UTF_8))
    out.write('\n')
    out.write(s"v${StateSchemaV3File.COLUMN_FAMILY_SCHEMA_VERSION}".getBytes(UTF_8))
    out.write('\n')
    out.write(schemas.map(_.json).mkString("\n").getBytes(UTF_8))
  }

  def addWithUUID(batchId: Long, metadata: List[ColumnFamilySchema]): Path = {
    val batchMetadataPath = batchIdToPath(batchId)
    val schemaFilePath = new Path(batchMetadataPath, UUID.randomUUID().toString)
    fileManager.mkdirs(batchMetadataPath)
    write(schemaFilePath, out => serialize(metadata, out))
    schemaFilePath
  }

  def getWithPath(schemaFilePath: Path): List[ColumnFamilySchema] = {
    deserialize(fileManager.open(schemaFilePath))
  }

  override def add(batchId: Long, metadata: List[ColumnFamilySchema]): Boolean = {
    throw new UnsupportedOperationException("StateSchemaFile does not support add operation." +
      "Please use addWithUUID instead.")
  }

  override def get(batchId: Long): Option[List[ColumnFamilySchema]] = {
    throw new UnsupportedOperationException("StateSchemaFile does not support get operation." +
      "Please use getWithPath instead.")
  }

  override def getLatest(): Option[(Long, List[ColumnFamilySchema])] = {
    throw new UnsupportedOperationException(
      "StateSchemaFile does not support getLatest operation." +
        "Please use getWithpath instead.")
  }
}
