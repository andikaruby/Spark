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

// scalastyle:off println
package org.apache.spark.examples.ml

// $example on$
import org.apache.spark.ml.feature.BucketedRandomProjectionLSH
import org.apache.spark.ml.linalg.Vectors
// $example off$
import org.apache.spark.sql.SparkSession

object BucketedRandomProjectionLSHExample {
  def main(args: Array[String]): Unit = {
    // Creates a SparkSession
    val spark = SparkSession
      .builder
      .appName("BucketedRandomProjectionLSHExample")
      .getOrCreate()

    // $example on$
    val dfA = spark.createDataFrame(Seq(
      (0, Vectors.dense(1.0, 1.0)),
      (1, Vectors.dense(1.0, -1.0)),
      (2, Vectors.dense(-1.0, -1.0)),
      (3, Vectors.dense(-1.0, 1.0))
    )).toDF("id", "features")

    val dfB = spark.createDataFrame(Seq(
      (4, Vectors.dense(1.0, 0.0)),
      (5, Vectors.dense(-1.0, 0.0)),
      (6, Vectors.dense(0.0, 1.0)),
      (7, Vectors.dense(0.0, -1.0))
    )).toDF("id", "features")

    val key = Vectors.dense(1.0, 0.0)

    val brp = new BucketedRandomProjectionLSH()
      .setBucketLength(2.0)
      .setNumHashTables(3)
      .setInputCol("features")
      .setOutputCol("hashes")

    val model = brp.fit(dfA)

    // Feature Transformation
    println("The hashed dataset where hashed values are stored in the column 'values':")
    model.transform(dfA).show()
    // Cache the transformed columns
    val transformedA = model.transform(dfA).cache()
    val transformedB = model.transform(dfB).cache()

    // Approximate similarity join
    println("Approximately joining dfA and dfB on distance smaller than 1.5:")
    model.approxSimilarityJoin(dfA, dfB, 1.5)
      .select("datasetA.id", "datasetB.id", "distCol")
      .show()
    println("Joining cached datasets to avoid recomputing the hash values:")
    model.approxSimilarityJoin(transformedA, transformedB, 1.5)
      .select("datasetA.id", "datasetB.id", "distCol")
      .show()

    // Self Join
    println("Approximately self join of dfB on distance smaller than 2.5:")
    model.approxSimilarityJoin(dfA, dfA, 2.5)
      .filter("datasetA.id < datasetB.id")
      .select("datasetA.id", "datasetB.id", "distCol")
      .show()

    // Approximate nearest neighbor search
    println("Approximately searching dfA for 2 nearest neighbors of the key:")
    model.approxNearestNeighbors(dfA, key, 2).show()
    println("Searching cached dataset to avoid recomputing the hash values:")
    model.approxNearestNeighbors(transformedA, key, 2).show()
    // $example off$

    spark.stop()
  }
}
// scalastyle:on println
