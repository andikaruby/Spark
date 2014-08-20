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

package org.apache.spark.mllib.tree.impurity

import org.apache.spark.annotation.{DeveloperApi, Experimental}

/**
 * :: Experimental ::
 * Class for calculating the
 * [[http://en.wikipedia.org/wiki/Decision_tree_learning#Gini_impurity Gini impurity]]
 * during binary classification.
 */
@Experimental
object Gini extends Impurity {

  /**
   * :: DeveloperApi ::
   * information calculation for multiclass classification
   * @param counts Array[Double] with counts for each label
   * @param totalCount sum of counts for all labels
   * @return information value, or 0 if totalCount = 0
   */
  @DeveloperApi
  override def calculate(counts: Array[Double], totalCount: Double): Double = {
    if (totalCount == 0) {
      return 0
    }
    val numClasses = counts.length
    var impurity = 1.0
    var classIndex = 0
    while (classIndex < numClasses) {
      val freq = counts(classIndex) / totalCount
      impurity -= freq * freq
      classIndex += 1
    }
    impurity
  }

  /**
   * :: DeveloperApi ::
   * variance calculation
   * @param count number of instances
   * @param sum sum of labels
   * @param sumSquares summation of squares of the labels
   * @return information value, or 0 if count = 0
   */
  @DeveloperApi
  override def calculate(count: Double, sum: Double, sumSquares: Double): Double =
    throw new UnsupportedOperationException("Gini.calculate")

  /**
   * Get this impurity instance.
   * This is useful for passing impurity parameters to a Strategy in Java.
   */
  def instance = this

}

private[tree] class GiniAggregator(numClasses: Int)
  extends ImpurityAggregator(numClasses) with Serializable {

  def calculate(): Double = {
    Gini.calculate(counts, counts.sum)
  }

  def copy: GiniAggregator = {
    val tmp = new GiniAggregator(counts.size)
    tmp.counts = this.counts.clone()
    tmp
  }

  def add(label: Double): Unit = {
    if (label >= counts.size) {
      throw new IllegalArgumentException(s"GiniAggregator given label $label" +
        s" but requires label < numClasses (= ${counts.size}).")
    }
    counts(label.toInt) += 1
  }

  def count: Long = counts.sum.toLong

  def predict: Double = if (count == 0) {
    0
  } else {
    indexOfLargestArrayElement(counts)
  }

  override def prob(label: Double): Double = {
    val lbl = label.toInt
    require(lbl < counts.length,
      s"GiniAggregator.prob given invalid label: $lbl (should be < ${counts.length}")
    val cnt = count
    if (cnt == 0) {
      0
    } else {
      counts(lbl) / cnt
    }
  }

  override def toString: String = {
    s"GiniAggregator(counts = [${counts.mkString(", ")}])"
  }

  def newAggregator: GiniAggregator = {
    new GiniAggregator(counts.size)
  }

}
