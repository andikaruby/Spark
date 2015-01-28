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

package org.apache.spark.rdd.kafka

import org.apache.spark.Partition

/** @param topic kafka topic name
  * @param partition kafka partition id
  * @param fromOffset inclusive starting offset
  * @param untilOffset exclusive ending offset
  * @param host preferred kafka host, i.e. the leader at the time the rdd was created
  * @param port preferred kafka host's port
  */
private[spark]
class KafkaRDDPartition(
  override val index: Int,
  override val topic: String,
  override val partition: Int,
  override val fromOffset: Long,
  override val untilOffset: Long,
  override val host: String,
  override val port: Int
) extends Partition with OffsetRange {
  def toTuple: (Int, String, Int, Long, Long, String, Int) = (
    index,
    topic,
    partition,
    fromOffset,
    untilOffset,
    host,
    port
  )

}

private[spark]
object KafkaRDDPartition {
  def apply(
    index: Int,
    topic: String,
    partition: Int,
    fromOffset: Long,
    untilOffset: Long,
    host: String,
    port: Int
  ): KafkaRDDPartition = new KafkaRDDPartition(
    index,
    topic,
    partition,
    fromOffset,
    untilOffset,
    host,
    port
  )

  def apply(tuple: (Int, String, Int, Long, Long, String, Int)): KafkaRDDPartition = {
    new KafkaRDDPartition(
      tuple._1,
      tuple._2,
      tuple._3,
      tuple._4,
      tuple._5,
      tuple._6,
      tuple._7
    )
  }
}
