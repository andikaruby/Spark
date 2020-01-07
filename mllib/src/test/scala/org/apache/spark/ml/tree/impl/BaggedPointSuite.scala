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

package org.apache.spark.ml.tree.impl

import org.apache.spark.{SparkConf, SparkFunSuite}
import org.apache.spark.internal.config.Kryo._
import org.apache.spark.ml.feature.{Instance, LabeledPoint}
import org.apache.spark.ml.util.MLUtils
import org.apache.spark.mllib.tree.EnsembleTestHelper
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.serializer.KryoSerializer

/**
 * Test suite for [[BaggedPoint]].
 */
class BaggedPointSuite extends SparkFunSuite with MLlibTestSparkContext  {

  test("BaggedPoint RDD: without subsampling with weights") {
    val arr = EnsembleTestHelper.generateOrderedLabeledPoints(1, 1000).map { lp =>
      Instance(lp.label, 0.5, lp.features.asML)
    }
    val rdd = sc.parallelize(arr)
    val baggedRDD = BaggedPoint.convertToBaggedRDD(rdd, 1.0, 1, false,
      (instance: Instance) => instance.weight * 4.0, seed = 42)
    baggedRDD.collect().foreach { baggedPoint =>
      assert(baggedPoint.subsampleCounts.size === 1 && baggedPoint.subsampleCounts(0) === 1)
      assert(baggedPoint.sampleWeight === 2.0)
    }
  }

  test("BaggedPoint RDD: with subsampling with replacement (fraction = 1.0)") {
    val numSubsamples = 100
    val (expectedMean, expectedStddev) = (1.0, 1.0)

    val seeds = Array(123, 5354, 230, 349867, 23987)
    val arr = EnsembleTestHelper.generateOrderedLabeledPoints(1, 1000).map(_.asML)
    val rdd = sc.parallelize(arr)
    seeds.foreach { seed =>
      val baggedRDD = BaggedPoint.convertToBaggedRDD(rdd, 1.0, numSubsamples, true,
        (_: LabeledPoint) => 2.0, seed)
      val subsampleCounts: Array[Array[Double]] =
        baggedRDD.map(_.subsampleCounts.map(_.toDouble)).collect()
      EnsembleTestHelper.testRandomArrays(subsampleCounts, numSubsamples, expectedMean,
        expectedStddev, epsilon = 0.01)
      assert(baggedRDD.collect().forall(_.sampleWeight === 2.0))
    }
  }

  test("BaggedPoint RDD: with subsampling with replacement (fraction = 0.5)") {
    val numSubsamples = 100
    val subsample = 0.5
    val (expectedMean, expectedStddev) = (subsample, math.sqrt(subsample))

    val seeds = Array(123, 5354, 230, 349867, 23987)
    val arr = EnsembleTestHelper.generateOrderedLabeledPoints(1, 1000)
    val rdd = sc.parallelize(arr)
    seeds.foreach { seed =>
      val baggedRDD =
        BaggedPoint.convertToBaggedRDD(rdd, subsample, numSubsamples, true, seed = seed)
      val subsampleCounts: Array[Array[Double]] =
        baggedRDD.map(_.subsampleCounts.map(_.toDouble)).collect()
      EnsembleTestHelper.testRandomArrays(subsampleCounts, numSubsamples, expectedMean,
        expectedStddev, epsilon = 0.01)
    }
  }

  test("BaggedPoint RDD: with subsampling without replacement (fraction = 1.0)") {
    val numSubsamples = 100
    val (expectedMean, expectedStddev) = (1.0, 0)

    val seeds = Array(123, 5354, 230, 349867, 23987)
    val arr = EnsembleTestHelper.generateOrderedLabeledPoints(1, 1000).map(_.asML)
    val rdd = sc.parallelize(arr)
    seeds.foreach { seed =>
      val baggedRDD = BaggedPoint.convertToBaggedRDD(rdd, 1.0, numSubsamples, false,
        (_: LabeledPoint) => 2.0, seed)
      val subsampleCounts: Array[Array[Double]] =
        baggedRDD.map(_.subsampleCounts.map(_.toDouble)).collect()
      EnsembleTestHelper.testRandomArrays(subsampleCounts, numSubsamples, expectedMean,
        expectedStddev, epsilon = 0.01)
      assert(baggedRDD.collect().forall(_.sampleWeight === 2.0))
    }
  }

  test("BaggedPoint RDD: with subsampling without replacement (fraction = 0.5)") {
    val numSubsamples = 100
    val subsample = 0.5
    val (expectedMean, expectedStddev) = (subsample, math.sqrt(subsample * (1 - subsample)))

    val seeds = Array(123, 5354, 230, 349867, 23987)
    val arr = EnsembleTestHelper.generateOrderedLabeledPoints(1, 1000)
    val rdd = sc.parallelize(arr)
    seeds.foreach { seed =>
      val baggedRDD = BaggedPoint.convertToBaggedRDD(rdd, subsample, numSubsamples, false,
        seed = seed)
      val subsampleCounts: Array[Array[Double]] =
        baggedRDD.map(_.subsampleCounts.map(_.toDouble)).collect()
      EnsembleTestHelper.testRandomArrays(subsampleCounts, numSubsamples, expectedMean,
        expectedStddev, epsilon = 0.01)
    }
  }

  test("Kryo class register") {
    val conf = new SparkConf(false)
    MLUtils.registerKryoClasses(conf)
    conf.set(KRYO_REGISTRATION_REQUIRED, true)

    val ser = new KryoSerializer(conf).newInstance()

    val values = Array(1, 2, 3)

    {
      val point = new TreePoint[Int](1.0, values, 1.0)
      val bagged = new BaggedPoint[TreePoint[Int]](point, Array(1), 1.0)
      val bagged2 = ser.deserialize[BaggedPoint[TreePoint[Int]]](ser.serialize(bagged))
      assert(bagged.datum.label === bagged2.datum.label)
      assert(bagged.datum.binnedFeatures === bagged2.datum.binnedFeatures)
      assert(bagged.datum.weight === bagged2.datum.weight)
      assert(bagged.subsampleCounts === bagged2.subsampleCounts)
      assert(bagged.sampleWeight === bagged2.sampleWeight)
    }

    {
      val point = new TreePoint[Short](1.0, values.map(_.toShort), 1.0)
      val bagged = new BaggedPoint[TreePoint[Short]](point, Array(1), 1.0)
      val bagged2 = ser.deserialize[BaggedPoint[TreePoint[Short]]](ser.serialize(bagged))
      assert(bagged.datum.label === bagged2.datum.label)
      assert(bagged.datum.binnedFeatures === bagged2.datum.binnedFeatures)
      assert(bagged.datum.weight === bagged2.datum.weight)
      assert(bagged.subsampleCounts === bagged2.subsampleCounts)
      assert(bagged.sampleWeight === bagged2.sampleWeight)
    }

    {
      val point = new TreePoint[Byte](1.0, values.map(_.toByte), 1.0)
      val bagged = new BaggedPoint[TreePoint[Byte]](point, Array(1), 1.0)
      val bagged2 = ser.deserialize[BaggedPoint[TreePoint[Byte]]](ser.serialize(bagged))
      assert(bagged.datum.label === bagged2.datum.label)
      assert(bagged.datum.binnedFeatures === bagged2.datum.binnedFeatures)
      assert(bagged.datum.weight === bagged2.datum.weight)
      assert(bagged.subsampleCounts === bagged2.subsampleCounts)
      assert(bagged.sampleWeight === bagged2.sampleWeight)
    }
  }
}
