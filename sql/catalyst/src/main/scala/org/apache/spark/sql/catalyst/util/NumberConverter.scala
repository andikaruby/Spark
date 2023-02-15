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

package org.apache.spark.sql.catalyst.util

import java.util.Locale

import scala.annotation.tailrec
import scala.collection.mutable

import org.apache.spark.unsafe.types.UTF8String

object NumberConverter {
  /**
   * Convert numbers between different number bases.
   * @param num number to convert.
   * @param fromBase base of a given number. Should be between 2 and 36.
   * @param toBase base to convert a given number. Should be between 2 and 36 by absolute value.
   *               if it is lower then 0, then the result will be signed value, otherwise
   *               it will calculate unsigned value.
   * @return converted value in case of valid arguments, null otherwise.
   */
  def convert(num: UTF8String, fromBase: Int, toBase: Int): UTF8String = {
    if (isValidBase(fromBase, toBase)) {
      getLongestValidPrefix(num, fromBase) match {
        case None => null
        case Some(number) =>
          val bigIntNum = BigInt(number, fromBase)
          val bigIntResolvedNum = if (toBase > 0) getUnsignedBigInt(bigIntNum) else bigIntNum

          val convertedValue = bigIntResolvedNum
            .toString(math.abs(toBase))
            .toUpperCase(Locale.ROOT)
          UTF8String.fromString(convertedValue)
      }
    } else {
      null
    }
  }

  private def isValidBase(fromBase: Int, toBase: Int): Boolean =
    fromBase >= Character.MIN_RADIX &&
      fromBase <= Character.MAX_RADIX &&
      math.abs(toBase) >= Character.MIN_RADIX &&
      math.abs(toBase) <= Character.MAX_RADIX

  private def getLongestValidPrefix(num: UTF8String, radix: Int): Option[String] = {
    def isSign(c: Char) = c == '-' || c == '+'

    def isValidDigit(digit: Char) = Character.digit(digit, radix) >= 0

    @tailrec
    def getValidNumPrefix(numChars: List[Char])(
      implicit resultNums: mutable.StringBuilder): mutable.StringBuilder = numChars match {
      case head :: tail if isValidDigit(head) =>
        resultNums.append(head)
        getValidNumPrefix(tail)
      case _ => resultNums
    }

    lazy val emptyBuilder = mutable.StringBuilder.newBuilder
    val listOfNumbers = num.toString.trim().toList
    listOfNumbers match {
      case head :: tail if isSign(head) =>
        val validNums = getValidNumPrefix(tail)(mutable.StringBuilder.newBuilder)
        if (validNums.isEmpty) {
          None
        } else {
          val result = emptyBuilder.append(head).append(validNums).toString()
          Some(result)
        }
      case numsWithoutSign =>
        val validNums = getValidNumPrefix(numsWithoutSign)(emptyBuilder)
        if (validNums.isEmpty) None else Some(validNums.toString())
    }
  }

  private def getUnsignedBigInt(value: BigInt): BigInt = {
    val isValueSigned = value.signum < 0
    if (isValueSigned) {
      value + (BigInt(1) << math.max(value.bitLength, 64))
    } else {
      value
    }
  }

  def toBinary(l: Long): Array[Byte] = {
    val result = new Array[Byte](8)
    result(0) = (l >>> 56 & 0xFF).toByte
    result(1) = (l >>> 48 & 0xFF).toByte
    result(2) = (l >>> 40 & 0xFF).toByte
    result(3) = (l >>> 32 & 0xFF).toByte
    result(4) = (l >>> 24 & 0xFF).toByte
    result(5) = (l >>> 16 & 0xFF).toByte
    result(6) = (l >>> 8 & 0xFF).toByte
    result(7) = (l & 0xFF).toByte
    result
  }

  def toBinary(i: Int): Array[Byte] = {
    val result = new Array[Byte](4)
    result(0) = (i >>> 24 & 0xFF).toByte
    result(1) = (i >>> 16 & 0xFF).toByte
    result(2) = (i >>> 8 & 0xFF).toByte
    result(3) = (i & 0xFF).toByte
    result
  }

  def toBinary(s: Short): Array[Byte] = {
    val result = new Array[Byte](2)
    result(0) = (s >>> 8 & 0xFF).toByte
    result(1) = (s & 0xFF).toByte
    result
  }

  def toBinary(s: Byte): Array[Byte] = {
    val result = new Array[Byte](1)
    result(0) = s
    result
  }
}
