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

package org.apache.spark.streaming.dstream

import org.apache.spark.SparkException
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.apache.spark.streaming.{Duration, Time}
import scala.reflect.ClassTag

private[streaming]
class TransformedDStream[U: ClassTag] (
    parents: Seq[DStream[_]],
    transformFunc: (Seq[RDD[_]], Time) => RDD[U]
  ) extends DStream[U](parents.head.ssc) {

  require(parents.length > 0, "List of DStreams to transform is empty")
  require(parents.map(_.ssc).distinct.size == 1, "Some of the DStreams have different contexts")
  require(parents.map(_.slideDuration).distinct.size == 1,
    "Some of the DStreams have different slide durations")

  override def dependencies: List[DStream[_]] = parents.toList

  override def slideDuration: Duration = parents.head.slideDuration

  override def compute(validTime: Time): Option[RDD[U]] = {
    val parentRDDs = parents.map(_.getOrCompute(validTime).orNull).toSeq
    val transformedRDD = transformFunc(parentRDDs, validTime)
    if (transformedRDD == null) {
      throw new SparkException("Transform function return null instead of an RDD; " + 
          "this should be avoided . You can return an empty RDD using RDD.empty " + 
          "if you dont want to return anything.")
    }
    Some(transformedRDD)
  }
}
