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

import org.apache.hadoop.fs.Path

import org.apache.spark.SparkException
import org.apache.spark.annotation.{Experimental, Since}
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.linalg.{Vector, VectorUDT}
import org.apache.spark.ml.param._
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.util._
import org.apache.spark.mllib.clustering.{KMeans => MLlibKMeans, KMeansModel => MLlibKMeansModel}
import org.apache.spark.mllib.linalg.{Vector => OldVector, Vectors => OldVectors}
import org.apache.spark.mllib.linalg.VectorImplicits._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types.{IntegerType, StructType}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.VersionUtils.majorVersion

/**
 * Common params for KMeans and KMeansModel
 */
private[clustering] trait KMeansParams extends Params with HasMaxIter with HasFeaturesCol
  with HasSeed with HasPredictionCol with HasTol {

  /**
   * The number of clusters to create (k). Must be > 1. Note that it is possible for fewer than
   * k clusters to be returned, for example, if there are fewer than k distinct points to cluster.
   * Default: 2.
   * @group param
   */
  @Since("1.5.0")
  final val k = new IntParam(this, "k", "The number of clusters to create. " +
    "Must be > 1.", ParamValidators.gt(1))

  /** @group getParam */
  @Since("1.5.0")
  def getK: Int = $(k)

  /**
   * Param for the initialization algorithm. This can be either "random" to choose random points as
   * initial cluster centers, or "k-means||" to use a parallel variant of k-means++
   * (Bahmani et al., Scalable K-Means++, VLDB 2012). Default: k-means||.
   * @group expertParam
   */
  @Since("1.5.0")
  final val initMode = new Param[String](this, "initMode", "The initialization algorithm. " +
    "Supported options: 'random' and 'k-means||'.",
    (value: String) => MLlibKMeans.validateInitMode(value))

  /** @group expertGetParam */
  @Since("1.5.0")
  def getInitMode: String = $(initMode)

  /**
   * Param for the number of steps for the k-means|| initialization mode. This is an advanced
   * setting -- the default of 2 is almost always enough. Must be > 0. Default: 2.
   * @group expertParam
   */
  @Since("1.5.0")
  final val initSteps = new IntParam(this, "initSteps", "The number of steps for k-means|| " +
    "initialization mode. Must be > 0.", ParamValidators.gt(0))

  /** @group expertGetParam */
  @Since("1.5.0")
  def getInitSteps: Int = $(initSteps)

  /**
   * Validates and transforms the input schema.
   * @param schema input schema
   * @return output schema
   */
  protected def validateAndTransformSchema(schema: StructType): StructType = {
    SchemaUtils.checkColumnType(schema, $(featuresCol), new VectorUDT)
    SchemaUtils.appendColumn(schema, $(predictionCol), IntegerType)
  }
}

/**
 * :: Experimental ::
 * Model fitted by KMeans.
 *
 * @param parentModel a model trained by spark.mllib.clustering.KMeans.
 */
@Since("1.5.0")
@Experimental
class KMeansModel private[ml] (
    @Since("1.5.0") override val uid: String,
    private val parentModel: MLlibKMeansModel)
  extends Model[KMeansModel] with KMeansParams with MLWritable {

  @Since("1.5.0")
  override def copy(extra: ParamMap): KMeansModel = {
    val copied = copyValues(new KMeansModel(uid, parentModel), extra)
    if (trainingSummary.isDefined) copied.setSummary(trainingSummary.get)
    copied.setParent(this.parent)
  }

  /** @group setParam */
  @Since("2.0.0")
  def setFeaturesCol(value: String): this.type = set(featuresCol, value)

  /** @group setParam */
  @Since("2.0.0")
  def setPredictionCol(value: String): this.type = set(predictionCol, value)

  @Since("2.0.0")
  override def transform(dataset: Dataset[_]): DataFrame = {
    transformSchema(dataset.schema, logging = true)
    val predictUDF = udf((vector: Vector) => predict(vector))
    dataset.withColumn($(predictionCol), predictUDF(col($(featuresCol))))
  }

  @Since("1.5.0")
  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }

  private[clustering] def predict(features: Vector): Int = parentModel.predict(features)

  @Since("2.0.0")
  def clusterCenters: Array[Vector] = parentModel.clusterCenters.map(_.asML)

  /**
   * Return the K-means cost (sum of squared distances of points to their nearest center) for this
   * model on the given data.
   */
  // TODO: Replace the temp fix when we have proper evaluators defined for clustering.
  @Since("2.0.0")
  def computeCost(dataset: Dataset[_]): Double = {
    SchemaUtils.checkColumnType(dataset.schema, $(featuresCol), new VectorUDT)
    val data: RDD[OldVector] = dataset.select(col($(featuresCol))).rdd.map {
      case Row(point: Vector) => OldVectors.fromML(point)
    }
    parentModel.computeCost(data)
  }

  /**
   * Returns a [[org.apache.spark.ml.util.MLWriter]] instance for this ML instance.
   *
   * For [[KMeansModel]], this does NOT currently save the training [[summary]].
   * An option to save [[summary]] may be added in the future.
   *
   */
  @Since("1.6.0")
  override def write: MLWriter = new KMeansModel.KMeansModelWriter(this)

  private var trainingSummary: Option[KMeansSummary] = None

  private[clustering] def setSummary(summary: KMeansSummary): this.type = {
    this.trainingSummary = Some(summary)
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
  def summary: KMeansSummary = trainingSummary.getOrElse {
    throw new SparkException(
      s"No training summary available for the ${this.getClass.getSimpleName}")
  }
}

@Since("1.6.0")
object KMeansModel extends MLReadable[KMeansModel] {

  @Since("1.6.0")
  override def read: MLReader[KMeansModel] = new KMeansModelReader

  @Since("1.6.0")
  override def load(path: String): KMeansModel = super.load(path)

  /** Helper class for storing model data */
  private case class Data(clusterIdx: Int, clusterCenter: Vector)

  /**
   * We store all cluster centers in a single row and use this class to store model data by
   * Spark 1.6 and earlier. A model can be loaded from such older data for backward compatibility.
   */
  private case class OldData(clusterCenters: Array[OldVector])

  /** [[MLWriter]] instance for [[KMeansModel]] */
  private[KMeansModel] class KMeansModelWriter(instance: KMeansModel) extends MLWriter {

    override protected def saveImpl(path: String): Unit = {
      // Save metadata and Params
      DefaultParamsWriter.saveMetadata(instance, path, sc)
      // Save model data: cluster centers
      val data: Array[Data] = instance.clusterCenters.zipWithIndex.map { case (center, idx) =>
        Data(idx, center)
      }
      val dataPath = new Path(path, "data").toString
      sparkSession.createDataFrame(data).repartition(1).write.parquet(dataPath)
    }
  }

  private class KMeansModelReader extends MLReader[KMeansModel] {

    /** Checked against metadata when loading model */
    private val className = classOf[KMeansModel].getName

    override def load(path: String): KMeansModel = {
      // Import implicits for Dataset Encoder
      val sparkSession = super.sparkSession
      import sparkSession.implicits._

      val metadata = DefaultParamsReader.loadMetadata(path, sc, className)
      val dataPath = new Path(path, "data").toString

      val clusterCenters = if (majorVersion(metadata.sparkVersion) >= 2) {
        val data: Dataset[Data] = sparkSession.read.parquet(dataPath).as[Data]
        data.collect().sortBy(_.clusterIdx).map(_.clusterCenter).map(OldVectors.fromML)
      } else {
        // Loads KMeansModel stored with the old format used by Spark 1.6 and earlier.
        sparkSession.read.parquet(dataPath).as[OldData].head().clusterCenters
      }
      val model = new KMeansModel(metadata.uid, new MLlibKMeansModel(clusterCenters))
      DefaultParamsReader.getAndSetParams(model, metadata)
      model
    }
  }
}

/**
 * :: Experimental ::
 * K-means clustering with support for k-means|| initialization proposed by Bahmani et al.
 *
 * @see [[http://dx.doi.org/10.14778/2180912.2180915 Bahmani et al., Scalable k-means++.]]
 */
@Since("1.5.0")
@Experimental
class KMeans @Since("1.5.0") (
    @Since("1.5.0") override val uid: String)
  extends Estimator[KMeansModel] with KMeansParams with DefaultParamsWritable {

  setDefault(
    k -> 2,
    maxIter -> 20,
    initMode -> MLlibKMeans.K_MEANS_PARALLEL,
    initSteps -> 2,
    tol -> 1e-4)

  @Since("1.5.0")
  override def copy(extra: ParamMap): KMeans = defaultCopy(extra)

  @Since("1.5.0")
  def this() = this(Identifiable.randomUID("kmeans"))

  /** @group setParam */
  @Since("1.5.0")
  def setFeaturesCol(value: String): this.type = set(featuresCol, value)

  /** @group setParam */
  @Since("1.5.0")
  def setPredictionCol(value: String): this.type = set(predictionCol, value)

  /** @group setParam */
  @Since("1.5.0")
  def setK(value: Int): this.type = set(k, value)

  /** @group expertSetParam */
  @Since("1.5.0")
  def setInitMode(value: String): this.type = set(initMode, value)

  /** @group expertSetParam */
  @Since("1.5.0")
  def setInitSteps(value: Int): this.type = set(initSteps, value)

  /** @group setParam */
  @Since("1.5.0")
  def setMaxIter(value: Int): this.type = set(maxIter, value)

  /** @group setParam */
  @Since("1.5.0")
  def setTol(value: Double): this.type = set(tol, value)

  /** @group setParam */
  @Since("1.5.0")
  def setSeed(value: Long): this.type = set(seed, value)

  @Since("2.0.0")
  override def fit(dataset: Dataset[_]): KMeansModel = {
    val handlePersistence = dataset.rdd.getStorageLevel == StorageLevel.NONE
    fit(dataset, handlePersistence)
  }
  @Since("2.0.0")
  protected def fit(dataset: Dataset[_], handlePersistence: Boolean): KMeansModel = {
    transformSchema(dataset.schema, logging = true)
    val instances: RDD[OldVector] = dataset.select(col($(featuresCol))).rdd.map {
      case Row(point: Vector) => OldVectors.fromML(point)
    }
    if (handlePersistence) {
      instances.persist(StorageLevel.MEMORY_AND_DISK)
    }
    val instr = Instrumentation.create(this, instances)
    instr.logParams(featuresCol, predictionCol, k, initMode, initSteps, maxIter, seed, tol)

    val algo = new MLlibKMeans()
      .setK($(k))
      .setInitializationMode($(initMode))
      .setInitializationSteps($(initSteps))
      .setMaxIterations($(maxIter))
      .setSeed($(seed))
      .setEpsilon($(tol))
    val parentModel = algo.run(instances, Option(instr))
    val model = copyValues(new KMeansModel(uid, parentModel).setParent(this))
    val summary = new KMeansSummary(
      model.transform(dataset), $(predictionCol), $(featuresCol), $(k))
    model.setSummary(summary)
    instr.logSuccess(model)
    if (handlePersistence) {
     instances.unpersist()
    }
    model
  }

  @Since("1.5.0")
  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }
}

@Since("1.6.0")
object KMeans extends DefaultParamsReadable[KMeans] {

  @Since("1.6.0")
  override def load(path: String): KMeans = super.load(path)
}

/**
 * :: Experimental ::
 * Summary of KMeans.
 *
 * @param predictions  [[DataFrame]] produced by [[KMeansModel.transform()]].
 * @param predictionCol  Name for column of predicted clusters in `predictions`.
 * @param featuresCol  Name for column of features in `predictions`.
 * @param k  Number of clusters.
 */
@Since("2.0.0")
@Experimental
class KMeansSummary private[clustering] (
    predictions: DataFrame,
    predictionCol: String,
    featuresCol: String,
    k: Int) extends ClusteringSummary(predictions, predictionCol, featuresCol, k)
