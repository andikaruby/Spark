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

package org.apache.spark.sql.execution

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.aggregate2.{_}
import org.apache.spark.sql.catalyst.planning._
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql.catalyst.plans.logical.{BroadcastHint, LogicalPlan}
import org.apache.spark.sql.catalyst.plans.physical._
import org.apache.spark.sql.columnar.{InMemoryColumnarTableScan, InMemoryRelation}
import org.apache.spark.sql.execution.aggregate2.{FinalAndCompleteAggregate2Sort, Aggregate2Sort}
import org.apache.spark.sql.execution.{DescribeCommand => RunnableDescribeCommand}
import org.apache.spark.sql.parquet._
import org.apache.spark.sql.sources.{CreateTableUsing, CreateTempTableUsing, DescribeCommand => LogicalDescribeCommand, _}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SQLContext, Strategy, execution}

private[sql] abstract class SparkStrategies extends QueryPlanner[SparkPlan] {
  self: SQLContext#SparkPlanner =>

  object LeftSemiJoin extends Strategy with PredicateHelper {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case ExtractEquiJoinKeys(LeftSemi, leftKeys, rightKeys, condition, left, right)
        if sqlContext.conf.autoBroadcastJoinThreshold > 0 &&
          right.statistics.sizeInBytes <= sqlContext.conf.autoBroadcastJoinThreshold =>
        joins.BroadcastLeftSemiJoinHash(
          leftKeys, rightKeys, planLater(left), planLater(right), condition) :: Nil
      // Find left semi joins where at least some predicates can be evaluated by matching join keys
      case ExtractEquiJoinKeys(LeftSemi, leftKeys, rightKeys, condition, left, right) =>
        joins.LeftSemiJoinHash(
          leftKeys, rightKeys, planLater(left), planLater(right), condition) :: Nil
      // no predicate can be evaluated by matching hash keys
      case logical.Join(left, right, LeftSemi, condition) =>
        joins.LeftSemiJoinBNL(planLater(left), planLater(right), condition) :: Nil
      case _ => Nil
    }
  }

  /**
   * Matches a plan whose output should be small enough to be used in broadcast join.
   */
  object CanBroadcast {
    def unapply(plan: LogicalPlan): Option[LogicalPlan] = plan match {
      case BroadcastHint(p) => Some(p)
      case p if sqlContext.conf.autoBroadcastJoinThreshold > 0 &&
        p.statistics.sizeInBytes <= sqlContext.conf.autoBroadcastJoinThreshold => Some(p)
      case _ => None
    }
  }

  /**
   * Uses the ExtractEquiJoinKeys pattern to find joins where at least some of the predicates can be
   * evaluated by matching hash keys.
   *
   * This strategy applies a simple optimization based on the estimates of the physical sizes of
   * the two join sides.  When planning a [[joins.BroadcastHashJoin]], if one side has an
   * estimated physical size smaller than the user-settable threshold
   * [[org.apache.spark.sql.SQLConf.AUTO_BROADCASTJOIN_THRESHOLD]], the planner would mark it as the
   * ''build'' relation and mark the other relation as the ''stream'' side.  The build table will be
   * ''broadcasted'' to all of the executors involved in the join, as a
   * [[org.apache.spark.broadcast.Broadcast]] object.  If both estimates exceed the threshold, they
   * will instead be used to decide the build side in a [[joins.ShuffledHashJoin]].
   */
  object HashJoin extends Strategy with PredicateHelper {

    private[this] def makeBroadcastHashJoin(
        leftKeys: Seq[Expression],
        rightKeys: Seq[Expression],
        left: LogicalPlan,
        right: LogicalPlan,
        condition: Option[Expression],
        side: joins.BuildSide) = {
      val broadcastHashJoin = execution.joins.BroadcastHashJoin(
        leftKeys, rightKeys, side, planLater(left), planLater(right))
      condition.map(Filter(_, broadcastHashJoin)).getOrElse(broadcastHashJoin) :: Nil
    }

    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case ExtractEquiJoinKeys(Inner, leftKeys, rightKeys, condition, left, CanBroadcast(right)) =>
        makeBroadcastHashJoin(leftKeys, rightKeys, left, right, condition, joins.BuildRight)

      case ExtractEquiJoinKeys(Inner, leftKeys, rightKeys, condition, CanBroadcast(left), right) =>
        makeBroadcastHashJoin(leftKeys, rightKeys, left, right, condition, joins.BuildLeft)

      // If the sort merge join option is set, we want to use sort merge join prior to hashjoin
      // for now let's support inner join first, then add outer join
      case ExtractEquiJoinKeys(Inner, leftKeys, rightKeys, condition, left, right)
        if sqlContext.conf.sortMergeJoinEnabled =>
        val mergeJoin =
          joins.SortMergeJoin(leftKeys, rightKeys, planLater(left), planLater(right))
        condition.map(Filter(_, mergeJoin)).getOrElse(mergeJoin) :: Nil

      case ExtractEquiJoinKeys(Inner, leftKeys, rightKeys, condition, left, right) =>
        val buildSide =
          if (right.statistics.sizeInBytes <= left.statistics.sizeInBytes) {
            joins.BuildRight
          } else {
            joins.BuildLeft
          }
        val hashJoin = joins.ShuffledHashJoin(
          leftKeys, rightKeys, buildSide, planLater(left), planLater(right))
        condition.map(Filter(_, hashJoin)).getOrElse(hashJoin) :: Nil

      case ExtractEquiJoinKeys(
             LeftOuter, leftKeys, rightKeys, condition, left, CanBroadcast(right)) =>
        joins.BroadcastHashOuterJoin(
          leftKeys, rightKeys, LeftOuter, condition, planLater(left), planLater(right)) :: Nil

      case ExtractEquiJoinKeys(
             RightOuter, leftKeys, rightKeys, condition, CanBroadcast(left), right) =>
        joins.BroadcastHashOuterJoin(
          leftKeys, rightKeys, RightOuter, condition, planLater(left), planLater(right)) :: Nil

      case ExtractEquiJoinKeys(joinType, leftKeys, rightKeys, condition, left, right) =>
        joins.ShuffledHashOuterJoin(
          leftKeys, rightKeys, joinType, condition, planLater(left), planLater(right)) :: Nil

      case _ => Nil
    }
  }

  object HashAggregation extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      // Aggregations that can be performed in two phases, before and after the shuffle.

      // Cases where all aggregates can be codegened.
      case PartialAggregation(
             namedGroupingAttributes,
             rewrittenAggregateExpressions,
             groupingExpressions,
             partialComputation,
             child)
             if canBeCodeGened(
                  allAggregates(partialComputation) ++
                  allAggregates(rewrittenAggregateExpressions)) &&
               codegenEnabled &&
               !sqlContext.conf.useSqlAggregate2 =>
          execution.GeneratedAggregate(
            partial = false,
            namedGroupingAttributes,
            rewrittenAggregateExpressions,
            unsafeEnabled,
            execution.GeneratedAggregate(
              partial = true,
              groupingExpressions,
              partialComputation,
              unsafeEnabled,
              planLater(child))) :: Nil

      // Cases where some aggregate can not be codegened
      case PartialAggregation(
             namedGroupingAttributes,
             rewrittenAggregateExpressions,
             groupingExpressions,
             partialComputation,
             child) if !sqlContext.conf.useSqlAggregate2 =>
        execution.Aggregate(
          partial = false,
          namedGroupingAttributes,
          rewrittenAggregateExpressions,
          execution.Aggregate(
            partial = true,
            groupingExpressions,
            partialComputation,
            planLater(child))) :: Nil

      case _ => Nil
    }

    def canBeCodeGened(aggs: Seq[AggregateExpression1]): Boolean = !aggs.exists {
      case _: CombineSum | _: Sum | _: Count | _: Max | _: Min |  _: CombineSetsAndCount => false
      // The generated set implementation is pretty limited ATM.
      case CollectHashSet(exprs) if exprs.size == 1  &&
           Seq(IntegerType, LongType).contains(exprs.head.dataType) => false
      case _ => true
    }

    def allAggregates(exprs: Seq[Expression]): Seq[AggregateExpression1] =
      exprs.flatMap(_.collect { case a: AggregateExpression1 => a })
  }

  /**
   * Used to plan the aggregate operator for expressions based on the AggregateFunction2 interface.
   */
  object AggregateOperator2 extends Strategy {
    private def planAggregateWithoutDistinct(
        groupingExpressions: Seq[Expression],
        aggregateExpressions: Seq[AggregateExpression2],
        aggregateFunctionMap: Map[AggregateFunction2, Attribute],
        resultExpressions: Seq[NamedExpression],
        child: SparkPlan): Seq[SparkPlan] = {
      // 1. Create an Aggregate Operator for partial aggregations.
      val namedGroupingExpressions = groupingExpressions.map {
        case ne: NamedExpression => ne -> ne
        // If the expression is not a NamedExpressions, we add an alias.
        // So, when we generate the result of the operator, the Aggregate Operator
        // can directly get the Seq of attributes representing the grouping expressions.
        case other =>
          val withAlias = Alias(other, other.toString)()
          other -> withAlias
      }
      val groupExpressionMap = namedGroupingExpressions.toMap
      val namedGroupingAttributes = namedGroupingExpressions.map(_._2.toAttribute)
      val partialAggregateExpressions = aggregateExpressions.map {
        case AggregateExpression2(aggregateFunction, mode, isDistinct) =>
          AggregateExpression2(aggregateFunction, Partial, isDistinct)
      }
      val partialAggregateAttributes = partialAggregateExpressions.flatMap { agg =>
        agg.aggregateFunction.bufferAttributes
      }
      val partialAggregate =
        Aggregate2Sort(
          None: Option[Seq[Expression]],
          namedGroupingExpressions.map(_._2),
          partialAggregateExpressions,
          partialAggregateAttributes,
          namedGroupingAttributes ++ partialAggregateAttributes,
          child)

      // 2. Create an Aggregate Operator for final aggregations.
      val finalAggregateExpressions = aggregateExpressions.map {
        case AggregateExpression2(aggregateFunction, mode, isDistinct) =>
          AggregateExpression2(aggregateFunction, Final, isDistinct)
      }
      val finalAggregateAttributes =
        finalAggregateExpressions.map {
          expr => aggregateFunctionMap(expr.aggregateFunction)
        }
      val rewrittenResultExpressions = resultExpressions.map { expr =>
        expr.transform {
          case agg: AggregateExpression2 =>
            aggregateFunctionMap(agg.aggregateFunction).toAttribute
          case expression if groupExpressionMap.contains(expression) =>
            groupExpressionMap(expression).toAttribute
        }.asInstanceOf[NamedExpression]
      }
      val finalAggregate = Aggregate2Sort(
        Some(namedGroupingAttributes),
        namedGroupingAttributes,
        finalAggregateExpressions,
        finalAggregateAttributes,
        rewrittenResultExpressions,
        partialAggregate)

      finalAggregate :: Nil
    }

    private def planAggregateWithOneDistinct(
      groupingExpressions: Seq[Expression],
      functionsWithDistinct: Seq[AggregateExpression2],
      functionsWithoutDistinct: Seq[AggregateExpression2],
      aggregateFunctionMap: Map[AggregateFunction2, Attribute],
      resultExpressions: Seq[NamedExpression],
      child: SparkPlan): Seq[SparkPlan] = {

      // 1. Create an Aggregate Operator for partial aggregations.
      // The grouping expressions are original groupingExpressions and
      // distinct columns. For example, for avg(distinct value) ... group by key
      // the grouping expressions of this Aggregate Operator will be [key, value].
      val namedGroupingExpressions = groupingExpressions.map {
        case ne: NamedExpression => ne -> ne
        // If the expression is not a NamedExpressions, we add an alias.
        // So, when we generate the result of the operator, the Aggregate Operator
        // can directly get the Seq of attributes representing the grouping expressions.
        case other =>
          val withAlias = Alias(other, other.toString)()
          other -> withAlias
      }
      val groupExpressionMap = namedGroupingExpressions.toMap
      val namedGroupingAttributes = namedGroupingExpressions.map(_._2.toAttribute)

      // It is safe to call head at here since functionsWithDistinct has at least one
      // AggregateExpression2.
      val distinctColumnExpressions =
        functionsWithDistinct.head.aggregateFunction.children
      val namedDistinctColumnExpressions = distinctColumnExpressions.map {
        case ne: NamedExpression => ne -> ne
        case other =>
          val withAlias = Alias(other, other.toString)()
          other -> withAlias
      }
      val distinctColumnExpressionMap = namedDistinctColumnExpressions.toMap
      val distinctColumnAttributes = namedDistinctColumnExpressions.map(_._2.toAttribute)

      val partialAggregateExpressions = functionsWithoutDistinct.map {
        case AggregateExpression2(aggregateFunction, mode, _) =>
          AggregateExpression2(aggregateFunction, Partial, false)
      }
      val partialAggregateAttributes = partialAggregateExpressions.flatMap { agg =>
        agg.aggregateFunction.bufferAttributes
      }
      println("namedDistinctColumnExpressions " + namedDistinctColumnExpressions)
      val partialAggregate =
        Aggregate2Sort(
          None: Option[Seq[Expression]],
          (namedGroupingExpressions ++ namedDistinctColumnExpressions).map(_._2),
          partialAggregateExpressions,
          partialAggregateAttributes,
          namedGroupingAttributes ++ distinctColumnAttributes ++ partialAggregateAttributes,
          child)

      // 2. Create an Aggregate Operator for partial merge aggregations.
      val partialMergeAggregateExpressions = functionsWithoutDistinct.map {
        case AggregateExpression2(aggregateFunction, mode, _) =>
          AggregateExpression2(aggregateFunction, PartialMerge, false)
      }
      val partialMergeAggregateAttributes =
        partialMergeAggregateExpressions.map {
          expr => aggregateFunctionMap(expr.aggregateFunction)
        }
      val partialMergeAggregate =
        Aggregate2Sort(
          Some(namedGroupingAttributes),
          namedGroupingAttributes ++ distinctColumnAttributes,
          partialMergeAggregateExpressions,
          partialMergeAggregateAttributes,
          namedGroupingAttributes ++ distinctColumnAttributes ++ partialMergeAggregateAttributes,
          partialAggregate)

      // 3. Create an Aggregate Operator for partial merge aggregations.
      val finalAggregateExpressions = functionsWithoutDistinct.map {
        Need to replace the children to distinctColumnAttributes
        case AggregateExpression2(aggregateFunction, mode, _) =>
          AggregateExpression2(aggregateFunction, Final, false)
      }
      val finalAggregateAttributes =
        finalAggregateExpressions.map {
          expr => aggregateFunctionMap(expr.aggregateFunction)
        }
      val completeAggregateExpressions = functionsWithDistinct.map {
        case AggregateExpression2(aggregateFunction, mode, _) =>
          AggregateExpression2(aggregateFunction, Complete, false)
      }
      val completeAggregateAttributes =
        completeAggregateExpressions.map {
          expr => aggregateFunctionMap(expr.aggregateFunction)
        }

      val rewrittenResultExpressions = resultExpressions.map { expr =>
        expr.transform {
          case agg: AggregateExpression2 =>
            aggregateFunctionMap(agg.aggregateFunction).toAttribute
          case expression if groupExpressionMap.contains(expression) =>
            groupExpressionMap(expression).toAttribute
          case expression if distinctColumnExpressionMap.contains(expression) =>
            distinctColumnExpressionMap(expression).toAttribute
        }.asInstanceOf[NamedExpression]
      }
      val finalAndCompleteAggregate = FinalAndCompleteAggregate2Sort(
        namedGroupingAttributes,
        finalAggregateExpressions,
        finalAggregateAttributes,
        completeAggregateExpressions,
        completeAggregateAttributes,
        rewrittenResultExpressions,
        partialMergeAggregate)

      finalAndCompleteAggregate :: Nil
    }

    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case logical.Aggregate(groupingExpressions, resultExpressions, child)
        if sqlContext.conf.useSqlAggregate2 =>
        // 1. Extracts all distinct aggregate expressions from the resultExpressions.
        val aggregateExpressions = resultExpressions.flatMap { expr =>
          expr.collect {
            case agg: AggregateExpression2 => agg
          }
        }.toSet.toSeq
        // For those distinct aggregate expressions, we create a map from the aggregate function
        // to the corresponding attribute of the function.
        val aggregateFunctionMap = aggregateExpressions.map { agg =>
          val aggregateFunction = agg.aggregateFunction
          aggregateFunction -> Alias(aggregateFunction, aggregateFunction.toString)().toAttribute
        }.toMap

        val (functionsWithDistinct, functionsWithoutDistinct) =
          aggregateExpressions.partition(_.isDistinct)
        println("functionsWithDistinct " + functionsWithDistinct)
        if (functionsWithDistinct.map(_.aggregateFunction.children).distinct.length > 1) {
          // This is a sanity check. We should not reach here since we check the same thing in
          // CheckAggregateFunction.
          sys.error("Having more than one distinct column sets is not allowed.")
        }
        val aggregate =
          if (functionsWithDistinct.isEmpty) {
            planAggregateWithoutDistinct(
              groupingExpressions,
              aggregateExpressions,
              aggregateFunctionMap,
              resultExpressions,
              planLater(child))
          } else {
            planAggregateWithOneDistinct(
              groupingExpressions,
              functionsWithDistinct,
              functionsWithoutDistinct,
              aggregateFunctionMap,
              resultExpressions,
              planLater(child))
          }

        aggregate
      case _ => Nil
    }
  }


  object BroadcastNestedLoopJoin extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case logical.Join(left, right, joinType, condition) =>
        val buildSide =
          if (right.statistics.sizeInBytes <= left.statistics.sizeInBytes) {
            joins.BuildRight
          } else {
            joins.BuildLeft
          }
        joins.BroadcastNestedLoopJoin(
          planLater(left), planLater(right), buildSide, joinType, condition) :: Nil
      case _ => Nil
    }
  }

  object CartesianProduct extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case logical.Join(left, right, _, None) =>
        execution.joins.CartesianProduct(planLater(left), planLater(right)) :: Nil
      case logical.Join(left, right, Inner, Some(condition)) =>
        execution.Filter(condition,
          execution.joins.CartesianProduct(planLater(left), planLater(right))) :: Nil
      case _ => Nil
    }
  }

  protected lazy val singleRowRdd = sparkContext.parallelize(Seq(InternalRow()), 1)

  object TakeOrderedAndProject extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case logical.Limit(IntegerLiteral(limit), logical.Sort(order, true, child)) =>
        execution.TakeOrderedAndProject(limit, order, None, planLater(child)) :: Nil
      case logical.Limit(
             IntegerLiteral(limit),
             logical.Project(projectList, logical.Sort(order, true, child))) =>
        execution.TakeOrderedAndProject(limit, order, Some(projectList), planLater(child)) :: Nil
      case _ => Nil
    }
  }

  object ParquetOperations extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      // TODO: need to support writing to other types of files.  Unify the below code paths.
      case logical.WriteToFile(path, child) =>
        val relation =
          ParquetRelation.create(path, child, sparkContext.hadoopConfiguration, sqlContext)
        // Note: overwrite=false because otherwise the metadata we just created will be deleted
        InsertIntoParquetTable(relation, planLater(child), overwrite = false) :: Nil
      case logical.InsertIntoTable(
          table: ParquetRelation, partition, child, overwrite, ifNotExists) =>
        InsertIntoParquetTable(table, planLater(child), overwrite) :: Nil
      case PhysicalOperation(projectList, filters: Seq[Expression], relation: ParquetRelation) =>
        val partitionColNames = relation.partitioningAttributes.map(_.name).toSet
        val filtersToPush = filters.filter { pred =>
            val referencedColNames = pred.references.map(_.name).toSet
            referencedColNames.intersect(partitionColNames).isEmpty
          }
        val prunePushedDownFilters =
          if (sqlContext.conf.parquetFilterPushDown) {
            (predicates: Seq[Expression]) => {
              // Note: filters cannot be pushed down to Parquet if they contain more complex
              // expressions than simple "Attribute cmp Literal" comparisons. Here we remove all
              // filters that have been pushed down. Note that a predicate such as "(A AND B) OR C"
              // can result in "A OR C" being pushed down. Here we are conservative in the sense
              // that even if "A" was pushed and we check for "A AND B" we still want to keep
              // "A AND B" in the higher-level filter, not just "B".
              predicates.map(p => p -> ParquetFilters.createFilter(p)).collect {
                case (predicate, None) => predicate
                // Filter needs to be applied above when it contains partitioning
                // columns
                case (predicate, _)
                  if !predicate.references.map(_.name).toSet.intersect(partitionColNames).isEmpty =>
                  predicate
              }
            }
          } else {
            identity[Seq[Expression]] _
          }
        pruneFilterProject(
          projectList,
          filters,
          prunePushedDownFilters,
          ParquetTableScan(
            _,
            relation,
            if (sqlContext.conf.parquetFilterPushDown) filtersToPush else Nil)) :: Nil

      case _ => Nil
    }
  }

  object InMemoryScans extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case PhysicalOperation(projectList, filters, mem: InMemoryRelation) =>
        pruneFilterProject(
          projectList,
          filters,
          identity[Seq[Expression]], // All filters still need to be evaluated.
          InMemoryColumnarTableScan(_, filters, mem)) :: Nil
      case _ => Nil
    }
  }

  // Can we automate these 'pass through' operations?
  object BasicOperators extends Strategy {
    def numPartitions: Int = self.numPartitions

    /**
     * Picks an appropriate sort operator.
     *
     * @param global when true performs a global sort of all partitions by shuffling the data first
     *               if necessary.
     */
    def getSortOperator(sortExprs: Seq[SortOrder], global: Boolean, child: SparkPlan): SparkPlan = {
      if (sqlContext.conf.unsafeEnabled && UnsafeExternalSort.supportsSchema(child.schema)) {
        execution.UnsafeExternalSort(sortExprs, global, child)
      } else if (sqlContext.conf.externalSortEnabled) {
        execution.ExternalSort(sortExprs, global, child)
      } else {
        execution.Sort(sortExprs, global, child)
      }
    }

    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case r: RunnableCommand => ExecutedCommand(r) :: Nil

      case logical.Distinct(child) =>
        throw new IllegalStateException(
          "logical distinct operator should have been replaced by aggregate in the optimizer")
      case logical.Repartition(numPartitions, shuffle, child) =>
        execution.Repartition(numPartitions, shuffle, planLater(child)) :: Nil
      case logical.SortPartitions(sortExprs, child) =>
        // This sort only sorts tuples within a partition. Its requiredDistribution will be
        // an UnspecifiedDistribution.
        getSortOperator(sortExprs, global = false, planLater(child)) :: Nil
      case logical.Sort(sortExprs, global, child) =>
        getSortOperator(sortExprs, global, planLater(child)):: Nil
      case logical.Project(projectList, child) =>
        execution.Project(projectList, planLater(child)) :: Nil
      case logical.Filter(condition, child) =>
        execution.Filter(condition, planLater(child)) :: Nil
      case e @ logical.Expand(_, _, _, child) =>
        execution.Expand(e.projections, e.output, planLater(child)) :: Nil
      case logical.Aggregate(group, agg, child) if !sqlContext.conf.useSqlAggregate2 =>
        execution.Aggregate(partial = false, group, agg, planLater(child)) :: Nil
      case logical.Window(projectList, windowExpressions, spec, child) =>
        execution.Window(projectList, windowExpressions, spec, planLater(child)) :: Nil
      case logical.Sample(lb, ub, withReplacement, seed, child) =>
        execution.Sample(lb, ub, withReplacement, seed, planLater(child)) :: Nil
      case logical.LocalRelation(output, data) =>
        LocalTableScan(output, data) :: Nil
      case logical.Limit(IntegerLiteral(limit), child) =>
        execution.Limit(limit, planLater(child)) :: Nil
      case Unions(unionChildren) =>
        execution.Union(unionChildren.map(planLater)) :: Nil
      case logical.Except(left, right) =>
        execution.Except(planLater(left), planLater(right)) :: Nil
      case logical.Intersect(left, right) =>
        execution.Intersect(planLater(left), planLater(right)) :: Nil
      case g @ logical.Generate(generator, join, outer, _, _, child) =>
        execution.Generate(
          generator, join = join, outer = outer, g.output, planLater(child)) :: Nil
      case logical.OneRowRelation =>
        execution.PhysicalRDD(Nil, singleRowRdd) :: Nil
      case logical.RepartitionByExpression(expressions, child) =>
        execution.Exchange(HashPartitioning(expressions, numPartitions), planLater(child)) :: Nil
      case e @ EvaluatePython(udf, child, _) =>
        BatchPythonEvaluation(udf, e.output, planLater(child)) :: Nil
      case LogicalRDD(output, rdd) => PhysicalRDD(output, rdd) :: Nil
      case BroadcastHint(child) => apply(child)
      case _ => Nil
    }
  }

  object DDLStrategy extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case CreateTableUsing(tableName, userSpecifiedSchema, provider, true, opts, false, _) =>
        ExecutedCommand(
          CreateTempTableUsing(
            tableName, userSpecifiedSchema, provider, opts)) :: Nil
      case c: CreateTableUsing if !c.temporary =>
        sys.error("Tables created with SQLContext must be TEMPORARY. Use a HiveContext instead.")
      case c: CreateTableUsing if c.temporary && c.allowExisting =>
        sys.error("allowExisting should be set to false when creating a temporary table.")

      case CreateTableUsingAsSelect(tableName, provider, true, partitionsCols, mode, opts, query)
          if partitionsCols.nonEmpty =>
        sys.error("Cannot create temporary partitioned table.")

      case CreateTableUsingAsSelect(tableName, provider, true, _, mode, opts, query) =>
        val cmd = CreateTempTableUsingAsSelect(
          tableName, provider, Array.empty[String], mode, opts, query)
        ExecutedCommand(cmd) :: Nil
      case c: CreateTableUsingAsSelect if !c.temporary =>
        sys.error("Tables created with SQLContext must be TEMPORARY. Use a HiveContext instead.")

      case describe @ LogicalDescribeCommand(table, isExtended) =>
        val resultPlan = self.sqlContext.executePlan(table).executedPlan
        ExecutedCommand(
          RunnableDescribeCommand(resultPlan, describe.output, isExtended)) :: Nil

      case _ => Nil
    }
  }
}
