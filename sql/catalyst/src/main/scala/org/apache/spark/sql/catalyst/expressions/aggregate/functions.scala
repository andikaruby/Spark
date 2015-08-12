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

import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types._

case class Average(child: Expression) extends AlgebraicAggregate {

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

  private val currentSum = AttributeReference("currentSum", sumDataType)()
  private val currentCount = AttributeReference("currentCount", LongType)()

  override val bufferAttributes = currentSum :: currentCount :: Nil

  override val initialValues = Seq(
    /* currentSum = */ Cast(Literal(0), sumDataType),
    /* currentCount = */ Literal(0L)
  )

  override val updateExpressions = Seq(
    /* currentSum = */
    Add(
      currentSum,
      Coalesce(Cast(child, sumDataType) :: Cast(Literal(0), sumDataType) :: Nil)),
    /* currentCount = */ If(IsNull(child), currentCount, currentCount + 1L)
  )

  override val mergeExpressions = Seq(
    /* currentSum = */ currentSum.left + currentSum.right,
    /* currentCount = */ currentCount.left + currentCount.right
  )

  // If all input are nulls, currentCount will be 0 and we will get null after the division.
  override val evaluateExpression = child.dataType match {
    case DecimalType.Fixed(p, s) =>
      // increase the precision and scale to prevent precision loss
      val dt = DecimalType.bounded(p + 14, s + 4)
      Cast(Cast(currentSum, dt) / Cast(currentCount, dt), resultType)
    case _ =>
      Cast(currentSum, resultType) / Cast(currentCount, resultType)
  }
}

case class Count(child: Expression) extends AlgebraicAggregate {
  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = false

  // Return data type.
  override def dataType: DataType = LongType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val currentCount = AttributeReference("currentCount", LongType)()

  override val bufferAttributes = currentCount :: Nil

  override val initialValues = Seq(
    /* currentCount = */ Literal(0L)
  )

  override val updateExpressions = Seq(
    /* currentCount = */ If(IsNull(child), currentCount, currentCount + 1L)
  )

  override val mergeExpressions = Seq(
    /* currentCount = */ currentCount.left + currentCount.right
  )

  override val evaluateExpression = Cast(currentCount, LongType)
}

/**
 * Returns the first value of `child` for a group of rows. If the first value of `child`
 * is `null`, it returns `null` (respecting nulls). Even if [[First]] is used on a already
 * sorted column, if we do partial aggregation and final aggregation (when mergeExpression
 * is used) its result will not be deterministic (unless the input table is sorted and has
 * a single partition, and we use a single reducer to do the aggregation.).
 * @param child
 */
case class First(child: Expression, ignoreNulls: Boolean) extends AlgebraicAggregate {

  def this(child: Expression) = this(child, false)

  def this(child: Expression, ignoreNulls: Expression) = this(child, ignoreNulls match {
    case Literal(b: Boolean, BooleanType) => b
    case _ =>
      throw new AnalysisException("The second argument of First should be a boolean literal.")
  })

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

  override val bufferAttributes = first :: valueSet :: Nil

  override val initialValues = Seq(
    /* first = */ Literal.create(null, child.dataType),
    /* valueSet = */ Literal.create(false, BooleanType)
  )

  override val updateExpressions = {
    val litTrue = Literal.create(true, BooleanType)
    if (ignoreNulls) {
      Seq(
        /* first = */ If(Or(valueSet, IsNull(child)), first, child),
        /* valueSet = */ If(Or(valueSet, IsNull(child)), valueSet, litTrue)
      )
    } else {
      Seq(
        /* first = */ If(valueSet, first, child),
        /* valueSet = */ litTrue
      )
    }
  }

  override val mergeExpressions = {
    val litTrue = Literal.create(true, BooleanType)
    if (ignoreNulls) {
      Seq(
        /* first = */ If(Or(valueSet.left, IsNull(first.right)), first.left, first.right),
        /* valueSet = */ If(Or(valueSet.left, IsNull(first.right)), valueSet.left, litTrue)
      )
    } else {
      Seq(
        /* first = */ If(valueSet.left, first.left, first.right),
        /* valueSet = */ litTrue
      )
    }
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
case class Last(child: Expression, ignoreNulls: Boolean) extends AlgebraicAggregate {

  def this(child: Expression) = this(child, false)

  def this(child: Expression, ignoreNulls: Expression) = this(child, ignoreNulls match {
    case Literal(b: Boolean, BooleanType) => b
    case _ =>
      throw new AnalysisException("The second argument of Last should be a boolean literal.")
  })

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Last is not a deterministic function.
  override def deterministic: Boolean = false

  // Return data type.
  override def dataType: DataType = child.dataType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val last = AttributeReference("last", child.dataType)()

  override val bufferAttributes = last :: Nil

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

case class Max(child: Expression) extends AlgebraicAggregate {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = child.dataType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val max = AttributeReference("max", child.dataType)()

  override val bufferAttributes = max :: Nil

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

case class Min(child: Expression) extends AlgebraicAggregate {

  override def children: Seq[Expression] = child :: Nil

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = child.dataType

  // Expected input data type.
  override def inputTypes: Seq[AbstractDataType] = Seq(AnyDataType)

  private val min = AttributeReference("min", child.dataType)()

  override val bufferAttributes = min :: Nil

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

case class Sum(child: Expression) extends AlgebraicAggregate {

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

  private val currentSum = AttributeReference("currentSum", sumDataType)()

  private val zero = Cast(Literal(0), sumDataType)

  override val bufferAttributes = currentSum :: Nil

  override val initialValues = Seq(
    /* currentSum = */ Literal.create(null, sumDataType)
  )

  override val updateExpressions = Seq(
    /* currentSum = */
    Coalesce(Seq(Add(Coalesce(Seq(currentSum, zero)), Cast(child, sumDataType)), currentSum))
  )

  override val mergeExpressions = {
    val add = Add(Coalesce(Seq(currentSum.left, zero)), Cast(currentSum.right, sumDataType))
    Seq(
      /* currentSum = */
      Coalesce(Seq(add, currentSum.left))
    )
  }

  override val evaluateExpression = Cast(currentSum, resultType)
}
