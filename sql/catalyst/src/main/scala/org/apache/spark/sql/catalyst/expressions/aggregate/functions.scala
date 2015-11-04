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

package org.apache.spark.sql.catalyst.expressions.aggregate

import java.lang.{Long => JLong}
import java.util

import com.clearspring.analytics.hash.MurmurHash

import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.catalyst._
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types._

case class Average(child: Expression) extends DeclarativeAggregate {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = resultType

  // Expected input data type.
  // TODO: Right now, we replace old aggregate functions (based on AggregateExpression1) to the
  // new version at planning time (after analysis phase). For now, NullType is added at here
  // to make it resolved when we have cases like `select avg(null)`.
  // We can use our analyzer to cast NullType to the default data type of the NumericType once
  // we remove the old aggregate functions. Then, we will not need NullType at here.
  override def inputTypes: Seq[AbstractDataType] = Seq(TypeCollection(NumericType, NullType))

  private val resultType = child.dataType match {
    case DecimalType.Fixed(p, s) =>
      DecimalType.bounded(p + 4, s + 4)
    case _ => DoubleType
  }

  private val sumDataType = child.dataType match {
    case _ @ DecimalType.Fixed(p, s) => DecimalType.bounded(p + 10, s)
    case _ => DoubleType
  }

  private val sum = AttributeReference("sum", sumDataType)()
  private val count = AttributeReference("count", LongType)()

  override val aggBufferAttributes = sum :: count :: Nil

  override val initialValues = Seq(
    /* sum = */ Cast(Literal(0), sumDataType),
    /* count = */ Literal(0L)
  )

  override val updateExpressions = Seq(
    /* sum = */
    Add(
      sum,
      Coalesce(Cast(child, sumDataType) :: Cast(Literal(0), sumDataType) :: Nil)),
    /* count = */ If(IsNull(child), count, count + 1L)
  )

  override val mergeExpressions = Seq(
    /* sum = */ sum.left + sum.right,
    /* count = */ count.left + count.right
  )

  // If all input are nulls, count will be 0 and we will get null after the division.
  override val evaluateExpression = child.dataType match {
    case DecimalType.Fixed(p, s) =>
      // increase the precision and scale to prevent precision loss
      val dt = DecimalType.bounded(p + 14, s + 4)
      Cast(Cast(sum, dt) / Cast(count, dt), resultType)
    case _ =>
      Cast(sum, resultType) / Cast(count, resultType)
  }
}

case class Count(child: Expression) extends DeclarativeAggregate {
  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = false

  // Return data type.
  override def dataType: DataType = LongType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val count = AttributeReference("count", LongType)()

  override val aggBufferAttributes = count :: Nil

  override val initialValues = Seq(
    /* count = */ Literal(0L)
  )

  override val updateExpressions = Seq(
    /* count = */ If(IsNull(child), count, count + 1L)
  )

  override val mergeExpressions = Seq(
    /* count = */ count.left + count.right
  )

  override val evaluateExpression = Cast(count, LongType)
}

/**
 * Returns the first value of `child` for a group of rows. If the first value of `child`
 * is `null`, it returns `null` (respecting nulls). Even if [[First]] is used on a already
 * sorted column, if we do partial aggregation and final aggregation (when mergeExpression
 * is used) its result will not be deterministic (unless the input table is sorted and has
 * a single partition, and we use a single reducer to do the aggregation.).
 * @param child
 */
case class First(child: Expression, ignoreNullsExpr: Expression) extends DeclarativeAggregate {

  def this(child: Expression) = this(child, Literal.create(false, BooleanType))

  private val ignoreNulls: Boolean = ignoreNullsExpr match {
    case Literal(b: Boolean, BooleanType) => b
    case _ =>
      throw new AnalysisException("The second argument of First should be a boolean literal.")
  }

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // First is not a deterministic function.
  override def deterministic: Boolean = false

  // Return data type.
  override def dataType: DataType = child.dataType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val first = AttributeReference("first", child.dataType)()

  private val valueSet = AttributeReference("valueSet", BooleanType)()

  override val aggBufferAttributes = first :: valueSet :: Nil

  override val initialValues = Seq(
    /* first = */ Literal.create(null, child.dataType),
    /* valueSet = */ Literal.create(false, BooleanType)
  )

  override val updateExpressions = {
    if (ignoreNulls) {
      Seq(
        /* first = */ If(Or(valueSet, IsNull(child)), first, child),
        /* valueSet = */ Or(valueSet, IsNotNull(child))
      )
    } else {
      Seq(
        /* first = */ If(valueSet, first, child),
        /* valueSet = */ Literal.create(true, BooleanType)
      )
    }
  }

  override val mergeExpressions = {
    // For first, we can just check if valueSet.left is set to true. If it is set
    // to true, we use first.right. If not, we use first.right (even if valueSet.right is
    // false, we are safe to do so because first.right will be null in this case).
    Seq(
      /* first = */ If(valueSet.left, first.left, first.right),
      /* valueSet = */ Or(valueSet.left, valueSet.right)
    )
  }

  override val evaluateExpression = first

  override def toString: String = s"FIRST($child)${if (ignoreNulls) " IGNORE NULLS"}"
}

/**
 * Returns the last value of `child` for a group of rows. If the last value of `child`
 * is `null`, it returns `null` (respecting nulls). Even if [[Last]] is used on a already
 * sorted column, if we do partial aggregation and final aggregation (when mergeExpression
 * is used) its result will not be deterministic (unless the input table is sorted and has
 * a single partition, and we use a single reducer to do the aggregation.).
 * @param child
 */
case class Last(child: Expression, ignoreNullsExpr: Expression) extends DeclarativeAggregate {

  def this(child: Expression) = this(child, Literal.create(false, BooleanType))

  private val ignoreNulls: Boolean = ignoreNullsExpr match {
    case Literal(b: Boolean, BooleanType) => b
    case _ =>
      throw new AnalysisException("The second argument of First should be a boolean literal.")
  }

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Last is not a deterministic function.
  override def deterministic: Boolean = false

  // Return data type.
  override def dataType: DataType = child.dataType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val last = AttributeReference("last", child.dataType)()

  override val aggBufferAttributes = last :: Nil

  override val initialValues = Seq(
    /* last = */ Literal.create(null, child.dataType)
  )

  override val updateExpressions = {
    if (ignoreNulls) {
      Seq(
        /* last = */ If(IsNull(child), last, child)
      )
    } else {
      Seq(
        /* last = */ child
      )
    }
  }

  override val mergeExpressions = {
    if (ignoreNulls) {
      Seq(
        /* last = */ If(IsNull(last.right), last.left, last.right)
      )
    } else {
      Seq(
        /* last = */ last.right
      )
    }
  }

  override val evaluateExpression = last

  override def toString: String = s"LAST($child)${if (ignoreNulls) " IGNORE NULLS"}"
}

case class Max(child: Expression) extends DeclarativeAggregate {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = child.dataType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val max = AttributeReference("max", child.dataType)()

  override val aggBufferAttributes = max :: Nil

  override val initialValues = Seq(
    /* max = */ Literal.create(null, child.dataType)
  )

  override val updateExpressions = Seq(
    /* max = */ If(IsNull(child), max, If(IsNull(max), child, Greatest(Seq(max, child))))
  )

  override val mergeExpressions = {
    val greatest = Greatest(Seq(max.left, max.right))
    Seq(
      /* max = */ If(IsNull(max.right), max.left, If(IsNull(max.left), max.right, greatest))
    )
  }

  override val evaluateExpression = max
}

case class Min(child: Expression) extends DeclarativeAggregate {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = child.dataType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val min = AttributeReference("min", child.dataType)()

  override val aggBufferAttributes = min :: Nil

  override val initialValues = Seq(
    /* min = */ Literal.create(null, child.dataType)
  )

  override val updateExpressions = Seq(
    /* min = */ If(IsNull(child), min, If(IsNull(min), child, Least(Seq(min, child))))
  )

  override val mergeExpressions = {
    val least = Least(Seq(min.left, min.right))
    Seq(
      /* min = */ If(IsNull(min.right), min.left, If(IsNull(min.left), min.right, least))
    )
  }

  override val evaluateExpression = min
}

// Compute the sample standard deviation of a column
case class Stddev(child: Expression) extends StddevAgg(child) {

  override def isSample: Boolean = true
  override def prettyName: String = "stddev"
}

// Compute the population standard deviation of a column
case class StddevPop(child: Expression) extends StddevAgg(child) {

  override def isSample: Boolean = false
  override def prettyName: String = "stddev_pop"
}

// Compute the sample standard deviation of a column
case class StddevSamp(child: Expression) extends StddevAgg(child) {

  override def isSample: Boolean = true
  override def prettyName: String = "stddev_samp"
}

// Compute standard deviation based on online algorithm specified here:
// http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
abstract class StddevAgg(child: Expression) extends DeclarativeAggregate {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  def isSample: Boolean

  // Return data type.
  override def dataType: DataType = resultType

  // Expected input data type.
  // TODO: Right now, we replace old aggregate functions (based on AggregateExpression1) to the
  // new version at planning time (after analysis phase). For now, NullType is added at here
  // to make it resolved when we have cases like `select stddev(null)`.
  // We can use our analyzer to cast NullType to the default data type of the NumericType once
  // we remove the old aggregate functions. Then, we will not need NullType at here.
  override def inputTypes: Seq[AbstractDataType] = Seq(TypeCollection(NumericType, NullType))

  private val resultType = DoubleType

  private val count = AttributeReference("count", resultType)()
  private val avg = AttributeReference("avg", resultType)()
  private val mk = AttributeReference("mk", resultType)()

  override val aggBufferAttributes = count :: avg :: mk :: Nil

  override val initialValues = Seq(
    /* count = */ Cast(Literal(0), resultType),
    /* avg = */ Cast(Literal(0), resultType),
    /* mk = */ Cast(Literal(0), resultType)
  )

  override val updateExpressions = {
    val value = Cast(child, resultType)
    val newCount = count + Cast(Literal(1), resultType)

    // update average
    // avg = avg + (value - avg)/count
    val newAvg = avg + (value - avg) / newCount

    // update sum of square of difference from mean
    // Mk = Mk + (value - preAvg) * (value - updatedAvg)
    val newMk = mk + (value - avg) * (value - newAvg)

    Seq(
      /* count = */ If(IsNull(child), count, newCount),
      /* avg = */ If(IsNull(child), avg, newAvg),
      /* mk = */ If(IsNull(child), mk, newMk)
    )
  }

  override val mergeExpressions = {

    // count merge
    val newCount = count.left + count.right

    // average merge
    val newAvg = ((avg.left * count.left) + (avg.right * count.right)) / newCount

    // update sum of square differences
    val newMk = {
      val avgDelta = avg.right - avg.left
      val mkDelta = (avgDelta * avgDelta) * (count.left * count.right) / newCount
      mk.left + mk.right + mkDelta
    }

    Seq(
      /* count = */ If(IsNull(count.left), count.right,
                       If(IsNull(count.right), count.left, newCount)),
      /* avg = */ If(IsNull(avg.left), avg.right,
                     If(IsNull(avg.right), avg.left, newAvg)),
      /* mk = */ If(IsNull(mk.left), mk.right,
                    If(IsNull(mk.right), mk.left, newMk))
    )
  }

  override val evaluateExpression = {
    // when count == 0, return null
    // when count == 1, return 0
    // when count >1
    // stddev_samp = sqrt (mk/(count -1))
    // stddev_pop = sqrt (mk/count)
    val varCol =
      if (isSample) {
        mk / Cast((count - Cast(Literal(1), resultType)), resultType)
      } else {
        mk / count
      }

    If(EqualTo(count, Cast(Literal(0), resultType)), Cast(Literal(null), resultType),
      If(EqualTo(count, Cast(Literal(1), resultType)), Cast(Literal(0), resultType),
        Cast(Sqrt(varCol), resultType)))
  }
}

case class Sum(child: Expression) extends DeclarativeAggregate {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = resultType

  // Expected input data type.
  // TODO: Right now, we replace old aggregate functions (based on AggregateExpression1) to the
  // new version at planning time (after analysis phase). For now, NullType is added at here
  // to make it resolved when we have cases like `select sum(null)`.
  // We can use our analyzer to cast NullType to the default data type of the NumericType once
  // we remove the old aggregate functions. Then, we will not need NullType at here.
  override def inputTypes: Seq[AbstractDataType] =
    Seq(TypeCollection(LongType, DoubleType, DecimalType, NullType))

  private val resultType = child.dataType match {
    case DecimalType.Fixed(precision, scale) =>
      DecimalType.bounded(precision + 10, scale)
    // TODO: Remove this line once we remove the NullType from inputTypes.
    case NullType => IntegerType
    case _ => child.dataType
  }

  private val sumDataType = resultType

  private val sum = AttributeReference("sum", sumDataType)()

  private val zero = Cast(Literal(0), sumDataType)

  override val aggBufferAttributes = sum :: Nil

  override val initialValues = Seq(
    /* sum = */ Literal.create(null, sumDataType)
  )

  override val updateExpressions = Seq(
    /* sum = */
    Coalesce(Seq(Add(Coalesce(Seq(sum, zero)), Cast(child, sumDataType)), sum))
  )

  override val mergeExpressions = {
    val add = Add(Coalesce(Seq(sum.left, zero)), Cast(sum.right, sumDataType))
    Seq(
      /* sum = */
      Coalesce(Seq(add, sum.left))
    )
  }

  override val evaluateExpression = Cast(sum, resultType)
}

/**
 * Compute Pearson correlation between two expressions.
 * When applied on empty data (i.e., count is zero), it returns NULL.
 *
 * Definition of Pearson correlation can be found at
 * http://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient
 *
 * @param left one of the expressions to compute correlation with.
 * @param right another expression to compute correlation with.
 */
case class Corr(
    left: Expression,
    right: Expression,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0)
  extends ImperativeAggregate {

  def children: Seq[Expression] = Seq(left, right)

  def nullable: Boolean = false

  def dataType: DataType = DoubleType

  override def inputTypes: Seq[AbstractDataType] = Seq(DoubleType, DoubleType)

  def aggBufferSchema: StructType = StructType.fromAttributes(aggBufferAttributes)

  def inputAggBufferAttributes: Seq[AttributeReference] = aggBufferAttributes.map(_.newInstance())

  val aggBufferAttributes: Seq[AttributeReference] = Seq(
    AttributeReference("xAvg", DoubleType)(),
    AttributeReference("yAvg", DoubleType)(),
    AttributeReference("Ck", DoubleType)(),
    AttributeReference("MkX", DoubleType)(),
    AttributeReference("MkY", DoubleType)(),
    AttributeReference("count", LongType)())

  // Local cache of mutableAggBufferOffset(s) that will be used in update and merge
  private[this] val mutableAggBufferOffsetPlus1 = mutableAggBufferOffset + 1
  private[this] val mutableAggBufferOffsetPlus2 = mutableAggBufferOffset + 2
  private[this] val mutableAggBufferOffsetPlus3 = mutableAggBufferOffset + 3
  private[this] val mutableAggBufferOffsetPlus4 = mutableAggBufferOffset + 4
  private[this] val mutableAggBufferOffsetPlus5 = mutableAggBufferOffset + 5

  // Local cache of inputAggBufferOffset(s) that will be used in update and merge
  private[this] val inputAggBufferOffsetPlus1 = inputAggBufferOffset + 1
  private[this] val inputAggBufferOffsetPlus2 = inputAggBufferOffset + 2
  private[this] val inputAggBufferOffsetPlus3 = inputAggBufferOffset + 3
  private[this] val inputAggBufferOffsetPlus4 = inputAggBufferOffset + 4
  private[this] val inputAggBufferOffsetPlus5 = inputAggBufferOffset + 5

  override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
    copy(mutableAggBufferOffset = newMutableAggBufferOffset)

  override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
    copy(inputAggBufferOffset = newInputAggBufferOffset)

  override def initialize(buffer: MutableRow): Unit = {
    buffer.setDouble(mutableAggBufferOffset, 0.0)
    buffer.setDouble(mutableAggBufferOffsetPlus1, 0.0)
    buffer.setDouble(mutableAggBufferOffsetPlus2, 0.0)
    buffer.setDouble(mutableAggBufferOffsetPlus3, 0.0)
    buffer.setDouble(mutableAggBufferOffsetPlus4, 0.0)
    buffer.setLong(mutableAggBufferOffsetPlus5, 0L)
  }

  override def update(buffer: MutableRow, input: InternalRow): Unit = {
    val leftEval = left.eval(input)
    val rightEval = right.eval(input)

    if (leftEval != null && rightEval != null) {
      val x = leftEval.asInstanceOf[Double]
      val y = rightEval.asInstanceOf[Double]

      var xAvg = buffer.getDouble(mutableAggBufferOffset)
      var yAvg = buffer.getDouble(mutableAggBufferOffsetPlus1)
      var Ck = buffer.getDouble(mutableAggBufferOffsetPlus2)
      var MkX = buffer.getDouble(mutableAggBufferOffsetPlus3)
      var MkY = buffer.getDouble(mutableAggBufferOffsetPlus4)
      var count = buffer.getLong(mutableAggBufferOffsetPlus5)

      val deltaX = x - xAvg
      val deltaY = y - yAvg
      count += 1
      xAvg += deltaX / count
      yAvg += deltaY / count
      Ck += deltaX * (y - yAvg)
      MkX += deltaX * (x - xAvg)
      MkY += deltaY * (y - yAvg)

      buffer.setDouble(mutableAggBufferOffset, xAvg)
      buffer.setDouble(mutableAggBufferOffsetPlus1, yAvg)
      buffer.setDouble(mutableAggBufferOffsetPlus2, Ck)
      buffer.setDouble(mutableAggBufferOffsetPlus3, MkX)
      buffer.setDouble(mutableAggBufferOffsetPlus4, MkY)
      buffer.setLong(mutableAggBufferOffsetPlus5, count)
    }
  }

  // Merge counters from other partitions. Formula can be found at:
  // http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
  override def merge(buffer1: MutableRow, buffer2: InternalRow): Unit = {
    val count2 = buffer2.getLong(inputAggBufferOffsetPlus5)

    // We only go to merge two buffers if there is at least one record aggregated in buffer2.
    // We don't need to check count in buffer1 because if count2 is more than zero, totalCount
    // is more than zero too, then we won't get a divide by zero exception.
    if (count2 > 0) {
      var xAvg = buffer1.getDouble(mutableAggBufferOffset)
      var yAvg = buffer1.getDouble(mutableAggBufferOffsetPlus1)
      var Ck = buffer1.getDouble(mutableAggBufferOffsetPlus2)
      var MkX = buffer1.getDouble(mutableAggBufferOffsetPlus3)
      var MkY = buffer1.getDouble(mutableAggBufferOffsetPlus4)
      var count = buffer1.getLong(mutableAggBufferOffsetPlus5)

      val xAvg2 = buffer2.getDouble(inputAggBufferOffset)
      val yAvg2 = buffer2.getDouble(inputAggBufferOffsetPlus1)
      val Ck2 = buffer2.getDouble(inputAggBufferOffsetPlus2)
      val MkX2 = buffer2.getDouble(inputAggBufferOffsetPlus3)
      val MkY2 = buffer2.getDouble(inputAggBufferOffsetPlus4)

      val totalCount = count + count2
      val deltaX = xAvg - xAvg2
      val deltaY = yAvg - yAvg2
      Ck += Ck2 + deltaX * deltaY * count / totalCount * count2
      xAvg = (xAvg * count + xAvg2 * count2) / totalCount
      yAvg = (yAvg * count + yAvg2 * count2) / totalCount
      MkX += MkX2 + deltaX * deltaX * count / totalCount * count2
      MkY += MkY2 + deltaY * deltaY * count / totalCount * count2
      count = totalCount

      buffer1.setDouble(mutableAggBufferOffset, xAvg)
      buffer1.setDouble(mutableAggBufferOffsetPlus1, yAvg)
      buffer1.setDouble(mutableAggBufferOffsetPlus2, Ck)
      buffer1.setDouble(mutableAggBufferOffsetPlus3, MkX)
      buffer1.setDouble(mutableAggBufferOffsetPlus4, MkY)
      buffer1.setLong(mutableAggBufferOffsetPlus5, count)
    }
  }

  override def eval(buffer: InternalRow): Any = {
    val count = buffer.getLong(mutableAggBufferOffsetPlus5)
    if (count > 0) {
      val Ck = buffer.getDouble(mutableAggBufferOffsetPlus2)
      val MkX = buffer.getDouble(mutableAggBufferOffsetPlus3)
      val MkY = buffer.getDouble(mutableAggBufferOffsetPlus4)
      val corr = Ck / math.sqrt(MkX * MkY)
      if (corr.isNaN) {
        null
      } else {
        corr
      }
    } else {
      null
    }
  }
}

// scalastyle:off
/**
 * HyperLogLog++ (HLL++) is a state of the art cardinality estimation algorithm. This class
 * implements the dense version of the HLL++ algorithm as an Aggregate Function.
 *
 * This implementation has been based on the following papers:
 * HyperLogLog: the analysis of a near-optimal cardinality estimation algorithm
 * http://algo.inria.fr/flajolet/Publications/FlFuGaMe07.pdf
 *
 * HyperLogLog in Practice: Algorithmic Engineering of a State of The Art Cardinality Estimation
 * Algorithm
 * http://static.googleusercontent.com/external_content/untrusted_dlcp/research.google.com/en/us/pubs/archive/40671.pdf
 *
 * Appendix to HyperLogLog in Practice: Algorithmic Engineering of a State of the Art Cardinality
 * Estimation Algorithm
 * https://docs.google.com/document/d/1gyjfMHy43U9OWBXxfaeG-3MjGzejW1dlpyMwEYAAWEI/view?fullscreen#
 *
 * @param child to estimate the cardinality of.
 * @param relativeSD the maximum estimation error allowed.
 */
// scalastyle:on
case class HyperLogLogPlusPlus(
    child: Expression,
    relativeSD: Double = 0.05,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0)
  extends ImperativeAggregate {
  import HyperLogLogPlusPlus._

  override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
    copy(mutableAggBufferOffset = newMutableAggBufferOffset)

  override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
    copy(inputAggBufferOffset = newInputAggBufferOffset)

  /**
   * HLL++ uses 'p' bits for addressing. The more addressing bits we use, the more precise the
   * algorithm will be, and the more memory it will require. The 'p' value is based on the relative
   * error requested.
   *
   * HLL++ requires that we use at least 4 bits of addressing space (a minimum precision of 27%).
   *
   * This method rounds up to the nearest integer. This means that the error is always equal to or
   * lower than the requested error. Use the <code>trueRsd</code> method to get the actual RSD
   * value.
   */
  private[this] val p = Math.ceil(2.0d * Math.log(1.106d / relativeSD) / Math.log(2.0d)).toInt

  require(p >= 4, "HLL++ requires at least 4 bits for addressing. " +
    "Use a lower error, at most 27%.")

  /**
   * Shift used to extract the index of the register from the hashed value.
   *
   * This assumes the use of 64-bit hashcodes.
   */
  private[this] val idxShift = JLong.SIZE - p

  /**
   * Value to pad the 'w' value with before the number of leading zeros is determined.
   */
  private[this] val wPadding = 1L << (p - 1)

  /**
   * The number of registers used.
   */
  private[this] val m = 1 << p

  /**
   * The pre-calculated combination of: alpha * m * m
   *
   * 'alpha' corrects the raw cardinality estimate 'Z'. See the FlFuGaMe07 paper for its
   * derivation.
   */
  private[this] val alphaM2 = p match {
    case 4 => 0.673d * m * m
    case 5 => 0.697d * m * m
    case 6 => 0.709d * m * m
    case _ => (0.7213d / (1.0d + 1.079d / m)) * m * m
  }

  /**
   * The number of words used to store the registers. We use Longs for storage because this is the
   * most compact way of storage; Spark aligns to 8-byte words or uses Long wrappers.
   *
   * We only store whole registers per word in order to prevent overly complex bitwise operations.
   * In practice this means we only use 60 out of 64 bits.
   */
  private[this] val numWords = m / REGISTERS_PER_WORD + 1

  override def children: Seq[Expression] = Seq(child)

  override def nullable: Boolean = false

  override def dataType: DataType = LongType

  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  override def aggBufferSchema: StructType = StructType.fromAttributes(aggBufferAttributes)

  /** Allocate enough words to store all registers. */
  override val aggBufferAttributes: Seq[AttributeReference] = Seq.tabulate(numWords) { i =>
    AttributeReference(s"MS[$i]", LongType)()
  }

  // Note: although this simply copies aggBufferAttributes, this common code can not be placed
  // in the superclass because that will lead to initialization ordering issues.
  override val inputAggBufferAttributes: Seq[AttributeReference] =
    aggBufferAttributes.map(_.newInstance())

  /** Fill all words with zeros. */
  override def initialize(buffer: MutableRow): Unit = {
    var word = 0
    while (word < numWords) {
      buffer.setLong(mutableAggBufferOffset + word, 0)
      word += 1
    }
  }

  /**
   * Update the HLL++ buffer.
   *
   * Variable names in the HLL++ paper match variable names in the code.
   */
  override def update(buffer: MutableRow, input: InternalRow): Unit = {
    val v = child.eval(input)
    if (v != null) {
      // Create the hashed value 'x'.
      val x = MurmurHash.hash64(v)

      // Determine the index of the register we are going to use.
      val idx = (x >>> idxShift).toInt

      // Determine the number of leading zeros in the remaining bits 'w'.
      val pw = JLong.numberOfLeadingZeros((x << p) | wPadding) + 1L

      // Get the word containing the register we are interested in.
      val wordOffset = idx / REGISTERS_PER_WORD
      val word = buffer.getLong(mutableAggBufferOffset + wordOffset)

      // Extract the M[J] register value from the word.
      val shift = REGISTER_SIZE * (idx - (wordOffset * REGISTERS_PER_WORD))
      val mask = REGISTER_WORD_MASK << shift
      val Midx = (word & mask) >>> shift

      // Assign the maximum number of leading zeros to the register.
      if (pw > Midx) {
        buffer.setLong(mutableAggBufferOffset + wordOffset, (word & ~mask) | (pw << shift))
      }
    }
  }

  /**
   * Merge the HLL buffers by iterating through the registers in both buffers and select the
   * maximum number of leading zeros for each register.
   */
  override def merge(buffer1: MutableRow, buffer2: InternalRow): Unit = {
    var idx = 0
    var wordOffset = 0
    while (wordOffset < numWords) {
      val word1 = buffer1.getLong(mutableAggBufferOffset + wordOffset)
      val word2 = buffer2.getLong(inputAggBufferOffset + wordOffset)
      var word = 0L
      var i = 0
      var mask = REGISTER_WORD_MASK
      while (idx < m && i < REGISTERS_PER_WORD) {
        word |= Math.max(word1 & mask, word2 & mask)
        mask <<= REGISTER_SIZE
        i += 1
        idx += 1
      }
      buffer1.setLong(mutableAggBufferOffset + wordOffset, word)
      wordOffset += 1
    }
  }

  /**
   * Estimate the bias using the raw estimates with their respective biases from the HLL++
   * appendix. We currently use KNN interpolation to determine the bias (as suggested in the
   * paper).
   */
  def estimateBias(e: Double): Double = {
    val estimates = RAW_ESTIMATE_DATA(p - 4)
    val numEstimates = estimates.length

    // The estimates are sorted so we can use a binary search to find the index of the
    // interpolation estimate closest to the current estimate.
    val nearestEstimateIndex = util.Arrays.binarySearch(estimates, 0, numEstimates, e) match {
      case ix if ix < 0 => -(ix + 1)
      case ix => ix
    }

    // Use square of the difference between the current estimate and the estimate at the given
    // index as distance metric.
    def distance(i: Int): Double = {
      val diff = e - estimates(i)
      diff * diff
    }

    // Keep moving bounds as long as the the (exclusive) high bound is closer to the estimate than
    // the lower (inclusive) bound.
    var low = math.max(nearestEstimateIndex - K + 1, 0)
    var high = math.min(low + K, numEstimates)
    while (high < numEstimates && distance(high) < distance(low)) {
      low += 1
      high += 1
    }

    // Calculate the sum of the biases in low-high interval.
    val biases = BIAS_DATA(p - 4)
    var i = low
    var biasSum = 0.0
    while (i < high) {
      biasSum += biases(i)
      i += 1
    }

    // Calculate the bias.
    biasSum / (high - low)
  }

  /**
   * Compute the HyperLogLog estimate.
   *
   * Variable names in the HLL++ paper match variable names in the code.
   */
  override def eval(buffer: InternalRow): Any = {
    // Compute the inverse of indicator value 'z' and count the number of zeros 'V'.
    var zInverse = 0.0d
    var V = 0.0d
    var idx = 0
    var wordOffset = 0
    while (wordOffset < numWords) {
      val word = buffer.getLong(mutableAggBufferOffset + wordOffset)
      var i = 0
      var shift = 0
      while (idx < m && i < REGISTERS_PER_WORD) {
        val Midx = (word >>> shift) & REGISTER_WORD_MASK
        zInverse += 1.0 / (1 << Midx)
        if (Midx == 0) {
          V += 1.0d
        }
        shift += REGISTER_SIZE
        i += 1
        idx += 1
      }
      wordOffset += 1
    }

    // We integrate two steps from the paper:
    // val Z = 1.0d / zInverse
    // val E = alphaM2 * Z
    @inline
    def EBiasCorrected = alphaM2 / zInverse match {
      case e if p < 19 && e < 5.0d * m => e - estimateBias(e)
      case e => e
    }

    // Estimate the cardinality.
    val estimate = if (V > 0) {
      // Use linear counting for small cardinality estimates.
      val H = m * Math.log(m / V)
      if (H <= THRESHOLDS(p - 4)) {
        H
      } else {
        EBiasCorrected
      }
    } else {
      EBiasCorrected
    }

    // Round to the nearest long value.
    Math.round(estimate)
  }

  /**
   * The <code>rsd</code> of HLL++ is always equal to or better than the <code>rsd</code> requested.
   * This method returns the <code>rsd</code> this instance actually guarantees.
   *
   * @return the actual <code>rsd</code>.
   */
  def trueRsd: Double = 1.04 / math.sqrt(m)
}

// scalastyle:off
/**
 * Constants used in the implementation of the HyperLogLogPlusPlus aggregate function.
 *
 * See the Appendix to HyperLogLog in Practice: Algorithmic Engineering of a State of the Art
 * Cardinality (https://docs.google.com/document/d/1gyjfMHy43U9OWBXxfaeG-3MjGzejW1dlpyMwEYAAWEI/view?fullscreen)
 * for more information.
 */
// scalastyle:on
object HyperLogLogPlusPlus {
  /**
   * The size of a word used for storing registers: 64 Bits.
   */
  val WORD_SIZE = java.lang.Long.SIZE

  /**
   * The number of bits that is required per register.
   *
   * This number is determined by the maximum number of leading binary zeros a hashcode can
   * produce. This is equal to the number of bits the hashcode returns. The current
   * implementation uses a 64-bit hashcode, this means 6-bits are (at most) needed to store the
   * number of leading zeros.
   */
  val REGISTER_SIZE = 6

  /**
   * Value used to mask a register stored in a word.
   */
  val REGISTER_WORD_MASK: Long = (1 << REGISTER_SIZE) - 1

  /**
   * The number of registers which can be stored in one word.
   */
  val REGISTERS_PER_WORD = WORD_SIZE / REGISTER_SIZE

  /**
   * Number of points used for interpolating the bias value.
   */
  val K = 6

  // Sacrificing style for readability here.
  // scalastyle:off

  /**
   * Thresholds which decide if the linear counting or the regular algorithm is used.
   */
  val THRESHOLDS = Array[Double](10, 20, 40, 80, 220, 400, 900, 1800, 3100, 6500, 15500, 20000, 50000, 120000, 350000)

  /**
   * Lookup table used to find the (index of the) bias correction for a given precision (exact)
   * and estimate (nearest).
   */
  val RAW_ESTIMATE_DATA = Array(
    // precision 4
    Array(11, 11.717, 12.207, 12.7896, 13.2882, 13.8204, 14.3772, 14.9342, 15.5202, 16.161, 16.7722, 17.4636, 18.0396, 18.6766, 19.3566, 20.0454, 20.7936, 21.4856, 22.2666, 22.9946, 23.766, 24.4692, 25.3638, 26.0764, 26.7864, 27.7602, 28.4814, 29.433, 30.2926, 31.0664, 31.9996, 32.7956, 33.5366, 34.5894, 35.5738, 36.2698, 37.3682, 38.0544, 39.2342, 40.0108, 40.7966, 41.9298, 42.8704, 43.6358, 44.5194, 45.773, 46.6772, 47.6174, 48.4888, 49.3304, 50.2506, 51.4996, 52.3824, 53.3078, 54.3984, 55.5838, 56.6618, 57.2174, 58.3514, 59.0802, 60.1482, 61.0376, 62.3598, 62.8078, 63.9744, 64.914, 65.781, 67.1806, 68.0594, 68.8446, 69.7928, 70.8248, 71.8324, 72.8598, 73.6246, 74.7014, 75.393, 76.6708, 77.2394),
    // precision 5
    Array(23, 23.1194, 23.8208, 24.2318, 24.77, 25.2436, 25.7774, 26.2848, 26.8224, 27.3742, 27.9336, 28.503, 29.0494, 29.6292, 30.2124, 30.798, 31.367, 31.9728, 32.5944, 33.217, 33.8438, 34.3696, 35.0956, 35.7044, 36.324, 37.0668, 37.6698, 38.3644, 39.049, 39.6918, 40.4146, 41.082, 41.687, 42.5398, 43.2462, 43.857, 44.6606, 45.4168, 46.1248, 46.9222, 47.6804, 48.447, 49.3454, 49.9594, 50.7636, 51.5776, 52.331, 53.19, 53.9676, 54.7564, 55.5314, 56.4442, 57.3708, 57.9774, 58.9624, 59.8796, 60.755, 61.472, 62.2076, 63.1024, 63.8908, 64.7338, 65.7728, 66.629, 67.413, 68.3266, 69.1524, 70.2642, 71.1806, 72.0566, 72.9192, 73.7598, 74.3516, 75.5802, 76.4386, 77.4916, 78.1524, 79.1892, 79.8414, 80.8798, 81.8376, 82.4698, 83.7656, 84.331, 85.5914, 86.6012, 87.7016, 88.5582, 89.3394, 90.3544, 91.4912, 92.308, 93.3552, 93.9746, 95.2052, 95.727, 97.1322, 98.3944, 98.7588, 100.242, 101.1914, 102.2538, 102.8776, 103.6292, 105.1932, 105.9152, 107.0868, 107.6728, 108.7144, 110.3114, 110.8716, 111.245, 112.7908, 113.7064, 114.636, 115.7464, 116.1788, 117.7464, 118.4896, 119.6166, 120.5082, 121.7798, 122.9028, 123.4426, 124.8854, 125.705, 126.4652, 128.3464, 128.3462, 130.0398, 131.0342, 131.0042, 132.4766, 133.511, 134.7252, 135.425, 136.5172, 138.0572, 138.6694, 139.3712, 140.8598, 141.4594, 142.554, 143.4006, 144.7374, 146.1634, 146.8994, 147.605, 147.9304, 149.1636, 150.2468, 151.5876, 152.2096, 153.7032, 154.7146, 155.807, 156.9228, 157.0372, 158.5852),
    // precision 6
    Array(46, 46.1902, 47.271, 47.8358, 48.8142, 49.2854, 50.317, 51.354, 51.8924, 52.9436, 53.4596, 54.5262, 55.6248, 56.1574, 57.2822, 57.837, 58.9636, 60.074, 60.7042, 61.7976, 62.4772, 63.6564, 64.7942, 65.5004, 66.686, 67.291, 68.5672, 69.8556, 70.4982, 71.8204, 72.4252, 73.7744, 75.0786, 75.8344, 77.0294, 77.8098, 79.0794, 80.5732, 81.1878, 82.5648, 83.2902, 84.6784, 85.3352, 86.8946, 88.3712, 89.0852, 90.499, 91.2686, 92.6844, 94.2234, 94.9732, 96.3356, 97.2286, 98.7262, 100.3284, 101.1048, 102.5962, 103.3562, 105.1272, 106.4184, 107.4974, 109.0822, 109.856, 111.48, 113.2834, 114.0208, 115.637, 116.5174, 118.0576, 119.7476, 120.427, 122.1326, 123.2372, 125.2788, 126.6776, 127.7926, 129.1952, 129.9564, 131.6454, 133.87, 134.5428, 136.2, 137.0294, 138.6278, 139.6782, 141.792, 143.3516, 144.2832, 146.0394, 147.0748, 148.4912, 150.849, 151.696, 153.5404, 154.073, 156.3714, 157.7216, 158.7328, 160.4208, 161.4184, 163.9424, 165.2772, 166.411, 168.1308, 168.769, 170.9258, 172.6828, 173.7502, 175.706, 176.3886, 179.0186, 180.4518, 181.927, 183.4172, 184.4114, 186.033, 188.5124, 189.5564, 191.6008, 192.4172, 193.8044, 194.997, 197.4548, 198.8948, 200.2346, 202.3086, 203.1548, 204.8842, 206.6508, 206.6772, 209.7254, 210.4752, 212.7228, 214.6614, 215.1676, 217.793, 218.0006, 219.9052, 221.66, 223.5588, 225.1636, 225.6882, 227.7126, 229.4502, 231.1978, 232.9756, 233.1654, 236.727, 238.1974, 237.7474, 241.1346, 242.3048, 244.1948, 245.3134, 246.879, 249.1204, 249.853, 252.6792, 253.857, 254.4486, 257.2362, 257.9534, 260.0286, 260.5632, 262.663, 264.723, 265.7566, 267.2566, 267.1624, 270.62, 272.8216, 273.2166, 275.2056, 276.2202, 278.3726, 280.3344, 281.9284, 283.9728, 284.1924, 286.4872, 287.587, 289.807, 291.1206, 292.769, 294.8708, 296.665, 297.1182, 299.4012, 300.6352, 302.1354, 304.1756, 306.1606, 307.3462, 308.5214, 309.4134, 310.8352, 313.9684, 315.837, 316.7796, 318.9858),
    // precision 7
    Array(92, 93.4934, 94.9758, 96.4574, 97.9718, 99.4954, 101.5302, 103.0756, 104.6374, 106.1782, 107.7888, 109.9522, 111.592, 113.2532, 114.9086, 116.5938, 118.9474, 120.6796, 122.4394, 124.2176, 125.9768, 128.4214, 130.2528, 132.0102, 133.8658, 135.7278, 138.3044, 140.1316, 142.093, 144.0032, 145.9092, 148.6306, 150.5294, 152.5756, 154.6508, 156.662, 159.552, 161.3724, 163.617, 165.5754, 167.7872, 169.8444, 172.7988, 174.8606, 177.2118, 179.3566, 181.4476, 184.5882, 186.6816, 189.0824, 191.0258, 193.6048, 196.4436, 198.7274, 200.957, 203.147, 205.4364, 208.7592, 211.3386, 213.781, 215.8028, 218.656, 221.6544, 223.996, 226.4718, 229.1544, 231.6098, 234.5956, 237.0616, 239.5758, 242.4878, 244.5244, 248.2146, 250.724, 252.8722, 255.5198, 258.0414, 261.941, 264.9048, 266.87, 269.4304, 272.028, 274.4708, 278.37, 281.0624, 283.4668, 286.5532, 289.4352, 293.2564, 295.2744, 298.2118, 300.7472, 304.1456, 307.2928, 309.7504, 312.5528, 315.979, 318.2102, 322.1834, 324.3494, 327.325, 330.6614, 332.903, 337.2544, 339.9042, 343.215, 345.2864, 348.0814, 352.6764, 355.301, 357.139, 360.658, 363.1732, 366.5902, 369.9538, 373.0828, 375.922, 378.9902, 382.7328, 386.4538, 388.1136, 391.2234, 394.0878, 396.708, 401.1556, 404.1852, 406.6372, 409.6822, 412.7796, 416.6078, 418.4916, 422.131, 424.5376, 428.1988, 432.211, 434.4502, 438.5282, 440.912, 444.0448, 447.7432, 450.8524, 453.7988, 456.7858, 458.8868, 463.9886, 466.5064, 468.9124, 472.6616, 475.4682, 478.582, 481.304, 485.2738, 488.6894, 490.329, 496.106, 497.6908, 501.1374, 504.5322, 506.8848, 510.3324, 513.4512, 516.179, 520.4412, 522.6066, 526.167, 528.7794, 533.379, 536.067, 538.46, 542.9116, 545.692, 547.9546, 552.493, 555.2722, 557.335, 562.449, 564.2014, 569.0738, 571.0974, 574.8564, 578.2996, 581.409, 583.9704, 585.8098, 589.6528, 594.5998, 595.958, 600.068, 603.3278, 608.2016, 609.9632, 612.864, 615.43, 620.7794, 621.272, 625.8644, 629.206, 633.219, 634.5154, 638.6102),
    // precision 8
    Array(184.2152, 187.2454, 190.2096, 193.6652, 196.6312, 199.6822, 203.249, 206.3296, 210.0038, 213.2074, 216.4612, 220.27, 223.5178, 227.4412, 230.8032, 234.1634, 238.1688, 241.6074, 245.6946, 249.2664, 252.8228, 257.0432, 260.6824, 264.9464, 268.6268, 272.2626, 276.8376, 280.4034, 284.8956, 288.8522, 292.7638, 297.3552, 301.3556, 305.7526, 309.9292, 313.8954, 318.8198, 322.7668, 327.298, 331.6688, 335.9466, 340.9746, 345.1672, 349.3474, 354.3028, 358.8912, 364.114, 368.4646, 372.9744, 378.4092, 382.6022, 387.843, 392.5684, 397.1652, 402.5426, 407.4152, 412.5388, 417.3592, 422.1366, 427.486, 432.3918, 437.5076, 442.509, 447.3834, 453.3498, 458.0668, 463.7346, 469.1228, 473.4528, 479.7, 484.644, 491.0518, 495.5774, 500.9068, 506.432, 512.1666, 517.434, 522.6644, 527.4894, 533.6312, 538.3804, 544.292, 550.5496, 556.0234, 562.8206, 566.6146, 572.4188, 579.117, 583.6762, 590.6576, 595.7864, 601.509, 607.5334, 612.9204, 619.772, 624.2924, 630.8654, 636.1836, 642.745, 649.1316, 655.0386, 660.0136, 666.6342, 671.6196, 678.1866, 684.4282, 689.3324, 695.4794, 702.5038, 708.129, 713.528, 720.3204, 726.463, 732.7928, 739.123, 744.7418, 751.2192, 756.5102, 762.6066, 769.0184, 775.2224, 781.4014, 787.7618, 794.1436, 798.6506, 805.6378, 811.766, 819.7514, 824.5776, 828.7322, 837.8048, 843.6302, 849.9336, 854.4798, 861.3388, 867.9894, 873.8196, 880.3136, 886.2308, 892.4588, 899.0816, 905.4076, 912.0064, 917.3878, 923.619, 929.998, 937.3482, 943.9506, 947.991, 955.1144, 962.203, 968.8222, 975.7324, 981.7826, 988.7666, 994.2648, 1000.3128, 1007.4082, 1013.7536, 1020.3376, 1026.7156, 1031.7478, 1037.4292, 1045.393, 1051.2278, 1058.3434, 1062.8726, 1071.884, 1076.806, 1082.9176, 1089.1678, 1095.5032, 1102.525, 1107.2264, 1115.315, 1120.93, 1127.252, 1134.1496, 1139.0408, 1147.5448, 1153.3296, 1158.1974, 1166.5262, 1174.3328, 1175.657, 1184.4222, 1190.9172, 1197.1292, 1204.4606, 1210.4578, 1218.8728, 1225.3336, 1226.6592, 1236.5768, 1241.363, 1249.4074, 1254.6566, 1260.8014, 1266.5454, 1274.5192),
    // precision 9
    Array(369, 374.8294, 381.2452, 387.6698, 394.1464, 400.2024, 406.8782, 413.6598, 420.462, 427.2826, 433.7102, 440.7416, 447.9366, 455.1046, 462.285, 469.0668, 476.306, 483.8448, 491.301, 498.9886, 506.2422, 513.8138, 521.7074, 529.7428, 537.8402, 545.1664, 553.3534, 561.594, 569.6886, 577.7876, 585.65, 594.228, 602.8036, 611.1666, 620.0818, 628.0824, 637.2574, 646.302, 655.1644, 664.0056, 672.3802, 681.7192, 690.5234, 700.2084, 708.831, 718.485, 728.1112, 737.4764, 746.76, 756.3368, 766.5538, 775.5058, 785.2646, 795.5902, 804.3818, 814.8998, 824.9532, 835.2062, 845.2798, 854.4728, 864.9582, 875.3292, 886.171, 896.781, 906.5716, 916.7048, 927.5322, 937.875, 949.3972, 958.3464, 969.7274, 980.2834, 992.1444, 1003.4264, 1013.0166, 1024.018, 1035.0438, 1046.34, 1057.6856, 1068.9836, 1079.0312, 1091.677, 1102.3188, 1113.4846, 1124.4424, 1135.739, 1147.1488, 1158.9202, 1169.406, 1181.5342, 1193.2834, 1203.8954, 1216.3286, 1226.2146, 1239.6684, 1251.9946, 1262.123, 1275.4338, 1285.7378, 1296.076, 1308.9692, 1320.4964, 1333.0998, 1343.9864, 1357.7754, 1368.3208, 1380.4838, 1392.7388, 1406.0758, 1416.9098, 1428.9728, 1440.9228, 1453.9292, 1462.617, 1476.05, 1490.2996, 1500.6128, 1513.7392, 1524.5174, 1536.6322, 1548.2584, 1562.3766, 1572.423, 1587.1232, 1596.5164, 1610.5938, 1622.5972, 1633.1222, 1647.7674, 1658.5044, 1671.57, 1683.7044, 1695.4142, 1708.7102, 1720.6094, 1732.6522, 1747.841, 1756.4072, 1769.9786, 1782.3276, 1797.5216, 1808.3186, 1819.0694, 1834.354, 1844.575, 1856.2808, 1871.1288, 1880.7852, 1893.9622, 1906.3418, 1920.6548, 1932.9302, 1945.8584, 1955.473, 1968.8248, 1980.6446, 1995.9598, 2008.349, 2019.8556, 2033.0334, 2044.0206, 2059.3956, 2069.9174, 2082.6084, 2093.7036, 2106.6108, 2118.9124, 2132.301, 2144.7628, 2159.8422, 2171.0212, 2183.101, 2193.5112, 2208.052, 2221.3194, 2233.3282, 2247.295, 2257.7222, 2273.342, 2286.5638, 2299.6786, 2310.8114, 2322.3312, 2335.516, 2349.874, 2363.5968, 2373.865, 2387.1918, 2401.8328, 2414.8496, 2424.544, 2436.7592, 2447.1682, 2464.1958, 2474.3438, 2489.0006, 2497.4526, 2513.6586, 2527.19, 2540.7028, 2553.768),
    // precision 10
    Array(738.1256, 750.4234, 763.1064, 775.4732, 788.4636, 801.0644, 814.488, 827.9654, 841.0832, 854.7864, 868.1992, 882.2176, 896.5228, 910.1716, 924.7752, 938.899, 953.6126, 968.6492, 982.9474, 998.5214, 1013.1064, 1028.6364, 1044.2468, 1059.4588, 1075.3832, 1091.0584, 1106.8606, 1123.3868, 1139.5062, 1156.1862, 1172.463, 1189.339, 1206.1936, 1223.1292, 1240.1854, 1257.2908, 1275.3324, 1292.8518, 1310.5204, 1328.4854, 1345.9318, 1364.552, 1381.4658, 1400.4256, 1419.849, 1438.152, 1456.8956, 1474.8792, 1494.118, 1513.62, 1532.5132, 1551.9322, 1570.7726, 1590.6086, 1610.5332, 1630.5918, 1650.4294, 1669.7662, 1690.4106, 1710.7338, 1730.9012, 1750.4486, 1770.1556, 1791.6338, 1812.7312, 1833.6264, 1853.9526, 1874.8742, 1896.8326, 1918.1966, 1939.5594, 1961.07, 1983.037, 2003.1804, 2026.071, 2047.4884, 2070.0848, 2091.2944, 2114.333, 2135.9626, 2158.2902, 2181.0814, 2202.0334, 2224.4832, 2246.39, 2269.7202, 2292.1714, 2314.2358, 2338.9346, 2360.891, 2384.0264, 2408.3834, 2430.1544, 2454.8684, 2476.9896, 2501.4368, 2522.8702, 2548.0408, 2570.6738, 2593.5208, 2617.0158, 2640.2302, 2664.0962, 2687.4986, 2714.2588, 2735.3914, 2759.6244, 2781.8378, 2808.0072, 2830.6516, 2856.2454, 2877.2136, 2903.4546, 2926.785, 2951.2294, 2976.468, 3000.867, 3023.6508, 3049.91, 3073.5984, 3098.162, 3121.5564, 3146.2328, 3170.9484, 3195.5902, 3221.3346, 3242.7032, 3271.6112, 3296.5546, 3317.7376, 3345.072, 3369.9518, 3394.326, 3418.1818, 3444.6926, 3469.086, 3494.2754, 3517.8698, 3544.248, 3565.3768, 3588.7234, 3616.979, 3643.7504, 3668.6812, 3695.72, 3719.7392, 3742.6224, 3770.4456, 3795.6602, 3819.9058, 3844.002, 3869.517, 3895.6824, 3920.8622, 3947.1364, 3973.985, 3995.4772, 4021.62, 4046.628, 4074.65, 4096.2256, 4121.831, 4146.6406, 4173.276, 4195.0744, 4223.9696, 4251.3708, 4272.9966, 4300.8046, 4326.302, 4353.1248, 4374.312, 4403.0322, 4426.819, 4450.0598, 4478.5206, 4504.8116, 4528.8928, 4553.9584, 4578.8712, 4603.8384, 4632.3872, 4655.5128, 4675.821, 4704.6222, 4731.9862, 4755.4174, 4781.2628, 4804.332, 4832.3048, 4862.8752, 4883.4148, 4906.9544, 4935.3516, 4954.3532, 4984.0248, 5011.217, 5035.3258, 5057.3672, 5084.1828),
    // precision 11
    Array(1477, 1501.6014, 1526.5802, 1551.7942, 1577.3042, 1603.2062, 1629.8402, 1656.2292, 1682.9462, 1709.9926, 1737.3026, 1765.4252, 1793.0578, 1821.6092, 1849.626, 1878.5568, 1908.527, 1937.5154, 1967.1874, 1997.3878, 2027.37, 2058.1972, 2089.5728, 2120.1012, 2151.9668, 2183.292, 2216.0772, 2247.8578, 2280.6562, 2313.041, 2345.714, 2380.3112, 2414.1806, 2447.9854, 2481.656, 2516.346, 2551.5154, 2586.8378, 2621.7448, 2656.6722, 2693.5722, 2729.1462, 2765.4124, 2802.8728, 2838.898, 2876.408, 2913.4926, 2951.4938, 2989.6776, 3026.282, 3065.7704, 3104.1012, 3143.7388, 3181.6876, 3221.1872, 3261.5048, 3300.0214, 3339.806, 3381.409, 3421.4144, 3461.4294, 3502.2286, 3544.651, 3586.6156, 3627.337, 3670.083, 3711.1538, 3753.5094, 3797.01, 3838.6686, 3882.1678, 3922.8116, 3967.9978, 4009.9204, 4054.3286, 4097.5706, 4140.6014, 4185.544, 4229.5976, 4274.583, 4316.9438, 4361.672, 4406.2786, 4451.8628, 4496.1834, 4543.505, 4589.1816, 4632.5188, 4678.2294, 4724.8908, 4769.0194, 4817.052, 4861.4588, 4910.1596, 4956.4344, 5002.5238, 5048.13, 5093.6374, 5142.8162, 5187.7894, 5237.3984, 5285.6078, 5331.0858, 5379.1036, 5428.6258, 5474.6018, 5522.7618, 5571.5822, 5618.59, 5667.9992, 5714.88, 5763.454, 5808.6982, 5860.3644, 5910.2914, 5953.571, 6005.9232, 6055.1914, 6104.5882, 6154.5702, 6199.7036, 6251.1764, 6298.7596, 6350.0302, 6398.061, 6448.4694, 6495.933, 6548.0474, 6597.7166, 6646.9416, 6695.9208, 6742.6328, 6793.5276, 6842.1934, 6894.2372, 6945.3864, 6996.9228, 7044.2372, 7094.1374, 7142.2272, 7192.2942, 7238.8338, 7288.9006, 7344.0908, 7394.8544, 7443.5176, 7490.4148, 7542.9314, 7595.6738, 7641.9878, 7694.3688, 7743.0448, 7797.522, 7845.53, 7899.594, 7950.3132, 7996.455, 8050.9442, 8092.9114, 8153.1374, 8197.4472, 8252.8278, 8301.8728, 8348.6776, 8401.4698, 8453.551, 8504.6598, 8553.8944, 8604.1276, 8657.6514, 8710.3062, 8758.908, 8807.8706, 8862.1702, 8910.4668, 8960.77, 9007.2766, 9063.164, 9121.0534, 9164.1354, 9218.1594, 9267.767, 9319.0594, 9372.155, 9419.7126, 9474.3722, 9520.1338, 9572.368, 9622.7702, 9675.8448, 9726.5396, 9778.7378, 9827.6554, 9878.1922, 9928.7782, 9978.3984, 10026.578, 10076.5626, 10137.1618, 10177.5244, 10229.9176),
    // precision 12
    Array(2954, 3003.4782, 3053.3568, 3104.3666, 3155.324, 3206.9598, 3259.648, 3312.539, 3366.1474, 3420.2576, 3474.8376, 3530.6076, 3586.451, 3643.38, 3700.4104, 3757.5638, 3815.9676, 3875.193, 3934.838, 3994.8548, 4055.018, 4117.1742, 4178.4482, 4241.1294, 4304.4776, 4367.4044, 4431.8724, 4496.3732, 4561.4304, 4627.5326, 4693.949, 4761.5532, 4828.7256, 4897.6182, 4965.5186, 5034.4528, 5104.865, 5174.7164, 5244.6828, 5316.6708, 5387.8312, 5459.9036, 5532.476, 5604.8652, 5679.6718, 5753.757, 5830.2072, 5905.2828, 5980.0434, 6056.6264, 6134.3192, 6211.5746, 6290.0816, 6367.1176, 6447.9796, 6526.5576, 6606.1858, 6686.9144, 6766.1142, 6847.0818, 6927.9664, 7010.9096, 7091.0816, 7175.3962, 7260.3454, 7344.018, 7426.4214, 7511.3106, 7596.0686, 7679.8094, 7765.818, 7852.4248, 7936.834, 8022.363, 8109.5066, 8200.4554, 8288.5832, 8373.366, 8463.4808, 8549.7682, 8642.0522, 8728.3288, 8820.9528, 8907.727, 9001.0794, 9091.2522, 9179.988, 9269.852, 9362.6394, 9453.642, 9546.9024, 9640.6616, 9732.6622, 9824.3254, 9917.7484, 10007.9392, 10106.7508, 10196.2152, 10289.8114, 10383.5494, 10482.3064, 10576.8734, 10668.7872, 10764.7156, 10862.0196, 10952.793, 11049.9748, 11146.0702, 11241.4492, 11339.2772, 11434.2336, 11530.741, 11627.6136, 11726.311, 11821.5964, 11918.837, 12015.3724, 12113.0162, 12213.0424, 12306.9804, 12408.4518, 12504.8968, 12604.586, 12700.9332, 12798.705, 12898.5142, 12997.0488, 13094.788, 13198.475, 13292.7764, 13392.9698, 13486.8574, 13590.1616, 13686.5838, 13783.6264, 13887.2638, 13992.0978, 14081.0844, 14189.9956, 14280.0912, 14382.4956, 14486.4384, 14588.1082, 14686.2392, 14782.276, 14888.0284, 14985.1864, 15088.8596, 15187.0998, 15285.027, 15383.6694, 15495.8266, 15591.3736, 15694.2008, 15790.3246, 15898.4116, 15997.4522, 16095.5014, 16198.8514, 16291.7492, 16402.6424, 16499.1266, 16606.2436, 16697.7186, 16796.3946, 16902.3376, 17005.7672, 17100.814, 17206.8282, 17305.8262, 17416.0744, 17508.4092, 17617.0178, 17715.4554, 17816.758, 17920.1748, 18012.9236, 18119.7984, 18223.2248, 18324.2482, 18426.6276, 18525.0932, 18629.8976, 18733.2588, 18831.0466, 18940.1366, 19032.2696, 19131.729, 19243.4864, 19349.6932, 19442.866, 19547.9448, 19653.2798, 19754.4034, 19854.0692, 19965.1224, 20065.1774, 20158.2212, 20253.353, 20366.3264, 20463.22),
    // precision 13
    Array(5908.5052, 6007.2672, 6107.347, 6208.5794, 6311.2622, 6414.5514, 6519.3376, 6625.6952, 6732.5988, 6841.3552, 6950.5972, 7061.3082, 7173.5646, 7287.109, 7401.8216, 7516.4344, 7633.3802, 7751.2962, 7870.3784, 7990.292, 8110.79, 8233.4574, 8356.6036, 8482.2712, 8607.7708, 8735.099, 8863.1858, 8993.4746, 9123.8496, 9255.6794, 9388.5448, 9522.7516, 9657.3106, 9792.6094, 9930.5642, 10068.794, 10206.7256, 10347.81, 10490.3196, 10632.0778, 10775.9916, 10920.4662, 11066.124, 11213.073, 11358.0362, 11508.1006, 11659.1716, 11808.7514, 11959.4884, 12112.1314, 12265.037, 12420.3756, 12578.933, 12734.311, 12890.0006, 13047.2144, 13207.3096, 13368.5144, 13528.024, 13689.847, 13852.7528, 14018.3168, 14180.5372, 14346.9668, 14513.5074, 14677.867, 14846.2186, 15017.4186, 15184.9716, 15356.339, 15529.2972, 15697.3578, 15871.8686, 16042.187, 16216.4094, 16389.4188, 16565.9126, 16742.3272, 16919.0042, 17094.7592, 17273.965, 17451.8342, 17634.4254, 17810.5984, 17988.9242, 18171.051, 18354.7938, 18539.466, 18721.0408, 18904.9972, 19081.867, 19271.9118, 19451.8694, 19637.9816, 19821.2922, 20013.1292, 20199.3858, 20387.8726, 20572.9514, 20770.7764, 20955.1714, 21144.751, 21329.9952, 21520.709, 21712.7016, 21906.3868, 22096.2626, 22286.0524, 22475.051, 22665.5098, 22862.8492, 23055.5294, 23249.6138, 23437.848, 23636.273, 23826.093, 24020.3296, 24213.3896, 24411.7392, 24602.9614, 24805.7952, 24998.1552, 25193.9588, 25389.0166, 25585.8392, 25780.6976, 25981.2728, 26175.977, 26376.5252, 26570.1964, 26773.387, 26962.9812, 27163.0586, 27368.164, 27565.0534, 27758.7428, 27961.1276, 28163.2324, 28362.3816, 28565.7668, 28758.644, 28956.9768, 29163.4722, 29354.7026, 29561.1186, 29767.9948, 29959.9986, 30164.0492, 30366.9818, 30562.5338, 30762.9928, 30976.1592, 31166.274, 31376.722, 31570.3734, 31770.809, 31974.8934, 32179.5286, 32387.5442, 32582.3504, 32794.076, 32989.9528, 33191.842, 33392.4684, 33595.659, 33801.8672, 34000.3414, 34200.0922, 34402.6792, 34610.0638, 34804.0084, 35011.13, 35218.669, 35418.6634, 35619.0792, 35830.6534, 36028.4966, 36229.7902, 36438.6422, 36630.7764, 36833.3102, 37048.6728, 37247.3916, 37453.5904, 37669.3614, 37854.5526, 38059.305, 38268.0936, 38470.2516, 38674.7064, 38876.167, 39068.3794, 39281.9144, 39492.8566, 39684.8628, 39898.4108, 40093.1836, 40297.6858, 40489.7086, 40717.2424),
    // precision 14
    Array(11817.475, 12015.0046, 12215.3792, 12417.7504, 12623.1814, 12830.0086, 13040.0072, 13252.503, 13466.178, 13683.2738, 13902.0344, 14123.9798, 14347.394, 14573.7784, 14802.6894, 15033.6824, 15266.9134, 15502.8624, 15741.4944, 15980.7956, 16223.8916, 16468.6316, 16715.733, 16965.5726, 17217.204, 17470.666, 17727.8516, 17986.7886, 18247.6902, 18510.9632, 18775.304, 19044.7486, 19314.4408, 19587.202, 19862.2576, 20135.924, 20417.0324, 20697.9788, 20979.6112, 21265.0274, 21550.723, 21841.6906, 22132.162, 22428.1406, 22722.127, 23020.5606, 23319.7394, 23620.4014, 23925.2728, 24226.9224, 24535.581, 24845.505, 25155.9618, 25470.3828, 25785.9702, 26103.7764, 26420.4132, 26742.0186, 27062.8852, 27388.415, 27714.6024, 28042.296, 28365.4494, 28701.1526, 29031.8008, 29364.2156, 29704.497, 30037.1458, 30380.111, 30723.8168, 31059.5114, 31404.9498, 31751.6752, 32095.2686, 32444.7792, 32794.767, 33145.204, 33498.4226, 33847.6502, 34209.006, 34560.849, 34919.4838, 35274.9778, 35635.1322, 35996.3266, 36359.1394, 36722.8266, 37082.8516, 37447.7354, 37815.9606, 38191.0692, 38559.4106, 38924.8112, 39294.6726, 39663.973, 40042.261, 40416.2036, 40779.2036, 41161.6436, 41540.9014, 41921.1998, 42294.7698, 42678.5264, 43061.3464, 43432.375, 43818.432, 44198.6598, 44583.0138, 44970.4794, 45353.924, 45729.858, 46118.2224, 46511.5724, 46900.7386, 47280.6964, 47668.1472, 48055.6796, 48446.9436, 48838.7146, 49217.7296, 49613.7796, 50010.7508, 50410.0208, 50793.7886, 51190.2456, 51583.1882, 51971.0796, 52376.5338, 52763.319, 53165.5534, 53556.5594, 53948.2702, 54346.352, 54748.7914, 55138.577, 55543.4824, 55941.1748, 56333.7746, 56745.1552, 57142.7944, 57545.2236, 57935.9956, 58348.5268, 58737.5474, 59158.5962, 59542.6896, 59958.8004, 60349.3788, 60755.0212, 61147.6144, 61548.194, 61946.0696, 62348.6042, 62763.603, 63162.781, 63560.635, 63974.3482, 64366.4908, 64771.5876, 65176.7346, 65597.3916, 65995.915, 66394.0384, 66822.9396, 67203.6336, 67612.2032, 68019.0078, 68420.0388, 68821.22, 69235.8388, 69640.0724, 70055.155, 70466.357, 70863.4266, 71276.2482, 71677.0306, 72080.2006, 72493.0214, 72893.5952, 73314.5856, 73714.9852, 74125.3022, 74521.2122, 74933.6814, 75341.5904, 75743.0244, 76166.0278, 76572.1322, 76973.1028, 77381.6284, 77800.6092, 78189.328, 78607.0962, 79012.2508, 79407.8358, 79825.725, 80238.701, 80646.891, 81035.6436, 81460.0448, 81876.3884),
    // precision 15
    Array(23635.0036, 24030.8034, 24431.4744, 24837.1524, 25246.7928, 25661.326, 26081.3532, 26505.2806, 26933.9892, 27367.7098, 27805.318, 28248.799, 28696.4382, 29148.8244, 29605.5138, 30066.8668, 30534.2344, 31006.32, 31480.778, 31962.2418, 32447.3324, 32938.0232, 33432.731, 33930.728, 34433.9896, 34944.1402, 35457.5588, 35974.5958, 36497.3296, 37021.9096, 37554.326, 38088.0826, 38628.8816, 39171.3192, 39723.2326, 40274.5554, 40832.3142, 41390.613, 41959.5908, 42532.5466, 43102.0344, 43683.5072, 44266.694, 44851.2822, 45440.7862, 46038.0586, 46640.3164, 47241.064, 47846.155, 48454.7396, 49076.9168, 49692.542, 50317.4778, 50939.65, 51572.5596, 52210.2906, 52843.7396, 53481.3996, 54127.236, 54770.406, 55422.6598, 56078.7958, 56736.7174, 57397.6784, 58064.5784, 58730.308, 59404.9784, 60077.0864, 60751.9158, 61444.1386, 62115.817, 62808.7742, 63501.4774, 64187.5454, 64883.6622, 65582.7468, 66274.5318, 66976.9276, 67688.7764, 68402.138, 69109.6274, 69822.9706, 70543.6108, 71265.5202, 71983.3848, 72708.4656, 73433.384, 74158.4664, 74896.4868, 75620.9564, 76362.1434, 77098.3204, 77835.7662, 78582.6114, 79323.9902, 80067.8658, 80814.9246, 81567.0136, 82310.8536, 83061.9952, 83821.4096, 84580.8608, 85335.547, 86092.5802, 86851.6506, 87612.311, 88381.2016, 89146.3296, 89907.8974, 90676.846, 91451.4152, 92224.5518, 92995.8686, 93763.5066, 94551.2796, 95315.1944, 96096.1806, 96881.0918, 97665.679, 98442.68, 99229.3002, 100011.0994, 100790.6386, 101580.1564, 102377.7484, 103152.1392, 103944.2712, 104730.216, 105528.6336, 106324.9398, 107117.6706, 107890.3988, 108695.2266, 109485.238, 110294.7876, 111075.0958, 111878.0496, 112695.2864, 113464.5486, 114270.0474, 115068.608, 115884.3626, 116673.2588, 117483.3716, 118275.097, 119085.4092, 119879.2808, 120687.5868, 121499.9944, 122284.916, 123095.9254, 123912.5038, 124709.0454, 125503.7182, 126323.259, 127138.9412, 127943.8294, 128755.646, 129556.5354, 130375.3298, 131161.4734, 131971.1962, 132787.5458, 133588.1056, 134431.351, 135220.2906, 136023.398, 136846.6558, 137667.0004, 138463.663, 139283.7154, 140074.6146, 140901.3072, 141721.8548, 142543.2322, 143356.1096, 144173.7412, 144973.0948, 145794.3162, 146609.5714, 147420.003, 148237.9784, 149050.5696, 149854.761, 150663.1966, 151494.0754, 152313.1416, 153112.6902, 153935.7206, 154746.9262, 155559.547, 156401.9746, 157228.7036, 158008.7254, 158820.75, 159646.9184, 160470.4458, 161279.5348, 162093.3114, 162918.542, 163729.2842),
    // precision 16
    Array(47271, 48062.3584, 48862.7074, 49673.152, 50492.8416, 51322.9514, 52161.03, 53009.407, 53867.6348, 54734.206, 55610.5144, 56496.2096, 57390.795, 58297.268, 59210.6448, 60134.665, 61068.0248, 62010.4472, 62962.5204, 63923.5742, 64895.0194, 65876.4182, 66862.6136, 67862.6968, 68868.8908, 69882.8544, 70911.271, 71944.0924, 72990.0326, 74040.692, 75100.6336, 76174.7826, 77252.5998, 78340.2974, 79438.2572, 80545.4976, 81657.2796, 82784.6336, 83915.515, 85059.7362, 86205.9368, 87364.4424, 88530.3358, 89707.3744, 90885.9638, 92080.197, 93275.5738, 94479.391, 95695.918, 96919.2236, 98148.4602, 99382.3474, 100625.6974, 101878.0284, 103141.6278, 104409.4588, 105686.2882, 106967.5402, 108261.6032, 109548.1578, 110852.0728, 112162.231, 113479.0072, 114806.2626, 116137.9072, 117469.5048, 118813.5186, 120165.4876, 121516.2556, 122875.766, 124250.5444, 125621.2222, 127003.2352, 128387.848, 129775.2644, 131181.7776, 132577.3086, 133979.9458, 135394.1132, 136800.9078, 138233.217, 139668.5308, 141085.212, 142535.2122, 143969.0684, 145420.2872, 146878.1542, 148332.7572, 149800.3202, 151269.66, 152743.6104, 154213.0948, 155690.288, 157169.4246, 158672.1756, 160160.059, 161650.6854, 163145.7772, 164645.6726, 166159.1952, 167682.1578, 169177.3328, 170700.0118, 172228.8964, 173732.6664, 175265.5556, 176787.799, 178317.111, 179856.6914, 181400.865, 182943.4612, 184486.742, 186033.4698, 187583.7886, 189148.1868, 190688.4526, 192250.1926, 193810.9042, 195354.2972, 196938.7682, 198493.5898, 200079.2824, 201618.912, 203205.5492, 204765.5798, 206356.1124, 207929.3064, 209498.7196, 211086.229, 212675.1324, 214256.7892, 215826.2392, 217412.8474, 218995.6724, 220618.6038, 222207.1166, 223781.0364, 225387.4332, 227005.7928, 228590.4336, 230217.8738, 231805.1054, 233408.9, 234995.3432, 236601.4956, 238190.7904, 239817.2548, 241411.2832, 243002.4066, 244640.1884, 246255.3128, 247849.3508, 249479.9734, 251106.8822, 252705.027, 254332.9242, 255935.129, 257526.9014, 259154.772, 260777.625, 262390.253, 264004.4906, 265643.59, 267255.4076, 268873.426, 270470.7252, 272106.4804, 273722.4456, 275337.794, 276945.7038, 278592.9154, 280204.3726, 281841.1606, 283489.171, 285130.1716, 286735.3362, 288364.7164, 289961.1814, 291595.5524, 293285.683, 294899.6668, 296499.3434, 298128.0462, 299761.8946, 301394.2424, 302997.6748, 304615.1478, 306269.7724, 307886.114, 309543.1028, 311153.2862, 312782.8546, 314421.2008, 316033.2438, 317692.9636, 319305.2648, 320948.7406, 322566.3364, 324228.4224, 325847.1542),
    // precision 17
    Array(94542, 96125.811, 97728.019, 99348.558, 100987.9705, 102646.7565, 104324.5125, 106021.7435, 107736.7865, 109469.272, 111223.9465, 112995.219, 114787.432, 116593.152, 118422.71, 120267.2345, 122134.6765, 124020.937, 125927.2705, 127851.255, 129788.9485, 131751.016, 133726.8225, 135722.592, 137736.789, 139770.568, 141821.518, 143891.343, 145982.1415, 148095.387, 150207.526, 152355.649, 154515.6415, 156696.05, 158887.7575, 161098.159, 163329.852, 165569.053, 167837.4005, 170121.6165, 172420.4595, 174732.6265, 177062.77, 179412.502, 181774.035, 184151.939, 186551.6895, 188965.691, 191402.8095, 193857.949, 196305.0775, 198774.6715, 201271.2585, 203764.78, 206299.3695, 208818.1365, 211373.115, 213946.7465, 216532.076, 219105.541, 221714.5375, 224337.5135, 226977.5125, 229613.0655, 232270.2685, 234952.2065, 237645.3555, 240331.1925, 243034.517, 245756.0725, 248517.6865, 251232.737, 254011.3955, 256785.995, 259556.44, 262368.335, 265156.911, 267965.266, 270785.583, 273616.0495, 276487.4835, 279346.639, 282202.509, 285074.3885, 287942.2855, 290856.018, 293774.0345, 296678.5145, 299603.6355, 302552.6575, 305492.9785, 308466.8605, 311392.581, 314347.538, 317319.4295, 320285.9785, 323301.7325, 326298.3235, 329301.3105, 332301.987, 335309.791, 338370.762, 341382.923, 344431.1265, 347464.1545, 350507.28, 353619.2345, 356631.2005, 359685.203, 362776.7845, 365886.488, 368958.2255, 372060.6825, 375165.4335, 378237.935, 381328.311, 384430.5225, 387576.425, 390683.242, 393839.648, 396977.8425, 400101.9805, 403271.296, 406409.8425, 409529.5485, 412678.7, 415847.423, 419020.8035, 422157.081, 425337.749, 428479.6165, 431700.902, 434893.1915, 438049.582, 441210.5415, 444379.2545, 447577.356, 450741.931, 453959.548, 457137.0935, 460329.846, 463537.4815, 466732.3345, 469960.5615, 473164.681, 476347.6345, 479496.173, 482813.1645, 486025.6995, 489249.4885, 492460.1945, 495675.8805, 498908.0075, 502131.802, 505374.3855, 508550.9915, 511806.7305, 515026.776, 518217.0005, 521523.9855, 524705.9855, 527950.997, 531210.0265, 534472.497, 537750.7315, 540926.922, 544207.094, 547429.4345, 550666.3745, 553975.3475, 557150.7185, 560399.6165, 563662.697, 566916.7395, 570146.1215, 573447.425, 576689.6245, 579874.5745, 583202.337, 586503.0255, 589715.635, 592910.161, 596214.3885, 599488.035, 602740.92, 605983.0685, 609248.67, 612491.3605, 615787.912, 619107.5245, 622307.9555, 625577.333, 628840.4385, 632085.2155, 635317.6135, 638691.7195, 641887.467, 645139.9405, 648441.546, 651666.252, 654941.845),
    // precision 18
    Array(189084, 192250.913, 195456.774, 198696.946, 201977.762, 205294.444, 208651.754, 212042.099, 215472.269, 218941.91, 222443.912, 225996.845, 229568.199, 233193.568, 236844.457, 240543.233, 244279.475, 248044.27, 251854.588, 255693.2, 259583.619, 263494.621, 267445.385, 271454.061, 275468.769, 279549.456, 283646.446, 287788.198, 291966.099, 296181.164, 300431.469, 304718.618, 309024.004, 313393.508, 317760.803, 322209.731, 326675.061, 331160.627, 335654.47, 340241.442, 344841.833, 349467.132, 354130.629, 358819.432, 363574.626, 368296.587, 373118.482, 377914.93, 382782.301, 387680.669, 392601.981, 397544.323, 402529.115, 407546.018, 412593.658, 417638.657, 422762.865, 427886.169, 433017.167, 438213.273, 443441.254, 448692.421, 453937.533, 459239.049, 464529.569, 469910.083, 475274.03, 480684.473, 486070.26, 491515.237, 496995.651, 502476.617, 507973.609, 513497.19, 519083.233, 524726.509, 530305.505, 535945.728, 541584.404, 547274.055, 552967.236, 558667.862, 564360.216, 570128.148, 575965.08, 581701.952, 587532.523, 593361.144, 599246.128, 605033.418, 610958.779, 616837.117, 622772.818, 628672.04, 634675.369, 640574.831, 646585.739, 652574.547, 658611.217, 664642.684, 670713.914, 676737.681, 682797.313, 688837.897, 694917.874, 701009.882, 707173.648, 713257.254, 719415.392, 725636.761, 731710.697, 737906.209, 744103.074, 750313.39, 756504.185, 762712.579, 768876.985, 775167.859, 781359, 787615.959, 793863.597, 800245.477, 806464.582, 812785.294, 819005.925, 825403.057, 831676.197, 837936.284, 844266.968, 850642.711, 856959.756, 863322.774, 869699.931, 876102.478, 882355.787, 888694.463, 895159.952, 901536.143, 907872.631, 914293.672, 920615.14, 927130.974, 933409.404, 939922.178, 946331.47, 952745.93, 959209.264, 965590.224, 972077.284, 978501.961, 984953.19, 991413.271, 997817.479, 1004222.658, 1010725.676, 1017177.138, 1023612.529, 1030098.236, 1036493.719, 1043112.207, 1049537.036, 1056008.096, 1062476.184, 1068942.337, 1075524.95, 1081932.864, 1088426.025, 1094776.005, 1101327.448, 1107901.673, 1114423.639, 1120884.602, 1127324.923, 1133794.24, 1140328.886, 1146849.376, 1153346.682, 1159836.502, 1166478.703, 1172953.304, 1179391.502, 1185950.982, 1192544.052, 1198913.41, 1205430.994, 1212015.525, 1218674.042, 1225121.683, 1231551.101, 1238126.379, 1244673.795, 1251260.649, 1257697.86, 1264320.983, 1270736.319, 1277274.694, 1283804.95, 1290211.514, 1296858.568, 1303455.691)
  )

  /**
   * Bias corrections given a precision and the index of the raw estimate table.
   */
  val BIAS_DATA = Array(
    // precision 4
    Array(10, 9.717, 9.207, 8.7896, 8.2882, 7.8204, 7.3772, 6.9342, 6.5202, 6.161, 5.7722, 5.4636, 5.0396, 4.6766, 4.3566, 4.0454, 3.7936, 3.4856, 3.2666, 2.9946, 2.766, 2.4692, 2.3638, 2.0764, 1.7864, 1.7602, 1.4814, 1.433, 1.2926, 1.0664, 0.999600000000001, 0.7956, 0.5366, 0.589399999999998, 0.573799999999999, 0.269799999999996, 0.368200000000002, 0.0544000000000011, 0.234200000000001, 0.0108000000000033, -0.203400000000002, -0.0701999999999998, -0.129600000000003, -0.364199999999997, -0.480600000000003, -0.226999999999997, -0.322800000000001, -0.382599999999996, -0.511200000000002, -0.669600000000003, -0.749400000000001, -0.500399999999999, -0.617600000000003, -0.6922, -0.601599999999998, -0.416200000000003, -0.338200000000001, -0.782600000000002, -0.648600000000002, -0.919800000000002, -0.851799999999997, -0.962400000000002, -0.6402, -1.1922, -1.0256, -1.086, -1.21899999999999, -0.819400000000002, -0.940600000000003, -1.1554, -1.2072, -1.1752, -1.16759999999999, -1.14019999999999, -1.3754, -1.29859999999999, -1.607, -1.3292, -1.7606),
    // precision 5
    Array(22, 21.1194, 20.8208, 20.2318, 19.77, 19.2436, 18.7774, 18.2848, 17.8224, 17.3742, 16.9336, 16.503, 16.0494, 15.6292, 15.2124, 14.798, 14.367, 13.9728, 13.5944, 13.217, 12.8438, 12.3696, 12.0956, 11.7044, 11.324, 11.0668, 10.6698, 10.3644, 10.049, 9.6918, 9.4146, 9.082, 8.687, 8.5398, 8.2462, 7.857, 7.6606, 7.4168, 7.1248, 6.9222, 6.6804, 6.447, 6.3454, 5.9594, 5.7636, 5.5776, 5.331, 5.19, 4.9676, 4.7564, 4.5314, 4.4442, 4.3708, 3.9774, 3.9624, 3.8796, 3.755, 3.472, 3.2076, 3.1024, 2.8908, 2.7338, 2.7728, 2.629, 2.413, 2.3266, 2.1524, 2.2642, 2.1806, 2.0566, 1.9192, 1.7598, 1.3516, 1.5802, 1.43859999999999, 1.49160000000001, 1.1524, 1.1892, 0.841399999999993, 0.879800000000003, 0.837599999999995, 0.469800000000006, 0.765600000000006, 0.331000000000003, 0.591399999999993, 0.601200000000006, 0.701599999999999, 0.558199999999999, 0.339399999999998, 0.354399999999998, 0.491200000000006, 0.308000000000007, 0.355199999999996, -0.0254000000000048, 0.205200000000005, -0.272999999999996, 0.132199999999997, 0.394400000000005, -0.241200000000006, 0.242000000000004, 0.191400000000002, 0.253799999999998, -0.122399999999999, -0.370800000000003, 0.193200000000004, -0.0848000000000013, 0.0867999999999967, -0.327200000000005, -0.285600000000002, 0.311400000000006, -0.128399999999999, -0.754999999999995, -0.209199999999996, -0.293599999999998, -0.364000000000004, -0.253600000000006, -0.821200000000005, -0.253600000000006, -0.510400000000004, -0.383399999999995, -0.491799999999998, -0.220200000000006, -0.0972000000000008, -0.557400000000001, -0.114599999999996, -0.295000000000002, -0.534800000000004, 0.346399999999988, -0.65379999999999, 0.0398000000000138, 0.0341999999999985, -0.995800000000003, -0.523400000000009, -0.489000000000004, -0.274799999999999, -0.574999999999989, -0.482799999999997, 0.0571999999999946, -0.330600000000004, -0.628800000000012, -0.140199999999993, -0.540600000000012, -0.445999999999998, -0.599400000000003, -0.262599999999992, 0.163399999999996, -0.100599999999986, -0.39500000000001, -1.06960000000001, -0.836399999999998, -0.753199999999993, -0.412399999999991, -0.790400000000005, -0.29679999999999, -0.28540000000001, -0.193000000000012, -0.0772000000000048, -0.962799999999987, -0.414800000000014),
    // precision 6
    Array(45, 44.1902, 43.271, 42.8358, 41.8142, 41.2854, 40.317, 39.354, 38.8924, 37.9436, 37.4596, 36.5262, 35.6248, 35.1574, 34.2822, 33.837, 32.9636, 32.074, 31.7042, 30.7976, 30.4772, 29.6564, 28.7942, 28.5004, 27.686, 27.291, 26.5672, 25.8556, 25.4982, 24.8204, 24.4252, 23.7744, 23.0786, 22.8344, 22.0294, 21.8098, 21.0794, 20.5732, 20.1878, 19.5648, 19.2902, 18.6784, 18.3352, 17.8946, 17.3712, 17.0852, 16.499, 16.2686, 15.6844, 15.2234, 14.9732, 14.3356, 14.2286, 13.7262, 13.3284, 13.1048, 12.5962, 12.3562, 12.1272, 11.4184, 11.4974, 11.0822, 10.856, 10.48, 10.2834, 10.0208, 9.637, 9.51739999999999, 9.05759999999999, 8.74760000000001, 8.42700000000001, 8.1326, 8.2372, 8.2788, 7.6776, 7.79259999999999, 7.1952, 6.9564, 6.6454, 6.87, 6.5428, 6.19999999999999, 6.02940000000001, 5.62780000000001, 5.6782, 5.792, 5.35159999999999, 5.28319999999999, 5.0394, 5.07480000000001, 4.49119999999999, 4.84899999999999, 4.696, 4.54040000000001, 4.07300000000001, 4.37139999999999, 3.7216, 3.7328, 3.42080000000001, 3.41839999999999, 3.94239999999999, 3.27719999999999, 3.411, 3.13079999999999, 2.76900000000001, 2.92580000000001, 2.68279999999999, 2.75020000000001, 2.70599999999999, 2.3886, 3.01859999999999, 2.45179999999999, 2.92699999999999, 2.41720000000001, 2.41139999999999, 2.03299999999999, 2.51240000000001, 2.5564, 2.60079999999999, 2.41720000000001, 1.80439999999999, 1.99700000000001, 2.45480000000001, 1.8948, 2.2346, 2.30860000000001, 2.15479999999999, 1.88419999999999, 1.6508, 0.677199999999999, 1.72540000000001, 1.4752, 1.72280000000001, 1.66139999999999, 1.16759999999999, 1.79300000000001, 1.00059999999999, 0.905200000000008, 0.659999999999997, 1.55879999999999, 1.1636, 0.688199999999995, 0.712600000000009, 0.450199999999995, 1.1978, 0.975599999999986, 0.165400000000005, 1.727, 1.19739999999999, -0.252600000000001, 1.13460000000001, 1.3048, 1.19479999999999, 0.313400000000001, 0.878999999999991, 1.12039999999999, 0.853000000000009, 1.67920000000001, 0.856999999999999, 0.448599999999999, 1.2362, 0.953399999999988, 1.02859999999998, 0.563199999999995, 0.663000000000011, 0.723000000000013, 0.756599999999992, 0.256599999999992, -0.837600000000009, 0.620000000000005, 0.821599999999989, 0.216600000000028, 0.205600000000004, 0.220199999999977, 0.372599999999977, 0.334400000000016, 0.928400000000011, 0.972800000000007, 0.192400000000021, 0.487199999999973, -0.413000000000011, 0.807000000000016, 0.120600000000024, 0.769000000000005, 0.870799999999974, 0.66500000000002, 0.118200000000002, 0.401200000000017, 0.635199999999998, 0.135400000000004, 0.175599999999974, 1.16059999999999, 0.34620000000001, 0.521400000000028, -0.586599999999976, -1.16480000000001, 0.968399999999974, 0.836999999999989, 0.779600000000016, 0.985799999999983),
    // precision 7
    Array(91, 89.4934, 87.9758, 86.4574, 84.9718, 83.4954, 81.5302, 80.0756, 78.6374, 77.1782, 75.7888, 73.9522, 72.592, 71.2532, 69.9086, 68.5938, 66.9474, 65.6796, 64.4394, 63.2176, 61.9768, 60.4214, 59.2528, 58.0102, 56.8658, 55.7278, 54.3044, 53.1316, 52.093, 51.0032, 49.9092, 48.6306, 47.5294, 46.5756, 45.6508, 44.662, 43.552, 42.3724, 41.617, 40.5754, 39.7872, 38.8444, 37.7988, 36.8606, 36.2118, 35.3566, 34.4476, 33.5882, 32.6816, 32.0824, 31.0258, 30.6048, 29.4436, 28.7274, 27.957, 27.147, 26.4364, 25.7592, 25.3386, 24.781, 23.8028, 23.656, 22.6544, 21.996, 21.4718, 21.1544, 20.6098, 19.5956, 19.0616, 18.5758, 18.4878, 17.5244, 17.2146, 16.724, 15.8722, 15.5198, 15.0414, 14.941, 14.9048, 13.87, 13.4304, 13.028, 12.4708, 12.37, 12.0624, 11.4668, 11.5532, 11.4352, 11.2564, 10.2744, 10.2118, 9.74720000000002, 10.1456, 9.2928, 8.75040000000001, 8.55279999999999, 8.97899999999998, 8.21019999999999, 8.18340000000001, 7.3494, 7.32499999999999, 7.66140000000001, 6.90300000000002, 7.25439999999998, 6.9042, 7.21499999999997, 6.28640000000001, 6.08139999999997, 6.6764, 6.30099999999999, 5.13900000000001, 5.65800000000002, 5.17320000000001, 4.59019999999998, 4.9538, 5.08280000000002, 4.92200000000003, 4.99020000000002, 4.7328, 5.4538, 4.11360000000002, 4.22340000000003, 4.08780000000002, 3.70800000000003, 4.15559999999999, 4.18520000000001, 3.63720000000001, 3.68220000000002, 3.77960000000002, 3.6078, 2.49160000000001, 3.13099999999997, 2.5376, 3.19880000000001, 3.21100000000001, 2.4502, 3.52820000000003, 2.91199999999998, 3.04480000000001, 2.7432, 2.85239999999999, 2.79880000000003, 2.78579999999999, 1.88679999999999, 2.98860000000002, 2.50639999999999, 1.91239999999999, 2.66160000000002, 2.46820000000002, 1.58199999999999, 1.30399999999997, 2.27379999999999, 2.68939999999998, 1.32900000000001, 3.10599999999999, 1.69080000000002, 2.13740000000001, 2.53219999999999, 1.88479999999998, 1.33240000000001, 1.45119999999997, 1.17899999999997, 2.44119999999998, 1.60659999999996, 2.16700000000003, 0.77940000000001, 2.37900000000002, 2.06700000000001, 1.46000000000004, 2.91160000000002, 1.69200000000001, 0.954600000000028, 2.49300000000005, 2.2722, 1.33500000000004, 2.44899999999996, 1.20140000000004, 3.07380000000001, 2.09739999999999, 2.85640000000001, 2.29960000000005, 2.40899999999999, 1.97040000000004, 0.809799999999996, 1.65279999999996, 2.59979999999996, 0.95799999999997, 2.06799999999998, 2.32780000000002, 4.20159999999998, 1.96320000000003, 1.86400000000003, 1.42999999999995, 3.77940000000001, 1.27200000000005, 1.86440000000005, 2.20600000000002, 3.21900000000005, 1.5154, 2.61019999999996),
    // precision 8
    Array(183.2152, 180.2454, 177.2096, 173.6652, 170.6312, 167.6822, 164.249, 161.3296, 158.0038, 155.2074, 152.4612, 149.27, 146.5178, 143.4412, 140.8032, 138.1634, 135.1688, 132.6074, 129.6946, 127.2664, 124.8228, 122.0432, 119.6824, 116.9464, 114.6268, 112.2626, 109.8376, 107.4034, 104.8956, 102.8522, 100.7638, 98.3552, 96.3556, 93.7526, 91.9292, 89.8954, 87.8198, 85.7668, 83.298, 81.6688, 79.9466, 77.9746, 76.1672, 74.3474, 72.3028, 70.8912, 69.114, 67.4646, 65.9744, 64.4092, 62.6022, 60.843, 59.5684, 58.1652, 56.5426, 55.4152, 53.5388, 52.3592, 51.1366, 49.486, 48.3918, 46.5076, 45.509, 44.3834, 43.3498, 42.0668, 40.7346, 40.1228, 38.4528, 37.7, 36.644, 36.0518, 34.5774, 33.9068, 32.432, 32.1666, 30.434, 29.6644, 28.4894, 27.6312, 26.3804, 26.292, 25.5496000000001, 25.0234, 24.8206, 22.6146, 22.4188, 22.117, 20.6762, 20.6576, 19.7864, 19.509, 18.5334, 17.9204, 17.772, 16.2924, 16.8654, 15.1836, 15.745, 15.1316, 15.0386, 14.0136, 13.6342, 12.6196, 12.1866, 12.4281999999999, 11.3324, 10.4794000000001, 11.5038, 10.129, 9.52800000000002, 10.3203999999999, 9.46299999999997, 9.79280000000006, 9.12300000000005, 8.74180000000001, 9.2192, 7.51020000000005, 7.60659999999996, 7.01840000000004, 7.22239999999999, 7.40139999999997, 6.76179999999999, 7.14359999999999, 5.65060000000005, 5.63779999999997, 5.76599999999996, 6.75139999999999, 5.57759999999996, 3.73220000000003, 5.8048, 5.63019999999995, 4.93359999999996, 3.47979999999995, 4.33879999999999, 3.98940000000005, 3.81960000000004, 3.31359999999995, 3.23080000000004, 3.4588, 3.08159999999998, 3.4076, 3.00639999999999, 2.38779999999997, 2.61900000000003, 1.99800000000005, 3.34820000000002, 2.95060000000001, 0.990999999999985, 2.11440000000005, 2.20299999999997, 2.82219999999995, 2.73239999999998, 2.7826, 3.76660000000004, 2.26480000000004, 2.31280000000004, 2.40819999999997, 2.75360000000001, 3.33759999999995, 2.71559999999999, 1.7478000000001, 1.42920000000004, 2.39300000000003, 2.22779999999989, 2.34339999999997, 0.87259999999992, 3.88400000000001, 1.80600000000004, 1.91759999999999, 1.16779999999994, 1.50320000000011, 2.52500000000009, 0.226400000000012, 2.31500000000005, 0.930000000000064, 1.25199999999995, 2.14959999999996, 0.0407999999999902, 2.5447999999999, 1.32960000000003, 0.197400000000016, 2.52620000000002, 3.33279999999991, -1.34300000000007, 0.422199999999975, 0.917200000000093, 1.12920000000008, 1.46060000000011, 1.45779999999991, 2.8728000000001, 3.33359999999993, -1.34079999999994, 1.57680000000005, 0.363000000000056, 1.40740000000005, 0.656600000000026, 0.801400000000058, -0.454600000000028, 1.51919999999996),
    // precision 9
    Array(368, 361.8294, 355.2452, 348.6698, 342.1464, 336.2024, 329.8782, 323.6598, 317.462, 311.2826, 305.7102, 299.7416, 293.9366, 288.1046, 282.285, 277.0668, 271.306, 265.8448, 260.301, 254.9886, 250.2422, 244.8138, 239.7074, 234.7428, 229.8402, 225.1664, 220.3534, 215.594, 210.6886, 205.7876, 201.65, 197.228, 192.8036, 188.1666, 184.0818, 180.0824, 176.2574, 172.302, 168.1644, 164.0056, 160.3802, 156.7192, 152.5234, 149.2084, 145.831, 142.485, 139.1112, 135.4764, 131.76, 129.3368, 126.5538, 122.5058, 119.2646, 116.5902, 113.3818, 110.8998, 107.9532, 105.2062, 102.2798, 99.4728, 96.9582, 94.3292, 92.171, 89.7809999999999, 87.5716, 84.7048, 82.5322, 79.875, 78.3972, 75.3464, 73.7274, 71.2834, 70.1444, 68.4263999999999, 66.0166, 64.018, 62.0437999999999, 60.3399999999999, 58.6856, 57.9836, 55.0311999999999, 54.6769999999999, 52.3188, 51.4846, 49.4423999999999, 47.739, 46.1487999999999, 44.9202, 43.4059999999999, 42.5342000000001, 41.2834, 38.8954000000001, 38.3286000000001, 36.2146, 36.6684, 35.9946, 33.123, 33.4338, 31.7378000000001, 29.076, 28.9692, 27.4964, 27.0998, 25.9864, 26.7754, 24.3208, 23.4838, 22.7388000000001, 24.0758000000001, 21.9097999999999, 20.9728, 19.9228000000001, 19.9292, 16.617, 17.05, 18.2996000000001, 15.6128000000001, 15.7392, 14.5174, 13.6322, 12.2583999999999, 13.3766000000001, 11.423, 13.1232, 9.51639999999998, 10.5938000000001, 9.59719999999993, 8.12220000000002, 9.76739999999995, 7.50440000000003, 7.56999999999994, 6.70440000000008, 6.41419999999994, 6.71019999999999, 5.60940000000005, 4.65219999999999, 6.84099999999989, 3.4072000000001, 3.97859999999991, 3.32760000000007, 5.52160000000003, 3.31860000000006, 2.06940000000009, 4.35400000000004, 1.57500000000005, 0.280799999999999, 2.12879999999996, -0.214799999999968, -0.0378000000000611, -0.658200000000079, 0.654800000000023, -0.0697999999999865, 0.858400000000074, -2.52700000000004, -2.1751999999999, -3.35539999999992, -1.04019999999991, -0.651000000000067, -2.14439999999991, -1.96659999999997, -3.97939999999994, -0.604400000000169, -3.08260000000018, -3.39159999999993, -5.29640000000018, -5.38920000000007, -5.08759999999984, -4.69900000000007, -5.23720000000003, -3.15779999999995, -4.97879999999986, -4.89899999999989, -7.48880000000008, -5.94799999999987, -5.68060000000014, -6.67180000000008, -4.70499999999993, -7.27779999999984, -4.6579999999999, -4.4362000000001, -4.32139999999981, -5.18859999999995, -6.66879999999992, -6.48399999999992, -5.1260000000002, -4.4032000000002, -6.13500000000022, -5.80819999999994, -4.16719999999987, -4.15039999999999, -7.45600000000013, -7.24080000000004, -9.83179999999993, -5.80420000000004, -8.6561999999999, -6.99940000000015, -10.5473999999999, -7.34139999999979, -6.80999999999995, -6.29719999999998, -6.23199999999997),
    // precision 10
    Array(737.1256, 724.4234, 711.1064, 698.4732, 685.4636, 673.0644, 660.488, 647.9654, 636.0832, 623.7864, 612.1992, 600.2176, 588.5228, 577.1716, 565.7752, 554.899, 543.6126, 532.6492, 521.9474, 511.5214, 501.1064, 490.6364, 480.2468, 470.4588, 460.3832, 451.0584, 440.8606, 431.3868, 422.5062, 413.1862, 404.463, 395.339, 386.1936, 378.1292, 369.1854, 361.2908, 353.3324, 344.8518, 337.5204, 329.4854, 321.9318, 314.552, 306.4658, 299.4256, 292.849, 286.152, 278.8956, 271.8792, 265.118, 258.62, 252.5132, 245.9322, 239.7726, 233.6086, 227.5332, 222.5918, 216.4294, 210.7662, 205.4106, 199.7338, 194.9012, 188.4486, 183.1556, 178.6338, 173.7312, 169.6264, 163.9526, 159.8742, 155.8326, 151.1966, 147.5594, 143.07, 140.037, 134.1804, 131.071, 127.4884, 124.0848, 120.2944, 117.333, 112.9626, 110.2902, 107.0814, 103.0334, 99.4832000000001, 96.3899999999999, 93.7202000000002, 90.1714000000002, 87.2357999999999, 85.9346, 82.8910000000001, 80.0264000000002, 78.3834000000002, 75.1543999999999, 73.8683999999998, 70.9895999999999, 69.4367999999999, 64.8701999999998, 65.0408000000002, 61.6738, 59.5207999999998, 57.0158000000001, 54.2302, 53.0962, 50.4985999999999, 52.2588000000001, 47.3914, 45.6244000000002, 42.8377999999998, 43.0072, 40.6516000000001, 40.2453999999998, 35.2136, 36.4546, 33.7849999999999, 33.2294000000002, 32.4679999999998, 30.8670000000002, 28.6507999999999, 28.9099999999999, 27.5983999999999, 26.1619999999998, 24.5563999999999, 23.2328000000002, 21.9484000000002, 21.5902000000001, 21.3346000000001, 17.7031999999999, 20.6111999999998, 19.5545999999999, 15.7375999999999, 17.0720000000001, 16.9517999999998, 15.326, 13.1817999999998, 14.6925999999999, 13.0859999999998, 13.2754, 10.8697999999999, 11.248, 7.3768, 4.72339999999986, 7.97899999999981, 8.7503999999999, 7.68119999999999, 9.7199999999998, 7.73919999999998, 5.6224000000002, 7.44560000000001, 6.6601999999998, 5.9058, 4.00199999999995, 4.51699999999983, 4.68240000000014, 3.86220000000003, 5.13639999999987, 5.98500000000013, 2.47719999999981, 2.61999999999989, 1.62800000000016, 4.65000000000009, 0.225599999999758, 0.831000000000131, -0.359400000000278, 1.27599999999984, -2.92559999999958, -0.0303999999996449, 2.37079999999969, -2.0033999999996, 0.804600000000391, 0.30199999999968, 1.1247999999996, -2.6880000000001, 0.0321999999996478, -1.18099999999959, -3.9402, -1.47940000000017, -0.188400000000001, -2.10720000000038, -2.04159999999956, -3.12880000000041, -4.16160000000036, -0.612799999999879, -3.48719999999958, -8.17900000000009, -5.37780000000021, -4.01379999999972, -5.58259999999973, -5.73719999999958, -7.66799999999967, -5.69520000000011, -1.1247999999996, -5.58520000000044, -8.04560000000038, -4.64840000000004, -11.6468000000004, -7.97519999999986, -5.78300000000036, -7.67420000000038, -10.6328000000003, -9.81720000000041),
    // precision 11
    Array(1476, 1449.6014, 1423.5802, 1397.7942, 1372.3042, 1347.2062, 1321.8402, 1297.2292, 1272.9462, 1248.9926, 1225.3026, 1201.4252, 1178.0578, 1155.6092, 1132.626, 1110.5568, 1088.527, 1066.5154, 1045.1874, 1024.3878, 1003.37, 982.1972, 962.5728, 942.1012, 922.9668, 903.292, 884.0772, 864.8578, 846.6562, 828.041, 809.714, 792.3112, 775.1806, 757.9854, 740.656, 724.346, 707.5154, 691.8378, 675.7448, 659.6722, 645.5722, 630.1462, 614.4124, 600.8728, 585.898, 572.408, 558.4926, 544.4938, 531.6776, 517.282, 505.7704, 493.1012, 480.7388, 467.6876, 456.1872, 445.5048, 433.0214, 420.806, 411.409, 400.4144, 389.4294, 379.2286, 369.651, 360.6156, 350.337, 342.083, 332.1538, 322.5094, 315.01, 305.6686, 298.1678, 287.8116, 280.9978, 271.9204, 265.3286, 257.5706, 249.6014, 242.544, 235.5976, 229.583, 220.9438, 214.672, 208.2786, 201.8628, 195.1834, 191.505, 186.1816, 178.5188, 172.2294, 167.8908, 161.0194, 158.052, 151.4588, 148.1596, 143.4344, 138.5238, 133.13, 127.6374, 124.8162, 118.7894, 117.3984, 114.6078, 109.0858, 105.1036, 103.6258, 98.6018000000004, 95.7618000000002, 93.5821999999998, 88.5900000000001, 86.9992000000002, 82.8800000000001, 80.4539999999997, 74.6981999999998, 74.3644000000004, 73.2914000000001, 65.5709999999999, 66.9232000000002, 65.1913999999997, 62.5882000000001, 61.5702000000001, 55.7035999999998, 56.1764000000003, 52.7596000000003, 53.0302000000001, 49.0609999999997, 48.4694, 44.933, 46.0474000000004, 44.7165999999997, 41.9416000000001, 39.9207999999999, 35.6328000000003, 35.5276000000003, 33.1934000000001, 33.2371999999996, 33.3864000000003, 33.9228000000003, 30.2371999999996, 29.1373999999996, 25.2272000000003, 24.2942000000003, 19.8338000000003, 18.9005999999999, 23.0907999999999, 21.8544000000002, 19.5176000000001, 15.4147999999996, 16.9314000000004, 18.6737999999996, 12.9877999999999, 14.3688000000002, 12.0447999999997, 15.5219999999999, 12.5299999999997, 14.5940000000001, 14.3131999999996, 9.45499999999993, 12.9441999999999, 3.91139999999996, 13.1373999999996, 5.44720000000052, 9.82779999999912, 7.87279999999919, 3.67760000000089, 5.46980000000076, 5.55099999999948, 5.65979999999945, 3.89439999999922, 3.1275999999998, 5.65140000000065, 6.3062000000009, 3.90799999999945, 1.87060000000019, 5.17020000000048, 2.46680000000015, 0.770000000000437, -3.72340000000077, 1.16400000000067, 8.05340000000069, 0.135399999999208, 2.15940000000046, 0.766999999999825, 1.0594000000001, 3.15500000000065, -0.287399999999252, 2.37219999999979, -2.86620000000039, -1.63199999999961, -2.22979999999916, -0.15519999999924, -1.46039999999994, -0.262199999999211, -2.34460000000036, -2.8078000000005, -3.22179999999935, -5.60159999999996, -8.42200000000048, -9.43740000000071, 0.161799999999857, -10.4755999999998, -10.0823999999993),
    // precision 12
    Array(2953, 2900.4782, 2848.3568, 2796.3666, 2745.324, 2694.9598, 2644.648, 2595.539, 2546.1474, 2498.2576, 2450.8376, 2403.6076, 2357.451, 2311.38, 2266.4104, 2221.5638, 2176.9676, 2134.193, 2090.838, 2048.8548, 2007.018, 1966.1742, 1925.4482, 1885.1294, 1846.4776, 1807.4044, 1768.8724, 1731.3732, 1693.4304, 1657.5326, 1621.949, 1586.5532, 1551.7256, 1517.6182, 1483.5186, 1450.4528, 1417.865, 1385.7164, 1352.6828, 1322.6708, 1291.8312, 1260.9036, 1231.476, 1201.8652, 1173.6718, 1145.757, 1119.2072, 1092.2828, 1065.0434, 1038.6264, 1014.3192, 988.5746, 965.0816, 940.1176, 917.9796, 894.5576, 871.1858, 849.9144, 827.1142, 805.0818, 783.9664, 763.9096, 742.0816, 724.3962, 706.3454, 688.018, 667.4214, 650.3106, 633.0686, 613.8094, 597.818, 581.4248, 563.834, 547.363, 531.5066, 520.455400000001, 505.583199999999, 488.366, 476.480799999999, 459.7682, 450.0522, 434.328799999999, 423.952799999999, 408.727000000001, 399.079400000001, 387.252200000001, 373.987999999999, 360.852000000001, 351.6394, 339.642, 330.902400000001, 322.661599999999, 311.662200000001, 301.3254, 291.7484, 279.939200000001, 276.7508, 263.215200000001, 254.811400000001, 245.5494, 242.306399999999, 234.8734, 223.787200000001, 217.7156, 212.0196, 200.793, 195.9748, 189.0702, 182.449199999999, 177.2772, 170.2336, 164.741, 158.613600000001, 155.311, 147.5964, 142.837, 137.3724, 132.0162, 130.0424, 121.9804, 120.451800000001, 114.8968, 111.585999999999, 105.933199999999, 101.705, 98.5141999999996, 95.0488000000005, 89.7880000000005, 91.4750000000004, 83.7764000000006, 80.9698000000008, 72.8574000000008, 73.1615999999995, 67.5838000000003, 62.6263999999992, 63.2638000000006, 66.0977999999996, 52.0843999999997, 58.9956000000002, 47.0912000000008, 46.4956000000002, 48.4383999999991, 47.1082000000006, 43.2392, 37.2759999999998, 40.0283999999992, 35.1864000000005, 35.8595999999998, 32.0998, 28.027, 23.6694000000007, 33.8266000000003, 26.3736000000008, 27.2008000000005, 21.3245999999999, 26.4115999999995, 23.4521999999997, 19.5013999999992, 19.8513999999996, 10.7492000000002, 18.6424000000006, 13.1265999999996, 18.2436000000016, 6.71860000000015, 3.39459999999963, 6.33759999999893, 7.76719999999841, 0.813999999998487, 3.82819999999992, 0.826199999999517, 8.07440000000133, -1.59080000000176, 5.01780000000144, 0.455399999998917, -0.24199999999837, 0.174800000000687, -9.07640000000174, -4.20160000000033, -3.77520000000004, -4.75179999999818, -5.3724000000002, -8.90680000000066, -6.10239999999976, -5.74120000000039, -9.95339999999851, -3.86339999999836, -13.7304000000004, -16.2710000000006, -7.51359999999841, -3.30679999999847, -13.1339999999982, -10.0551999999989, -6.72019999999975, -8.59660000000076, -10.9307999999983, -1.8775999999998, -4.82259999999951, -13.7788, -21.6470000000008, -10.6735999999983, -15.7799999999988),
    // precision 13
    Array(5907.5052, 5802.2672, 5697.347, 5593.5794, 5491.2622, 5390.5514, 5290.3376, 5191.6952, 5093.5988, 4997.3552, 4902.5972, 4808.3082, 4715.5646, 4624.109, 4533.8216, 4444.4344, 4356.3802, 4269.2962, 4183.3784, 4098.292, 4014.79, 3932.4574, 3850.6036, 3771.2712, 3691.7708, 3615.099, 3538.1858, 3463.4746, 3388.8496, 3315.6794, 3244.5448, 3173.7516, 3103.3106, 3033.6094, 2966.5642, 2900.794, 2833.7256, 2769.81, 2707.3196, 2644.0778, 2583.9916, 2523.4662, 2464.124, 2406.073, 2347.0362, 2292.1006, 2238.1716, 2182.7514, 2128.4884, 2077.1314, 2025.037, 1975.3756, 1928.933, 1879.311, 1831.0006, 1783.2144, 1738.3096, 1694.5144, 1649.024, 1606.847, 1564.7528, 1525.3168, 1482.5372, 1443.9668, 1406.5074, 1365.867, 1329.2186, 1295.4186, 1257.9716, 1225.339, 1193.2972, 1156.3578, 1125.8686, 1091.187, 1061.4094, 1029.4188, 1000.9126, 972.3272, 944.004199999999, 915.7592, 889.965, 862.834200000001, 840.4254, 812.598399999999, 785.924200000001, 763.050999999999, 741.793799999999, 721.466, 699.040799999999, 677.997200000002, 649.866999999998, 634.911800000002, 609.8694, 591.981599999999, 570.2922, 557.129199999999, 538.3858, 521.872599999999, 502.951400000002, 495.776399999999, 475.171399999999, 459.751, 439.995200000001, 426.708999999999, 413.7016, 402.3868, 387.262599999998, 372.0524, 357.050999999999, 342.5098, 334.849200000001, 322.529399999999, 311.613799999999, 295.848000000002, 289.273000000001, 274.093000000001, 263.329600000001, 251.389599999999, 245.7392, 231.9614, 229.7952, 217.155200000001, 208.9588, 199.016599999999, 190.839199999999, 180.6976, 176.272799999999, 166.976999999999, 162.5252, 151.196400000001, 149.386999999999, 133.981199999998, 130.0586, 130.164000000001, 122.053400000001, 110.7428, 108.1276, 106.232400000001, 100.381600000001, 98.7668000000012, 86.6440000000002, 79.9768000000004, 82.4722000000002, 68.7026000000005, 70.1186000000016, 71.9948000000004, 58.998599999999, 59.0492000000013, 56.9818000000014, 47.5338000000011, 42.9928, 51.1591999999982, 37.2740000000013, 42.7220000000016, 31.3734000000004, 26.8090000000011, 25.8934000000008, 26.5286000000015, 29.5442000000003, 19.3503999999994, 26.0760000000009, 17.9527999999991, 14.8419999999969, 10.4683999999979, 8.65899999999965, 9.86720000000059, 4.34139999999752, -0.907800000000861, -3.32080000000133, -0.936199999996461, -11.9916000000012, -8.87000000000262, -6.33099999999831, -11.3366000000024, -15.9207999999999, -9.34659999999712, -15.5034000000014, -19.2097999999969, -15.357799999998, -28.2235999999975, -30.6898000000001, -19.3271999999997, -25.6083999999973, -24.409599999999, -13.6385999999984, -33.4473999999973, -32.6949999999997, -28.9063999999998, -31.7483999999968, -32.2935999999972, -35.8329999999987, -47.620600000002, -39.0855999999985, -33.1434000000008, -46.1371999999974, -37.5892000000022, -46.8164000000033, -47.3142000000007, -60.2914000000019, -37.7575999999972),
    // precision 14
    Array(11816.475, 11605.0046, 11395.3792, 11188.7504, 10984.1814, 10782.0086, 10582.0072, 10384.503, 10189.178, 9996.2738, 9806.0344, 9617.9798, 9431.394, 9248.7784, 9067.6894, 8889.6824, 8712.9134, 8538.8624, 8368.4944, 8197.7956, 8031.8916, 7866.6316, 7703.733, 7544.5726, 7386.204, 7230.666, 7077.8516, 6926.7886, 6778.6902, 6631.9632, 6487.304, 6346.7486, 6206.4408, 6070.202, 5935.2576, 5799.924, 5671.0324, 5541.9788, 5414.6112, 5290.0274, 5166.723, 5047.6906, 4929.162, 4815.1406, 4699.127, 4588.5606, 4477.7394, 4369.4014, 4264.2728, 4155.9224, 4055.581, 3955.505, 3856.9618, 3761.3828, 3666.9702, 3575.7764, 3482.4132, 3395.0186, 3305.8852, 3221.415, 3138.6024, 3056.296, 2970.4494, 2896.1526, 2816.8008, 2740.2156, 2670.497, 2594.1458, 2527.111, 2460.8168, 2387.5114, 2322.9498, 2260.6752, 2194.2686, 2133.7792, 2074.767, 2015.204, 1959.4226, 1898.6502, 1850.006, 1792.849, 1741.4838, 1687.9778, 1638.1322, 1589.3266, 1543.1394, 1496.8266, 1447.8516, 1402.7354, 1361.9606, 1327.0692, 1285.4106, 1241.8112, 1201.6726, 1161.973, 1130.261, 1094.2036, 1048.2036, 1020.6436, 990.901400000002, 961.199800000002, 924.769800000002, 899.526400000002, 872.346400000002, 834.375, 810.432000000001, 780.659800000001, 756.013800000001, 733.479399999997, 707.923999999999, 673.858, 652.222399999999, 636.572399999997, 615.738599999997, 586.696400000001, 564.147199999999, 541.679600000003, 523.943599999999, 505.714599999999, 475.729599999999, 461.779600000002, 449.750800000002, 439.020799999998, 412.7886, 400.245600000002, 383.188199999997, 362.079599999997, 357.533799999997, 334.319000000003, 327.553399999997, 308.559399999998, 291.270199999999, 279.351999999999, 271.791400000002, 252.576999999997, 247.482400000001, 236.174800000001, 218.774599999997, 220.155200000001, 208.794399999999, 201.223599999998, 182.995600000002, 185.5268, 164.547400000003, 176.5962, 150.689599999998, 157.8004, 138.378799999999, 134.021200000003, 117.614399999999, 108.194000000003, 97.0696000000025, 89.6042000000016, 95.6030000000028, 84.7810000000027, 72.635000000002, 77.3482000000004, 59.4907999999996, 55.5875999999989, 50.7346000000034, 61.3916000000027, 50.9149999999936, 39.0384000000049, 58.9395999999979, 29.633600000001, 28.2032000000036, 26.0078000000067, 17.0387999999948, 9.22000000000116, 13.8387999999977, 8.07240000000456, 14.1549999999988, 15.3570000000036, 3.42660000000615, 6.24820000000182, -2.96940000000177, -8.79940000000352, -5.97860000000219, -14.4048000000039, -3.4143999999942, -13.0148000000045, -11.6977999999945, -25.7878000000055, -22.3185999999987, -24.409599999999, -31.9756000000052, -18.9722000000038, -22.8678000000073, -30.8972000000067, -32.3715999999986, -22.3907999999938, -43.6720000000059, -35.9038, -39.7492000000057, -54.1641999999993, -45.2749999999942, -42.2989999999991, -44.1089999999967, -64.3564000000042, -49.9551999999967, -42.6116000000038),
    // precision 15
    Array(23634.0036, 23210.8034, 22792.4744, 22379.1524, 21969.7928, 21565.326, 21165.3532, 20770.2806, 20379.9892, 19994.7098, 19613.318, 19236.799, 18865.4382, 18498.8244, 18136.5138, 17778.8668, 17426.2344, 17079.32, 16734.778, 16397.2418, 16063.3324, 15734.0232, 15409.731, 15088.728, 14772.9896, 14464.1402, 14157.5588, 13855.5958, 13559.3296, 13264.9096, 12978.326, 12692.0826, 12413.8816, 12137.3192, 11870.2326, 11602.5554, 11340.3142, 11079.613, 10829.5908, 10583.5466, 10334.0344, 10095.5072, 9859.694, 9625.2822, 9395.7862, 9174.0586, 8957.3164, 8738.064, 8524.155, 8313.7396, 8116.9168, 7913.542, 7718.4778, 7521.65, 7335.5596, 7154.2906, 6968.7396, 6786.3996, 6613.236, 6437.406, 6270.6598, 6107.7958, 5945.7174, 5787.6784, 5635.5784, 5482.308, 5337.9784, 5190.0864, 5045.9158, 4919.1386, 4771.817, 4645.7742, 4518.4774, 4385.5454, 4262.6622, 4142.74679999999, 4015.5318, 3897.9276, 3790.7764, 3685.13800000001, 3573.6274, 3467.9706, 3368.61079999999, 3271.5202, 3170.3848, 3076.4656, 2982.38400000001, 2888.4664, 2806.4868, 2711.9564, 2634.1434, 2551.3204, 2469.7662, 2396.61139999999, 2318.9902, 2243.8658, 2171.9246, 2105.01360000001, 2028.8536, 1960.9952, 1901.4096, 1841.86079999999, 1777.54700000001, 1714.5802, 1654.65059999999, 1596.311, 1546.2016, 1492.3296, 1433.8974, 1383.84600000001, 1339.4152, 1293.5518, 1245.8686, 1193.50659999999, 1162.27959999999, 1107.19439999999, 1069.18060000001, 1035.09179999999, 999.679000000004, 957.679999999993, 925.300199999998, 888.099400000006, 848.638600000006, 818.156400000007, 796.748399999997, 752.139200000005, 725.271200000003, 692.216, 671.633600000001, 647.939799999993, 621.670599999998, 575.398799999995, 561.226599999995, 532.237999999998, 521.787599999996, 483.095799999996, 467.049599999998, 465.286399999997, 415.548599999995, 401.047399999996, 380.607999999993, 377.362599999993, 347.258799999996, 338.371599999999, 310.096999999994, 301.409199999995, 276.280799999993, 265.586800000005, 258.994399999996, 223.915999999997, 215.925399999993, 213.503800000006, 191.045400000003, 166.718200000003, 166.259000000005, 162.941200000001, 148.829400000002, 141.645999999993, 123.535399999993, 122.329800000007, 89.473399999988, 80.1962000000058, 77.5457999999926, 59.1056000000099, 83.3509999999951, 52.2906000000075, 36.3979999999865, 40.6558000000077, 42.0003999999899, 19.6630000000005, 19.7153999999864, -8.38539999999921, -0.692799999989802, 0.854800000000978, 3.23219999999856, -3.89040000000386, -5.25880000001052, -24.9052000000083, -22.6837999999989, -26.4286000000138, -34.997000000003, -37.0216000000073, -43.430400000012, -58.2390000000014, -68.8034000000043, -56.9245999999985, -57.8583999999973, -77.3097999999882, -73.2793999999994, -81.0738000000129, -87.4530000000086, -65.0254000000132, -57.296399999992, -96.2746000000043, -103.25, -96.081600000005, -91.5542000000132, -102.465200000006, -107.688599999994, -101.458000000013, -109.715800000005),
    // precision 16
    Array(47270, 46423.3584, 45585.7074, 44757.152, 43938.8416, 43130.9514, 42330.03, 41540.407, 40759.6348, 39988.206, 39226.5144, 38473.2096, 37729.795, 36997.268, 36272.6448, 35558.665, 34853.0248, 34157.4472, 33470.5204, 32793.5742, 32127.0194, 31469.4182, 30817.6136, 30178.6968, 29546.8908, 28922.8544, 28312.271, 27707.0924, 27114.0326, 26526.692, 25948.6336, 25383.7826, 24823.5998, 24272.2974, 23732.2572, 23201.4976, 22674.2796, 22163.6336, 21656.515, 21161.7362, 20669.9368, 20189.4424, 19717.3358, 19256.3744, 18795.9638, 18352.197, 17908.5738, 17474.391, 17052.918, 16637.2236, 16228.4602, 15823.3474, 15428.6974, 15043.0284, 14667.6278, 14297.4588, 13935.2882, 13578.5402, 13234.6032, 12882.1578, 12548.0728, 12219.231, 11898.0072, 11587.2626, 11279.9072, 10973.5048, 10678.5186, 10392.4876, 10105.2556, 9825.766, 9562.5444, 9294.2222, 9038.2352, 8784.848, 8533.2644, 8301.7776, 8058.30859999999, 7822.94579999999, 7599.11319999999, 7366.90779999999, 7161.217, 6957.53080000001, 6736.212, 6548.21220000001, 6343.06839999999, 6156.28719999999, 5975.15419999999, 5791.75719999999, 5621.32019999999, 5451.66, 5287.61040000001, 5118.09479999999, 4957.288, 4798.4246, 4662.17559999999, 4512.05900000001, 4364.68539999999, 4220.77720000001, 4082.67259999999, 3957.19519999999, 3842.15779999999, 3699.3328, 3583.01180000001, 3473.8964, 3338.66639999999, 3233.55559999999, 3117.799, 3008.111, 2909.69140000001, 2814.86499999999, 2719.46119999999, 2624.742, 2532.46979999999, 2444.7886, 2370.1868, 2272.45259999999, 2196.19260000001, 2117.90419999999, 2023.2972, 1969.76819999999, 1885.58979999999, 1833.2824, 1733.91200000001, 1682.54920000001, 1604.57980000001, 1556.11240000001, 1491.3064, 1421.71960000001, 1371.22899999999, 1322.1324, 1264.7892, 1196.23920000001, 1143.8474, 1088.67240000001, 1073.60380000001, 1023.11660000001, 959.036400000012, 927.433199999999, 906.792799999996, 853.433599999989, 841.873800000001, 791.1054, 756.899999999994, 704.343200000003, 672.495599999995, 622.790399999998, 611.254799999995, 567.283200000005, 519.406599999988, 519.188400000014, 495.312800000014, 451.350799999986, 443.973399999988, 431.882199999993, 392.027000000002, 380.924200000009, 345.128999999986, 298.901400000002, 287.771999999997, 272.625, 247.253000000026, 222.490600000019, 223.590000000026, 196.407599999977, 176.425999999978, 134.725199999986, 132.4804, 110.445599999977, 86.7939999999944, 56.7038000000175, 64.915399999998, 38.3726000000024, 37.1606000000029, 46.170999999973, 49.1716000000015, 15.3362000000197, 6.71639999997569, -34.8185999999987, -39.4476000000141, 12.6830000000191, -12.3331999999937, -50.6565999999875, -59.9538000000175, -65.1054000000004, -70.7576000000117, -106.325200000021, -126.852200000023, -110.227599999984, -132.885999999999, -113.897200000007, -142.713800000027, -151.145399999979, -150.799200000009, -177.756200000003, -156.036399999983, -182.735199999996, -177.259399999981, -198.663600000029, -174.577600000019, -193.84580000001),
    // precision 17
    Array(94541, 92848.811, 91174.019, 89517.558, 87879.9705, 86262.7565, 84663.5125, 83083.7435, 81521.7865, 79977.272, 78455.9465, 76950.219, 75465.432, 73994.152, 72546.71, 71115.2345, 69705.6765, 68314.937, 66944.2705, 65591.255, 64252.9485, 62938.016, 61636.8225, 60355.592, 59092.789, 57850.568, 56624.518, 55417.343, 54231.1415, 53067.387, 51903.526, 50774.649, 49657.6415, 48561.05, 47475.7575, 46410.159, 45364.852, 44327.053, 43318.4005, 42325.6165, 41348.4595, 40383.6265, 39436.77, 38509.502, 37594.035, 36695.939, 35818.6895, 34955.691, 34115.8095, 33293.949, 32465.0775, 31657.6715, 30877.2585, 30093.78, 29351.3695, 28594.1365, 27872.115, 27168.7465, 26477.076, 25774.541, 25106.5375, 24452.5135, 23815.5125, 23174.0655, 22555.2685, 21960.2065, 21376.3555, 20785.1925, 20211.517, 19657.0725, 19141.6865, 18579.737, 18081.3955, 17578.995, 17073.44, 16608.335, 16119.911, 15651.266, 15194.583, 14749.0495, 14343.4835, 13925.639, 13504.509, 13099.3885, 12691.2855, 12328.018, 11969.0345, 11596.5145, 11245.6355, 10917.6575, 10580.9785, 10277.8605, 9926.58100000001, 9605.538, 9300.42950000003, 8989.97850000003, 8728.73249999998, 8448.3235, 8175.31050000002, 7898.98700000002, 7629.79100000003, 7413.76199999999, 7149.92300000001, 6921.12650000001, 6677.1545, 6443.28000000003, 6278.23450000002, 6014.20049999998, 5791.20299999998, 5605.78450000001, 5438.48800000001, 5234.2255, 5059.6825, 4887.43349999998, 4682.935, 4496.31099999999, 4322.52250000002, 4191.42499999999, 4021.24200000003, 3900.64799999999, 3762.84250000003, 3609.98050000001, 3502.29599999997, 3363.84250000003, 3206.54849999998, 3079.70000000001, 2971.42300000001, 2867.80349999998, 2727.08100000001, 2630.74900000001, 2496.6165, 2440.902, 2356.19150000002, 2235.58199999999, 2120.54149999999, 2012.25449999998, 1933.35600000003, 1820.93099999998, 1761.54800000001, 1663.09350000002, 1578.84600000002, 1509.48149999999, 1427.3345, 1379.56150000001, 1306.68099999998, 1212.63449999999, 1084.17300000001, 1124.16450000001, 1060.69949999999, 1007.48849999998, 941.194499999983, 879.880500000028, 836.007500000007, 782.802000000025, 748.385499999975, 647.991500000004, 626.730500000005, 570.776000000013, 484.000500000024, 513.98550000001, 418.985499999952, 386.996999999974, 370.026500000036, 355.496999999974, 356.731499999994, 255.92200000002, 259.094000000041, 205.434499999974, 165.374500000034, 197.347500000033, 95.718499999959, 67.6165000000037, 54.6970000000438, 31.7395000000251, -15.8784999999916, 8.42500000004657, -26.3754999999655, -118.425500000012, -66.6629999999423, -42.9745000000112, -107.364999999991, -189.839000000036, -162.611499999999, -164.964999999967, -189.079999999958, -223.931499999948, -235.329999999958, -269.639500000048, -249.087999999989, -206.475499999942, -283.04449999996, -290.667000000016, -304.561499999953, -336.784499999951, -380.386500000022, -283.280499999993, -364.533000000054, -389.059499999974, -364.454000000027, -415.748000000021, -417.155000000028),
    // precision 18
    Array(189083, 185696.913, 182348.774, 179035.946, 175762.762, 172526.444, 169329.754, 166166.099, 163043.269, 159958.91, 156907.912, 153906.845, 150924.199, 147996.568, 145093.457, 142239.233, 139421.475, 136632.27, 133889.588, 131174.2, 128511.619, 125868.621, 123265.385, 120721.061, 118181.769, 115709.456, 113252.446, 110840.198, 108465.099, 106126.164, 103823.469, 101556.618, 99308.004, 97124.508, 94937.803, 92833.731, 90745.061, 88677.627, 86617.47, 84650.442, 82697.833, 80769.132, 78879.629, 77014.432, 75215.626, 73384.587, 71652.482, 69895.93, 68209.301, 66553.669, 64921.981, 63310.323, 61742.115, 60205.018, 58698.658, 57190.657, 55760.865, 54331.169, 52908.167, 51550.273, 50225.254, 48922.421, 47614.533, 46362.049, 45098.569, 43926.083, 42736.03, 41593.473, 40425.26, 39316.237, 38243.651, 37170.617, 36114.609, 35084.19, 34117.233, 33206.509, 32231.505, 31318.728, 30403.404, 29540.0550000001, 28679.236, 27825.862, 26965.216, 26179.148, 25462.08, 24645.952, 23922.523, 23198.144, 22529.128, 21762.4179999999, 21134.779, 20459.117, 19840.818, 19187.04, 18636.3689999999, 17982.831, 17439.7389999999, 16874.547, 16358.2169999999, 15835.684, 15352.914, 14823.681, 14329.313, 13816.897, 13342.874, 12880.882, 12491.648, 12021.254, 11625.392, 11293.7610000001, 10813.697, 10456.209, 10099.074, 9755.39000000001, 9393.18500000006, 9047.57900000003, 8657.98499999999, 8395.85900000005, 8033, 7736.95900000003, 7430.59699999995, 7258.47699999996, 6924.58200000005, 6691.29399999999, 6357.92500000005, 6202.05700000003, 5921.19700000004, 5628.28399999999, 5404.96799999999, 5226.71100000001, 4990.75600000005, 4799.77399999998, 4622.93099999998, 4472.478, 4171.78700000001, 3957.46299999999, 3868.95200000005, 3691.14300000004, 3474.63100000005, 3341.67200000002, 3109.14000000001, 3071.97400000005, 2796.40399999998, 2756.17799999996, 2611.46999999997, 2471.93000000005, 2382.26399999997, 2209.22400000005, 2142.28399999999, 2013.96100000001, 1911.18999999994, 1818.27099999995, 1668.47900000005, 1519.65800000005, 1469.67599999998, 1367.13800000004, 1248.52899999998, 1181.23600000003, 1022.71900000004, 1088.20700000005, 959.03600000008, 876.095999999903, 791.183999999892, 703.337000000058, 731.949999999953, 586.86400000006, 526.024999999907, 323.004999999888, 320.448000000091, 340.672999999952, 309.638999999966, 216.601999999955, 102.922999999952, 19.2399999999907, -0.114000000059605, -32.6240000000689, -89.3179999999702, -153.497999999905, -64.2970000000205, -143.695999999996, -259.497999999905, -253.017999999924, -213.948000000091, -397.590000000084, -434.006000000052, -403.475000000093, -297.958000000101, -404.317000000039, -528.898999999976, -506.621000000043, -513.205000000075, -479.351000000024, -596.139999999898, -527.016999999993, -664.681000000099, -680.306000000099, -704.050000000047, -850.486000000034, -757.43200000003, -713.308999999892)
  )
  // scalastyle:on
}

/**
 * A central moment is the expected value of a specified power of the deviation of a random
 * variable from the mean. Central moments are often used to characterize the properties of about
 * the shape of a distribution.
 *
 * This class implements online, one-pass algorithms for computing the central moments of a set of
 * points.
 *
 * Behavior:
 *  - null values are ignored
 *  - returns `Double.NaN` when the column contains `Double.NaN` values
 *
 * References:
 *  - Xiangrui Meng.  "Simpler Online Updates for Arbitrary-Order Central Moments."
 *      2015. http://arxiv.org/abs/1510.04923
 *
 * @see [[https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 *     Algorithms for calculating variance (Wikipedia)]]
 *
 * @param child to compute central moments of.
 */
abstract class CentralMomentAgg(child: Expression) extends ImperativeAggregate with Serializable {

  /**
   * The central moment order to be computed.
   */
  protected def momentOrder: Int

  override def children: Seq[Expression] = Seq(child)

  override def nullable: Boolean = false

  override def dataType: DataType = DoubleType

  // Expected input data type.
  // TODO: Right now, we replace old aggregate functions (based on AggregateExpression1) to the
  // new version at planning time (after analysis phase). For now, NullType is added at here
  // to make it resolved when we have cases like `select avg(null)`.
  // We can use our analyzer to cast NullType to the default data type of the NumericType once
  // we remove the old aggregate functions. Then, we will not need NullType at here.
  override def inputTypes: Seq[AbstractDataType] = Seq(TypeCollection(NumericType, NullType))

  override def aggBufferSchema: StructType = StructType.fromAttributes(aggBufferAttributes)

  /**
   * Size of aggregation buffer.
   */
  private[this] val bufferSize = 5

  override val aggBufferAttributes: Seq[AttributeReference] = Seq.tabulate(bufferSize) { i =>
    AttributeReference(s"M$i", DoubleType)()
  }

  // Note: although this simply copies aggBufferAttributes, this common code can not be placed
  // in the superclass because that will lead to initialization ordering issues.
  override val inputAggBufferAttributes: Seq[AttributeReference] =
    aggBufferAttributes.map(_.newInstance())

  // buffer offsets
  private[this] val nOffset = mutableAggBufferOffset
  private[this] val meanOffset = mutableAggBufferOffset + 1
  private[this] val secondMomentOffset = mutableAggBufferOffset + 2
  private[this] val thirdMomentOffset = mutableAggBufferOffset + 3
  private[this] val fourthMomentOffset = mutableAggBufferOffset + 4

  // frequently used values for online updates
  private[this] var delta = 0.0
  private[this] var deltaN = 0.0
  private[this] var delta2 = 0.0
  private[this] var deltaN2 = 0.0
  private[this] var n = 0.0
  private[this] var mean = 0.0
  private[this] var m2 = 0.0
  private[this] var m3 = 0.0
  private[this] var m4 = 0.0

  /**
   * Initialize all moments to zero.
   */
  override def initialize(buffer: MutableRow): Unit = {
    for (aggIndex <- 0 until bufferSize) {
      buffer.setDouble(mutableAggBufferOffset + aggIndex, 0.0)
    }
  }

  /**
   * Update the central moments buffer.
   */
  override def update(buffer: MutableRow, input: InternalRow): Unit = {
    val v = Cast(child, DoubleType).eval(input)
    if (v != null) {
      val updateValue = v match {
        case d: Double => d
      }

      n = buffer.getDouble(nOffset)
      mean = buffer.getDouble(meanOffset)

      n += 1.0
      buffer.setDouble(nOffset, n)
      delta = updateValue - mean
      deltaN = delta / n
      mean += deltaN
      buffer.setDouble(meanOffset, mean)

      if (momentOrder >= 2) {
        m2 = buffer.getDouble(secondMomentOffset)
        m2 += delta * (delta - deltaN)
        buffer.setDouble(secondMomentOffset, m2)
      }

      if (momentOrder >= 3) {
        delta2 = delta * delta
        deltaN2 = deltaN * deltaN
        m3 = buffer.getDouble(thirdMomentOffset)
        m3 += -3.0 * deltaN * m2 + delta * (delta2 - deltaN2)
        buffer.setDouble(thirdMomentOffset, m3)
      }

      if (momentOrder >= 4) {
        m4 = buffer.getDouble(fourthMomentOffset)
        m4 += -4.0 * deltaN * m3 - 6.0 * deltaN2 * m2 +
          delta * (delta * delta2 - deltaN * deltaN2)
        buffer.setDouble(fourthMomentOffset, m4)
      }
    }
  }

  /**
   * Merge two central moment buffers.
   */
  override def merge(buffer1: MutableRow, buffer2: InternalRow): Unit = {
    val n1 = buffer1.getDouble(nOffset)
    val n2 = buffer2.getDouble(inputAggBufferOffset)
    val mean1 = buffer1.getDouble(meanOffset)
    val mean2 = buffer2.getDouble(inputAggBufferOffset + 1)

    var secondMoment1 = 0.0
    var secondMoment2 = 0.0

    var thirdMoment1 = 0.0
    var thirdMoment2 = 0.0

    var fourthMoment1 = 0.0
    var fourthMoment2 = 0.0

    n = n1 + n2
    buffer1.setDouble(nOffset, n)
    delta = mean2 - mean1
    deltaN = if (n == 0.0) 0.0 else delta / n
    mean = mean1 + deltaN * n2
    buffer1.setDouble(mutableAggBufferOffset + 1, mean)

    // higher order moments computed according to:
    // https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Higher-order_statistics
    if (momentOrder >= 2) {
      secondMoment1 = buffer1.getDouble(secondMomentOffset)
      secondMoment2 = buffer2.getDouble(inputAggBufferOffset + 2)
      m2 = secondMoment1 + secondMoment2 + delta * deltaN * n1 * n2
      buffer1.setDouble(secondMomentOffset, m2)
    }

    if (momentOrder >= 3) {
      thirdMoment1 = buffer1.getDouble(thirdMomentOffset)
      thirdMoment2 = buffer2.getDouble(inputAggBufferOffset + 3)
      m3 = thirdMoment1 + thirdMoment2 + deltaN * deltaN * delta * n1 * n2 *
        (n1 - n2) + 3.0 * deltaN * (n1 * secondMoment2 - n2 * secondMoment1)
      buffer1.setDouble(thirdMomentOffset, m3)
    }

    if (momentOrder >= 4) {
      fourthMoment1 = buffer1.getDouble(fourthMomentOffset)
      fourthMoment2 = buffer2.getDouble(inputAggBufferOffset + 4)
      m4 = fourthMoment1 + fourthMoment2 + deltaN * deltaN * deltaN * delta * n1 *
        n2 * (n1 * n1 - n1 * n2 + n2 * n2) + deltaN * deltaN * 6.0 *
        (n1 * n1 * secondMoment2 + n2 * n2 * secondMoment1) +
        4.0 * deltaN * (n1 * thirdMoment2 - n2 * thirdMoment1)
      buffer1.setDouble(fourthMomentOffset, m4)
    }
  }

  /**
   * Compute aggregate statistic from sufficient moments.
   * @param centralMoments Length `momentOrder + 1` array of central moments (un-normalized)
   *                       needed to compute the aggregate stat.
   */
  def getStatistic(n: Double, mean: Double, centralMoments: Array[Double]): Double

  override final def eval(buffer: InternalRow): Any = {
    val n = buffer.getDouble(nOffset)
    val mean = buffer.getDouble(meanOffset)
    val moments = Array.ofDim[Double](momentOrder + 1)
    moments(0) = 1.0
    moments(1) = 0.0
    if (momentOrder >= 2) {
      moments(2) = buffer.getDouble(secondMomentOffset)
    }
    if (momentOrder >= 3) {
      moments(3) = buffer.getDouble(thirdMomentOffset)
    }
    if (momentOrder >= 4) {
      moments(4) = buffer.getDouble(fourthMomentOffset)
    }

    getStatistic(n, mean, moments)
  }
}

case class Variance(child: Expression,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0) extends CentralMomentAgg(child) {

  override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
    copy(mutableAggBufferOffset = newMutableAggBufferOffset)

  override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
    copy(inputAggBufferOffset = newInputAggBufferOffset)

  override def prettyName: String = "variance"

  override protected val momentOrder = 2

  override def getStatistic(n: Double, mean: Double, moments: Array[Double]): Double = {
    require(moments.length == momentOrder + 1,
      s"$prettyName requires ${momentOrder + 1} central moments, received: ${moments.length}")

    if (n == 0.0) Double.NaN else moments(2) / n
  }
}

case class VarianceSamp(child: Expression,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0) extends CentralMomentAgg(child) {

  override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
    copy(mutableAggBufferOffset = newMutableAggBufferOffset)

  override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
    copy(inputAggBufferOffset = newInputAggBufferOffset)

  override def prettyName: String = "variance_samp"

  override protected val momentOrder = 2

  override def getStatistic(n: Double, mean: Double, moments: Array[Double]): Double = {
    require(moments.length == momentOrder + 1,
      s"$prettyName requires ${momentOrder + 1} central moment, received: ${moments.length}")

    if (n == 0.0 || n == 1.0) Double.NaN else moments(2) / (n - 1.0)
  }
}

case class VariancePop(child: Expression,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0) extends CentralMomentAgg(child) {

  override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
    copy(mutableAggBufferOffset = newMutableAggBufferOffset)

  override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
    copy(inputAggBufferOffset = newInputAggBufferOffset)

  override def prettyName: String = "variance_pop"

  override protected val momentOrder = 2

  override def getStatistic(n: Double, mean: Double, moments: Array[Double]): Double = {
    require(moments.length == momentOrder + 1,
      s"$prettyName requires ${momentOrder + 1} central moment, received: ${moments.length}")

    if (n == 0.0) Double.NaN else moments(2) / n
  }
}

case class Skewness(child: Expression,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0) extends CentralMomentAgg(child) {

  override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
    copy(mutableAggBufferOffset = newMutableAggBufferOffset)

  override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
    copy(inputAggBufferOffset = newInputAggBufferOffset)

  override def prettyName: String = "skewness"

  override protected val momentOrder = 3

  override def getStatistic(n: Double, mean: Double, moments: Array[Double]): Double = {
    require(moments.length == momentOrder + 1,
      s"$prettyName requires ${momentOrder + 1} central moments, received: ${moments.length}")
    val m2 = moments(2)
    val m3 = moments(3)
    if (n == 0.0 || m2 == 0.0) {
      Double.NaN
    } else {
      math.sqrt(n) * m3 / math.sqrt(m2 * m2 * m2)
    }
  }
}

case class Kurtosis(child: Expression,
    mutableAggBufferOffset: Int = 0,
    inputAggBufferOffset: Int = 0) extends CentralMomentAgg(child) {

  override def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate =
    copy(mutableAggBufferOffset = newMutableAggBufferOffset)

  override def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate =
    copy(inputAggBufferOffset = newInputAggBufferOffset)

  override def prettyName: String = "kurtosis"

  override protected val momentOrder = 4

  // NOTE: this is the formula for excess kurtosis, which is default for R and SciPy
  override def getStatistic(n: Double, mean: Double, moments: Array[Double]): Double = {
    require(moments.length == momentOrder + 1,
      s"$prettyName requires ${momentOrder + 1} central moments, received: ${moments.length}")
    val m2 = moments(2)
    val m4 = moments(4)
    if (n == 0.0 || m2 == 0.0) {
      Double.NaN
    } else {
      n * m4 / (m2 * m2) - 3.0
    }
  }
}
