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

import org.scalatest.{BeforeAndAfterAll, FunSuite}

import org.apache.spark.rdd.RDD

import parquet.schema.MessageTypeParser

import org.apache.hadoop.fs.{Path, FileSystem}
import parquet.hadoop.ParquetFileWriter
import org.apache.hadoop.mapreduce.Job
import parquet.hadoop.util.ContextUtil
import org.apache.spark.sql.TestSqlContext
import org.apache.spark.sql.catalyst.expressions.Row

class ParquetQueryTests extends FunSuite with BeforeAndAfterAll {
  override def beforeAll() {
    ParquetTestData.writeFile
  }

  override def afterAll() {
    ParquetTestData.testFile.delete()
  }

  test("Import of simple Parquet file") {
    val result = getRDD(ParquetTestData.testData).collect()
    val allChecks: Boolean = result.zipWithIndex.forall {
      case (row, index) => {
        val checkBoolean =
          if (index % 3 == 0)
            (row(0) == true)
          else
            (row(0) == false)
        val checkInt = ((index % 5) != 0) || (row(1) == 5)
        val checkString = (row(2) == "abc")
        val checkLong = (row(3) == (1L<<33))
        val checkFloat = (row(4) == 2.5F)
        val checkDouble = (row(5) == 4.5D)
        checkBoolean && checkInt && checkString && checkLong && checkFloat && checkDouble
      }
    }
    assert(allChecks)
  }

  test("Projection of simple Parquet file") {
    val scanner = new ParquetTableScan(ParquetTestData.testData.output, ParquetTestData.testData, None)(TestSqlContext.sparkContext)
    val projected = scanner.pruneColumns(ParquetTypesConverter.convertToAttributes(MessageTypeParser.parseMessageType(ParquetTestData.subTestSchema)))
    assert(projected.output.size === 2)
    val result = projected.execute().collect()
    val allChecks: Boolean = result.zipWithIndex.forall {
      case (row, index) => {
        val checkBoolean =
          if (index % 3 == 0)
            (row(0) == true)
          else
            (row(0) == false)
        val checkLong = (row(1) == (1L<<33))
        checkBoolean && checkLong && (row.size == 2)
      }
    }
    assert(allChecks)
  }

  test("Writing metadata from scratch for table CREATE") {
    val job = new Job()
    val path = new Path("file:///tmp/test/mytesttable")
    val fs: FileSystem = FileSystem.getLocal(ContextUtil.getConfiguration(job))
    ParquetTypesConverter.writeMetaData(ParquetTestData.testData.output, path, TestSqlContext.sparkContext.hadoopConfiguration)
    assert(fs.exists(new Path(path, ParquetFileWriter.PARQUET_METADATA_FILE)))
    val metaData = ParquetTypesConverter.readMetaData(path)
    assert(metaData != null)
    ParquetTestData.testData.parquetSchema.checkContains(metaData.getFileMetaData.getSchema) // throws exception if incompatible
    metaData.getFileMetaData.getSchema.checkContains(ParquetTestData.testData.parquetSchema) // throws exception if incompatible
    fs.delete(path.getParent, true)
  }

  /**
   * Computes the given [[ParquetRelation]] and returns its RDD.
   *
   * @param parquetRelation The Parquet relation.
   * @return An RDD of Rows.
   */
  private def getRDD(parquetRelation: ParquetRelation): RDD[Row] = {
    val scanner = new ParquetTableScan(parquetRelation.output, parquetRelation, None)(TestSqlContext.sparkContext)
    scanner.execute
  }
}

