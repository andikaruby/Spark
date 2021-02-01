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

package org.apache.spark.ml.tuning

import org.apache.spark.ml.param._

trait RandomT[T] {
  def randomT(): T
}

object RandomRanges {

  val rnd = new scala.util.Random

  private[tuning] def randomBigInt0To(x: BigInt): BigInt = {
    var randVal = BigInt(x.bitLength, rnd)
    while (randVal > x) {
      randVal = BigInt(x.bitLength, rnd)
    }
    randVal
  }

  def bigIntBetween(lower: BigInt, upper: BigInt): BigInt = {
    val diff: BigInt = upper - lower
    randomBigInt0To(diff.bitLength) + lower
  }

  implicit class RandomInt(limits: Limits[Int]) extends RandomT[Int] {
    def randomT(): Int = {
      import limits._
      bigIntBetween(BigInt(math.min(x, y)), BigInt(math.max(x, y))).intValue()
    }
  }
}


case class Limits[T: Numeric](x: T, y: T)

/**
 * "For any distribution over a sample space with a finite maximum, the maximum of 60 random
 * observations lies within the top 5% of the true maximum, with 95% probability"
 * - Evaluating Machine Learning Models by Alice Zheng
 * https://www.oreilly.com/library/view/evaluating-machine-learning/9781492048756/ch04.html
 */
class ParamRandomBuilder {

  // Java interface

  def addGrid(param: DoubleParam, values: Array[Double]): this.type = ???

  def addGrid(param: IntParam, values: Array[Int]): this.type = ???

  def addGrid(param: FloatParam, values: Array[Float]): this.type = ???

  def addGrid(param: LongParam, values: Array[Long]): this.type = ???

  // Scala interface

  def addGrid[T: RandomT](param: Param[T], values: Iterable[T]): this.type = {
    ???
  }

  def addGrid[T](param: Param[T], values: Iterable[T]): this.type = ???

  def build(): Array[ParamMap] = {
    ???
  }
}
