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

import breeze.linalg.{DenseVector => BDV}
import breeze.optimize.{CachedDiffFunction, DiffFunction, LBFGS => BreezeLBFGS}

import org.apache.spark.{SparkException, Logging}
import org.apache.spark.annotation.Experimental
import org.apache.spark.ml.{Model, Estimator}
import org.apache.spark.ml.param._
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.util.{SchemaUtils, Identifiable}
import org.apache.spark.mllib.linalg.{DenseVector, Vector, Vectors, VectorUDT}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DoubleType, StructType}
import org.apache.spark.storage.StorageLevel

/**
 * Params for Accelerated Failure Time regression.
 */
private[regression] trait AFTRegressionParams extends Params
  with HasFeaturesCol with HasLabelCol with HasPredictionCol with HasMaxIter
  with HasTol with HasFitIntercept {

  /**
   * Param for censored column name.
   * @group param
   */
  final val censorCol: Param[String] = new Param[String](this, "censorCol", "censored column name")

  /** @group getParam */
  final def getCensorCol: String = $(censorCol)

  /**
   * Param for quantile column name.
   * @group param
   */
  final val quantileCol: Param[String] = new Param[String](this,
    "quantileCol", "quantile column name")

  /** @group getParam */
  final def getQuantileCol: String = $(quantileCol)

  /**
   * Validates and transforms the input schema with the provided param map.
   * @param schema input schema
   * @param fitting whether this is in fitting or prediction
   * @return output schema
   */
  protected def validateAndTransformSchema(
      schema: StructType,
      fitting: Boolean): StructType = {
    SchemaUtils.checkColumnType(schema, $(featuresCol), new VectorUDT)
    if (fitting) {
      SchemaUtils.checkColumnType(schema, $(censorCol), DoubleType)
      SchemaUtils.checkColumnType(schema, $(labelCol), DoubleType)
    } else {
      SchemaUtils.checkColumnType(schema, $(quantileCol), new VectorUDT)
    }
    SchemaUtils.appendColumn(schema, $(predictionCol), DoubleType)
  }
}

@Experimental
class AFTRegression(override val uid: String)
  extends Estimator[AFTRegressionModel] with AFTRegressionParams with Logging {

  def this() = this(Identifiable.randomUID("aftReg"))

  /** @group setParam */
  def setFeaturesCol(value: String): this.type = set(featuresCol, value)

  /** @group setParam */
  def setLabelCol(value: String): this.type = set(labelCol, value)

  /** @group setParam */
  def setCensorCol(value: String): this.type = set(censorCol, value)
  setDefault(censorCol -> "censored")

  /** @group setParam */
  def setPredictionCol(value: String): this.type = set(predictionCol, value)

  /**
   * Set if we should fit the intercept
   * Default is true.
   * @group setParam
   */
  def setFitIntercept(value: Boolean): this.type = set(fitIntercept, value)
  setDefault(fitIntercept -> true)

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
   * Extracts (feature, label, censored) from input dataset.
   */
  protected[ml] def extractCensoredLabeledPoints(
      dataset: DataFrame): RDD[(Vector, Double, Double)] = {
    dataset.select($(featuresCol), $(labelCol), $(censorCol))
      .map { case Row(feature: Vector, label: Double, censored: Double) =>
      (feature, label, censored)
    }
  }

  override def fit(dataset: DataFrame): AFTRegressionModel = {
    validateAndTransformSchema(dataset.schema, fitting = true)
    val instances = extractCensoredLabeledPoints(dataset)
    val handlePersistence = dataset.rdd.getStorageLevel == StorageLevel.NONE
    if (handlePersistence) instances.persist(StorageLevel.MEMORY_AND_DISK)

    val costFun = new AFTCostFun(instances, $(fitIntercept))
    val optimizer = new BreezeLBFGS[BDV[Double]]($(maxIter), 10, $(tol))

    val numFeatures = dataset.select($(featuresCol)).take(1)(0).getAs[Vector](0).size
    val initialWeights = new DenseVector(Array.fill(numFeatures + 2)(1.0))

    val states = optimizer.iterations(new CachedDiffFunction(costFun),
      initialWeights.toBreeze.toDenseVector)

    val weights = {
      val arrayBuilder = mutable.ArrayBuilder.make[Double]
      var state: optimizer.State = null
      while (states.hasNext) {
        state = states.next()
        // println(state.fVals.mkString(","))
        arrayBuilder += state.adjustedValue
      }
      if (state == null) {
        val msg = s"${optimizer.getClass.getName} failed."
        throw new SparkException(msg)
      }

      val rawWeights = state.x.toArray.clone()
      rawWeights
    }

    if (handlePersistence) instances.unpersist()

    val realWeights = Vectors.dense(weights.slice(2, weights.length))
    val intercept = weights(1)
    val scale = weights(0)
    val model = new AFTRegressionModel(uid, realWeights, intercept, scale)
    copyValues(model.setParent(this))
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema, fitting = true)
  }

  override def copy(extra: ParamMap): AFTRegression = defaultCopy(extra)
}

/**
 * :: Experimental ::
 * Model produced by [[AFTRegression]].
 */
@Experimental
class AFTRegressionModel private[ml] (
    override val uid: String,
    val weights: Vector,
    val intercept: Double,
    val scale: Double)
  extends Model[AFTRegressionModel] with AFTRegressionParams {

  /** @group setParam */
  def setFeaturesCol(value: String): this.type = set(featuresCol, value)

  /** @group setParam */
  def setQuantileCol(value: String): this.type = set(quantileCol, value)
  setDefault(quantileCol -> "quantile")

  /** @group setParam */
  def setPredictionCol(value: String): this.type = set(predictionCol, value)

  def predict(features: Vector, quantile: Vector): Vector = {
    // scale parameter of the Weibull distribution
    val lambda = math.exp(weights.toBreeze.dot(features.toBreeze) + intercept)
    // shape parameter of the Weibull distribution
    val k = 1 / scale
    val array = quantile.toArray.map { q => lambda * math.exp(math.log(-math.log(1-q)) / k) }
    Vectors.dense(array)
  }

  override def transform(dataset: DataFrame): DataFrame = {
    transformSchema(dataset.schema)
    val predictUDF = udf { (features: Vector, quantile: Vector) => predict(features, quantile) }
    dataset.withColumn($(predictionCol), predictUDF(col($(featuresCol)), col($(quantileCol))))
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema, fitting = false)
  }

  override def copy(extra: ParamMap): AFTRegressionModel = {
    copyValues(new AFTRegressionModel(uid, weights, intercept, scale), extra).setParent(parent)
  }
}

private class AFTAggregator(weights: BDV[Double], fitIntercept: Boolean)
  extends Serializable {

  private val beta = weights.slice(1, weights.length)
  private val sigma = weights(0)

  private var totalCnt: Long = 0L
  private var lossSum = 0.0
  private var gradientBetaSum = BDV.zeros[Double](beta.length)
  private var gradientSigmaSum = 0.0

  def count: Long = totalCnt
  def loss: Double = if (totalCnt == 0) 1.0 else lossSum / totalCnt
  def gradient: BDV[Double] = BDV.vertcat(BDV(Array(gradientSigmaSum / totalCnt.toDouble)),
    gradientBetaSum/totalCnt.toDouble)

  def add(data: (Vector, Double, Double)): this.type = {

    val xi = if (fitIntercept) {
      Vectors.dense(Array(1.0) ++ data._1.toArray).toBreeze
    } else {
      Vectors.dense(Array(0.0) ++ data._1.toArray).toBreeze
    }
    val ti = data._2
    val delta = data._3
    val epsilon = (math.log(ti) - beta.dot(xi)) / sigma

    lossSum += math.log(sigma) * delta
    lossSum += (math.exp(epsilon) - delta * epsilon)

    // Sanity check (should never occur):
    assert(!lossSum.isInfinity,
      s"AFTAggregator loss sum is infinity. Error for unknown reason.")

    gradientBetaSum += xi * (delta - math.exp(epsilon)) / sigma
    gradientSigmaSum += (delta + (delta - math.exp(epsilon)) * epsilon) / sigma

    totalCnt += 1
    this
  }

  def merge(other: AFTAggregator): this.type = {
    if (totalCnt != 0) {
      totalCnt += other.totalCnt
      lossSum += other.lossSum

      gradientBetaSum += other.gradientBetaSum
      gradientSigmaSum += other.gradientSigmaSum
    }
    this
  }
}

private class AFTCostFun(data: RDD[(Vector, Double, Double)], fitIntercept: Boolean)
  extends DiffFunction[BDV[Double]] {

  override def calculate(weights: BDV[Double]): (Double, BDV[Double]) = {

    val aftAggregator = data.treeAggregate(new AFTAggregator(weights, fitIntercept))(
      seqOp = (c, v) => (c, v) match {
        case (aggregator, instance) => aggregator.add(instance)
      },
      combOp = (c1, c2) => (c1, c2) match {
        case (aggregator1, aggregator2) => aggregator1.merge(aggregator2)
      })

    (aftAggregator.loss, aftAggregator.gradient)
  }
}
