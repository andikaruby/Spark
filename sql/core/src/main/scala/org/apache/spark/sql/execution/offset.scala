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

import org.apache.spark.rdd.RDD
import org.apache.spark.serializer.Serializer
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Attribute, SortOrder}
import org.apache.spark.sql.catalyst.plans.physical.{Partitioning, SinglePartition}


/**
 * Skip the first `offset` elements and collect them to a single partition.
 * This operator will be used when a logical `Offset` operation is the final operator in an
 * logical plan, which happens when the user is collecting results back to the driver.
 */
case class CollectOffsetExec(offset: Int, child: SparkPlan) extends UnaryExecNode {

  override def output: Seq[Attribute] = child.output

  override def outputPartitioning: Partitioning = SinglePartition

  override def outputOrdering: Seq[SortOrder] = child.outputOrdering

  override def executeCollect(): Array[InternalRow] = child.executeCollect.drop(offset)

  private val serializer: Serializer = new UnsafeRowSerializer(child.output.size)

  protected override def doExecute(): RDD[InternalRow] = {
    sparkContext.parallelize(executeCollect(), 1)
  }

}

/**
 * Skip the first `offset` elements and collect them to a single partition.
 */
case class OffsetExec(offset: Int, child: SparkPlan) extends UnaryExecNode {

  override def output: Seq[Attribute] = child.output

  override def outputOrdering: Seq[SortOrder] = child.outputOrdering

  protected override def doExecute(): RDD[InternalRow] = {
    val rdd = child.execute()
    val arr = rdd.take(offset)
    rdd.filter(!arr.contains(_))
  }

}

