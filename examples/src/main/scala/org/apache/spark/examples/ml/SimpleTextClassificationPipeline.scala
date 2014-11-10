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

package org.apache.spark.examples.ml

import org.apache.spark.sql.{StringType, DataType, SQLContext}

import scala.beans.BeanInfo

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.{Pipeline, UnaryTransformer}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.HashingTF
import org.apache.spark.ml.param.ParamMap

@BeanInfo
case class LabeledDocument(id: Long, text: String, label: Double)

@BeanInfo
case class Document(id: Long, text: String)

/**
 * A tokenizer that converts the input string to lowercase and then splits it by white spaces.
 */
class MyTokenizer extends UnaryTransformer[String, Seq[String], MyTokenizer] {

  override def createTransformFunc(paramMap: ParamMap): String => Seq[String] = {
    _.toLowerCase.split("\\s")
  }

  override protected def validateInputType(inputType: DataType): Unit = {
    require(inputType == StringType, s"Input type must be string type but got $inputType.")
  }
}

/**
 * A simple text classification pipeline that recognizes "spark" from input text. This is to show
 * how to define a simple tokenizer and then use it as part of a ML pipeline. Run with
 * {{{
 * bin/run-example ml.SimpleTextClassificationPipeline
 * }}}
 */
object SimpleTextClassificationPipeline {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("SimpleTextClassificationPipeline")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext._

    // Prepare training documents, which are labeled.
    val training = sparkContext.parallelize(Seq(
      LabeledDocument(0L, "a b c d e spark", 1.0),
      LabeledDocument(1L, "b d", 0.0),
      LabeledDocument(2L, "spark f g h", 1.0),
      LabeledDocument(3L, "hadoop mapreduce", 0.0)))

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
    val tokenizer = new MyTokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setNumFeatures(1000)
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.01)
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))

    // Fit the pipeline to training documents.
    val model = pipeline.fit(training)

    // Prepare test documents, which are unlabeled.
    val test = sparkContext.parallelize(Seq(
      Document(4L, "spark i j k"),
      Document(5L, "l m n"),
      Document(6L, "mapreduce spark"),
      Document(7L, "apache hadoop")))

    // Make predictions on test documents.
    model.transform(test)
      .select('id, 'text, 'score, 'prediction)
      .collect()
      .foreach(println)
  }
}
