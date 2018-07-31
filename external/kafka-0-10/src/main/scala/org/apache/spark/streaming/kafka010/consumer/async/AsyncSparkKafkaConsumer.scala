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

package org.apache.spark.streaming.kafka010.consumer.async

import java.{util => ju}
import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import scala.concurrent.{ExecutionContext, Future}

import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.common.TopicPartition

import org.apache.spark.internal.Logging
import org.apache.spark.streaming.kafka010.consumer.SparkKafkaConsumer

class AsyncSparkKafkaConsumer[K, V](
                                        val topicPartition: TopicPartition,
                                        val kafkaParams: ju.Map[String, Object],
                                        val maintainBufferMin: Int,
                                        val pollTimeout: Long
                                      ) extends SparkKafkaConsumer[K, V] {

  /** We need to maintain lastRecord to revert buffer to previous state
   * in case of compacted iteration
   */
  @volatile private var lastRecord: ConsumerRecord[K, V] = null

  // Pre-initialize async task and output to empty list
  // This tuple will cleaned-up and updated as required
  var asyncPollTask: AsyncPollTask[K, V] = null

  private def createNewPollTask(offset: Long) = {
    new AsyncPollTask[K, V](
      topicPartition, ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()),
      createConsumer, maintainBufferMin, pollTimeout, offset
    )
  }

  /**
   * Ensure that next record have expected offset.
   * If not clear the buffer and async poll task, and reset seek offset
   */
  def ensureOffset(offset: Long): Unit = {
    if (asyncPollTask == null) {
      asyncPollTask = createNewPollTask(offset)
    }
    else if (asyncPollTask.getNextOffset() != offset) {
      logInfo(s"Seeking to offset $offset")
      asyncPollTask.close()
      asyncPollTask = createNewPollTask(offset)
    }
  }

  /**
   * Get next record from buffer.
   *  First ensure buffer have data.
   *  Then return first record from buffer.
   */
  def getNextRecord(): ConsumerRecord[K, V] = {
    lastRecord = asyncPollTask.getNextRecord(pollTimeout)
    lastRecord
  }

  /**
   * Add last record back to buffer to revert the state.
   */
  def moveToPrevious(): ConsumerRecord[K, V] = {
    asyncPollTask.revertLastRecord(lastRecord)
    lastRecord
  }

  def getNextOffset(): Long = {
    if (asyncPollTask == null) {
      AsyncSparkKafkaConsumer.UNKNOWN_OFFSET
    }
    else {
      asyncPollTask.getNextOffset()
    }
  }

  /** Create a KafkaConsumer to fetch records for `topicPartition` */
  private def createConsumer: KafkaConsumer[K, V] = {
    val c = new KafkaConsumer[K, V](kafkaParams)
    val topics = ju.Arrays.asList(topicPartition)
    c.assign(topics)
    logInfo(s"$topicPartition Created new kafka consumer with assigned topics $topics")
    c
  }

  def close(): Unit = {
    logInfo(s"$topicPartition Closing consumers")
    asyncPollTask.close()
  }
}

private[kafka010] class AsyncPollTask[K, V](
                                             topicPartition: TopicPartition,
                                             executionContext: ExecutionContext,
                                             kafkaConsumer: KafkaConsumer[K, V],
                                             maintainBufferSize: Long,
                                             pollTimeout: Long,
                                             startOffset: Long
                                           ) extends Logging {

  var bufferList: ThreadSafeEfficientConcurrentLinkedQueue[ConsumerRecord[K, V]] =
    new ThreadSafeEfficientConcurrentLinkedQueue[ConsumerRecord[K, V]]()
  private val maintainBufferMin = Math.max(maintainBufferSize, 1)
  private val currentSeekOffset: AtomicLong = new AtomicLong(
    startOffset
  )
  private val lastRequestedAt: AtomicLong = new AtomicLong(System.nanoTime())
  private val shouldClose: AtomicBoolean = new AtomicBoolean(false)

  private val future = Future {
    var listSize: Int = 0
    while (!shouldClose.get()) {
      val lastRequestedAtBefore = lastRequestedAt.get()
      listSize = bufferList.size()
      logInfo(s"$topicPartition Buffersize is $listSize and maintain buffer is $maintainBufferMin")
      if (listSize < maintainBufferMin) {
        var records = fetch(currentSeekOffset.get())
        var count = if (records == null) 0 else records.size()
        if (count == 0) {
          waitForReactivation(lastRequestedAtBefore)
        } else {
          logInfo(s"$topicPartition Adding $count records to buffer")
          currentSeekOffset.set(records.get(count - 1).offset() + 1)
          bufferList.addAll(records)
        }
      } else {
        logInfo(s"$topicPartition Since buffer is sufficient")
        waitForReactivation(lastRequestedAtBefore)
      }
    }
    logInfo(s"$topicPartition Closing consumer")
    kafkaConsumer.close()
  }(executionContext)

  private def waitForReactivation(lastRequestedAtBefore: Long) = {
    lastRequestedAt.synchronized {
      if (lastRequestedAtBefore == lastRequestedAt.get()) {
        logInfo(s"$topicPartition Future going to sleep")
        lastRequestedAt.wait()
        logInfo(s"$topicPartition Future is awake")
      } else {
        logInfo(s"$topicPartition Last requested value mismatched Before : $lastRequestedAtBefore"
          + " Current : " + lastRequestedAt.get() + ", Skipping wait.")
      }
    }
  }

  private def fetch(seekOffset: Long): ju.List[ConsumerRecord[K, V]] = {
    var result: ju.List[ConsumerRecord[K, V]] = new ju.ArrayList[ConsumerRecord[K, V]]()
    try {
      var count = 0
      if (seekOffset != AsyncSparkKafkaConsumer.UNKNOWN_OFFSET) {
        logInfo(s"$topicPartition Polling from offset $seekOffset")
        kafkaConsumer.seek(topicPartition, seekOffset)
        val records = kafkaConsumer.poll(pollTimeout)
        count = records.count()
        logInfo(s"$topicPartition Received records $count offset $seekOffset " + this.toString)
        result = records.records(topicPartition)
      }
    }
    catch {
      case x: Exception =>
        logError(s"$topicPartition Exception in fetch", x)
    }
    result
  }

  def ensureAsyncTaskRunning(): Unit = {
    lastRequestedAt.synchronized {
      lastRequestedAt.set(System.nanoTime())
      lastRequestedAt.notify()
    }
  }

  def resetOffset(offset: Long): Unit = {
    logInfo(s"$topicPartition Clearing buffer and resetting offset from old value "
      + currentSeekOffset.get() + " to new value " + offset + s"for partition $topicPartition"
      + this.toString)
    bufferList.clear()
    currentSeekOffset.set(offset)
    logInfo(s"$topicPartition Notifying sleeping poll task for partition $topicPartition")
    ensureAsyncTaskRunning()
  }

  def getNextOffset(): Long =
    if (bufferList.size() == 0) currentSeekOffset.get else bufferList.peekFirst().offset

  def getNextRecord(timeout: Long): ConsumerRecord[K, V] = {
    ensureAsyncTaskRunning
    bufferList.pollFirst(timeout, TimeUnit.MILLISECONDS)
  }

  def revertLastRecord(record: ConsumerRecord[K, V]): Unit = bufferList.addFirst(record)

  /**
   * Close async kafka consumer. It uses future to close the consumer
   * since task could be running and throw exception if closed directly
   */
  def close(): Unit = {
    logInfo(s"$topicPartition Mark consumer for closure")
    resetOffset(AsyncSparkKafkaConsumer.UNKNOWN_OFFSET)
    shouldClose.set(true)
  }

}

private[kafka010] class ThreadSafeEfficientConcurrentLinkedQueue[T] {
  val queue: ju.concurrent.LinkedBlockingDeque[T] = new ju.concurrent.LinkedBlockingDeque[T]()

  def addAll(list: ju.List[T]): Unit = queue.synchronized {
    queue.addAll(list)
    queue.notifyAll()
  }

  def add(record: T): Unit = queue.synchronized {
    queue.add(record)
    queue.notifyAll()
  }

  def addFirst(record: T): Unit = queue.synchronized {
    queue.addFirst(record)
    queue.notifyAll()
  }

  def clear(): Unit = queue.synchronized {
    queue.clear()
    queue.notifyAll()
  }

  def pollFirst(timeout: Long, timeunit: TimeUnit): T = {
    queue.synchronized {
      var rec = queue.peekFirst()
      if (rec == null) {
        queue.wait(timeunit.toMillis(timeout))
      }
    }
    queue.pollFirst()
  }

  def peekFirst(): T = queue.synchronized {
    queue.peekFirst()
  }

  def size(): Int = queue.synchronized {
    queue.size()
  }

}

private[kafka010] object AsyncSparkKafkaConsumer {
  val UNKNOWN_OFFSET = -2L
}
