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

import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules._

/**
 * Collapse plans consisting all empty local relations generated by [[PruneFilters]].
 * Note that the ObjectProducer/Consumer and direct aggregations are the exceptions.
 * {{{
 *   SELECT a, b FROM t WHERE 1=0 GROUP BY a, b ORDER BY a, b ==> empty result
 *   SELECT SUM(a) FROM t WHERE 1=0 GROUP BY a HAVING COUNT(*)>1 ORDER BY a (Not optimized)
 * }}}
 */
object CollapseEmptyPlan extends Rule[LogicalPlan] with PredicateHelper {
  private def isEmptyLocalRelation(plan: LogicalPlan): Boolean =
    plan.isInstanceOf[LocalRelation] && plan.asInstanceOf[LocalRelation].data.isEmpty

  def apply(plan: LogicalPlan): LogicalPlan = plan transformUp {
    case x if x.isInstanceOf[ObjectProducer] || x.isInstanceOf[ObjectConsumer] => x

    // Case 1: If groupingExpressions contains all aggregation expressions, the result is empty.
    case a @ Aggregate(ge, ae, child) if isEmptyLocalRelation(child) && ae.forall(ge.contains(_)) =>
      LocalRelation(a.output, data = Seq.empty)

    // Case 2: General aggregations can generate non-empty results.
    case a: Aggregate => a

    // Case 3: The following plans having only empty relations return empty results.
    case p: LogicalPlan if p.children.nonEmpty && p.children.forall(isEmptyLocalRelation) =>
      p match {
        case _: Project | _: Generate | _: Filter | _: Sample | _: Join |
             _: Sort | _: GlobalLimit | _: LocalLimit |
             _: Distinct | _: Except | _: Union |
             _: Repartition =>
          LocalRelation(p.output, data = Seq.empty)
        case _ => p
      }

    // Case 4: The following plans having at least one empty relation return empty results.
    case p @ Join(_, _, Inner, _) if p.children.exists(isEmptyLocalRelation) =>
      LocalRelation(p.output, data = Seq.empty)

    case p: Intersect if p.children.exists(isEmptyLocalRelation) =>
      LocalRelation(p.output, data = Seq.empty)
  }
}
