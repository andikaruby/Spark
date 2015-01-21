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

package org.apache.spark.mllib.classification

import breeze.linalg.{DenseMatrix => BDM, DenseVector => BDV, argmax => brzArgmax, sum => brzSum, Axis}
import breeze.stats.distributions.Multinomial
import org.apache.spark.mllib.classification.NaiveBayesModels.NaiveBayesModels

import scala.util.Random

import org.scalatest.FunSuite

import org.apache.spark.SparkException
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.{LocalClusterSparkContext, MLlibTestSparkContext}

object NaiveBayesSuite {

  private def calcLabel(p: Double, pi: Array[Double]): Int = {
    var sum = 0.0
    for (j <- 0 until pi.length) {
      sum += pi(j)
      if (p < sum) return j
    }
    -1
  }

  // Generate input of the form Y = (theta * x).argmax()
  def generateNaiveBayesInput(
    pi: Array[Double],            // 1XC
    theta: Array[Array[Double]],  // CXD
    nPoints: Int,
    seed: Int,
    dataModel: NaiveBayesModels = NaiveBayesModels.Multinomial,
    sample: Int = 10): Seq[LabeledPoint] = {
    val D = theta(0).length
    val rnd = new Random(seed)

    val _pi = pi.map(math.pow(math.E, _))
    val _theta = theta.map(row => row.map(math.pow(math.E, _)))

    for (i <- 0 until nPoints) yield {
      val y = calcLabel(rnd.nextDouble(), _pi)
      val xi = dataModel match {
        case NaiveBayesModels.Bernoulli => Array.tabulate[Double] (D) {j =>
            if (rnd.nextDouble () < _theta(y)(j) ) 1 else 0
        }
        case NaiveBayesModels.Multinomial =>
          val mult = Multinomial(BDV(_theta(y)))
          val emptyMap = (0 until D).map(x => (x, 0.0)).toMap
          val counts = emptyMap ++ mult.sample(sample).groupBy(x => x).map {
            case (index, reps) => (index, reps.size.toDouble)
          }
          counts.toArray.sortBy(_._1).map(_._2)
      }

      LabeledPoint(y, Vectors.dense(xi))
    }
  }
}

class NaiveBayesSuite extends FunSuite with MLlibTestSparkContext {

  def validatePrediction(predictions: Seq[Double], input: Seq[LabeledPoint]) {
    val numOfPredictions = predictions.zip(input).count {
      case (prediction, expected) =>
        prediction != expected.label
    }
    // At least 80% of the predictions should be on.
    assert(numOfPredictions < input.length / 5)
  }

  def validateModelFit(piData: Array[Double], thetaData: Array[Array[Double]], model: NaiveBayesModel) = {
    def closeFit(d1: Double, d2: Double, precision: Double): Boolean = {
      (d1 - d2).abs <= precision
    }
    val modelIndex = (0 until piData.length).zip(model.labels.map(_.toInt))
    for (i <- modelIndex) {
      assert(closeFit(math.exp(piData(i._2)), math.exp(model.pi(i._1)), 0.05))
    }
    for (i <- modelIndex) {
      val sortedData = thetaData(i._2).sorted
      val sortedModel = model.theta(i._1).sorted
      for (j <- 0 until sortedData.length) {
        assert(closeFit(math.exp(sortedData(j)), math.exp(sortedModel(j)), 0.05))
      }
    }
  }

  test("Naive Bayes Multinomial") {
    val nPoints = 1000

    val pi = Array(0.5, 0.1, 0.4).map(math.log)
    val theta = Array(
      Array(0.70, 0.10, 0.10, 0.10), // label 0
      Array(0.10, 0.70, 0.10, 0.10), // label 1
      Array(0.10, 0.10, 0.70, 0.10)  // label 2
    ).map(_.map(math.log))

    val testData = NaiveBayesSuite.generateNaiveBayesInput(pi, theta, nPoints, 42, NaiveBayesModels.Multinomial)
    val testRDD = sc.parallelize(testData, 2)
    testRDD.cache()

    val model = NaiveBayes.train(testRDD, 1.0, "Multinomial")
    validateModelFit(pi, theta, model)

    val validationData = NaiveBayesSuite.generateNaiveBayesInput(pi, theta, nPoints, 17, NaiveBayesModels.Multinomial)
    val validationRDD = sc.parallelize(validationData, 2)

    // Test prediction on RDD.
    validatePrediction(model.predict(validationRDD.map(_.features)).collect(), validationData)

    // Test prediction on Array.
    validatePrediction(validationData.map(row => model.predict(row.features)), validationData)
  }

  test("Naive Bayes Bernoulli") {
    val nPoints = 10000

    val pi = Array(0.5, 0.3, 0.2).map(math.log)
    val theta = Array(
      Array(0.50, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.40), // label 0
      Array(0.02, 0.70, 0.10, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02), // label 1
      Array(0.02, 0.02, 0.60, 0.02,  0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.30)  // label 2
    ).map(_.map(math.log))


    val testData = NaiveBayesSuite.generateNaiveBayesInput(pi, theta, nPoints, 45, NaiveBayesModels.Bernoulli)
    val testRDD = sc.parallelize(testData, 2)
    testRDD.cache()

    val model = NaiveBayes.train(testRDD, 1.0, "Bernoulli") ///!!! this gives same result on both models check the math
    validateModelFit(pi, theta, model)

    val validationData = NaiveBayesSuite.generateNaiveBayesInput(pi, theta, nPoints, 20, NaiveBayesModels.Bernoulli)
    val validationRDD = sc.parallelize(validationData, 2)

    // Test prediction on RDD.
    validatePrediction(model.predict(validationRDD.map(_.features)).collect(), validationData)

    // Test prediction on Array.
    validatePrediction(validationData.map(row => model.predict(row.features)), validationData)
  }

  test("detect negative values") {
    val dense = Seq(
      LabeledPoint(1.0, Vectors.dense(1.0)),
      LabeledPoint(0.0, Vectors.dense(-1.0)),
      LabeledPoint(1.0, Vectors.dense(1.0)),
      LabeledPoint(1.0, Vectors.dense(0.0)))
    intercept[SparkException] {
      NaiveBayes.train(sc.makeRDD(dense, 2))
    }
    val sparse = Seq(
      LabeledPoint(1.0, Vectors.sparse(1, Array(0), Array(1.0))),
      LabeledPoint(0.0, Vectors.sparse(1, Array(0), Array(-1.0))),
      LabeledPoint(1.0, Vectors.sparse(1, Array(0), Array(1.0))),
      LabeledPoint(1.0, Vectors.sparse(1, Array.empty, Array.empty)))
    intercept[SparkException] {
      NaiveBayes.train(sc.makeRDD(sparse, 2))
    }
    val nan = Seq(
      LabeledPoint(1.0, Vectors.sparse(1, Array(0), Array(1.0))),
      LabeledPoint(0.0, Vectors.sparse(1, Array(0), Array(Double.NaN))),
      LabeledPoint(1.0, Vectors.sparse(1, Array(0), Array(1.0))),
      LabeledPoint(1.0, Vectors.sparse(1, Array.empty, Array.empty)))
    intercept[SparkException] {
      NaiveBayes.train(sc.makeRDD(nan, 2))
    }
  }
}

class NaiveBayesClusterSuite extends FunSuite with LocalClusterSparkContext {

  test("task size should be small in both training and prediction") {
    val m = 10
    val n = 200000
    val examples = sc.parallelize(0 until m, 2).mapPartitionsWithIndex { (idx, iter) =>
      val random = new Random(idx)
      iter.map { i =>
        LabeledPoint(random.nextInt(2), Vectors.dense(Array.fill(n)(random.nextDouble())))
      }
    }
    // If we serialize data directly in the task closure, the size of the serialized task would be
    // greater than 1MB and hence Spark would throw an error.
    val model = NaiveBayes.train(examples)
    val predictions = model.predict(examples.map(_.features))
  }
}
