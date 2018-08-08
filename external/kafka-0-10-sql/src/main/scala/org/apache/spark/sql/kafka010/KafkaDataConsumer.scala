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

package org.apache.spark.sql.kafka010

import java.{util => ju}
import java.util.concurrent.TimeoutException

import scala.collection.JavaConverters._

import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer, OffsetOutOfRangeException}
import org.apache.kafka.common.TopicPartition

import org.apache.spark.{SparkEnv, SparkException, TaskContext}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.kafka010.KafkaDataConsumer.AvailableOffsetRange
import org.apache.spark.sql.kafka010.KafkaSourceProvider._
import org.apache.spark.util.UninterruptibleThread

/**
 * An exception to indicate there is a missing offset in the records returned by Kafka consumer.
 * This means it's either a transaction (commit or abort) marker, or an aborted message if
 * "isolation.level" is "read_committed". The offsets in the range [offset, nextOffsetToFetch) are
 * missing. In order words, `nextOffsetToFetch` indicates the next offset to fetch.
 */
private[kafka010] class MissingOffsetException(
    val offset: Long,
    val nextOffsetToFetch: Long) extends Exception(
  s"Offset $offset is missing. The next offset to fetch is: $nextOffsetToFetch")

private[kafka010] sealed trait KafkaDataConsumer {
  /**
   * Get the record for the given offset if available. Otherwise it will either throw error
   * (if failOnDataLoss = true), or return the next available offset within [offset, untilOffset),
   * or null.
   *
   * @param offset         the offset to fetch.
   * @param untilOffset    the max offset to fetch. Exclusive.
   * @param pollTimeoutMs  timeout in milliseconds to poll data from Kafka.
   * @param failOnDataLoss When `failOnDataLoss` is `true`, this method will either return record at
   *                       offset if available, or throw exception.when `failOnDataLoss` is `false`,
   *                       this method will either return record at offset if available, or return
   *                       the next earliest available record less than untilOffset, or null. It
   *                       will not throw any exception.
   */
  def get(
      offset: Long,
      untilOffset: Long,
      pollTimeoutMs: Long,
      failOnDataLoss: Boolean): ConsumerRecord[Array[Byte], Array[Byte]] = {
    internalConsumer.get(offset, untilOffset, pollTimeoutMs, failOnDataLoss)
  }

  /**
   * Return the available offset range of the current partition. It's a pair of the earliest offset
   * and the latest offset.
   */
  def getAvailableOffsetRange(): AvailableOffsetRange = internalConsumer.getAvailableOffsetRange()

  /**
   * Release this consumer from being further used. Depending on its implementation,
   * this consumer will be either finalized, or reset for reuse later.
   */
  def release(): Unit

  /** Reference to the internal implementation that this wrapper delegates to */
  protected def internalConsumer: InternalKafkaConsumer
}


/**
 * A wrapper around Kafka's KafkaConsumer that throws error when data loss is detected.
 * This is not for direct use outside this file.
 */
private[kafka010] case class InternalKafkaConsumer(
    topicPartition: TopicPartition,
    kafkaParams: ju.Map[String, Object]) extends Logging {
  import InternalKafkaConsumer._

  private val groupId = kafkaParams.get(ConsumerConfig.GROUP_ID_CONFIG).asInstanceOf[String]

  @volatile private var consumer = createConsumer

  /** indicates whether this consumer is in use or not */
  @volatile var inUse = true

  /** indicate whether this consumer is going to be stopped in the next release */
  @volatile var markedForClose = false

  /** Iterator to the already fetch data */
  @volatile private var fetchedData =
    ju.Collections.emptyIterator[ConsumerRecord[Array[Byte], Array[Byte]]]
  @volatile private var nextOffsetInFetchedData = UNKNOWN_OFFSET

  @volatile private var offsetBeforePoll: Long = UNKNOWN_OFFSET

  @volatile private var offsetAfterPoll: Long = UNKNOWN_OFFSET

  /** Create a KafkaConsumer to fetch records for `topicPartition` */
  private def createConsumer: KafkaConsumer[Array[Byte], Array[Byte]] = {
    val c = new KafkaConsumer[Array[Byte], Array[Byte]](kafkaParams)
    val tps = new ju.ArrayList[TopicPartition]()
    tps.add(topicPartition)
    c.assign(tps)
    c
  }

  private def runUninterruptiblyIfPossible[T](body: => T): T = Thread.currentThread match {
    case ut: UninterruptibleThread =>
      ut.runUninterruptibly(body)
    case _ =>
      logWarning("CachedKafkaConsumer is not running in UninterruptibleThread. " +
        "It may hang when CachedKafkaConsumer's methods are interrupted because of KAFKA-1894")
      body
  }

  /**
   * Return the available offset range of the current partition. It's a pair of the earliest offset
   * and the latest offset.
   */
  def getAvailableOffsetRange(): AvailableOffsetRange = runUninterruptiblyIfPossible {
    consumer.seekToBeginning(Set(topicPartition).asJava)
    val earliestOffset = consumer.position(topicPartition)
    consumer.seekToEnd(Set(topicPartition).asJava)
    val latestOffset = consumer.position(topicPartition)
    AvailableOffsetRange(earliestOffset, latestOffset)
  }

  /**
   * Get the record for the given offset if available. Otherwise it will either throw error
   * (if failOnDataLoss = true), or return the next available offset within [offset, untilOffset),
   * or null.
   *
   * @param offset the offset to fetch.
   * @param untilOffset the max offset to fetch. Exclusive.
   * @param pollTimeoutMs timeout in milliseconds to poll data from Kafka.
   * @param failOnDataLoss When `failOnDataLoss` is `true`, this method will either return record at
   *                       offset if available, or throw exception.when `failOnDataLoss` is `false`,
   *                       this method will either return record at offset if available, or return
   *                       the next earliest available record less than untilOffset, or null. It
   *                       will not throw any exception.
   */
  def get(
      offset: Long,
      untilOffset: Long,
      pollTimeoutMs: Long,
      failOnDataLoss: Boolean):
    ConsumerRecord[Array[Byte], Array[Byte]] = runUninterruptiblyIfPossible {
    require(offset < untilOffset,
      s"offset must always be less than untilOffset [offset: $offset, untilOffset: $untilOffset]")
    logDebug(s"Get $groupId $topicPartition nextOffset $nextOffsetInFetchedData requested $offset")
    // The following loop is basically for `failOnDataLoss = false`. When `failOnDataLoss` is
    // `false`, first, we will try to fetch the record at `offset`. If no such record exists, then
    // we will move to the next available offset within `[offset, untilOffset)` and retry.
    // If `failOnDataLoss` is `true`, the loop body will be executed only once.
    var toFetchOffset = offset
    var consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]] = null
    // We want to break out of the while loop on a successful fetch to avoid using "return"
    // which may causes a NonLocalReturnControl exception when this method is used as a function.
    var isFetchComplete = false

    while (toFetchOffset != UNKNOWN_OFFSET && !isFetchComplete) {
      try {
        consumerRecord = fetchData(toFetchOffset, untilOffset, pollTimeoutMs, failOnDataLoss)
        isFetchComplete = true
      } catch {
        case e: OffsetOutOfRangeException =>
          // When there is some error thrown, it's better to use a new consumer to drop all cached
          // states in the old consumer. We don't need to worry about the performance because this
          // is not a common path.
          resetConsumer()
          reportDataLoss(failOnDataLoss, s"Cannot fetch offset $toFetchOffset", e)
          toFetchOffset = getEarliestAvailableOffsetBetween(toFetchOffset, untilOffset)
        case e: MissingOffsetException =>
          toFetchOffset = e.nextOffsetToFetch
          if (toFetchOffset >= untilOffset) {
            resetFetchedData()
            toFetchOffset = UNKNOWN_OFFSET
          } else {
            logDebug(s"Skipped offsets [$offset, $toFetchOffset]")
          }
      }
    }

    if (isFetchComplete) {
      consumerRecord
    } else {
      resetFetchedData()
      null
    }
  }

  /**
   * Return the next earliest available offset in [offset, untilOffset). If all offsets in
   * [offset, untilOffset) are invalid (e.g., the topic is deleted and recreated), it will return
   * `UNKNOWN_OFFSET`.
   */
  private def getEarliestAvailableOffsetBetween(offset: Long, untilOffset: Long): Long = {
    val range = getAvailableOffsetRange()
    logWarning(s"Some data may be lost. Recovering from the earliest offset: ${range.earliest}")
    if (offset >= range.latest || range.earliest >= untilOffset) {
      // [offset, untilOffset) and [earliestOffset, latestOffset) have no overlap,
      // either
      // --------------------------------------------------------
      //         ^                 ^         ^         ^
      //         |                 |         |         |
      //   earliestOffset   latestOffset   offset   untilOffset
      //
      // or
      // --------------------------------------------------------
      //      ^          ^              ^                ^
      //      |          |              |                |
      //   offset   untilOffset   earliestOffset   latestOffset
      val warningMessage =
        s"""
          |The current available offset range is $range.
          | Offset ${offset} is out of range, and records in [$offset, $untilOffset) will be
          | skipped ${additionalMessage(failOnDataLoss = false)}
        """.stripMargin
      logWarning(warningMessage)
      UNKNOWN_OFFSET
    } else if (offset >= range.earliest) {
      // -----------------------------------------------------------------------------
      //         ^            ^                  ^                                 ^
      //         |            |                  |                                 |
      //   earliestOffset   offset   min(untilOffset,latestOffset)   max(untilOffset, latestOffset)
      //
      // This will happen when a topic is deleted and recreated, and new data are pushed very fast,
      // then we will see `offset` disappears first then appears again. Although the parameters
      // are same, the state in Kafka cluster is changed, so the outer loop won't be endless.
      logWarning(s"Found a disappeared offset $offset. " +
        s"Some data may be lost ${additionalMessage(failOnDataLoss = false)}")
      offset
    } else {
      // ------------------------------------------------------------------------------
      //      ^           ^                       ^                                 ^
      //      |           |                       |                                 |
      //   offset   earliestOffset   min(untilOffset,latestOffset)   max(untilOffset, latestOffset)
      val warningMessage =
        s"""
           |The current available offset range is $range.
           | Offset ${offset} is out of range, and records in [$offset, ${range.earliest}) will be
           | skipped ${additionalMessage(failOnDataLoss = false)}
        """.stripMargin
      logWarning(warningMessage)
      range.earliest
    }
  }

  /**
   * Get the record for the given offset if available. Otherwise it will either throw error
   * (if failOnDataLoss = true), or return the next available offset within [offset, untilOffset),
   * or null.
   *
   * @throws OffsetOutOfRangeException if `offset` is out of range
   * @throws TimeoutException if cannot fetch the record in `pollTimeoutMs` milliseconds.
   */
  private def fetchData(
      offset: Long,
      untilOffset: Long,
      pollTimeoutMs: Long,
      failOnDataLoss: Boolean): ConsumerRecord[Array[Byte], Array[Byte]] = {
    if (offset != nextOffsetInFetchedData) {
      // This is the first fetch, or the fetched data has been reset.
      // Seek to the offset because we may call seekToBeginning or seekToEnd before this.
      seek(offset)
      poll(pollTimeoutMs)
    } else if (!fetchedData.hasNext) {
      // The last pre-fetched data has been drained.
      if (offset < offsetAfterPoll) {
        // Offsets in [offset, offsetAfterPoll) are missing. We should skip them.
        resetFetchedData()
        throw new MissingOffsetException(offset, offsetAfterPoll)
      } else {
        seek(offset)
        poll(pollTimeoutMs)
      }
    }

    if (!fetchedData.hasNext()) {
      // We cannot fetch anything after `poll`. Three possible cases:
      // - `offset` is out of range so that Kafka returns nothing. Just throw
      // `OffsetOutOfRangeException` to let the caller handle it.
      // - Cannot fetch any data before timeout. TimeoutException will be thrown.
      // - Fetched something but all of them are not valid date messages. In this case, the position
      //   will be changed and we can use it to determine this case.
      val range = getAvailableOffsetRange()
      if (offset < range.earliest || offset >= range.latest) {
        throw new OffsetOutOfRangeException(
          Map(topicPartition -> java.lang.Long.valueOf(offset)).asJava)
      } else if (offsetBeforePoll == offsetAfterPoll) {
        throw new TimeoutException(
          s"Cannot fetch record for offset $offset in $pollTimeoutMs milliseconds")
      } else {
        assert(offset <= offsetAfterPoll,
          s"seek to $offset and poll but the offset was reset to $offsetAfterPoll")
        throw new MissingOffsetException(offset, offsetAfterPoll)
      }
    } else {
      val record = fetchedData.next()
      nextOffsetInFetchedData = record.offset + 1
      // In general, Kafka uses the specified offset as the start point, and tries to fetch the next
      // available offset. Hence we need to handle offset mismatch.
      if (record.offset > offset) {
        val range = getAvailableOffsetRange()
        if (range.earliest <= offset) {
          resetFetchedData()
          throw new MissingOffsetException(offset, record.offset)
        }
        // This may happen when some records aged out but their offsets already got verified
        if (failOnDataLoss) {
          reportDataLoss(true, s"Cannot fetch records in [$offset, ${record.offset})")
          // Never happen as "reportDataLoss" will throw an exception
          null
        } else {
          if (record.offset >= untilOffset) {
            reportDataLoss(false, s"Skip missing records in [$offset, $untilOffset)")
            null
          } else {
            reportDataLoss(false, s"Skip missing records in [$offset, ${record.offset})")
            record
          }
        }
      } else if (record.offset < offset) {
        // This should not happen. If it does happen, then we probably misunderstand Kafka internal
        // mechanism.
        throw new IllegalStateException(
          s"Tried to fetch $offset but the returned record offset was ${record.offset}")
      } else {
        record
      }
    }
  }

  /** Create a new consumer and reset cached states */
  private def resetConsumer(): Unit = {
    consumer.close()
    consumer = createConsumer
    resetFetchedData()
  }

  /** Reset the internal pre-fetched data. */
  private def resetFetchedData(): Unit = {
    nextOffsetInFetchedData = UNKNOWN_OFFSET
    fetchedData = ju.Collections.emptyIterator[ConsumerRecord[Array[Byte], Array[Byte]]]
  }

  /**
   * Return an addition message including useful message and instruction.
   */
  private def additionalMessage(failOnDataLoss: Boolean): String = {
    if (failOnDataLoss) {
      s"(GroupId: $groupId, TopicPartition: $topicPartition). " +
        s"$INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_TRUE"
    } else {
      s"(GroupId: $groupId, TopicPartition: $topicPartition). " +
        s"$INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_FALSE"
    }
  }

  /**
   * Throw an exception or log a warning as per `failOnDataLoss`.
   */
  private def reportDataLoss(
      failOnDataLoss: Boolean,
      message: String,
      cause: Throwable = null): Unit = {
    val finalMessage = s"$message ${additionalMessage(failOnDataLoss)}"
    reportDataLoss0(failOnDataLoss, finalMessage, cause)
  }

  def close(): Unit = consumer.close()

  private def seek(offset: Long): Unit = {
    logDebug(s"Seeking to $groupId $topicPartition $offset")
    consumer.seek(topicPartition, offset)
  }

  private def poll(pollTimeoutMs: Long): Unit = {
    offsetBeforePoll = consumer.position(topicPartition)
    val p = consumer.poll(pollTimeoutMs)
    val r = p.records(topicPartition)
    logDebug(s"Polled $groupId ${p.partitions()}  ${r.size}")
    offsetAfterPoll = consumer.position(topicPartition)
    logDebug(s"Offset changed from $offsetBeforePoll to $offsetAfterPoll after polling")
    fetchedData = r.iterator
  }
}


private[kafka010] object KafkaDataConsumer extends Logging {

  case class AvailableOffsetRange(earliest: Long, latest: Long)

  private case class CachedKafkaDataConsumer(internalConsumer: InternalKafkaConsumer)
    extends KafkaDataConsumer {
    assert(internalConsumer.inUse) // make sure this has been set to true
    override def release(): Unit = { KafkaDataConsumer.release(internalConsumer) }
  }

  private case class NonCachedKafkaDataConsumer(internalConsumer: InternalKafkaConsumer)
    extends KafkaDataConsumer {
    override def release(): Unit = { internalConsumer.close() }
  }

  private case class CacheKey(groupId: String, topicPartition: TopicPartition) {
    def this(topicPartition: TopicPartition, kafkaParams: ju.Map[String, Object]) =
      this(kafkaParams.get(ConsumerConfig.GROUP_ID_CONFIG).asInstanceOf[String], topicPartition)
  }

  // This cache has the following important properties.
  // - We make a best-effort attempt to maintain the max size of the cache as configured capacity.
  //   The capacity is not guaranteed to be maintained, especially when there are more active
  //   tasks simultaneously using consumers than the capacity.
  private lazy val cache = {
    val conf = SparkEnv.get.conf
    val capacity = conf.getInt("spark.sql.kafkaConsumerCache.capacity", 64)
    new ju.LinkedHashMap[CacheKey, InternalKafkaConsumer](capacity, 0.75f, true) {
      override def removeEldestEntry(
        entry: ju.Map.Entry[CacheKey, InternalKafkaConsumer]): Boolean = {

        // Try to remove the least-used entry if its currently not in use.
        //
        // If you cannot remove it, then the cache will keep growing. In the worst case,
        // the cache will grow to the max number of concurrent tasks that can run in the executor,
        // (that is, number of tasks slots) after which it will never reduce. This is unlikely to
        // be a serious problem because an executor with more than 64 (default) tasks slots is
        // likely running on a beefy machine that can handle a large number of simultaneously
        // active consumers.

        if (!entry.getValue.inUse && this.size > capacity) {
          logWarning(
            s"KafkaConsumer cache hitting max capacity of $capacity, " +
              s"removing consumer for ${entry.getKey}")
          try {
            entry.getValue.close()
          } catch {
            case e: SparkException =>
              logError(s"Error closing earliest Kafka consumer for ${entry.getKey}", e)
          }
          true
        } else {
          false
        }
      }
    }
  }

  /**
   * Get a cached consumer for groupId, assigned to topic and partition.
   * If matching consumer doesn't already exist, will be created using kafkaParams.
   * The returned consumer must be released explicitly using [[KafkaDataConsumer.release()]].
   *
   * Note: This method guarantees that the consumer returned is not currently in use by any one
   * else. Within this guarantee, this method will make a best effort attempt to re-use consumers by
   * caching them and tracking when they are in use.
   */
  def acquire(
      topicPartition: TopicPartition,
      kafkaParams: ju.Map[String, Object],
      useCache: Boolean): KafkaDataConsumer = synchronized {
    val key = new CacheKey(topicPartition, kafkaParams)
    val existingInternalConsumer = cache.get(key)

    lazy val newInternalConsumer = new InternalKafkaConsumer(topicPartition, kafkaParams)

    if (TaskContext.get != null && TaskContext.get.attemptNumber >= 1) {
      // If this is reattempt at running the task, then invalidate cached consumer if any and
      // start with a new one.
      if (existingInternalConsumer != null) {
        // Consumer exists in cache. If its in use, mark it for closing later, or close it now.
        if (existingInternalConsumer.inUse) {
          existingInternalConsumer.markedForClose = true
        } else {
          existingInternalConsumer.close()
        }
      }
      cache.remove(key)  // Invalidate the cache in any case
      NonCachedKafkaDataConsumer(newInternalConsumer)

    } else if (!useCache) {
      // If planner asks to not reuse consumers, then do not use it, return a new consumer
      NonCachedKafkaDataConsumer(newInternalConsumer)

    } else if (existingInternalConsumer == null) {
      // If consumer is not already cached, then put a new in the cache and return it
      cache.put(key, newInternalConsumer)
      newInternalConsumer.inUse = true
      CachedKafkaDataConsumer(newInternalConsumer)

    } else if (existingInternalConsumer.inUse) {
      // If consumer is already cached but is currently in use, then return a new consumer
      NonCachedKafkaDataConsumer(newInternalConsumer)

    } else {
      // If consumer is already cached and is currently not in use, then return that consumer
      existingInternalConsumer.inUse = true
      CachedKafkaDataConsumer(existingInternalConsumer)
    }
  }

  private def release(intConsumer: InternalKafkaConsumer): Unit = {
    synchronized {

      // Clear the consumer from the cache if this is indeed the consumer present in the cache
      val key = new CacheKey(intConsumer.topicPartition, intConsumer.kafkaParams)
      val cachedIntConsumer = cache.get(key)
      if (intConsumer.eq(cachedIntConsumer)) {
        // The released consumer is the same object as the cached one.
        if (intConsumer.markedForClose) {
          intConsumer.close()
          cache.remove(key)
        } else {
          intConsumer.inUse = false
        }
      } else {
        // The released consumer is either not the same one as in the cache, or not in the cache
        // at all. This may happen if the cache was invalidate while this consumer was being used.
        // Just close this consumer.
        intConsumer.close()
        logInfo(s"Released a supposedly cached consumer that was not found in the cache")
      }
    }
  }
}

private[kafka010] object InternalKafkaConsumer extends Logging {

  private val UNKNOWN_OFFSET = -2L

  private def reportDataLoss0(
      failOnDataLoss: Boolean,
      finalMessage: String,
      cause: Throwable = null): Unit = {
    if (failOnDataLoss) {
      if (cause != null) {
        throw new IllegalStateException(finalMessage, cause)
      } else {
        throw new IllegalStateException(finalMessage)
      }
    } else {
      if (cause != null) {
        logWarning(finalMessage, cause)
      } else {
        logWarning(finalMessage)
      }
    }
  }
}
