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

package org.apache.spark.mllib.kernels

import org.apache.spark.Logging
import org.apache.spark.mllib.linalg.{DenseVector, Vectors, Vector}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

/**
 * Trait defining the basic behavior
 * of a Kernel density estimator
 */
trait KernelEstimator extends Logging {

  protected def R(r: Int, N: Long, pilot: breeze.linalg.Vector[Double],
                  kernel: RDD[((Long, Long), Vector)]): breeze.linalg.Vector[Double]


  /**
   * Calculate the AMISE (Asymptotic Mean Integrated Square Error)
   * optimal bandwidth assignment by 'solve the equation plug in method'
   **/
  def optimalBandwidth(data: RDD[Vector]): Unit

}
