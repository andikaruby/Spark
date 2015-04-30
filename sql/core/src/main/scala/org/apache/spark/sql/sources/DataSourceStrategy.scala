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

package org.apache.spark.sql.sources

import org.apache.hadoop.fs.Path

import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.planning.PhysicalOperation
import org.apache.spark.sql.catalyst.plans.logical
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.types.{StructType, UTF8String, StringType}
import org.apache.spark.sql.{Row, Strategy, execution, sources}

/**
 * A Strategy for planning scans over data sources defined using the sources API.
 */
private[sql] object DataSourceStrategy extends Strategy {
  def apply(plan: LogicalPlan): Seq[execution.SparkPlan] = plan match {
    case PhysicalOperation(projectList, filters, l @ LogicalRelation(t: CatalystScan)) =>
      pruneFilterProjectRaw(
        l,
        projectList,
        filters,
        (a, f) => t.buildScan(a, f)) :: Nil

    case PhysicalOperation(projectList, filters, l @ LogicalRelation(t: PrunedFilteredScan)) =>
      pruneFilterProject(
        l,
        projectList,
        filters,
        (a, f) => t.buildScan(a, f)) :: Nil

    case PhysicalOperation(projectList, filters, l @ LogicalRelation(t: PrunedScan)) =>
      pruneFilterProject(
        l,
        projectList,
        filters,
        (a, _) => t.buildScan(a)) :: Nil

    // Scanning partitioned FSBasedRelation
    case PhysicalOperation(projectList, filters, l @ LogicalRelation(t: FSBasedRelation))
        if t.partitionSpec.partitionColumns.nonEmpty =>
      val selectedPartition = prunePartitions(filters, t.partitionSpec).toArray

      // Only pushes down predicates that do not reference partition columns.
      val pushedFilters = {
        val partitionColumnNames = t.partitionSpec.partitionColumns.map(_.name).toSet
        filters.filter { f =>
          val referencedColumnNames = f.references.map(_.name).toSet
          referencedColumnNames.intersect(partitionColumnNames).isEmpty
        }
      }

      buildPartitionedTableScan(
        l,
        projectList,
        pushedFilters,
        t.partitionSpec.partitionColumns,
        selectedPartition) :: Nil

    // Scanning non-partitioned FSBasedRelation
    case PhysicalOperation(projectList, filters, l @ LogicalRelation(t: FSBasedRelation)) =>
      val inputPaths = t.paths.map(new Path(_)).flatMap { path =>
        val fs = path.getFileSystem(t.sqlContext.sparkContext.hadoopConfiguration)
        val qualifiedPath = fs.makeQualified(path)
        SparkHadoopUtil.get.listLeafStatuses(fs, qualifiedPath).map(_.getPath.toString)
      }

      pruneFilterProject(
        l,
        projectList,
        filters,
        (a, f) => t.buildScan(a, f, inputPaths)) :: Nil

    case l @ LogicalRelation(t: TableScan) =>
      createPhysicalRDD(l.relation, l.output, t.buildScan()) :: Nil

    case i @ logical.InsertIntoTable(
      l @ LogicalRelation(t: InsertableRelation), part, query, overwrite, false) if part.isEmpty =>
      execution.ExecutedCommand(InsertIntoDataSource(l, query, overwrite)) :: Nil

    case _ => Nil
  }

  private def buildPartitionedTableScan(
      logicalRelation: LogicalRelation,
      projections: Seq[NamedExpression],
      filters: Seq[Expression],
      partitionColumns: StructType,
      partitions: Array[Partition]) = {
    val output = projections.map(_.toAttribute)
    val relation = logicalRelation.relation.asInstanceOf[FSBasedRelation]
    val dataSchema = relation.dataSchema

    // Builds RDD[Row]s for each selected partition.
    val perPartitionRows = partitions.map { case Partition(partitionValues, dir) =>
      // Paths to all data files within this partition
      val dataFilePaths = {
        val dirPath = new Path(dir)
        val fs = dirPath.getFileSystem(SparkHadoopUtil.get.conf)
        fs.listStatus(dirPath)
          .map(_.getPath)
          .filter { path =>
            val name = path.getName
            name.startsWith("_") || name.startsWith(".")
          }
          .map(fs.makeQualified(_).toString)
      }

      // The table scan operator (PhysicalRDD) which retrieves required columns from data files.
      // Notice that the schema of data files, represented by `relation.dataSchema`, may contain
      // some partition column(s). Those partition columns that are only encoded in partition
      // directory paths are not covered by this table scan operator.
      val scan =
        pruneFilterProject(
          logicalRelation,
          projections,
          filters,
          (requiredColumns, filters) => {
            // Only columns appear in actual data, which possibly include some partition column(s)
            relation.buildScan(
              requiredColumns.filter(dataSchema.fieldNames.contains),
              filters,
              dataFilePaths)
          })

      // Merges in those partition values that are not contained in data rows.
      mergePartitionValues(output, partitionValues, scan)
    }

    val unionedRows =
      perPartitionRows.reduceOption(_ ++ _).getOrElse(relation.sqlContext.emptyResult)
    createPhysicalRDD(logicalRelation.relation, output, unionedRows)
  }

  private def mergePartitionValues(
      output: Seq[Attribute],
      partitionValues: Row,
      scan: SparkPlan): RDD[Row] = {
    val mergeWithPartitionValues = {
      val outputColNames = output.map(_.name)
      val outputDataColNames = scan.schema.fieldNames

      outputColNames.zipWithIndex.map { case (name, index) =>
        val i = outputDataColNames.indexOf(name)
        if (i > -1) {
          // Column appears in data files, retrieve it from data rows
          (mutableRow: MutableRow, dataRow: expressions.Row, ordinal: Int) => {
            mutableRow(ordinal) = dataRow(i)
          }
        } else {
          // Column doesn't appear in data file (must be a partition column), retrieve it from
          // partition values of this partition.
          (mutableRow: MutableRow, dataRow: expressions.Row, ordinal: Int) => {
            mutableRow(ordinal) = partitionValues(i)
          }
        }
      }
    }

    scan.execute().mapPartitions { iterator =>
      val mutableRow = new SpecificMutableRow(output.map(_.dataType))
      iterator.map { row =>
        var i = 0
        while (i < mutableRow.length) {
          mergeWithPartitionValues(i)(mutableRow, row, i)
          i += 1
        }
        mutableRow.asInstanceOf[expressions.Row]
      }
    }
  }

  protected def prunePartitions(
      predicates: Seq[Expression],
      partitionSpec: PartitionSpec): Seq[Partition] = {
    val PartitionSpec(partitionColumns, partitions) = partitionSpec
    val partitionColumnNames = partitionColumns.map(_.name).toSet
    val partitionPruningPredicates = predicates.filter {
      _.references.map(_.name).toSet.subsetOf(partitionColumnNames)
    }

    if (partitionPruningPredicates.nonEmpty) {
      val predicate =
        partitionPruningPredicates
          .reduceOption(expressions.And)
          .getOrElse(Literal(true))

      val boundPredicate = InterpretedPredicate.create(predicate.transform {
        case a: AttributeReference =>
          val index = partitionColumns.indexWhere(a.name == _.name)
          BoundReference(index, partitionColumns(index).dataType, nullable = true)
      })

      partitions.filter { case Partition(values, _) => boundPredicate(values) }
    } else {
      partitions
    }
  }

  // Based on Public API.
  protected def pruneFilterProject(
      relation: LogicalRelation,
      projectList: Seq[NamedExpression],
      filterPredicates: Seq[Expression],
      scanBuilder: (Array[String], Array[Filter]) => RDD[Row]) = {
    pruneFilterProjectRaw(
      relation,
      projectList,
      filterPredicates,
      (requestedColumns, pushedFilters) => {
        scanBuilder(requestedColumns.map(_.name).toArray, selectFilters(pushedFilters).toArray)
      })
  }

  // Based on Catalyst expressions.
  protected def pruneFilterProjectRaw(
      relation: LogicalRelation,
      projectList: Seq[NamedExpression],
      filterPredicates: Seq[Expression],
      scanBuilder: (Seq[Attribute], Seq[Expression]) => RDD[Row]) = {

    val projectSet = AttributeSet(projectList.flatMap(_.references))
    val filterSet = AttributeSet(filterPredicates.flatMap(_.references))
    val filterCondition = filterPredicates.reduceLeftOption(expressions.And)

    val pushedFilters = filterPredicates.map { _ transform {
      case a: AttributeReference => relation.attributeMap(a) // Match original case of attributes.
    }}

    if (projectList.map(_.toAttribute) == projectList &&
        projectSet.size == projectList.size &&
        filterSet.subsetOf(projectSet)) {
      // When it is possible to just use column pruning to get the right projection and
      // when the columns of this projection are enough to evaluate all filter conditions,
      // just do a scan followed by a filter, with no extra project.
      val requestedColumns =
        projectList.asInstanceOf[Seq[Attribute]] // Safe due to if above.
          .map(relation.attributeMap)            // Match original case of attributes.

      val scan = createPhysicalRDD(relation.relation, projectList.map(_.toAttribute),
          scanBuilder(requestedColumns, pushedFilters))
      filterCondition.map(execution.Filter(_, scan)).getOrElse(scan)
    } else {
      val requestedColumns = (projectSet ++ filterSet).map(relation.attributeMap).toSeq

      val scan = createPhysicalRDD(relation.relation, requestedColumns,
        scanBuilder(requestedColumns, pushedFilters))
      execution.Project(projectList, filterCondition.map(execution.Filter(_, scan)).getOrElse(scan))
    }
  }

  private[this] def createPhysicalRDD(
      relation: BaseRelation,
      output: Seq[Attribute],
      rdd: RDD[Row]): SparkPlan = {
    val converted = if (relation.needConversion) {
      execution.RDDConversions.rowToRowRdd(rdd, relation.schema)
    } else {
      rdd
    }
    execution.PhysicalRDD(output, converted)
  }

  /**
   * Selects Catalyst predicate [[Expression]]s which are convertible into data source [[Filter]]s,
   * and convert them.
   */
  protected[sql] def selectFilters(filters: Seq[Expression]) = {
    def translate(predicate: Expression): Option[Filter] = predicate match {
      case expressions.EqualTo(a: Attribute, Literal(v, _)) =>
        Some(sources.EqualTo(a.name, v))
      case expressions.EqualTo(Literal(v, _), a: Attribute) =>
        Some(sources.EqualTo(a.name, v))

      case expressions.GreaterThan(a: Attribute, Literal(v, _)) =>
        Some(sources.GreaterThan(a.name, v))
      case expressions.GreaterThan(Literal(v, _), a: Attribute) =>
        Some(sources.LessThan(a.name, v))

      case expressions.LessThan(a: Attribute, Literal(v, _)) =>
        Some(sources.LessThan(a.name, v))
      case expressions.LessThan(Literal(v, _), a: Attribute) =>
        Some(sources.GreaterThan(a.name, v))

      case expressions.GreaterThanOrEqual(a: Attribute, Literal(v, _)) =>
        Some(sources.GreaterThanOrEqual(a.name, v))
      case expressions.GreaterThanOrEqual(Literal(v, _), a: Attribute) =>
        Some(sources.LessThanOrEqual(a.name, v))

      case expressions.LessThanOrEqual(a: Attribute, Literal(v, _)) =>
        Some(sources.LessThanOrEqual(a.name, v))
      case expressions.LessThanOrEqual(Literal(v, _), a: Attribute) =>
        Some(sources.GreaterThanOrEqual(a.name, v))

      case expressions.InSet(a: Attribute, set) =>
        Some(sources.In(a.name, set.toArray))

      case expressions.IsNull(a: Attribute) =>
        Some(sources.IsNull(a.name))
      case expressions.IsNotNull(a: Attribute) =>
        Some(sources.IsNotNull(a.name))

      case expressions.And(left, right) =>
        (translate(left) ++ translate(right)).reduceOption(sources.And)

      case expressions.Or(left, right) =>
        for {
          leftFilter <- translate(left)
          rightFilter <- translate(right)
        } yield sources.Or(leftFilter, rightFilter)

      case expressions.Not(child) =>
        translate(child).map(sources.Not)

      case expressions.StartsWith(a: Attribute, Literal(v: UTF8String, StringType)) =>
        Some(sources.StringStartsWith(a.name, v.toString()))

      case expressions.EndsWith(a: Attribute, Literal(v: UTF8String, StringType)) =>
        Some(sources.StringEndsWith(a.name, v.toString()))

      case expressions.Contains(a: Attribute, Literal(v: UTF8String, StringType)) =>
        Some(sources.StringContains(a.name, v.toString()))

      case _ => None
    }

    filters.flatMap(translate)
  }
}
