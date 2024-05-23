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

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.analysis._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.QueryPlan
import org.apache.spark.sql.catalyst.plans.logical.statsEstimation.LogicalPlanStats
import org.apache.spark.sql.catalyst.trees.{BinaryLike, LeafLike, UnaryLike}
import org.apache.spark.sql.errors.{QueryCompilationErrors, QueryExecutionErrors}
import org.apache.spark.sql.types.StructType


abstract class LogicalPlan
  extends QueryPlan[LogicalPlan]
  with AnalysisHelper
  with LogicalPlanStats
  with LogicalPlanDistinctKeys
  with QueryPlanConstraints
  with Logging {

  /**
   * Metadata fields that can be projected from this node.
   * Should be overridden if the plan does not propagate its children's output.
   */
  def metadataOutput: Seq[Attribute] = children.flatMap(_.metadataOutput)

  /** Returns true if this subtree has data from a streaming data source. */
  def isStreaming: Boolean = _isStreaming
  private[this] lazy val _isStreaming = children.exists(_.isStreaming)

  override def verboseStringWithSuffix(maxFields: Int): String = {
    super.verboseString(maxFields) + statsCache.map(", " + _.toString).getOrElse("")
  }

  /**
   * Returns the maximum number of rows that this plan may compute.
   *
   * Any operator that a Limit can be pushed passed should override this function (e.g., Union).
   * Any operator that can push through a Limit should override this function (e.g., Project).
   */
  def maxRows: Option[Long] = None

  /**
   * Returns the maximum number of rows this plan may compute on each partition.
   */
  def maxRowsPerPartition: Option[Long] = maxRows

  /**
   * Returns true if this expression and all its children have been resolved to a specific schema
   * and false if it still contains any unresolved placeholders. Implementations of LogicalPlan
   * can override this (e.g.
   * [[org.apache.spark.sql.catalyst.analysis.UnresolvedRelation UnresolvedRelation]]
   * should return `false`).
   */
  lazy val resolved: Boolean = expressions.forall(_.resolved) && childrenResolved

  override protected def statePrefix = if (!resolved) "'" else super.statePrefix

  /**
   * Returns true if all its children of this query plan have been resolved.
   */
  def childrenResolved: Boolean = children.forall(_.resolved)

  /**
   * Resolves a given schema to concrete [[Attribute]] references in this query plan. This function
   * should only be called on analyzed plans since it will throw [[AnalysisException]] for
   * unresolved [[Attribute]]s.
   */
  def resolve(schema: StructType, resolver: Resolver): Seq[Attribute] = {
    schema.map { field =>
      resolve(field.name :: Nil, resolver).map {
        case a: AttributeReference => a
        case _ => throw QueryExecutionErrors.resolveCannotHandleNestedSchema(this)
      }.getOrElse {
        throw QueryCompilationErrors.cannotResolveAttributeError(
          field.name, output.map(_.name).mkString(", "))
      }
    }
  }

  private[this] lazy val childAttributes = AttributeSeq(children.flatMap(_.output))

  private[this] lazy val childMetadataAttributes = AttributeSeq(children.flatMap(_.metadataOutput))

  private[this] lazy val outputAttributes = AttributeSeq(output)

  private[this] lazy val outputMetadataAttributes = AttributeSeq(metadataOutput)

  /**
   * Optionally resolves the given strings to a [[NamedExpression]] using the input from all child
   * nodes of this LogicalPlan. The attribute is expressed as
   * string in the following form: `[scope].AttributeName.[nested].[fields]...`.
   */
  def resolveChildren(
      nameParts: Seq[String],
      resolver: Resolver): Option[NamedExpression] =
    childAttributes.resolve(nameParts, resolver)
      .orElse(childMetadataAttributes.resolve(nameParts, resolver))

  /**
   * Optionally resolves the given strings to a [[NamedExpression]] based on the output of this
   * LogicalPlan. The attribute is expressed as string in the following form:
   * `[scope].AttributeName.[nested].[fields]...`.
   */
  def resolve(
      nameParts: Seq[String],
      resolver: Resolver): Option[NamedExpression] =
    outputAttributes.resolve(nameParts, resolver)
      .orElse(outputMetadataAttributes.resolve(nameParts, resolver))

  /**
   * Given an attribute name, split it to name parts by dot, but
   * don't split the name parts quoted by backticks, for example,
   * `ab.cd`.`efg` should be split into two parts "ab.cd" and "efg".
   */
  def resolveQuoted(
      name: String,
      resolver: Resolver): Option[NamedExpression] = {
    resolve(UnresolvedAttribute.parseAttributeName(name), resolver)
  }

  /**
   * Refreshes (or invalidates) any metadata/data cached in the plan recursively.
   */
  def refresh(): Unit = children.foreach(_.refresh())

  /**
   * Returns the output ordering that this plan generates.
   */
  def outputOrdering: Seq[SortOrder] = Nil

  /**
   * Returns true iff `other`'s output is semantically the same, i.e.:
   *  - it contains the same number of `Attribute`s;
   *  - references are the same;
   *  - the order is equal too.
   */
  def sameOutput(other: LogicalPlan): Boolean = {
    val thisOutput = this.output
    val otherOutput = other.output
    thisOutput.length == otherOutput.length && thisOutput.zip(otherOutput).forall {
      case (a1, a2) => a1.semanticEquals(a2)
    }
  }
}

/**
 * A logical plan node with no children.
 */
trait LeafNode extends LogicalPlan with LeafLike[LogicalPlan] {
  override def producedAttributes: AttributeSet = outputSet

  /** Leaf nodes that can survive analysis must define their own statistics. */
  def computeStats(): Statistics = throw new UnsupportedOperationException
}

/**
 * A logical plan node with single child.
 */
trait UnaryNode extends LogicalPlan with UnaryLike[LogicalPlan] {
  /**
   * Generates all valid constraints including an set of aliased constraints by replacing the
   * original constraint expressions with the corresponding alias
   */
  protected def getAllValidConstraints(projectList: Seq[NamedExpression]): ExpressionSet = {
    var allConstraints = child.constraints

    // For each expression collect its aliases
    val aliasMap = projectList.collect {
      case alias @ Alias(expr, _) if !expr.foldable && expr.deterministic =>
        (expr.canonicalized, alias)
    }.groupBy(_._1).mapValues(_.map(_._2))
    val remainingExpressions = collection.mutable.Set(aliasMap.keySet.toSeq: _*)

    /**
     * https://issues.apache.org/jira/browse/SPARK-33152
     * Filtering allConstraints between each iteration is necessary, because
     * otherwise collecting valid constraints could in the worst case have exponential
     * time and memory complexity. Each replaced alias could double the number of constraints,
     * because we would keep both the original constraint and the one with alias.
     */
    def shouldBeKept(expr: Expression): Boolean = {
      expr.references.subsetOf(outputSet) ||
        remainingExpressions.contains(expr.canonicalized) ||
        (expr.children.nonEmpty && expr.children.forall(shouldBeKept))
    }

    // Replace expressions with aliases
    for ((expr, aliases) <- aliasMap) {
      allConstraints ++= allConstraints.flatMap(constraint => {
        aliases.map(alias => {
          constraint transform {
            case e: Expression if e.semanticEquals(expr) =>
              alias.toAttribute
          }
        })
      })

      remainingExpressions.remove(expr)
      allConstraints = allConstraints.filter(shouldBeKept)
    }

    // Equality between aliases for the same expression
    aliasMap.values.foreach(_.combinations(2).foreach {
      case Seq(a1, a2) =>
        allConstraints += EqualNullSafe(a1.toAttribute, a2.toAttribute)
    })

    /**
     * We keep the child constraints and equality between original and aliased attributes,
     * so [[ConstraintHelper.inferAdditionalConstraints]] would have the full information available.
     */
    projectList.foreach {
      case alias @ Alias(expr, _) =>
        allConstraints += EqualNullSafe(alias.toAttribute, expr)
      case _ => // Don't change.
    }
    allConstraints ++ child.constraints
  }

  override protected lazy val validConstraints: ExpressionSet = child.constraints
}

/**
 * A logical plan node with a left and right child.
 */
trait BinaryNode extends LogicalPlan with BinaryLike[LogicalPlan]

abstract class OrderPreservingUnaryNode extends UnaryNode {
  override final def outputOrdering: Seq[SortOrder] = child.outputOrdering
}

object LogicalPlanIntegrity {

  private def canGetOutputAttrs(p: LogicalPlan): Boolean = {
    p.resolved && !p.expressions.exists { e =>
      e.exists {
        // We cannot call `output` in plans with a `ScalarSubquery` expr having no column,
        // so, we filter out them in advance.
        case s: ScalarSubquery => s.plan.schema.fields.isEmpty
        case _ => false
      }
    }
  }

  /**
   * Since some logical plans (e.g., `Union`) can build `AttributeReference`s in their `output`,
   * this method checks if the same `ExprId` refers to attributes having the same data type
   * in plan output.
   */
  def hasUniqueExprIdsForOutput(plan: LogicalPlan): Boolean = {
    val exprIds = plan.collect { case p if canGetOutputAttrs(p) =>
      // NOTE: we still need to filter resolved expressions here because the output of
      // some resolved logical plans can have unresolved references,
      // e.g., outer references in `ExistenceJoin`.
      p.output.filter(_.resolved).map { a => (a.exprId, a.dataType.asNullable) }
    }.flatten

    val ignoredExprIds = plan.collect {
      // NOTE: `Union` currently reuses input `ExprId`s for output references, but we cannot
      // simply modify the code for assigning new `ExprId`s in `Union#output` because
      // the modification will make breaking changes (See SPARK-32741(#29585)).
      // So, this check just ignores the `exprId`s of `Union` output.
      case u: Union if u.resolved => u.output.map(_.exprId)
    }.flatten.toSet

    val groupedDataTypesByExprId = exprIds.filterNot { case (exprId, _) =>
      ignoredExprIds.contains(exprId)
    }.groupBy(_._1).values.map(_.distinct)

    groupedDataTypesByExprId.forall(_.length == 1)
  }

  /**
   * This method checks if reference `ExprId`s are not reused when assigning a new `ExprId`.
   * For example, it returns false if plan transformers create an alias having the same `ExprId`
   * with one of reference attributes, e.g., `a#1 + 1 AS a#1`.
   */
  def checkIfSameExprIdNotReused(plan: LogicalPlan): Boolean = {
    plan.collect { case p if p.resolved =>
      p.expressions.forall {
        case a: Alias =>
          // Even if a plan is resolved, `a.references` can return unresolved references,
          // e.g., in `Grouping`/`GroupingID`, so we need to filter out them and
          // check if the same `exprId` in `Alias` does not exist
          // among reference `exprId`s.
          !a.references.filter(_.resolved).map(_.exprId).exists(_ == a.exprId)
        case _ =>
          true
      }
    }.forall(identity)
  }

  /**
   * This method checks if the same `ExprId` refers to an unique attribute in a plan tree.
   * Some plan transformers (e.g., `RemoveNoopOperators`) rewrite logical
   * plans based on this assumption.
   */
  def checkIfExprIdsAreGloballyUnique(plan: LogicalPlan): Boolean = {
    checkIfSameExprIdNotReused(plan) && hasUniqueExprIdsForOutput(plan)
  }
}

/**
 * A logical plan node that can generate metadata columns
 */
trait ExposesMetadataColumns extends LogicalPlan {
  def withMetadataColumns(): LogicalPlan
}
