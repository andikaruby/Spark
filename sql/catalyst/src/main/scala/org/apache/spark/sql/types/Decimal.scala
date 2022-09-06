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

package org.apache.spark.sql.types

import java.math.{BigDecimal => JavaBigDecimal, BigInteger}

import scala.util.Try

import org.apache.spark.annotation.Unstable
import org.apache.spark.sql.catalyst.trees.SQLQueryContext
import org.apache.spark.sql.errors.QueryExecutionErrors
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.unsafe.types.UTF8String

/**
 * A mutable implementation of BigDecimal that can hold a Long if values are small enough.
 *
 * The semantics of the fields are as follows:
 * - _precision and _scale represent the SQL precision and scale we are looking for
 * - If decimalVal is set, it represents the whole decimal value
 * - Otherwise, the decimal value is longVal / (10 ** _scale)
 *
 * Note, for values between -1.0 and 1.0, precision digits are only counted after dot.
 */
@Unstable
final class Decimal(
    decimal128Enabled: Boolean, initEnabled: Boolean) extends Ordered[Decimal] with Serializable {
  import org.apache.spark.sql.types.Decimal._

  private var jdkDecimalOperation: JDKDecimalOperation = null
  private var decimal128Operation: Decimal128Operation = null

  def this(decimal128Enabled: Boolean) = this(decimal128Enabled, true)

  def this() = this(SQLConf.get.getConf(SQLConf.DECIMAL_OPERATION_IMPLEMENTATION) == "Int128")

  if (initEnabled) initDecimalOperation(decimal128Enabled)

  def initDecimalOperation(flag: Boolean): Unit = if (flag) {
    decimal128Operation = new Decimal128Operation()
  } else {
    jdkDecimalOperation = new JDKDecimalOperation()
  }

  def decimalOperation: DecimalOperation[_] = if (decimal128Enabled) {
    decimal128Operation
  } else {
    jdkDecimalOperation
  }

  def precision: Int = if (decimal128Enabled) {
    decimal128Operation.precision
  } else {
    jdkDecimalOperation.precision
  }

  def scale: Int = if (decimal128Enabled) {
    decimal128Operation.scale
  } else {
    jdkDecimalOperation.scale
  }

  /**
   * Set this Decimal to the given Long. Will have precision 20 and scale 0.
   */
  def set(longVal: Long): Decimal = {
    if (decimal128Enabled) {
      decimal128Operation.set(longVal)
    } else {
      jdkDecimalOperation.set(longVal)
    }

    this
  }

  /**
   * Set this Decimal to the given Int. Will have precision 10 and scale 0.
   */
  def set(intVal: Int): Decimal = {
    if (decimal128Enabled) {
      decimal128Operation.set(intVal)
    } else {
      jdkDecimalOperation.set(intVal)
    }

    this
  }

  /**
   * Set this Decimal to the given unscaled Long, with a given precision and scale.
   */
  def set(unscaled: Long, precision: Int, scale: Int): Decimal = {
    if (setOrNull(unscaled, precision, scale) == null) {
      throw QueryExecutionErrors.unscaledValueTooLargeForPrecisionError()
    }
    this
  }

  /**
   * Set this Decimal to the given unscaled Long, with a given precision and scale,
   * and return it, or return null if it cannot be set due to overflow.
   */
  def setOrNull(unscaled: Long, precision: Int, scale: Int): Decimal = {
    DecimalType.checkNegativeScale(scale)
    val result = if (decimal128Enabled) {
      decimal128Operation.setOrNull(unscaled, precision, scale)
    } else {
      jdkDecimalOperation.setOrNull(unscaled, precision, scale)
    }
    if (result == null) {
      return null
    }

    this
  }

  /**
   * Set this Decimal to the given BigDecimal value, with a given precision and scale.
   */
  def set(decimal: BigDecimal, precision: Int, scale: Int): Decimal = {
    DecimalType.checkNegativeScale(scale)
    if (decimal128Enabled) {
      decimal128Operation.set(decimal, precision, scale)
    } else {
      jdkDecimalOperation.set(decimal, precision, scale)
    }

    this
  }

  /**
   * Set this Decimal to the given BigDecimal value, inheriting its precision and scale.
   */
  def set(decimal: BigDecimal): Decimal = {
    if (decimal128Enabled) {
      decimal128Operation.set(decimal)
    } else {
      jdkDecimalOperation.set(decimal)
    }

    this
  }

  /**
   * If the value is not in the range of long, convert it to BigDecimal and
   * the precision and scale are based on the converted value.
   *
   * This code avoids BigDecimal object allocation as possible to improve runtime efficiency
   */
  def set(bigintval: BigInteger): Decimal = {
    if (decimal128Enabled) {
      decimal128Operation.set(bigintval)
    } else {
      jdkDecimalOperation.set(bigintval)
    }

    this
  }

  /**
   * Set this Decimal to the given Decimal value.
   */
  def set(decimal: Decimal): Decimal = {
    if (decimal128Enabled) {
      decimal128Operation.set(decimal.decimal128Operation)
    } else {
      jdkDecimalOperation.set(decimal.jdkDecimalOperation)
    }

    this
  }

  def toBigDecimal: BigDecimal = if (decimal128Enabled) {
    decimal128Operation.toBigDecimal
  } else {
    jdkDecimalOperation.toBigDecimal
  }

  def toJavaBigDecimal: java.math.BigDecimal = if (decimal128Enabled) {
    decimal128Operation.toJavaBigDecimal
  } else {
    jdkDecimalOperation.toJavaBigDecimal
  }

  def toScalaBigInt: BigInt = if (decimal128Enabled) {
    decimal128Operation.toScalaBigInt
  } else {
    jdkDecimalOperation.toScalaBigInt
  }

  def toJavaBigInteger: java.math.BigInteger = if (decimal128Enabled) {
    decimal128Operation.toJavaBigInteger
  } else {
    jdkDecimalOperation.toJavaBigInteger
  }

  def toUnscaledLong: Long = if (decimal128Enabled) {
    decimal128Operation.toUnscaledLong
  } else {
    jdkDecimalOperation.toUnscaledLong
  }

  override def toString: String = toBigDecimal.toString()

  def toPlainString: String = toJavaBigDecimal.toPlainString

  def toDebugString: String = if (decimal128Enabled) {
    decimal128Operation.toDebugString
  } else {
    jdkDecimalOperation.toDebugString
  }

  def toDouble: Double = toBigDecimal.doubleValue

  def toFloat: Float = toBigDecimal.floatValue

  def toLong: Long = if (decimal128Enabled) {
    decimal128Operation.toLong
  } else {
    jdkDecimalOperation.toLong
  }

  def toInt: Int = toLong.toInt

  def toShort: Short = toLong.toShort

  def toByte: Byte = toLong.toByte

  /**
   * @return the Byte value that is equal to the rounded decimal.
   * @throws ArithmeticException if the decimal is too big to fit in Byte type.
   */
  private[sql] def roundToByte(): Byte = if (decimal128Enabled) {
    decimal128Operation.roundToNumeric[Byte](
      this, ByteType, Byte.MaxValue, Byte.MinValue) (_.toByte) (_.toByte)
  } else {
    jdkDecimalOperation.roundToNumeric[Byte](
      this, ByteType, Byte.MaxValue, Byte.MinValue) (_.toByte) (_.toByte)
  }

  /**
   * @return the Short value that is equal to the rounded decimal.
   * @throws ArithmeticException if the decimal is too big to fit in Short type.
   */
  private[sql] def roundToShort(): Short = if (decimal128Enabled) {
    decimal128Operation.roundToNumeric[Short](
      this, ShortType, Short.MaxValue, Short.MinValue) (_.toShort) (_.toShort)
  } else {
    jdkDecimalOperation.roundToNumeric[Short](
      this, ShortType, Short.MaxValue, Short.MinValue) (_.toShort) (_.toShort)
  }

  /**
   * @return the Int value that is equal to the rounded decimal.
   * @throws ArithmeticException if the decimal too big to fit in Int type.
   */
  private[sql] def roundToInt(): Int = if (decimal128Enabled) {
    decimal128Operation.roundToNumeric[Int](
      this, IntegerType, Int.MaxValue, Int.MinValue) (_.toInt) (_.toInt)
  } else {
    jdkDecimalOperation.roundToNumeric[Int](
      this, IntegerType, Int.MaxValue, Int.MinValue) (_.toInt) (_.toInt)
  }

  /**
   * @return the Long value that is equal to the rounded decimal.
   * @throws ArithmeticException if the decimal too big to fit in Long type.
   */
  private[sql] def roundToLong(): Long = if (decimal128Enabled) {
    decimal128Operation.roundToLong(this)
  } else {
    jdkDecimalOperation.roundToLong(this)
  }

  /**
   * Update precision and scale while keeping our value the same, and return true if successful.
   *
   * @return true if successful, false if overflow would occur
   */
  def changePrecision(precision: Int, scale: Int): Boolean = {
    changePrecision(precision, scale, ROUND_HALF_UP)
  }

  /**
   * Create new `Decimal` with given precision and scale.
   *
   * @return a non-null `Decimal` value if successful. Otherwise, if `nullOnOverflow` is true, null
   *         is returned; if `nullOnOverflow` is false, an `ArithmeticException` is thrown.
   */
  private[sql] def toPrecision(
      precision: Int,
      scale: Int,
      roundMode: BigDecimal.RoundingMode.Value = ROUND_HALF_UP,
      nullOnOverflow: Boolean = true,
      context: SQLQueryContext = null): Decimal = {
    val copy = clone()
    if (copy.changePrecision(precision, scale, roundMode)) {
      copy
    } else {
      if (nullOnOverflow) {
        null
      } else {
        throw QueryExecutionErrors.cannotChangeDecimalPrecisionError(
          this, precision, scale, context)
      }
    }
  }

  /**
   * Update precision and scale while keeping our value the same, and return true if successful.
   *
   * @return true if successful, false if overflow would occur
   */
  private[sql] def changePrecision(
      precision: Int,
      scale: Int,
      roundMode: BigDecimal.RoundingMode.Value): Boolean = if (decimal128Enabled) {
    decimal128Operation.changePrecision(precision, scale, roundMode)
  } else {
    jdkDecimalOperation.changePrecision(precision, scale, roundMode)
  }

  override def clone(): Decimal = new Decimal().set(this)

  override def compare(other: Decimal): Int = if (decimal128Enabled) {
    decimal128Operation.compare(other.decimal128Operation)
  } else {
    jdkDecimalOperation.compare(other.jdkDecimalOperation)
  }

  override def equals(other: Any): Boolean = other match {
    case d: Decimal =>
      compare(d) == 0
    case _ =>
      false
  }

  override def hashCode(): Int = toBigDecimal.hashCode()

  def isZero: Boolean = if (decimal128Enabled) {
    decimal128Operation.isZero
  } else {
    jdkDecimalOperation.isZero
  }

  // We should follow DecimalPrecision promote if use longVal for add and subtract:
  // Operation    Result Precision                        Result Scale
  // ------------------------------------------------------------------------
  // e1 + e2      max(s1, s2) + max(p1-s1, p2-s2) + 1     max(s1, s2)
  // e1 - e2      max(s1, s2) + max(p1-s1, p2-s2) + 1     max(s1, s2)
  def + (that: Decimal): Decimal = {
    val decimal = new Decimal(decimal128Enabled, false)
    if (decimal128Enabled) {
      decimal.decimal128Operation = decimal128Operation.add(that.decimal128Operation)
    } else {
      decimal.jdkDecimalOperation = jdkDecimalOperation.add(that.jdkDecimalOperation)
    }

    decimal
  }

  def - (that: Decimal): Decimal = {
    val decimal = new Decimal
    if (decimal128Enabled) {
      decimal.decimal128Operation = decimal128Operation.subtract(that.decimal128Operation)
    } else {
      decimal.jdkDecimalOperation = jdkDecimalOperation.subtract(that.jdkDecimalOperation)
    }

    decimal
  }

  // TypeCoercion will take care of the precision, scale of result
  def * (that: Decimal): Decimal = {
    val decimal = new Decimal
    if (decimal128Enabled) {
      decimal.decimal128Operation = decimal128Operation.multiply(that.decimal128Operation)
    } else {
      decimal.jdkDecimalOperation = jdkDecimalOperation.multiply(that.jdkDecimalOperation)
    }

    decimal
  }

  def / (that: Decimal): Decimal = if (that.isZero) {
    null
  } else {
    val decimal = new Decimal
    if (decimal128Enabled) {
      decimal.decimal128Operation = decimal128Operation.divide(that.decimal128Operation)
    } else {
      decimal.jdkDecimalOperation = jdkDecimalOperation.divide(that.jdkDecimalOperation)
    }

    decimal
  }

  def % (that: Decimal): Decimal = if (that.isZero) {
    null
  } else {
    val decimal = new Decimal
    if (decimal128Enabled) {
      decimal.decimal128Operation = decimal128Operation.remainder(that.decimal128Operation)
    } else {
      decimal.jdkDecimalOperation = jdkDecimalOperation.remainder(that.jdkDecimalOperation)
    }

    decimal
  }

  def quot(that: Decimal): Decimal = if (that.isZero) {
    null
  } else {
    val decimal = new Decimal
    if (decimal128Enabled) {
      decimal.decimal128Operation = decimal128Operation.quot(that.decimal128Operation)
    } else {
      decimal.jdkDecimalOperation = jdkDecimalOperation.quot(that.jdkDecimalOperation)
    }

    decimal
  }

  def remainder(that: Decimal): Decimal = this % that

  def unary_- : Decimal = {
    val decimal = new Decimal
    if (decimal128Enabled) {
      decimal.decimal128Operation = decimal128Operation.negative
    } else {
      decimal.jdkDecimalOperation = jdkDecimalOperation.negative
    }

    decimal
  }

  def abs: Decimal = if (this < Decimal.ZERO) this.unary_- else this

  def floor: Decimal = if (scale == 0) this else {
    val newPrecision = DecimalType.bounded(precision - scale + 1, 0).precision
    toPrecision(newPrecision, 0, ROUND_FLOOR, nullOnOverflow = false)
  }

  def ceil: Decimal = if (scale == 0) this else {
    val newPrecision = DecimalType.bounded(precision - scale + 1, 0).precision
    toPrecision(newPrecision, 0, ROUND_CEILING, nullOnOverflow = false)
  }
}

@Unstable
object Decimal {
  val ROUND_HALF_UP = BigDecimal.RoundingMode.HALF_UP
  val ROUND_HALF_EVEN = BigDecimal.RoundingMode.HALF_EVEN
  val ROUND_CEILING = BigDecimal.RoundingMode.CEILING
  val ROUND_FLOOR = BigDecimal.RoundingMode.FLOOR

  /** Maximum number of decimal digits an Int can represent */
  val MAX_INT_DIGITS = 9

  /** Maximum number of decimal digits a Long can represent */
  val MAX_LONG_DIGITS = 18

  val POW_10 = Array.tabulate[Long](MAX_LONG_DIGITS + 1)(i => math.pow(10, i).toLong)

  private[sql] val ZERO = new Decimal(false).set(0)
  private[sql] val ONE = new Decimal(false).set(1)

  private[sql] val ZERO128 = new Decimal(true).set(0)
  private[sql] val ONE128 = new Decimal(true).set(1)

  def apply(value: Double): Decimal = new Decimal().set(value)

  def apply(value: Long): Decimal = new Decimal().set(value)

  def apply(value: Int): Decimal = new Decimal().set(value)

  def apply(value: BigDecimal): Decimal = new Decimal().set(value)

  def apply(value: java.math.BigDecimal): Decimal = new Decimal().set(value)

  def apply(value: java.math.BigInteger): Decimal = new Decimal().set(value)

  def apply(value: scala.math.BigInt): Decimal = new Decimal().set(value.bigInteger)

  def apply(value: BigDecimal, precision: Int, scale: Int): Decimal =
    new Decimal().set(value, precision, scale)

  def apply(value: java.math.BigDecimal, precision: Int, scale: Int): Decimal =
    new Decimal().set(value, precision, scale)

  def apply(unscaled: Long, precision: Int, scale: Int): Decimal =
    new Decimal().set(unscaled, precision, scale)

  def apply(value: String): Decimal = new Decimal().set(BigDecimal(value))

  // This is used for RowEncoder to handle Decimal inside external row.
  def fromDecimal(value: Any): Decimal = {
    value match {
      case j: java.math.BigDecimal => apply(j)
      case d: BigDecimal => apply(d)
      case k: scala.math.BigInt => apply(k)
      case l: java.math.BigInteger => apply(l)
      case d: Decimal => d
    }
  }

  private def numDigitsInIntegralPart(bigDecimal: JavaBigDecimal): Int =
    bigDecimal.precision - bigDecimal.scale

  private def stringToJavaBigDecimal(str: UTF8String): JavaBigDecimal = {
    // According the benchmark test,  `s.toString.trim` is much faster than `s.trim.toString`.
    // Please refer to https://github.com/apache/spark/pull/26640
    new JavaBigDecimal(str.toString.trim)
  }

  def fromString(str: UTF8String): Decimal = {
    try {
      val bigDecimal = stringToJavaBigDecimal(str)
      // We fast fail because constructing a very large JavaBigDecimal to Decimal is very slow.
      // For example: Decimal("6.0790316E+25569151")
      if (numDigitsInIntegralPart(bigDecimal) > DecimalType.MAX_PRECISION &&
          !SQLConf.get.allowNegativeScaleOfDecimalEnabled) {
        null
      } else {
        Decimal(bigDecimal)
      }
    } catch {
      case _: NumberFormatException =>
        null
    }
  }

  def fromStringANSI(
      str: UTF8String,
      to: DecimalType = DecimalType.USER_DEFAULT,
      context: SQLQueryContext = null): Decimal = {
    try {
      val bigDecimal = stringToJavaBigDecimal(str)
      // We fast fail because constructing a very large JavaBigDecimal to Decimal is very slow.
      // For example: Decimal("6.0790316E+25569151")
      if (numDigitsInIntegralPart(bigDecimal) > DecimalType.MAX_PRECISION &&
          !SQLConf.get.allowNegativeScaleOfDecimalEnabled) {
        throw QueryExecutionErrors.outOfDecimalTypeRangeError(str)
      } else {
        Decimal(bigDecimal)
      }
    } catch {
      case _: NumberFormatException =>
        throw QueryExecutionErrors.invalidInputInCastToNumberError(to, str, context)
    }
  }

  /**
   * Creates a decimal from unscaled, precision and scale without checking the bounds.
   */
  def createUnsafe(unscaled: Long, precision: Int, scale: Int): Decimal = {
    DecimalType.checkNegativeScale(scale)
    val dec = new Decimal()
    dec.set(unscaled, precision, scale)
    dec
  }

  // Max precision of a decimal value stored in `numBytes` bytes
  def maxPrecisionForBytes(numBytes: Int): Int = {
    Math.round(                               // convert double to long
      Math.floor(Math.log10(                  // number of base-10 digits
        Math.pow(2, 8 * numBytes - 1) - 1)))  // max value stored in numBytes
      .asInstanceOf[Int]
  }

  // Returns the minimum number of bytes needed to store a decimal with a given `precision`.
  lazy val minBytesForPrecision = Array.tabulate[Int](39)(computeMinBytesForPrecision)

  private def computeMinBytesForPrecision(precision : Int) : Int = {
    var numBytes = 1
    while (math.pow(2.0, 8 * numBytes - 1) < math.pow(10.0, precision)) {
      numBytes += 1
    }
    numBytes
  }

  // Evidence parameters for Decimal considered either as Fractional or Integral. We provide two
  // parameters inheriting from a common trait since both traits define mkNumericOps.
  // See scala.math's Numeric.scala for examples for Scala's built-in types.

  /** Common methods for Decimal evidence parameters */
  private[sql] trait DecimalIsConflicted extends Numeric[Decimal] {
    override def plus(x: Decimal, y: Decimal): Decimal = x + y
    override def times(x: Decimal, y: Decimal): Decimal = x * y
    override def minus(x: Decimal, y: Decimal): Decimal = x - y
    override def negate(x: Decimal): Decimal = -x
    override def toDouble(x: Decimal): Double = x.toDouble
    override def toFloat(x: Decimal): Float = x.toFloat
    override def toInt(x: Decimal): Int = x.toInt
    override def toLong(x: Decimal): Long = x.toLong
    override def fromInt(x: Int): Decimal = new Decimal().set(x)
    override def compare(x: Decimal, y: Decimal): Int = x.compare(y)
    // Added from Scala 2.13; don't override to work in 2.12
    // TODO revisit once Scala 2.12 support is dropped
    def parseString(str: String): Option[Decimal] = Try(Decimal(str)).toOption
  }

  /** A [[scala.math.Fractional]] evidence parameter for Decimals. */
  private[sql] object DecimalIsFractional extends DecimalIsConflicted with Fractional[Decimal] {
    override def div(x: Decimal, y: Decimal): Decimal = x / y
  }

  /** A [[scala.math.Integral]] evidence parameter for Decimals. */
  private[sql] object DecimalAsIfIntegral extends DecimalIsConflicted with Integral[Decimal] {
    override def quot(x: Decimal, y: Decimal): Decimal = x quot y
    override def rem(x: Decimal, y: Decimal): Decimal = x % y
  }
}
