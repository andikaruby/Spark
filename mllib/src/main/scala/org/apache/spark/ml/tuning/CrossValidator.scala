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

package org.apache.spark.ml.tuning

import com.github.fommil.netlib.F2jBLAS
import org.apache.hadoop.fs.Path
import org.json4s.JsonAST.{JString, JNothing, JObject}
import org.json4s.{JValue, DefaultFormats}
import org.json4s.jackson.JsonMethods._

import org.apache.spark.ml.util.DefaultParamsReader.Metadata
import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.annotation.{Experimental, Since}
import org.apache.spark.ml._
import org.apache.spark.ml.evaluation.Evaluator
import org.apache.spark.ml.param._
import org.apache.spark.ml.util._
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType


/**
 * Params for [[CrossValidator]] and [[CrossValidatorModel]].
 */
private[ml] trait CrossValidatorParams extends ValidatorParams {
  /**
   * Param for number of folds for cross validation.  Must be >= 2.
   * Default: 3
   * @group param
   */
  val numFolds: IntParam = new IntParam(this, "numFolds",
    "number of folds for cross validation (>= 2)", ParamValidators.gtEq(2))

  /** @group getParam */
  def getNumFolds: Int = $(numFolds)

  setDefault(numFolds -> 3)
}

/**
 * :: Experimental ::
 * K-fold cross validation.
 */
@Experimental
class CrossValidator(override val uid: String) extends Estimator[CrossValidatorModel]
  with CrossValidatorParams with MLWritable with Logging {

  def this() = this(Identifiable.randomUID("cv"))

  private val f2jBLAS = new F2jBLAS

  /** @group setParam */
  def setEstimator(value: Estimator[_]): this.type = set(estimator, value)

  /** @group setParam */
  def setEstimatorParamMaps(value: Array[ParamMap]): this.type = set(estimatorParamMaps, value)

  /** @group setParam */
  def setEvaluator(value: Evaluator): this.type = set(evaluator, value)

  /** @group setParam */
  def setNumFolds(value: Int): this.type = set(numFolds, value)

  override def fit(dataset: DataFrame): CrossValidatorModel = {
    val schema = dataset.schema
    transformSchema(schema, logging = true)
    val sqlCtx = dataset.sqlContext
    val est = $(estimator)
    val eval = $(evaluator)
    val epm = $(estimatorParamMaps)
    val numModels = epm.length
    val metrics = new Array[Double](epm.length)
    val splits = MLUtils.kFold(dataset.rdd, $(numFolds), 0)
    splits.zipWithIndex.foreach { case ((training, validation), splitIndex) =>
      val trainingDataset = sqlCtx.createDataFrame(training, schema).cache()
      val validationDataset = sqlCtx.createDataFrame(validation, schema).cache()
      // multi-model training
      logDebug(s"Train split $splitIndex with multiple sets of parameters.")
      val models = est.fit(trainingDataset, epm).asInstanceOf[Seq[Model[_]]]
      trainingDataset.unpersist()
      var i = 0
      while (i < numModels) {
        // TODO: duplicate evaluator to take extra params from input
        val metric = eval.evaluate(models(i).transform(validationDataset, epm(i)))
        logDebug(s"Got metric $metric for model trained with ${epm(i)}.")
        metrics(i) += metric
        i += 1
      }
      validationDataset.unpersist()
    }
    f2jBLAS.dscal(numModels, 1.0 / $(numFolds), metrics, 1)
    logInfo(s"Average cross-validation metrics: ${metrics.toSeq}")
    val (bestMetric, bestIndex) =
      if (eval.isLargerBetter) metrics.zipWithIndex.maxBy(_._1)
      else metrics.zipWithIndex.minBy(_._1)
    logInfo(s"Best set of parameters:\n${epm(bestIndex)}")
    logInfo(s"Best cross-validation metric: $bestMetric.")
    val bestModel = est.fit(dataset, epm(bestIndex)).asInstanceOf[Model[_]]
    copyValues(new CrossValidatorModel(uid, bestModel, metrics).setParent(this))
  }

  override def transformSchema(schema: StructType): StructType = {
    $(estimator).transformSchema(schema)
  }

  override def validateParams(): Unit = {
    super.validateParams()
    val est = $(estimator)
    for (paramMap <- $(estimatorParamMaps)) {
      est.copy(paramMap).validateParams()
    }
  }

  override def copy(extra: ParamMap): CrossValidator = {
    val copied = defaultCopy(extra).asInstanceOf[CrossValidator]
    if (copied.isDefined(estimator)) {
      copied.setEstimator(copied.getEstimator.copy(extra))
    }
    if (copied.isDefined(evaluator)) {
      copied.setEvaluator(copied.getEvaluator.copy(extra))
    }
    copied
  }

  // Currently, this only works if all [[Param]]s in [[estimatorParamMaps]] are simple types.
  // E.g., this may fail if a [[Param]] is an instance of an [[Estimator]].
  // However, this case should be unusual.
  @Since("1.6.0")
  override def write: MLWriter = new CrossValidator.CrossValidatorWriter(this)
}

@Since("1.6.0")
object CrossValidator extends MLReadable[CrossValidator] {

  @Since("1.6.0")
  override def read: MLReader[CrossValidator] = new CrossValidatorReader

  @Since("1.6.0")
  override def load(path: String): CrossValidator = super.load(path)

  private[CrossValidator] class CrossValidatorWriter(instance: CrossValidator) extends MLWriter {

    SharedReadWrite.validateParams(instance)

    override protected def saveImpl(path: String): Unit =
      SharedReadWrite.saveImpl(path, instance, sc, JNothing)
  }

  private class CrossValidatorReader extends MLReader[CrossValidator] {

    /** Checked against metadata when loading model */
    private val className = classOf[CrossValidator].getName

    override def load(path: String): CrossValidator = {
      val (metadata, estimator, evaluator, estimatorParamMaps, numFolds) =
        SharedReadWrite.load(path, sc, className)
      new CrossValidator(metadata.uid)
        .setEstimator(estimator)
        .setEvaluator(evaluator)
        .setEstimatorParamMaps(estimatorParamMaps)
        .setNumFolds(numFolds)
    }
  }

  private object CrossValidatorReader {
    /**
     * Examine the given estimator (which may be a compound estimator) and extract a mapping
     * from UIDs to corresponding [[Params]] instances.
     */
    def getUidMap(instance: Params): Map[String, Params] = {
      val subStages: Array[Params] = instance match {
        case p: Pipeline => p.getStages.asInstanceOf[Array[Params]]
        case pm: PipelineModel => pm.stages.asInstanceOf[Array[Params]]
        case v: ValidatorParams => Array(v.getEstimator, v.getEvaluator)
        case _: Params => Array()
      }
      val subStageMaps = subStages.map(getUidMap).foldLeft(Map.empty[String, Params])(_ ++ _)
      Map(instance.uid -> instance) ++ subStageMaps
    }
  }

  private[tuning] object SharedReadWrite {

    /**
     * Check that [[CrossValidator.evaluator]] and [[CrossValidator.estimator]] are Writable.
     * This does not check [[CrossValidator.estimatorParamMaps]].
     */
    def validateParams(instance: ValidatorParams): Unit = {
      def checkElement(elem: Params, name: String): Unit = elem match {
        case stage: MLWritable => // good
        case other =>
          throw new UnsupportedOperationException("CrossValidator write will fail " +
            s" because it contains $name which does not implement Writable." +
            s" Non-Writable $name: ${other.uid} of type ${other.getClass}")
      }
      checkElement(instance.getEvaluator, "evaluator")
      checkElement(instance.getEstimator, "estimator")
      // Check to make sure all Params apply to this estimator.  Throw an error if any do not.
      // Extraneous Params would cause problems when loading the estimatorParamMaps.
      val uidToInstance: Map[String, Params] = CrossValidatorReader.getUidMap(instance)
      instance.getEstimatorParamMaps.foreach { case pMap: ParamMap =>
        pMap.toSeq.foreach { case ParamPair(p, v) =>
          require(uidToInstance.contains(p.parent), s"CrossValidator save requires all Params in" +
            s" estimatorParamMaps to apply to this CrossValidator, its Estimator, or its" +
            s" Evaluator.  An extraneous Param was found: $p")
        }
      }
    }

    private[tuning] def saveImpl(
        path: String,
        instance: CrossValidatorParams,
        sc: SparkContext,
        extraMetadata: JValue): Unit = {
      import org.json4s.JsonDSL._

      val uid = instance.uid
      val cls = instance.getClass.getName
      val estimatorParamMapsJson = compact(render(
        instance.getEstimatorParamMaps.map { case paramMap =>
          paramMap.toSeq.map { case ParamPair(p, v) =>
            Map("parent" -> p.parent, "name" -> p.name, "value" -> p.jsonEncode(v))
          }
        }.toSeq
      ))
      val jsonParams = List(
        "numFolds" -> parse(instance.numFolds.jsonEncode(instance.getNumFolds)),
        "estimatorParamMaps" -> parse(estimatorParamMapsJson)
      )
      val metadata = ("class" -> cls) ~
        ("timestamp" -> System.currentTimeMillis()) ~
        ("sparkVersion" -> sc.version) ~
        ("uid" -> uid) ~
        ("paramMap" -> jsonParams) ~
        ("extraMetadata" -> extraMetadata)
      val metadataPath = new Path(path, "metadata").toString
      val metadataJson = compact(render(metadata))
      sc.parallelize(Seq(metadataJson), 1).saveAsTextFile(metadataPath)

      val evaluatorPath = new Path(path, "evaluator").toString
      instance.getEvaluator.asInstanceOf[MLWritable].save(evaluatorPath)
      val estimatorPath = new Path(path, "estimator").toString
      instance.getEstimator.asInstanceOf[MLWritable].save(estimatorPath)
    }

    private[tuning] def load[M <: Model[M]](
        path: String,
        sc: SparkContext,
        expectedClassName: String): (Metadata, Estimator[M], Evaluator, Array[ParamMap], Int) = {
      val metadata = DefaultParamsReader.loadMetadata(path, sc, expectedClassName)

      implicit val format = DefaultFormats
      val evaluatorPath = new Path(path, "evaluator").toString
      val evaluator = DefaultParamsReader.loadParamsInstance[Evaluator](evaluatorPath, sc)
      val estimatorPath = new Path(path, "estimator").toString
      val estimator = DefaultParamsReader.loadParamsInstance[Estimator[M]](estimatorPath, sc)

      val uidToParams = Map(evaluator.uid -> evaluator) ++ CrossValidatorReader.getUidMap(estimator)

      val (numFolds: Int, estimatorParamMaps: Array[ParamMap]) = metadata.params match {
        case JObject(pairs) =>
          if (pairs.length != 2) {
            // Should not happen unless file is corrupted or we have a bug.
            throw new RuntimeException(s"CrossValidator read expected 2 Params (numFolds," +
              s" estimatorParamMaps), but found ${pairs.length}.")
          }
          val numFolds = pairs.head match {
            case ("numFolds", jsonValue) =>
              jsonValue.extract[Int]
            case (paramName, _) =>
              // Should not happen unless file is corrupted or we have a bug.
              throw new RuntimeException(s"CrossValidator read expected numFolds but encountered" +
                s" unexpected Param $paramName in metadata: ${metadata.metadataStr}")
          }
          val estimatorParamMaps: Array[ParamMap] = pairs(1) match {
            case ("estimatorParamMaps", epmJsonValue: JValue) =>
              epmJsonValue.extract[Seq[Seq[Map[String, String]]]].map { pMap =>
                val paramPairs = pMap.map { case pInfo: Map[String, String] =>
                  val est = uidToParams(pInfo("parent"))
                  val param = est.getParam(pInfo("name"))
                  val value = param.jsonDecode(pInfo("value"))
                  param -> value
                }
                ParamMap(paramPairs: _*)
              }.toArray
            case (paramName, _) =>
              // Should not happen unless file is corrupted or we have a bug.
              throw new RuntimeException(s"CrossValidator read expected estimatorParamMaps but" +
                s" encountered unexpected Param $paramName in metadata: ${metadata.metadataStr}")
          }
          (numFolds, estimatorParamMaps)
        case _ =>
          throw new IllegalArgumentException(
            s"Cannot recognize JSON metadata: ${metadata.metadataStr}.")
      }
      (metadata, estimator, evaluator, estimatorParamMaps, numFolds)
    }
  }
}

/**
 * :: Experimental ::
 * Model from k-fold cross validation.
 *
 * @param bestModel The best model selected from k-fold cross validation.
 * @param avgMetrics Average cross-validation metrics for each paramMap in
 *                   [[CrossValidator.estimatorParamMaps]], in the corresponding order.
 */
@Experimental
class CrossValidatorModel private[ml] (
    override val uid: String,
    val bestModel: Model[_],
    val avgMetrics: Array[Double])
  extends Model[CrossValidatorModel] with CrossValidatorParams with MLWritable {

  override def validateParams(): Unit = {
    bestModel.validateParams()
  }

  override def transform(dataset: DataFrame): DataFrame = {
    transformSchema(dataset.schema, logging = true)
    bestModel.transform(dataset)
  }

  override def transformSchema(schema: StructType): StructType = {
    bestModel.transformSchema(schema)
  }

  override def copy(extra: ParamMap): CrossValidatorModel = {
    val copied = new CrossValidatorModel(
      uid,
      bestModel.copy(extra).asInstanceOf[Model[_]],
      avgMetrics.clone())
    copyValues(copied, extra).setParent(parent)
  }

  @Since("1.6.0")
  override def write: MLWriter = new CrossValidatorModel.CrossValidatorModelWriter(this)
}

@Since("1.6.0")
object CrossValidatorModel extends MLReadable[CrossValidatorModel] {

  import CrossValidator.SharedReadWrite

  @Since("1.6.0")
  override def read: MLReader[CrossValidatorModel] = new CrossValidatorModelReader

  @Since("1.6.0")
  override def load(path: String): CrossValidatorModel = super.load(path)

  private[CrossValidatorModel]
  class CrossValidatorModelWriter(instance: CrossValidatorModel) extends MLWriter {

    SharedReadWrite.validateParams(instance)

    override protected def saveImpl(path: String): Unit = {
      import org.json4s.JsonDSL._
      val extraMetadata = compact(render(instance.avgMetrics.toSeq))
      SharedReadWrite.saveImpl(path, instance, sc, extraMetadata)
      val bestModelPath = new Path(path, "bestModel").toString
      instance.bestModel.asInstanceOf[MLWritable].save(bestModelPath)
    }
  }

  private class CrossValidatorModelReader extends MLReader[CrossValidatorModel] {

    /** Checked against metadata when loading model */
    private val className = classOf[CrossValidatorModel].getName

    override def load(path: String): CrossValidatorModel = {
      implicit val format = DefaultFormats

      val (metadata, estimator, evaluator, estimatorParamMaps, numFolds) =
        SharedReadWrite.load(path, sc, className)
      val bestModelPath = new Path(path, "bestModel").toString
      val bestModel = DefaultParamsReader.loadParamsInstance[Model[_]](bestModelPath, sc)
      val avgMetrics = metadata.extraMetadata match {
        case Some(JString(extraMetaData: String)) =>
          parse(extraMetaData).extract[Seq[Double]].toArray
        case _ =>
          throw new RuntimeException(s"CrossValidatorModel load could not find avgMetrics in" +
            s" JSON metadata: ${metadata.metadataStr}")
      }
      val cv = new CrossValidatorModel(metadata.uid, bestModel, avgMetrics)
      cv.set(cv.estimator, estimator)
        .set(cv.evaluator, evaluator)
        .set(cv.estimatorParamMaps, estimatorParamMaps)
        .set(cv.numFolds, numFolds)
    }
  }
}
