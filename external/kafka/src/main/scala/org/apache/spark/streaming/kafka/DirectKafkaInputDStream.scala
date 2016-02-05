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

package org.apache.spark.streaming.kafka

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.ClassTag

import kafka.common.TopicAndPartition
import kafka.message.MessageAndMetadata
import kafka.serializer.Decoder

import org.apache.spark.{Logging, SparkException}
import org.apache.spark.streaming.{StreamingContext, Time}
import org.apache.spark.streaming.dstream._

/**
 *  A stream of {@link org.apache.spark.streaming.kafka.KafkaRDD} where
 * each given Kafka topic/partition corresponds to an RDD partition.
 * The spark configuration spark.streaming.kafka.maxRatePerPartition gives the maximum number
 *  of messages
 * per second that each '''partition''' will accept.
 * Starting offsets are specified in advance,
 * and this DStream is not responsible for committing offsets,
 * so that you can control exactly-once semantics.
 * For an easy interface to Kafka-managed offsets,
 *  see {@link org.apache.spark.streaming.kafka.KafkaCluster}
 * @param kafkaParams Kafka <a href="http://kafka.apache.org/documentation.html#configuration">
 * configuration parameters</a>.
 *   Requires "metadata.broker.list" or "bootstrap.servers" to be set with Kafka broker(s),
 *   NOT zookeeper servers, specified in host1:port1,host2:port2 form.
 * @param fromOffsets per-topic/partition Kafka offsets defining the (inclusive)
 *  starting point of the stream
 * @param messageHandler function for translating each message into the desired type
 */
private[streaming]
class DirectKafkaInputDStream[
  K: ClassTag,
  V: ClassTag,
  U <: Decoder[K]: ClassTag,
  T <: Decoder[V]: ClassTag,
  R: ClassTag](
    _ssc: StreamingContext,
    val kafkaParams: Map[String, String],
    val fromOffsets: Map[TopicAndPartition, Long],
    messageHandler: MessageAndMetadata[K, V] => R
  ) extends DirectKafkaInputDStreamBase[K, V, U, T, R] (_ssc, kafkaParams, fromOffsets) {

  // Keep this consistent with how other streams are named (e.g. "Flume polling stream [2]")
  private[streaming] override def name: String = s"Kafka direct stream [$id]"

  protected[streaming] override val checkpointData =
    new DirectKafkaInputDStreamCheckpointData

  protected val kc = new KafkaCluster(kafkaParams)

  @tailrec
  protected final override def latestLeaderOffsets(retries: Int): Map[TopicAndPartition,
    LeaderOffset] = {
    val o = kc.getLatestLeaderOffsets(currentOffsets.keySet)
    // Either.fold would confuse @tailrec, do it manually
    if (o.isLeft) {
      val err = o.left.get.toString
      if (retries <= 0) {
        throw new SparkException(err)
      } else {
        log.error(err)
        Thread.sleep(kc.config.refreshLeaderBackoffMs)
        latestLeaderOffsets(retries - 1)
      }
    } else {
      o.right.get
    }
  }

  override def getRdd(untilOffsets: Map[TopicAndPartition, LeaderOffset]): KafkaRDD[K, V, U, T,
    R] = {
    KafkaRDD[K, V, U, T, R](context.sparkContext, kafkaParams, currentOffsets, untilOffsets,
      messageHandler)
  }

  private[streaming]
  class DirectKafkaInputDStreamCheckpointData extends DStreamCheckpointData(this) {
    def batchForTime: mutable.HashMap[Time, Array[(String, Int, Long, Long, String)]] = {
      data.asInstanceOf[mutable.HashMap[Time, Array[OffsetRange.OffsetRangeTuple]]]
    }

    override def update(time: Time) {
      batchForTime.clear()
      generatedRDDs.foreach { kv =>
        val a = kv._2.asInstanceOf[KafkaRDD[K, V, U, T, R]].offsetRanges.map(_.toTuple).toArray
        batchForTime += kv._1 -> a
      }
    }

    override def cleanup(time: Time) { }

    override def restore() {
      // this is assuming that the topics don't change during execution, which is true currently
      val topics = fromOffsets.keySet
      val leaders = KafkaCluster.checkErrors(kc.findLeaders(topics))

      batchForTime.toSeq.sortBy(_._1)(Time.ordering).foreach { case (t, b) =>
         logInfo(s"Restoring KafkaRDD for time $t ${b.mkString("[", ", ", "]")}")
         generatedRDDs += t -> new KafkaRDD[K, V, U, T, R](
           context.sparkContext, kafkaParams, b.map(OffsetRange(_)), leaders, messageHandler)
      }
    }
  }

}
