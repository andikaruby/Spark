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

import java.util.Locale

import scala.util.Try

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
import org.apache.spark.util.Utils

/**
 * User specified options for file streams.
 */
class FileStreamOptions(parameters: CaseInsensitiveMap[String]) extends Logging {

  def this(parameters: Map[String, String]) = this(CaseInsensitiveMap(parameters))

  val maxFilesPerTrigger: Option[Int] = parameters.get("maxFilesPerTrigger").map { str =>
    Try(str.toInt).toOption.filter(_ > 0).getOrElse {
      throw new IllegalArgumentException(
        s"Invalid value '$str' for option 'maxFilesPerTrigger', must be a positive integer")
    }
  }

  /**
   * Maximum age of a file that can be found in this directory, before it is ignored. For the
   * first batch all files will be considered valid. If `latestFirst` is set to `true` and
   * `maxFilesPerTrigger` is set, then this parameter will be ignored, because old files that are
   * valid, and should be processed, may be ignored. Please refer to SPARK-19813 for details.
   *
   * The max age is specified with respect to the timestamp of the latest file, and not the
   * timestamp of the current system. That this means if the last file has timestamp 1000, and the
   * current system time is 2000, and max age is 200, the system will purge files older than
   * 800 (rather than 1800) from the internal state.
   *
   * Default to a week.
   */
  val maxFileAgeMs: Long =
    Utils.timeStringAsMs(parameters.getOrElse("maxFileAge", "7d"))

  /** Options as specified by the user, in a case-insensitive map, without "path" set. */
  val optionMapWithoutPath: Map[String, String] = parameters - "path"

  /**
   * Whether to scan latest files first. If it's true, when the source finds unprocessed files in a
   * trigger, it will first process the latest files.
   */
  val latestFirst: Boolean = withBooleanParameter("latestFirst", false)

  /**
   * Whether to check new files based on only the filename instead of on the full path.
   *
   * With this set to `true`, the following files would be considered as the same file, because
   * their filenames, "dataset.txt", are the same:
   * - "file:///dataset.txt"
   * - "s3://a/dataset.txt"
   * - "s3n://a/b/dataset.txt"
   * - "s3a://a/b/c/dataset.txt"
   */
  val fileNameOnly: Boolean = withBooleanParameter("fileNameOnly", false)

  /**
   * Maximum age of a file that can be found in this directory, before it is ignored.
   *
   * This is the "hard" limit of input data retention - input files older than the max age will be
   * ignored regardless of source options (while `maxFileAgeMs` depends on the condition), as well
   * as entries in checkpoint metadata will be purged based on this.
   *
   * Unlike `maxFileAgeMs`, the max age is specified with respect to the timestamp of the current
   * system, to provide consistent behavior regardless of metadata entries.
   *
   * NOTE 1: Please be careful to set the value if the query replays from the old input files.
   * NOTE 2: Please make sure the timestamp is in sync between nodes which run the query.
   */
  val inputRetentionMs = parameters.get("inputRetention").map(Utils.timeStringAsMs)

  /**
   * The archive directory to move completed files. The option will be only effective when
   * "cleanSource" is set to "archive".
   *
   * Note that the completed file will be moved to this archive directory with respecting to
   * its own path.
   *
   * For example, if the path of source file is "/a/b/dataset.txt", and the path of archive
   * directory is "/archived/here", file will be moved to "/archived/here/a/b/dataset.txt".
   */
  val sourceArchiveDir: Option[String] = parameters.get("sourceArchiveDir")

  /**
   * Defines how to clean up completed files. Available options are "archive", "delete", "off".
   */
  val cleanSource: CleanSourceMode.Value = {
    val matchedMode = CleanSourceMode.fromString(parameters.get("cleanSource"))
    if (matchedMode == CleanSourceMode.ARCHIVE && sourceArchiveDir.isEmpty) {
      throw new IllegalArgumentException("Archive mode must be used with 'sourceArchiveDir' " +
        "option.")
    }
    matchedMode
  }

  private def withBooleanParameter(name: String, default: Boolean) = {
    parameters.get(name).map { str =>
      try {
        str.toBoolean
      } catch {
        case _: IllegalArgumentException =>
          throw new IllegalArgumentException(
            s"Invalid value '$str' for option '$name', must be 'true' or 'false'")
      }
    }.getOrElse(default)
  }
}

object CleanSourceMode extends Enumeration {
  val ARCHIVE, DELETE, OFF = Value

  def fromString(value: Option[String]): CleanSourceMode.Value = value.map { v =>
    CleanSourceMode.values.find(_.toString == v.toUpperCase(Locale.ROOT))
      .getOrElse(throw new IllegalArgumentException(
        s"Invalid mode for clean source option $value." +
        s" Must be one of ${CleanSourceMode.values.mkString(",")}"))
  }.getOrElse(OFF)
}
