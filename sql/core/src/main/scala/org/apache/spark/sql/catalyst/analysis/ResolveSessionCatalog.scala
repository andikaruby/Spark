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

package org.apache.spark.sql.catalyst.analysis

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.{FunctionIdentifier, TableIdentifier}
import org.apache.spark.sql.catalyst.catalog.{BucketSpec, CatalogStorageFormat, CatalogTable, CatalogTableType, CatalogUtils}
import org.apache.spark.sql.catalyst.expressions.{Alias, Attribute}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.catalyst.util.toPrettySQL
import org.apache.spark.sql.connector.catalog.{CatalogManager, CatalogPlugin, CatalogV2Util, Identifier, LookupCatalog, SupportsNamespaces, TableCatalog, TableChange, V1Table}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.errors.QueryCompilationErrors
import org.apache.spark.sql.execution.command._
import org.apache.spark.sql.execution.datasources.{CreateTable, DataSource}
import org.apache.spark.sql.execution.datasources.v2.FileDataSourceV2
import org.apache.spark.sql.internal.{HiveSerDe, SQLConf}
import org.apache.spark.sql.types.{MetadataBuilder, StructField, StructType}

/**
 * Resolves catalogs from the multi-part identifiers in SQL statements, and convert the statements
 * to the corresponding v1 or v2 commands if the resolved catalog is the session catalog.
 *
 * We can remove this rule once we implement all the catalog functionality in `V2SessionCatalog`.
 */
class ResolveSessionCatalog(
    val catalogManager: CatalogManager,
    isTempView: Seq[String] => Boolean,
    isTempFunction: String => Boolean)
  extends Rule[LogicalPlan] with LookupCatalog {
  import org.apache.spark.sql.connector.catalog.CatalogV2Implicits._
  import org.apache.spark.sql.connector.catalog.CatalogV2Util._
  import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Implicits._

  override def apply(plan: LogicalPlan): LogicalPlan = plan.resolveOperatorsUp {
    case AlterTableAddColumnsStatement(
         nameParts @ SessionCatalogAndTable(catalog, tbl), cols) =>
      cols.foreach(c => failNullType(c.dataType))
      loadTable(catalog, tbl.asIdentifier).collect {
        case v1Table: V1Table =>
          cols.foreach { c =>
            assertTopLevelColumn(c.name, "AlterTableAddColumnsCommand")
            if (!c.nullable) {
              throw QueryCompilationErrors.addColumnWithV1TableCannotSpecifyNotNullError
            }
          }
          AlterTableAddColumnsCommand(tbl.asTableIdentifier, cols.map(convertToStructField))
      }.getOrElse {
        val changes = cols.map { col =>
          TableChange.addColumn(
            col.name.toArray,
            col.dataType,
            col.nullable,
            col.comment.orNull,
            col.position.orNull)
        }
        createAlterTable(nameParts, catalog, tbl, changes)
      }

    case AlterTableReplaceColumnsStatement(
        nameParts @ SessionCatalogAndTable(catalog, tbl), cols) =>
      cols.foreach(c => failNullType(c.dataType))
      val changes: Seq[TableChange] = loadTable(catalog, tbl.asIdentifier) match {
        case Some(_: V1Table) =>
          throw QueryCompilationErrors.replaceColumnsOnlySupportedWithV2TableError
        case Some(table) =>
          // REPLACE COLUMNS deletes all the existing columns and adds new columns specified.
          val deleteChanges = table.schema.fieldNames.map { name =>
            TableChange.deleteColumn(Array(name))
          }
          val addChanges = cols.map { col =>
            TableChange.addColumn(
              col.name.toArray,
              col.dataType,
              col.nullable,
              col.comment.orNull,
              col.position.orNull)
          }
          deleteChanges ++ addChanges
        case None => Seq() // Unresolved table will be handled in CheckAnalysis.
      }
      createAlterTable(nameParts, catalog, tbl, changes)

    case a @ AlterTableAlterColumnStatement(
         nameParts @ SessionCatalogAndTable(catalog, tbl), _, _, _, _, _) =>
      a.dataType.foreach(failNullType)
      loadTable(catalog, tbl.asIdentifier).collect {
        case v1Table: V1Table =>
          if (a.column.length > 1) {
            throw QueryCompilationErrors.alterQualifiedColumnOnlySupportedWithV2TableError
          }
          if (a.nullable.isDefined) {
            throw QueryCompilationErrors.alterColumnWithV1TableCannotSpecifyNotNullError
          }
          if (a.position.isDefined) {
            throw QueryCompilationErrors.alterOnlySupportedWithV2TableError
          }
          val builder = new MetadataBuilder
          // Add comment to metadata
          a.comment.map(c => builder.putString("comment", c))
          val colName = a.column(0)
          val dataType = a.dataType.getOrElse {
            v1Table.schema.findNestedField(Seq(colName), resolver = conf.resolver)
              .map(_._2.dataType)
              .getOrElse {
                throw QueryCompilationErrors.alterColumnCannotFindColumnInV1TableError(
                  quoteIfNeeded(colName), v1Table)
              }
          }
          val newColumn = StructField(
            colName,
            dataType,
            nullable = true,
            builder.build())
          AlterTableChangeColumnCommand(tbl.asTableIdentifier, colName, newColumn)
      }.getOrElse {
        val colName = a.column.toArray
        val typeChange = a.dataType.map { newDataType =>
          TableChange.updateColumnType(colName, newDataType)
        }
        val nullabilityChange = a.nullable.map { nullable =>
          TableChange.updateColumnNullability(colName, nullable)
        }
        val commentChange = a.comment.map { newComment =>
          TableChange.updateColumnComment(colName, newComment)
        }
        val positionChange = a.position.map { newPosition =>
          TableChange.updateColumnPosition(colName, newPosition)
        }
        createAlterTable(
          nameParts,
          catalog,
          tbl,
          typeChange.toSeq ++ nullabilityChange ++ commentChange ++ positionChange)
      }

    case AlterTableRenameColumnStatement(
         nameParts @ SessionCatalogAndTable(catalog, tbl), col, newName) =>
      loadTable(catalog, tbl.asIdentifier).collect {
        case v1Table: V1Table =>
          throw QueryCompilationErrors.renameColumnOnlySupportedWithV2TableError
      }.getOrElse {
        val changes = Seq(TableChange.renameColumn(col.toArray, newName))
        createAlterTable(nameParts, catalog, tbl, changes)
      }

    case AlterTableDropColumnsStatement(
         nameParts @ SessionCatalogAndTable(catalog, tbl), cols) =>
      loadTable(catalog, tbl.asIdentifier).collect {
        case v1Table: V1Table =>
          throw QueryCompilationErrors.dropColumnOnlySupportedWithV2TableError
      }.getOrElse {
        val changes = cols.map(col => TableChange.deleteColumn(col.toArray))
        createAlterTable(nameParts, catalog, tbl, changes)
      }

    case AlterTableSetPropertiesStatement(
         nameParts @ SessionCatalogAndTable(catalog, tbl), props) =>
      loadTable(catalog, tbl.asIdentifier).collect {
        case v1Table: V1Table =>
          AlterTableSetPropertiesCommand(tbl.asTableIdentifier, props, isView = false)
      }.getOrElse {
        val changes = props.map { case (key, value) =>
          TableChange.setProperty(key, value)
        }.toSeq
        createAlterTable(nameParts, catalog, tbl, changes)
      }

    case AlterTableUnsetPropertiesStatement(
         nameParts @ SessionCatalogAndTable(catalog, tbl), keys, ifExists) =>
      loadTable(catalog, tbl.asIdentifier).collect {
        case v1Table: V1Table =>
          AlterTableUnsetPropertiesCommand(
            tbl.asTableIdentifier, keys, ifExists, isView = false)
      }.getOrElse {
        val changes = keys.map(key => TableChange.removeProperty(key))
        createAlterTable(nameParts, catalog, tbl, changes)
      }

    case AlterTableSetLocationStatement(
         nameParts @ SessionCatalogAndTable(catalog, tbl), partitionSpec, newLoc) =>
      loadTable(catalog, tbl.asIdentifier).collect {
        case v1Table: V1Table =>
          AlterTableSetLocationCommand(tbl.asTableIdentifier, partitionSpec, newLoc)
      }.getOrElse {
        if (partitionSpec.nonEmpty) {
          throw QueryCompilationErrors.alterV2TableSetLocationWithPartitionNotSupportedError
        }
        val changes = Seq(TableChange.setProperty(TableCatalog.PROP_LOCATION, newLoc))
        createAlterTable(nameParts, catalog, tbl, changes)
      }

    case AlterViewSetProperties(ResolvedView(ident, _), props) =>
      AlterTableSetPropertiesCommand(ident.asTableIdentifier, props, isView = true)

    case AlterViewUnsetProperties(ResolvedView(ident, _), keys, ifExists) =>
      AlterTableUnsetPropertiesCommand(ident.asTableIdentifier, keys, ifExists, isView = true)

    case d @ DescribeNamespace(DatabaseInSessionCatalog(db), _) =>
      DescribeDatabaseCommand(db, d.extended)

    case AlterNamespaceSetProperties(DatabaseInSessionCatalog(db), properties) =>
      AlterDatabasePropertiesCommand(db, properties)

    case AlterNamespaceSetLocation(DatabaseInSessionCatalog(db), location) =>
      AlterDatabaseSetLocationCommand(db, location)

    case RenameTable(ResolvedV1TableOrViewIdentifier(oldName), newName, isView) =>
      AlterTableRenameCommand(oldName.asTableIdentifier, newName.asTableIdentifier, isView)

    // Use v1 command to describe (temp) view, as v2 catalog doesn't support view yet.
    case DescribeRelation(ResolvedV1TableOrViewIdentifier(ident), partitionSpec, isExtended) =>
      DescribeTableCommand(ident.asTableIdentifier, partitionSpec, isExtended)

    case DescribeColumn(ResolvedViewIdentifier(ident), column: UnresolvedAttribute, isExtended) =>
      // For views, the column will not be resolved by `ResolveReferences` because
      // `ResolvedView` stores only the identifier.
      DescribeColumnCommand(ident.asTableIdentifier, column.nameParts, isExtended)

    case DescribeColumn(ResolvedV1TableIdentifier(ident), column, isExtended) =>
      column match {
        case u: UnresolvedAttribute =>
          throw QueryCompilationErrors.columnDoesNotExistError(u.name)
        case a: Attribute =>
          DescribeColumnCommand(ident.asTableIdentifier, a.qualifier :+ a.name, isExtended)
        case Alias(child, _) =>
          throw QueryCompilationErrors.commandNotSupportNestedColumnError(
            "DESC TABLE COLUMN", toPrettySQL(child))
        case other =>
          throw QueryCompilationErrors.unexpectedColumnExpressionError(other)
      }

    // For CREATE TABLE [AS SELECT], we should use the v1 command if the catalog is resolved to the
    // session catalog and the table provider is not v2.
    case c @ CreateTableStatement(
         SessionCatalogAndTable(catalog, tbl), _, _, _, _, _, _, _, _, _, _, _) =>
      assertNoNullTypeInSchema(c.tableSchema)
      val (storageFormat, provider) = getStorageFormatAndProvider(
        c.provider, c.options, c.location, c.serde, ctas = false)
      if (!isV2Provider(provider)) {
        val tableDesc = buildCatalogTable(tbl.asTableIdentifier, c.tableSchema,
          c.partitioning, c.bucketSpec, c.properties, provider, c.location,
          c.comment, storageFormat, c.external)
        val mode = if (c.ifNotExists) SaveMode.Ignore else SaveMode.ErrorIfExists
        CreateTable(tableDesc, mode, None)
      } else {
        CreateV2Table(
          catalog.asTableCatalog,
          tbl.asIdentifier,
          c.tableSchema,
          // convert the bucket spec and add it as a transform
          c.partitioning ++ c.bucketSpec.map(_.asTransform),
          convertTableProperties(c),
          ignoreIfExists = c.ifNotExists)
      }

    case c @ CreateTableAsSelectStatement(
         SessionCatalogAndTable(catalog, tbl), _, _, _, _, _, _, _, _, _, _, _, _) =>
      if (c.asSelect.resolved) {
        assertNoNullTypeInSchema(c.asSelect.schema)
      }
      val (storageFormat, provider) = getStorageFormatAndProvider(
        c.provider, c.options, c.location, c.serde, ctas = true)
      if (!isV2Provider(provider)) {
        val tableDesc = buildCatalogTable(tbl.asTableIdentifier, new StructType,
          c.partitioning, c.bucketSpec, c.properties, provider, c.location,
          c.comment, storageFormat, c.external)
        val mode = if (c.ifNotExists) SaveMode.Ignore else SaveMode.ErrorIfExists
        CreateTable(tableDesc, mode, Some(c.asSelect))
      } else {
        CreateTableAsSelect(
          catalog.asTableCatalog,
          tbl.asIdentifier,
          // convert the bucket spec and add it as a transform
          c.partitioning ++ c.bucketSpec.map(_.asTransform),
          c.asSelect,
          convertTableProperties(c),
          writeOptions = c.writeOptions,
          ignoreIfExists = c.ifNotExists)
      }

    case RefreshTable(ResolvedV1TableIdentifier(ident)) =>
      RefreshTableCommand(ident.asTableIdentifier)

    case RefreshTable(r: ResolvedView) =>
      RefreshTableCommand(r.identifier.asTableIdentifier)

    // For REPLACE TABLE [AS SELECT], we should fail if the catalog is resolved to the
    // session catalog and the table provider is not v2.
    case c @ ReplaceTableStatement(
         SessionCatalogAndTable(catalog, tbl), _, _, _, _, _, _, _, _, _, _) =>
      assertNoNullTypeInSchema(c.tableSchema)
      val provider = c.provider.getOrElse(conf.defaultDataSourceName)
      if (!isV2Provider(provider)) {
        throw QueryCompilationErrors.replaceTableOnlySupportedWithV2TableError
      } else {
        ReplaceTable(
          catalog.asTableCatalog,
          tbl.asIdentifier,
          c.tableSchema,
          // convert the bucket spec and add it as a transform
          c.partitioning ++ c.bucketSpec.map(_.asTransform),
          convertTableProperties(c),
          orCreate = c.orCreate)
      }

    case c @ ReplaceTableAsSelectStatement(
         SessionCatalogAndTable(catalog, tbl), _, _, _, _, _, _, _, _, _, _, _) =>
      if (c.asSelect.resolved) {
        assertNoNullTypeInSchema(c.asSelect.schema)
      }
      val provider = c.provider.getOrElse(conf.defaultDataSourceName)
      if (!isV2Provider(provider)) {
        throw QueryCompilationErrors.replaceTableAsSelectOnlySupportedWithV2TableError
      } else {
        ReplaceTableAsSelect(
          catalog.asTableCatalog,
          tbl.asIdentifier,
          // convert the bucket spec and add it as a transform
          c.partitioning ++ c.bucketSpec.map(_.asTransform),
          c.asSelect,
          convertTableProperties(c),
          writeOptions = c.writeOptions,
          orCreate = c.orCreate)
      }

    case DropTable(ResolvedV1TableIdentifier(ident), ifExists, purge) =>
      DropTableCommand(ident.asTableIdentifier, ifExists, isView = false, purge = purge)

    // v1 DROP TABLE supports temp view.
    case DropTable(r: ResolvedView, ifExists, purge) =>
      if (!r.isTemp) {
        throw QueryCompilationErrors.cannotDropViewWithDropTableError
      }
      DropTableCommand(r.identifier.asTableIdentifier, ifExists, isView = false, purge = purge)

    case DropView(r: ResolvedView, ifExists) =>
      DropTableCommand(r.identifier.asTableIdentifier, ifExists, isView = true, purge = false)

    case c @ CreateNamespaceStatement(CatalogAndNamespace(catalog, ns), _, _)
        if isSessionCatalog(catalog) =>
      if (ns.length != 1) {
        throw QueryCompilationErrors.invalidDatabaseNameError(ns.quoted)
      }

      val comment = c.properties.get(SupportsNamespaces.PROP_COMMENT)
      val location = c.properties.get(SupportsNamespaces.PROP_LOCATION)
      val newProperties = c.properties -- CatalogV2Util.NAMESPACE_RESERVED_PROPERTIES
      CreateDatabaseCommand(ns.head, c.ifNotExists, location, comment, newProperties)

    case d @ DropNamespace(DatabaseInSessionCatalog(db), _, _) =>
      DropDatabaseCommand(db, d.ifExists, d.cascade)

    case ShowTables(DatabaseInSessionCatalog(db), pattern) =>
      ShowTablesCommand(Some(db), pattern)

    case ShowTableExtended(
        DatabaseInSessionCatalog(db),
        pattern,
        partitionSpec @ (None | Some(UnresolvedPartitionSpec(_, _)))) =>
      ShowTablesCommand(
        databaseName = Some(db),
        tableIdentifierPattern = Some(pattern),
        isExtended = true,
        partitionSpec.map(_.asInstanceOf[UnresolvedPartitionSpec].spec))

    // ANALYZE TABLE works on permanent views if the views are cached.
    case AnalyzeTable(ResolvedV1TableOrViewIdentifier(ident), partitionSpec, noScan) =>
      if (partitionSpec.isEmpty) {
        AnalyzeTableCommand(ident.asTableIdentifier, noScan)
      } else {
        AnalyzePartitionCommand(ident.asTableIdentifier, partitionSpec, noScan)
      }

    case AnalyzeColumn(ResolvedV1TableOrViewIdentifier(ident), columnNames, allColumns) =>
      AnalyzeColumnCommand(ident.asTableIdentifier, columnNames, allColumns)

    case RepairTable(ResolvedV1TableIdentifier(ident)) =>
      AlterTableRecoverPartitionsCommand(ident.asTableIdentifier, "MSCK REPAIR TABLE")

    case LoadData(ResolvedV1TableIdentifier(ident), path, isLocal, isOverwrite, partition) =>
      LoadDataCommand(
        ident.asTableIdentifier,
        path,
        isLocal,
        isOverwrite,
        partition)

    case ShowCreateTable(ResolvedV1TableOrViewIdentifier(ident), asSerde) =>
      if (asSerde) {
        ShowCreateTableAsSerdeCommand(ident.asTableIdentifier)
      } else {
        ShowCreateTableCommand(ident.asTableIdentifier)
      }

    case TruncateTable(ResolvedV1TableIdentifier(ident), partitionSpec) =>
      TruncateTableCommand(
        ident.asTableIdentifier,
        partitionSpec)

    case ShowPartitions(
        ResolvedV1TableOrViewIdentifier(ident),
        pattern @ (None | Some(UnresolvedPartitionSpec(_, _)))) =>
      ShowPartitionsCommand(
        ident.asTableIdentifier,
        pattern.map(_.asInstanceOf[UnresolvedPartitionSpec].spec))

    case ShowColumns(ResolvedV1TableOrViewIdentifier(ident), ns) =>
      val v1TableName = ident.asTableIdentifier
      val resolver = conf.resolver
      val db = ns match {
        case Some(db) if v1TableName.database.exists(!resolver(_, db.head)) =>
          throw QueryCompilationErrors.showColumnsWithConflictDatabasesError(db, v1TableName)
        case _ => ns.map(_.head)
      }
      ShowColumnsCommand(db, v1TableName)

    case AlterTableRecoverPartitions(ResolvedV1TableIdentifier(ident)) =>
      AlterTableRecoverPartitionsCommand(
        ident.asTableIdentifier,
        "ALTER TABLE RECOVER PARTITIONS")

    case AlterTableAddPartition(ResolvedV1TableIdentifier(ident), partSpecsAndLocs, ifNotExists) =>
      AlterTableAddPartitionCommand(
        ident.asTableIdentifier,
        partSpecsAndLocs.asUnresolvedPartitionSpecs.map(spec => (spec.spec, spec.location)),
        ifNotExists)

    case AlterTableRenamePartition(
        ResolvedV1TableIdentifier(ident),
        UnresolvedPartitionSpec(from, _),
        UnresolvedPartitionSpec(to, _)) =>
      AlterTableRenamePartitionCommand(ident.asTableIdentifier, from, to)

    case AlterTableDropPartition(
        ResolvedV1TableIdentifier(ident), specs, ifExists, purge) =>
      AlterTableDropPartitionCommand(
        ident.asTableIdentifier,
        specs.asUnresolvedPartitionSpecs.map(_.spec),
        ifExists,
        purge,
        retainData = false)

    case AlterTableSerDeProperties(
        ResolvedV1TableIdentifier(ident),
        serdeClassName,
        serdeProperties,
        partitionSpec) =>
      AlterTableSerDePropertiesCommand(
        ident.asTableIdentifier,
        serdeClassName,
        serdeProperties,
        partitionSpec)

    case AlterViewAs(ResolvedView(ident, _), originalText, query) =>
      AlterViewAsCommand(
        ident.asTableIdentifier,
        originalText,
        query)

    case CreateViewStatement(
      tbl, userSpecifiedColumns, comment, properties,
      originalText, child, allowExisting, replace, viewType) =>

      val v1TableName = if (viewType != PersistedView) {
        // temp view doesn't belong to any catalog and we shouldn't resolve catalog in the name.
        tbl
      } else {
        parseV1Table(tbl, "CREATE VIEW")
      }
      CreateViewCommand(
        v1TableName.asTableIdentifier,
        userSpecifiedColumns,
        comment,
        properties,
        originalText,
        child,
        allowExisting,
        replace,
        viewType)

    case ShowViews(resolved: ResolvedNamespace, pattern) =>
      resolved match {
        case DatabaseInSessionCatalog(db) => ShowViewsCommand(db, pattern)
        case _ =>
          throw QueryCompilationErrors.externalCatalogNotSupportShowViewsError(resolved)
      }

    case ShowTableProperties(ResolvedV1TableOrViewIdentifier(ident), propertyKey) =>
      ShowTablePropertiesCommand(ident.asTableIdentifier, propertyKey)

    case DescribeFunction(ResolvedFunc(identifier), extended) =>
      DescribeFunctionCommand(identifier.asFunctionIdentifier, extended)

    case ShowFunctions(None, userScope, systemScope, pattern) =>
      ShowFunctionsCommand(None, pattern, userScope, systemScope)

    case ShowFunctions(Some(ResolvedFunc(identifier)), userScope, systemScope, _) =>
      val funcIdentifier = identifier.asFunctionIdentifier
      ShowFunctionsCommand(
        funcIdentifier.database, Some(funcIdentifier.funcName), userScope, systemScope)

    case DropFunction(ResolvedFunc(identifier), ifExists, isTemp) =>
      val funcIdentifier = identifier.asFunctionIdentifier
      DropFunctionCommand(funcIdentifier.database, funcIdentifier.funcName, ifExists, isTemp)

    case CreateFunctionStatement(nameParts,
      className, resources, isTemp, ignoreIfExists, replace) =>
      if (isTemp) {
        // temp func doesn't belong to any catalog and we shouldn't resolve catalog in the name.
        val database = if (nameParts.length > 2) {
          throw QueryCompilationErrors.unsupportedFunctionNameError(nameParts.quoted)
        } else if (nameParts.length == 2) {
          Some(nameParts.head)
        } else {
          None
        }
        CreateFunctionCommand(
          database,
          nameParts.last,
          className,
          resources,
          isTemp,
          ignoreIfExists,
          replace)
      } else {
        val FunctionIdentifier(function, database) =
          parseSessionCatalogFunctionIdentifier(nameParts)
        CreateFunctionCommand(database, function, className, resources, isTemp, ignoreIfExists,
          replace)
      }

    case RefreshFunction(ResolvedFunc(identifier)) =>
      // Fallback to v1 command
      val funcIdentifier = identifier.asFunctionIdentifier
      RefreshFunctionCommand(funcIdentifier.database, funcIdentifier.funcName)
  }

  private def parseV1Table(tableName: Seq[String], sql: String): Seq[String] = tableName match {
    case SessionCatalogAndTable(_, tbl) => tbl
    case _ => throw QueryCompilationErrors.sqlOnlySupportedWithV1TablesError(sql)
  }

  private def getStorageFormatAndProvider(
      provider: Option[String],
      options: Map[String, String],
      location: Option[String],
      maybeSerdeInfo: Option[SerdeInfo],
      ctas: Boolean): (CatalogStorageFormat, String) = {
    val nonHiveStorageFormat = CatalogStorageFormat.empty.copy(
      locationUri = location.map(CatalogUtils.stringToURI),
      properties = options)
    val defaultHiveStorage = HiveSerDe.getDefaultStorage(conf).copy(
      locationUri = location.map(CatalogUtils.stringToURI),
      properties = options)

    if (provider.isDefined) {
      // The parser guarantees that USING and STORED AS/ROW FORMAT won't co-exist.
      if (maybeSerdeInfo.isDefined) {
        throw QueryCompilationErrors.cannotCreateTableWithBothProviderAndSerdeError(
          provider, maybeSerdeInfo)
      }
      (nonHiveStorageFormat, provider.get)
    } else if (maybeSerdeInfo.isDefined) {
      val serdeInfo = maybeSerdeInfo.get
      SerdeInfo.checkSerdePropMerging(serdeInfo.serdeProperties, defaultHiveStorage.properties)
      val storageFormat = if (serdeInfo.storedAs.isDefined) {
        // If `STORED AS fileFormat` is used, infer inputFormat, outputFormat and serde from it.
        HiveSerDe.sourceToSerDe(serdeInfo.storedAs.get) match {
          case Some(hiveSerde) =>
            defaultHiveStorage.copy(
              inputFormat = hiveSerde.inputFormat.orElse(defaultHiveStorage.inputFormat),
              outputFormat = hiveSerde.outputFormat.orElse(defaultHiveStorage.outputFormat),
              // User specified serde takes precedence over the one inferred from file format.
              serde = serdeInfo.serde.orElse(hiveSerde.serde).orElse(defaultHiveStorage.serde),
              properties = serdeInfo.serdeProperties ++ defaultHiveStorage.properties)
          case _ => throw QueryCompilationErrors.invalidFileFormatForStoredAsError(serdeInfo)
        }
      } else {
        defaultHiveStorage.copy(
          inputFormat =
            serdeInfo.formatClasses.map(_.input).orElse(defaultHiveStorage.inputFormat),
          outputFormat =
            serdeInfo.formatClasses.map(_.output).orElse(defaultHiveStorage.outputFormat),
          serde = serdeInfo.serde.orElse(defaultHiveStorage.serde),
          properties = serdeInfo.serdeProperties ++ defaultHiveStorage.properties)
      }
      (storageFormat, DDLUtils.HIVE_PROVIDER)
    } else {
      // If neither USING nor STORED AS/ROW FORMAT is specified, we create native data source
      // tables if:
      //   1. `LEGACY_CREATE_HIVE_TABLE_BY_DEFAULT` is false, or
      //   2. It's a CTAS and `conf.convertCTAS` is true.
      val createHiveTableByDefault = conf.getConf(SQLConf.LEGACY_CREATE_HIVE_TABLE_BY_DEFAULT)
      if (!createHiveTableByDefault || (ctas && conf.convertCTAS)) {
        (nonHiveStorageFormat, conf.defaultDataSourceName)
      } else {
        logWarning("A Hive serde table will be created as there is no table provider " +
          s"specified. You can set ${SQLConf.LEGACY_CREATE_HIVE_TABLE_BY_DEFAULT.key} to false " +
          "so that native data source table will be created instead.")
        (defaultHiveStorage, DDLUtils.HIVE_PROVIDER)
      }
    }
  }

  private def buildCatalogTable(
      table: TableIdentifier,
      schema: StructType,
      partitioning: Seq[Transform],
      bucketSpec: Option[BucketSpec],
      properties: Map[String, String],
      provider: String,
      location: Option[String],
      comment: Option[String],
      storageFormat: CatalogStorageFormat,
      external: Boolean): CatalogTable = {
    val tableType = if (external || location.isDefined) {
      CatalogTableType.EXTERNAL
    } else {
      CatalogTableType.MANAGED
    }

    CatalogTable(
      identifier = table,
      tableType = tableType,
      storage = storageFormat,
      schema = schema,
      provider = Some(provider),
      partitionColumnNames = partitioning.asPartitionColumns,
      bucketSpec = bucketSpec,
      properties = properties,
      comment = comment)
  }

  object SessionCatalogAndTable {
    def unapply(nameParts: Seq[String]): Option[(CatalogPlugin, Seq[String])] = nameParts match {
      case SessionCatalogAndIdentifier(catalog, ident) =>
        Some(catalog -> ident.asMultipartIdentifier)
      case _ => None
    }
  }

  object ResolvedViewIdentifier {
    def unapply(resolved: LogicalPlan): Option[Identifier] = resolved match {
      case ResolvedView(ident, _) => Some(ident)
      case _ => None
    }
  }

  object ResolvedV1TableIdentifier {
    def unapply(resolved: LogicalPlan): Option[Identifier] = resolved match {
      case ResolvedTable(catalog, ident, _: V1Table, _) if isSessionCatalog(catalog) => Some(ident)
      case _ => None
    }
  }

  object ResolvedV1TableOrViewIdentifier {
    def unapply(resolved: LogicalPlan): Option[Identifier] = resolved match {
      case ResolvedV1TableIdentifier(ident) => Some(ident)
      case ResolvedViewIdentifier(ident) => Some(ident)
      case _ => None
    }
  }

  private def assertTopLevelColumn(colName: Seq[String], command: String): Unit = {
    if (colName.length > 1) {
      throw QueryCompilationErrors.commandNotSupportNestedColumnError(command, colName.quoted)
    }
  }

  private def convertToStructField(col: QualifiedColType): StructField = {
    val builder = new MetadataBuilder
    col.comment.foreach(builder.putString("comment", _))
    StructField(col.name.head, col.dataType, nullable = true, builder.build())
  }

  private def isV2Provider(provider: String): Boolean = {
    // Return earlier since `lookupDataSourceV2` may fail to resolve provider "hive" to
    // `HiveFileFormat`, when running tests in sql/core.
    if (DDLUtils.isHiveTable(Some(provider))) return false
    DataSource.lookupDataSourceV2(provider, conf) match {
      // TODO(SPARK-28396): Currently file source v2 can't work with tables.
      case Some(_: FileDataSourceV2) => false
      case Some(_) => true
      case _ => false
    }
  }

  private object DatabaseInSessionCatalog {
    def unapply(resolved: ResolvedNamespace): Option[String] = resolved match {
      case ResolvedNamespace(catalog, _) if !isSessionCatalog(catalog) => None
      case ResolvedNamespace(_, Seq()) =>
        throw QueryCompilationErrors.databaseFromV1SessionCatalogNotSpecifiedError()
      case ResolvedNamespace(_, Seq(dbName)) => Some(dbName)
      case _ =>
        assert(resolved.namespace.length > 1)
        throw QueryCompilationErrors.nestedDatabaseUnsupportedByV1SessionCatalogError(
          resolved.namespace.map(quoteIfNeeded).mkString("."))
    }
  }
}
