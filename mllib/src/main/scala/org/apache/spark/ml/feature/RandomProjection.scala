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

package org.apache.spark.ml.feature

import scala.util.Random

import breeze.linalg.normalize

import org.apache.spark.annotation.{Experimental, Since}
import org.apache.spark.ml.linalg.{BLAS, Vector, Vectors}
import org.apache.spark.ml.param.{DoubleParam, Params, ParamValidators}
import org.apache.spark.ml.util.Identifiable

/**
 * Params for [[RandomProjection]].
 */
@Experimental
@Since("2.1.0")
private[ml] trait RandomProjectionParams extends Params {
  @Since("2.1.0")
  val bucketLength: DoubleParam = new DoubleParam(this, "bucketLength",
    "the length of each hash bucket, a larger bucket lowers the false negative rate.",
    ParamValidators.gt(0))

  /** @group getParam */
  @Since("2.1.0")
  final def getBucketLength: Double = $(bucketLength)
}

/**
 * Model produced by [[LSH]]
 */
@Experimental
@Since("2.1.0")
class RandomProjectionModel private[ml] (
    @Since("2.1.0") override val uid: String,
    @Since("2.1.0") val randUnitVectors: Array[Vector])
  extends LSHModel[RandomProjectionModel] with RandomProjectionParams {

  @Since("2.1.0")
  override protected[this] val hashFunction: (Vector) => Vector = {
    key: Vector => {
      val hashValues: Array[Double] = randUnitVectors.map({
        randUnitVector => Math.floor(BLAS.dot(key, randUnitVector) / $(bucketLength))
      })
      Vectors.dense(hashValues)
    }
  }

  @Since("2.1.0")
  override protected[ml] def keyDistance(x: Vector, y: Vector): Double = {
    Math.sqrt(Vectors.sqdist(x, y))
  }

  @Since("2.1.0")
  override protected[ml] def hashDistance(x: Vector, y: Vector): Double = {
    // Since it's generated by hashing, it will be a pair of dense vectors.
    x.toDense.values.zip(y.toDense.values).map(x => math.abs(x._1 - x._2)).min
  }
}

/**
 * This [[RandomProjection]] implements Locality Sensitive Hashing functions with 2-stable
 * distributions.
 *
 * References:
 * Wang, Jingdong et al. "Hashing for similarity search: A survey." arXiv preprint
 * arXiv:1408.2927 (2014).
 */
@Experimental
@Since("2.1.0")
class RandomProjection private[ml] (
      @Since("2.1.0") override val uid: String) extends LSH[RandomProjectionModel]
  with RandomProjectionParams {

  @Since("2.1.0")
  override def setInputCol(value: String): this.type = super.setInputCol(value)

  @Since("2.1.0")
  override def setOutputCol(value: String): this.type = super.setOutputCol(value)

  @Since("2.1.0")
  override def setOutputDim(value: Int): this.type = super.setOutputDim(value)

  @Since("2.1.0")
  private[ml] def this() = {
    this(Identifiable.randomUID("random projection"))
  }

  /** @group setParam */
  @Since("2.1.0")
  def setBucketLength(value: Double): this.type = set(bucketLength, value)

  @Since("2.1.0")
  override protected[this] def createRawLSHModel(inputDim: Int): RandomProjectionModel = {
    val randUnitVectors: Array[Vector] = {
      Array.fill($(outputDim)) {
        val randArray = Array.fill(inputDim)(Random.nextGaussian())
        Vectors.fromBreeze(normalize(breeze.linalg.Vector(randArray)))
      }
    }
    new RandomProjectionModel(uid, randUnitVectors)
  }
}
