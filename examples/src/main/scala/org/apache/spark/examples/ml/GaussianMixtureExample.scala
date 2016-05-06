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

package org.apache.spark.examples.ml

// scalastyle:off println

import org.apache.spark.{SparkConf, SparkContext}
// $example on$
import org.apache.spark.ml.clustering.{GaussianMixture, GaussianMixtureModel}
import org.apache.spark.mllib.linalg.{Vectors, VectorUDT}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{StructField, StructType}
// $example off$

/**
 * An example demonstrating Gaussian Mixture Model (GMM).
 * Run with
 * {{{
 * bin/run-example ml.GaussianMixtureExample
 * }}}
 */
object GaussianMixtureExample {
  def main(args: Array[String]): Unit = {
    // Creates a SparkSession
    val spark = SparkSession.builder.appName(s"${this.getClass.getSimpleName}").getOrCreate()
    val input = "data/mllib/gmm_data.txt"

    // $example on$
    // Crates a DataFrame
    val rowRDD = spark.read.text(input).rdd.filter(_.nonEmpty)
      .map(_.trim.split(" ").map(_.toDouble)).map(Vectors.dense).map(Row(_))
    val schema = StructType(Array(StructField("features", new VectorUDT, false)))
    val dataset: DataFrame = spark.createDataFrame(rowRDD, schema)

    // Trains Gaussian Mixture Model
    val gmm = new GaussianMixture()
      .setK(2)
      .setFeaturesCol("features")
      .setPredictionCol("prediction")
      .setTol(0.0001)
      .setMaxIter(10)
      .setSeed(10)
    val model = gmm.fit(dataset)


    // output parameters of max-likelihood model
    for (i <- 0 until model.getK) {
      println("weight=%f\nmu=%s\nsigma=\n%s\n" format
        (model.weights(i), model.gaussians(i).mean, model.gaussians(i).cov))
    }
    // $example off$

    spark.stop()
  }
}
// scalastyle:on println
