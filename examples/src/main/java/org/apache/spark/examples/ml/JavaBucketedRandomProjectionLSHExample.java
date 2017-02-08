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

package org.apache.spark.examples.ml;

import org.apache.spark.sql.SparkSession;

// $example on$
import java.util.Arrays;
import java.util.List;

import org.apache.spark.ml.feature.BucketedRandomProjectionLSH;
import org.apache.spark.ml.feature.BucketedRandomProjectionLSHModel;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
// $example off$

public class JavaBucketedRandomProjectionLSHExample {
  public static void main(String[] args) {
    SparkSession spark = SparkSession
      .builder()
      .appName("JavaBucketedRandomProjectionLSHExample")
      .getOrCreate();

    // $example on$
    List<Row> dataA = Arrays.asList(
      RowFactory.create(0, Vectors.dense(1.0, 1.0)),
      RowFactory.create(1, Vectors.dense(1.0, -1.0)),
      RowFactory.create(2, Vectors.dense(-1.0, -1.0)),
      RowFactory.create(3, Vectors.dense(-1.0, 1.0))
    );

    List<Row> dataB = Arrays.asList(
        RowFactory.create(4, Vectors.dense(1.0, 0.0)),
        RowFactory.create(5, Vectors.dense(-1.0, 0.0)),
        RowFactory.create(6, Vectors.dense(0.0, 1.0)),
        RowFactory.create(7, Vectors.dense(0.0, -1.0))
    );

    StructType schema = new StructType(new StructField[]{
      new StructField("id", DataTypes.IntegerType, false, Metadata.empty()),
      new StructField("features", new VectorUDT(), false, Metadata.empty())
    });
    Dataset<Row> dfA = spark.createDataFrame(dataA, schema);
    Dataset<Row> dfB = spark.createDataFrame(dataB, schema);

    Vector key = Vectors.dense(1.0, 0.0);

    BucketedRandomProjectionLSH mh = new BucketedRandomProjectionLSH()
      .setBucketLength(2.0)
      .setNumHashTables(3)
      .setInputCol("features")
      .setOutputCol("hashes");

    BucketedRandomProjectionLSHModel model = mh.fit(dfA);

    // Feature Transformation
    System.out.println("The hashed dataset where hashed values are stored in the column 'values':");
    model.transform(dfA).show();
    // Cache the transformed columns
    Dataset<Row> transformedA = model.transform(dfA).cache();
    Dataset<Row> transformedB = model.transform(dfB).cache();

    // Approximate similarity join
    System.out.println("Approximately joining dfA and dfB on distance smaller than 1.5:");
    model.approxSimilarityJoin(dfA, dfB, 1.5).show();
    System.out.println("Joining cached datasets to avoid recomputing the hash values:");
    model.approxSimilarityJoin(transformedA, transformedB, 1.5).show();

    // Self Join
    System.out.println("Approximately self join of dfB on distance smaller than 2.5:");
    model.approxSimilarityJoin(dfA, dfA, 2.5).filter("datasetA.id < datasetB.id").show();

    // Approximate nearest neighbor search
    System.out.println("Approximately searching dfA for 2 nearest neighbors of the key:");
    model.approxNearestNeighbors(dfA, key, 2).show();
    System.out.println("Searching cached dataset to avoid recomputing the hash values:");
    model.approxNearestNeighbors(transformedA, key, 2).show();
    // $example off$

    spark.stop();
  }
}
