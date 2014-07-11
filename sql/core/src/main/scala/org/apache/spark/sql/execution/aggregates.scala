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

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.physical._
import org.apache.spark.sql.catalyst.types._

case class AggregateEvaluation(
    schema: Seq[Attribute],
    initialValues: Seq[Expression],
    update: Seq[Expression],
    result: Expression)

/**
 * :: DeveloperApi ::
 * Alternate version of aggregation that leverages projection and thus code generation.
 * Aggregations are converted into a set of projections from a aggregation buffer tuple back onto
 * itself. Currently only used for simple aggregations like SUM, COUNT, or AVERAGE are supported.
 *
 * @param partial if true then aggregation is done partially on local data without shuffling to
 *                ensure all values where `groupingExpressions` are equal are present.
 * @param groupingExpressions expressions that are evaluated to determine grouping.
 * @param aggregateExpressions expressions that are computed for each group.
 * @param child the input data source.
 */
@DeveloperApi
case class GeneratedAggregate(
    partial: Boolean,
    groupingExpressions: Seq[Expression],
    aggregateExpressions: Seq[NamedExpression],
    child: SparkPlan)(@transient sqlContext: SQLContext)
  extends UnaryNode with NoBind {

  override def requiredChildDistribution =
    if (partial) {
      UnspecifiedDistribution :: Nil
    } else {
      if (groupingExpressions == Nil) {
        AllTuples :: Nil
      } else {
        ClusteredDistribution(groupingExpressions) :: Nil
      }
    }

  override def otherCopyArgs = sqlContext :: Nil

  override def output = aggregateExpressions.map(_.toAttribute)

  override def execute() = {
    val aggregatesToCompute = aggregateExpressions.flatMap { a =>
      a.collect { case agg: AggregateExpression => agg}
    }

    val computeFunctions = aggregatesToCompute.map {
      case c @ Count(expr) =>
        val currentCount = AttributeReference("currentCount", LongType, nullable = false)()
        val initialValue = Literal(0L)
        val updateFunction = If(IsNotNull(expr), Add(currentCount, Literal(1L)), currentCount)
        val result = currentCount

        AggregateEvaluation(currentCount :: Nil, initialValue :: Nil, updateFunction :: Nil, result)

      case Sum(expr) =>
        val currentSum = AttributeReference("currentSum", expr.dataType, nullable = false)()
        val initialValue = Cast(Literal(0L), expr.dataType)

        // Coalasce avoids double calculation...
        // but really, common sub expression elimination would be better....
        val updateFunction = Coalesce(Add(expr, currentSum) :: currentSum :: Nil)
        val result = currentSum

        AggregateEvaluation(currentSum :: Nil, initialValue :: Nil, updateFunction :: Nil, result)

      case a @ Average(expr) =>
        val currentCount = AttributeReference("currentCount", LongType, nullable = false)()
        val currentSum = AttributeReference("currentSum", expr.dataType, nullable = false)()
        val initialCount = Literal(0L)
        val initialSum = Cast(Literal(0L), expr.dataType)
        val updateCount = If(IsNotNull(expr), Add(currentCount, Literal(1L)), currentCount)
        val updateSum = Coalesce(Add(expr, currentSum) :: currentSum :: Nil)

        val result = Divide(Cast(currentSum, DoubleType), Cast(currentCount, DoubleType))

        AggregateEvaluation(
          currentCount :: currentSum :: Nil,
          initialCount :: initialSum :: Nil,
          updateCount :: updateSum :: Nil,
          result
        )
    }

    val computationSchema = computeFunctions.flatMap(_.schema)

    val resultMap = aggregatesToCompute.zip(computeFunctions).map {
      case (agg, func) => agg.id -> func.result
    }.toMap

    val namedGroups = groupingExpressions.zipWithIndex.map {
      case (ne: NamedExpression, _) => (ne, ne)
      case (e, i) => (e, Alias(e, s"GroupingExpr$i")())
    }

    val groupMap = namedGroups.map { case (k, v) => k -> v.toAttribute}.toMap

    val resultExpressions = aggregateExpressions.map(_.transform {
      case e: Expression if resultMap.contains(e.id) => resultMap(e.id)
      case e: Expression if groupMap.contains(e) => groupMap(e)
    })

    child.execute().mapPartitions { iter =>
      // Builds a new custom class for holding the results of aggregation for a group.
      @transient
      val newAggregationBuffer =
        newProjection(computeFunctions.flatMap(_.initialValues), child.output)

      // A projection that is used to update the aggregate values for a group given a new tuple.
      // This projection should be targeted at the current values for the group and then applied
      // to a joined row of the current values with the new input row.
      @transient
      val updateProjection =
        newMutableProjection(
          computeFunctions.flatMap(_.update),
          computeFunctions.flatMap(_.schema) ++ child.output)()

      // A projection that computes the group given an input tuple.
      @transient
      val groupProjection = newProjection(groupingExpressions, child.output)

      // A projection that produces the final result, given a computation.
      @transient
      val resultProjectionBuilder =
        newMutableProjection(
          resultExpressions,
          (namedGroups.map(_._2.toAttribute) ++ computationSchema).toSeq)

      val joinedRow = new JoinedRow

      if (groupingExpressions.isEmpty) {
        // TODO: Codegening anything other than the updateProjection is probably over kill.
        val buffer = newAggregationBuffer(EmptyRow).asInstanceOf[MutableRow]
        var currentRow: Row = null
        while (iter.hasNext) {
          currentRow = iter.next()
          updateProjection.target(buffer)(joinedRow(buffer, currentRow))
        }

        val resultProjection = resultProjectionBuilder()
        Iterator(resultProjection(buffer))
      } else {
        val buffers = new java.util.HashMap[Row, MutableRow]()

        var currentRow: Row = null
        while (iter.hasNext) {
          currentRow = iter.next()
          val currentGroup = groupProjection(currentRow)
          var currentBuffer = buffers.get(currentGroup)
          if (currentBuffer == null) {
            currentBuffer = newAggregationBuffer(EmptyRow).asInstanceOf[MutableRow]
            buffers.put(currentGroup, currentBuffer)
          }
          // Target the projection at the current aggregation buffer and then project the updated
          // values.
          updateProjection.target(currentBuffer)(joinedRow(currentBuffer, currentRow))
        }

        new Iterator[Row] {
          private[this] val resultIterator = buffers.entrySet.iterator()
          private[this] val resultProjection = resultProjectionBuilder()

          def hasNext = resultIterator.hasNext

          def next() = {
            val currentGroup = resultIterator.next()
            resultProjection(joinedRow(currentGroup.getKey, currentGroup.getValue))
          }
        }
      }
    }
  }
}