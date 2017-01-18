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

package org.apache.spark.sql.catalyst.plans.logical.statsEstimation

import java.sql.{Date, Timestamp}

import scala.collection.immutable.{HashSet, Map}
import scala.collection.mutable

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.CatalystConf
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

/**
 * @param plan a LogicalPlan node that must be an instance of Filter
 * @param catalystConf a configuration showing if CBO is enabled
 */
case class FilterEstimation(plan: Filter, catalystConf: CatalystConf) extends Logging {

  /**
   * We use a mutable colStats because we need to update the corresponding ColumnStat
   * for a column after we apply a predicate condition.  For example, column c has
   * [min, max] value as [0, 100].  In a range condition such as (c > 40 AND c <= 50),
   * we need to set the column's [min, max] value to [40, 100] after we evaluate the
   * first condition c > 40.  We need to set the column's [min, max] value to [40, 50]
   * after we evaluate the second condition c <= 50.
   */
  private var mutableColStats: mutable.Map[ExprId, ColumnStat] = mutable.Map.empty

  /**
   * Returns an option of Statistics for a Filter logical plan node.
   * For a given compound expression condition, this method computes filter selectivity
   * (or the percentage of rows meeting the filter condition), which
   * is used to compute row count, size in bytes, and the updated statistics after a given
   * predicated is applied.
   *
   * @return Option[Statistics] When there is no statistics collected, it returns None.
   */
  def estimate: Option[Statistics] = {
    val stats: Statistics = plan.child.stats(catalystConf)
    if (stats.rowCount.isEmpty) return None

    // save a mutable copy of colStats so that we can later change it recursively
    mutableColStats = mutable.Map(stats.attributeStats.map(kv => (kv._1.exprId, kv._2)).toSeq: _*)

    // estimate selectivity of this filter predicate
    val filterSelectivity: Double = calculateConditions(plan.condition)

    // attributeStats has mapping Attribute-to-ColumnStat.
    // mutableColStats has mapping ExprId-to-ColumnStat.
    // We use an ExprId-to-Attribute map to facilitate the mapping Attribute-to-ColumnStat
    val expridToAttrMap: Map[ExprId, Attribute] =
      stats.attributeStats.map(kv => (kv._1.exprId, kv._1))
    // copy mutableColStats contents to an immutable AttributeMap.
    val mutableAttributeStats: mutable.Map[Attribute, ColumnStat] =
      mutableColStats.map(kv => expridToAttrMap(kv._1) -> kv._2)
    val newColStats = AttributeMap(mutableAttributeStats.toSeq)

    val filteredRowCount: BigInt =
      EstimationUtils.ceil(BigDecimal(stats.rowCount.get) * filterSelectivity)
    val filteredSizeInBytes: BigInt = EstimationUtils.ceil(BigDecimal(
        EstimationUtils.getOutputSize(plan.output, newColStats, filteredRowCount)
    ))

    Some(stats.copy(sizeInBytes = filteredSizeInBytes, rowCount = Some(filteredRowCount),
      attributeStats = newColStats))
  }

  /**
   * Returns a percentage of rows meeting a compound condition in Filter node.
   * A compound condition is decomposed into multiple single conditions linked with AND, OR, NOT.
   * For logical AND conditions, we need to update stats after a condition estimation
   * so that the stats will be more accurate for subsequent estimation.  This is needed for
   * range condition such as (c > 40 AND c <= 50)
   * For logical OR conditions, we do not update stats after a condition estimation.
   *
   * @param condition the compound logical expression
   * @param update a boolean flag to specify if we need to update ColumnStat of a column
   *               for subsequent conditions
   * @return a double value to show the percentage of rows meeting a given condition
   */
  def calculateConditions(condition: Expression, update: Boolean = true): Double = {

    condition match {
      case And(cond1, cond2) =>
        val p1 = calculateConditions(cond1, update)
        val p2 = calculateConditions(cond2, update)
        p1 * p2

      case Or(cond1, cond2) =>
        val p1 = calculateConditions(cond1, update = false)
        val p2 = calculateConditions(cond2, update = false)
        math.min(1.0, p1 + p2 - (p1 * p2))

      case Not(cond) => calculateSingleCondition(cond, update = false) match {
        case Some(percent) => 1.0 - percent
        // for not-supported condition, set filter selectivity to a conservative estimate 100%
        case None => 1.0
      }
      case _ => calculateSingleCondition(condition, update) match {
        case Some(percent) => percent
        // for not-supported condition, set filter selectivity to a conservative estimate 100%
        case None => 1.0
      }
    }
  }

  /**
   * Returns a percentage of rows meeting a single condition in Filter node.
   * Currently we only support binary predicates where one side is a column,
   * and the other is a literal.
   *
   * @param condition a single logical expression
   * @param update a boolean flag to specify if we need to update ColumnStat of a column
   *               for subsequent conditions
   * @return Option[Double] value to show the percentage of rows meeting a given condition.
   *         It returns None if the condition is not supported.
   */
  def calculateSingleCondition(condition: Expression, update: Boolean): Option[Double] = {
    condition match {
      // For evaluateBinary method, we assume the literal on the right side of an operator.
      // So we will change the order if not.

      // EqualTo does not care about the order
      case op @ EqualTo(ar: AttributeReference, l: Literal) =>
        evaluateBinary(op, ar, l, update)
      case op @ EqualTo(l: Literal, ar: AttributeReference) =>
        evaluateBinary(op, ar, l, update)

      case op @ LessThan(ar: AttributeReference, l: Literal) =>
        evaluateBinary(op, ar, l, update)
      case op @ LessThan(l: Literal, ar: AttributeReference) =>
        evaluateBinary(GreaterThan(ar, l), ar, l, update)

      case op @ LessThanOrEqual(ar: AttributeReference, l: Literal) =>
        evaluateBinary(op, ar, l, update)
      case op @ LessThanOrEqual(l: Literal, ar: AttributeReference) =>
        evaluateBinary(GreaterThanOrEqual(ar, l), ar, l, update)

      case op @ GreaterThan(ar: AttributeReference, l: Literal) =>
        evaluateBinary(op, ar, l, update)
      case op @ GreaterThan(l: Literal, ar: AttributeReference) =>
        evaluateBinary(LessThan(ar, l), ar, l, update)

      case op @ GreaterThanOrEqual(ar: AttributeReference, l: Literal) =>
        evaluateBinary(op, ar, l, update)
      case op @ GreaterThanOrEqual(l: Literal, ar: AttributeReference) =>
        evaluateBinary(LessThanOrEqual(ar, l), ar, l, update)

      case In(ar: AttributeReference, expList) if !expList.exists(!_.isInstanceOf[Literal]) =>
        // Expression [In (value, seq[Literal])] will be replaced with optimized version
        // [InSet (value, HashSet[Literal])] in Optimizer, but only for list.size > 10.
        // Here we convert In into InSet anyway, because they share the same processing logic.
        val hSet = expList.map(e => e.eval())
        evaluateInSet(ar, HashSet() ++ hSet, update)

      case InSet(ar: AttributeReference, set) =>
        evaluateInSet(ar, set, update)

      // It's difficult to estimate IsNull after outer joins.  Hence,
      // we support IsNull and IsNotNull only when the child is a leaf node (table).
      case IsNull(ar: AttributeReference) =>
        if (plan.child.isInstanceOf[LeafNode ]) {
          evaluateIsNull(ar, isNull = true, update)
        } else {
          None
        }

      case IsNotNull(ar: AttributeReference) =>
        if (plan.child.isInstanceOf[LeafNode ]) {
          evaluateIsNull(ar, isNull = false, update)
        } else {
          None
        }

      case _ =>
        // TODO: it's difficult to support string operators without advanced statistics.
        // Hence, these string operators Like(_, _) | Contains(_, _) | StartsWith(_, _)
        // | EndsWith(_, _) are not supported yet
        logDebug("[CBO] Unsupported filter condition: " + condition)
        None
    }
  }

  /**
   * Returns a percentage of rows meeting "IS NULL" or "IS NOT NULL" condition.
   *
   * @param attrRef an AttributeReference (or a column)
   * @param isNull set to true for "IS NULL" condition.  set to false for "IS NOT NULL" condition
   * @param update a boolean flag to specify if we need to update ColumnStat of a given column
   *               for subsequent conditions
   * @return a doube value to show the percentage of rows meeting a given condition
   *         It returns None if no statistics collected for a given column.
   */
  def evaluateIsNull(
      attrRef: AttributeReference,
      isNull: Boolean,
      update: Boolean)
    : Option[Double] = {
    if (!mutableColStats.contains(attrRef.exprId)) {
      logDebug("[CBO] No statistics for " + attrRef)
      return None
    }
    val aColStat = mutableColStats(attrRef.exprId)
    val rowCountValue = plan.child.stats(catalystConf).rowCount.get
    val nullPercent: BigDecimal =
      if (rowCountValue == 0) 0.0
      else BigDecimal(aColStat.nullCount) / BigDecimal(rowCountValue)

    if (update) {
      val newStats =
        if (isNull) aColStat.copy(distinctCount = 0, min = None, max = None)
        else aColStat.copy(nullCount = 0)

      mutableColStats += (attrRef.exprId -> newStats)
    }

    val percent =
      if (isNull) {
        nullPercent.toDouble
      }
      else {
        /** ISNOTNULL(column) */
        1.0 - nullPercent.toDouble
      }

    Some(percent)
  }

  /**
   * Returns a percentage of rows meeting a binary comparison expression.
   *
   * @param op a binary comparison operator uch as =, <, <=, >, >=
   * @param attrRef an AttributeReference (or a column)
   * @param literal a literal value (or constant)
   * @param update a boolean flag to specify if we need to update ColumnStat of a given column
   *               for subsequent conditions
   * @return a doube value to show the percentage of rows meeting a given condition
    *         It returns None if no statistics exists for a given column or wrong value.
   */
  def evaluateBinary(
      op: BinaryComparison,
      attrRef: AttributeReference,
      literal: Literal,
      update: Boolean)
    : Option[Double] = {
    if (!mutableColStats.contains(attrRef.exprId)) {
      logDebug("[CBO] No statistics for " + attrRef)
      return None
    }

    // Make sure that the Date/Timestamp literal is a valid one
    attrRef.dataType match {
      case DateType if literal.dataType.isInstanceOf[StringType] =>
        val dateLiteral = DateTimeUtils.stringToDate(literal.value.asInstanceOf[UTF8String])
        if (dateLiteral.isEmpty) {
          logDebug("[CBO] Date literal is wrong, No statistics for " + attrRef)
          return None
        }
      case TimestampType if literal.dataType.isInstanceOf[StringType] =>
        val tsLiteral = DateTimeUtils.stringToTimestamp(literal.value.asInstanceOf[UTF8String])
        if (tsLiteral.isEmpty) {
          logDebug("[CBO] Timestamp literal is wrong, No statistics for " + attrRef)
          return None
        }
      case _ =>
    }

    op match {
      case EqualTo(l, r) => evaluateEqualTo(attrRef, literal, update)
      case _ =>
        attrRef.dataType match {
          case _: NumericType | DateType | TimestampType =>
            evaluateBinaryForNumeric(op, attrRef, literal, update)
          case StringType | BinaryType =>
            // TODO: It is difficult to support other binary comparisons for String/Binary
            // type without min/max and advanced statistics like histogram.
            logDebug("[CBO] No range comparison statistics for String/Binary type " + attrRef)
            None
        }
    }
  }

  /**
   * This method converts a numeric or Literal value of numeric type to a BigDecimal value.
   * In order to avoid type casting error such as Java int to Java long, we need to
   * convert a numeric integer value to String, and then convert it to long,
   * and then convert it to BigDecimal.
   *
   * @param attrDataType the column data type
   * @param litValue the literal value
   * @param litDataType the data type of literal value
   * @return a BigDecimal value
   */
  def numericLiteralToBigDecimal(
       attrDataType: DataType,
       litValue: Any,
       litDataType: DataType)
    : BigDecimal = {
    attrDataType match {
      case _: IntegralType =>
        BigDecimal(java.lang.Long.valueOf(litValue.toString))

      case _: FractionalType =>
        BigDecimal(litValue.toString.toDouble)

      case DateType =>
        val dateLiteral = litDataType match {
          case StringType =>
            DateTimeUtils.stringToDate(litValue.asInstanceOf[UTF8String])
              .getOrElse(0).toString
          case _ => litValue.toString
        }
        BigDecimal(java.lang.Long.valueOf(dateLiteral))

      case TimestampType =>
        val tsLiteral = litDataType match {
          case StringType =>
            DateTimeUtils.stringToTimestamp(litValue.asInstanceOf[UTF8String])
                .getOrElse(0).toString
          case _ => litValue.toString
        }
        BigDecimal(java.lang.Long.valueOf(tsLiteral))
    }
  }

  /**
   * Returns a percentage of rows meeting an equality (=) expression.
   * This method evaluates the equality predicate for all data types.
   *
   * @param attrRef an AttributeReference (or a column)
   * @param literal a literal value (or constant)
   * @param update a boolean flag to specify if we need to update ColumnStat of a given column
   *               for subsequent conditions
   * @return a doube value to show the percentage of rows meeting a given condition
   */
  def evaluateEqualTo(
      attrRef: AttributeReference,
      literal: Literal,
      update: Boolean)
    : Option[Double] = {

    val aColStat = mutableColStats(attrRef.exprId)
    val ndv = aColStat.distinctCount

    // decide if the value is in [min, max] of the column.
    // We currently don't store min/max for binary/string type.
    // Hence, we assume it is in boundary for binary/string type.
    val inBoundary: Boolean = attrRef.dataType match {
      case _: NumericType | DateType | TimestampType =>
        val statsRange =
          Range(aColStat.min, aColStat.max, attrRef.dataType).asInstanceOf[NumericRange]
        val lit = numericLiteralToBigDecimal(attrRef.dataType, literal.value, literal.dataType)
        (lit >= statsRange.min) && (lit <= statsRange.max)

      case _ => true  /** for String/Binary type */
    }

    if (inBoundary) {

      if (update) {
        // We update ColumnStat structure after apply this equality predicate.
        // Set distinctCount to 1.  Set nullCount to 0.
        val newStats = attrRef.dataType match {
          case _: NumericType =>
            val newValue = Some(literal.value)
            aColStat.copy(distinctCount = 1, min = newValue,
              max = newValue, nullCount = 0)
          case DateType =>
            val dateValue = literal.dataType match {
              case StringType =>
                Some(Date.valueOf(literal.value.toString))
              case _ => Some(DateTimeUtils.toJavaDate(literal.value.toString.toInt))
            }
            aColStat.copy(distinctCount = 1, min = dateValue,
              max = dateValue, nullCount = 0)
          case TimestampType =>
            val tsValue = literal.dataType match {
              case StringType =>
                Some(Timestamp.valueOf(literal.value.toString))
              case _ => Some(DateTimeUtils.toJavaTimestamp(literal.value.toString.toLong))
            }
            aColStat.copy(distinctCount = 1, min = tsValue,
              max = tsValue, nullCount = 0)
          case _ => aColStat.copy(distinctCount = 1, nullCount = 0)
        }
        mutableColStats += (attrRef.exprId -> newStats)
      }

      Some(1.0 / ndv.toDouble)
    } else {
      Some(0.0)
    }

  }

  /**
   * Returns a percentage of rows meeting "IN" operator expression.
   * This method evaluates the equality predicate for all data types.
   *
   * @param attrRef an AttributeReference (or a column)
   * @param hSet a set of literal values
   * @param update a boolean flag to specify if we need to update ColumnStat of a given column
   *               for subsequent conditions
   * @return a doube value to show the percentage of rows meeting a given condition
   *         It returns None if no statistics exists for a given column.
   */

  def evaluateInSet(
      attrRef: AttributeReference,
      hSet: Set[Any],
      update: Boolean)
    : Option[Double] = {
    if (!mutableColStats.contains(attrRef.exprId)) {
      logDebug("[CBO] No statistics for " + attrRef)
      return None
    }

    val aColStat = mutableColStats(attrRef.exprId)
    val ndv = aColStat.distinctCount
    val aType = attrRef.dataType

    // use [min, max] to filter the original hSet
    val validQuerySet = aType match {
      case _: NumericType | DateType | TimestampType =>
        val statsRange =
          Range(aColStat.min, aColStat.max, aType).asInstanceOf[NumericRange]
        val hSetMap = hSet.map(e =>
            if (e.isInstanceOf[String]) {
              val utf8String = UTF8String.fromString(e.asInstanceOf[String])
              numericLiteralToBigDecimal(aType, utf8String, StringType)
            } else {
              numericLiteralToBigDecimal(aType, e, aType)
            })
        hSetMap.filter(e => e >= statsRange.min && e <= statsRange.max)

      // We assume the whole set since there is no min/max information for String/Binary type
      case StringType | BinaryType => hSet
    }
    if (validQuerySet.isEmpty) {
      return Some(0.0)
    }

    // newNdv should be no greater than the old ndv.  For example, column has only 2 values
    // 1 and 6. The predicate column IN (1, 2, 3, 4, 5). validQuerySet.size is 5.
    val newNdv = math.min(validQuerySet.size.toLong, ndv.longValue())
    val(newMax, newMin) = aType match {
      case _: NumericType =>
        val tmpSet: Set[Double] = validQuerySet.map(e => e.toString.toDouble)
        (Some(tmpSet.max), Some(tmpSet.min))
      case DateType =>
        if (hSet.forall(e => e.isInstanceOf[String])) {
          val dateMax = Date.valueOf(hSet.asInstanceOf[Set[String]].max)
          val dateMin = Date.valueOf(hSet.asInstanceOf[Set[String]].min)
          (Some(dateMax), Some(dateMin))
        } else {
          val tmpSet: Set[Long] = validQuerySet.map(e => e.toString.toLong)
          (Some(tmpSet.max), Some(tmpSet.min))
        }
      case TimestampType =>
        if (hSet.forall(e => e.isInstanceOf[String])) {
          val dateMax = Timestamp.valueOf(hSet.asInstanceOf[Set[String]].max)
          val dateMin = Timestamp.valueOf(hSet.asInstanceOf[Set[String]].min)
          (Some(dateMax), Some(dateMin))
        } else {
          val tmpSet: Set[Long] = validQuerySet.map(e => e.toString.toLong)
          (Some(tmpSet.max), Some(tmpSet.min))
        }
      case _ =>
        (None, None)
    }

    if (update) {
      val newStats = attrRef.dataType match {
        case _: NumericType | DateType | TimestampType =>
          aColStat.copy(distinctCount = newNdv, min = newMin,
            max = newMax, nullCount = 0)
        case StringType | BinaryType =>
          aColStat.copy(distinctCount = newNdv, nullCount = 0)
      }
      mutableColStats += (attrRef.exprId -> newStats)
    }

    // return the filter selectivity.  Without advanced statistics such as histograms,
    // we have to assume uniform distribution.
    Some(math.min(1.0, validQuerySet.size / ndv.toDouble))
  }

  /**
   * Returns a percentage of rows meeting a binary comparison expression.
   * This method evaluate expression for Numeric columns only.
   *
   * @param op a binary comparison operator uch as =, <, <=, >, >=
   * @param attrRef an AttributeReference (or a column)
   * @param literal a literal value (or constant)
   * @param update a boolean flag to specify if we need to update ColumnStat of a given column
   *               for subsequent conditions
   * @return a doube value to show the percentage of rows meeting a given condition
   */
  def evaluateBinaryForNumeric(
      op: BinaryComparison,
      attrRef: AttributeReference,
      literal: Literal,
      update: Boolean)
    : Option[Double] = {

    var percent = 1.0
    val aColStat = mutableColStats(attrRef.exprId)
    val ndv = aColStat.distinctCount
    val statsRange =
      Range(aColStat.min, aColStat.max, attrRef.dataType).asInstanceOf[NumericRange]

    val literalValueBD =
      numericLiteralToBigDecimal(attrRef.dataType, literal.value, literal.dataType)

    // determine the overlapping degree between predicate range and column's range
    val (noOverlap: Boolean, completeOverlap: Boolean) = op match {
      case LessThan(l, r) =>
        (literalValueBD <= statsRange.min, literalValueBD > statsRange.max)
      case LessThanOrEqual(l, r) =>
        (literalValueBD < statsRange.min, literalValueBD >= statsRange.max)
      case GreaterThan(l, r) =>
        (literalValueBD >= statsRange.max, literalValueBD < statsRange.min)
      case GreaterThanOrEqual(l, r) =>
        (literalValueBD > statsRange.max, literalValueBD <= statsRange.min)
    }

    if (noOverlap) {
      percent = 0.0
    } else if (completeOverlap) {
      percent = 1.0
    } else {
      // this is partial overlap case
      var newMax = aColStat.max
      var newMin = aColStat.min
      var newNdv = ndv
      val literalToDouble = literalValueBD.toDouble
      val maxToDouble = BigDecimal(statsRange.max).toDouble
      val minToDouble = BigDecimal(statsRange.min).toDouble

      // Without advanced statistics like histogram, we assume uniform data distribution.
      // We just prorate the adjusted range over the initial range to compute filter selectivity.
      // For ease of computation, we convert all relevant numeric values to Double.
      percent = op match {
        case LessThan(l, r) =>
          (literalToDouble - minToDouble) / (maxToDouble - minToDouble)
        case LessThanOrEqual(l, r) =>
          if (literalValueBD == BigDecimal(statsRange.min)) 1.0 / ndv.toDouble
          else (literalToDouble - minToDouble) / (maxToDouble - minToDouble)
        case GreaterThan(l, r) =>
          (maxToDouble - literalToDouble) / (maxToDouble - minToDouble)
        case GreaterThanOrEqual(l, r) =>
          if (literalValueBD == BigDecimal(statsRange.max)) 1.0 / ndv.toDouble
          else (maxToDouble - literalToDouble) / (maxToDouble - minToDouble)
      }

      if (update) {
        attrRef.dataType match {
          case DateType =>
            val dateValue = literal.dataType match {
              case StringType =>
                Date.valueOf(literal.value.toString)
              case _ => DateTimeUtils.toJavaDate(literal.value.toString.toInt)
            }
            op match {
              case GreaterThan(l, r) => newMin = Some(dateValue)
              case GreaterThanOrEqual(l, r) => newMin = Some(dateValue)
              case LessThan(l, r) => newMax = Some(dateValue)
              case LessThanOrEqual(l, r) => newMax = Some(dateValue)
            }

          case TimestampType =>
            val tsValue = literal.dataType match {
              case StringType =>
                Timestamp.valueOf(literal.value.toString)
              case _ => DateTimeUtils.toJavaTimestamp(literal.value.toString.toLong)
            }
            op match {
              case GreaterThan(l, r) => newMin = Some(tsValue)
              case GreaterThanOrEqual(l, r) => newMin = Some(tsValue)
              case LessThan(l, r) => newMax = Some(tsValue)
              case LessThanOrEqual(l, r) => newMax = Some(tsValue)
            }

          case _ =>
            op match {
              case GreaterThan (l, r) => newMin = Some (literal.value)
              case GreaterThanOrEqual (l, r) => newMin = Some (literal.value)
              case LessThan (l, r) => newMax = Some (literal.value)
              case LessThanOrEqual (l, r) => newMax = Some (literal.value)
            }
        }
        newNdv = math.max(math.round(ndv.toDouble * percent), 1)
        val newStats = aColStat.copy(distinctCount = newNdv, min = newMin,
          max = newMax, nullCount = 0)

        mutableColStats += (attrRef.exprId -> newStats)
      }
    }

    Some(percent)
  }

}
