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

package org.apache.spark.sql.catalyst.plans.logical

import org.apache.spark.sql.catalyst.expressions.{Generator, WindowExpression}
import org.apache.spark.sql.catalyst.expressions.aggregate.AggregateExpression

/**
 * [[PlanHelper]] Contains utility methods that can be used by Analyzer and Optimizer.
 * It can also be container of methods that are common across multiple rules in Analyzer
 * and optimizer.
 */
object PlanHelper {
  /**
   * Check if there's any expression in this query plan operator that is
   * - A WindowExpression but the plan is not Window
   * - An AggregateExpresion but the plan is not Aggregate or Window
   * - A Generator but the plan is not Generate
   * Returns true when this operator hosts illegal expressions. This can happen when
   * 1. The input query from users contain invalid expressions.
   *    Example : SELECT * FROM tab WHERE max(c1) > 0
   * 2. Query rewrites inadvertently produce plans that are invalid.
   */
  def specialExpressionInUnsupportedOperator(plan: LogicalPlan): Boolean = {
    val exprs = plan.expressions
    exprs.flatMap { root =>
      root.find {
        case e: WindowExpression
          if !plan.isInstanceOf[Window] => true
        case e: AggregateExpression
          if !(plan.isInstanceOf[Aggregate] || plan.isInstanceOf[Window]) => true
        case e: Generator
          if !plan.isInstanceOf[Generate] => true
        case _ => false
      }
    }.nonEmpty
  }
}
