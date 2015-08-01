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

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.physical.Partitioning
import org.apache.spark.sql.catalyst.plans.{FullOuter, JoinType, LeftOuter, RightOuter}
import org.apache.spark.sql.execution.{BinaryNode, SparkPlan}
import org.apache.spark.util.collection.CompactBuffer

/**
 * :: DeveloperApi ::
 */
@DeveloperApi
case class BroadcastNestedLoopJoin(
    left: SparkPlan,
    right: SparkPlan,
    buildSide: BuildSide,
    joinType: JoinType,
    condition: Option[Expression]) extends BinaryNode {
  // TODO: Override requiredChildDistribution.

  /** BuildRight means the right relation <=> the broadcast relation. */
  private val (streamed, broadcast) = buildSide match {
    case BuildRight => (left, right)
    case BuildLeft => (right, left)
  }

  override def outputsUnsafeRows: Boolean = left.outputsUnsafeRows || right.outputsUnsafeRows
  override def canProcessUnsafeRows: Boolean = true

  @transient private[this] lazy val resultProjection: InternalRow => InternalRow = {
    if (outputsUnsafeRows) {
      UnsafeProjection.create(schema)
    } else {
      identity[InternalRow]
    }
  }

  override def outputPartitioning: Partitioning = streamed.outputPartitioning

  override def output: Seq[Attribute] = {
    joinType match {
      case LeftOuter =>
        left.output ++ right.output.map(_.withNullability(true))
      case RightOuter =>
        left.output.map(_.withNullability(true)) ++ right.output
      case FullOuter =>
        left.output.map(_.withNullability(true)) ++ right.output.map(_.withNullability(true))
      case _ =>
        left.output ++ right.output
    }
  }

  @transient private lazy val boundCondition =
    newPredicate(condition.getOrElse(Literal(true)), left.output ++ right.output)

  protected override def doExecute(): RDD[InternalRow] = {
    val broadcastedRelation =
      sparkContext.broadcast(broadcast.execute().map(_.copy())
        .collect().toIndexedSeq)

    /** All rows that either match both-way, or rows from streamed joined with nulls. */
    val matchesOrStreamedRowsWithNulls = streamed.execute().mapPartitions { streamedIter =>
      val matchedRows = new CompactBuffer[InternalRow]
      // TODO: Use Spark's BitSet.
      val includedBroadcastTuples =
        new scala.collection.mutable.BitSet(broadcastedRelation.value.size)
      val joinedRow = new JoinedRow

      val leftNulls = new GenericMutableRow(left.output.size)
      val rightNulls = new GenericMutableRow(right.output.size)

      streamedIter.foreach { streamedRow =>
        var i = 0
        var streamRowMatched = false

        while (i < broadcastedRelation.value.size) {
          val broadcastedRow = broadcastedRelation.value(i)
          buildSide match {
            case BuildRight if boundCondition(joinedRow(streamedRow, broadcastedRow)) =>
              matchedRows += resultProjection(joinedRow(streamedRow, broadcastedRow)).copy()
              streamRowMatched = true
              includedBroadcastTuples += i
            case BuildLeft if boundCondition(joinedRow(broadcastedRow, streamedRow)) =>
              matchedRows += resultProjection(joinedRow(broadcastedRow, streamedRow)).copy()
              streamRowMatched = true
              includedBroadcastTuples += i
            case _ =>
          }
          i += 1
        }

        (streamRowMatched, joinType, buildSide) match {
          case (false, LeftOuter | FullOuter, BuildRight) =>
            matchedRows += resultProjection(joinedRow(streamedRow, rightNulls)).copy()
          case (false, RightOuter | FullOuter, BuildLeft) =>
            matchedRows += resultProjection(joinedRow(leftNulls, streamedRow)).copy()
          case _ =>
        }
      }
      Iterator((matchedRows, includedBroadcastTuples))
    }

    val includedBroadcastTuples = matchesOrStreamedRowsWithNulls.map(_._2)
    val allIncludedBroadcastTuples = includedBroadcastTuples.fold(
      new scala.collection.mutable.BitSet(broadcastedRelation.value.size)
    )(_ ++ _)

    val leftNulls = new GenericMutableRow(left.output.size)
    val rightNulls = new GenericMutableRow(right.output.size)
    /** Rows from broadcasted joined with nulls. */
    val broadcastRowsWithNulls: Seq[InternalRow] = {
      val buf: CompactBuffer[InternalRow] = new CompactBuffer()
      var i = 0
      val rel = broadcastedRelation.value
      (joinType, buildSide) match {
        case (RightOuter | FullOuter, BuildRight) =>
          val joinedRow = new JoinedRow
          joinedRow.withLeft(leftNulls)
          while (i < rel.length) {
            if (!allIncludedBroadcastTuples.contains(i)) {
              buf += resultProjection(joinedRow.withRight(rel(i))).copy()
            }
            i += 1
          }
        case (LeftOuter | FullOuter, BuildLeft) =>
          val joinedRow = new JoinedRow
          joinedRow.withRight(rightNulls)
          while (i < rel.length) {
            if (!allIncludedBroadcastTuples.contains(i)) {
              buf += resultProjection(joinedRow.withLeft(rel(i))).copy()
            }
            i += 1
          }
        case _ =>
      }
      buf.toSeq
    }

    // TODO: Breaks lineage.
    sparkContext.union(
      matchesOrStreamedRowsWithNulls.flatMap(_._1), sparkContext.makeRDD(broadcastRowsWithNulls))
  }
}
