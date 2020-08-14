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

package org.apache.spark.sql.execution.bucketing

import org.apache.spark.{Partition, TaskContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.catalog.BucketSpec
import org.apache.spark.sql.catalyst.expressions.{Attribute, UnsafeProjection}
import org.apache.spark.sql.catalyst.plans.physical.HashPartitioning
import org.apache.spark.sql.execution.RowToColumnConverter
import org.apache.spark.sql.execution.datasources.{FilePartition, FileScanRDD, PartitionedFile}
import org.apache.spark.sql.execution.vectorized.{OffHeapColumnVector, OnHeapColumnVector}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.vectorized.ColumnarBatch

private[spark] class SplitBucketRDD(
    @transient private val sparkSession: SparkSession,
    readFunction: PartitionedFile => Iterator[InternalRow],
    @transient override val filePartitions: Seq[FilePartition],
    bucketSpec: BucketSpec,
    newNumBuckets: Int,
    output: Seq[Attribute])
  extends FileScanRDD(sparkSession, readFunction, filePartitions) {

  private val numRows: Int = sparkSession.sessionState.conf.columnBatchSize
  private val useOffHeap: Boolean = sparkSession.sessionState.conf.offHeapColumnVectorEnabled

  override def compute(split: Partition, context: TaskContext): Iterator[InternalRow] = {
    val schema = StructType.fromAttributes(output)
    val converters = new RowToColumnConverter(schema)
    val vectors = if (useOffHeap) {
      OffHeapColumnVector.allocateColumns(numRows, schema)
    } else {
      OnHeapColumnVector.allocateColumns(numRows, schema)
    }
    val columnarBatch = new ColumnarBatch(vectors.toArray)
    context.addTaskCompletionListener[Unit] { _ =>
      columnarBatch.close()
    }

    val iter: Iterator[_] = super.compute(split, context)
    iter.map {
      case row: InternalRow => row
      case batch: ColumnarBatch =>
        val rowIterator = batch.rowIterator()
        columnarBatch.setNumRows(0)
        vectors.foreach(_.reset())
        var rowCount = 0
        while (rowIterator.hasNext) {
          val row = rowIterator.next()
          if (getBucketId(row) == split.index) {
            converters.convert(row, vectors.toArray)
            rowCount += 1
          }
        }
        columnarBatch.setNumRows(rowCount)
        columnarBatch
    }.filter {
      case r: InternalRow =>
        getBucketId(r) == split.index
      case _: ColumnarBatch => true
    }.asInstanceOf[Iterator[InternalRow]]
  }

  private lazy val getBucketId: InternalRow => Int = {
    val bucketIdExpression = {
      val bucketColumns = bucketSpec.bucketColumnNames.map(c => output.find(_.name == c).get)
      HashPartitioning(bucketColumns, newNumBuckets).partitionIdExpression
    }

    val projection = UnsafeProjection.create(Seq(bucketIdExpression), output)
    row => projection(row).getInt(0)
  }
}
