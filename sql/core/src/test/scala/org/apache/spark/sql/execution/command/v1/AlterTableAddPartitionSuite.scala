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

package org.apache.spark.sql.execution.command.v1

import java.io.File

import org.apache.commons.io.FileUtils

import org.apache.spark.sql.{AnalysisException, Row}
import org.apache.spark.sql.execution.command

/**
 * This base suite contains unified tests for the `ALTER TABLE .. ADD PARTITION` command that
 * check V1 table catalogs. The tests that cannot run for all V1 catalogs are located in more
 * specific test suites:
 *
 *   - V1 In-Memory catalog:
 *     `org.apache.spark.sql.execution.command.v1.AlterTableAddPartitionSuite`
 *   - V1 Hive External catalog:
 *     `org.apache.spark.sql.hive.execution.command.AlterTableAddPartitionSuite`
 */
trait AlterTableAddPartitionSuiteBase extends command.AlterTableAddPartitionSuiteBase {
  test("empty string as partition value") {
    withNamespaceAndTable("ns", "tbl") { t =>
      sql(s"CREATE TABLE $t (col1 INT, p1 STRING) $defaultUsing PARTITIONED BY (p1)")
      val errMsg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $t ADD PARTITION (p1 = '')")
      }.getMessage
      assert(errMsg.contains("Partition spec is invalid. " +
        "The spec ([p1=]) contains an empty partition column value"))
    }
  }

  test("SPARK-34055: refresh cache in partition adding") {
    withTable("t") {
      sql(s"CREATE TABLE t (id int, part int) $defaultUsing PARTITIONED BY (part)")
      sql("INSERT INTO t PARTITION (part=0) SELECT 0")
      assert(!spark.catalog.isCached("t"))
      sql("CACHE TABLE t")
      assert(spark.catalog.isCached("t"))
      checkAnswer(sql("SELECT * FROM t"), Seq(Row(0, 0)))

      // Create new partition (part = 1) in the filesystem
      val information = sql("SHOW TABLE EXTENDED LIKE 't' PARTITION (part = 0)")
        .select("information")
        .first().getString(0)
      val part0Loc = information
        .split("\\r?\\n")
        .filter(_.startsWith("Location:"))
        .head
        .replace("Location: file:", "")
      val part1Loc = part0Loc.replace("part=0", "part=1")
      FileUtils.copyDirectory(new File(part0Loc), new File(part1Loc))

      sql(s"ALTER TABLE t ADD PARTITION (part=1) LOCATION '$part1Loc'")
      assert(spark.catalog.isCached("t"))
      checkAnswer(sql("SELECT * FROM t"), Seq(Row(0, 0), Row(0, 1)))
    }
  }
}

/**
 * The class contains tests for the `ALTER TABLE .. ADD PARTITION` command to check
 * V1 In-Memory table catalog.
 */
class AlterTableAddPartitionSuite extends AlterTableAddPartitionSuiteBase with CommandSuiteBase
