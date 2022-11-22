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

import org.apache.commons.lang3.StringUtils

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.{FunctionIdentifier, TableIdentifier}
import org.apache.spark.sql.catalyst.catalog.{CatalogStorageFormat, CatalogTable, CatalogTableType, CatalogUtils}
import org.apache.spark.sql.catalyst.expressions.{Alias, Attribute}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.catalyst.util.{quoteIfNeeded, toPrettySQL, ResolveDefaultColumns => DefaultCols}
import org.apache.spark.sql.catalyst.util.ResolveDefaultColumns._
import org.apache.spark.sql.connector.catalog.{CatalogManager, CatalogV2Util, Identifier, LookupCatalog, SupportsNamespaces, Table, TableCatalog, V1Table}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.errors.{QueryCompilationErrors, QueryExecutionErrors}
import org.apache.spark.sql.execution.command._
import org.apache.spark.sql.execution.datasources.{CreateTable => CreateTableV1, DataSource}
import org.apache.spark.sql.execution.datasources.v2.{FileDataSourceV2, FileTable}
import org.apache.spark.sql.internal.{HiveSerDe, SQLConf}
import org.apache.spark.sql.internal.connector.V1Function
import org.apache.spark.sql.types.{MetadataBuilder, StructField, StructType}

/**
 * Converts resolved v2 commands to v1 if the catalog is the session catalog. Since the v2 commands
 * are resolved, the referred tables/views/functions are resolved as well. This rule uses qualified
 * identifiers to construct the v1 commands, so that v1 commands do not need to qualify identifiers
 * again, which may lead to inconsistent behavior if the current database is changed in the middle.
 */
class ResolveSessionCatalog(val catalogManager: CatalogManager)
  extends Rule[LogicalPlan] with LookupCatalog {
  import org.apache.spark.sql.connector.catalog.CatalogV2Implicits._
  import org.apache.spark.sql.connector.catalog.CatalogV2Util._
  import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Implicits._

  override def apply(plan: LogicalPlan): LogicalPlan = plan.resolveOperatorsUp {
    case AddColumns(ResolvedV1TableOrV2FileTableIdentifier(ident), cols) =>
      cols.foreach { c =>
        if (c.name.length > 1) {
          throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
            Seq(ident.catalog.get, ident.database.get, ident.table),
            "ADD COLUMN with qualified column")
        }
        if (!c.nullable) {
          throw QueryCompilationErrors.addColumnWithV1TableCannotSpecifyNotNullError
        }
      }
      AlterTableAddColumnsCommand(ident, cols.map(convertToStructField))

    case ReplaceColumns(ResolvedV1TableOrV2FileTableIdentifier(ident), _) =>
      throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
        Seq(ident.catalog.get, ident.database.get, ident.table),
        "REPLACE COLUMNS")

    case a @ AlterColumn(ResolvedCatalogTable(catalog, ident, table, catalogTable),
        _, _, _, _, _, _)
        if isSessionCatalog(catalog) =>
      if (a.column.name.length > 1) {
        throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
          Seq(catalog.name, ident.namespace()(0), ident.name),
          "ALTER COLUMN with qualified column")
      }
      if (a.nullable.isDefined) {
        throw QueryCompilationErrors.alterColumnWithV1TableCannotSpecifyNotNullError
      }
      if (a.position.isDefined) {
        throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
          Seq(catalog.name, ident.namespace()(0), ident.name),
          "ALTER COLUMN ... FIRST | ALTER")
      }
      val builder = new MetadataBuilder
      // Add comment to metadata
      a.comment.map(c => builder.putString("comment", c))
      val colName = a.column.name(0)
      val dataType = a.dataType.getOrElse {
        table.schema.findNestedField(Seq(colName), resolver = conf.resolver)
          .map(_._2.dataType)
          .getOrElse {
            throw QueryCompilationErrors.alterColumnCannotFindColumnInV1TableError(
              quoteIfNeeded(colName), table.schema)
          }
      }
      // Add the current default column value string (if any) to the column metadata.
      a.setDefaultExpression.map { c => builder.putString(CURRENT_DEFAULT_COLUMN_METADATA_KEY, c) }
      val newColumn = StructField(
        colName,
        dataType,
        nullable = true,
        builder.build())
      AlterTableChangeColumnCommand(catalogTable.identifier, colName, newColumn)

    case RenameColumn(ResolvedV1TableOrV2FileTableIdentifier(ident), _, _) =>
      throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
        Seq(ident.catalog.get, ident.database.get, ident.table),
        "RENAME COLUMN")

    case DropColumns(ResolvedV1TableOrV2FileTableIdentifier(ident), _, _) =>
      throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
        Seq(ident.catalog.get, ident.database.get, ident.table),
        "DROP COLUMN")

    case SetTableProperties(ResolvedV1TableOrV2FileTableIdentifier(ident), props) =>
      AlterTableSetPropertiesCommand(ident, props, isView = false)

    case UnsetTableProperties(ResolvedV1TableOrV2FileTableIdentifier(ident), keys, ifExists) =>
      AlterTableUnsetPropertiesCommand(ident, keys, ifExists, isView = false)

    case SetViewProperties(ResolvedViewIdentifier(ident), props) =>
      AlterTableSetPropertiesCommand(ident, props, isView = true)

    case UnsetViewProperties(ResolvedViewIdentifier(ident), keys, ifExists) =>
      AlterTableUnsetPropertiesCommand(ident, keys, ifExists, isView = true)

    case DescribeNamespace(DatabaseInSessionCatalog(db), extended, output) if conf.useV1Command =>
      DescribeDatabaseCommand(db, extended, output)

    case SetNamespaceProperties(DatabaseInSessionCatalog(db), properties) if conf.useV1Command =>
      AlterDatabasePropertiesCommand(db, properties)

    case SetNamespaceLocation(DatabaseInSessionCatalog(db), location) if conf.useV1Command =>
      if (StringUtils.isEmpty(location)) {
        throw QueryExecutionErrors.invalidEmptyLocationError(location)
      }
      AlterDatabaseSetLocationCommand(db, location)

    case RenameTable(ResolvedV1TableOrV2FileTableOrViewIdentifier(oldIdent), newName, isView) =>
      AlterTableRenameCommand(oldIdent, newName.asTableIdentifier, isView)

    // Use v1 command to describe (temp) view, as v2 catalog doesn't support view yet.
    case DescribeRelation(
         ResolvedV1TableOrV2FileTableOrViewIdentifier(ident), partitionSpec, isExtended, output) =>
      DescribeTableCommand(ident, partitionSpec, isExtended, output)

    case DescribeColumn(
         ResolvedViewIdentifier(ident), column: UnresolvedAttribute, isExtended, output) =>
      // For views, the column will not be resolved by `ResolveReferences` because
      // `ResolvedView` stores only the identifier.
      DescribeColumnCommand(ident, column.nameParts, isExtended, output)

    case DescribeColumn(ResolvedV1TableOrV2FileTableIdentifier(ident), column, isExtended,
        output) =>
      column match {
        case u: UnresolvedAttribute =>
          throw QueryCompilationErrors.columnNotFoundError(u.name)
        case a: Attribute =>
          DescribeColumnCommand(ident, a.qualifier :+ a.name, isExtended, output)
        case Alias(child, _) =>
          throw QueryCompilationErrors.commandNotSupportNestedColumnError(
            "DESC TABLE COLUMN", toPrettySQL(child))
        case _ =>
          throw new IllegalStateException(s"[BUG] unexpected column expression: $column")
      }

    // For CREATE TABLE [AS SELECT], we should use the v1 command if the catalog is resolved to the
    // session catalog and the table provider is not v2.
    case c @ CreateTable(ResolvedV1Identifier(ident), _, _, _, _) =>
      val (storageFormat, provider) = getStorageFormatAndProvider(
        c.tableSpec.provider, c.tableSpec.options, c.tableSpec.location, c.tableSpec.serde,
        ctas = false)
      if (!isV2Provider(provider)) {
        constructV1TableCmd(None, c.tableSpec, ident, c.tableSchema, c.partitioning,
          c.ignoreIfExists, storageFormat, provider)
      } else {
        c
      }

    case c @ CreateTableAsSelect(ResolvedV1Identifier(ident), _, _, _, writeOptions, _, _) =>
      val (storageFormat, provider) = getStorageFormatAndProvider(
        c.tableSpec.provider,
        c.tableSpec.options ++ writeOptions,
        c.tableSpec.location,
        c.tableSpec.serde,
        ctas = true)

      if (!isV2Provider(provider)) {
        constructV1TableCmd(Some(c.query), c.tableSpec, ident, new StructType, c.partitioning,
          c.ignoreIfExists, storageFormat, provider)
      } else {
        c
      }

    case RefreshTable(ResolvedV1TableOrV2FileTableIdentifier(ident)) =>
      RefreshTableCommand(ident)

    case RefreshTable(ResolvedViewIdentifier(ident)) =>
      RefreshTableCommand(ident)

    // For REPLACE TABLE [AS SELECT], we should fail if the catalog is resolved to the
    // session catalog and the table provider is not v2.
    case c @ ReplaceTable(ResolvedV1Identifier(ident), _, _, _, _) =>
      val provider = c.tableSpec.provider.getOrElse(conf.defaultDataSourceName)
      if (!isV2Provider(provider)) {
        throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
          Seq(ident.catalog.get, ident.database.get, ident.table),
          "REPLACE TABLE")
      } else {
        c
      }

    case c @ ReplaceTableAsSelect(ResolvedV1Identifier(ident), _, _, _, _, _, _) =>
      val provider = c.tableSpec.provider.getOrElse(conf.defaultDataSourceName)
      if (!isV2Provider(provider)) {
        throw QueryCompilationErrors.operationOnlySupportedWithV2TableError(
          Seq(ident.catalog.get, ident.database.get, ident.table),
          "REPLACE TABLE AS SELECT")
      } else {
        c
      }

    case DropTable(ResolvedV1Identifier(ident), ifExists, purge) =>
      DropTableCommand(ident, ifExists, isView = false, purge = purge)

    // v1 DROP TABLE supports temp view.
    case DropTable(ResolvedIdentifier(FakeSystemCatalog, ident), _, _) =>
      DropTempViewCommand(ident)

    case DropView(ResolvedV1Identifier(ident), ifExists) =>
      DropTableCommand(ident, ifExists, isView = true, purge = false)

    case DropView(r @ ResolvedIdentifier(catalog, ident), _) =>
      if (catalog == FakeSystemCatalog) {
        DropTempViewCommand(ident)
      } else {
        throw QueryCompilationErrors.catalogOperationNotSupported(catalog, "views")
      }

    case c @ CreateNamespace(DatabaseNameInSessionCatalog(name), _, _) if conf.useV1Command =>
      val comment = c.properties.get(SupportsNamespaces.PROP_COMMENT)
      val location = c.properties.get(SupportsNamespaces.PROP_LOCATION)
      val newProperties = c.properties -- CatalogV2Util.NAMESPACE_RESERVED_PROPERTIES
      if (location.isDefined && location.get.isEmpty) {
        throw QueryExecutionErrors.invalidEmptyLocationError(location.get)
      }
      CreateDatabaseCommand(name, c.ifNotExists, location, comment, newProperties)

    case d @ DropNamespace(DatabaseInSessionCatalog(db), _, _) if conf.useV1Command =>
      DropDatabaseCommand(db, d.ifExists, d.cascade)

    case ShowTables(DatabaseInSessionCatalog(db), pattern, output) if conf.useV1Command =>
      ShowTablesCommand(Some(db), pattern, output)

    case ShowTableExtended(
        DatabaseInSessionCatalog(db),
        pattern,
        partitionSpec @ (None | Some(UnresolvedPartitionSpec(_, _))),
        output) =>
      val newOutput = if (conf.getConf(SQLConf.LEGACY_KEEP_COMMAND_OUTPUT_SCHEMA)) {
        assert(output.length == 4)
        output.head.withName("database") +: output.tail
      } else {
        output
      }
      val tablePartitionSpec = partitionSpec.map(_.asInstanceOf[UnresolvedPartitionSpec].spec)
      ShowTablesCommand(Some(db), Some(pattern), newOutput, true, tablePartitionSpec)

    // ANALYZE TABLE works on permanent views if the views are cached.
    case AnalyzeTable(ResolvedV1TableOrV2FileTableOrViewIdentifier(ident), partitionSpec, noScan) =>
      if (partitionSpec.isEmpty) {
        AnalyzeTableCommand(ident, noScan)
      } else {
        AnalyzePartitionCommand(ident, partitionSpec, noScan)
      }

    case AnalyzeTables(DatabaseInSessionCatalog(db), noScan) =>
      AnalyzeTablesCommand(Some(db), noScan)

    case AnalyzeColumn(ResolvedV1TableOrV2FileTableOrViewIdentifier(ident), columnNames,
        allColumns) =>
      AnalyzeColumnCommand(ident, columnNames, allColumns)

    case RepairTable(ResolvedV1TableOrV2FileTableIdentifier(ident), addPartitions,
        dropPartitions) =>
      RepairTableCommand(ident, addPartitions, dropPartitions)

    case LoadData(ResolvedV1TableOrV2FileTableIdentifier(ident), path, isLocal, isOverwrite,
        partition) =>
      LoadDataCommand(
        ident,
        path,
        isLocal,
        isOverwrite,
        partition)

    case ShowCreateTable(ResolvedV1TableOrV2FileTableOrViewIdentifier(ident), asSerde, output)
      if asSerde =>
      ShowCreateTableAsSerdeCommand(ident, output)

    // If target is view, force use v1 command
    case ShowCreateTable(ResolvedViewIdentifier(ident), _, output) =>
      ShowCreateTableCommand(ident, output)

    case ShowCreateTable(ResolvedV1TableOrV2FileTableIdentifier(ident), _, output)
      if conf.useV1Command => ShowCreateTableCommand(ident, output)

    case ShowCreateTable(ResolvedCatalogTable(catalog, _, _, catalogTable), _, output)
        if isSessionCatalog(catalog) && DDLUtils.isHiveTable(catalogTable) =>
      ShowCreateTableCommand(catalogTable.identifier, output)

    case TruncateTable(ResolvedV1TableOrV2FileTableIdentifier(ident)) =>
      TruncateTableCommand(ident, None)

    case TruncatePartition(ResolvedV1TableOrV2FileTableIdentifier(ident), partitionSpec) =>
      TruncateTableCommand(
        ident,
        Seq(partitionSpec).asUnresolvedPartitionSpecs.map(_.spec).headOption)

    case ShowPartitions(
        ResolvedV1TableOrV2FileTableOrViewIdentifier(ident),
        pattern @ (None | Some(UnresolvedPartitionSpec(_, _))), output) =>
      ShowPartitionsCommand(
        ident,
        output,
        pattern.map(_.asInstanceOf[UnresolvedPartitionSpec].spec))

    case ShowColumns(ResolvedV1TableOrV2FileTableOrViewIdentifier(ident), ns, output) =>
      val v1TableName = ident
      val resolver = conf.resolver
      val db = ns match {
        case Some(db) if v1TableName.database.exists(!resolver(_, db.head)) =>
          throw QueryCompilationErrors.showColumnsWithConflictDatabasesError(db, v1TableName)
        case _ => ns.map(_.head)
      }
      ShowColumnsCommand(db, v1TableName, output)

    case RecoverPartitions(ResolvedV1TableOrV2FileTableIdentifier(ident)) =>
      RepairTableCommand(
        ident,
        enableAddPartitions = true,
        enableDropPartitions = false,
        "ALTER TABLE RECOVER PARTITIONS")

    case AddPartitions(ResolvedV1TableOrV2FileTableIdentifier(ident), partSpecsAndLocs,
        ifNotExists) =>
      AlterTableAddPartitionCommand(
        ident,
        partSpecsAndLocs.asUnresolvedPartitionSpecs.map(spec => (spec.spec, spec.location)),
        ifNotExists)

    case RenamePartitions(
        ResolvedV1TableOrV2FileTableIdentifier(ident),
        UnresolvedPartitionSpec(from, _),
        UnresolvedPartitionSpec(to, _)) =>
      AlterTableRenamePartitionCommand(ident, from, to)

    case DropPartitions(
        ResolvedV1TableOrV2FileTableIdentifier(ident), specs, ifExists, purge) =>
      AlterTableDropPartitionCommand(
        ident,
        specs.asUnresolvedPartitionSpecs.map(_.spec),
        ifExists,
        purge,
        retainData = false)

    case SetTableSerDeProperties(
        ResolvedV1TableOrV2FileTableIdentifier(ident),
        serdeClassName,
        serdeProperties,
        partitionSpec) =>
      AlterTableSerDePropertiesCommand(
        ident,
        serdeClassName,
        serdeProperties,
        partitionSpec)

    case SetTableLocation(ResolvedV1TableOrV2FileTableIdentifier(ident), partitionSpec, location) =>
      AlterTableSetLocationCommand(ident, partitionSpec, location)

    case AlterViewAs(ResolvedViewIdentifier(ident), originalText, query) =>
      AlterViewAsCommand(ident, originalText, query)

    case CreateView(ResolvedV1Identifier(ident), userSpecifiedColumns, comment,
        properties, originalText, child, allowExisting, replace) =>
      CreateViewCommand(
        name = ident,
        userSpecifiedColumns = userSpecifiedColumns,
        comment = comment,
        properties = properties,
        originalText = originalText,
        plan = child,
        allowExisting = allowExisting,
        replace = replace,
        viewType = PersistedView)

    case CreateView(ResolvedIdentifier(catalog, _), _, _, _, _, _, _, _) =>
      throw QueryCompilationErrors.missingCatalogAbilityError(catalog, "views")

    case ShowViews(ns: ResolvedNamespace, pattern, output) =>
      ns match {
        case DatabaseInSessionCatalog(db) => ShowViewsCommand(db, pattern, output)
        case _ =>
          throw QueryCompilationErrors.missingCatalogAbilityError(ns.catalog, "views")
      }

    // If target is view, force use v1 command
    case ShowTableProperties(ResolvedViewIdentifier(ident), propertyKey, output) =>
      ShowTablePropertiesCommand(ident, propertyKey, output)

    case ShowTableProperties(ResolvedV1TableOrV2FileTableIdentifier(ident), propertyKey, output)
        if conf.useV1Command =>
      ShowTablePropertiesCommand(ident, propertyKey, output)

    case DescribeFunction(ResolvedNonPersistentFunc(_, V1Function(info)), extended) =>
      DescribeFunctionCommand(info, extended)

    case DescribeFunction(ResolvedPersistentFunc(catalog, _, func), extended) =>
      if (isSessionCatalog(catalog)) {
        DescribeFunctionCommand(func.asInstanceOf[V1Function].info, extended)
      } else {
        throw QueryCompilationErrors.missingCatalogAbilityError(catalog, "functions")
      }

    case ShowFunctions(DatabaseInSessionCatalog(db), userScope, systemScope, pattern, output) =>
      ShowFunctionsCommand(db, pattern, userScope, systemScope, output)

    case DropFunction(ResolvedPersistentFunc(catalog, identifier, _), ifExists) =>
      if (isSessionCatalog(catalog)) {
        val funcIdentifier = catalogManager.v1SessionCatalog.qualifyIdentifier(
          identifier.asFunctionIdentifier)
        DropFunctionCommand(funcIdentifier, ifExists, false)
      } else {
        throw QueryCompilationErrors.missingCatalogAbilityError(catalog, "DROP FUNCTION")
      }

    case RefreshFunction(ResolvedPersistentFunc(catalog, identifier, _)) =>
      if (isSessionCatalog(catalog)) {
        val funcIdentifier = catalogManager.v1SessionCatalog.qualifyIdentifier(
          identifier.asFunctionIdentifier)
        RefreshFunctionCommand(funcIdentifier.database, funcIdentifier.funcName)
      } else {
        throw QueryCompilationErrors.missingCatalogAbilityError(catalog, "REFRESH FUNCTION")
      }

    case CreateFunction(ResolvedV1Identifier(ident), className, resources, ifExists, replace) =>
      CreateFunctionCommand(
        FunctionIdentifier(ident.table, ident.database, ident.catalog),
        className,
        resources,
        false,
        ifExists,
        replace)

    case CreateFunction(ResolvedIdentifier(catalog, _), _, _, _, _) =>
      throw QueryCompilationErrors.missingCatalogAbilityError(catalog, "CREATE FUNCTION")
  }

  private def constructV1TableCmd(
      query: Option[LogicalPlan],
      tableSpec: TableSpec,
      ident: TableIdentifier,
      tableSchema: StructType,
      partitioning: Seq[Transform],
      ignoreIfExists: Boolean,
      storageFormat: CatalogStorageFormat,
      provider: String): CreateTableV1 = {
    val tableDesc = buildCatalogTable(
      ident, tableSchema, partitioning, tableSpec.properties, provider,
      tableSpec.location, tableSpec.comment, storageFormat, tableSpec.external)
    val mode = if (ignoreIfExists) SaveMode.Ignore else SaveMode.ErrorIfExists
    CreateTableV1(tableDesc, mode, query)
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
    val (partitionColumns, maybeBucketSpec) = partitioning.convertTransforms

    CatalogTable(
      identifier = table,
      tableType = tableType,
      storage = storageFormat,
      schema = schema,
      provider = Some(provider),
      partitionColumnNames = partitionColumns,
      bucketSpec = maybeBucketSpec,
      properties = properties,
      comment = comment)
  }

  object ResolvedViewIdentifier {
    def unapply(resolved: LogicalPlan): Option[TableIdentifier] = resolved match {
      case ResolvedPersistentView(catalog, ident, _) =>
        assert(isSessionCatalog(catalog))
        assert(ident.namespace().length == 1)
        Some(TableIdentifier(ident.name, Some(ident.namespace.head), Some(catalog.name)))

      case ResolvedTempView(ident, _) => Some(ident.asTableIdentifier)

      case _ => None
    }
  }

  object ResolvedCatalogTable {
    def unapply(resolved: LogicalPlan): Option[(TableCatalog, Identifier, Table, CatalogTable)] = {
      resolved match {
        case ResolvedTable(catalog, identifier, t: V1Table, _) if isSessionCatalog(catalog) =>
          Some(catalog, identifier, t, t.catalogTable)
        case ResolvedTable(catalog, identifier, t: FileTable, _)
          if isSessionCatalog(catalog) && t.v1Table.isDefined =>
          Some(catalog, identifier, t, t.v1Table.get)
        case _ => None
      }
    }
  }

  object ResolvedV1TableOrV2FileTableIdentifier {
    def unapply(resolved: LogicalPlan): Option[TableIdentifier] = resolved match {
      case ResolvedTable(catalog, _, t: V1Table, _) if isSessionCatalog(catalog) =>
        Some(t.catalogTable.identifier)
      case ResolvedTable(catalog, _, t: FileTable, _)
        if isSessionCatalog(catalog) && t.v1Table.isDefined =>
        Some(t.v1Table.get.identifier)
      case _ => None
    }
  }

  object ResolvedV1TableOrV2FileTableOrViewIdentifier {
    def unapply(resolved: LogicalPlan): Option[TableIdentifier] = resolved match {
      case ResolvedV1TableOrV2FileTableIdentifier(ident) => Some(ident)
      case ResolvedViewIdentifier(ident) => Some(ident)
      case _ => None
    }
  }

  object ResolvedV1Identifier {
    def unapply(resolved: LogicalPlan): Option[TableIdentifier] = resolved match {
      case ResolvedIdentifier(catalog, ident) if isSessionCatalog(catalog) =>
        if (ident.namespace().length != 1) {
          throw QueryCompilationErrors.requiresSinglePartNamespaceError(ident.namespace())
        }
        Some(TableIdentifier(ident.name, Some(ident.namespace.head), Some(catalog.name)))
      case _ => None
    }
  }

  private def convertToStructField(col: QualifiedColType): StructField = {
    val builder = new MetadataBuilder
    col.comment.foreach(builder.putString("comment", _))
    col.default.map {
      value: String => builder.putString(DefaultCols.CURRENT_DEFAULT_COLUMN_METADATA_KEY, value)
    }
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

  private object DatabaseNameInSessionCatalog {
    def unapply(resolved: ResolvedNamespace): Option[String] = resolved match {
      case ResolvedNamespace(catalog, _) if !isSessionCatalog(catalog) => None
      case ResolvedNamespace(_, Seq(dbName)) => Some(dbName)
      case _ =>
        assert(resolved.namespace.length > 1)
        throw QueryCompilationErrors.invalidDatabaseNameError(resolved.namespace.quoted)
    }
  }
}
