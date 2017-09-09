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
 * Collapse plans consisting empty local relations generated by [[PruneFilters]].
 * 1. Binary(or Higher)-node Logical Plans
 *    - Union with all empty children.
 *    - Join with one or two empty children (including Intersect/Except).
 * 2. Unary-node Logical Plans
 *    - Project/Filter/Sample/Join/Limit/Repartition with all empty children.
 *    - Aggregate with all empty children and at least one grouping expression.
 *    - Generate(Explode) with all empty children. Others like Hive UDTF may return results.
 */
object PropagateEmptyRelation extends Rule[LogicalPlan] with PredicateHelper {
  private def isEmptyLocalRelation(plan: LogicalPlan): Boolean = plan match {
    case p: LocalRelation => p.data.isEmpty
    case _ => false
  }

  private def empty(plan: LogicalPlan) =
    LocalRelation(plan.output, data = Seq.empty, isStreaming = plan.isStreaming)

  def apply(plan: LogicalPlan): LogicalPlan = plan transformUp {
    case p: Union if p.children.forall(isEmptyLocalRelation) =>
      empty(p)

    case p @ Join(_, _, joinType, _) if p.children.exists(isEmptyLocalRelation) => joinType match {
      case _: InnerLike => empty(p)
      // Intersect is handled as LeftSemi by `ReplaceIntersectWithSemiJoin` rule.
      // Except is handled as LeftAnti by `ReplaceExceptWithAntiJoin` rule.
      case LeftOuter | LeftSemi | LeftAnti if isEmptyLocalRelation(p.left) => empty(p)
      case RightOuter if isEmptyLocalRelation(p.right) => empty(p)
      case FullOuter if p.children.forall(isEmptyLocalRelation) => empty(p)
      case _ => p
    }

    case p: UnaryNode if p.children.nonEmpty && p.children.forall(isEmptyLocalRelation) => p match {
      case _: Project => empty(p)
      case _: Filter => empty(p)
      case _: Sample => empty(p)
      case _: Sort => empty(p)
      case _: GlobalLimit => empty(p)
      case _: LocalLimit => empty(p)
      case _: Repartition => empty(p)
      case _: RepartitionByExpression => empty(p)
      // An aggregate with non-empty group expression will return one output row per group when the
      // input to the aggregate is not empty. If the input to the aggregate is empty then all groups
      // will be empty and thus the output will be empty. If we're working on batch data, we can
      // then treat the aggregate as redundant.
      //
      // If the aggregate is over streaming data, we may need to update the state store even if no
      // new rows are processed, so we can't eliminate the node.
      //
      // If the grouping expressions are empty, however, then the aggregate will always produce a
      // single output row and thus we cannot propagate the EmptyRelation.
      case Aggregate(ge, _, _) if ge.nonEmpty && !p.isStreaming => empty(p)
      // Generators like Hive-style UDTF may return their records within `close`.
      case Generate(_: Explode, _, _, _, _, _) => empty(p)
      case _ => p
    }
  }
}
