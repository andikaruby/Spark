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

package org.apache.spark.sql.execution

import org.apache.spark.{SparkEnv, TaskContext}
import org.apache.spark.executor.TaskMetrics
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, ExprCode, GenerateUnsafeProjection}
import org.apache.spark.sql.catalyst.plans.physical.{Distribution, OrderedDistribution, UnspecifiedDistribution}
import org.apache.spark.sql.execution.metric.SQLMetrics

/**
 * Performs (external) sorting.
 *
 * @param global when true performs a global sort of all partitions by shuffling the data first
 *               if necessary.
 * @param testSpillFrequency Method for configuring periodic spilling in unit tests. If set, will
 *                           spill every `frequency` records.
 */
case class Sort(
    sortOrder: Seq[SortOrder],
    global: Boolean,
    child: SparkPlan,
    testSpillFrequency: Int = 0)
  extends UnaryNode with CodegenSupport {

  override def output: Seq[Attribute] = child.output

  override def outputOrdering: Seq[SortOrder] = sortOrder

  override def requiredChildDistribution: Seq[Distribution] =
    if (global) OrderedDistribution(sortOrder) :: Nil else UnspecifiedDistribution :: Nil

  override private[sql] lazy val metrics = Map(
    "dataSize" -> SQLMetrics.createSizeMetric(sparkContext, "data size"),
    "spillSize" -> SQLMetrics.createSizeMetric(sparkContext, "spill size"))

  def createSorter(): UnsafeExternalRowSorter = {
    val ordering = newOrdering(sortOrder, output)

    // The comparator for comparing prefix
    val boundSortExpression = BindReferences.bindReference(sortOrder.head, output)
    val prefixComparator = SortPrefixUtils.getPrefixComparator(boundSortExpression)

    // The generator for prefix
    val prefixProjection = UnsafeProjection.create(Seq(SortPrefix(boundSortExpression)))
    val prefixComputer = new UnsafeExternalRowSorter.PrefixComputer {
      override def computePrefix(row: InternalRow): Long = {
        prefixProjection.apply(row).getLong(0)
      }
    }

    val pageSize = SparkEnv.get.memoryManager.pageSizeBytes
    val sorter = new UnsafeExternalRowSorter(
      schema, ordering, prefixComparator, prefixComputer, pageSize)
    if (testSpillFrequency > 0) {
      sorter.setTestSpillFrequency(testSpillFrequency)
    }
    sorter
  }

  protected override def doExecute(): RDD[InternalRow] = {
    val dataSize = longMetric("dataSize")
    val spillSize = longMetric("spillSize")

    child.execute().mapPartitionsInternal { iter =>
      val sorter = createSorter()

      val metrics = TaskContext.get().taskMetrics()
      // Remember spill data size of this task before execute this operator so that we can
      // figure out how many bytes we spilled for this operator.
      val spillSizeBefore = metrics.memoryBytesSpilled

      val sortedIterator = sorter.sort(iter.asInstanceOf[Iterator[UnsafeRow]])

      dataSize += sorter.getPeakMemoryUsage
      spillSize += metrics.memoryBytesSpilled - spillSizeBefore
      metrics.incPeakExecutionMemory(sorter.getPeakMemoryUsage)

      sortedIterator
    }
  }

  override def upstreams(): Seq[RDD[InternalRow]] = {
    child.asInstanceOf[CodegenSupport].upstreams()
  }

  // Name of sorter variable used in codegen.
  private var sorterVariable: String = _

  override protected def doProduce(ctx: CodegenContext): String = {
    val needToSort = ctx.freshName("needToSort")
    ctx.addMutableState("boolean", needToSort, s"$needToSort = true;")


    // Initialize the class member variables. This includes the instance of the Sorter and
    // the iterator to return sorted rows.
    val thisPlan = ctx.addReferenceObj("plan", this)
    sorterVariable = ctx.freshName("sorter")
    ctx.addMutableState(classOf[UnsafeExternalRowSorter].getName, sorterVariable,
      s"$sorterVariable = $thisPlan.createSorter();")
    val metrics = ctx.freshName("metrics")
    ctx.addMutableState(classOf[TaskMetrics].getName, metrics,
      s"$metrics = org.apache.spark.TaskContext.get().taskMetrics();")
    val sortedIterator = ctx.freshName("sortedIter")
    ctx.addMutableState("scala.collection.Iterator<UnsafeRow>", sortedIterator, "")

    val addToSorter = ctx.freshName("addToSorter")
    ctx.addNewFunction(addToSorter,
      s"""
        | private void $addToSorter() throws java.io.IOException {
        |   ${child.asInstanceOf[CodegenSupport].produce(ctx, this)}
        | }
      """.stripMargin.trim)

    val outputRow = ctx.freshName("outputRow")
    val dataSize = ctx.freshName("dataSize")
    ctx.addMutableState(classOf[Long].getName, dataSize, "")
    val spillSize = ctx.freshName("spillSize")
    ctx.addMutableState(classOf[Long].getName, spillSize, "")
    val spillSizeBefore = ctx.freshName("spillSizeBefore")
    ctx.addMutableState(classOf[Long].getName, spillSizeBefore,
      s"$spillSizeBefore = $metrics.memoryBytesSpilled();")
    s"""
       | if ($needToSort) {
       |   $addToSorter();
       |   $sortedIterator = $sorterVariable.sort();
       |   $dataSize += $sorterVariable.getPeakMemoryUsage();
       |   $spillSize += $metrics.memoryBytesSpilled() - $spillSizeBefore;
       |   $metrics.incPeakExecutionMemory($sorterVariable.getPeakMemoryUsage());
       |   $needToSort = false;
       | }
       |
       | while ($sortedIterator.hasNext()) {
       |   UnsafeRow $outputRow = (UnsafeRow)$sortedIterator.next();
       |   ${consume(ctx, null, outputRow)}
       | }
     """.stripMargin.trim
  }

  override def doConsume(ctx: CodegenContext, input: Seq[ExprCode]): String = {
    val colExprs = child.output.zipWithIndex.map { case (attr, i) =>
      BoundReference(i, attr.dataType, attr.nullable)
    }

    ctx.currentVars = input
    val code = GenerateUnsafeProjection.createCode(ctx, colExprs, useSubexprElimination = false)

    s"""
       | // Convert the input attributes to an UnsafeRow and add it to the sorter
       | ${code.code}
       | $sorterVariable.insertRow(${code.value});
     """.stripMargin.trim
  }
}
