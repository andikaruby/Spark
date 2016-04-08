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

import java.{ util => ju }

import org.apache.kafka.clients.consumer.{ ConsumerConfig, ConsumerRecord, KafkaConsumer }
import org.apache.kafka.common.TopicPartition

import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging


/**
 * Consumer of single topicpartition, intended for cached reuse.
 * Underlying consumer is not threadsafe, so neither is this,
 * but processing the same topicpartition and group id in multiple threads would be bad anyway.
 */
private[kafka]
class CachedKafkaConsumer[K, V] private(
  val groupId: String,
  val topic: String,
  val partition: Int,
  val kafkaParams: ju.Map[String, Object]) extends Logging {

  assert(groupId == kafkaParams.get(ConsumerConfig.GROUP_ID_CONFIG),
    "groupId used for cache key must match the groupId in kafkaParams")

  val topicPartition = new TopicPartition(topic, partition)

  protected val consumer = {
    val c = new KafkaConsumer[K, V](kafkaParams)
    val tps = new ju.ArrayList[TopicPartition]()
    tps.add(topicPartition)
    c.assign(tps)
    c
  }

  // TODO if the buffer was kept around as a random-access structure,
  // could possibly optimize re-calculating of an RDD in the same batch
  protected var buffer = ju.Collections.emptyList[ConsumerRecord[K, V]]().iterator
  protected var nextOffset = -2L

  /**
   * Get the record for the given offset, waiting up to timeout ms if IO is necessary.
   * Sequential forward access will use buffers, but random access will be horribly inefficient.
   */
  def get(offset: Long, timeout: Long): ConsumerRecord[K, V] = {
    log.debug(s"get $groupId $topic $partition nextOffset $nextOffset requested $offset")
    if (offset != nextOffset) {
      log.info(s"initial fetch for $groupId $topic $partition $offset")
      buffer = ju.Collections.emptyList[ConsumerRecord[K, V]]().iterator
      seek(offset)
      poll(timeout)
    }

    if (!buffer.hasNext()) { poll(timeout) }
    assert(buffer.hasNext(),
      s"failed to get records for $groupId $topic $partition $offset after polling for $timeout")
    var record = buffer.next()

    if (record.offset != offset) {
      log.info(s"buffer miss for $groupId $topic $partition $offset")
      seek(offset)
      poll(timeout)
      assert(buffer.hasNext(),
        s"failed to get records for $groupId $topic $partition $offset after polling for $timeout")
      record = buffer.next()
      assert(record.offset == offset,
        s"Got wrong record for $groupId $topic $partition even after seeking to offset $offset")
    }

    nextOffset = offset + 1
    record
  }

  private def seek(offset: Long): Unit = {
    log.debug(s"seeking to $topicPartition $offset")
    consumer.seek(topicPartition, offset)
  }

  private def poll(timeout: Long): Unit = {
    val p = consumer.poll(timeout)
    val r = p.records(topicPartition)
    log.debug(s"polled ${p.partitions()}  ${r.size}")
    buffer = r.iterator
  }

}

private[kafka]
object CachedKafkaConsumer extends Logging {

  private case class CacheKey(groupId: String, topic: String, partition: Int)

  // Don't want to depend on guava, don't want a cleanup thread, use a simple LinkedHashMap
  private var cache: ju.LinkedHashMap[CacheKey, CachedKafkaConsumer[_, _]] = null

  /** Must be called before get, once per JVM, to configure the cache. Further calls are ignored */
  def init(
    initialCapacity: Int,
    maxCapacity: Int,
    loadFactor: Float): Unit = CachedKafkaConsumer.synchronized {
    if (null == cache) {
      log.info(s"initializing cache $initialCapacity $maxCapacity $loadFactor")
      cache = new ju.LinkedHashMap[CacheKey, CachedKafkaConsumer[_, _]](
        initialCapacity, loadFactor, true) {
        override def removeEldestEntry(
          entry: ju.Map.Entry[CacheKey, CachedKafkaConsumer[_, _]]): Boolean = {
          if (this.size > maxCapacity) {
            entry.getValue.consumer.close()
            true
          } else {
            false
          }
        }
      }
    }
  }

  /**
   * Get a cached consumer for groupId, assigned to topic and partition.
   * If matching consumer doesn't already exist, will be created using kafkaParams.
   */
  def get[K, V](
    groupId: String,
    topic: String,
    partition: Int,
    kafkaParams: ju.Map[String, Object]): CachedKafkaConsumer[K, V] =
    CachedKafkaConsumer.synchronized {
      val k = CacheKey(groupId, topic, partition)
      val v = cache.get(k)
      if (null == v) {
        log.info(s"cache miss for $k")
        log.debug(cache.keySet.toString)
        val c = new CachedKafkaConsumer[K, V](groupId, topic, partition, kafkaParams)
        cache.put(k, c)
        c
      } else {
        // any given topicpartition should have a consistent key and value type
        v.asInstanceOf[CachedKafkaConsumer[K, V]]
      }
    }

  /** remove consumer for given groupId, topic, and partition, if it exists */
  def remove(groupId: String, topic: String, partition: Int): Unit =
    CachedKafkaConsumer.synchronized {
      val k = CacheKey(groupId, topic, partition)
      log.info(s"removing $k from cache")
      val v = cache.get(k)
      if (null != v) {
        v.consumer.close()
        cache.remove(k)
      }
    }
}
