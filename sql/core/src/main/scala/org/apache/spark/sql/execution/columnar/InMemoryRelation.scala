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

package org.apache.spark.sql.execution.columnar

import scala.collection.JavaConverters._

import org.apache.commons.lang.StringUtils

import org.apache.spark.memory.MemoryMode
import org.apache.spark.network.util.JavaUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.analysis.MultiInstanceRelation
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.QueryPlan
import org.apache.spark.sql.catalyst.plans.logical
import org.apache.spark.sql.catalyst.plans.logical.Statistics
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.vectorized.ColumnarBatch
import org.apache.spark.sql.types.StructType
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.CollectionAccumulator


private[sql] object InMemoryRelation {
  def apply(
      useCompression: Boolean,
      batchSize: Int,
      storageLevel: StorageLevel,
      child: SparkPlan,
      tableName: Option[String]): InMemoryRelation =
    new InMemoryRelation(child.output, useCompression, batchSize, storageLevel, child, tableName)()
}


/**
 * CachedBatch is a cached batch of rows.
 *
 * @param numRows The total number of rows in this batch
 * @param buffers The buffers for serialized columns
 * @param stats The stat of columns
 */
private[columnar]
case class CachedBatch(numRows: Int, buffers: Array[Array[Byte]], stats: InternalRow)

private[sql] case class InMemoryRelation(
    output: Seq[Attribute],
    useCompression: Boolean,
    batchSize: Int,
    storageLevel: StorageLevel,
    @transient child: SparkPlan,
    tableName: Option[String])(
    @transient private[sql] var _cachedColumnBuffers: RDD[CachedBatch] = null,
    @transient private[sql] var _cachedColumnVectors: RDD[ColumnarBatch] = null,
    @transient private[sql] var _statistics: Statistics = null,
    private[sql] var _batchStats: CollectionAccumulator[InternalRow] = null)
  extends logical.LeafNode with MultiInstanceRelation {

  override protected def innerChildren: Seq[QueryPlan[_]] = Seq(child)

  override def producedAttributes: AttributeSet = outputSet

  private[sql] val batchStats: CollectionAccumulator[InternalRow] =
    if (_batchStats == null) {
      child.sqlContext.sparkContext.collectionAccumulator[InternalRow]
    } else {
      _batchStats
    }

  @transient val partitionStatistics = new PartitionStatistics(output)

  private def computeSizeInBytes = {
    val sizeOfRow: Expression =
      BindReferences.bindReference(
        output.map(a => partitionStatistics.forAttribute(a).sizeInBytes).reduce(Add),
        partitionStatistics.schema)

    batchStats.value.asScala.map(row => sizeOfRow.eval(row).asInstanceOf[Long]).sum
  }

  // Statistics propagation contracts:
  // 1. Non-null `_statistics` must reflect the actual statistics of the underlying data
  // 2. Only propagate statistics when `_statistics` is non-null
  private def statisticsToBePropagated = if (_statistics == null) {
    val updatedStats = statistics
    if (_statistics == null) null else updatedStats
  } else {
    _statistics
  }

  override def statistics: Statistics = {
    if (_statistics == null) {
      if (batchStats.value.isEmpty) {
        // Underlying columnar RDD hasn't been materialized, no useful statistics information
        // available, return the default statistics.
        Statistics(sizeInBytes = child.sqlContext.conf.defaultSizeInBytes)
      } else {
        // Underlying columnar RDD has been materialized, required information has also been
        // collected via the `batchStats` accumulator, compute the final statistics,
        // and update `_statistics`.
        _statistics = Statistics(sizeInBytes = computeSizeInBytes)
        _statistics
      }
    } else {
      // Pre-computed statistics
      _statistics
    }
  }

  // If the cached column buffers were not passed in, we calculate them in the constructor.
  // As in Spark, the actual work of caching is lazy.
  if (_cachedColumnBuffers == null) {
    buildBuffers()
    buildBuffers2()
  }

  def recache(): Unit = {
    _cachedColumnBuffers.unpersist()
    _cachedColumnBuffers = null
    buildBuffers()
    buildBuffers2()
  }

  private def buildBuffers(): Unit = {
    val output = child.output
    val cached = child.execute().mapPartitionsInternal { rowIterator =>
      new Iterator[CachedBatch] {
        def next(): CachedBatch = {
          val columnBuilders = output.map { attribute =>
            ColumnBuilder(attribute.dataType, batchSize, attribute.name, useCompression)
          }.toArray

          var rowCount = 0
          var totalSize = 0L
          while (rowIterator.hasNext && rowCount < batchSize
            && totalSize < ColumnBuilder.MAX_BATCH_SIZE_IN_BYTE) {
            val row = rowIterator.next()

            // Added for SPARK-6082. This assertion can be useful for scenarios when something
            // like Hive TRANSFORM is used. The external data generation script used in TRANSFORM
            // may result malformed rows, causing ArrayIndexOutOfBoundsException, which is somewhat
            // hard to decipher.
            assert(
              row.numFields == columnBuilders.length,
              s"Row column number mismatch, expected ${output.size} columns, " +
                s"but got ${row.numFields}." +
                s"\nRow content: $row")

            var i = 0
            totalSize = 0
            while (i < row.numFields) {
              columnBuilders(i).appendFrom(row, i)
              totalSize += columnBuilders(i).columnStats.sizeInBytes
              i += 1
            }
            rowCount += 1
          }

          val stats = InternalRow.fromSeq(columnBuilders.map(_.columnStats.collectedStatistics)
            .flatMap(_.values))

          batchStats.add(stats)
          CachedBatch(rowCount, columnBuilders.map { builder =>
            JavaUtils.bufferToArray(builder.build())
          }, stats)
        }

        def hasNext: Boolean = rowIterator.hasNext
      }
    }.persist(storageLevel)

    cached.setName(
      tableName.map(n => s"In-memory table $n")
        .getOrElse(StringUtils.abbreviate(child.toString, 1024)))
    _cachedColumnBuffers = cached
  }

  // IWASHERE
  private def buildBuffers2(): Unit = {
    val schema = StructType.fromAttributes(child.output)
    val cached = child.execute().mapPartitionsInternal { rowIterator =>
      new Iterator[ColumnarBatch] {
        def next(): ColumnarBatch = {
          val columnarBatch = ColumnarBatch.allocate(schema, MemoryMode.ON_HEAP, batchSize)
          var rowCount = 0
          var totalSize = 0L
          while (rowIterator.hasNext && rowCount < batchSize
            && totalSize < ColumnBuilder.MAX_BATCH_SIZE_IN_BYTE) {
            val row = rowIterator.next()
            assert(row.numFields == columnarBatch.numCols, "Row column number mismatch, " +
              s"expected ${columnarBatch.numCols} columns, but got ${row.numFields}. \n" +
              s"Row content: $row")
            var i = 0
            totalSize = 0
            while (i < row.numFields) {
              columnarBatch.column(i).appendLong(row.getLong(i))
              totalSize += 8
              i += 1
            }
            rowCount += 1
          }
          columnarBatch.setNumRows(rowCount)
          columnarBatch
        }

        def hasNext: Boolean = rowIterator.hasNext
      }
    }.persist(storageLevel)

    cached.setName(
      tableName.map(n => s"In-memory table $n")
        .getOrElse(StringUtils.abbreviate(child.toString, 1024)))
    _cachedColumnVectors = cached
  }

  def withOutput(newOutput: Seq[Attribute]): InMemoryRelation = {
    InMemoryRelation(
      newOutput, useCompression, batchSize, storageLevel, child, tableName)(
        _cachedColumnBuffers, _cachedColumnVectors, statisticsToBePropagated, batchStats)
  }

  override def newInstance(): this.type = {
    new InMemoryRelation(
      output.map(_.newInstance()),
      useCompression,
      batchSize,
      storageLevel,
      child,
      tableName)(
        _cachedColumnBuffers,
        _cachedColumnVectors,
        statisticsToBePropagated,
        batchStats).asInstanceOf[this.type]
  }

  def cachedColumnBuffers: RDD[CachedBatch] = _cachedColumnBuffers
  def cachedColumnVectors: RDD[ColumnarBatch] = _cachedColumnVectors

  override protected def otherCopyArgs: Seq[AnyRef] =
    Seq(_cachedColumnBuffers, statisticsToBePropagated, batchStats)
}
