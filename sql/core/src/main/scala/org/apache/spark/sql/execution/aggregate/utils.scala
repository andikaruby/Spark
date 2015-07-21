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

package org.apache.spark.sql.execution.aggregate

import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.catalyst._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.aggregate._
import org.apache.spark.sql.catalyst.plans.logical.{Aggregate, LogicalPlan}
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.types.{StructType, MapType, ArrayType}

object Utils {
  // Right now, we do not support complex types in the grouping key schema.
  private def groupingKeySchemaIsSupported(aggregate: Aggregate): Boolean = {
    val hasComplexTypes = aggregate.groupingExpressions.map(_.dataType).exists {
      case array: ArrayType => true
      case map: MapType => true
      case struct: StructType => true
      case _ => false
    }

    !hasComplexTypes
  }

  private def tryConvert(plan: LogicalPlan): Option[Aggregate] = plan match {
    case p: Aggregate if groupingKeySchemaIsSupported(p) =>
      val converted = p.transformExpressionsDown {
        case expressions.Average(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Average(child),
            mode = aggregate.Complete,
            isDistinct = false)

        case expressions.Count(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Count(child),
            mode = aggregate.Complete,
            isDistinct = false)

        // We do not support multiple COUNT DISTINCT columns for now.
        case expressions.CountDistinct(children) if children.length == 1 =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Count(children.head),
            mode = aggregate.Complete,
            isDistinct = true)

        case expressions.First(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.First(child),
            mode = aggregate.Complete,
            isDistinct = false)

        case expressions.Last(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Last(child),
            mode = aggregate.Complete,
            isDistinct = false)

        case expressions.Max(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Max(child),
            mode = aggregate.Complete,
            isDistinct = false)

        case expressions.Min(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Min(child),
            mode = aggregate.Complete,
            isDistinct = false)

        case expressions.Sum(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Sum(child),
            mode = aggregate.Complete,
            isDistinct = false)

        case expressions.SumDistinct(child) =>
          aggregate.AggregateExpression2(
            aggregateFunction = aggregate.Sum(child),
            mode = aggregate.Complete,
            isDistinct = true)
      }
      // Check if there is any expressions.AggregateExpression1 left.
      // If so, we cannot convert this plan.
      val hasAggregateExpression1 = converted.aggregateExpressions.exists { expr =>
        // For every expressions, check if it contains AggregateExpression1.
        expr.find {
          case agg: expressions.AggregateExpression1 => true
          case other => false
        }.isDefined
      }

      // Check if there are multiple distinct columns.
      val aggregateExpressions = converted.aggregateExpressions.flatMap { expr =>
        expr.collect {
          case agg: AggregateExpression2 => agg
        }
      }.toSet.toSeq
      val functionsWithDistinct = aggregateExpressions.filter(_.isDistinct)
      val hasMultipleDistinctColumnSets =
        if (functionsWithDistinct.map(_.aggregateFunction.children).distinct.length > 1) {
          true
        } else {
          false
        }

      if (!hasAggregateExpression1 && !hasMultipleDistinctColumnSets) Some(converted) else None

    case other => None
  }

  def tryConvert(
      plan: LogicalPlan,
      useNewAggregation: Boolean,
      codeGenEnabled: Boolean): Option[Aggregate] = plan match {
    case p: Aggregate if useNewAggregation && codeGenEnabled =>
      val converted = tryConvert(p)
      if (converted.isDefined) {
        converted
      } else {
        // If the plan cannot be converted, we will do a final round check to if the original
        // logical.Aggregate contains both AggregateExpression1 and AggregateExpression2. If so,
        // we need to throw an exception.
        p match {
          case Aggregate(_, aggregateExpressions, _) => aggregateExpressions.foreach { expr =>
            expr.foreach {
              case agg2: AggregateExpression2 =>
                // TODO: Make this errorMessage more user-friendly.
                val errorMessage =
                  s"${agg2.aggregateFunction} is implemented based on new Aggregate Function " +
                    s"interface and it cannot be used with old Aggregate Function implementaion."
                throw new AnalysisException(errorMessage)
              case other => // OK
            }
          }
          case other => // OK
        }

        None
      }
    case other => None
  }

  def planAggregateWithoutDistinct(
      groupingExpressions: Seq[Expression],
      aggregateExpressions: Seq[AggregateExpression2],
      aggregateFunctionMap: Map[(AggregateFunction2, Boolean), Attribute],
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
        expr => aggregateFunctionMap(expr.aggregateFunction, expr.isDistinct)
      }
    val rewrittenResultExpressions = resultExpressions.map { expr =>
      expr.transform {
        case agg: AggregateExpression2 =>
          aggregateFunctionMap(agg.aggregateFunction, agg.isDistinct).toAttribute
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

  def planAggregateWithOneDistinct(
      groupingExpressions: Seq[Expression],
      functionsWithDistinct: Seq[AggregateExpression2],
      functionsWithoutDistinct: Seq[AggregateExpression2],
      aggregateFunctionMap: Map[(AggregateFunction2, Boolean), Attribute],
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
        expr => aggregateFunctionMap(expr.aggregateFunction, expr.isDistinct)
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
      case AggregateExpression2(aggregateFunction, mode, _) =>
        AggregateExpression2(aggregateFunction, Final, false)
    }
    val finalAggregateAttributes =
      finalAggregateExpressions.map {
        expr => aggregateFunctionMap(expr.aggregateFunction, expr.isDistinct)
      }
    val (completeAggregateExpressions, completeAggregateAttributes) = functionsWithDistinct.map {
      // Children of an AggregateFunction with DISTINCT keyword has already
      // been evaluated. At here, we need to replace original children
      // to AttributeReferences.
      case agg @ AggregateExpression2(aggregateFunction, mode, isDistinct) =>
        val rewrittenAggregateFunction = aggregateFunction.transformDown {
          case expr if distinctColumnExpressionMap.contains(expr) =>
            distinctColumnExpressionMap(expr).toAttribute
        }.asInstanceOf[AggregateFunction2]
        // We rewrite the aggregate function to a non-distinct aggregation because
        // its input will have distinct arguments.
        val rewrittenAggregateExpression =
          AggregateExpression2(rewrittenAggregateFunction, Complete, false)

        val aggregateFunctionAttribute = aggregateFunctionMap(agg.aggregateFunction, isDistinct)
        (rewrittenAggregateExpression -> aggregateFunctionAttribute)
    }.unzip

    val rewrittenResultExpressions = resultExpressions.map { expr =>
      expr.transform {
        case agg: AggregateExpression2 =>
          aggregateFunctionMap(agg.aggregateFunction, agg.isDistinct).toAttribute
        case expression if groupExpressionMap.contains(expression) =>
          groupExpressionMap(expression).toAttribute
      }.asInstanceOf[NamedExpression]
    }
    val finalAndCompleteAggregate = FinalAndCompleteAggregate2Sort(
      namedGroupingAttributes ++ distinctColumnAttributes,
      namedGroupingAttributes,
      finalAggregateExpressions,
      finalAggregateAttributes,
      completeAggregateExpressions,
      completeAggregateAttributes,
      rewrittenResultExpressions,
      partialMergeAggregate)

    finalAndCompleteAggregate :: Nil
  }
}

object NewAggregation {
  type ReturnType = (Seq[Expression], Seq[NamedExpression], LogicalPlan)

  def unapply(plan: LogicalPlan): Option[ReturnType] = {
    Utils.tryConvert(plan, true).map(agg =>
      (agg.groupingExpressions, agg.aggregateExpressions, agg.child))
  }
}
