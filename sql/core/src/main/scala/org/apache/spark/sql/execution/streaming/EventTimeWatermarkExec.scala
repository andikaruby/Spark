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

package org.apache.spark.sql.execution.streaming

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Attribute, UnsafeProjection}
import org.apache.spark.sql.catalyst.plans.logical.EventTimeWatermark
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.types.MetadataBuilder
import org.apache.spark.util.AccumulatorV2

class MaxLong(protected var currentValue: Long = 0)
  extends AccumulatorV2[Long, Long]
  with Serializable {

  override def isZero: Boolean = value == 0
  override def value: Long = currentValue
  override def copy(): AccumulatorV2[Long, Long] = new MaxLong(currentValue)

  override def reset(): Unit = {
    currentValue = 0
  }

  override def add(v: Long): Unit = {
    if (value < v) { currentValue = v }
  }

  override def merge(other: AccumulatorV2[Long, Long]): Unit = {
    if (currentValue < other.value) {
      currentValue = other.value
    }
  }
}

case class EventTimeWatermarkExec(
    eventTime: Attribute,
    delayMs: Long,
    child: SparkPlan) extends SparkPlan {

  // TODO: Hide internal metrics?
  val maxEventTime = new MaxLong
  sparkContext.register(maxEventTime)

  override protected def doExecute(): RDD[InternalRow] = {
    child.execute().mapPartitions { iter =>
      val getEventTime = UnsafeProjection.create(eventTime :: Nil, child.output)
      iter.map { row =>
        maxEventTime.add(getEventTime(row).getLong(0))
        row
      }
    }
  }

  // Update the metadata on the eventTime column to include the desired delay.
  // TODO: Duplicated?
  override val output: Seq[Attribute] = child.output.map { a =>
    if (a semanticEquals eventTime) {
      val updatedMetadata = new MetadataBuilder()
          .withMetadata(a.metadata)
          .putLong(EventTimeWatermark.delayKey, delayMs)
          .build()

      a.withMetadata(updatedMetadata)
    } else {
      a
    }
  }

  override def children: Seq[SparkPlan] = child :: Nil
}