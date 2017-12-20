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

package org.apache.spark.ml.evaluation

import org.apache.spark.SparkFunSuite
import org.apache.spark.ml.param.ParamsSuite
import org.apache.spark.ml.util.{DefaultReadWriteTest, MLTestingUtils}
import org.apache.spark.mllib.util.MLlibTestSparkContext

class MulticlassClassificationEvaluatorSuite
  extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest {
  import testImplicits._

  test("params") {
    ParamsSuite.checkParams(new MulticlassClassificationEvaluator)
  }

  test("read/write") {
    val evaluator = new MulticlassClassificationEvaluator()
      .setPredictionCol("myPrediction")
      .setLabelCol("myLabel")
      .setMetricName("accuracy")
    testDefaultReadWrite(evaluator)
  }

  test("should support all NumericType labels and not support other types") {
    MLTestingUtils.checkNumericTypes(new MulticlassClassificationEvaluator, spark)
  }

  test("getMetrics should return a MultiClassClassificationMetrics") {
    val df = Seq(
      (1d, 0d),
      (0d, 1d),
      (2d, 2d)
    ).toDF("prediction", "label")
    val evaluator = new MulticlassClassificationEvaluator().setMetricName("accuracy")
    val metrics = evaluator.getMetrics(df)
    assert(metrics.accuracy == evaluator.evaluate(df))
  }
}
