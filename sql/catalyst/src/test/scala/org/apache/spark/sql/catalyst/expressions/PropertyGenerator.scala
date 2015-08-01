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

package org.apache.spark.sql.catalyst.expressions

import java.sql.{Date, Timestamp}

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.Matchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.CalendarInterval


trait PropertyGenerator extends GeneratorDrivenPropertyChecks with Matchers {

  lazy val byteLiteralGen: Gen[Literal] =
    for { b <- Arbitrary.arbByte.arbitrary } yield Literal.create(b, ByteType)

  lazy val shortLiteralGen: Gen[Literal] =
    for { s <- Arbitrary.arbShort.arbitrary } yield Literal.create(s, ShortType)

  lazy val integerLiteralGen: Gen[Literal] =
    for { i <- Arbitrary.arbInt.arbitrary } yield Literal.create(i, IntegerType)

  lazy val longLiteralGen: Gen[Literal] =
    for { l <- Arbitrary.arbLong.arbitrary } yield Literal.create(l, LongType)

  lazy val floatLiteralGen: Gen[Literal] =
    for { f <- Arbitrary.arbFloat.arbitrary } yield Literal.create(f, FloatType)

  lazy val doubleLiteralGen: Gen[Literal] =
    for { d <- Arbitrary.arbDouble.arbitrary } yield Literal.create(d, DoubleType)

  // TODO: decimal type

  lazy val stringLiteralGen: Gen[Literal] =
    for { s <- Arbitrary.arbString.arbitrary } yield Literal.create(s, StringType)

  lazy val binaryLiteralGen: Gen[Literal] =
    for { ab <- Gen.listOf[Byte](Arbitrary.arbByte.arbitrary) }
      yield Literal.create(ab, BinaryType)

  lazy val booleanLiteralGen: Gen[Literal] =
    for { b <- Arbitrary.arbBool.arbitrary } yield Literal.create(b, BooleanType)

  lazy val dateLiteralGen: Gen[Literal] =
    for { d <- Arbitrary.arbInt.arbitrary } yield Literal.create(new Date(d), DateType)

  lazy val timestampLiteralGen: Gen[Literal] =
    for { t <- Arbitrary.arbLong.arbitrary } yield Literal.create(new Timestamp(t), TimestampType)

  lazy val calendarIntervalLiterGen: Gen[Literal] =
    for { m <- Arbitrary.arbInt.arbitrary; s <- Arbitrary.arbLong.arbitrary}
      yield Literal.create(new CalendarInterval(m, s), CalendarIntervalType)


  // Sometimes, it would be quite expensive when unlimited value is used,
  // for example, the `times` arguments for StringRepeat would hang the test 'forever'
  // if it's tested against Int.MaxValue by ScalaCheck, therefore, use values from a limited
  // range is more reasonable
  val limitedIntegerLiteralGen: Gen[Literal] =
    for {i <- Gen.choose(-100, 100)} yield Literal.create(i, IntegerType)

  def randomGen(dt: DataType): Gen[Literal] = {
    dt match {
      case ByteType => byteLiteralGen
      case ShortType => shortLiteralGen
      case IntegerType => integerLiteralGen
      case LongType => longLiteralGen
      case DoubleType => doubleLiteralGen
      case FloatType => floatLiteralGen
      case DateType => dateLiteralGen
      case TimestampType => timestampLiteralGen
      case BooleanType => booleanLiteralGen
      case StringType => stringLiteralGen
      case BinaryType => binaryLiteralGen
      case CalendarIntervalType => calendarIntervalLiterGen
      case dt => throw new IllegalArgumentException(s"not supported type $dt")
    }
  }
}
