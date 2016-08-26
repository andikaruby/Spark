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

package org.apache.spark.sql.execution.datasources

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan


/**
 * LogicalRelationCache is a key-value cache built on Google Guava Cache to speed up building
 * logical plan nodes (LogicalRelation) for data source tables. The cache key is a unique
 * identifier of a table. Here, the identifier is the fully qualified table name, including the
 * database in which it resides. The value is the corresponding LogicalRelation that represents
 * a specific data source table.
 */
class LogicalRelationCache(sparkSession: SparkSession) extends Logging {
  /** A fully qualified identifier for a table (i.e., database.tableName) */
  case class QualifiedTableName(database: String, name: String)

  private def getQualifiedTableName(tableIdent: TableIdentifier): QualifiedTableName = {
    QualifiedTableName(
      tableIdent.database.getOrElse(
        sparkSession.sessionState.catalog.getCurrentDatabase).toLowerCase,
      tableIdent.table.toLowerCase)
  }

  /** A cache of Spark SQL data source tables that have been accessed. */
  private val cachedDataSourceTables: LoadingCache[QualifiedTableName, LogicalPlan] = {
    val cacheLoader = new CacheLoader[QualifiedTableName, LogicalPlan]() {
      override def load(in: QualifiedTableName): LogicalPlan = {
        logDebug(s"Creating new cached data source for $in")
        val table = sparkSession.sharedState.externalCatalog.getTable(in.database, in.name)

        val dataSource =
          DataSource(
            sparkSession,
            userSpecifiedSchema = Some(table.schema),
            partitionColumns = table.partitionColumnNames,
            bucketSpec = table.bucketSpec,
            className = table.provider.get,
            options = table.storage.properties)

        LogicalRelation(
          dataSource.resolveRelation(checkPathExist = true),
          metastoreTableIdentifier = Some(TableIdentifier(in.name, Some(in.database))))
      }
    }

    CacheBuilder.newBuilder().maximumSize(1000).build(cacheLoader)
  }

  /**
   * cacheTable is a wrapper of cache.put(key, value). It associates value with key in this cache.
   * If the cache previously contained a value associated with key, the old value is replaced by
   * value.
   * Note, this is not using automatic cache loading.
   */
  def cacheTable(tableIdent: TableIdentifier, plan: LogicalPlan): Unit = {
    cachedDataSourceTables.put(getQualifiedTableName(tableIdent), plan)
  }

  /**
   * getTableIfPresent is a wrapper of cache.getIfPresent(key) that never causes values to be
   * automatically loaded.
   */
  def getTableIfPresent(tableIdent: TableIdentifier): Option[LogicalPlan] = {
    cachedDataSourceTables.getIfPresent(getQualifiedTableName(tableIdent)) match {
      case null => None // Cache miss
      case o: LogicalPlan => Option(o.asInstanceOf[LogicalPlan])
    }
  }

  /**
   * getTable is a wrapper of cache.get(key). If cache misses, Caches loaded by a CacheLoader
   * will call CacheLoader.load(K) to load new values into the cache. That means, it will call
   * the function load.
   */
  def getTable(tableIdent: TableIdentifier): LogicalPlan = {
    cachedDataSourceTables.get(getQualifiedTableName(tableIdent))
  }

  /**
   * refreshTable is a wrapper of cache.invalidate. It does not eagerly reload the cache. It just
   * invalidates the cache. Next time when we use the table, it will be populated in the cache.
   */
  def refreshTable(tableIdent: TableIdentifier): Unit = {
    // refreshTable does not eagerly reload the cache. It just invalidate the cache.
    // Next time when we use the table, it will be populated in the cache.
    // Since we also cache ParquetRelations converted from Hive Parquet tables and
    // adding converted ParquetRelations into the cache is not defined in the load function
    // of the cache (instead, we add the cache entry in convertToParquetRelation),
    // it is better at here to invalidate the cache to avoid confusing waring logs from the
    // cache loader (e.g. cannot find data source provider, which is only defined for
    // data source table.).
    cachedDataSourceTables.invalidate(getQualifiedTableName(tableIdent))
  }

  /**
   * Discards all entries in the cache. It is a wrapper of cache.invalidateAll.
   */
  def invalidateAll(): Unit = {
    cachedDataSourceTables.invalidateAll()
  }
}
