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

package org.apache.spark.mllib.stat

import org.apache.spark.annotation.{DeveloperApi, Since}
import org.apache.spark.mllib.linalg.{Vectors, Vector}

/**
 * :: DeveloperApi ::
 * MultivariateOnlineSummarizer implements [[MultivariateStatisticalSummary]] to compute the mean,
 * variance, minimum, maximum, counts, and nonzero counts for samples in sparse or dense vector
 * format in a online fashion.
 *
 * Two MultivariateOnlineSummarizer can be merged together to have a statistical summary of
 * the corresponding joint dataset.
 *
 * A numerically stable algorithm is implemented to compute sample mean and variance:
 * Reference: [[http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance variance-wiki]]
 * Zero elements (including explicit zero values) are skipped when calling add(),
 * to have time complexity O(nnz) instead of O(n) for each column.
 */
@Since("1.1.0")
@DeveloperApi
class MultivariateOnlineSummarizer extends MultivariateStatisticalSummary with Serializable {

  private var n = 0
  private var currMean: Array[Double] = _
  private var currM2n: Array[Double] = _
  private var currM2: Array[Double] = _
  private var currL1: Array[Double] = _
  private var totalCnt: Long = 0
  private var totalWeightSum: Double = 0.0
  private var nnz: Array[Double] = _
  private var currMax: Array[Double] = _
  private var currMin: Array[Double] = _

  /**
   * Add a new sample to this summarizer, and update the statistical summary.
   *
   * @param sample The sample in dense/sparse vector format to be added into this summarizer.
   * @return This MultivariateOnlineSummarizer object.
   */
  @Since("1.1.0")
  def add(sample: Vector): this.type = add(sample, 1.0)

  private[spark] def add(sample: Vector, sampleWeight: Double): this.type = {
    if (sampleWeight == 0.0) return this
    require(sampleWeight > 0.0, s"sampleWeight, ${sampleWeight} has to be >= 0.0")

    if (n == 0) {
      require(sample.size > 0, s"Vector should have dimension larger than zero.")
      n = sample.size

      currMean = Array.ofDim[Double](n)
      currM2n = Array.ofDim[Double](n)
      currM2 = Array.ofDim[Double](n)
      currL1 = Array.ofDim[Double](n)
      nnz = Array.ofDim[Double](n)
      currMax = Array.fill[Double](n)(Double.MinValue)
      currMin = Array.fill[Double](n)(Double.MaxValue)
    }

    require(n == sample.size, s"Dimensions mismatch when adding new sample." +
      s" Expecting $n but got ${sample.size}.")

    val localCurrMean = currMean
    val localCurrM2n = currM2n
    val localCurrM2 = currM2
    val localCurrL1 = currL1
    val localNnz = nnz
    val localCurrMax = currMax
    val localCurrMin = currMin
    sample.foreachActive { (index, value) =>
      if (value != 0.0) {
        if (localCurrMax(index) < value) {
          localCurrMax(index) = value
        }
        if (localCurrMin(index) > value) {
          localCurrMin(index) = value
        }

        val prevMean = localCurrMean(index)
        val diff = value - prevMean
        localCurrMean(index) = prevMean + sampleWeight * diff / (localNnz(index) + sampleWeight)
        localCurrM2n(index) += sampleWeight * (value - localCurrMean(index)) * diff
        localCurrM2(index) += sampleWeight * value * value
        localCurrL1(index) += sampleWeight * math.abs(value)

        localNnz(index) += sampleWeight
      }
    }

    totalWeightSum += sampleWeight
    totalCnt += 1
    this
  }

  /**
   * Merge another MultivariateOnlineSummarizer, and update the statistical summary.
   * (Note that it's in place merging; as a result, `this` object will be modified.)
   *
   * @param other The other MultivariateOnlineSummarizer to be merged.
   * @return This MultivariateOnlineSummarizer object.
   */
  @Since("1.1.0")
  def merge(other: MultivariateOnlineSummarizer): this.type = {
    if (this.totalWeightSum != 0.0 && other.totalWeightSum != 0.0) {
      require(n == other.n, s"Dimensions mismatch when merging with another summarizer. " +
        s"Expecting $n but got ${other.n}.")
      totalCnt += other.totalCnt
      totalWeightSum += other.totalWeightSum
      var i = 0
      while (i < n) {
        val thisNnz = nnz(i)
        val otherNnz = other.nnz(i)
        val totalNnz = thisNnz + otherNnz
        if (totalNnz != 0.0) {
          val deltaMean = other.currMean(i) - currMean(i)
          // merge mean together
          currMean(i) += deltaMean * otherNnz / totalNnz
          // merge m2n together
          currM2n(i) += other.currM2n(i) + deltaMean * deltaMean * thisNnz * otherNnz / totalNnz
          // merge m2 together
          currM2(i) += other.currM2(i)
          // merge l1 together
          currL1(i) += other.currL1(i)
          // merge max and min
          currMax(i) = math.max(currMax(i), other.currMax(i))
          currMin(i) = math.min(currMin(i), other.currMin(i))
        }
        nnz(i) = totalNnz
        i += 1
      }
    } else if (totalWeightSum == 0.0 && other.totalWeightSum != 0.0) {
      this.n = other.n
      this.currMean = other.currMean.clone()
      this.currM2n = other.currM2n.clone()
      this.currM2 = other.currM2.clone()
      this.currL1 = other.currL1.clone()
      this.totalCnt = other.totalCnt
      this.totalWeightSum = other.totalWeightSum
      this.nnz = other.nnz.clone()
      this.currMax = other.currMax.clone()
      this.currMin = other.currMin.clone()
    }
    this
  }

  /**
   * Sample mean of each dimension.
   *
   */
  @Since("1.1.0")
  override def mean: Vector = {
    require(totalWeightSum >= 1.0, s"The effective number of samples should be " +
      s"greater than or equal to 1.0, but $totalWeightSum.")

    val realMean = Array.ofDim[Double](n)
    var i = 0
    while (i < n) {
      realMean(i) = currMean(i) * (nnz(i) / totalWeightSum)
      i += 1
    }
    Vectors.dense(realMean)
  }

  /**
   * Sample variance of each dimension.
   *
   */
  @Since("1.1.0")
  override def variance: Vector = {
    require(totalWeightSum >= 1.0, s"The effective number of samples should be " +
      s"greater than or equal to 1.0, but $totalWeightSum.")

    val realVariance = Array.ofDim[Double](n)

    val denominator = totalWeightSum - 1.0

    // Sample variance is computed, if the denominator is less than 0, the variance is just 0.
    if (denominator > 0.0) {
      val deltaMean = currMean
      var i = 0
      val len = currM2n.length
      while (i < len) {
        realVariance(i) = currM2n(i) + deltaMean(i) * deltaMean(i) * nnz(i) *
          (totalWeightSum - nnz(i)) / totalWeightSum
        realVariance(i) /= denominator
        i += 1
      }
    }
    Vectors.dense(realVariance)
  }

  /**
   * Sample size.
   *
   */
  @Since("1.1.0")
  override def count: Long = totalCnt

  private[spark] def weightedCount: Double = totalWeightSum

  /**
   * Number of nonzero elements in each dimension.
   *
   */
  @Since("1.1.0")
  override def numNonzeros: Vector = {
    require(totalWeightSum >= 1.0, s"The effective number of samples should be " +
      s"greater than or equal to 1.0, but $totalWeightSum.")

    Vectors.dense(nnz)
  }

  /**
   * Maximum value of each dimension.
   *
   */
  @Since("1.1.0")
  override def max: Vector = {
    require(totalWeightSum >= 1.0, s"The effective number of samples should be " +
      s"greater than or equal to 1.0, but $totalWeightSum.")

    var i = 0
    while (i < n) {
      if ((nnz(i) < totalWeightSum) && (currMax(i) < 0.0)) currMax(i) = 0.0
      i += 1
    }
    Vectors.dense(currMax)
  }

  /**
   * Minimum value of each dimension.
   *
   */
  @Since("1.1.0")
  override def min: Vector = {
    require(totalWeightSum >= 1.0, s"The effective number of samples should be " +
      s"greater than or equal to 1.0, but $totalWeightSum.")

    var i = 0
    while (i < n) {
      if ((nnz(i) < totalWeightSum) && (currMin(i) > 0.0)) currMin(i) = 0.0
      i += 1
    }
    Vectors.dense(currMin)
  }

  /**
   * L2 (Euclidian) norm of each dimension.
   *
   */
  @Since("1.2.0")
  override def normL2: Vector = {
    require(totalWeightSum >= 1.0, s"The effective number of samples should be " +
      s"greater than or equal to 1.0, but $totalWeightSum.")

    val realMagnitude = Array.ofDim[Double](n)

    var i = 0
    val len = currM2.length
    while (i < len) {
      realMagnitude(i) = math.sqrt(currM2(i))
      i += 1
    }
    Vectors.dense(realMagnitude)
  }

  /**
   * L1 norm of each dimension.
   *
   */
  @Since("1.2.0")
  override def normL1: Vector = {
    require(totalWeightSum >= 1.0, s"The effective number of samples should be " +
      s"greater than or equal to 1.0, but $totalWeightSum.")

    Vectors.dense(currL1)
  }
}
