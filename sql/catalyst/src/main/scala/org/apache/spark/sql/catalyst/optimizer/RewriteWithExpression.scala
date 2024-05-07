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

import scala.collection.mutable

import org.apache.spark.SparkException
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.aggregate.AggregateExpression
import org.apache.spark.sql.catalyst.planning.PhysicalAggregation
import org.apache.spark.sql.catalyst.plans.logical.{Aggregate, LogicalPlan, PlanHelper, Project}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.catalyst.trees.TreePattern.{COMMON_EXPR_REF, WITH_EXPRESSION}
import org.apache.spark.sql.internal.SQLConf

/**
 * Rewrites the `With` expressions by adding a `Project` to pre-evaluate the common expressions, or
 * just inline them if they are cheap.
 *
 * Since this rule can introduce new `Project` operators, it is advised to run [[CollapseProject]]
 * after this rule.
 *
 * Note: For now we only use `With` in a few `RuntimeReplaceable` expressions. If we expand its
 *       usage, we should support aggregate/window functions as well.
 */
object RewriteWithExpression extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = {
    plan.transformUpWithSubqueriesAndPruning(_.containsPattern(WITH_EXPRESSION)) {
      // For aggregates, separate the computation of the aggregations themselves from the final
      // result by moving the final result computation into a projection above it. This prevents
      // this rule from producing an invalid Aggregate operator.
      case p @ PhysicalAggregation(physGroupingExprs, physAggExprs, physResExprs, child)
        if containsWithExpression(p) =>
        // We need to first handle a special case: if there is an aggregate function in the child of
        // a With expression, PhysicalAggregation will separate the With expression's reference(s)
        // from its definition, leaving a dangling common expression reference.
        val (aggExprs, resExprs) =
          fixDanglingCommonExpressionRefs(physAggExprs, physResExprs) match {
            case (ae, re) => (ae.map(_.asInstanceOf[AggregateExpression]), re)
          }
        // PhysicalAggregation returns physAggExprs as attribute references, which we change to
        // aliases so that they can be referred to by physResExprs.
        val aggExprsAliases = aggExprs.map(ae => Alias(ae, "_aggregateexpression")(ae.resultId))
        val aggExprIds = aggExprsAliases.map(_.exprId).toSet
        val resExprsAttrs = resExprs.map(_.transform {
          case a: AttributeReference if aggExprIds.contains(a.exprId) =>
            a.withName("_aggregateexpression")
        }.asInstanceOf[NamedExpression])
        // Rewrite the projection and the aggregate separately and then piece them together.
        val agg = Aggregate(physGroupingExprs, physGroupingExprs ++ aggExprsAliases, child)
        val rewrittenAgg = applyInternal(agg)
        val proj = Project(resExprsAttrs, rewrittenAgg)
        applyInternal(proj)
      case p if containsWithExpression(p) => applyInternal(p)
    }
  }

  private def containsWithExpression(p: LogicalPlan): Boolean =
    p.expressions.exists(_.containsPattern(WITH_EXPRESSION))

  private def fixDanglingCommonExpressionRefs(
    exprsWithDanglingRefs: Seq[Expression],
    exprsWithMissingRefs: Seq[Expression],
  ): (Seq[Expression], Seq[Expression]) = {
    lazy val defs = exprsWithMissingRefs.flatMap(_.collect {
      case d: CommonExpressionDef => d.id -> d.child
    }).toMap

    // If there is a dangling reference, we find its definition in exprsWithMissingRefs by looking
    // up by common expression ID in defs - and inline it.
    def inlineDanglingRefs(e: Expression): Expression = e match {
      case w: With => w
      case ref: CommonExpressionRef => defs.getOrElse(ref.id, ref)
      case _ => e.mapChildren(inlineDanglingRefs)
    }

    (
      exprsWithDanglingRefs.map(ae => inlineDanglingRefs(ae)),
      exprsWithMissingRefs.map(_.transformWithPruning(_.containsPattern(WITH_EXPRESSION)) {
        // If there was a dangling reference in exprsWithDanglingRefs, then there will be a
        // corresponding With expression in exprsWithMissingRefs without a ref, which we can unwrap.
        case w: With if !w.containsPattern(COMMON_EXPR_REF) => w.child
      })
    )
  }

  private def applyInternal(p: LogicalPlan): LogicalPlan = {
    val inputPlans = p.children.toArray
    var newPlan: LogicalPlan = p.mapExpressions { expr =>
      rewriteWithExprAndInputPlans(expr, inputPlans)
    }
    newPlan = newPlan.withNewChildren(inputPlans.toIndexedSeq)
    // Since we add extra Projects with extra columns to pre-evaluate the common expressions,
    // the current operator may have extra columns if it inherits the output columns from its
    // child, and we need to project away the extra columns to keep the plan schema unchanged.
    assert(p.output.length <= newPlan.output.length)
    if (p.output.length < newPlan.output.length) {
      assert(p.outputSet.subsetOf(newPlan.outputSet))
      Project(p.output, newPlan)
    } else {
      newPlan
    }
  }

  private def rewriteWithExprAndInputPlans(
      e: Expression,
      inputPlans: Array[LogicalPlan]): Expression = {
    if (!e.containsPattern(WITH_EXPRESSION)) return e
    e match {
      case w: With =>
        // Rewrite nested With expressions first
        val child = rewriteWithExprAndInputPlans(w.child, inputPlans)
        val defs = w.defs.map(rewriteWithExprAndInputPlans(_, inputPlans))
        val refToExpr = mutable.HashMap.empty[CommonExpressionId, Expression]
        val childProjections = Array.fill(inputPlans.length)(mutable.ArrayBuffer.empty[Alias])

        defs.zipWithIndex.foreach { case (CommonExpressionDef(child, id), index) =>
          if (child.containsPattern(COMMON_EXPR_REF)) {
            throw SparkException.internalError(
              "Common expression definition cannot reference other Common expression definitions")
          }
          if (id.canonicalized) {
            throw SparkException.internalError(
              "Cannot rewrite canonicalized Common expression definitions")
          }

          if (CollapseProject.isCheap(child)) {
            refToExpr(id) = child
          } else {
            val childProjectionIndex = inputPlans.indexWhere(
              c => child.references.subsetOf(c.outputSet)
            )
            if (childProjectionIndex == -1) {
              // When we cannot rewrite the common expressions, force to inline them so that the
              // query can still run. This can happen if the join condition contains `With` and
              // the common expression references columns from both join sides.
              // TODO: things can go wrong if the common expression is nondeterministic. We
              //       don't fix it for now to match the old buggy behavior when certain
              //       `RuntimeReplaceable` did not use the `With` expression.
              // TODO: we should calculate the ref count and also inline the common expression
              //       if it's ref count is 1.
              refToExpr(id) = child
            } else {
              val aliasName = if (SQLConf.get.getConf(SQLConf.USE_COMMON_EXPR_ID_FOR_ALIAS)) {
                s"_common_expr_${id.id}"
              } else {
                s"_common_expr_$index"
              }
              val alias = Alias(child, aliasName)()
              val fakeProj = Project(Seq(alias), inputPlans(childProjectionIndex))
              if (PlanHelper.specialExpressionsInUnsupportedOperator(fakeProj).nonEmpty) {
                // We have to inline the common expression if it cannot be put in a Project.
                refToExpr(id) = child
              } else {
                childProjections(childProjectionIndex) += alias
                refToExpr(id) = alias.toAttribute
              }
            }
          }
        }

        for (i <- inputPlans.indices) {
          val projectList = childProjections(i)
          if (projectList.nonEmpty) {
            inputPlans(i) = Project(inputPlans(i).output ++ projectList, inputPlans(i))
          }
        }

        child.transformWithPruning(_.containsPattern(COMMON_EXPR_REF)) {
          case ref: CommonExpressionRef =>
            if (!refToExpr.contains(ref.id)) {
              throw SparkException.internalError("Undefined common expression id " + ref.id)
            }
            if (ref.id.canonicalized) {
              throw SparkException.internalError(
                "Cannot rewrite canonicalized Common expression references")
            }
            refToExpr(ref.id)
        }

      case c: ConditionalExpression =>
        val newAlwaysEvaluatedInputs = c.alwaysEvaluatedInputs.map(
          rewriteWithExprAndInputPlans(_, inputPlans))
        val newExpr = c.withNewAlwaysEvaluatedInputs(newAlwaysEvaluatedInputs)
        // Use transformUp to handle nested With.
        newExpr.transformUpWithPruning(_.containsPattern(WITH_EXPRESSION)) {
          case With(child, defs) =>
            // For With in the conditional branches, they may not be evaluated at all and we can't
            // pull the common expressions into a project which will always be evaluated. Inline it.
            val refToExpr = defs.map(d => d.id -> d.child).toMap
            child.transformWithPruning(_.containsPattern(COMMON_EXPR_REF)) {
              case ref: CommonExpressionRef => refToExpr(ref.id)
            }
        }

      case other => other.mapChildren(rewriteWithExprAndInputPlans(_, inputPlans))
    }
  }
}
