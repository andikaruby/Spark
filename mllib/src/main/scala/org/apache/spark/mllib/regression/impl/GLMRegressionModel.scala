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

package org.apache.spark.mllib.regression.impl

import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.util.Importable
import org.apache.spark.sql.{Row, DataFrame, SQLContext}

/**
 * Helper methods for import/export of GLM regression models.
 */
private[regression] object GLMRegressionModel {

  /** Model data for model import/export */
  case class Data(weights: Vector, intercept: Double)

  def save(
      sc: SparkContext,
      path: String,
      modelClass: String,
      weights: Vector,
      intercept: Double): Unit = {
    val sqlContext = new SQLContext(sc)
    import sqlContext._

    // Create JSON metadata.
    val metadataRDD =
      sc.parallelize(Seq((modelClass, formatVersion))).toDataFrame("class", "version")
    metadataRDD.toJSON.repartition(1).saveAsTextFile(path + "/metadata")

    // Create Parquet data.
    val data = Data(weights, intercept)
    val dataRDD: DataFrame = sc.parallelize(Seq(data))
    // TODO: repartition with 1 partition after SPARK-5532 gets fixed
    dataRDD.saveAsParquetFile(path + "/data")
  }

  private object ImporterV1 {

    def load(sc: SparkContext, path: String, modelClass: String): Data = {
      val sqlContext = new SQLContext(sc)
      val dataRDD = sqlContext.parquetFile(path + "/data")
      val dataArray = dataRDD.select("weights", "intercept").take(1)
      assert(dataArray.size == 1, s"Unable to load $modelClass data from: ${path + "/data"}")
      val data = dataArray(0)
      assert(data.size == 2, s"Unable to load $modelClass data from: ${path + "/data"}")
      data match {
        case Row(weights: Vector, intercept: Double) =>
          Data(weights, intercept)
      }
    }
  }

  def formatVersion: String = "1.0"

  def loadData(sc: SparkContext, path: String, modelClass: String): Data = {
    val (clazz, version, metadata) = Importable.loadMetadata(sc, path)
    // Note: This check of the class name should happen here since we may eventually want to load
    //       other classes (such as deprecated versions).
    assert(clazz == modelClass, s"$modelClass.load" +
      s" was given model file with metadata specifying a different model class: $clazz")
    version match {
      case "1.0" =>
        ImporterV1.load(sc, path, modelClass)
      case _ => throw new Exception(
        s"$modelClass.load did not recognize model format version: $version." +
          s" Supported versions: 1.0.")
    }
  }

}
