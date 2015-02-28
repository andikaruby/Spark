
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

package org.apache.spark.sql.parquet

import java.io.File

import scala.collection.mutable.ArrayBuffer

import org.scalatest.BeforeAndAfterAll

import org.apache.spark.sql.{SQLConf, QueryTest}
import org.apache.spark.sql.catalyst.expressions.Row
import org.apache.spark.sql.execution.{ExecutedCommand, PhysicalRDD}
import org.apache.spark.sql.hive.execution.{InsertIntoHiveTable, HiveTableScan}
import org.apache.spark.sql.hive.test.TestHive._
import org.apache.spark.sql.hive.test.TestHive.implicits._
import org.apache.spark.sql.sources.{InsertIntoDataSource, LogicalRelation}
import org.apache.spark.sql.SaveMode

// The data where the partitioning key exists only in the directory structure.
case class ParquetData(intField: Int, stringField: String)
// The data that also includes the partitioning key
case class ParquetDataWithKey(p: Int, intField: Int, stringField: String)


/**
 * A suite to test the automatic conversion of metastore tables with parquet data to use the
 * built in parquet support.
 */
class ParquetMetastoreSuiteBase extends ParquetPartitioningTest {
  override def beforeAll(): Unit = {
    super.beforeAll()

    sql(s"""
      create external table partitioned_parquet
      (
        intField INT,
        stringField STRING
      )
      PARTITIONED BY (p int)
      ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
       STORED AS
       INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
       OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      location '${partitionedTableDir.getCanonicalPath}'
    """)

    sql(s"""
      create external table partitioned_parquet_with_key
      (
        intField INT,
        stringField STRING
      )
      PARTITIONED BY (p int)
      ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
       STORED AS
       INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
       OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      location '${partitionedTableDirWithKey.getCanonicalPath}'
    """)

    sql(s"""
      create external table normal_parquet
      (
        intField INT,
        stringField STRING
      )
      ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
       STORED AS
       INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
       OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      location '${new File(normalTableDir, "normal").getCanonicalPath}'
    """)

    (1 to 10).foreach { p =>
      sql(s"ALTER TABLE partitioned_parquet ADD PARTITION (p=$p)")
    }

    (1 to 10).foreach { p =>
      sql(s"ALTER TABLE partitioned_parquet_with_key ADD PARTITION (p=$p)")
    }

    val rdd1 = sparkContext.parallelize((1 to 10).map(i => s"""{"a":$i, "b":"str${i}"}"""))
    jsonRDD(rdd1).registerTempTable("jt")
    val rdd2 = sparkContext.parallelize((1 to 10).map(i => s"""{"a":[$i, null]}"""))
    jsonRDD(rdd2).registerTempTable("jt_array")

    setConf("spark.sql.hive.convertMetastoreParquet", "true")
  }

  override def afterAll(): Unit = {
    sql("DROP TABLE partitioned_parquet")
    sql("DROP TABLE partitioned_parquet_with_key")
    sql("DROP TABLE normal_parquet")
    sql("DROP TABLE IF EXISTS jt")
    sql("DROP TABLE IF EXISTS jt_array")
    setConf("spark.sql.hive.convertMetastoreParquet", "false")
  }

  test(s"conversion is working") {
    assert(
      sql("SELECT * FROM normal_parquet").queryExecution.executedPlan.collect {
        case _: HiveTableScan => true
      }.isEmpty)
    assert(
      sql("SELECT * FROM normal_parquet").queryExecution.executedPlan.collect {
        case _: ParquetTableScan => true
        case _: PhysicalRDD => true
      }.nonEmpty)
  }
}

class ParquetDataSourceOnMetastoreSuite extends ParquetMetastoreSuiteBase {
  val originalConf = conf.parquetUseDataSourceApi

  override def beforeAll(): Unit = {
    super.beforeAll()

    sql(
      """
        |create table test_parquet
        |(
        |  intField INT,
        |  stringField STRING
        |)
        |ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      """.stripMargin)

    conf.setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, "true")
  }

  override def afterAll(): Unit = {
    super.afterAll()
    sql("DROP TABLE IF EXISTS test_parquet")

    setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, originalConf.toString)
  }

  test("scan an empty parquet table") {
    checkAnswer(sql("SELECT count(*) FROM test_parquet"), Row(0))
  }

  test("scan an empty parquet table with upper case") {
    checkAnswer(sql("SELECT count(INTFIELD) FROM TEST_parquet"), Row(0))
  }

  test("insert into an empty parquet table") {
    sql(
      """
        |create table test_insert_parquet
        |(
        |  intField INT,
        |  stringField STRING
        |)
        |ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      """.stripMargin)

    // Insert into am empty table.
    sql("insert into table test_insert_parquet select a, b from jt where jt.a > 5")
    checkAnswer(
      sql(s"SELECT intField, stringField FROM test_insert_parquet WHERE intField < 8"),
      Row(6, "str6") :: Row(7, "str7") :: Nil
    )
    // Insert overwrite.
    sql("insert overwrite table test_insert_parquet select a, b from jt where jt.a < 5")
    checkAnswer(
      sql(s"SELECT intField, stringField FROM test_insert_parquet WHERE intField > 2"),
      Row(3, "str3") :: Row(4, "str4") :: Nil
    )
    sql("DROP TABLE IF EXISTS test_insert_parquet")

    // Create it again.
    sql(
      """
        |create table test_insert_parquet
        |(
        |  intField INT,
        |  stringField STRING
        |)
        |ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      """.stripMargin)
    // Insert overwrite an empty table.
    sql("insert overwrite table test_insert_parquet select a, b from jt where jt.a < 5")
    checkAnswer(
      sql(s"SELECT intField, stringField FROM test_insert_parquet WHERE intField > 2"),
      Row(3, "str3") :: Row(4, "str4") :: Nil
    )
    // Insert into the table.
    sql("insert into table test_insert_parquet select a, b from jt")
    checkAnswer(
      sql(s"SELECT intField, stringField FROM test_insert_parquet"),
      (1 to 10).map(i => Row(i, s"str$i")) ++ (1 to 4).map(i => Row(i, s"str$i"))
    )
    sql("DROP TABLE IF EXISTS test_insert_parquet")
  }

  test("scan a parquet table created through a CTAS statement") {
    sql(
      """
        |create table test_parquet_ctas ROW FORMAT
        |SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
        |AS select * from jt
      """.stripMargin)

    checkAnswer(
      sql(s"SELECT a, b FROM test_parquet_ctas WHERE a = 1"),
      Seq(Row(1, "str1"))
    )

    table("test_parquet_ctas").queryExecution.analyzed match {
      case LogicalRelation(p: ParquetRelation2) => // OK
      case _ =>
        fail(
          s"test_parquet_ctas should be converted to ${classOf[ParquetRelation2].getCanonicalName}")
    }

    sql("DROP TABLE IF EXISTS test_parquet_ctas")
  }

  test("MetastoreRelation in InsertIntoTable will be converted") {
    sql(
      """
        |create table test_insert_parquet
        |(
        |  intField INT
        |)
        |ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      """.stripMargin)

    val df = sql("INSERT INTO TABLE test_insert_parquet SELECT a FROM jt")
    df.queryExecution.executedPlan match {
      case ExecutedCommand(
        InsertIntoDataSource(
          LogicalRelation(r: ParquetRelation2), query, overwrite)) => // OK
      case o => fail("test_insert_parquet should be converted to a " +
        s"${classOf[ParquetRelation2].getCanonicalName} and " +
        s"${classOf[InsertIntoDataSource].getCanonicalName} is expcted as the SparkPlan." +
        s"However, found a ${o.toString} ")
    }

    checkAnswer(
      sql("SELECT intField FROM test_insert_parquet WHERE test_insert_parquet.intField > 5"),
      sql("SELECT a FROM jt WHERE jt.a > 5").collect()
    )

    sql("DROP TABLE IF EXISTS test_insert_parquet")
  }

  test("MetastoreRelation in InsertIntoHiveTable will be converted") {
    sql(
      """
        |create table test_insert_parquet
        |(
        |  int_array array<int>
        |)
        |ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      """.stripMargin)

    val df = sql("INSERT INTO TABLE test_insert_parquet SELECT a FROM jt_array")
    df.queryExecution.executedPlan match {
      case ExecutedCommand(
        InsertIntoDataSource(
          LogicalRelation(r: ParquetRelation2), query, overwrite)) => // OK
      case o => fail("test_insert_parquet should be converted to a " +
        s"${classOf[ParquetRelation2].getCanonicalName} and " +
        s"${classOf[InsertIntoDataSource].getCanonicalName} is expcted as the SparkPlan." +
        s"However, found a ${o.toString} ")
    }

    checkAnswer(
      sql("SELECT int_array FROM test_insert_parquet"),
      sql("SELECT a FROM jt_array").collect()
    )

    sql("DROP TABLE IF EXISTS test_insert_parquet")
  }
}

class ParquetDataSourceOffMetastoreSuite extends ParquetMetastoreSuiteBase {
  val originalConf = conf.parquetUseDataSourceApi

  override def beforeAll(): Unit = {
    super.beforeAll()
    conf.setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, "false")
  }

  override def afterAll(): Unit = {
    super.afterAll()
    setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, originalConf.toString)
  }

  test("MetastoreRelation in InsertIntoTable will not be converted") {
    sql(
      """
        |create table test_insert_parquet
        |(
        |  intField INT
        |)
        |ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      """.stripMargin)

    val df = sql("INSERT INTO TABLE test_insert_parquet SELECT a FROM jt")
    df.queryExecution.executedPlan match {
      case insert: InsertIntoHiveTable => // OK
      case o => fail(s"The SparkPlan should be ${classOf[InsertIntoHiveTable].getCanonicalName}. " +
        s"However, found ${o.toString}.")
    }

    checkAnswer(
      sql("SELECT intField FROM test_insert_parquet WHERE test_insert_parquet.intField > 5"),
      sql("SELECT a FROM jt WHERE jt.a > 5").collect()
    )

    sql("DROP TABLE IF EXISTS test_insert_parquet")
  }

  // TODO: enable it after the fix of SPARK-5950.
  ignore("MetastoreRelation in InsertIntoHiveTable will not be converted") {
    sql(
      """
        |create table test_insert_parquet
        |(
        |  int_array array<int>
        |)
        |ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
        |STORED AS
        |  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
        |  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
      """.stripMargin)

    val df = sql("INSERT INTO TABLE test_insert_parquet SELECT a FROM jt_array")
    df.queryExecution.executedPlan match {
      case insert: InsertIntoHiveTable => // OK
      case o => fail(s"The SparkPlan should be ${classOf[InsertIntoHiveTable].getCanonicalName}. " +
        s"However, found ${o.toString}.")
    }

    checkAnswer(
      sql("SELECT int_array FROM test_insert_parquet"),
      sql("SELECT a FROM jt_array").collect()
    )

    sql("DROP TABLE IF EXISTS test_insert_parquet")
  }
}

/**
 * A suite of tests for the Parquet support through the data sources API.
 */
class ParquetSourceSuiteBase extends ParquetPartitioningTest {
  override def beforeAll(): Unit = {
    super.beforeAll()

    sql( s"""
      create temporary table partitioned_parquet
      USING org.apache.spark.sql.parquet
      OPTIONS (
        path '${partitionedTableDir.getCanonicalPath}'
      )
    """)

    sql( s"""
      create temporary table partitioned_parquet_with_key
      USING org.apache.spark.sql.parquet
      OPTIONS (
        path '${partitionedTableDirWithKey.getCanonicalPath}'
      )
    """)

    sql( s"""
      create temporary table normal_parquet
      USING org.apache.spark.sql.parquet
      OPTIONS (
        path '${new File(partitionedTableDir, "p=1").getCanonicalPath}'
      )
    """)
  }

  test("SPARK-6016 make sure to use the latest footers") {
    sql("drop table if exists spark_6016_fix")

    // Create a DataFrame with two partitions. So, the created table will have two parquet files.
    val df1 = jsonRDD(sparkContext.parallelize((1 to 10).map(i => s"""{"a":$i}"""), 2))
    df1.saveAsTable("spark_6016_fix", "parquet", SaveMode.Overwrite)
    checkAnswer(
      sql("select * from spark_6016_fix"),
      (1 to 10).map(i => Row(i))
    )

    // Create a DataFrame with four partitions. So, the created table will have four parquet files.
    val df2 = jsonRDD(sparkContext.parallelize((1 to 10).map(i => s"""{"b":$i}"""), 4))
    df2.saveAsTable("spark_6016_fix", "parquet", SaveMode.Overwrite)
    // For the bug of SPARK-6016, we are caching two outdated footers for df1. Then,
    // since the new table has four parquet files, we are trying to read new footers from two files
    // and then merge metadata in footers of these four (two outdated ones and two latest one),
    // which will cause an error.
    checkAnswer(
      sql("select * from spark_6016_fix"),
      (1 to 10).map(i => Row(i))
    )

    sql("drop table spark_6016_fix")
  }
}

class ParquetDataSourceOnSourceSuite extends ParquetSourceSuiteBase {
  val originalConf = conf.parquetUseDataSourceApi

  override def beforeAll(): Unit = {
    super.beforeAll()
    conf.setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, "true")
  }

  override def afterAll(): Unit = {
    super.afterAll()
    setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, originalConf.toString)
  }

  test("insert array into parquet hive table using data source api") {
    val data1="""{ "timestamp": 1422435598, "data_array": [ { "field0": null, "field1": 1, "field2": 2} ] }"""
    val data2="""{ "timestamp": 1422435599, "data_array": [ { "field0": 0, "field1": null, "field2": 3} ] }"""

    val json = sparkContext.makeRDD(data1 :: data2 :: Nil)
    val rdd = jsonRDD(json)
    rdd.registerTempTable("tmp_table")

    val partitionedTableDir = File.createTempFile("persisted_table", "sparksql")
    partitionedTableDir.delete()
    partitionedTableDir.mkdir()

    sql(
      s"""
        |create external table persisted_table
        |(
        |  data_array ARRAY <STRUCT<field0: BIGINT, field1: BIGINT, field2: BIGINT>>,
        |  timestamp BIGINT
        |)
        |STORED AS PARQUET Location '${partitionedTableDir.getCanonicalPath}'
      """.stripMargin)

    sql("insert into table persisted_table select * from tmp_table").collect

    checkAnswer(
      sql("select data_array.field0, data_array.field1, data_array.field2 from persisted_table"),
      Row(ArrayBuffer(null), ArrayBuffer(1), ArrayBuffer(2)) ::
      Row (ArrayBuffer(0), ArrayBuffer(null), ArrayBuffer(3)) :: Nil
    )
  }
}

class ParquetDataSourceOffSourceSuite extends ParquetSourceSuiteBase {
  val originalConf = conf.parquetUseDataSourceApi

  override def beforeAll(): Unit = {
    super.beforeAll()
    conf.setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, "false")
  }

  override def afterAll(): Unit = {
    super.afterAll()
    setConf(SQLConf.PARQUET_USE_DATA_SOURCE_API, originalConf.toString)
  }
}

/**
 * A collection of tests for parquet data with various forms of partitioning.
 */
abstract class ParquetPartitioningTest extends QueryTest with BeforeAndAfterAll {
  var partitionedTableDir: File = null
  var normalTableDir: File = null
  var partitionedTableDirWithKey: File = null


  override def beforeAll(): Unit = {
    partitionedTableDir = File.createTempFile("parquettests", "sparksql")
    partitionedTableDir.delete()
    partitionedTableDir.mkdir()

    normalTableDir = File.createTempFile("parquettests", "sparksql")
    normalTableDir.delete()
    normalTableDir.mkdir()

    (1 to 10).foreach { p =>
      val partDir = new File(partitionedTableDir, s"p=$p")
      sparkContext.makeRDD(1 to 10)
        .map(i => ParquetData(i, s"part-$p"))
        .toDF()
        .saveAsParquetFile(partDir.getCanonicalPath)
    }

    sparkContext
      .makeRDD(1 to 10)
      .map(i => ParquetData(i, s"part-1"))
      .toDF()
      .saveAsParquetFile(new File(normalTableDir, "normal").getCanonicalPath)

    partitionedTableDirWithKey = File.createTempFile("parquettests", "sparksql")
    partitionedTableDirWithKey.delete()
    partitionedTableDirWithKey.mkdir()

    (1 to 10).foreach { p =>
      val partDir = new File(partitionedTableDirWithKey, s"p=$p")
      sparkContext.makeRDD(1 to 10)
        .map(i => ParquetDataWithKey(p, i, s"part-$p"))
        .toDF()
        .saveAsParquetFile(partDir.getCanonicalPath)
    }
  }

  Seq("partitioned_parquet", "partitioned_parquet_with_key").foreach { table =>
    test(s"ordering of the partitioning columns $table") {
      checkAnswer(
        sql(s"SELECT p, stringField FROM $table WHERE p = 1"),
        Seq.fill(10)(Row(1, "part-1"))
      )

      checkAnswer(
        sql(s"SELECT stringField, p FROM $table WHERE p = 1"),
        Seq.fill(10)(Row("part-1", 1))
      )
    }

    test(s"project the partitioning column $table") {
      checkAnswer(
        sql(s"SELECT p, count(*) FROM $table group by p"),
        Row(1, 10) ::
          Row(2, 10) ::
          Row(3, 10) ::
          Row(4, 10) ::
          Row(5, 10) ::
          Row(6, 10) ::
          Row(7, 10) ::
          Row(8, 10) ::
          Row(9, 10) ::
          Row(10, 10) :: Nil
      )
    }

    test(s"project partitioning and non-partitioning columns $table") {
      checkAnswer(
        sql(s"SELECT stringField, p, count(intField) FROM $table GROUP BY p, stringField"),
        Row("part-1", 1, 10) ::
          Row("part-2", 2, 10) ::
          Row("part-3", 3, 10) ::
          Row("part-4", 4, 10) ::
          Row("part-5", 5, 10) ::
          Row("part-6", 6, 10) ::
          Row("part-7", 7, 10) ::
          Row("part-8", 8, 10) ::
          Row("part-9", 9, 10) ::
          Row("part-10", 10, 10) :: Nil
      )
    }

    test(s"simple count $table") {
      checkAnswer(
        sql(s"SELECT COUNT(*) FROM $table"),
        Row(100))
    }

    test(s"pruned count $table") {
      checkAnswer(
        sql(s"SELECT COUNT(*) FROM $table WHERE p = 1"),
        Row(10))
    }

    test(s"non-existent partition $table") {
      checkAnswer(
        sql(s"SELECT COUNT(*) FROM $table WHERE p = 1000"),
        Row(0))
    }

    test(s"multi-partition pruned count $table") {
      checkAnswer(
        sql(s"SELECT COUNT(*) FROM $table WHERE p IN (1,2,3)"),
        Row(30))
    }

    test(s"non-partition predicates $table") {
      checkAnswer(
        sql(s"SELECT COUNT(*) FROM $table WHERE intField IN (1,2,3)"),
        Row(30))
    }

    test(s"sum $table") {
      checkAnswer(
        sql(s"SELECT SUM(intField) FROM $table WHERE intField IN (1,2,3) AND p = 1"),
        Row(1 + 2 + 3))
    }

    test(s"hive udfs $table") {
      checkAnswer(
        sql(s"SELECT concat(stringField, stringField) FROM $table"),
        sql(s"SELECT stringField FROM $table").map {
          case Row(s: String) => Row(s + s)
        }.collect().toSeq)
    }
  }

  test("non-part select(*)") {
    checkAnswer(
      sql("SELECT COUNT(*) FROM normal_parquet"),
      Row(10))
  }
}
