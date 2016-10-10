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

package org.apache.spark.ml.clustering

import breeze.linalg.{DenseVector => BDV}
import org.apache.hadoop.fs.Path

import org.apache.spark.annotation.{Experimental, Since}
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.impl.Utils.EPSILON
import org.apache.spark.ml.linalg._
import org.apache.spark.ml.param._
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.stat.distribution.MultivariateGaussian
import org.apache.spark.ml.util._
import org.apache.spark.mllib.linalg.{Matrices => OldMatrices, Matrix => OldMatrix,
  Vector => OldVector, Vectors => OldVectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types.{IntegerType, StructType}


/**
 * Common params for GaussianMixture and GaussianMixtureModel
 */
private[clustering] trait GaussianMixtureParams extends Params with HasMaxIter with HasFeaturesCol
  with HasSeed with HasPredictionCol with HasProbabilityCol with HasTol {

  /**
   * Number of independent Gaussians in the mixture model. Must be greater than 1. Default: 2.
   * @group param
   */
  @Since("2.0.0")
  final val k = new IntParam(this, "k", "Number of independent Gaussians in the mixture model. " +
    "Must be > 1.", ParamValidators.gt(1))

  /** @group getParam */
  @Since("2.0.0")
  def getK: Int = $(k)

  /**
   * Validates and transforms the input schema.
   * @param schema input schema
   * @return output schema
   */
  protected def validateAndTransformSchema(schema: StructType): StructType = {
    SchemaUtils.checkColumnType(schema, $(featuresCol), new VectorUDT)
    SchemaUtils.appendColumn(schema, $(predictionCol), IntegerType)
    SchemaUtils.appendColumn(schema, $(probabilityCol), new VectorUDT)
  }
}

/**
 * Multivariate Gaussian Mixture Model (GMM) consisting of k Gaussians, where points
 * are drawn from each Gaussian i with probability weights(i).
 *
 * @param weights Weight for each Gaussian distribution in the mixture.
 *                This is a multinomial probability distribution over the k Gaussians,
 *                where weights(i) is the weight for Gaussian i, and weights sum to 1.
 * @param gaussians Array of `MultivariateGaussian` where gaussians(i) represents
 *                  the Multivariate Gaussian (Normal) Distribution for Gaussian i
 */
@Since("2.0.0")
class GaussianMixtureModel private[ml] (
    @Since("2.0.0") override val uid: String,
    @Since("2.0.0") val weights: Array[Double],
    @Since("2.0.0") val gaussians: Array[MultivariateGaussian])
  extends Model[GaussianMixtureModel] with GaussianMixtureParams with MLWritable {

  /** @group setParam */
  @Since("2.1.0")
  def setFeaturesCol(value: String): this.type = set(featuresCol, value)

  /** @group setParam */
  @Since("2.1.0")
  def setPredictionCol(value: String): this.type = set(predictionCol, value)

  /** @group setParam */
  @Since("2.1.0")
  def setProbabilityCol(value: String): this.type = set(probabilityCol, value)

  @Since("2.0.0")
  override def copy(extra: ParamMap): GaussianMixtureModel = {
    val copied = copyValues(new GaussianMixtureModel(uid, weights, gaussians), extra)
    copied.setSummary(trainingSummary).setParent(this.parent)
  }

  @Since("2.0.0")
  override def transform(dataset: Dataset[_]): DataFrame = {
    transformSchema(dataset.schema, logging = true)
    val predUDF = udf((vector: Vector) => predict(vector))
    val probUDF = udf((vector: Vector) => predictProbability(vector))
    dataset.withColumn($(predictionCol), predUDF(col($(featuresCol))))
      .withColumn($(probabilityCol), probUDF(col($(featuresCol))))
  }

  @Since("2.0.0")
  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }

  private[clustering] def predict(features: Vector): Int = {
    val r = predictProbability(features)
    r.argmax
  }

  private[clustering] def predictProbability(features: Vector): Vector = {
    val probs: Array[Double] =
      GaussianMixtureModel.computeProbabilities(features.asBreeze.toDenseVector, gaussians, weights)
    Vectors.dense(probs)
  }

  /**
   * Retrieve Gaussian distributions as a DataFrame.
   * Each row represents a Gaussian Distribution.
   * Two columns are defined: mean and cov.
   * Schema:
   * {{{
   *  root
   *   |-- mean: vector (nullable = true)
   *   |-- cov: matrix (nullable = true)
   * }}}
   */
  @Since("2.0.0")
  def gaussiansDF: DataFrame = {
    val modelGaussians = gaussians.map { gaussian =>
      (OldVectors.fromML(gaussian.mean), OldMatrices.fromML(gaussian.cov))
    }
    SparkSession.builder().getOrCreate().createDataFrame(modelGaussians).toDF("mean", "cov")
  }

  /**
   * Returns a [[org.apache.spark.ml.util.MLWriter]] instance for this ML instance.
   *
   * For [[GaussianMixtureModel]], this does NOT currently save the training [[summary]].
   * An option to save [[summary]] may be added in the future.
   *
   */
  @Since("2.0.0")
  override def write: MLWriter = new GaussianMixtureModel.GaussianMixtureModelWriter(this)

  private var trainingSummary: Option[GaussianMixtureSummary] = None

  private[clustering] def setSummary(summary: Option[GaussianMixtureSummary]): this.type = {
    this.trainingSummary = summary
    this
  }

  /**
   * Return true if there exists summary of model.
   */
  @Since("2.0.0")
  def hasSummary: Boolean = trainingSummary.nonEmpty

  /**
   * Gets summary of model on training set. An exception is
   * thrown if `trainingSummary == None`.
   */
  @Since("2.0.0")
  def summary: GaussianMixtureSummary = trainingSummary.getOrElse {
    throw new RuntimeException(
      s"No training summary available for the ${this.getClass.getSimpleName}")
  }
}

@Since("2.0.0")
object GaussianMixtureModel extends MLReadable[GaussianMixtureModel] {

  @Since("2.0.0")
  override def read: MLReader[GaussianMixtureModel] = new GaussianMixtureModelReader

  @Since("2.0.0")
  override def load(path: String): GaussianMixtureModel = super.load(path)

  /** [[MLWriter]] instance for [[GaussianMixtureModel]] */
  private[GaussianMixtureModel] class GaussianMixtureModelWriter(
      instance: GaussianMixtureModel) extends MLWriter {

    private case class Data(weights: Array[Double], mus: Array[OldVector], sigmas: Array[OldMatrix])

    override protected def saveImpl(path: String): Unit = {
      // Save metadata and Params
      DefaultParamsWriter.saveMetadata(instance, path, sc)
      // Save model data: weights and gaussians
      val weights = instance.weights
      val gaussians = instance.gaussians
      val mus = gaussians.map(g => OldVectors.fromML(g.mean))
      val sigmas = gaussians.map(c => OldMatrices.fromML(c.cov))
      val data = Data(weights, mus, sigmas)
      val dataPath = new Path(path, "data").toString
      sparkSession.createDataFrame(Seq(data)).repartition(1).write.parquet(dataPath)
    }
  }

  private class GaussianMixtureModelReader extends MLReader[GaussianMixtureModel] {

    /** Checked against metadata when loading model */
    private val className = classOf[GaussianMixtureModel].getName

    override def load(path: String): GaussianMixtureModel = {
      val metadata = DefaultParamsReader.loadMetadata(path, sc, className)

      val dataPath = new Path(path, "data").toString
      val row = sparkSession.read.parquet(dataPath).select("weights", "mus", "sigmas").head()
      val weights = row.getSeq[Double](0).toArray
      val mus = row.getSeq[OldVector](1).toArray
      val sigmas = row.getSeq[OldMatrix](2).toArray
      require(mus.length == sigmas.length, "Length of Mu and Sigma array must match")
      require(mus.length == weights.length, "Length of weight and Gaussian array must match")

      val gaussians = mus.zip(sigmas).map {
        case (mu, sigma) =>
          new MultivariateGaussian(mu.asML, sigma.asML)
      }
      val model = new GaussianMixtureModel(metadata.uid, weights, gaussians)

      DefaultParamsReader.getAndSetParams(model, metadata)
      model
    }
  }

  /**
   * Compute the probability (partial assignment) for each cluster for the given data point.
   * @param features  Data point
   * @param dists  Gaussians for model
   * @param weights  Weights for each Gaussian
   * @return  Probability (partial assignment) for each of the k clusters
   */
  private[clustering]
  def computeProbabilities(
      features: BDV[Double],
      dists: Array[MultivariateGaussian],
      weights: Array[Double]): Array[Double] = {
    val p = weights.zip(dists).map {
      case (weight, dist) => EPSILON + weight * dist.pdf(features)
    }
    val pSum = p.sum
    var i = 0
    while (i < weights.length) {
      p(i) /= pSum
      i += 1
    }
    p
  }
}

/**
 * Gaussian Mixture clustering.
 *
 * This class performs expectation maximization for multivariate Gaussian
 * Mixture Models (GMMs).  A GMM represents a composite distribution of
 * independent Gaussian distributions with associated "mixing" weights
 * specifying each's contribution to the composite.
 *
 * Given a set of sample points, this class will maximize the log-likelihood
 * for a mixture of k Gaussians, iterating until the log-likelihood changes by
 * less than convergenceTol, or until it has reached the max number of iterations.
 * While this process is generally guaranteed to converge, it is not guaranteed
 * to find a global optimum.
 *
 * @note For high-dimensional data (with many features), this algorithm may perform poorly.
 * This is due to high-dimensional data (a) making it difficult to cluster at all (based
 * on statistical/theoretical arguments) and (b) numerical issues with Gaussian distributions.
 */
@Since("2.0.0")
class GaussianMixture @Since("2.0.0") (
    @Since("2.0.0") override val uid: String)
  extends Estimator[GaussianMixtureModel] with GaussianMixtureParams with DefaultParamsWritable {

  setDefault(
    k -> 2,
    maxIter -> 100,
    tol -> 0.01)

  @Since("2.0.0")
  override def copy(extra: ParamMap): GaussianMixture = defaultCopy(extra)

  @Since("2.0.0")
  def this() = this(Identifiable.randomUID("GaussianMixture"))

  /** @group setParam */
  @Since("2.0.0")
  def setFeaturesCol(value: String): this.type = set(featuresCol, value)

  /** @group setParam */
  @Since("2.0.0")
  def setPredictionCol(value: String): this.type = set(predictionCol, value)

  /** @group setParam */
  @Since("2.0.0")
  def setProbabilityCol(value: String): this.type = set(probabilityCol, value)

  /** @group setParam */
  @Since("2.0.0")
  def setK(value: Int): this.type = set(k, value)

  /** @group setParam */
  @Since("2.0.0")
  def setMaxIter(value: Int): this.type = set(maxIter, value)

  /** @group setParam */
  @Since("2.0.0")
  def setTol(value: Double): this.type = set(tol, value)

  /** @group setParam */
  @Since("2.0.0")
  def setSeed(value: Long): this.type = set(seed, value)

  // number of samples per cluster to use when initializing Gaussians
  private val nSamples = 5

  @Since("2.0.0")
  override def fit(dataset: Dataset[_]): GaussianMixtureModel = {
    transformSchema(dataset.schema, logging = true)

    val sc = dataset.sparkSession.sparkContext
    val _k = $(k)
    // Extract the number of features.
    val numFeatures = dataset.select(col($(featuresCol))).first().getAs[Vector](0).size

    val instances: RDD[Vector] = dataset.select(col($(featuresCol))).rdd.map {
      case Row(features: Vector) => features
    }

    val instr = Instrumentation.create(this, instances)
    instr.logParams(featuresCol, predictionCol, probabilityCol, k, maxIter, seed, tol)

    val shouldDistributeGaussians = GaussianMixture.shouldDistributeGaussians(_k, numFeatures)

    // Determine initial weights and corresponding Gaussians.
    // We start with uniform weights, a random mean from the data, and
    // diagonal covariance matrices using component variances
    // derived from the samples.
    // TODO: Support users supplied initial GMM.
    val samples = instances.takeSample(withReplacement = true, _k * nSamples, $(seed))
    val weights = Array.fill(_k)(1.0 / _k)
    val gaussians = Array.tabulate(_k) { i =>
      val slice = samples.view(i * nSamples, (i + 1) * nSamples)
      val mean = {
        val v = Vectors.zeros(numFeatures)
        var i = 0
        while (i < nSamples) {
          BLAS.axpy(1.0, slice(i), v)
          i += 1
        }
        BLAS.scal(1.0 / nSamples, v)
        v
      }
      /**
       * Construct matrix where diagonal entries are element-wise
       * variance of input vectors (computes biased variance)
       */
      val cov = {
        val ss = Vectors.zeros(numFeatures).asBreeze
        slice.foreach(xi => ss += (xi.asBreeze - mean.asBreeze) :^ 2.0)
        val diagVec = Vectors.fromBreeze(ss)
        BLAS.scal(1.0 / nSamples, diagVec)
        Matrices.diag(diagVec)
      }
      new MultivariateGaussian(mean, cov)
    }

    var llh = Double.MinValue // current log-likelihood
    var llhp = 0.0            // previous log-likelihood

    var iter = 0
    while (iter < $(maxIter) && math.abs(llh - llhp) > $(tol)) {
      // create and broadcast curried cluster contribution function
      val compute = sc.broadcast(ExpectationSum.add(weights, gaussians)_)

      // aggregate the cluster contribution for all sample points
      val sums = instances.treeAggregate(ExpectationSum.zero(_k, numFeatures))(
        compute.value, _ += _)

      /**
       * Create new distributions based on the partial assignments
       * (often referred to as the "M" step in literature)
       */
      val sumWeights = sums.weights.sum

      if (shouldDistributeGaussians) {
        val numPartitions = math.min(_k, 1024)
        val tuples = Seq.tabulate(_k) { i =>
          (sums.means(i), sums.sigmas(i), sums.weights(i))
        }
        val (ws, gs) = sc.parallelize(tuples, numPartitions).map { case (mean, sigma, weight) =>
          GaussianMixture.updateWeightsAndGaussians(mean, sigma, weight, sumWeights)
        }.collect().unzip
        Array.copy(ws.toArray, 0, weights, 0, ws.length)
        Array.copy(gs.toArray, 0, gaussians, 0, gs.length)
      } else {
        var i = 0
        while (i < _k) {
          val (weight, gaussian) = GaussianMixture.updateWeightsAndGaussians(
            sums.means(i), sums.sigmas(i), sums.weights(i), sumWeights)
          weights(i) = weight
          gaussians(i) = gaussian
          i += 1
        }
      }

      llhp = llh  // current becomes previous
      llh = sums.logLikelihood  // this is the freshly computed log-likelihood
      iter += 1
      compute.destroy(blocking = false)
    }

    val model = copyValues(new GaussianMixtureModel(uid, weights, gaussians)).setParent(this)
    val summary = new GaussianMixtureSummary(model.transform(dataset),
      $(predictionCol), $(probabilityCol), $(featuresCol), $(k))
    model.setSummary(Some(summary))
    instr.logNumFeatures(model.gaussians.head.mean.size)
    instr.logSuccess(model)
    model
  }

  @Since("2.0.0")
  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }
}

@Since("2.0.0")
object GaussianMixture extends DefaultParamsReadable[GaussianMixture] {

  @Since("2.0.0")
  override def load(path: String): GaussianMixture = super.load(path)

  /**
   * Heuristic to distribute the computation of the [[MultivariateGaussian]]s, approximately when
   * d > 25 except for when k is very small.
   * @param k  Number of topics
   * @param d  Number of features
   */
  private[clustering] def shouldDistributeGaussians(k: Int, d: Int): Boolean = {
    ((k - 1.0) / k) * d > 25
  }

  private[clustering] def updateWeightsAndGaussians(
      mean: Vector,
      cov: Matrix,
      weight: Double,
      sumWeights: Double): (Double, MultivariateGaussian) = {
    BLAS.scal(1.0 / weight, mean)
    // TODO: Handle sparse matrix more efficiently
    BLAS.syr(-weight, mean, cov.asInstanceOf[DenseMatrix])
    val newWeight = weight / sumWeights
    cov.update(_ / weight)
    val newGaussian = new MultivariateGaussian(mean, cov)
    (newWeight, newGaussian)
  }
}

/**
 * Aggregation class for partial expectation results.
 */
private class ExpectationSum(
    var logLikelihood: Double,
    val weights: Array[Double],
    val means: Array[Vector],
    val sigmas: Array[Matrix]) extends Serializable {

  val k = weights.length

  def += (x: ExpectationSum): ExpectationSum = {
    var i = 0
    while (i < k) {
      weights(i) += x.weights(i)
      BLAS.axpy(1.0, x.means(i), means(i))
      sigmas(i).asBreeze += x.sigmas(i).asBreeze
      i += 1
    }
    logLikelihood += x.logLikelihood
    this
  }
}

/**
 * Companion class to provide zero constructor for ExpectationSum.
 */
private object ExpectationSum {

  def zero(k: Int, d: Int): ExpectationSum = {
    new ExpectationSum(0.0, Array.fill(k)(0.0), Array.fill(k)(Vectors.zeros(d)),
      Array.fill(k)(Matrices.zeros(d, d)))
  }

  /**
   * Compute cluster contributions for each input point
   * (U, T) => U for aggregation.
   */
  def add(
      weights: Array[Double],
      dists: Array[MultivariateGaussian])
      (sum: ExpectationSum, x: Vector): ExpectationSum = {
    val p = weights.zip(dists).map { case (weight, dist) =>
        EPSILON + weight * dist.pdf(x)
    }
    val pSum = p.sum
    sum.logLikelihood += math.log(pSum)
    var i = 0
    while(i < sum.k) {
      p(i) /= pSum
      sum.weights(i) += p(i)
      BLAS.axpy(p(i), x, sum.means(i))
      // TODO: Handle sparse matrix more efficiently
      BLAS.syr(p(i), x, sum.sigmas(i).asInstanceOf[DenseMatrix])
      i += 1
    }
    sum
  }
}

/**
 * :: Experimental ::
 * Summary of GaussianMixture.
 *
 * @param predictions  `DataFrame` produced by `GaussianMixtureModel.transform()`.
 * @param predictionCol  Name for column of predicted clusters in `predictions`.
 * @param probabilityCol  Name for column of predicted probability of each cluster
 *                        in `predictions`.
 * @param featuresCol  Name for column of features in `predictions`.
 * @param k  Number of clusters.
 */
@Since("2.0.0")
@Experimental
class GaussianMixtureSummary private[clustering] (
    predictions: DataFrame,
    predictionCol: String,
    @Since("2.0.0") val probabilityCol: String,
    featuresCol: String,
    k: Int) extends ClusteringSummary(predictions, predictionCol, featuresCol, k) {

  /**
   * Probability of each cluster.
   */
  @Since("2.0.0")
  @transient lazy val probability: DataFrame = predictions.select(probabilityCol)
}
