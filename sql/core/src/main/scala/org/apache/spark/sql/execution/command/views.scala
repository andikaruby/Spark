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

import scala.collection.mutable

import org.apache.spark.sql.{AnalysisException, Row, SparkSession}
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.analysis.{UnresolvedFunction, UnresolvedRelation}
import org.apache.spark.sql.catalyst.catalog.{CatalogStorageFormat, CatalogTable, CatalogTableType}
import org.apache.spark.sql.catalyst.expressions.Alias
import org.apache.spark.sql.catalyst.plans.QueryPlan
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, Project}
import org.apache.spark.sql.types.MetadataBuilder


/**
 * ViewType is used to specify the expected view type when we want to create or replace a view in
 * [[CreateViewCommand]].
 */
sealed trait ViewType {
  override def toString: String = getClass.getSimpleName.stripSuffix("$")
}

/**
 * LocalTempView means session-scoped local temporary views. Its lifetime is the lifetime of the
 * session that created it, i.e. it will be automatically dropped when the session terminates. It's
 * not tied to any databases, i.e. we can't use `db1.view1` to reference a local temporary view.
 */
object LocalTempView extends ViewType

/**
 * GlobalTempView means cross-session global temporary views. Its lifetime is the lifetime of the
 * Spark application, i.e. it will be automatically dropped when the application terminates. It's
 * tied to a system preserved database `_global_temp`, and we must use the qualified name to refer a
 * global temp view, e.g. SELECT * FROM _global_temp.view1.
 */
object GlobalTempView extends ViewType

/**
 * PersistedView means cross-session persisted views. Persisted views stay until they are
 * explicitly dropped by user command. It's always tied to a database, default to the current
 * database if not specified.
 *
 * Note that, Existing persisted view with the same name are not visible to the current session
 * while the local temporary view exists, unless the view name is qualified by database.
 */
object PersistedView extends ViewType


/**
 * Create or replace a view with given query plan. This command will generate some view-specific
 * properties(e.g. view default database, view query output column names) and store them as
 * properties in metastore, if we need to create a permanent view.
 *
 * @param name the name of this view.
 * @param userSpecifiedColumns the output column names and optional comments specified by users,
 *                             can be Nil if not specified.
 * @param comment the comment of this view.
 * @param properties the properties of this view.
 * @param originalText the original SQL text of this view, can be None if this view is created via
 *                     Dataset API.
 * @param child the logical plan that represents the view; this is used to generate the logical
 *              plan for temporary view and the view schema.
 * @param allowExisting if true, and if the view already exists, noop; if false, and if the view
 *                already exists, throws analysis exception.
 * @param replace if true, and if the view already exists, updates it; if false, and if the view
 *                already exists, throws analysis exception.
 * @param viewType the expected view type to be created with this command.
 */
case class CreateViewCommand(
    name: TableIdentifier,
    userSpecifiedColumns: Seq[(String, Option[String])],
    comment: Option[String],
    properties: Map[String, String],
    originalText: Option[String],
    child: LogicalPlan,
    allowExisting: Boolean,
    replace: Boolean,
    viewType: ViewType)
  extends RunnableCommand {

  import ViewHelper._

  override protected def innerChildren: Seq[QueryPlan[_]] = Seq(child)

  if (viewType == PersistedView) {
    require(originalText.isDefined, "'originalText' must be provided to create permanent view")
  }

  if (allowExisting && replace) {
    throw new AnalysisException("CREATE VIEW with both IF NOT EXISTS and REPLACE is not allowed.")
  }

  private def isTemporary = viewType == LocalTempView || viewType == GlobalTempView

  // Disallows 'CREATE TEMPORARY VIEW IF NOT EXISTS' to be consistent with 'CREATE TEMPORARY TABLE'
  if (allowExisting && isTemporary) {
    throw new AnalysisException(
      "It is not allowed to define a TEMPORARY view with IF NOT EXISTS.")
  }

  // Temporary view names should NOT contain database prefix like "database.table"
  if (isTemporary && name.database.isDefined) {
    val database = name.database.get
    throw new AnalysisException(
      s"It is not allowed to add database prefix `$database` for the TEMPORARY view name.")
  }

  override def run(sparkSession: SparkSession): Seq[Row] = {
    // If the plan cannot be analyzed, throw an exception and don't proceed.
    val qe = sparkSession.sessionState.executePlan(child)
    qe.assertAnalyzed()
    val analyzedPlan = qe.analyzed

    if (userSpecifiedColumns.nonEmpty &&
        userSpecifiedColumns.length != analyzedPlan.output.length) {
      throw new AnalysisException(s"The number of columns produced by the SELECT clause " +
        s"(num: `${analyzedPlan.output.length}`) does not match the number of column names " +
        s"specified by CREATE VIEW (num: `${userSpecifiedColumns.length}`).")
    }

    // When creating a permanent view, not allowed to reference temporary objects.
    // This should be called after `qe.assertAnalyzed()` (i.e., `child` can be resolved)
    verifyTemporaryObjectsNotExists(sparkSession)

    val aliasedPlan = if (userSpecifiedColumns.isEmpty) {
      analyzedPlan
    } else {
      val projectList = analyzedPlan.output.zip(userSpecifiedColumns).map {
        case (attr, (colName, None)) => Alias(attr, colName)()
        case (attr, (colName, Some(colComment))) =>
          val meta = new MetadataBuilder().putString("comment", colComment).build()
          Alias(attr, colName)(explicitMetadata = Some(meta))
      }
      sparkSession.sessionState.executePlan(Project(projectList, analyzedPlan)).analyzed
    }

    val catalog = sparkSession.sessionState.catalog
    if (viewType == LocalTempView) {
      catalog.createTempView(name.table, aliasedPlan, overrideIfExists = replace)
    } else if (viewType == GlobalTempView) {
      catalog.createGlobalTempView(name.table, aliasedPlan, overrideIfExists = replace)
    } else if (catalog.tableExists(name)) {
      val tableMetadata = catalog.getTableMetadata(name)
      if (allowExisting) {
        // Handles `CREATE VIEW IF NOT EXISTS v0 AS SELECT ...`. Does nothing when the target view
        // already exists.
      } else if (tableMetadata.tableType != CatalogTableType.VIEW) {
        throw new AnalysisException(s"$name is not a view")
      } else if (replace) {
        // Handles `CREATE OR REPLACE VIEW v0 AS SELECT ...`
        catalog.alterTable(prepareTable(sparkSession, aliasedPlan, analyzedPlan))
      } else {
        // Handles `CREATE VIEW v0 AS SELECT ...`. Throws exception when the target view already
        // exists.
        throw new AnalysisException(
          s"View $name already exists. If you want to update the view definition, " +
            "please use ALTER VIEW AS or CREATE OR REPLACE VIEW AS")
      }
    } else {
      // Create the view if it doesn't exist.
      catalog.createTable(prepareTable(sparkSession, aliasedPlan, analyzedPlan),
        ignoreIfExists = false)
    }
    Seq.empty[Row]
  }

  /**
   * Permanent views are not allowed to reference temp objects, including temp function and views
   */
  private def verifyTemporaryObjectsNotExists(sparkSession: SparkSession): Unit = {
    if (!isTemporary) {
      // This func traverses the unresolved plan `child`. Below are the reasons:
      // 1) Analyzer replaces unresolved temporary views by a SubqueryAlias with the corresponding
      // logical plan. After replacement, it is impossible to detect whether the SubqueryAlias is
      // added/generated from a temporary view.
      // 2) The temp functions are represented by multiple classes. Most are inaccessible from this
      // package (e.g., HiveGenericUDF).
      child.collect {
        // Disallow creating permanent views based on temporary views.
        case s: UnresolvedRelation
          if sparkSession.sessionState.catalog.isTemporaryTable(s.tableIdentifier) =>
          throw new AnalysisException(s"Not allowed to create a permanent view $name by " +
            s"referencing a temporary view ${s.tableIdentifier}")
        case other if !other.resolved => other.expressions.flatMap(_.collect {
          // Disallow creating permanent views based on temporary UDFs.
          case e: UnresolvedFunction
            if sparkSession.sessionState.catalog.isTemporaryFunction(e.name) =>
            throw new AnalysisException(s"Not allowed to create a permanent view $name by " +
              s"referencing a temporary function `${e.name}`")
        })
      }
    }
  }

  /**
   * Returns a [[CatalogTable]] that can be used to save in the catalog. Generate the view-specific
   * properties(e.g. view default database, view query output column names) and store them as
   * properties in the CatalogTable, and also creates the proper schema for the view.
   *
   * @param session the spark session.
   * @param aliasedPlan if `userSpecifiedColumns` is defined, the aliased plan outputs the user
   *                    specified columns, else it is the same as the `analyzedPlan`.
   * @param analyzedPlan the analyzed logical plan that represents the child of a view.
   * @return a CatalogTable that can be used to save in the catalog.
   */
  private def prepareTable(
      session: SparkSession,
      aliasedPlan: LogicalPlan,
      analyzedPlan: LogicalPlan): CatalogTable = {
    if (originalText.isEmpty) {
      throw new AnalysisException(
        "It is not allowed to create a persisted view from the Dataset API")
    }

    val newProperties = generateViewProperties(properties, session, analyzedPlan)

    CatalogTable(
      identifier = name,
      tableType = CatalogTableType.VIEW,
      storage = CatalogStorageFormat.empty,
      schema = aliasedPlan.schema,
      properties = newProperties,
      viewOriginalText = originalText,
      viewText = originalText,
      comment = comment
    )
  }
}

/**
 * Alter a view with given query plan. If the view name contains database prefix, this command will
 * alter a permanent view matching the given name, or throw an exception if view not exist. Else,
 * this command will try to alter a temporary view first, if view not exist, try permanent view
 * next, if still not exist, throw an exception.
 *
 * @param name the name of this view.
 * @param originalText the original SQL text of this view. Note that we can only alter a view by
 *                     SQL API, which means we always have originalText.
 * @param query the logical plan that represents the view; this is used to generate the new view
 *              schema.
 */
case class AlterViewAsCommand(
    name: TableIdentifier,
    originalText: String,
    query: LogicalPlan) extends RunnableCommand {

  import ViewHelper._

  override protected def innerChildren: Seq[QueryPlan[_]] = Seq(query)

  override def run(session: SparkSession): Seq[Row] = {
    // If the plan cannot be analyzed, throw an exception and don't proceed.
    val qe = session.sessionState.executePlan(query)
    qe.assertAnalyzed()
    val analyzedPlan = qe.analyzed

    if (session.sessionState.catalog.alterTempViewDefinition(name, analyzedPlan)) {
      // a local/global temp view has been altered, we are done.
    } else {
      alterPermanentView(session, analyzedPlan)
    }

    Seq.empty[Row]
  }

  private def alterPermanentView(session: SparkSession, analyzedPlan: LogicalPlan): Unit = {
    val viewMeta = session.sessionState.catalog.getTableMetadata(name)
    if (viewMeta.tableType != CatalogTableType.VIEW) {
      throw new AnalysisException(s"${viewMeta.identifier} is not a view.")
    }

    val newProperties = generateViewProperties(viewMeta.properties, session, analyzedPlan)

    val updatedViewMeta = viewMeta.copy(
      schema = analyzedPlan.schema,
      properties = newProperties,
      viewOriginalText = Some(originalText),
      viewText = Some(originalText))

    session.sessionState.catalog.alterTable(updatedViewMeta)
  }
}

object ViewHelper {

  import CatalogTable._

  /**
   * Generate the view default database in `properties`.
   */
  def generateViewDefaultDatabase(databaseName: String): Map[String, String] = {
    Map(VIEW_DEFAULT_DATABASE -> databaseName)
  }

  /**
   * Generate the view query output column names in `properties`.
   */
  def generateQueryColumnNames(columns: Seq[String]): Map[String, String] = {
    val props = new mutable.HashMap[String, String]
    if (columns.nonEmpty) {
      props.put(VIEW_QUERY_OUTPUT_NUM_COLUMNS, columns.length.toString)
      columns.zipWithIndex.foreach { case (colName, index) =>
        props.put(s"$VIEW_QUERY_OUTPUT_COLUMN_NAME_PREFIX$index", colName)
      }
    }
    props.toMap
  }

  /**
   * Remove the view query output column names in `properties`.
   */
  def removeQueryColumnNames(properties: Map[String, String]): Map[String, String] = {
    // We can't use `filterKeys` here, as the map returned by `filterKeys` is not serializable,
    // while `CatalogTable` should be serializable.
    properties.filterNot { case (key, _) =>
      key.startsWith(VIEW_QUERY_OUTPUT_PREFIX)
    }
  }

  /**
   * Generate the view properties in CatalogTable, including:
   * 1. view default database that is used to provide the default database name on view resolution.
   * 2. the output column names of the query that creates a view, this is used to map the output of
   *    the view child to the view output during view resolution.
   *
   * @param properties the `properties` in CatalogTable.
   * @param session the spark session.
   * @param analyzedPlan the analyzed logical plan that represents the child of a view.
   * @return new view properties including view default database and query column names properties.
   */
  def generateViewProperties(
      properties: Map[String, String],
      session: SparkSession,
      analyzedPlan: LogicalPlan): Map[String, String] = {
    // Generate the query column names, throw an AnalysisException if there exists duplicate column
    // names.
    val queryOutput = analyzedPlan.schema.fieldNames
    assert(queryOutput.distinct.size == queryOutput.size,
      s"The view output ${queryOutput.mkString("(", ",", ")")} contains duplicate column name.")

    // Generate the view default database name.
    val viewDefaultDatabase = session.sessionState.catalog.getCurrentDatabase

    removeQueryColumnNames(properties) ++
      generateViewDefaultDatabase(viewDefaultDatabase) ++
      generateQueryColumnNames(queryOutput)
  }
}
