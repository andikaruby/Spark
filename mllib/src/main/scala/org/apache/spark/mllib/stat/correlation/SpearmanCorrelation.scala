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

package org.apache.spark.mllib.stat.correlation

import scala.collection.mutable.ArrayBuffer

import org.apache.spark.Logging
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.linalg.{Matrix, Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

/**
 * Compute Spearman's correlation for two RDDs of the type RDD[Double] or the correlation matrix
 * for an RDD of the type RDD[Vector].
 *
 * Definition of Spearman's correlation can be found at
 * http://en.wikipedia.org/wiki/Spearman's_rank_correlation_coefficient
 */
private[stat] object SpearmanCorrelation extends Correlation with Logging {

  /**
   * Compute Spearman's correlation for two datasets.
   */
  override def computeCorrelation(x: RDD[Double], y: RDD[Double]): Double = {
    computeCorrelationWithMatrixImpl(x, y)
  }

  /**
   * Compute Spearman's correlation matrix S, for the input matrix, where S(i, j) is the
   * correlation between column i and j.
   */
  override def computeCorrelationMatrix(X: RDD[Vector]): Matrix = {
    // ((columnIndex, value), rowId)
    val colBased = X.zipWithUniqueId().flatMap { case (vec, uid) =>
      vec.toArray.view.zipWithIndex.map { case (v, j) =>
        ((j, v), uid)
      }
    }.persist(StorageLevel.MEMORY_AND_DISK) // used by sortByKey
    // global sort by (columnIndex, value)
    val sorted = colBased.sortByKey().persist(StorageLevel.MEMORY_AND_DISK) // used by zipWithIndex
    // Assign global ranks (using average ranks for tied values)
    val globalRanks = sorted.zipWithIndex().mapPartitions { iter =>
      var preCol = -1
      var preVal = Double.NaN
      var startRank = -1.0
      var cachedIds = ArrayBuffer.empty[Long]
      def flush(): Iterable[(Long, (Int, Double))] = {
        val averageRank = startRank + (cachedIds.size - 1) / 2.0
        val output = cachedIds.map { i =>
          (i, (preCol, averageRank))
        }
        cachedIds.clear()
        output
      }
      iter.flatMap { case (((j, v), uid), rank) =>
        if (j != preCol || v != preVal) {
          val output = flush()
          preCol = j
          preVal = v
          startRank = rank
          cachedIds += uid
          output
        } else {
          cachedIds += uid
          Iterator.empty
        }
      } ++ {
        flush()
      }
    }
    // Replace values in the input matrix by their ranks compared with values in the same column.
    // Note that shifting all ranks in a column by a constant value doesn't affect result.
    val groupedRanks = globalRanks.groupByKey().map { case (uid, iter) =>
      // sort by column index and then convert values to a vector
      Vectors.dense(iter.toSeq.sortBy(_._1).map(_._2).toArray)
    }
    val corrMatrix = PearsonCorrelation.computeCorrelationMatrix(groupedRanks)

    colBased.unpersist(blocking = false)
    sorted.unpersist(blocking = false)

    corrMatrix
  }
}

