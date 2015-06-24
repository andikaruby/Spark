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

import scala.beans.BeanInfo

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_1D

import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.sql.{DataFrame, Row}

@BeanInfo
case class DCTTestData(vec: Vector, wantedVec: Vector)

class DiscreteCosineTransformerSuite extends SparkFunSuite with MLlibTestSparkContext {
  import org.apache.spark.ml.feature.DiscreteCosineTransformerSuite._

  test("forward transform of discrete cosine matches jTransforms result") {
    val data = Vectors.dense((0 until 128).map(_ => 2D * math.random - 1D).toArray)
    val inverse = false

    val dataset = sqlContext.createDataFrame(Seq(
      DCTTestData(data, makeExpectedResult(data, inverse))
    ))

    val transformer = new DiscreteCosineTransformer()
      .setInputCol("vec")
      .setOutputCol("resultVec")
      .setInverse(inverse)

    testDCT(transformer, dataset)
  }

  test("inverse transform of discrete cosine matches jTransforms result") {
    val data = Vectors.dense((0 until 128).map(_ => 2D * math.random - 1D).toArray)
    val inverse = true

    val dataset = sqlContext.createDataFrame(Seq(
      DCTTestData(data, makeExpectedResult(data, inverse))
    ))

    val transformer = new DiscreteCosineTransformer()
      .setInputCol("vec")
      .setOutputCol("resultVec")
      .setInverse(inverse)

    testDCT(transformer, dataset)
  }
}

object DiscreteCosineTransformerSuite extends SparkFunSuite {

  def makeExpectedResult(data: Vector, inverse: Boolean): Vector = {
    val expectedResultBuffer = data.toArray.clone()
    if (inverse) {
      (new DoubleDCT_1D(data.size)).inverse(expectedResultBuffer, true)
    } else {
      (new DoubleDCT_1D(data.size)).forward(expectedResultBuffer, true)
    }
    Vectors.dense(expectedResultBuffer)
  }

  def testDCT(t: DiscreteCosineTransformer, dataset: DataFrame): Unit = {
    t.transform(dataset)
      .select("resultVec", "wantedVec")
      .collect()
      .foreach { case Row(resultVec: Vector, wantedVec : Vector) =>
        assert(Vectors.sqdist(resultVec, wantedVec) < 1e-6)
      }
  }
}
