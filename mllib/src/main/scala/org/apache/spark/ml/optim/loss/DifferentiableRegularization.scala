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
package org.apache.spark.ml.optim.loss

import breeze.optimize.DiffFunction

trait DifferentiableRegularization[T] extends DiffFunction[T] {

  def regParam: Double

}

class L2RegularizationLoss(
    val regParam: Double,
    shouldApply: Int => Boolean,
    featuresStd: Option[Array[Double]]) extends DifferentiableRegularization[Array[Double]] {

  override def calculate(coefficients: Array[Double]): (Double, Array[Double]) = {
    var sum = 0.0
    val gradient = new Array[Double](coefficients.length)
    coefficients.indices.filter(shouldApply).foreach { j =>
      featuresStd match {
        case Some(std) =>
          if (std(j) != 0.0) {
            val temp = coefficients(j) / (std(j) * std(j))
            sum += coefficients(j) * temp
            gradient(j) = regParam * temp
          } else {
            0.0
          }
        case None =>
          sum += coefficients(j) * coefficients(j)
          gradient(j) = coefficients(j) * regParam
      }
    }
    (0.5 * sum * regParam, gradient)
  }
}
