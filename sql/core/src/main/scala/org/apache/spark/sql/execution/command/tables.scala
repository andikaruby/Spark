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

import java.io.File
import java.net.URI

import scala.collection.mutable.ArrayBuffer

import org.apache.spark.sql.{AnalysisException, Row, SparkSession}
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.catalog._
import org.apache.spark.sql.catalyst.catalog.CatalogTypes.TablePartitionSpec
import org.apache.spark.sql.catalyst.expressions.{Attribute, AttributeReference}
import org.apache.spark.sql.catalyst.parser.CatalystSqlParser
import org.apache.spark.sql.catalyst.plans.logical.{Command, LogicalPlan, UnaryNode}
import org.apache.spark.sql.execution.datasources.PartitioningUtils
import org.apache.spark.sql.types._
import org.apache.spark.util.Utils

case class CreateTableAsSelectLogicalPlan(
  tableDesc: CatalogTable,
  child: LogicalPlan,
  allowExisting: Boolean) extends UnaryNode with Command {

  override def output: Seq[Attribute] = Seq.empty[Attribute]

  override lazy val resolved: Boolean =
    tableDesc.identifier.database.isDefined &&
      tableDesc.schema.nonEmpty &&
      tableDesc.storage.serde.isDefined &&
      tableDesc.storage.inputFormat.isDefined &&
      tableDesc.storage.outputFormat.isDefined &&
      childrenResolved
}

/**
 * A command to create a table with the same definition of the given existing table.
 *
 * The syntax of using this command in SQL is:
 * {{{
 *   CREATE TABLE [IF NOT EXISTS] [db_name.]table_name
 *   LIKE [other_db_name.]existing_table_name
 * }}}
 */
case class CreateTableLike(
    targetTable: TableIdentifier,
    sourceTable: TableIdentifier,
    ifNotExists: Boolean) extends RunnableCommand {

  override def run(sparkSession: SparkSession): Seq[Row] = {
    val catalog = sparkSession.sessionState.catalog
    if (!catalog.tableExists(sourceTable)) {
      throw new AnalysisException(
        s"Source table in CREATE TABLE LIKE does not exist: '$sourceTable'")
    }
    if (catalog.isTemporaryTable(sourceTable)) {
      throw new AnalysisException(
        s"Source table in CREATE TABLE LIKE cannot be temporary: '$sourceTable'")
    }

    val tableToCreate = catalog.getTableMetadata(sourceTable).copy(
      identifier = targetTable,
      tableType = CatalogTableType.MANAGED,
      createTime = System.currentTimeMillis,
      lastAccessTime = -1).withNewStorage(locationUri = None)

    catalog.createTable(tableToCreate, ifNotExists)
    Seq.empty[Row]
  }
}


// TODO: move the rest of the table commands from ddl.scala to this file

/**
 * A command to create a table.
 *
 * Note: This is currently used only for creating Hive tables.
 * This is not intended for temporary tables.
 *
 * The syntax of using this command in SQL is:
 * {{{
 *   CREATE [EXTERNAL] TABLE [IF NOT EXISTS] [db_name.]table_name
 *   [(col1 data_type [COMMENT col_comment], ...)]
 *   [COMMENT table_comment]
 *   [PARTITIONED BY (col3 data_type [COMMENT col_comment], ...)]
 *   [CLUSTERED BY (col1, ...) [SORTED BY (col1 [ASC|DESC], ...)] INTO num_buckets BUCKETS]
 *   [SKEWED BY (col1, col2, ...) ON ((col_value, col_value, ...), ...)
 *   [STORED AS DIRECTORIES]
 *   [ROW FORMAT row_format]
 *   [STORED AS file_format | STORED BY storage_handler_class [WITH SERDEPROPERTIES (...)]]
 *   [LOCATION path]
 *   [TBLPROPERTIES (property_name=property_value, ...)]
 *   [AS select_statement];
 * }}}
 */
case class CreateTable(table: CatalogTable, ifNotExists: Boolean) extends RunnableCommand {

  override def run(sparkSession: SparkSession): Seq[Row] = {
    sparkSession.sessionState.catalog.createTable(table, ifNotExists)
    Seq.empty[Row]
  }

}


/**
 * A command that renames a table/view.
 *
 * The syntax of this command is:
 * {{{
 *    ALTER TABLE table1 RENAME TO table2;
 *    ALTER VIEW view1 RENAME TO view2;
 * }}}
 */
case class AlterTableRename(
    oldName: TableIdentifier,
    newName: TableIdentifier,
    isView: Boolean)
  extends RunnableCommand {

  override def run(sparkSession: SparkSession): Seq[Row] = {
    val catalog = sparkSession.sessionState.catalog
    DDLUtils.verifyAlterTableType(catalog, oldName, isView)
    catalog.invalidateTable(oldName)
    catalog.renameTable(oldName, newName)
    Seq.empty[Row]
  }

}

/**
 * A command that loads data into a Hive table.
 *
 * The syntax of this command is:
 * {{{
 *  LOAD DATA [LOCAL] INPATH 'filepath' [OVERWRITE] INTO TABLE tablename
 *  [PARTITION (partcol1=val1, partcol2=val2 ...)]
 * }}}
 */
case class LoadData(
    table: TableIdentifier,
    path: String,
    isLocal: Boolean,
    isOverwrite: Boolean,
    partition: Option[TablePartitionSpec]) extends RunnableCommand {

  override def run(sparkSession: SparkSession): Seq[Row] = {
    val catalog = sparkSession.sessionState.catalog
    if (!catalog.tableExists(table)) {
      throw new AnalysisException(
        s"Table in LOAD DATA does not exist: '$table'")
    }

    val targetTable = catalog.getTableMetadataOption(table).getOrElse {
      throw new AnalysisException(
        s"Table in LOAD DATA cannot be temporary: '$table'")
    }

    if (DDLUtils.isDatasourceTable(targetTable)) {
      throw new AnalysisException(
        "LOAD DATA is not supported for datasource tables")
    }

    if (targetTable.partitionColumnNames.nonEmpty) {
      if (partition.isEmpty || targetTable.partitionColumnNames.size != partition.get.size) {
        throw new AnalysisException(
          "LOAD DATA to partitioned table must specify a specific partition of " +
          "the table by specifying values for all of the partitioning columns.")
      }

      partition.get.keys.foreach { colName =>
        if (!targetTable.partitionColumnNames.contains(colName)) {
          throw new AnalysisException(
            s"LOAD DATA to partitioned table specifies a non-existing partition column: '$colName'")
        }
      }
    } else {
      if (partition.nonEmpty) {
        throw new AnalysisException(
          "LOAD DATA to non-partitioned table cannot specify partition.")
      }
    }

    val loadPath =
      if (isLocal) {
        val uri = Utils.resolveURI(path)
        if (!new File(uri.getPath()).exists()) {
          throw new AnalysisException(s"LOAD DATA with non-existing path: $path")
        }
        uri
      } else {
        val uri = new URI(path)
        if (uri.getScheme() != null && uri.getAuthority() != null) {
          uri
        } else {
          // Follow Hive's behavior:
          // If no schema or authority is provided with non-local inpath,
          // we will use hadoop configuration "fs.default.name".
          val defaultFSConf = sparkSession.sessionState.newHadoopConf().get("fs.default.name")
          val defaultFS = if (defaultFSConf == null) {
            new URI("")
          } else {
            new URI(defaultFSConf)
          }

          val scheme = if (uri.getScheme() != null) {
            uri.getScheme()
          } else {
            defaultFS.getScheme()
          }
          val authority = if (uri.getAuthority() != null) {
            uri.getAuthority()
          } else {
            defaultFS.getAuthority()
          }

          if (scheme == null) {
            throw new AnalysisException(
              "LOAD DATA with non-local path must specify URI Scheme.")
          }

          // Follow Hive's behavior:
          // If LOCAL is not specified, and the path is relative,
          // then the path is interpreted relative to "/user/<username>"
          val uriPath = uri.getPath()
          val absolutePath = if (uriPath != null && uriPath.startsWith("/")) {
            uriPath
          } else {
            s"/user/${System.getProperty("user.name")}/$uriPath"
          }
          new URI(scheme, authority, absolutePath, uri.getQuery(), uri.getFragment())
        }
      }

    if (partition.nonEmpty) {
      catalog.loadPartition(
        targetTable.identifier,
        loadPath.toString,
        partition.get,
        isOverwrite,
        holdDDLTime = false,
        inheritTableSpecs = true,
        isSkewedStoreAsSubdir = false)
    } else {
      catalog.loadTable(
        targetTable.identifier,
        loadPath.toString,
        isOverwrite,
        holdDDLTime = false)
    }
    Seq.empty[Row]
  }
}

/**
 * A command for users to describe a table in the given database. If a databaseName is not given,
 * the current database will be used.
 * The syntax of using this command in SQL is:
 * {{{
 *   DESCRIBE [EXTENDED|FORMATTED] [db_name.]table_name [column_name] [PARTITION partition_spec]
 * }}}
 * Note : FORMATTED option is not supported.
 * @param table table to be described.
 * @param partSpec spec If specified, the specified partition is described. It is effective only
 *                 when the table is a Hive table
 * @param  colPath If specified, only the specified column is described. It is effective only
 *                 when the table is a Hive table
 * @param isExtended True if "DESCRIBE EXTENDED" is used. Otherwise, false. It is effective only
 *                   when the table is a Hive table
 */
case class DescribeTableCommand(
    table: TableIdentifier,
    partSpec: Option[TablePartitionSpec],
    colPath: Option[String],
    isExtended: Boolean)
  extends RunnableCommand {

  override val output: Seq[Attribute] = Seq(
    // Column names are based on Hive.
    AttributeReference("col_name", StringType, nullable = false,
      new MetadataBuilder().putString("comment", "name of the column").build())(),
    AttributeReference("data_type", StringType, nullable = false,
      new MetadataBuilder().putString("comment", "data type of the column").build())(),
    AttributeReference("comment", StringType, nullable = true,
      new MetadataBuilder().putString("comment", "comment of the column").build())()
  )

  private def formatColumns(cols: Seq[CatalogColumn]): String = {
    cols.map { col =>
      s"""
         |${col.getClass.getSimpleName}
         |(name:${col.name}
         |type:${col.dataType}
         |comment:${col.comment.orNull}
       """.stripMargin
    }.mkString(",")
  }

  private def formatProperties(props: Map[String, String]): String = {
    props.map {
      case (k, v) => s"$k=$v"
    }.mkString("{", ", ", "}")
  }

  private def getPartValues(part: CatalogTablePartition, cols: Seq[String]): String = {
    cols.map { name =>
      PartitioningUtils.escapePathName(part.spec(name))
    }.mkString(", ")
  }

  private def descColPath(table: CatalogTable, colPath: String): Array[Row] = {
    val names = colPath.split("\\.");
    val lastName = names(names.length - 1)
    val fields = table.schema.map {c =>
      StructField(c.name, CatalystSqlParser.parseDataType(c.dataType), c.nullable)
    }
    var dataType: DataType = StructType(fields)
    for (i <- 0 to names.length -1) {
      dataType match {
        case s: StructType =>
          try {
            dataType = s.apply(names(i)).dataType
          } catch {
            case e: Exception =>
              throw new AnalysisException(s"Column name/path: ${colPath} does not exist.")
          }
        case m: MapType if names(i) == "$key$" => dataType = m.keyType
        case m: MapType if names(i) == "$value$" => dataType = m.valueType
        case a: ArrayType if names(i) == "$value$" => dataType = a.elementType
        case _ => throw new AnalysisException("Column name/path: ${colPath} does not exist")
      }
    }

    val result: Seq[Row] = dataType match {
      case s: StructType =>
        s.map { f =>
          Row(f.name, f.dataType.simpleString, "from deserializer")}
      case d: DataType => Seq(Row(lastName, dataType.simpleString, "from deserializer"))
    }
    result.toArray
  }

  private def descStorageFormat(
      table: CatalogTable,
      storage: CatalogStorageFormat): String = {
    // TODO - check with Lian - from StorageDesc - compress, skewedInfo, storedAsSubDirectories
    // are not availble. So these are dropped from the output.
    val storageLocationStr =
      s"""
         |${storage.getClass.getSimpleName}(location:${storage.locationUri.orNull},
         | inputFormat:${storage.inputFormat.orNull},
         | outputFormat:${storage.outputFormat.orNull},
         | numBuckets:${table.numBuckets},
         | serializationLib=${storage.serde.orNull},
         | parameters=${formatProperties(storage.serdeProperties)},
         | bucketCols:[${formatColumns(table.bucketColumns)}],
         | sortCols=[${formatColumns(table.sortColumns)}])
       """.stripMargin.replaceAll("\n", "").trim
    storageLocationStr
  }

  private def descPartExtended(table: CatalogTable, part: CatalogTablePartition): String = {
    val result = StringBuilder.newBuilder
    val clsName = part.getClass.getSimpleName
    result ++= s"${clsName}(values:[${getPartValues(part, table.partitionColumnNames)}], "
    result ++= s"dbName:${table.database}, "
    // TODO - check with Lian - no owner info available.
    result ++= s"createTime:${table.createTime}, "
    result ++= s"lastAccessTime:${table.lastAccessTime}, "
    // TODO - check with Lian - no retention info available.

    result ++= s"sd:${descStorageFormat(table, part.storage)}, "
    // TODO Check with Lian - Hive prints partition keys here. Since we output paritioning keys and
    // schema already at the start i don't output it here again.
    result ++= s"parameters:${formatProperties(table.properties)}, "
    result ++= s"viewOriginalText:${table.viewOriginalText.orNull}, "
    result ++= s"viewExpandedText:${table.viewText.orNull}, "
    result ++= s"tableType:${table.tableType})"
    result.toString
  }

  private def descTableExtended(table: CatalogTable): String = {
    val result = StringBuilder.newBuilder
    result ++= s"${table.getClass.getSimpleName}(tableName:${table.identifier.table}, "
    result ++= s"dbName:${table.database}, "
    // TODO - check with Lian - no owner info available.
    result ++= s"createTime:${table.createTime}, "
    result ++= s"lastAccessTime:${table.lastAccessTime}, "
    // TODO - check with Lian - no retention info available.

    result ++= s"sd:${descStorageFormat(table, table.storage)}, "
    // TODO Check with Lian - Hive prints partition keys here. Since we output paritioning keys
    // and schema already i don't output it here again.
    result ++= s"parameters:${formatProperties(table.properties)}, "
    result ++= s"viewOriginalText:${table.viewOriginalText.orNull}, "
    result ++= s"viewExpandedText:${table.viewText.orNull}, "
    result ++= s"tableType:${table.tableType})"
    result.toString
  }

  override def run(sparkSession: SparkSession): Seq[Row] = {
    val result = new ArrayBuffer[Row]
    val catalog = sparkSession.sessionState.catalog
    catalog.lookupRelation(table) match {
      case catalogRelation: CatalogRelation =>
        val tab = catalogRelation.catalogTable
        val part = partSpec.map(p => Option(catalog.getPartition(table, p))).getOrElse(None)
        if (colPath.nonEmpty) {
           result ++= descColPath(tab, colPath.get)
        } else {
          catalogRelation.catalogTable.schema.foreach { column =>
            result += Row(column.name, column.dataType, column.comment.orNull)
          }
          if (tab.partitionColumns.nonEmpty) {
            result += Row("# Partition Information", "", "")
            result += Row(s"# ${output(0).name}", output(1).name, output(2).name)

            tab.partitionColumns.foreach { col =>
              result += Row(col.name, col.dataType, col.comment.orNull)
            }
          }
          if (isExtended) {
            if (partSpec.isEmpty) {
              result += Row("Detailed Table Information", descTableExtended(tab), "")
            } else {
              result +=
                Row("Detailed Partition Information", descPartExtended(tab, part.get), "")
            }
          }
        }

      case relation =>
        relation.schema.fields.foreach { field =>
          val comment =
            if (field.metadata.contains("comment")) field.metadata.getString("comment") else ""
          result += Row(field.name, field.dataType.simpleString, comment)
        }
    }

    result
  }
}

/**
 * A command for users to get tables in the given database.
 * If a databaseName is not given, the current database will be used.
 * The syntax of using this command in SQL is:
 * {{{
 *   SHOW TABLES [(IN|FROM) database_name] [[LIKE] 'identifier_with_wildcards'];
 * }}}
 */
case class ShowTablesCommand(
    databaseName: Option[String],
    tableIdentifierPattern: Option[String]) extends RunnableCommand {

  // The result of SHOW TABLES has two columns, tableName and isTemporary.
  override val output: Seq[Attribute] = {
    AttributeReference("tableName", StringType, nullable = false)() ::
      AttributeReference("isTemporary", BooleanType, nullable = false)() :: Nil
  }

  override def run(sparkSession: SparkSession): Seq[Row] = {
    // Since we need to return a Seq of rows, we will call getTables directly
    // instead of calling tables in sparkSession.
    val catalog = sparkSession.sessionState.catalog
    val db = databaseName.getOrElse(catalog.getCurrentDatabase)
    val tables =
      tableIdentifierPattern.map(catalog.listTables(db, _)).getOrElse(catalog.listTables(db))
    tables.map { t =>
      val isTemp = t.database.isEmpty
      Row(t.table, isTemp)
    }
  }
}


/**
 * A command for users to list the properties for a table If propertyKey is specified, the value
 * for the propertyKey is returned. If propertyKey is not specified, all the keys and their
 * corresponding values are returned.
 * The syntax of using this command in SQL is:
 * {{{
 *   SHOW TBLPROPERTIES table_name[('propertyKey')];
 * }}}
 */
case class ShowTablePropertiesCommand(table: TableIdentifier, propertyKey: Option[String])
  extends RunnableCommand {

  override val output: Seq[Attribute] = {
    val schema = AttributeReference("value", StringType, nullable = false)() :: Nil
    propertyKey match {
      case None => AttributeReference("key", StringType, nullable = false)() :: schema
      case _ => schema
    }
  }

  override def run(sparkSession: SparkSession): Seq[Row] = {
    val catalog = sparkSession.sessionState.catalog

    if (catalog.isTemporaryTable(table)) {
      Seq.empty[Row]
    } else {
      val catalogTable = sparkSession.sessionState.catalog.getTableMetadata(table)

      propertyKey match {
        case Some(p) =>
          val propValue = catalogTable
            .properties
            .getOrElse(p, s"Table ${catalogTable.qualifiedName} does not have property: $p")
          Seq(Row(propValue))
        case None =>
          catalogTable.properties.map(p => Row(p._1, p._2)).toSeq
      }
    }
  }
}
