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

package org.apache.spark.sql.execution.window

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Ascending, Attribute, BindReferences, DenseRank, Expression, Rank, RowNumber, SortOrder, UnsafeProjection, UnsafeRow}
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, CodeGenerator, ExprCode, ExpressionCanonicalizer, GenerateOrdering}
import org.apache.spark.sql.catalyst.plans.physical.{AllTuples, ClusteredDistribution, Distribution, Partitioning}
import org.apache.spark.sql.execution.{CodegenSupport, SparkPlan, UnaryExecNode}
import org.apache.spark.sql.execution.metric.{SQLMetric, SQLMetrics}
import org.apache.spark.sql.types.{NumericType, StringType}

sealed trait WindowGroupLimitMode

case object Partial extends WindowGroupLimitMode

case object Final extends WindowGroupLimitMode

/**
 * This operator is designed to filter out unnecessary rows before WindowExec
 * for top-k computation.
 * @param partitionSpec Should be the same as [[WindowExec#partitionSpec]].
 * @param orderSpec Should be the same as [[WindowExec#orderSpec]].
 * @param rankLikeFunction The function to compute row rank, should be RowNumber/Rank/DenseRank.
 * @param limit The limit for rank value.
 * @param mode The mode describes [[WindowGroupLimitExec]] before or after shuffle.
 * @param child The child spark plan.
 */
case class WindowGroupLimitExec(
    partitionSpec: Seq[Expression],
    orderSpec: Seq[SortOrder],
    rankLikeFunction: Expression,
    limit: Int,
    mode: WindowGroupLimitMode,
    child: SparkPlan) extends UnaryExecNode with CodegenSupport {

  override def output: Seq[Attribute] = child.output

  override def requiredChildDistribution: Seq[Distribution] = mode match {
    case Partial => super.requiredChildDistribution
    case Final =>
      if (partitionSpec.isEmpty) {
        AllTuples :: Nil
      } else {
        ClusteredDistribution(partitionSpec) :: Nil
      }
  }

  override def requiredChildOrdering: Seq[Seq[SortOrder]] =
    Seq(partitionSpec.map(SortOrder(_, Ascending)) ++ orderSpec)

  override def outputOrdering: Seq[SortOrder] = child.outputOrdering

  override def outputPartitioning: Partitioning = child.outputPartitioning

  override lazy val metrics = Map(
    "numOutputRows" -> SQLMetrics.createMetric(sparkContext, "number of output rows"))

  protected override def doExecute(): RDD[InternalRow] = {
    val numOutputRows = longMetric("numOutputRows")
    rankLikeFunction match {
      case _: RowNumber if partitionSpec.isEmpty =>
        child.execute().mapPartitionsInternal(SimpleLimitIterator(_, limit, numOutputRows))
      case _: RowNumber =>
        child.execute().mapPartitionsInternal(new GroupedLimitIterator(_, output, partitionSpec,
          (input: Iterator[InternalRow]) => SimpleLimitIterator(input, limit, numOutputRows)))
      case _: Rank if partitionSpec.isEmpty =>
        child.execute().mapPartitionsInternal(
          RankLimitIterator(output, _, orderSpec, limit, numOutputRows))
      case _: Rank =>
        child.execute().mapPartitionsInternal(new GroupedLimitIterator(_, output, partitionSpec,
          (input: Iterator[InternalRow]) =>
            RankLimitIterator(output, input, orderSpec, limit, numOutputRows)))
      case _: DenseRank if partitionSpec.isEmpty =>
        child.execute().mapPartitionsInternal(
          DenseRankLimitIterator(output, _, orderSpec, limit, numOutputRows))
      case _: DenseRank =>
        child.execute().mapPartitionsInternal(new GroupedLimitIterator(_, output, partitionSpec,
          (input: Iterator[InternalRow]) =>
            DenseRankLimitIterator(output, input, orderSpec, limit, numOutputRows)))
    }
  }

  override def supportCodegen: Boolean = rankLikeFunction.isInstanceOf[RowNumber]

  override def inputRDDs(): Seq[RDD[InternalRow]] = {
    child.asInstanceOf[CodegenSupport].inputRDDs()
  }

  protected lazy val countTerm = WindowGroupLimitExec.newCountTerm()
  private var groupMapVariable: String = _

  protected override def doProduce(ctx: CodegenContext): String = {
    ctx.addMutableState(CodeGenerator.JAVA_INT, countTerm, forceInline = true, useFreshName = false)

    if (partitionSpec.nonEmpty) {
      groupMapVariable = ctx.addMutableState("java.util.Map<java.lang.Object, java.lang.Integer>",
        "groupMapVariable",
        v => s"$v = new java.util.HashMap<java.lang.Object, java.lang.Integer>();",
        forceInline = true)
    }

    child.asInstanceOf[CodegenSupport].produce(ctx, this)
  }

  override def doConsume(ctx: CodegenContext, input: Seq[ExprCode], row: ExprCode): String =
    rankLikeFunction match {
      case _: RowNumber if partitionSpec.isEmpty =>
        s"""
           | if ($countTerm < $limit) {
           |   $countTerm += 1;
           |   ${consume(ctx, input)}
           | }
         """.stripMargin
      case _: RowNumber =>
        val partitionExprs = BindReferences.bindReferences[Expression](partitionSpec, output)
        val partitionEvs = partitionExprs.map(ExpressionCanonicalizer.execute(_).genCode(ctx))
        val partitionValues = partitionEvs.zip(partitionSpec.map(_.dataType)).map {
          case (ev, StringType) => s"${ev.value}.toString()"
          case (ev, _: NumericType) => s"String.valueOf(${ev.value})"
          case (ev, _) => s"${ev.value}"
        }
        val group = ctx.freshName("group")
        val rankValue = ctx.freshName("rankValue")
        s"""
           | int $rankValue = 0;
           | String $group = ${partitionValues.mkString(""" + "," + """)};
           | if ($groupMapVariable.containsKey($group)) {
           |   $rankValue = (Integer) $groupMapVariable.get($group);
           | }
           | if ($rankValue < $limit) {
           |   ${consume(ctx, input)}
           | }
           | $rankValue += 1;
           | $groupMapVariable.put($group, $rankValue);
         """.stripMargin
    }

  override protected def withNewChildInternal(newChild: SparkPlan): WindowGroupLimitExec =
    copy(child = newChild)
}

object WindowGroupLimitExec {
  private val curId = new java.util.concurrent.atomic.AtomicInteger()

  def newCountTerm(): String = {
    val id = curId.getAndIncrement()
    s"_window_limit_counter_$id"
  }
}

abstract class BaseLimitIterator extends Iterator[InternalRow] {
  def numOutputRows: SQLMetric

  def input: Iterator[InternalRow]

  def limit: Int

  var rank = 0

  var nextRow: UnsafeRow = null

  // Increase the rank value.
  def increaseRank(): Unit

  override def hasNext: Boolean = rank < limit && input.hasNext

  override def next(): InternalRow = {
    if (!hasNext) throw new NoSuchElementException
    nextRow = input.next().asInstanceOf[UnsafeRow]
    increaseRank()
    numOutputRows += 1
    nextRow
  }

  def reset(): Unit
}

case class SimpleLimitIterator(
    input: Iterator[InternalRow],
    limit: Int,
    numOutputRows: SQLMetric) extends BaseLimitIterator {

  override def increaseRank(): Unit = {
    rank += 1
  }

  override def reset(): Unit = {
    rank = 0
  }
}

trait OrderSpecProvider {
  def output: Seq[Attribute]
  def orderSpec: Seq[SortOrder]

  val ordering = GenerateOrdering.generate(orderSpec, output)
  var currentRankRow: UnsafeRow = null
}

case class RankLimitIterator(
    output: Seq[Attribute],
    input: Iterator[InternalRow],
    orderSpec: Seq[SortOrder],
    limit: Int,
    numOutputRows: SQLMetric) extends BaseLimitIterator with OrderSpecProvider {

  var count = 0

  override def increaseRank(): Unit = {
    if (count == 0) {
      currentRankRow = nextRow.copy()
    } else {
      if (ordering.compare(currentRankRow, nextRow) != 0) {
        rank = count
        currentRankRow = nextRow.copy()
      }
    }
    count += 1
  }

  override def reset(): Unit = {
    rank = 0
    count = 0
  }
}

case class DenseRankLimitIterator(
    output: Seq[Attribute],
    input: Iterator[InternalRow],
    orderSpec: Seq[SortOrder],
    limit: Int,
    numOutputRows: SQLMetric) extends BaseLimitIterator with OrderSpecProvider {

  override def increaseRank(): Unit = {
    if (currentRankRow == null) {
      currentRankRow = nextRow.copy()
    } else {
      if (ordering.compare(currentRankRow, nextRow) != 0) {
        rank += 1
        currentRankRow = nextRow.copy()
      }
    }
  }

  override def reset(): Unit = {
    rank = 0
  }
}

class GroupedLimitIterator(
    input: Iterator[InternalRow],
    output: Seq[Attribute],
    partitionSpec: Seq[Expression],
    createLimitIterator: Iterator[InternalRow] => BaseLimitIterator)
  extends Iterator[InternalRow] {

  val grouping = UnsafeProjection.create(partitionSpec, output)

  // Manage the stream and the grouping.
  var nextRow: UnsafeRow = null
  var nextGroup: UnsafeRow = null
  var nextRowAvailable: Boolean = false
  protected[this] def fetchNextRow(): Unit = {
    nextRowAvailable = input.hasNext
    if (nextRowAvailable) {
      nextRow = input.next().asInstanceOf[UnsafeRow]
      nextGroup = grouping(nextRow)
    }
  }
  fetchNextRow()

  var groupIterator: GroupIterator = _
  var limitIterator: BaseLimitIterator = _
  if (nextRowAvailable) {
    groupIterator = new GroupIterator()
    limitIterator = createLimitIterator(groupIterator)
  }

  override final def hasNext: Boolean = nextRowAvailable && {
    if (!limitIterator.hasNext) {
      // if `limitIterator.hasNext` is false, we should jump to the next group if present
      groupIterator.skipRemainingRows()
      limitIterator.reset()
    }
    limitIterator.hasNext
  }

  override final def next(): InternalRow = {
    if (!hasNext) throw new NoSuchElementException
    limitIterator.next()
  }

  class GroupIterator() extends Iterator[InternalRow] {
    // Before we start to fetch new input rows, make a copy of nextGroup.
    var currentGroup = nextGroup.copy()

    def hasNext: Boolean = nextRowAvailable && nextGroup == currentGroup

    def next(): InternalRow = {
      if (!hasNext) throw new NoSuchElementException
      val currentRow = nextRow.copy()
      fetchNextRow()
      currentRow
    }

    def skipRemainingRows(): Unit = {
      // Skip all the remaining rows in this group
      while (hasNext) {
        fetchNextRow()
      }

      // Switch to next group
      if (nextRowAvailable) currentGroup = nextGroup.copy()
    }
  }
}
