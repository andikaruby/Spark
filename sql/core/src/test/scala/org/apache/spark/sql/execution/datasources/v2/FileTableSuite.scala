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

import scala.jdk.CollectionConverters._

import org.apache.hadoop.fs.FileStatus

import org.apache.spark.sql.{QueryTest, SparkSession}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.connector.write.{LogicalWriteInfo, WriteBuilder}
import org.apache.spark.sql.execution.datasources.DataSource
import org.apache.spark.sql.execution.datasources.FileFormat
import org.apache.spark.sql.execution.datasources.text.TextFileFormat
import org.apache.spark.sql.execution.datasources.v2.csv.CSVScanBuilder
import org.apache.spark.sql.execution.datasources.v2.json.JsonScanBuilder
import org.apache.spark.sql.execution.datasources.v2.orc.OrcScanBuilder
import org.apache.spark.sql.execution.datasources.v2.parquet.ParquetScanBuilder
import org.apache.spark.sql.execution.datasources.v2.text.TextScanBuilder
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.util.CaseInsensitiveStringMap

class DummyFileTable(
    sparkSession: SparkSession,
    options: CaseInsensitiveStringMap,
    paths: Seq[String],
    expectedDataSchema: StructType,
    userSpecifiedSchema: Option[StructType])
  extends FileTable(sparkSession, options, paths, userSpecifiedSchema) {
  override def inferSchema(files: Seq[FileStatus]): Option[StructType] = Some(expectedDataSchema)

  override def name(): String = "Dummy"

  override def formatName: String = "Dummy"

  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder = null

  override def newWriteBuilder(info: LogicalWriteInfo): WriteBuilder = null

  override def supportsDataType(dataType: DataType): Boolean = dataType == StringType

  override def fallbackFileFormat: Class[_ <: FileFormat] = classOf[TextFileFormat]
}

class FileTableSuite extends QueryTest with SharedSparkSession {
  import testImplicits._

  private val allFileBasedDataSources = Seq("orc", "parquet", "csv", "json", "text")

  test("Data type validation should check data schema only") {
    withTempPath { dir =>
      val df = spark.createDataFrame(Seq(("a", 1), ("b", 2))).toDF("v", "p")
      val pathName = dir.getCanonicalPath
      df.write.partitionBy("p").text(pathName)
      val options = new CaseInsensitiveStringMap(Map("path" -> pathName).asJava)
      val expectedDataSchema = StructType(Seq(StructField("v", StringType, true)))
      // DummyFileTable doesn't support Integer data type.
      // However, the partition schema is handled by Spark, so it is allowed to contain
      // Integer data type here.
      val table = new DummyFileTable(spark, options, Seq(pathName), expectedDataSchema, None)
      assert(table.dataSchema == expectedDataSchema)
      val expectedPartitionSchema = StructType(Seq(StructField("p", IntegerType, true)))
      assert(table.fileIndex.partitionSchema == expectedPartitionSchema)
    }
  }

  test("Returns correct data schema when user specified schema contains partition schema") {
    withTempPath { dir =>
      val df = spark.createDataFrame(Seq(("a", 1), ("b", 2))).toDF("v", "p")
      val pathName = dir.getCanonicalPath
      df.write.partitionBy("p").text(pathName)
      val options = new CaseInsensitiveStringMap(Map("path" -> pathName).asJava)
      val userSpecifiedSchema = Some(StructType(Seq(
        StructField("v", StringType, true),
        StructField("p", IntegerType, true))))
      val expectedDataSchema = StructType(Seq(StructField("v", StringType, true)))
      val table =
        new DummyFileTable(spark, options, Seq(pathName), expectedDataSchema, userSpecifiedSchema)
      assert(table.dataSchema == expectedDataSchema)
    }
  }

  allFileBasedDataSources.foreach { format =>
    test(s"SPARK-49519: Merge options of table and relation when constructing FileScanBuilder" +
      s" - $format") {
      withTempPath { path =>
        withSQLConf(SQLConf.USE_V1_SOURCE_LIST.key -> "") {
          val dir = path.getCanonicalPath
          val df = Seq("a", "b").toDF("c1")
          df.write.format(format).option("header", "true").save(dir)

          val userSpecifiedSchema = StructType(Seq(StructField("c1", StringType)))

          DataSource.lookupDataSourceV2(format, spark.sessionState.conf) match {
            case Some(provider) =>
              val dsOptions = new CaseInsensitiveStringMap(
                Map("k1" -> "v1", "k2" -> "ds_v2").asJava)
              val table = provider.getTable(
                userSpecifiedSchema,
                Array.empty,
                dsOptions.asCaseSensitiveMap())
              val tableOptions = new CaseInsensitiveStringMap(
                Map("k2" -> "table_v2", "k3" -> "v3").asJava)
              val mergedOptions = table.asInstanceOf[FileTable].newScanBuilder(tableOptions) match {
                case csv: CSVScanBuilder => csv.options
                case json: JsonScanBuilder => json.options
                case orc: OrcScanBuilder => orc.options
                case parquet: ParquetScanBuilder => parquet.options
                case text: TextScanBuilder => text.options
              }
              assert(mergedOptions.size() == 3)
              assert("v1".equals(mergedOptions.get("k1")))
              assert("table_v2".equals(mergedOptions.get("k2")))
              assert("v3".equals(mergedOptions.get("k3")))
            case _ =>
              throw new IllegalArgumentException(s"Failed to get table provider for $format")
          }

        }
      }
    }
  }
}
