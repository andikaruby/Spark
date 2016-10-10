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

import org.apache.spark.annotation.{Experimental, Since}
import org.apache.spark.ml.linalg.{Vector, Vectors, VectorUDT}
import org.apache.spark.ml.param.{BooleanParam, Params}
import org.apache.spark.ml.util.{Identifiable, SchemaUtils}
import org.apache.spark.sql.types.StructType

/**
 * :: Experimental ::
 * Params for [[MinHash]].
 */
@Since("2.1.0")
private[ml] trait MinHashParams extends Params {

  /**
   * If true, set the random seed to 0. Otherwise, use default setting in scala.util.Random
   * @group param
   */
  @Since("2.1.0")
  val hasSeed: BooleanParam = new BooleanParam(this, "hasSeed",
    "If true, set the random seed to 0.")

  /** @group getParam */
  @Since("2.1.0")
  final def getHasSeed: Boolean = $(hasSeed)
}

/**
 * :: Experimental ::
 * Model produced by [[MinHash]]
 * @param hashFunctions A seq of hash functions, mapping elements to their hash values.
 */
@Experimental
@Since("2.1.0")
class MinHashModel private[ml] (override val uid: String, hashFunctions: Seq[Int => Long])
  extends LSHModel[MinHashModel] {

  @Since("2.1.0")
  override protected[this] val hashFunction: Vector => Vector = {
    elems: Vector =>
      require(elems.numNonzeros > 0, "Must have at least 1 non zero entry.")
      val elemsList = elems.toSparse.indices.toList
      Vectors.dense(hashFunctions.map(
        func => elemsList.map(func).min.toDouble
      ).toArray)
  }

  @Since("2.1.0")
  override protected[ml] def keyDistance(x: Vector, y: Vector): Double = {
    val xSet = x.toSparse.indices.toSet
    val ySet = y.toSparse.indices.toSet
    val intersectionSize = xSet.intersect(ySet).size.toDouble
    val unionSize = xSet.size + ySet.size - intersectionSize
    assert(unionSize > 0, "The union of two input sets must have at least 1 elements")
    1 - intersectionSize / unionSize
  }

  @Since("2.1.0")
  override protected[ml] def hashDistance(x: Vector, y: Vector): Double = {
    // Since it's generated by hashing, it will be a pair of dense vectors.
    x.toDense.values.zip(y.toDense.values).map(x => math.abs(x._1 - x._2)).min
  }
}

/**
 * :: Experimental ::
 * LSH class for Jaccard distance.
 *
 * The input can be dense or sparse vectors, but it is more efficient if it is sparse. For example,
 *    `Vectors.sparse(10, Array[(2, 1.0), (3, 1.0), (5, 1.0)])`
 * means there are 10 elements in the space. This set contains elem 2, elem 3 and elem 5.
 * Also, any input vector must have at least 1 non-zero indices, and all non-zero values are treated
 * as binary "1" values.
 */
@Experimental
@Since("2.1.0")
class MinHash(override val uid: String) extends LSH[MinHashModel] with MinHashParams {

  // A large prime smaller than sqrt(2^63 − 1)
  private[this] val prime = 2038074743

  @Since("2.1.0")
  override def setInputCol(value: String): this.type = super.setInputCol(value)

  @Since("2.1.0")
  override def setOutputCol(value: String): this.type = super.setOutputCol(value)

  @Since("2.1.0")
  override def setOutputDim(value: Int): this.type = super.setOutputDim(value)

  @Since("2.1.0")
  def this() = {
    this(Identifiable.randomUID("min hash"))
  }

  setDefault(outputDim -> 1, outputCol -> "lshFeatures", hasSeed -> false)

  @Since("2.1.0")
  def setHasSeed(value: Boolean): this.type = set(hasSeed, value)

  @Since("2.1.0")
  override protected[this] def createRawLSHModel(inputDim: Int): MinHashModel = {
    require(inputDim <= prime / 2, "The input vector dimension is too large for MinHash to handle.")
    if ($(hasSeed)) Random.setSeed(0)
    val numEntry = inputDim * 2
    val randSeq: Seq[Int] = {
      Seq.fill($(outputDim))(1 + Random.nextInt(prime - 1))
    }
    val hashFunctions: Seq[Int => Long] = {
      randSeq.map { randCoefficient: Int =>
        // Perfect Hash function, use 2n buckets to reduce collision.
        elem: Int => (1 + elem) * randCoefficient.toLong % prime % numEntry
      }
    }
    new MinHashModel(uid, hashFunctions)
  }

  @Since("2.1.0")
  override def transformSchema(schema: StructType): StructType = {
    SchemaUtils.checkColumnType(schema, $(inputCol), new VectorUDT)
    validateAndTransformSchema(schema)
  }
}
