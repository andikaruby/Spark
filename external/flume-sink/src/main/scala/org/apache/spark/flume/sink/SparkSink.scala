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
package org.apache.spark.flume.sink


import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

import scala.util.control.Breaks

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.avro.ipc.NettyServer
import org.apache.avro.ipc.specific.SpecificResponder
import org.apache.commons.lang.RandomStringUtils
import org.apache.flume.Sink.Status
import org.apache.flume.conf.{ConfigurationException, Configurable}
import org.apache.flume.sink.AbstractSink
import org.apache.flume.{FlumeException, Context}

import org.apache.spark.flume.{SparkSinkEvent, EventBatch, SparkFlumeProtocol}
import org.slf4j.LoggerFactory



class SparkSink extends AbstractSink with Configurable {
  private val LOG = LoggerFactory.getLogger(this.getClass)

  // This sink will not persist sequence numbers and reuses them if it gets restarted.
  // So it is possible to commit a transaction which may have been meant for the sink before the
  // restart.
  // Since the new txn may not have the same sequence number we must guard against accidentally
  // committing
  // a new transaction. To reduce the probability of that happening a random string is prepended
  // to the sequence number.
  // Does not change for life of sink
  private val seqBase = RandomStringUtils.randomAlphanumeric(8)
  // Incremented for each transaction
  private val seqNum = new AtomicLong(0)

  private var transactionExecutorOpt: Option[ExecutorService] = None

  private var numProcessors: Integer = SparkSinkConfig.DEFAULT_PROCESSOR_COUNT
  private var transactionTimeout = SparkSinkConfig.DEFAULT_TRANSACTION_TIMEOUT

  private val processorMap = new ConcurrentHashMap[CharSequence, TransactionProcessor]()

  private var processorManager: Option[TransactionProcessorManager] = None
  private var hostname: String = SparkSinkConfig.DEFAULT_HOSTNAME
  private var port: Int = 0
  private var maxThreads: Int = SparkSinkConfig.DEFAULT_MAX_THREADS
  private var serverOpt: Option[NettyServer] = None

  private val blockingLatch = new CountDownLatch(1)

  override def start() {
    transactionExecutorOpt = Option(Executors.newFixedThreadPool(numProcessors,
      new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("Spark Sink, " + getName + " Processor Thread - %d").build()))

    processorManager = Option(new TransactionProcessorManager(numProcessors))

    val responder = new SpecificResponder(classOf[SparkFlumeProtocol], new AvroCallbackHandler())

    // Using the constructor that takes specific thread-pools requires bringing in netty
    // dependencies which are being excluded in the build. In practice,
    // Netty dependencies are already available on the JVM as Flume would have pulled them in.
    serverOpt = Option(new NettyServer(responder, new InetSocketAddress(hostname, port)))

    serverOpt.map(server => server.start())
    super.start()
  }

  override def stop() {
    transactionExecutorOpt.map(executor => executor.shutdownNow())
    serverOpt.map(server => {
      server.close()
      server.join()
    })
    blockingLatch.countDown()
    super.stop()
  }

  override def configure(ctx: Context) {
    import SparkSinkConfig._
    hostname = ctx.getString(CONF_HOSTNAME, DEFAULT_HOSTNAME)
    port = Option(ctx.getInteger(CONF_PORT)).
      getOrElse(throw new ConfigurationException("The port to bind to must be specified"))
    numProcessors = ctx.getInteger(PROCESSOR_COUNT, DEFAULT_PROCESSOR_COUNT)
    transactionTimeout = ctx.getInteger(CONF_TRANSACTION_TIMEOUT, DEFAULT_TRANSACTION_TIMEOUT)
    maxThreads = ctx.getInteger(CONF_MAX_THREADS, DEFAULT_MAX_THREADS)
  }

  override def process(): Status = {
    // This method is called in a loop by the Flume framework - block it until the sink is
    // stopped to save CPU resources. The sink runner will interrupt this thread when the sink is
    // being shut down.
    blockingLatch.await()
    Status.BACKOFF
  }


  // Object representing an empty batch returned by the txn processor due to some error.
  case object ErrorEventBatch extends EventBatch

  private class AvroCallbackHandler extends SparkFlumeProtocol {

    override def getEventBatch(n: Int): EventBatch = {
      val processor = processorManager.get.checkOut(n)
      transactionExecutorOpt.map(executor => executor.submit(processor))
      // Wait until a batch is available - can be null if some error was thrown
      processor.eventQueue.take() match {
        case ErrorEventBatch => throw new FlumeException("Something went wrong. No events" +
          " retrieved from channel.")
        case eventBatch: EventBatch =>
          processorMap.put(eventBatch.getSequenceNumber, processor)
          if (LOG.isDebugEnabled) {
            LOG.debug("Sent " + eventBatch.getEvents.size() +
              " events with sequence number: " + eventBatch.getSequenceNumber)
          }
          eventBatch
      }
    }

    override def ack(sequenceNumber: CharSequence): Void = {
      completeTransaction(sequenceNumber, success = true)
      null
    }

    override def nack(sequenceNumber: CharSequence): Void = {
      completeTransaction(sequenceNumber, success = false)
      LOG.info("Spark failed to commit transaction. Will reattempt events.")
      null
    }

    def completeTransaction(sequenceNumber: CharSequence, success: Boolean) {
      val processorOpt = Option(processorMap.remove(sequenceNumber))
      if (processorOpt.isDefined) {
        val processor = processorOpt.get
        processor.resultQueueUpdateLock.lock()
        try {
          // Is the sequence number the same as the one the processor is processing? If not,
          // don't update {
          if (processor.eventBatch.getSequenceNumber.equals(sequenceNumber)) {
            processor.resultQueue.put(success)
          }
        } finally {
          processor.resultQueueUpdateLock.unlock()
        }
      }
    }
  }

  // Flume forces transactions to be thread-local (horrible, I know!)
  // So the sink basically spawns a new thread to pull the events out within a transaction.
  // The thread fills in the event batch object that is set before the thread is scheduled.
  // After filling it in, the thread waits on a condition - which is released only
  // when the success message comes back for the specific sequence number for that event batch.
  /**
   * This class represents a transaction on the Flume channel. This class runs a separate thread
   * which owns the transaction. It is blocked until the success call for that transaction comes
   * back.
   * @param maxBatchSize
   */
  private class TransactionProcessor(var maxBatchSize: Int) extends Callable[Void] {
    // Must be set to a new event batch before scheduling this!!
    val eventBatch = new EventBatch("", new util.LinkedList[SparkSinkEvent])
    val eventQueue = new SynchronousQueue[EventBatch]()
    val resultQueue = new SynchronousQueue[Boolean]()
    val resultQueueUpdateLock = new ReentrantLock()

    object Zero {
      val zero = "0" // Oh, I miss static finals
    }


    override def call(): Void = {
      val tx = getChannel.getTransaction
      tx.begin()
      try {
        eventBatch.setSequenceNumber(seqBase + seqNum.incrementAndGet())
        val events = eventBatch.getEvents
        events.clear()
        val loop = new Breaks
        var gotEventsInThisTxn = false
        loop.breakable {
          while (events.size() < maxBatchSize) {
            Option(getChannel.take()) match {
              case Some(event) =>
                events.add(new SparkSinkEvent(toCharSequenceMap(event.getHeaders),
                  ByteBuffer.wrap(event.getBody)))
                gotEventsInThisTxn = true
              case None =>
                if (!gotEventsInThisTxn) {
                  Thread.sleep(500)
                } else {
                  loop.break()
                }
            }
          }
        }
        // Make the data available to the sender thread
        eventQueue.put(eventBatch)

        // Wait till timeout for the ack/nack
        val maybeResult = Option(resultQueue.poll(transactionTimeout, TimeUnit.SECONDS))
        // There is a race condition here.
        // 1. This times out.
        // 2. The result is empty, so timeout exception is thrown.
        // 3. The ack comes in before the finally block is entered
        // 4. The thread with the ack has a handle to this processor,
        // and another thread has the same processor checked out
        // (since the finally block was executed and the processor checked back in)
        // 5. The thread with the ack now updates the result queue,
        // so the processor thinks it is the ack for the current batch.
        // To avoid this - update the sequence number to "0" (with or without a result - does not
        // matter).
        // In the ack method, check if the seq number is the same as the processor's -
        // if they are then update the result queue. Now if the
        // processor updates the seq number first - the ack/nack never updates the result. If the
        // ack/nack updates the
        // result after the timeout but before the seq number is updated to "0" it does not
        // matter - the processor would
        // still timeout and the result is cleared before reusing the processor.
        // Unfortunately, this needs to be done from within a lock
        // to make sure that the new sequence number is actually visible to the ack thread
        // (happens-before)
        resultQueueUpdateLock.lock()
        try {
          eventBatch.setSequenceNumber(Zero.zero)
        } finally {
          resultQueueUpdateLock.unlock()
        }
        eventBatch.getEvents.clear()
        // If the batch failed on spark side, throw a FlumeException
        maybeResult.map(success =>
          if (!success) {
            throw new
                FlumeException("Spark could not accept events. The transaction will be retried.")
          }
        )
        // If the operation timed out, throw a TimeoutException
        if (maybeResult.isEmpty) {
          throw new TimeoutException("Spark did not respond within the timeout period of " +
            transactionTimeout + "seconds. Transaction will be retried")
        }
        null
      } catch {
        case e: Throwable =>
          try {
            LOG.warn("Error while attempting to remove events from the channel.", e)
            tx.rollback()
          } catch {
            case e1: Throwable => LOG.error(
              "Rollback failed while attempting to rollback due to commit failure.", e1)
          }
          null // No point rethrowing the exception
      } finally {
        // Must *always* release the caller thread
        eventQueue.put(ErrorEventBatch)
        // In the case of success coming after the timeout, but before resetting the seq number
        // remove the event from the map and then clear the value
        resultQueue.clear()
        processorMap.remove(eventBatch.getSequenceNumber)
        processorManager.get.checkIn(this)
        tx.close()
      }
    }

    def toCharSequenceMap(inMap: java.util.Map[String, String]): java.util.Map[CharSequence,
      CharSequence] = {
      val charSeqMap = new util.HashMap[CharSequence, CharSequence](inMap.size())
      charSeqMap.putAll(inMap)
      charSeqMap
    }
  }

  private class TransactionProcessorManager(val maxInstances: Int) {
    val queue = new scala.collection.mutable.Queue[TransactionProcessor]
    val queueModificationLock = new ReentrantLock()
    var currentSize = 0
    val waitForCheckIn = queueModificationLock.newCondition()

    def checkOut(n: Int): TransactionProcessor = {
      def getProcessor = {
        val processor = queue.dequeue()
        processor.maxBatchSize = n
        processor
      }
      queueModificationLock.lock()
      try {
        if (queue.size > 0) {
          getProcessor
        }
        else {
          if (currentSize < maxInstances) {
            currentSize += 1
            new TransactionProcessor(n)
          } else {
            // No events in queue and cannot initialize more!
            // Since currentSize never reduces, queue size increasing is the only hope
            while (queue.size == 0 && currentSize >= maxInstances) {
              waitForCheckIn.await()
            }
            getProcessor
          }
        }
      } finally {
        queueModificationLock.unlock()
      }
    }

    def checkIn(processor: TransactionProcessor) {
      queueModificationLock.lock()
      try {
        queue.enqueue(processor)
        waitForCheckIn.signal()
      } finally {
        queueModificationLock.unlock()
      }
    }
  }
}

object SparkSinkConfig {
  val PROCESSOR_COUNT = "processorCount"
  val DEFAULT_PROCESSOR_COUNT = 10

  val CONF_TRANSACTION_TIMEOUT = "timeout"
  val DEFAULT_TRANSACTION_TIMEOUT = 60

  val CONF_HOSTNAME = "hostname"
  val DEFAULT_HOSTNAME = "0.0.0.0"

  val CONF_PORT = "port"

  val CONF_MAX_THREADS = "maxThreads"
  val DEFAULT_MAX_THREADS = 5
}
