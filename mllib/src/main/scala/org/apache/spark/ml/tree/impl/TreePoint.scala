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

package org.apache.spark.ml.tree.impl

import scala.reflect.ClassTag

import org.apache.spark.ml.feature.Instance
import org.apache.spark.ml.tree.{ContinuousSplit, Split}
import org.apache.spark.ml.util.MLUtils
import org.apache.spark.rdd.RDD


/**
 * Internal representation of LabeledPoint for DecisionTree.
 * This bins feature values based on a subsampled of data as follows:
 *  (a) Continuous features are binned into ranges.
 *  (b) Unordered categorical features are binned based on subsets of feature values.
 *      "Unordered categorical features" are categorical features with low arity used in
 *      multiclass classification.
 *  (c) Ordered categorical features are binned based on feature values.
 *      "Ordered categorical features" are categorical features with high arity,
 *      or any categorical feature used in regression or binary classification.
 *
 * @param label  Label from LabeledPoint
 * @param binnedFeatures  Binned feature values.
 *                        Same length as LabeledPoint.features, but values are bin indices.
 * @param weight Sample weight for this TreePoint.
 */
private[spark] class TreePoint[@specialized(Byte, Short, Int) B: Integral: ClassTag](
    val label: Double,
    val binnedFeatures: Array[B],
    val weight: Double) extends Serializable

private[spark] object TreePoint {

  /**
   * Convert an input dataset into its TreePoint representation,
   * binning feature values in preparation for DecisionTree training.
   * @param input     Input dataset.
   * @param splits    Splits for features, of size (numFeatures, numSplits).
   * @param metadata  Learning and dataset metadata
   * @return  TreePoint dataset representation
   */
  def convertToTreeRDD[B: Integral: ClassTag](
      input: RDD[Instance],
      splits: Array[Array[Split]],
      metadata: DecisionTreeMetadata): RDD[TreePoint[B]] = {
    MLUtils.registerKryoClasses(input.sparkContext.getConf)

    // Construct arrays for featureArity for efficiency in the inner loop.
    val featureArity = new Array[Int](metadata.numFeatures)
    var i = 0
    while (i < metadata.numFeatures) {
      featureArity(i) = metadata.featureArity.getOrElse(i, 0)
      i += 1
    }
    val thresholds = featureArity.zipWithIndex.map { case (arity, idx) =>
      if (arity == 0) {
        splits(idx).map(_.asInstanceOf[ContinuousSplit].threshold)
      } else {
        Array.emptyDoubleArray
      }
    }
    input.map { instance =>
      TreePoint.labeledPointToTreePoint[B](instance, thresholds, featureArity)
    }
  }

  /**
   * Convert one LabeledPoint into its TreePoint representation.
   * @param thresholds  For each feature, split thresholds for continuous features,
   *                    empty for categorical features.
   * @param featureArity  Array indexed by feature, with value 0 for continuous and numCategories
   *                      for categorical features.
   */
  private def labeledPointToTreePoint[B: Integral: ClassTag](
      instance: Instance,
      thresholds: Array[Array[Double]],
      featureArity: Array[Int]): TreePoint[B] = {
    val numFeatures = instance.features.size
    val arr = Array.ofDim[B](numFeatures)
    var i = 0
    val inb = implicitly[Integral[B]]
    while (i < numFeatures) {
      val bin = findBin(i, instance, featureArity(i), thresholds(i))
      arr(i) = inb.fromInt(bin)
      i += 1
    }
    new TreePoint[B](instance.label, arr, instance.weight)
  }

  /**
   * Find discretized value for one (labeledPoint, feature).
   *
   * NOTE: We cannot use Bucketizer since it handles split thresholds differently than the old
   *       (mllib) tree API.  We want to maintain the same behavior as the old tree API.
   *
   * @param featureArity  0 for continuous features; number of categories for categorical features.
   */
  private def findBin(
      featureIndex: Int,
      instance: Instance,
      featureArity: Int,
      thresholds: Array[Double]): Int = {
    val featureValue = instance.features(featureIndex)

    if (featureArity == 0) {
      val idx = java.util.Arrays.binarySearch(thresholds, featureValue)
      if (idx >= 0) {
        idx
      } else {
        -idx - 1
      }
    } else {
      // Categorical feature bins are indexed by feature values.
      if (featureValue < 0 || featureValue >= featureArity) {
        throw new IllegalArgumentException(
          s"DecisionTree given invalid data:" +
            s" Feature $featureIndex is categorical with values in {0,...,${featureArity - 1}," +
            s" but a data point gives it value $featureValue.\n" +
            s"  Bad data point: $instance")
      }
      featureValue.toInt
    }
  }
}
