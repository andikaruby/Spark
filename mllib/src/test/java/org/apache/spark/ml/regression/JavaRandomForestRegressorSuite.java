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

package org.apache.spark.ml.regression;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.tree.impl.TreeTests;
import org.apache.spark.mllib.classification.LogisticRegressionSuite;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.Utils;


public class JavaRandomForestRegressorSuite implements Serializable {

  private transient SparkSession spark;
  private transient JavaSparkContext jsc;

  @Before
  public void setUp() {
    spark = SparkSession.builder()
      .master("local")
      .appName("JavaRandomForestRegressorSuite")
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
    Dataset<Row> dataFrame = TreeTests.setMetadata(data, categoricalFeatures, 0);

    // This tests setters. Training with various options is tested in Scala.
    RandomForestRegressor rf = new RandomForestRegressor()
      .setMaxDepth(2)
      .setMaxBins(10)
      .setMinInstancesPerNode(5)
      .setMinInfoGain(0.0)
      .setMaxMemoryInMB(256)
      .setCacheNodeIds(false)
      .setCheckpointInterval(10)
      .setSubsamplingRate(1.0)
      .setSeed(1234)
      .setNumTrees(3)
      .setMaxDepth(2); // duplicate setMaxDepth to check builder pattern
    for (String impurity : RandomForestRegressor.supportedImpurities()) {
      rf.setImpurity(impurity);
    }
    for (String featureSubsetStrategy : RandomForestRegressor.supportedFeatureSubsetStrategies()) {
      rf.setFeatureSubsetStrategy(featureSubsetStrategy);
    }
    String[] realStrategies = {".1", ".10", "0.10", "0.1", "0.9", "1.0"};
    for (String strategy : realStrategies) {
      rf.setFeatureSubsetStrategy(strategy);
    }
    String[] integerStrategies = {"1", "10", "100", "1000", "10000"};
    for (String strategy : integerStrategies) {
      rf.setFeatureSubsetStrategy(strategy);
    }
    String[] invalidStrategies = {"-.1", "-.10", "-0.10", ".0", "0.0", "1.1", "0"};
    for (String strategy : invalidStrategies) {
      try {
        rf.setFeatureSubsetStrategy(strategy);
        Assert.fail("Expected exception to be thrown for invalid strategies");
      } catch (Exception e) {
        Assert.assertTrue(e instanceof IllegalArgumentException);
      }
    }

    RandomForestRegressionModel model = rf.fit(dataFrame);

    model.transform(dataFrame);
    model.totalNumNodes();
    model.toDebugString();
    model.trees();
    model.treeWeights();
    Vector importances = model.featureImportances();

    File tempDir = Utils.createTempDir(System.getProperty("java.io.tmpdir"), "spark");
    String path = tempDir.toURI().toString();
    try {
      model.save(path);
      RandomForestRegressionModel sameModel = RandomForestRegressionModel.load(path);
      TreeTests.checkEqual(model, sameModel);
    } finally {
      Utils.deleteRecursively(tempDir);
    }
  }
}
