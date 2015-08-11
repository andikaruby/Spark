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

import com.google.common.io.Files

import org.apache.spark.sql.hive.test.HiveTestUtils
import org.apache.spark.sql.test.SQLTestData.TestData
import org.apache.spark.sql.QueryTest
import org.apache.spark.util.Utils


class QueryPartitionSuite extends QueryTest with HiveTestUtils {
  import testImplicits._

  test("SPARK-5068: query data when path doesn't exist"){
    val testData = ctx.sparkContext.parallelize(
      (1 to 10).map(i => TestData(i, i.toString))).toDF()
    testData.registerTempTable("testData")

    val tmpDir = Files.createTempDir()
    // create the table for test
    ctx.sql(s"CREATE TABLE table_with_partition(key int,value string) " +
      s"PARTITIONED by (ds string) location '${tmpDir.toURI.toString}' ")
    ctx.sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='1') " +
      "SELECT key,value FROM testData")
    ctx.sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='2') " +
      "SELECT key,value FROM testData")
    ctx.sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='3') " +
      "SELECT key,value FROM testData")
    ctx.sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='4') " +
      "SELECT key,value FROM testData")

    // test for the exist path
    checkAnswer(ctx.sql("select key,value from table_with_partition"),
      testData.toDF.collect ++ testData.toDF.collect
        ++ testData.toDF.collect ++ testData.toDF.collect)

    // delete the path of one partition
    tmpDir.listFiles
      .find { f => f.isDirectory && f.getName().startsWith("ds=") }
      .foreach { f => Utils.deleteRecursively(f) }

    // test for after delete the path
    checkAnswer(ctx.sql("select key,value from table_with_partition"),
      testData.toDF.collect ++ testData.toDF.collect ++ testData.toDF.collect)

    ctx.sql("DROP TABLE table_with_partition")
    ctx.sql("DROP TABLE createAndInsertTest")
  }
}
