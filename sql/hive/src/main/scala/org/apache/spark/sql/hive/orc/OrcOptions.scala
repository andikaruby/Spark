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

package org.apache.spark.sql.hive.orc

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hive.ql.io.orc.CompressionKind
import org.apache.hadoop.hive.ql.io.orc.OrcFile.OrcTableProperties

/**
 * Options for the ORC data source.
 */
class OrcOptions(
    @transient private val parameters: Map[String, String],
    @transient private val conf: Configuration)
  extends Serializable {

  import OrcOptions._
  import org.apache.spark.sql.execution.datasources.ParameterUtils._

  /**
   * Compression codec to use. By default use the value specified in Hadoop configuration.
   * If it is not specified, then use [[CompressionKind.ZLIB]] because it is the default value.
   * Acceptable values are defined in [[shortOrcCompressionCodecNames]].
   */

  val compressionCodec: String = {
    val defaultName =
      conf.get(OrcTableProperties.COMPRESSION.getPropName, CompressionKind.ZLIB.name())
    val codecName = getNullSafeString(parameters, "compression", defaultName).toLowerCase
    if (!shortOrcCompressionCodecNames.contains(codecName)) {
      val availableCodecs = shortOrcCompressionCodecNames.keys.map(_.toLowerCase)
      throw new IllegalArgumentException(s"Codec [$codecName] " +
        s"is not available. Available codecs are ${availableCodecs.mkString(", ")}.")
    }
    shortOrcCompressionCodecNames(codecName).name()
  }
}

object OrcOptions {
  // The ORC compression short names
  private val shortOrcCompressionCodecNames = Map(
    "none" -> CompressionKind.NONE,
    "uncompressed" -> CompressionKind.NONE,
    "snappy" -> CompressionKind.SNAPPY,
    "zlib" -> CompressionKind.ZLIB,
    "lzo" -> CompressionKind.LZO)
}
