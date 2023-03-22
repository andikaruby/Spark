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

package org.apache.spark.ml.connect

import org.apache.spark.connect.proto
import org.apache.spark.ml
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.classification.TrainingSummary
import org.apache.spark.ml.util.{HasTrainingSummary, MLWriter}
import org.apache.spark.sql.DataFrame

object AlgorithmRegistry {

  def get(name: String): Algorithm = {
    name match {
      case "LogisticRegression" => new LogisticRegressionAlgorithm
      case _ =>
        throw new IllegalArgumentException()
    }
  }

}

abstract class Algorithm {

  def initiateEstimator(uid: String): Estimator[_]

  def getModelAttr(model: Model[_], name: String):
      Option[Either[proto.MlCommandResponse, DataFrame]] = {

    name match {
      case "hasSummary" =>
        if (model.isInstanceOf[HasTrainingSummary]) {
          Some(Left(
            Serializer.serializeResponseValue(model.asInstanceOf[HasTrainingSummary].hasSummary)
          ))
        } else None
      case "toString" =>
        Some(Left(Serializer.serializeResponseValue(model.toString)))
    }
  }

  def getModelSummaryAttr(
      model: Model[_],
      name: String,
      datasetOpt: Option[DataFrame]): Either[proto.MlCommandResponse, DataFrame]

  def loadModel(path: String): Model[_]

  def loadEstimator(path: String): Estimator[_]

  protected def getEstimatorWriter(estimator: Estimator[_]): MLWriter

  protected def getModelWriter(model: Model[_]): MLWriter

  def _save(
      writer: MLWriter,
      path: String,
      overwrite: Boolean,
      options: Map[String, String]): Unit = {
    if (overwrite) {
      writer.overwrite()
    }
    options.map { case (k, v) => writer.option(k, v) }
    writer.save(path)
  }

  def saveModel(
      model: Model[_],
      path: String,
      overwrite: Boolean,
      options: Map[String, String]): Unit = {
    _save(getModelWriter(model), path, overwrite, options)
  }

  def saveEstimator(
      estimator: Estimator[_],
      path: String,
      overwrite: Boolean,
      options: Map[String, String]): Unit = {
    _save(getEstimatorWriter(estimator), path, overwrite, options)
  }
}

class LogisticRegressionAlgorithm extends Algorithm {

  override def initiateEstimator(uid: String): Estimator[_] = {
    new ml.classification.LogisticRegression(uid)
  }

  override def loadModel(path: String): Model[_] = {
    ml.classification.LogisticRegressionModel.load(path)
  }

  override def loadEstimator(path: String): Estimator[_] = {
    ml.classification.LogisticRegression.load(path)
  }

  protected override def getModelWriter(model: Model[_]): MLWriter = {
    model.asInstanceOf[ml.classification.LogisticRegressionModel].write
  }

  protected override def getEstimatorWriter(estimator: Estimator[_]): MLWriter = {
    estimator.asInstanceOf[ml.classification.LogisticRegression].write
  }

  override def getModelAttr(
      model: Model[_],
      name: String): Option[Either[proto.MlCommandResponse, DataFrame]] = {

    super.getModelAttr(model, name).orElse {
      val lorModel = model.asInstanceOf[ml.classification.LogisticRegressionModel]

      name match {
        case "numClasses" => Some(Left(Serializer.serializeResponseValue(lorModel.numClasses)))
        case "numFeatures" => Some(Left(Serializer.serializeResponseValue(lorModel.numFeatures)))
        case "intercept" => Some(Left(Serializer.serializeResponseValue(lorModel.intercept)))
        case "interceptVector" =>
          Some(Left(Serializer.serializeResponseValue(lorModel.interceptVector)))
        case "coefficients" => Some(Left(Serializer.serializeResponseValue(lorModel.coefficients)))
        case "coefficientMatrix" =>
          Some(Left(Serializer.serializeResponseValue(lorModel.coefficientMatrix)))
        case _ =>
          None
      }
    }
  }

  override def getModelSummaryAttr(
      model: Model[_],
      name: String,
      datasetOpt: Option[DataFrame]): Either[proto.MlCommandResponse, DataFrame] = {
    val lorModel = model.asInstanceOf[ml.classification.LogisticRegressionModel]

    val summary = datasetOpt match {
      case Some(dataset) => lorModel.evaluate(dataset)
      case None => lorModel.summary
    }
    val attrValueOpt = if (lorModel.numClasses <= 2) {
      SummaryUtils.getBinaryClassificationSummaryAttr(summary.asBinary, name)
    } else {
      SummaryUtils.getClassificationSummaryAttr(summary, name)
    }
    attrValueOpt
      .orElse(if (datasetOpt.isEmpty) {
        SummaryUtils.getTrainingSummaryAttr(summary.asInstanceOf[TrainingSummary], name)
      } else None)
      .orElse {
        val lorSummary = summary
        name match {
          case "probabilityCol" =>
            Some(Left(Serializer.serializeResponseValue(lorSummary.probabilityCol)))
          case "featuresCol" =>
            Some(Left(Serializer.serializeResponseValue(lorSummary.featuresCol)))
          case _ =>
            throw new IllegalArgumentException()
        }
      }
      .get
  }
}
