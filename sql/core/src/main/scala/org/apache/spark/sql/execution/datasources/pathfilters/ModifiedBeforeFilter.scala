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

package org.apache.spark.sql.execution.datasources.pathfilters

import java.time.ZoneId

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.util.{CaseInsensitiveMap, DateTimeUtils}

/**
 * [SPARK-31962]
 * Filter used to determine whether file was modified
 * before the provided timestamp.
 *
 * @param sparkSession SparkSession
 * @param hadoopConf Hadoop Configuration object
 * @param options Map containing options
 */
class ModifiedBeforeFilter(sparkSession: SparkSession,
                           hadoopConf: Configuration,
                           options: CaseInsensitiveMap[String])
    extends ModifiedDateFilter(sparkSession, hadoopConf, options)
    with FileIndexFilter {
  override def accept(fileStatus: FileStatus): Boolean =
    /* We standardize on microseconds wherever possible */
    thresholdTime - localTime(DateTimeUtils
        /* getModificationTime returns in milliseconds */
        .millisToMicros(fileStatus.getModificationTime)) > 0

  override def accept(path: Path): Boolean = true
  override def strategy(): String = "modifiedbefore"
}
case object ModifiedBeforeFilter extends PathFilterObject {
  def get(sparkSession: SparkSession,
          configuration: Configuration,
          options: CaseInsensitiveMap[String]): ModifiedBeforeFilter = {
    new ModifiedBeforeFilter(sparkSession, configuration, options)
  }
  def strategy(): String = "modifiedbefore"
}
