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

package org.apache.spark.ml.regression

import scala.collection.mutable

import breeze.linalg.{DenseVector => BDV, norm => brzNorm}
import breeze.optimize.{CachedDiffFunction, DiffFunction, LBFGS => BreezeLBFGS, OWLQN => BreezeOWLQN}

import org.apache.spark.{Logging, SparkException}
import org.apache.spark.annotation.Experimental
import org.apache.spark.ml.PredictorParams
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.linalg.BLAS._
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.stat.MultivariateOnlineSummarizer
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types.StructField
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.StatCounter

/**
 * Params for linear regression.
 */
private[regression] trait LinearRegressionParams extends PredictorParams
    with HasRegParam with HasElasticNetParam with HasMaxIter with HasTol
    with HasFitIntercept with HasStandardization with HasWeightCol

/**
 * :: Experimental ::
 * Linear regression.
 *
 * The learning objective is to minimize the squared error, with regularization.
 * The specific squared error loss function used is:
 *   L = 1/2n ||A weights - y||^2^
 *
 * This support multiple types of regularization:
 *  - none (a.k.a. ordinary least squares)
 *  - L2 (ridge regression)
 *  - L1 (Lasso)
 *  - L2 + L1 (elastic net)
 */
@Experimental
class LinearRegression(override val uid: String)
  extends Regressor[Vector, LinearRegression, LinearRegressionModel]
  with LinearRegressionParams with Logging {

  def this() = this(Identifiable.randomUID("linReg"))

  /**
   * Set the regularization parameter.
   * Default is 0.0.
   * @group setParam
   */
  def setRegParam(value: Double): this.type = set(regParam, value)
  setDefault(regParam -> 0.0)

  /**
   * Set if we should fit the intercept
   * Default is true.
   * @group setParam
   */
  def setFitIntercept(value: Boolean): this.type = set(fitIntercept, value)
  setDefault(fitIntercept -> true)

  /**
   * Whether to standardize the training features before fitting the model.
   * The coefficients of models will be always returned on the original scale,
   * so it will be transparent for users. Note that with/without standardization,
   * the models should be always converged to the same solution when no regularization
   * is applied. In R's GLMNET package, the default behavior is true as well.
   * Default is true.
   * @group setParam
   */
  def setStandardization(value: Boolean): this.type = set(standardization, value)
  setDefault(standardization -> true)

  /**
   * Set the ElasticNet mixing parameter.
   * For alpha = 0, the penalty is an L2 penalty. For alpha = 1, it is an L1 penalty.
   * For 0 < alpha < 1, the penalty is a combination of L1 and L2.
   * Default is 0.0 which is an L2 penalty.
   * @group setParam
   */
  def setElasticNetParam(value: Double): this.type = set(elasticNetParam, value)
  setDefault(elasticNetParam -> 0.0)

  /**
   * Set the maximum number of iterations.
   * Default is 100.
   * @group setParam
   */
  def setMaxIter(value: Int): this.type = set(maxIter, value)
  setDefault(maxIter -> 100)

  /**
   * Set the convergence tolerance of iterations.
   * Smaller value will lead to higher accuracy with the cost of more iterations.
   * Default is 1E-6.
   * @group setParam
   */
  def setTol(value: Double): this.type = set(tol, value)
  setDefault(tol -> 1E-6)

  /**
   * Whether to over-/undersamples each of training instance according to the given
   * weight in `weightCol`. If empty, all samples are supposed to have weights as 1.0.
   * Default is empty, so all samples have weight one.
   * @group setParam
   */
  def setWeightCol(value: String): this.type = set(weightCol, value)
  setDefault(weightCol -> "")

  override protected def train(dataset: DataFrame): LinearRegressionModel = {
    // Extract columns from data.  If dataset is persisted, do not persist instances.
    val instances: RDD[(Double, Vector, Double)] = if ($(weightCol).isEmpty) {
      dataset.select($(labelCol), $(featuresCol)).map {
        case Row(label: Double, features: Vector) =>
          (label, features, 1.0)
      }
    } else {
      dataset.select($(labelCol), $(weightCol), $(featuresCol)).map {
        case Row(label: Double, weight: Double, features: Vector) =>
          (label, features, weight)
      }
    }

    val handlePersistence = dataset.rdd.getStorageLevel == StorageLevel.NONE
    if (handlePersistence) instances.persist(StorageLevel.MEMORY_AND_DISK)

    val (featuresSummarizer, ySummarizer) = instances.treeAggregate(
      (new MultivariateOnlineSummarizer, new MultivariateOnlineSummarizer))(
        seqOp = (c, v) => (c, v) match {
          case ((fs: MultivariateOnlineSummarizer, ys: MultivariateOnlineSummarizer),
          (label: Double, features: Vector, weight: Double)) =>
            (fs.add(features, weight), ys.add(Vectors.dense(label), weight))
        },
        combOp = (c1, c2) => (c1, c2) match {
          case ((summarizer1: MultivariateOnlineSummarizer, statCounter1: MultivariateOnlineSummarizer),
          (summarizer2: MultivariateOnlineSummarizer, statCounter2: MultivariateOnlineSummarizer)) =>
            (summarizer1.merge(summarizer2), statCounter1.merge(statCounter2))
        })

    val numFeatures = featuresSummarizer.mean.size
    val yMean = ySummarizer.mean(0)
    val yStd = math.sqrt(ySummarizer.variance(0))

    // If the yStd is zero, then the intercept is yMean with zero weights;
    // as a result, training is not needed.
    if (yStd == 0.0) {
      logWarning(s"The standard deviation of the label is zero, so the weights will be zeros " +
        s"and the intercept will be the mean of the label; as a result, training is not needed.")
      if (handlePersistence) instances.unpersist()
      val weights = Vectors.sparse(numFeatures, Seq())
      val intercept = yMean

      val model = new LinearRegressionModel(uid, weights, intercept)
      val trainingSummary = new LinearRegressionTrainingSummary(
        model.transform(dataset),
        $(predictionCol),
        $(labelCol),
        $(featuresCol),
        Array(0D))
      return copyValues(model.setSummary(trainingSummary))
    }

    val featuresMean = featuresSummarizer.mean.toArray
    val featuresStd = featuresSummarizer.variance.toArray.map(math.sqrt)

    // Since we implicitly do the feature scaling when we compute the cost function
    // to improve the convergence, the effective regParam will be changed.
    val effectiveRegParam = $(regParam) / yStd
    val effectiveL1RegParam = $(elasticNetParam) * effectiveRegParam
    val effectiveL2RegParam = (1.0 - $(elasticNetParam)) * effectiveRegParam

    val costFun = new LeastSquaresCostFun(instances, yStd, yMean, $(fitIntercept),
      $(standardization), featuresStd, featuresMean, effectiveL2RegParam)

    val optimizer = if ($(elasticNetParam) == 0.0 || effectiveRegParam == 0.0) {
      new BreezeLBFGS[BDV[Double]]($(maxIter), 10, $(tol))
    } else {
      def effectiveL1RegFun = (index: Int) => {
        if ($(standardization)) {
          effectiveL1RegParam
        } else {
          // If `standardization` is false, we still standardize the data
          // to improve the rate of convergence; as a result, we have to
          // perform this reverse standardization by penalizing each component
          // differently to get effectively the same objective function when
          // the training dataset is not standardized.
          if (featuresStd(index) != 0.0) effectiveL1RegParam / featuresStd(index) else 0.0
        }
      }
      new BreezeOWLQN[Int, BDV[Double]]($(maxIter), 10, effectiveL1RegFun, $(tol))
    }

    val initialWeights = Vectors.zeros(numFeatures)
    val states = optimizer.iterations(new CachedDiffFunction(costFun),
      initialWeights.toBreeze.toDenseVector)

    val (weights, objectiveHistory) = {
      /*
         Note that in Linear Regression, the objective history (loss + regularization) returned
         from optimizer is computed in the scaled space given by the following formula.
         {{{
         L = 1/2n||\sum_i w_i(x_i - \bar{x_i}) / \hat{x_i} - (y - \bar{y}) / \hat{y}||^2 + regTerms
         }}}
       */
      val arrayBuilder = mutable.ArrayBuilder.make[Double]
      var state: optimizer.State = null
      while (states.hasNext) {
        state = states.next()
        arrayBuilder += state.adjustedValue
      }
      if (state == null) {
        val msg = s"${optimizer.getClass.getName} failed."
        logError(msg)
        throw new SparkException(msg)
      }

      /*
         The weights are trained in the scaled space; we're converting them back to
         the original space.
       */
      val rawWeights = state.x.toArray.clone()
      var i = 0
      val len = rawWeights.length
      while (i < len) {
        rawWeights(i) *= { if (featuresStd(i) != 0.0) yStd / featuresStd(i) else 0.0 }
        i += 1
      }

      (Vectors.dense(rawWeights).compressed, arrayBuilder.result())
    }

    /*
       The intercept in R's GLMNET is computed using closed form after the coefficients are
       converged. See the following discussion for detail.
       http://stats.stackexchange.com/questions/13617/how-is-the-intercept-computed-in-glmnet
     */
    val intercept = if ($(fitIntercept)) yMean - dot(weights, Vectors.dense(featuresMean)) else 0.0

    if (handlePersistence) instances.unpersist()

    val model = copyValues(new LinearRegressionModel(uid, weights, intercept))
    val trainingSummary = new LinearRegressionTrainingSummary(
      model.transform(dataset),
      $(predictionCol),
      $(labelCol),
      $(featuresCol),
      objectiveHistory)
    model.setSummary(trainingSummary)
  }

  override def copy(extra: ParamMap): LinearRegression = defaultCopy(extra)
}

/**
 * :: Experimental ::
 * Model produced by [[LinearRegression]].
 */
@Experimental
class LinearRegressionModel private[ml] (
    override val uid: String,
    val weights: Vector,
    val intercept: Double)
  extends RegressionModel[Vector, LinearRegressionModel]
  with LinearRegressionParams {

  private var trainingSummary: Option[LinearRegressionTrainingSummary] = None

  /**
   * Gets summary (e.g. residuals, mse, r-squared ) of model on training set. An exception is
   * thrown if `trainingSummary == None`.
   */
  def summary: LinearRegressionTrainingSummary = trainingSummary match {
    case Some(summ) => summ
    case None =>
      throw new SparkException(
        "No training summary available for this LinearRegressionModel",
        new NullPointerException())
  }

  private[regression] def setSummary(summary: LinearRegressionTrainingSummary): this.type = {
    this.trainingSummary = Some(summary)
    this
  }

  /** Indicates whether a training summary exists for this model instance. */
  def hasSummary: Boolean = trainingSummary.isDefined

  /**
   * Evaluates the model on a testset.
   * @param dataset Test dataset to evaluate model on.
   */
  // TODO: decide on a good name before exposing to public API
  private[regression] def evaluate(dataset: DataFrame): LinearRegressionSummary = {
    val t = udf { features: Vector => predict(features) }
    val predictionAndObservations = dataset
      .select(col($(labelCol)), t(col($(featuresCol))).as($(predictionCol)))

    new LinearRegressionSummary(predictionAndObservations, $(predictionCol), $(labelCol))
  }

  override protected def predict(features: Vector): Double = {
    dot(features, weights) + intercept
  }

  override def copy(extra: ParamMap): LinearRegressionModel = {
    val newModel = copyValues(new LinearRegressionModel(uid, weights, intercept), extra)
    if (trainingSummary.isDefined) newModel.setSummary(trainingSummary.get)
    newModel.setParent(parent)
  }
}

/**
 * :: Experimental ::
 * Linear regression training results.
 * @param predictions predictions outputted by the model's `transform` method.
 * @param objectiveHistory objective function (scaled loss + regularization) at each iteration.
 */
@Experimental
class LinearRegressionTrainingSummary private[regression] (
    predictions: DataFrame,
    predictionCol: String,
    labelCol: String,
    val featuresCol: String,
    val objectiveHistory: Array[Double])
  extends LinearRegressionSummary(predictions, predictionCol, labelCol) {

  /** Number of training iterations until termination */
  val totalIterations = objectiveHistory.length

}

/**
 * :: Experimental ::
 * Linear regression results evaluated on a dataset.
 * @param predictions predictions outputted by the model's `transform` method.
 */
@Experimental
class LinearRegressionSummary private[regression] (
    @transient val predictions: DataFrame,
    val predictionCol: String,
    val labelCol: String) extends Serializable {

  @transient private val metrics = new RegressionMetrics(
    predictions
      .select(predictionCol, labelCol)
      .map { case Row(pred: Double, label: Double) => (pred, label) } )

  /**
   * Returns the explained variance regression score.
   * explainedVariance = 1 - variance(y - \hat{y}) / variance(y)
   * Reference: [[http://en.wikipedia.org/wiki/Explained_variation]]
   */
  val explainedVariance: Double = metrics.explainedVariance

  /**
   * Returns the mean absolute error, which is a risk function corresponding to the
   * expected value of the absolute error loss or l1-norm loss.
   */
  val meanAbsoluteError: Double = metrics.meanAbsoluteError

  /**
   * Returns the mean squared error, which is a risk function corresponding to the
   * expected value of the squared error loss or quadratic loss.
   */
  val meanSquaredError: Double = metrics.meanSquaredError

  /**
   * Returns the root mean squared error, which is defined as the square root of
   * the mean squared error.
   */
  val rootMeanSquaredError: Double = metrics.rootMeanSquaredError

  /**
   * Returns R^2^, the coefficient of determination.
   * Reference: [[http://en.wikipedia.org/wiki/Coefficient_of_determination]]
   */
  val r2: Double = metrics.r2

  /** Residuals (label - predicted value) */
  @transient lazy val residuals: DataFrame = {
    val t = udf { (pred: Double, label: Double) => label - pred }
    predictions.select(t(col(predictionCol), col(labelCol)).as("residuals"))
  }

}

/**
 * LeastSquaresAggregator computes the gradient and loss for a Least-squared loss function,
 * as used in linear regression for samples in sparse or dense vector in a online fashion.
 *
 * Two LeastSquaresAggregator can be merged together to have a summary of loss and gradient of
 * the corresponding joint dataset.
 *
 * For improving the convergence rate during the optimization process, and also preventing against
 * features with very large variances exerting an overly large influence during model training,
 * package like R's GLMNET performs the scaling to unit variance and removing the mean to reduce
 * the condition number, and then trains the model in scaled space but returns the weights in
 * the original scale. See page 9 in http://cran.r-project.org/web/packages/glmnet/glmnet.pdf
 *
 * However, we don't want to apply the `StandardScaler` on the training dataset, and then cache
 * the standardized dataset since it will create a lot of overhead. As a result, we perform the
 * scaling implicitly when we compute the objective function. The following is the mathematical
 * derivation.
 *
 * Note that we don't deal with intercept by adding bias here, because the intercept
 * can be computed using closed form after the coefficients are converged.
 * See this discussion for detail.
 * http://stats.stackexchange.com/questions/13617/how-is-the-intercept-computed-in-glmnet
 *
 * When training with intercept enabled,
 * The objective function in the scaled space is given by
 * {{{
 * L = 1/2n ||\sum_i w_i(x_i - \bar{x_i}) / \hat{x_i} - (y - \bar{y}) / \hat{y}||^2,
 * }}}
 * where \bar{x_i} is the mean of x_i, \hat{x_i} is the standard deviation of x_i,
 * \bar{y} is the mean of label, and \hat{y} is the standard deviation of label.
 *
 * If we fitting the intercept disabled (that is forced through 0.0),
 * we can use the same equation except we set \bar{y} and \bar{x_i} to 0 instead
 * of the respective means.
 *
 * This can be rewritten as
 * {{{
 * L = 1/2n ||\sum_i (w_i/\hat{x_i})x_i - \sum_i (w_i/\hat{x_i})\bar{x_i} - y / \hat{y}
 *     + \bar{y} / \hat{y}||^2
 *   = 1/2n ||\sum_i w_i^\prime x_i - y / \hat{y} + offset||^2 = 1/2n diff^2
 * }}}
 * where w_i^\prime^ is the effective weights defined by w_i/\hat{x_i}, offset is
 * {{{
 * - \sum_i (w_i/\hat{x_i})\bar{x_i} + \bar{y} / \hat{y}.
 * }}}, and diff is
 * {{{
 * \sum_i w_i^\prime x_i - y / \hat{y} + offset
 * }}}
 *
 *
 * Note that the effective weights and offset don't depend on training dataset,
 * so they can be precomputed.
 *
 * Now, the first derivative of the objective function in scaled space is
 * {{{
 * \frac{\partial L}{\partial\w_i} = diff/N (x_i - \bar{x_i}) / \hat{x_i}
 * }}}
 * However, ($x_i - \bar{x_i}$) will densify the computation, so it's not
 * an ideal formula when the training dataset is sparse format.
 *
 * This can be addressed by adding the dense \bar{x_i} / \har{x_i} terms
 * in the end by keeping the sum of diff. The first derivative of total
 * objective function from all the samples is
 * {{{
 * \frac{\partial L}{\partial\w_i} =
 *     1/N \sum_j diff_j (x_{ij} - \bar{x_i}) / \hat{x_i}
 *   = 1/N ((\sum_j diff_j x_{ij} / \hat{x_i}) - diffSum \bar{x_i}) / \hat{x_i})
 *   = 1/N ((\sum_j diff_j x_{ij} / \hat{x_i}) + correction_i)
 * }}},
 * where correction_i = - diffSum \bar{x_i}) / \hat{x_i}
 *
 * A simple math can show that diffSum is actually zero, so we don't even
 * need to add the correction terms in the end. From the definition of diff,
 * {{{
 * diffSum = \sum_j (\sum_i w_i(x_{ij} - \bar{x_i}) / \hat{x_i} - (y_j - \bar{y}) / \hat{y})
 *         = N * (\sum_i w_i(\bar{x_i} - \bar{x_i}) / \hat{x_i} - (\bar{y_j} - \bar{y}) / \hat{y})
 *         = 0
 * }}}
 *
 * As a result, the first derivative of the total objective function only depends on
 * the training dataset, which can be easily computed in distributed fashion, and is
 * sparse format friendly.
 * {{{
 * \frac{\partial L}{\partial\w_i} = 1/N ((\sum_j diff_j x_{ij} / \hat{x_i})
 * }}},
 *
 * @param coefficients The coefficients corresponding to the features.
 * @param labelStd The standard deviation value of the label.
 * @param labelMean The mean value of the label.
 * @param fitIntercept Whether to fit an intercept term.
 * @param featuresStd The standard deviation values of the features.
 * @param featuresMean The mean values of the features.
 */
private class LeastSquaresAggregator(
    coefficients: Vector,
    labelStd: Double,
    labelMean: Double,
    fitIntercept: Boolean,
    featuresStd: Array[Double],
    featuresMean: Array[Double]) extends Serializable {

  private var totalCnt: Double = 0
  private var lossSum = 0.0

  private val (effectiveCoefficientsArray: Array[Double], offset: Double, dim: Int) = {
    val coefficientsArray = coefficients.toArray.clone()
    var sum = 0.0
    var i = 0
    val len = coefficientsArray.length
    while (i < len) {
      if (featuresStd(i) != 0.0) {
        coefficientsArray(i) /=  featuresStd(i)
        sum += coefficientsArray(i) * featuresMean(i)
      } else {
        coefficientsArray(i) = 0.0
      }
      i += 1
    }
    (coefficientsArray, if (fitIntercept) labelMean / labelStd - sum else 0.0, coefficientsArray.length)
  }

  private val effectiveCoefficientsVector = Vectors.dense(effectiveCoefficientsArray)

  private val gradientSumArray = Array.ofDim[Double](dim)

  /**
   * Add a new training data to this LeastSquaresAggregator, and update the loss and gradient
   * of the objective function.
   *
   * @param label The label for this data point.
   * @param data The features for one data point in dense/sparse vector format to be added
   *             into this aggregator.
   * @param weight The weight for this data point.
   * @return This LeastSquaresAggregator object.
   */
  def add(label: Double, data: Vector, weight: Double): this.type = {
    require(dim == data.size, s"Dimensions mismatch when adding new sample." +
      s" Expecting $dim but got ${data.size}.")

    val diff = dot(data, effectiveCoefficientsVector) - label / labelStd + offset

    if (diff != 0) {
      val localGradientSumArray = gradientSumArray
      data.foreachActive { (index, value) =>
        if (featuresStd(index) != 0.0 && value != 0.0) {
          localGradientSumArray(index) += weight * diff * value / featuresStd(index)
        }
      }
      lossSum += weight * diff * diff / 2.0
    }

    totalCnt += weight
    this
  }

  /**
   * Merge another LeastSquaresAggregator, and update the loss and gradient
   * of the objective function.
   * (Note that it's in place merging; as a result, `this` object will be modified.)
   *
   * @param other The other LeastSquaresAggregator to be merged.
   * @return This LeastSquaresAggregator object.
   */
  def merge(other: LeastSquaresAggregator): this.type = {
    require(dim == other.dim, s"Dimensions mismatch when merging with another " +
      s"LeastSquaresAggregator. Expecting $dim but got ${other.dim}.")

    if (other.totalCnt != 0) {
      totalCnt += other.totalCnt
      lossSum += other.lossSum

      var i = 0
      val localThisGradientSumArray = this.gradientSumArray
      val localOtherGradientSumArray = other.gradientSumArray
      while (i < dim) {
        localThisGradientSumArray(i) += localOtherGradientSumArray(i)
        i += 1
      }
    }
    this
  }

  def count: Double = totalCnt

  def loss: Double = lossSum / totalCnt

  def gradient: Vector = {
    val result = Vectors.dense(gradientSumArray.clone())
    scal(1.0 / totalCnt, result)
    result
  }
}

/**
 * LeastSquaresCostFun implements Breeze's DiffFunction[T] for Least Squares cost.
 * It returns the loss and gradient with L2 regularization at a particular point (weights).
 * It's used in Breeze's convex optimization routines.
 */
private class LeastSquaresCostFun(
    data: RDD[(Double, Vector, Double)],
    labelStd: Double,
    labelMean: Double,
    fitIntercept: Boolean,
    standardization: Boolean,
    featuresStd: Array[Double],
    featuresMean: Array[Double],
    effectiveL2regParam: Double) extends DiffFunction[BDV[Double]] {

  override def calculate(coefficients: BDV[Double]): (Double, BDV[Double]) = {
    val coeff = Vectors.fromBreeze(coefficients)

    val leastSquaresAggregator = data.treeAggregate(new LeastSquaresAggregator(coeff, labelStd,
      labelMean, fitIntercept, featuresStd, featuresMean))(
        seqOp = (c, v) => (c, v) match {
          case (aggregator, (label, features, weight)) => aggregator.add(label, features, weight)
        },
        combOp = (c1, c2) => (c1, c2) match {
          case (aggregator1, aggregator2) => aggregator1.merge(aggregator2)
        })

    val totalGradientArray = leastSquaresAggregator.gradient.toArray

    val regVal = if (effectiveL2regParam == 0.0) {
      0.0
    } else {
      var sum = 0.0
      coeff.foreachActive { (index, value) =>
        // The following code will compute the loss of the regularization; also
        // the gradient of the regularization, and add back to totalGradientArray.
        sum += {
          if (standardization) {
            totalGradientArray(index) += effectiveL2regParam * value
            value * value
          } else {
            if (featuresStd(index) != 0.0) {
              // If `standardization` is false, we still standardize the data
              // to improve the rate of convergence; as a result, we have to
              // perform this reverse standardization by penalizing each component
              // differently to get effectively the same objective function when
              // the training dataset is not standardized.
              val temp = value / (featuresStd(index) * featuresStd(index))
              totalGradientArray(index) += effectiveL2regParam * temp
              value * temp
            } else {
              0.0
            }
          }
        }
      }
      0.5 * effectiveL2regParam * sum
    }

    (leastSquaresAggregator.loss + regVal, new BDV(totalGradientArray))
  }
}

case class WeightedLabeledPoint(label: Double, features: Vector, weight: Double)