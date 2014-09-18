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

package org.apache.spark.mllib.optimization

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.linalg.BLAS.{axpy, dot, gemm, scal}

/**
 * :: DeveloperApi ::
 * Class used to compute the gradient for a loss function, given a single data point.
 */
@DeveloperApi
abstract class Gradient extends Serializable {
  /**
   * Compute the gradient and loss given the features of a single data point.
   *
   * @param data features for one data point
   * @param label label for this data point
   * @param weights weights/coefficients corresponding to features
   *
   * @return (gradient: Vector, loss: Double)
   */
  def compute(data: Vector, label: Double, weights: Vector): (Vector, Double)

  /**
   * Compute the gradient and loss given the features of a single data point,
   * add the gradient to a provided vector to avoid creating new objects, and return loss.
   *
   * @param data features for one data point
   * @param label label for this data point
   * @param weights weights/coefficients corresponding to features
   * @param cumGradient the computed gradient will be added to this vector
   *
   * @return loss
   */
  def compute(data: Vector, label: Double, weights: Vector, cumGradient: Vector): Double
}

/**
 * :: DeveloperApi ::
 * Compute gradient and loss for a logistic loss function, as used in binary classification.
 * See also the documentation for the precise formulation.
 */
@DeveloperApi
class LogisticGradient extends Gradient {
  override def compute(data: Vector, label: Double, weights: Vector): (Vector, Double) = {
    val margin = -1.0 * dot(data, weights)
    val gradientMultiplier = (1.0 / (1.0 + math.exp(margin))) - label
    val gradient = data.copy
    scal(gradientMultiplier, gradient)
    val loss =
      if (label > 0) {
        math.log1p(math.exp(margin)) // log1p is log(1+p) but more accurate for small p
      } else {
        math.log1p(math.exp(margin)) - margin
      }

    (gradient, loss)
  }

  override def compute(
      data: Vector,
      label: Double,
      weights: Vector,
      cumGradient: Vector): Double = {
    val margin = -1.0 * dot(data, weights)
    val gradientMultiplier = (1.0 / (1.0 + math.exp(margin))) - label
    axpy(gradientMultiplier, data, cumGradient)
    if (label > 0) {
      math.log1p(math.exp(margin))
    } else {
      math.log1p(math.exp(margin)) - margin
    }
  }
}

/**
 * :: DeveloperApi ::
 * Compute gradient and loss for a Least-squared loss function, as used in linear regression.
 * This is correct for the averaged least squares loss function (mean squared error)
 *              L = 1/n ||A weights-y||^2
 * See also the documentation for the precise formulation.
 */
@DeveloperApi
class LeastSquaresGradient extends Gradient {
  override def compute(data: Vector, label: Double, weights: Vector): (Vector, Double) = {
    val diff = dot(data, weights) - label
    val loss = diff * diff
    val gradient = data.copy
    scal(2.0 * diff, gradient)
    (gradient, loss)
  }

  override def compute(
      data: Vector,
      label: Double,
      weights: Vector,
      cumGradient: Vector): Double = {
    val diff = dot(data, weights) - label
    axpy(2.0 * diff, data, cumGradient)
    diff * diff
  }
}

/**
 * :: DeveloperApi ::
 * Compute gradient and loss for a Hinge loss function, as used in SVM binary classification.
 * See also the documentation for the precise formulation.
 * NOTE: This assumes that the labels are {0,1}
 */
@DeveloperApi
class HingeGradient extends Gradient {
  override def compute(data: Vector, label: Double, weights: Vector): (Vector, Double) = {
    val dotProduct = dot(data, weights)
    // Our loss function with {0, 1} labels is max(0, 1 - (2y – 1) (f_w(x)))
    // Therefore the gradient is -(2y - 1)*x
    val labelScaled = 2 * label - 1.0
    if (1.0 > labelScaled * dotProduct) {
      val gradient = data.copy
      scal(-labelScaled, gradient)
      (gradient, 1.0 - labelScaled * dotProduct)
    } else {
      (Vectors.sparse(weights.size, Array.empty, Array.empty), 0.0)
    }
  }

  override def compute(
      data: Vector,
      label: Double,
      weights: Vector,
      cumGradient: Vector): Double = {
    val dotProduct = dot(data, weights)
    // Our loss function with {0, 1} labels is max(0, 1 - (2y – 1) (f_w(x)))
    // Therefore the gradient is -(2y - 1)*x
    val labelScaled = 2 * label - 1.0
    if (1.0 > labelScaled * dotProduct) {
      axpy(-labelScaled, data, cumGradient)
      1.0 - labelScaled * dotProduct
    } else {
      0.0
    }
  }
}

/**
 * :: DeveloperApi ::
 * Class used to compute the gradient for a loss function, given a series of data points.
 */
@DeveloperApi
abstract class MultiModelGradient extends Serializable {
  /**
   * Compute the gradient and loss given the features of all data points.
   *
   * @param data features for one data point
   * @param label label for this data point
   * @param weights weights/coefficients corresponding to features
   *
   * @return (gradient: DenseMatrix, loss: Double)
   */
  def compute(data: Matrix, label: DenseMatrix,
                       weights: DenseMatrix): (DenseMatrix, Matrix)

  /**
   * Compute the gradient and loss given the features of a series of data point,
   * add the gradient to a provided matrix to avoid creating new objects, and return loss.
   *
   * @param data features for the data points
   * @param label label for the data points
   * @param weights weights/coefficients corresponding to features
   * @param cumGradient the computed gradient will be added to this matrix
   *
   * @return loss
   */
  def compute(data: Matrix, label: DenseMatrix,
                       weights: DenseMatrix, cumGradient: DenseMatrix): Matrix
}

/**
 * :: DeveloperApi ::
 * Compute gradient and loss for a logistic loss function, as used in binary classification.
 * See also the documentation for the precise formulation.
 */
@DeveloperApi
class MultiModelLogisticGradient extends MultiModelGradient {

  private def sigmoid(p: DenseMatrix): DenseMatrix = {
    def takeSigmoid(p: Double): Double = {
      1.0 / (math.exp(-p) + 1.0)
    }
    p.map(takeSigmoid)
  }

  override def compute(data: Matrix, label: DenseMatrix,
                       weights: DenseMatrix): (DenseMatrix, Matrix) = {
    val margin = data transposeMultiply weights
    val gradient = DenseMatrix.zeros(weights.numRows, weights.numCols)

    gemm(false, false, 1.0, data, sigmoid(margin).elementWiseOperateOnColumnsInPlace(_ - _, label),
      0.0, gradient)

    val negativeLabels = label.compare(0.0, _ == _)
    val addMargin = margin.elementWiseOperateOnColumns(_ * _, negativeLabels)

    val loss = margin.update(v => math.log1p(math.exp(-v))).
      elementWiseOperateInPlace(_ + _, addMargin)

    val lossVector =
      if (data.isInstanceOf[DenseMatrix]) {
        val numFeatures = data.numRows
        val zeroEntries = data.compare(0.0, _ == _)
        val shouldSkip = zeroEntries.colSums.compareInPlace(numFeatures, _ == _)
        loss.colSums(false, shouldSkip)
      } else {
        loss.colSums
      }
    (gradient, lossVector)
  }

  override def compute(data: Matrix,
                       label: DenseMatrix,
                       weights: DenseMatrix,
                       cumGradient: DenseMatrix): Matrix = {
    val margin = data transposeMultiply weights
    gemm(false, false, 1.0, data, sigmoid(margin).elementWiseOperateOnColumnsInPlace(_ - _, label),
      1.0, cumGradient)

    val negativeLabels = label.compare(0.0, _ == _)
    val addMargin = margin.elementWiseOperateOnColumns(_ * _, negativeLabels)

    val loss = margin.update(v => math.log1p(math.exp(-v))).
      elementWiseOperateInPlace(_ + _, addMargin)

    if (data.isInstanceOf[DenseMatrix]) {
      val numFeatures = data.numRows
      val zeroEntries = data.compare(0.0, _ == _)
      val shouldSkip = zeroEntries.colSums.compareInPlace(numFeatures, _ == _)
      loss.colSums(false, shouldSkip)
    } else {
      loss.colSums
    }
  }
}

/**
 * :: DeveloperApi ::
 * Compute gradient and loss for a Least-squared loss function, as used in linear regression.
 * This is correct for the averaged least squares loss function (mean squared error)
 *              L = 1/n ||A weights-y||^2
 * See also the documentation for the precise formulation.
 */
@DeveloperApi
class MultiModelLeastSquaresGradient extends MultiModelGradient {
  override def compute(data: Matrix, label: DenseMatrix,
                       weights: DenseMatrix): (DenseMatrix, Matrix) = {

    val diff = (data transposeMultiply weights).elementWiseOperateOnColumnsInPlace(_ - _, label)

    val gradient = DenseMatrix.zeros(weights.numRows, weights.numCols)

    gemm(false, false, 2.0, data, diff, 0.0, gradient)

    val loss = diff.update(v => v * v)

    val lossVector =
      if (data.isInstanceOf[DenseMatrix]) {
        val numFeatures = data.numRows
        val zeroEntries = data.compare(0.0, _ == _)
        val shouldSkip = zeroEntries.colSums.compareInPlace(numFeatures, _ == _)
        loss.colSums(false, shouldSkip)
      } else {
        loss.colSums
      }
    (gradient, lossVector)
  }

  override def compute(data: Matrix,
                       label: DenseMatrix,
                       weights: DenseMatrix,
                       cumGradient: DenseMatrix): Matrix = {
    val diff = (data transposeMultiply weights).elementWiseOperateOnColumnsInPlace(_ - _, label)

    gemm(false, false, 2.0, data, diff, 1.0, cumGradient)
    val loss = diff.update(v => v * v)

    if (data.isInstanceOf[DenseMatrix]) {
      val numFeatures = data.numRows
      val zeroEntries = data.compare(0.0, _ == _)
      val shouldSkip = zeroEntries.colSums.compareInPlace(numFeatures, _ == _)
      loss.colSums(false, shouldSkip)
    } else {
      loss.colSums
    }
  }
}


/**
 * :: DeveloperApi ::
 * Compute gradient and loss for a Hinge loss function, as used in SVM binary classification.
 * See also the documentation for the precise formulation.
 * NOTE: This assumes that the labels are {0,1}
 */
@DeveloperApi
class MultiModelHingeGradient extends MultiModelGradient {
  override def compute(data: Matrix, label: DenseMatrix,
                       weights: DenseMatrix): (DenseMatrix, Matrix) = {

    val dotProduct = data transposeMultiply weights
    // Our loss function with {0, 1} labels is max(0, 1 - (2y – 1) (f_w(x)))
    // Therefore the gradient is -(2y - 1)*x
    val labelScaled = new DenseMatrix(1, label.numRows, label.map(_ * 2 - 1.0).values)

    dotProduct.elementWiseOperateOnColumnsInPlace(_ * _, labelScaled)

    val gradientMultiplier = data.elementWiseOperateOnRows(_ * _, labelScaled.negInPlace)
    val gradient = DenseMatrix.zeros(weights.numRows, weights.numCols)
    val activeExamples = dotProduct.compare(1.0, _ < _) // Examples where the hinge is active

    gemm(false, false, 1.0, gradientMultiplier, activeExamples, 1.0, gradient)

    val loss = activeExamples.elementWiseOperateInPlace(_ * _, dotProduct.update(1 - _))

    val lossVector =
      if (data.isInstanceOf[DenseMatrix]) {
        val numFeatures = data.numRows
        val zeroEntries = data.compare(0.0, _ == _)
        val shouldSkip = zeroEntries.colSums.compareInPlace(numFeatures, _ == _)
        loss.colSums(false, shouldSkip)
      } else {
        loss.colSums
      }
    (gradient, lossVector)
  }

  override def compute(data: Matrix, label: DenseMatrix,
                       weights: DenseMatrix, cumGradient: DenseMatrix): Matrix = {

    val dotProduct = data transposeMultiply weights
    // Our loss function with {0, 1} labels is max(0, 1 - (2y – 1) (f_w(x)))
    // Therefore the gradient is -(2y - 1)*x
    val labelScaled = new DenseMatrix(1, label.numRows, label.map(_ * 2 - 1.0).values)
    dotProduct.elementWiseOperateOnColumnsInPlace(_ * _, labelScaled)

    val gradientMultiplier = data.elementWiseOperateOnRows(_ * _, labelScaled.negInPlace)

    val activeExamples = dotProduct.compare(1.0, _ < _) // Examples where the hinge is active

    gemm(false, false, 1.0, gradientMultiplier, activeExamples, 1.0, cumGradient)

    val loss = activeExamples.elementWiseOperateInPlace(_ * _, dotProduct.update(1 - _))

    if (data.isInstanceOf[DenseMatrix]) {
      val numFeatures = data.numRows
      val zeroEntries = data.compare(0.0, _ == _)
      val shouldSkip = zeroEntries.colSums.compareInPlace(numFeatures, _ == _)
      loss.colSums(false, shouldSkip)
    } else {
      loss.colSums
    }
  }
}