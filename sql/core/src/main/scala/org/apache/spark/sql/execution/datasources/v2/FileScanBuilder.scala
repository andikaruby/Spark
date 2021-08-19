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

import scala.collection.mutable

import org.apache.spark.sql.{sources, SparkSession}
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.connector.read.{ScanBuilder, SupportsPushDownRequiredColumns}
import org.apache.spark.sql.execution.datasources.{DataSourceStrategy, PartitioningAwareFileIndex, PartitioningUtils}
import org.apache.spark.sql.sources.Filter
import org.apache.spark.sql.types.StructType

abstract class FileScanBuilder(
    sparkSession: SparkSession,
    fileIndex: PartitioningAwareFileIndex,
    dataSchema: StructType) extends ScanBuilder with SupportsPushDownRequiredColumns {
  private val partitionSchema = fileIndex.partitionSchema
  private val isCaseSensitive = sparkSession.sessionState.conf.caseSensitiveAnalysis
  protected val supportsNestedSchemaPruning = false
  protected var requiredSchema = StructType(dataSchema.fields ++ partitionSchema.fields)
  protected var partitionFilters = Seq.empty[Expression]
  protected var dataFilters = Seq.empty[Expression]
  protected var pushedDataFilters = Array.empty[Filter]

  override def pruneColumns(requiredSchema: StructType): Unit = {
    // [SPARK-30107] While `requiredSchema` might have pruned nested columns,
    // the actual data schema of this scan is determined in `readDataSchema`.
    // File formats that don't support nested schema pruning,
    // use `requiredSchema` as a reference and prune only top-level columns.
    this.requiredSchema = requiredSchema
  }

  protected def readDataSchema(): StructType = {
    val requiredNameSet = createRequiredNameSet()
    val schema = if (supportsNestedSchemaPruning) requiredSchema else dataSchema
    val fields = schema.fields.filter { field =>
      val colName = PartitioningUtils.getColName(field, isCaseSensitive)
      requiredNameSet.contains(colName) && !partitionNameSet.contains(colName)
    }
    StructType(fields)
  }

  def readPartitionSchema(): StructType = {
    val requiredNameSet = createRequiredNameSet()
    val fields = partitionSchema.fields.filter { field =>
      val colName = PartitioningUtils.getColName(field, isCaseSensitive)
      requiredNameSet.contains(colName)
    }
    StructType(fields)
  }

  def pushFilters(partitionFilters: Seq[Expression], dataFilters: Seq[Expression]): Unit = {

    def translateDataFilter(): Array[Filter] = {
      val translatedFilters = mutable.ArrayBuffer.empty[sources.Filter]
      for (filterExpr <- dataFilters) {
        val translated = DataSourceStrategy.translateFilter(filterExpr, true)
        if (translated.nonEmpty) {
          translatedFilters += translated.get
        }
      }
      translatedFilters.toArray
    }

    this.partitionFilters = partitionFilters
    this.dataFilters = dataFilters
    this.pushedDataFilters = pushDataFilters(translateDataFilter)
  }

  protected def pushDataFilters(dataFilters: Array[Filter]): Array[Filter] = Array.empty[Filter]


  def getSparkSession: SparkSession = sparkSession

  private def createRequiredNameSet(): Set[String] =
    requiredSchema.fields.map(PartitioningUtils.getColName(_, isCaseSensitive)).toSet

  private val partitionNameSet: Set[String] =
    partitionSchema.fields.map(PartitioningUtils.getColName(_, isCaseSensitive)).toSet
}
