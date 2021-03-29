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

package org.apache.spark.sql.execution.command

import scala.util.control.NonFatal

import org.apache.spark.sql.{Row, SparkSession}


/**
 * Analyzes all tables in the given database to generate statistics.
 */
case class AnalyzeTablesCommand(
    databaseName: Option[String],
    noScan: Boolean) extends RunnableCommand {

  override def run(sparkSession: SparkSession): Seq[Row] = {
    val catalog = sparkSession.sessionState.catalog
    val db = databaseName.getOrElse(catalog.getCurrentDatabase)
    catalog.listTables(db).foreach { tbl =>
      try {
        CommandUtils.analyzeTable(sparkSession, tbl, noScan)
      } catch {
        case NonFatal(e) =>
          logWarning(s"Failed to analyze table ${tbl.table} in the " +
            s"database $db because of ${e.toString}", e)
      }
    }
    Seq.empty[Row]
  }
}
