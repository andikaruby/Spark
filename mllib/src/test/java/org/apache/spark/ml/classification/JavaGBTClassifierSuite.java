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

package org.apache.spark.ml.classification;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.tree.impl.TreeTests;
import org.apache.spark.mllib.classification.LogisticRegressionSuite;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.Utils;


public class JavaGBTClassifierSuite implements Serializable {

  private transient SparkSession spark;
  private transient JavaSparkContext jsc;

  @Before
  public void setUp() {
    spark = SparkSession.builder()
      .master("local")
      .appName("JavaGBTClassifierSuite")
      .getOrCreate();
    jsc = new JavaSparkContext(spark.sparkContext());
  }

  @After
  public void tearDown() {
    spark.stop();
    spark = null;
  }

  @Test
  public void runDT() {
    int nPoints = 20;
    double A = 2.0;
    double B = -1.5;

    JavaRDD<LabeledPoint> data = jsc.parallelize(
      LogisticRegressionSuite.generateLogisticInputAsList(A, B, nPoints, 42), 2).cache();
    Map<Integer, Integer> categoricalFeatures = new HashMap<>();
    Dataset<Row> dataFrame = TreeTests.setMetadata(data, categoricalFeatures, 2);

    // This tests setters. Training with various options is tested in Scala.
    GBTClassifier rf = new GBTClassifier()
      .setMaxDepth(2)
      .setMaxBins(10)
      .setMinInstancesPerNode(5)
      .setMinInfoGain(0.0)
      .setMaxMemoryInMB(256)
      .setCacheNodeIds(false)
      .setCheckpointInterval(10)
      .setSubsamplingRate(1.0)
      .setSeed(1234)
      .setMaxIter(3)
      .setStepSize(0.1)
      .setMaxDepth(2); // duplicate setMaxDepth to check builder pattern
    for (String lossType : GBTClassifier.supportedLossTypes()) {
      rf.setLossType(lossType);
    }
    GBTClassificationModel model = rf.fit(dataFrame);

    model.transform(dataFrame);
    model.totalNumNodes();
    model.toDebugString();
    model.trees();
    model.treeWeights();


    File tempDir = Utils.createTempDir(System.getProperty("java.io.tmpdir"), "spark");
    String path = tempDir.toURI().toString();
    try {
      model.save(path);
      GBTClassificationModel sameModel = GBTClassificationModel.load(path);
      TreeTests.checkEqual(model, sameModel);
    } finally {
      Utils.deleteRecursively(tempDir);
    }
  }
}
