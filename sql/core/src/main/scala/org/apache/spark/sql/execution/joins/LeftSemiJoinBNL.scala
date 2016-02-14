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

package org.apache.spark.sql.execution.joins

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.physical._
import org.apache.spark.sql.execution._
import org.apache.spark.sql.execution.metric.SQLMetrics

/**
 * Using BroadcastNestedLoopJoin to calculate left semi join result when there's no join keys
 * for hash join.
 */
case class LeftSemiJoinBNL(
    left: SparkPlan, right: SparkPlan, condition: Option[Expression])
  extends BinaryNode {
  // TODO: Override requiredChildDistribution.

  override private[sql] lazy val metrics = Map(
    "numOutputRows" -> SQLMetrics.createLongMetric(sparkContext, "number of output rows"))

  override def requiredChildDistribution: Seq[Distribution] = {
    UnspecifiedDistribution :: BroadcastDistribution(IdentityBroadcastMode) :: Nil
  }

  override def outputPartitioning: Partitioning = left.outputPartitioning

  override def output: Seq[Attribute] = left.output

  @transient private lazy val boundCondition =
    newPredicate(condition.getOrElse(Literal(true)), left.output ++ right.output)

  protected override def doExecute(): RDD[InternalRow] = {
    val numOutputRows = longMetric("numOutputRows")

    val broadcastedRelation = right.executeBroadcast[Array[InternalRow]]()

    left.execute().mapPartitions { streamedIter =>
      val joinedRow = new JoinedRow
      val relation = broadcastedRelation.value

      streamedIter.filter(streamedRow => {
        var i = 0
        var matched = false

        while (i < relation.length && !matched) {
          if (boundCondition(joinedRow(streamedRow, relation(i)))) {
            matched = true
          }
          i += 1
        }
        if (matched) {
          numOutputRows += 1
        }
        matched
      })
    }
  }
}
