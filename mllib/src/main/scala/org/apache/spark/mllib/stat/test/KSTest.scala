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

package org.apache.spark.mllib.stat.test

import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest

import org.apache.spark.{SparkException, Logging}
import org.apache.spark.rdd.RDD


/**
 * Conduct the two-sided Kolmogorov Smirnov test for data sampled from a
 * continuous distribution. By comparing the largest difference between the empirical cumulative
 * distribution of the sample data and the theoretical distribution we can provide a test for the
 * the null hypothesis that the sample data comes from that theoretical distribution.
 * For more information on KS Test: https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test
 */
  private[stat] object KSTest {

  // Null hypothesis for the type of KS test to be included in the result.
  object NullHypothesis extends Enumeration {
    type NullHypothesis = Value
    val oneSampleTwoSided = Value("Sample follows theoretical distribution.")
  }

  /**
   * Calculate empirical cumulative distribution values needed for KS statistic
   * @param dat data over which we which to calculate the empirical cumulative distribution values
   * @return and RDD of (Double, Double, Double), where the first element in each tuple is the
   *         value, the second element is the ECDFV - 1 /n, and the third element is the ECDFV,
   *         where ECDF stands for empirical cumulative distribution function value
   *
   */
  def empirical(dat: RDD[Double]): RDD[(Double, Double, Double)] = {
    val n = dat.count().toDouble
    dat.sortBy(x => x).zipWithIndex().map { case (v, i) => (v, i / n, (i + 1) / n) }
  }

  /**
   * Runs a KS test for 1 set of sample data, comparing it to a theoretical distribution
   * @param dat the data we wish to evaluate
   * @param cdf a function to calculate the
   * @return a KSTestResult summarizing the test results (pval, statistic, and null hypothesis)
   */
  def testOneSample(dat: RDD[Double], cdf: Double => Double): KSTestResult = {
    val empiriRDD = empirical(dat) // empirical distribution
    val distances = empiriRDD.map {
        case (v, dl, dp) =>
          val cdfVal = cdf(v)
          Math.max(cdfVal - dl, dp - cdfVal)
      }
    val ksStat = distances.max()
    evalOneSampleP(ksStat, distances.count())
  }

  /**
   * Runs a KS test for 1 set of sample data, comparing it to a theoretical distribution. Optimized
   * such that each partition runs a separate mapping operation. This can help in cases where the
   * CDF calculation involves creating an object. By using this implementation we can make sure
   * only 1 object is created per partition, versus 1 per observation.
   * @param dat the data we wish to evaluate
   * @param distCalc a function to calculate the distance between the empirical values and the
   *                 theoretical value
   * @return a KSTestResult summarizing the test results (pval, statistic, and null hypothesis)
   */
  def testOneSampleOpt(dat: RDD[Double],
                       distCalc: Iterator[(Double, Double, Double)] => Iterator[Double])
  : KSTestResult = {
    val empiriRDD = empirical(dat) // empirical distribution information
    val distances = empiriRDD.mapPartitions(distCalc, false)
    val ksStat = distances.max
    evalOneSampleP(ksStat, distances.count())
  }

  /**
   * Returns a function to calculate the KSTest with a standard normal distribution
   * to be used with testOneSampleOpt
   * @return Return a function that we can map over partitions to calculate the KS distance in each
   */
  def stdNormDistances(): (Iterator[(Double, Double, Double)]) => Iterator[Double] = {
    val dist = new NormalDistribution(0, 1)
    (part: Iterator[(Double, Double, Double)]) => part.map {
      case (v, dl, dp) =>
        val cdfVal = dist.cumulativeProbability(v)
        Math.max(cdfVal - dl, dp - cdfVal)
    }
  }

  /**
   * A convenience function that allows running the KS test for 1 set of sample data against
   * a named distribution
   * @param dat the sample data that we wish to evaluate
   * @param distName the name of the theoretical distribution
   * @return The KS statistic and p-value associated with a two sided test
   */
  def testOneSample(dat: RDD[Double], distName: String): KSTestResult = {
    val distanceCalc =
      distName match {
        case "stdnorm" => stdNormDistances()
        case  _ =>  throw new UnsupportedOperationException()
      }

    testOneSampleOpt(dat, distanceCalc)
  }

  private def evalOneSampleP(ksStat: Double, n: Long): KSTestResult = {
    val pval = 1 - new KolmogorovSmirnovTest().cdf(ksStat, n.toInt)
    new KSTestResult(pval, ksStat, NullHypothesis.oneSampleTwoSided.toString)
  }
}