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

package org.apache.spark.sql.execution.aggregate

import scala.concurrent.duration.NANOSECONDS

import org.apache.spark.{PartitionEvaluator, PartitionEvaluatorFactory}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Attribute, MutableProjection, NamedExpression, UnsafeRow}
import org.apache.spark.sql.catalyst.expressions.aggregate.AggregateExpression
import org.apache.spark.sql.execution.metric.SQLMetric

class ObjectHashAggregateEvaluatorFactory(
    groupingExpressions: Seq[NamedExpression],
    aggregateExpressions: Seq[AggregateExpression],
    aggregateAttributes: Seq[Attribute],
    initialInputBufferOffset: Int,
    resultExpressions: Seq[NamedExpression],
    output: Seq[Attribute],
    inputAttributes: Seq[Attribute],
    numOutputRows: SQLMetric,
    aggTime: SQLMetric,
    spillSize: SQLMetric,
    numTasksFallBacked: SQLMetric,
    fallbackCountThreshold: Int) extends PartitionEvaluatorFactory[InternalRow, InternalRow] {
  override def createEvaluator(): PartitionEvaluator[InternalRow, InternalRow] = {
    new ObjectHashAggregateEvaluator
  }

  private class ObjectHashAggregateEvaluator extends PartitionEvaluator[InternalRow, InternalRow] {
    override def eval(partIndex: Int, inputs: Iterator[InternalRow]*): Iterator[InternalRow]
    = {
      val iter = inputs.head
      val beforeAgg = System.nanoTime()
      val hasInput = iter.hasNext
      val res = if (!hasInput && groupingExpressions.nonEmpty) {
        // This is a grouped aggregate and the input kvIterator is empty,
        // so return an empty kvIterator.
        Iterator.empty
      } else {
        val aggregationIterator =
          new ObjectAggregationIterator(
            partIndex,
            output,
            groupingExpressions,
            aggregateExpressions,
            aggregateAttributes,
            initialInputBufferOffset,
            resultExpressions,
            (expressions, inputSchema) =>
              MutableProjection.create(expressions, inputSchema),
            inputAttributes,
            iter,
            fallbackCountThreshold,
            numOutputRows,
            spillSize,
            numTasksFallBacked)
        if (!hasInput && groupingExpressions.isEmpty) {
          numOutputRows += 1
          Iterator.single[UnsafeRow](aggregationIterator.outputForEmptyGroupingKeyWithoutInput())
        } else {
          aggregationIterator
        }
      }
      aggTime += NANOSECONDS.toMillis(System.nanoTime() - beforeAgg)
      res
    }
  }
}
