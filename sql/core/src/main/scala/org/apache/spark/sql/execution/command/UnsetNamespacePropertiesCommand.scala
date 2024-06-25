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

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.catalyst.analysis.ResolvedNamespace
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.connector.catalog.CatalogV2Implicits.CatalogHelper
import org.apache.spark.sql.connector.catalog.NamespaceChange
import org.apache.spark.sql.errors.QueryCompilationErrors

/**
 * A command that ALTER NAMESPACE UNSET PROPERTIES command.
 *
 * The syntax of this command is:
 * {{{
 *     ALTER (DATABASE|SCHEMA|NAMESPACE) ... UNSET (DBPROPERTIES|PROPERTIES) [IF EXISTS] ...;
 * }}}
 */
case class UnsetNamespacePropertiesCommand(
    ident: LogicalPlan,
    propKeys: Seq[String],
    ifExists: Boolean) extends UnaryRunnableCommand {

  override def run(sparkSession: SparkSession): Seq[Row] = {
    val ResolvedNamespace(catalog, ns, metadata) = child
    if (!ifExists) {
      val nonexistentKeys = propKeys.filter(key => !metadata.contains(key))
      if (nonexistentKeys.nonEmpty) {
        throw QueryCompilationErrors.unsetNonExistentPropertiesError(
          nonexistentKeys, ns)
      }
    }
    val changes = propKeys.map {
      NamespaceChange.removeProperty
    }
    catalog.asNamespaceCatalog.alterNamespace(ns.toArray, changes: _*)

    Seq.empty
  }

  override def child: LogicalPlan = ident

  override protected def withNewChildInternal(
      newChild: LogicalPlan): UnsetNamespacePropertiesCommand =
    copy(ident = newChild)
}
