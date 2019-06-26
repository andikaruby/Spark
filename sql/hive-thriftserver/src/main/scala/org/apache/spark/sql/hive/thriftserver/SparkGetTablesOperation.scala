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

package org.apache.spark.sql.hive.thriftserver

import java.util.{List => JList, UUID}
import java.util.regex.Pattern

import scala.collection.JavaConverters.seqAsJavaListConverter

import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObjectUtils
import org.apache.hive.service.cli._
import org.apache.hive.service.cli.operation.GetTablesOperation
import org.apache.hive.service.cli.session.HiveSession

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.catalog.CatalogTableType
import org.apache.spark.sql.catalyst.catalog.CatalogTableType._
import org.apache.spark.sql.hive.HiveUtils

/**
 * Spark's own GetTablesOperation
 *
 * @param sqlContext SQLContext to use
 * @param parentSession a HiveSession from SessionManager
 * @param catalogName catalog name. null if not applicable
 * @param schemaName database name, null or a concrete database name
 * @param tableName table name pattern
 * @param tableTypes list of allowed table types, e.g. "TABLE", "VIEW"
 */
private[hive] class SparkGetTablesOperation(
    sqlContext: SQLContext,
    parentSession: HiveSession,
    catalogName: String,
    schemaName: String,
    tableName: String,
    tableTypes: JList[String])
  extends GetTablesOperation(parentSession, catalogName, schemaName, tableName, tableTypes) {

  if (tableTypes != null) {
    this.tableTypes.addAll(tableTypes)
  }

  override def runInternal(): Unit = {
    setState(OperationState.RUNNING)
    // Always use the latest class loader provided by executionHive's state.
    val executionHiveClassLoader = sqlContext.sharedState.jarClassLoader
    Thread.currentThread().setContextClassLoader(executionHiveClassLoader)

    val catalog = sqlContext.sessionState.catalog
    val schemaPattern = convertSchemaPattern(schemaName)
    val tablePattern = convertIdentifierPattern(tableName, true)
    val matchingDbs = catalog.listDatabases(schemaPattern)

    if (isAuthV2Enabled) {
      val privObjs =
        HivePrivilegeObjectUtils.getHivePrivDbObjects(seqAsJavaListConverter(matchingDbs).asJava)
      val cmdStr = s"catalog : $catalogName, schemaPattern : $schemaName"
      authorizeMetaGets(HiveOperationType.GET_TABLES, privObjs, cmdStr)
    }
    // scalastyle:off
    System.out.println("matchingDbs: " + matchingDbs.mkString(","))

    // Tables and views
    matchingDbs.foreach { dbName =>
      catalog.getTablesByName(catalog.listTables(dbName, tablePattern)).foreach { catalogTable =>
        val tableType = tableTypeString(catalogTable.tableType)
        if (tableTypes == null || tableTypes.isEmpty || tableTypes.contains(tableType)) {
          addToRowSet(
            catalogTable.database, catalogTable.identifier.table, tableType, catalogTable.comment)
        }
      }
    }

    // Temporary views and global temporary views
    if (tableTypes == null || tableTypes.isEmpty || tableTypes.contains(tableTypeString(VIEW))) {
      val globalTempViewDb = catalog.globalTempViewManager.database
      val databasePattern = Pattern.compile(CLIServiceUtils.patternToRegex(schemaName))
      if (databasePattern.matcher(globalTempViewDb).matches()) {
        catalog.listTempViews(globalTempViewDb, tablePattern).foreach { views =>
          val viewName = views.table
          catalog.getTempView(viewName).foreach { _ =>
            // Set temporary view's database name to empty
            addToRowSet("", viewName, tableTypeString(VIEW), None)
          }
          catalog.globalTempViewManager.get(viewName).foreach { plan =>
            addToRowSet(globalTempViewDb, viewName, tableTypeString(VIEW), None)
          }
        }
      }
    }
    setState(OperationState.FINISHED)
  }

  private def addToRowSet(
      dbName: String,
      tableName: String,
      tableType: String,
      comment: Option[String]): Unit = {
    val rowData = Array[AnyRef](
      "",
      dbName,
      tableName,
      tableType,
      comment.getOrElse(""))
    // Since HIVE-7575(Hive 2.0.0), adds 5 additional columns to the ResultSet of GetTables.
    if (HiveUtils.isHive23) {
      rowSet.addRow(rowData ++ Array(null, null, null, null, null))
    } else {
      rowSet.addRow(rowData)
    }
  }

  private def tableTypeString(tableType: CatalogTableType): String = tableType match {
    case EXTERNAL | MANAGED => "TABLE"
    case VIEW => "VIEW"
    case t =>
      throw new IllegalArgumentException(s"Unknown table type is found at showCreateHiveTable: $t")
  }
}
