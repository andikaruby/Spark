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

package org.apache.spark.sql.catalyst.optimizer

import org.apache.spark.sql.catalyst.expressions.{ArrayTransform, CreateNamedStruct, Expression, GetStructField, If, IsNull, LambdaFunction, Literal, MapFromArrays, MapKeys, MapSort, MapValues, NamedLambdaVariable}
import org.apache.spark.sql.catalyst.plans.logical.{Aggregate, LogicalPlan}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.catalyst.trees.TreePattern.AGGREGATE
import org.apache.spark.sql.types.{ArrayType, MapType, StructType}
import org.apache.spark.util.ArrayImplicits.SparkArrayOps

/**
 * Adds MapSort to group expressions containing map columns, as the key/value paris need to be
 * in the correct order before grouping:
 * SELECT COUNT(*) FROM TABLE GROUP BY map_column =>
 * SELECT COUNT(*) FROM TABLE GROUP BY map_sort(map_column)
 */
object InsertMapSortInGroupingExpressions extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = plan.transformWithPruning(
    _.containsPattern(AGGREGATE), ruleId) {
    case a @ Aggregate(groupingExpr, _, _) =>
      val newGrouping = groupingExpr.map { expr =>
        if (!expr.exists(_.isInstanceOf[MapSort])
          && expr.dataType.existsRecursively(_.isInstanceOf[MapType])) {
          insertMapSortRecursively(expr)
        } else {
          expr
        }
      }
      a.copy(groupingExpressions = newGrouping)
  }

  /*
  Inserts MapSort recursively taking into account when
  it is nested inside a struct or array.
   */
  private def insertMapSortRecursively(e: Expression): Expression = {
    e.dataType match {
      case m: MapType =>
        // Check if value type of MapType contains MapType (possibly nested)
        // and special handle this case.
        val mapSortExpr = if (m.valueType.existsRecursively(_.isInstanceOf[MapType])) {
          MapFromArrays(MapKeys(e), insertMapSortRecursively(MapValues(e)))
        } else {
          e
        }

        MapSort(mapSortExpr)

      case StructType(fields)
        if fields.exists(_.dataType.existsRecursively(_.isInstanceOf[MapType])) =>
        val struct = CreateNamedStruct(fields.zipWithIndex.flatMap { case (f, i) =>
          Seq(Literal(f.name), insertMapSortRecursively(
            GetStructField(e, i, Some(f.name))))
        }.toImmutableArraySeq)
        if (struct.valExprs.forall(_.isInstanceOf[GetStructField])) {
          // No field needs MapSort processing, just return the original expression.
          e
        } else if (e.nullable) {
          If(IsNull(e), Literal(null, struct.dataType), struct)
        } else {
          struct
        }

      case ArrayType(et, containsNull) if et.existsRecursively(_.isInstanceOf[MapType]) =>
        val param = NamedLambdaVariable("x", et, containsNull)
        val funcBody = insertMapSortRecursively(param)

        ArrayTransform(e, LambdaFunction(funcBody, Seq(param)))

      case _ => e
    }
  }

}
