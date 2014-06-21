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

package org.apache.spark.mllib.regression

import org.scalatest.FunSuite

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.util.{LinearDataGenerator, LocalSparkContext}

class LassoSuite extends FunSuite with LocalSparkContext {

  def validatePrediction(predictions: Seq[Double], input: Seq[LabeledPoint]) {
    val numOffPredictions = predictions.zip(input).count { case (prediction, expected) =>
      // A prediction is off if the prediction is more than 0.5 away from expected value.
      math.abs(prediction - expected.label) > 0.5
    }
    // At least 80% of the predictions should be on.
    assert(numOffPredictions < input.length / 5)
  }

  test("Lasso local random SGD") {
    val nPoints = 1000

    val A = 2.0
    val B = -1.5
    val C = 1.0e-2

    val testData = LinearDataGenerator.generateLinearInput(A, Array[Double](B, C), nPoints, 42)
      .map { case LabeledPoint(label, features) =>
      LabeledPoint(label, Vectors.dense(1.0 +: features.toArray))
    }
    val testRDD = sc.parallelize(testData, 2).cache()

    val ls = new LassoWithSGD()
    ls.optimizer.setStepSize(1.0).setRegParam(0.01).setNumIterations(40)

    val model = ls.run(testRDD)
    val weight0 = model.weights(0)
    val weight1 = model.weights(1)
    val weight2 = model.weights(2)
    assert(weight0 >= 1.9 && weight0 <= 2.1, weight0 + " not in [1.9, 2.1]")
    assert(weight1 >= -1.60 && weight1 <= -1.40, weight1 + " not in [-1.6, -1.4]")
    assert(weight2 >= -1.0e-3 && weight2 <= 1.0e-3, weight2 + " not in [-0.001, 0.001]")

    val validationData = LinearDataGenerator.generateLinearInput(A, Array[Double](B,C), nPoints, 17)
      .map { case LabeledPoint(label, features) =>
      LabeledPoint(label, Vectors.dense(1.0 +: features.toArray))
    }
    val validationRDD  = sc.parallelize(validationData, 2)

    // Test prediction on RDD.
    validatePrediction(model.predict(validationRDD.map(_.features)).collect(), validationData)

    // Test prediction on Array.
    validatePrediction(validationData.map(row => model.predict(row.features)), validationData)
  }

  test("Lasso local random SGD with initial weights") {
    val nPoints = 1000

    val A = 2.0
    val B = -1.5
    val C = 1.0e-2

    val testData = LinearDataGenerator.generateLinearInput(A, Array[Double](B, C), nPoints, 42)
      .map { case LabeledPoint(label, features) =>
      LabeledPoint(label, Vectors.dense(1.0 +: features.toArray))
    }

    val initialA = -1.0
    val initialB = -1.0
    val initialC = -1.0
    val initialWeights = Vectors.dense(initialA, initialB, initialC)

    val testRDD = sc.parallelize(testData, 2).cache()

    val ls = new LassoWithSGD()
    ls.optimizer.setStepSize(1.0).setRegParam(0.01).setNumIterations(40)

    val model = ls.run(testRDD, initialWeights)
    val weight0 = model.weights(0)
    val weight1 = model.weights(1)
    val weight2 = model.weights(2)
    assert(weight0 >= 1.9 && weight0 <= 2.1, weight0 + " not in [1.9, 2.1]")
    assert(weight1 >= -1.60 && weight1 <= -1.40, weight1 + " not in [-1.6, -1.4]")
    assert(weight2 >= -1.0e-3 && weight2 <= 1.0e-3, weight2 + " not in [-0.001, 0.001]")

    val validationData = LinearDataGenerator.generateLinearInput(A, Array[Double](B,C), nPoints, 17)
      .map { case LabeledPoint(label, features) =>
      LabeledPoint(label, Vectors.dense(1.0 +: features.toArray))
    }
    val validationRDD  = sc.parallelize(validationData,2)

    // Test prediction on RDD.
    validatePrediction(model.predict(validationRDD.map(_.features)).collect(), validationData)

    // Test prediction on Array.
    validatePrediction(validationData.map(row => model.predict(row.features)), validationData)
  }
}
