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

import org.apache.spark.sql.catalyst.CurrentUserContext.CURRENT_USER
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules._
import org.apache.spark.sql.catalyst.trees.TreePattern._
import org.apache.spark.sql.catalyst.util.DateTimeUtils.{convertSpecialDate, convertSpecialTimestamp, convertSpecialTimestampNTZ}
import org.apache.spark.sql.connector.catalog.CatalogManager
import org.apache.spark.sql.errors.QueryCompilationErrors
import org.apache.spark.sql.types._
import org.apache.spark.util.Utils


/**
 * Finds all the [[RuntimeReplaceable]] expressions that are unevaluable and replace them
 * with semantically equivalent expressions that can be evaluated.
 *
 * This is mainly used to provide compatibility with other databases.
 * Few examples are:
 *   we use this to support "left" by replacing it with "substring".
 *   we use this to replace Every and Any with Min and Max respectively.
 */
object ReplaceExpressions extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = plan.transformWithPruning(
    _.containsAnyPattern(RUNTIME_REPLACEABLE)) {
    case p => p.mapExpressions(replace)
  }

  private def replace(e: Expression): Expression = e match {
    case r: RuntimeReplaceable => replace(r.replacement)
    case _ => e.mapChildren(replace)
  }
}

/**
 * Rewrite non correlated exists subquery to use ScalarSubquery
 *   WHERE EXISTS (SELECT A FROM TABLE B WHERE COL1 > 10)
 * will be rewritten to
 *   WHERE (SELECT 1 FROM (SELECT A FROM TABLE B WHERE COL1 > 10) LIMIT 1) IS NOT NULL
 */
object RewriteNonCorrelatedExists extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = plan.transformAllExpressionsWithPruning(
    _.containsPattern(EXISTS_SUBQUERY)) {
    case exists: Exists if exists.children.isEmpty =>
      IsNotNull(
        ScalarSubquery(
          plan = Limit(Literal(1), Project(Seq(Alias(Literal(1), "col")()), exists.plan)),
          exprId = exists.exprId))
  }
}

/**
 * Computes the current date and time to make sure we return the same result in a single query.
 */
object ComputeCurrentTime extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = {
    val currentDates = mutable.Map.empty[String, Literal]
    val timeExpr = CurrentTimestamp()
    val timestamp = timeExpr.eval(EmptyRow).asInstanceOf[Long]
    val currentTime = Literal.create(timestamp, timeExpr.dataType)
    val timezone = Literal.create(conf.sessionLocalTimeZone, StringType)
    val localTimestamps = mutable.Map.empty[String, Literal]

    plan.transformAllExpressionsWithPruning(_.containsPattern(CURRENT_LIKE)) {
      case currentDate @ CurrentDate(Some(timeZoneId)) =>
        currentDates.getOrElseUpdate(timeZoneId, {
          Literal.create(currentDate.eval().asInstanceOf[Int], DateType)
        })
      case CurrentTimestamp() | Now() => currentTime
      case CurrentTimeZone() => timezone
      case localTimestamp @ LocalTimestamp(Some(timeZoneId)) =>
        localTimestamps.getOrElseUpdate(timeZoneId, {
          Literal.create(localTimestamp.eval().asInstanceOf[Long], TimestampNTZType)
        })
    }
  }
}


/**
 * Replaces the expression of CurrentDatabase with the current database name.
 * Replaces the expression of CurrentCatalog with the current catalog name.
 */
case class ReplaceCurrentLike(catalogManager: CatalogManager) extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = {
    import org.apache.spark.sql.connector.catalog.CatalogV2Implicits._
    val currentNamespace = catalogManager.currentNamespace.quoted
    val currentCatalog = catalogManager.currentCatalog.name()
    val currentUser = Option(CURRENT_USER.get()).getOrElse(Utils.getCurrentUserName())

    plan.transformAllExpressionsWithPruning(_.containsPattern(CURRENT_LIKE)) {
      case CurrentDatabase() =>
        Literal.create(currentNamespace, StringType)
      case CurrentCatalog() =>
        Literal.create(currentCatalog, StringType)
      case CurrentUser() =>
        Literal.create(currentUser, StringType)
    }
  }
}

/**
 * Replaces casts of special datetime strings by its date/timestamp values
 * if the input strings are foldable.
 */
object SpecialDatetimeValues extends Rule[LogicalPlan] {
  private val conv = Map[DataType, (String, java.time.ZoneId) => Option[Any]](
    DateType -> convertSpecialDate,
    TimestampType -> convertSpecialTimestamp,
    TimestampNTZType -> convertSpecialTimestampNTZ)
  def apply(plan: LogicalPlan): LogicalPlan = {
    plan.transformAllExpressionsWithPruning(_.containsPattern(CAST)) {
      case cast @ Cast(e, dt @ (DateType | TimestampType | TimestampNTZType), _, _)
        if e.foldable && e.dataType == StringType =>
        Option(e.eval())
          .flatMap(s => conv(dt)(s.toString, cast.zoneId))
          .map(Literal(_, dt))
          .getOrElse(cast)
    }
  }
}

/**
 * Validate whether the [[Offset]] is valid. Dataset API eagerly analyzes the query plan, so a
 * query plan may contains invalid [[Offset]] operators but it's not the final query plan that
 * gets evaluated.
 */
object CheckOffsetOperator extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = {
    plan.foreachUp {
      case o if !o.isInstanceOf[GlobalLimit] && !o.isInstanceOf[LocalLimit]
        && o.children.exists(_.isInstanceOf[Offset]) =>
        throw QueryCompilationErrors.invalidOffsetError(s"in: ${o.nodeName}")
      case _ =>
    }
    plan match {
      case Offset(_, _) =>
        throw QueryCompilationErrors.invalidOffsetError("to be the outermost node")
      case _ =>
    }
    plan
  }
}
