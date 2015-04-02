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

package org.apache.spark.ml.impl.tree

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.impl.estimator.PredictorParams
import org.apache.spark.ml.param.{IntParam, Param}
import org.apache.spark.mllib.tree.configuration.{Algo => OldAlgo,
  BoostingStrategy => OldBoostingStrategy, Strategy => OldStrategy}
import org.apache.spark.mllib.tree.impurity.{Gini => OldGini, Entropy => OldEntropy,
  Impurity => OldImpurity, Variance => OldVariance}
import org.apache.spark.mllib.tree.loss.{Loss => OldLoss}
import org.apache.spark.util.Utils


/**
 * :: DeveloperApi ::
 * Parameters for Decision Tree-based algorithms.
 * @tparam M  Concrete class implementing this parameter trait
 */
@DeveloperApi
private[ml] trait DecisionTreeParams[M] extends PredictorParams {

  /**
   * Maximum depth of the tree.
   * E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
   * (default = 5)
   * @group param
   */
  val maxDepth: IntParam =
    new IntParam(this, "maxDepth", "Maximum depth of the tree." +
      " E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.",
      Some(5))

  protected var maxBins: Int = 32

  protected var minInstancesPerNode: Int = 1

  protected var minInfoGain: Double = 0.0

  protected var maxMemoryInMB: Int = 256

  protected var cacheNodeIds: Boolean = false

  protected var checkpointInterval: Int = 10

  /** @group setParam */
  def setMaxDepth(value: Int): M = {
    require(value >= 0, s"maxDepth parameter must be >= 0.  Given bad value: $value")
    set(maxDepth, value)
    this.asInstanceOf[M]
  }

  /**
   * Maximum depth of the tree.
   * E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
   * (default = 5)
   * @group getParam
   */
  def getMaxDepth: Int = get(maxDepth)

  /**
   * Maximum number of bins used for discretizing continuous features and for choosing how to split
   * on features at each node.  More bins give higher granularity.
   * Must be >= 2 and >= number of categories in any categorical feature.
   * Values < 0 are interpreted as "auto" (algorithm chooses automatically).
   * (default = 32)
   * @group setParam
   */
  def setMaxBins(maxBins: Int): M = {
    this.maxBins = maxBins
    this.asInstanceOf[M]
  }

  /**
   * Maximum number of bins used for discretizing continuous features and for choosing how to split
   * on features at each node.  More bins give higher granularity.
   * Must be >= 2 and >= number of categories in any categorical feature.
   * Values < 0 are interpreted as "auto" (algorithm chooses automatically).
   * (default = 32)
   * @group getParam
   */
  def getMaxBins: Int = maxBins

  /**
   * Minimum number of instances each child must have after split.
   * If a split cause left or right child to have less than minInstancesPerNode,
   * this split will not be considered as a valid split.
   * (default = 1)
   * @group setParam
   */
  def setMinInstancesPerNode(minInstancesPerNode: Int): M = {
    this.minInstancesPerNode = minInstancesPerNode
    this.asInstanceOf[M]
  }

  /**
   * Minimum number of instances each child must have after split.
   * If a split cause left or right child to have less than minInstancesPerNode,
   * this split will not be considered as a valid split.
   * (default = 1)
   * @group getParam
   */
  def getMinInstancesPerNode: Int = minInstancesPerNode

  /**
   * Minimum information gain for a split to be considered at a tree node.
   * (default = 0.0)
   * @group setParam
   */
  def setMinInfoGain(minInfoGain: Double): M = {
    this.minInfoGain = minInfoGain
    this.asInstanceOf[M]
  }

  /**
   * Minimum information gain for a split to be considered at a tree node.
   * (default = 0.0)
   * @group getParam
   */
  def getMinInfoGain: Double = minInfoGain

  /**
   * Maximum memory in MB allocated to histogram aggregation.
   * (default = 256 MB)
   * @group expert
   */
  def setMaxMemoryInMB(maxMemoryInMB: Int): M = {
    require(maxMemoryInMB > 0,
      s"maxMemoryInMB parameter must be > 0.  Given bad value: $maxMemoryInMB")
    this.maxMemoryInMB = maxMemoryInMB
    this.asInstanceOf[M]
  }

  /**
   * Maximum memory in MB allocated to histogram aggregation.
   * (default = 256 MB)
   * @group expert
   */
  def getMaxMemoryInMB: Int = maxMemoryInMB

  /**
   * If false, the algorithm will pass trees to executors to match instances with nodes.
   * If true, the algorithm will cache node IDs for each instance.
   * Caching can speed up training of deeper trees.
   * (default = false)
   * @group expert
   */
  def setCacheNodeIds(cacheNodeIds: Boolean): M = {
    this.cacheNodeIds = cacheNodeIds
    this.asInstanceOf[M]
  }

  /**
   * If false, the algorithm will pass trees to executors to match instances with nodes.
   * If true, the algorithm will cache node IDs for each instance.
   * Caching can speed up training of deeper trees.
   * (default = false)
   * @group expert
   */
  def getCacheNodeIds: Boolean = cacheNodeIds

  /**
   * Specifies how often to checkpoint the cached node IDs.
   * E.g. 10 means that the cache will get checkpointed every 10 iterations.
   * This is only used if cacheNodeIds is true and if the checkpoint directory is set in
   * [[org.apache.spark.SparkContext]].
   * (default = 10)
   * @group expert
   */
  def setCheckpointInterval(checkpointInterval: Int): M = {
    this.checkpointInterval = checkpointInterval
    this.asInstanceOf[M]
  }

  /**
   * Specifies how often to checkpoint the cached node IDs.
   * E.g. 10 means that the cache will get checkpointed every 10 iterations.
   * This is only used if cacheNodeIds is true and if the checkpoint directory is set in
   * [[org.apache.spark.SparkContext]].
   * (default = 10)
   * @group expert
   */
  def getCheckpointInterval: Int = checkpointInterval

  /**
   * Create a Strategy instance to use with the old API.
   * NOTE: The caller should set impurity and subsamplingRate (which is set to 1.0,
   *       the default for single trees).
   */
  private[ml] def getOldStrategy(
      categoricalFeatures: Map[Int, Int],
      numClasses: Int): OldStrategy = {
    val strategy = OldStrategy.defaultStategy(OldAlgo.Classification)
    strategy.checkpointInterval = checkpointInterval
    strategy.maxBins = maxBins
    strategy.maxDepth = maxDepth
    strategy.maxMemoryInMB = maxMemoryInMB
    strategy.minInfoGain = minInfoGain
    strategy.minInstancesPerNode = minInstancesPerNode
    strategy.useNodeIdCache = cacheNodeIds
    strategy.numClasses = numClasses
    strategy.subsamplingRate = 1.0 // default for individual trees
    strategy
  }
}

/**
 * (private trait) Parameters for Decision Tree-based classification algorithms.
 * @tparam M  Concrete class implementing this parameter trait
 */
private[ml] trait TreeClassifierParams[M] {

  protected var impurityStr: String = "gini"

  /**
   * Criterion used for information gain calculation.
   * Supported: "Entropy" and "Gini".
   * (default = Gini)
   * @param impurity  String for the impurity (case-insensitive)
   * @group setParam
   */
  def setImpurity(impurity: String): M = {
    val impurityStr = impurity.toLowerCase
    require(TreeClassifierParams.supportedImpurities.contains(impurityStr),
      s"TreeClassifierParams was given unrecognized impurity: $impurity." +
      s"  Supported options: ${TreeClassifierParams.supportedImpurities.mkString(", ")}")
    this.impurityStr = impurityStr
    this.asInstanceOf[M]
  }

  /**
   * Criterion used for information gain calculation.
   * Supported: "Entropy" and "Gini".
   * (default = Gini)
   * @group getParam
   */
  def getImpurityStr: String = impurityStr

  /** Convert new impurity to old impurity. */
  protected def getOldImpurity: OldImpurity = {
    impurityStr match {
      case "entropy" => OldEntropy
      case "gini" => OldGini
      case _ =>
        // Should never happen because of check in setter method.
        throw new RuntimeException(
          s"TreeClassifierParams was given unrecognized impurity: $impurityStr.")
    }
  }
}

private[ml] object TreeClassifierParams {
  // These options should be lowercase.
  val supportedImpurities: Array[String] = Array("entropy", "gini")
}

/**
 * (private trait) Parameters for Decision Tree-based regression algorithms.
 * @tparam M  Concrete class implementing this parameter trait
 */
private[ml] trait TreeRegressorParams[M] {

  protected var impurityStr: String = "variance"

  /**
   * Criterion used for information gain calculation.
   * Supported: "Variance".
   * (default = Variance)
   * @param impurity  String for the impurity (case-insensitive)
   * @group setParam
   */
  def setImpurity(impurity: String): M = {
    val impurityStr = impurity.toLowerCase
    require(TreeRegressorParams.supportedImpurities.contains(impurityStr),
      s"TreeRegressorParams was given unrecognized impurity: $impurity." +
        s"  Supported options: ${TreeRegressorParams.supportedImpurities.mkString(", ")}")
    this.impurityStr = impurityStr
    this.asInstanceOf[M]
  }

  /**
   * Criterion used for information gain calculation.
   * Supported: "Variance".
   * (default = Variance)
   * @group getParam
   */
  def getImpurityStr: String = impurityStr

  /** Convert new impurity to old impurity. */
  protected def getOldImpurity: OldImpurity = {
    impurityStr match {
      case "variance" => OldVariance
      case _ =>
        // Should never happen because of check in setter method.
        throw new RuntimeException(
          s"TreeRegressorParams was given unrecognized impurity: $impurityStr")
    }
  }
}

private[ml] object TreeRegressorParams {
  // These options should be lowercase.
  val supportedImpurities: Array[String] = Array("variance")
}

/**
 * (private trait) Parameters for Decision Tree-based ensemble algorithms.
 * @tparam M  Concrete class implementing this parameter trait
 */
private[ml] trait TreeEnsembleParams[M] extends DecisionTreeParams[M] {

  protected var subsamplingRate: Double = 1.0

  protected var seed: Long = Utils.random.nextLong()

  /**
   * Fraction of the training data used for learning each decision tree.
   * (default = 1.0)
   * @group setParam
   */
  def setSubsamplingRate(subsamplingRate: Double): M = {
    require(subsamplingRate > 0.0 && subsamplingRate <= 1.0,
      s"Subsampling rate must be in range (0,1]. Bad rate: $subsamplingRate")
    this.subsamplingRate = subsamplingRate
    this.asInstanceOf[M]
  }

  /**
   * Fraction of the training data used for learning each decision tree.
   * (default = 1.0)
   * @group getParam
   */
  def getSubsamplingRate: Double = subsamplingRate

  /**
   * Random seed for bootstrapping and choosing feature subsets.
   * @group setParam
   */
  def setSeed(seed: Long): M = {
    this.seed = seed
    this.asInstanceOf[M]
  }

  /**
   * Random seed for bootstrapping and choosing feature subsets.
   * @group getParam
   */
  def getSeed: Long = seed

  /**
   * Create a Strategy instance to use with the old API.
   * NOTE: The caller should set impurity and seed.
   * TODO: Remove once we move implementation to new API.
   */
  override private[ml] def getOldStrategy(
      categoricalFeatures: Map[Int, Int],
      numClasses: Int): OldStrategy = {
    val strategy = super.getOldStrategy(categoricalFeatures, numClasses)
    strategy.setSubsamplingRate(subsamplingRate)
    strategy
  }
}

/**
 * (private trait) Parameters for Random Forest algorithms.
 * @tparam M  Concrete class implementing this parameter trait
 */
private[ml] trait RandomForestParams[M] extends TreeEnsembleParams[M] {

  protected var numTrees: Int = 20

  protected var featuresPerNodeStr: String = "auto"

  /**
   * Number of trees to train (>= 1).
   * If 1, then no bootstrapping is used.  If > 1, then bootstrapping is done.
   * TODO: Change to always do bootstrapping (simpler).
   * (default = 20)
   * @group setParam
   */
  def setNumTrees(numTrees: Int): M = {
    require(numTrees >= 1,
      s"Random Forest numTrees parameter cannot be $numTrees; it must be >= 1.")
    this.numTrees = numTrees
    this.asInstanceOf[M]
  }

  /**
   * Number of trees to train (>= 1).
   * If 1, then no bootstrapping is used.  If > 1, then bootstrapping is done.
   * TODO: Change to always do bootstrapping (simpler).
   * (default = 20)
   * @group getParam
   */
  def getNumTrees: Int = numTrees

  /**
   * The number of features to consider for splits at each tree node.
   * Supported options:
   *  - "auto": Choose automatically for task:
   *            If numTrees == 1, set to "all."
   *            If numTrees > 1 (forest), set to "sqrt" for classification and
   *              to "onethird" for regression.
   *  - "all": use all features
   *  - "onethird": use 1/3 of the features
   *  - "sqrt": use sqrt(number of features)
   *  - "log2": use log2(number of features)
   * (default = "auto")
   *
   * These various settings are based on the following references:
   *  - log2: tested in Breiman (2001)
   *  - sqrt: recommended by Breiman manual for random forests
   *  - The defaults of sqrt (classification) and onethird (regression) match the R randomForest
   *    package.
   * @see [[http://www.stat.berkeley.edu/~breiman/randomforest2001.pdf  Breiman (2001)]]
   * @see [[http://www.stat.berkeley.edu/~breiman/Using_random_forests_V3.1.pdf  Breiman manual for
   *     random forests]]
   *
   * @group setParam
   */
  def setFeaturesPerNode(featuresPerNode: String): M = {
    val featuresPerNodeStr = featuresPerNode.toLowerCase
    require(RandomForestParams.supportedFeaturesPerNode.contains(featuresPerNodeStr),
      s"RandomForestParams was given unrecognized featuresPerNode: $featuresPerNode." +
        s"  Supported options: ${RandomForestParams.supportedFeaturesPerNode.mkString(", ")}")
    this.featuresPerNodeStr = featuresPerNodeStr
    this.asInstanceOf[M]
  }

  /**
   * The number of features to consider for splits at each tree node.
   * Supported options:
   *  - "auto": choose automatically for task
   *  - "all": use all features
   *  - "onethird": use 1/3 of the features
   *  - "sqrt": use sqrt(number of features)
   *  - "log2": use log2(number of features)
   * (default = "auto")
   * @group getParam
   */
  def getFeaturesPerNodeStr: String = featuresPerNodeStr
}

private[ml] object RandomForestParams {
  // These options should be lowercase.
  val supportedFeaturesPerNode: Array[String] = Array("auto", "all", "onethird", "sqrt", "log2")
}

/**
 * (private trait) Parameters for Gradient-Boosted Tree algorithms.
 * @tparam M  Concrete class implementing this parameter trait
 */
private[ml] trait GBTParams[M] extends TreeEnsembleParams[M] {

  protected var numIterations: Int = 20

  protected var learningRate: Double = 0.1

  protected var validationTol: Double = 1e-5

  /**
   * Number of trees to train (>= 1).
   * (default = 20)
   * @group setParam
   */
  def setNumIterations(numIterations: Int): M = {
    require(numIterations >= 1,
      s"Gradient Boosting numIterations parameter cannot be $numIterations; it must be >= 1.")
    this.numIterations = numIterations
    this.asInstanceOf[M]
  }

  /**
   * Number of trees to train (>= 1).
   * (default = 20)
   * @group getParam
   */
  def getNumIterations: Int = numIterations

  /**
   * Learning rate in interval (0, 1] for shrinking the contribution of each estimator.
   * (default = 0.1)
   */
  def setLearningRate(learningRate: Double): M = {
    require(learningRate > 0.0 && learningRate <= 1.0,
      s"GBT given invalid learning rate ($learningRate).  Value should be in (0,1].")
    this.learningRate = learningRate
    this.asInstanceOf[M]
  }

  /**
   * Learning rate in interval (0, 1] for shrinking the contribution of each estimator.
   * (default = 0.1)
   */
  def getLearningRate: Double = learningRate

  /**
   * Threshold for stopping early when runWithValidation is used.
   * If the error rate on the validation input changes by less than the validationTol,
   * then learning will stop early (before [[numIterations]]).
   * This parameter is ignored when run is used.
   * (default = 1e-5)
   */
  def setValidationTol(validationTol: Double): M = {
    this.validationTol = validationTol
    this.asInstanceOf[M]
  }

  /**
   * Threshold for stopping early when runWithValidation is used.
   * If the error rate on the validation input changes by less than the validationTol,
   * then learning will stop early (before [[numIterations]]).
   * This parameter is ignored when run is used.
   * (default = 1e-5)
   */
  def getValidationTol: Double = validationTol

  /**
   * Create a BoostingStrategy instance to use with the old API.
   * NOTE: The caller should set numClasses and algo.
   * TODO: Remove once we move implementation to new API.
   */
  private[ml] def getOldBoostingStrategy(
      categoricalFeatures: Map[Int, Int]): OldBoostingStrategy = {
    val strategy = super.getOldStrategy(categoricalFeatures, numClasses = 2)
    // NOTE: The old API does not support "seed" so we ignore it.
    new OldBoostingStrategy(strategy, getOldLoss, numIterations, learningRate, validationTol)
  }

  protected def getOldLoss: OldLoss
}
