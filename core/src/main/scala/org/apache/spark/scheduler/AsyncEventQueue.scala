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

package org.apache.spark.scheduler

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import com.codahale.metrics.{Gauge, Timer}
import com.rabbitmq.client.impl.VariableLinkedBlockingQueue

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.internal.Logging
import org.apache.spark.internal.config._
import org.apache.spark.util.ThreadUtils
import org.apache.spark.util.Utils

/**
 * An asynchronous queue for events. All events posted to this queue will be delivered to the child
 * listeners in a separate thread.
 *
 * Delivery will only begin when the `start()` method is called. The `stop()` method should be
 * called when no more events need to be delivered.
 */
private class AsyncEventQueue(
    val name: String,
    conf: SparkConf,
    metrics: LiveListenerBusMetrics,
    bus: LiveListenerBus)
  extends SparkListenerBus
  with Logging {

  import AsyncEventQueue._

  // Cap the capacity of the queue so we get an explicit error (rather than an OOM exception) if
  // it's perpetually being added to more quickly than it's being drained.
  // The capacity can be configured by spark.scheduler.listenerbus.eventqueue.${name}.capacity,
  // if no such conf is specified, use the value specified in
  // LISTENER_BUS_EVENT_QUEUE_CAPACITY
  private[scheduler] def capacity: Int = {
    val queueSize = conf.getInt(s"$LISTENER_BUS_EVENT_QUEUE_PREFIX.$name.capacity",
      conf.get(LISTENER_BUS_EVENT_QUEUE_CAPACITY))
    assert(queueSize > 0, s"capacity for event queue $name must be greater than 0, " +
      s"but $queueSize is configured.")
    queueSize
  }

  private val (eventQueue, newQueueType): (BlockingQueue[SparkListenerEvent], Boolean) =
    if (conf.get(OPTMIZED_ASYNC_EVENT_QUEUE).contains(name)) {
    (new VariableLinkedBlockingQueue[SparkListenerEvent](capacity), true)
  } else {
    (new LinkedBlockingQueue[SparkListenerEvent](capacity), false)
  }

  private val queueSizeThreshold: Int = conf.get(OPTMIZED_ASYNC_EVENT_QUEUE_SIZE_THRESHOLD)

  private lazy val maxSizeQueue: Int = if (newQueueType) {
    capacity + (capacity * queueSizeThreshold) / 100
  } else {
    capacity
  }

  // For testing only.
  @volatile var newQueueCapacity: Int = capacity

  @volatile private var startCheckQueueCapacityThread: Boolean = true

  // scheduler thread to monitor the Queue Size for variable size event Queue
  private lazy val checkQueueCapacityThread =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor(s"checkQueueCapacity-$name")

  // Keep the event count separately, so that waitUntilEmpty() can be implemented properly;
  // this allows that method to return only when the events in the queue have been fully
  // processed (instead of just dequeued).
  private val eventCount = new AtomicLong()

  /** A counter for dropped events. */
  private val droppedEventsCounter = new AtomicLong(0L)

  /** A counter to keep number of dropped events last time it was logged */
  @volatile private var lastDroppedEventsCounter: Long = 0L

  /** When `droppedEventsCounter` was logged last time in milliseconds. */
  private val lastReportTimestamp = new AtomicLong(0L)

  private val logDroppedEvent = new AtomicBoolean(false)

  private var sc: SparkContext = null

  private val started = new AtomicBoolean(false)
  private val stopped = new AtomicBoolean(false)

  private val droppedEvents = metrics.metricRegistry.counter(s"queue.$name.numDroppedEvents")
  private val processingTime = metrics.metricRegistry.timer(s"queue.$name.listenerProcessingTime")

  // Remove the queue size gauge first, in case it was created by a previous incarnation of
  // this queue that was removed from the listener bus.
  metrics.metricRegistry.remove(s"queue.$name.size")
  metrics.metricRegistry.register(s"queue.$name.size", new Gauge[Int] {
    override def getValue: Int = eventQueue.size()
  })

  private val dispatchThread = new Thread(s"spark-listener-group-$name") {
    setDaemon(true)
    override def run(): Unit = Utils.tryOrStopSparkContext(sc) {
      dispatch()
    }
  }

  private def addEventToQueue(event: SparkListenerEvent): Boolean = {
    eventQueue match {
      case variableSizeQueue: VariableLinkedBlockingQueue[SparkListenerEvent] =>
        if (!variableSizeQueue.offer(event)) {
          synchronized {
            setQueueCapacity()
          }
          variableSizeQueue.offer(event)
        } else {
          true
        }
      case fixedSizedQueue: LinkedBlockingQueue[SparkListenerEvent] =>
        fixedSizedQueue.offer(event)
      case _ => false
    }
  }

  // Helper method to set the initial capacity for
  // VariableLinkedBlockingQueue.
  private def setQueueInitialCapacity(): Unit = {
    eventQueue match {
      case queue: VariableLinkedBlockingQueue[SparkListenerEvent] =>
        val queueSize = queue.size()
        // If the size of queue is less than 10 percent of the initial capacity
        // than set the initial capacity to the queue
        val diffInitialCapacity = if (queueSize < capacity) {
          ((capacity - queueSize)/capacity) * 100
        } else {
          0
        }
        if (diffInitialCapacity > MIN_SIZE_QUEUE_THRESHOLD_PERCENTAGE && queueSize == 0) {
          queue.setCapacity(capacity)
          newQueueCapacity = capacity
        }
    }
  }

  // Helper method to set new capacity for VariableLinkedBlockingQueue
  private def setQueueCapacity(): Unit = {
    eventQueue match {
      case queue: VariableLinkedBlockingQueue[SparkListenerEvent] =>
        val driverID: String = SparkContext.DRIVER_IDENTIFIER
        // get the driverMemoryUsedPct for deciding whether to increase
        // Queue capacity or not
        val driverMemoryUsedPct = if (sc != null && sc.statusStore != null) {
          val driverSummary = sc.statusStore.executorSummary(driverID)
          (driverSummary.memoryUsed / driverSummary.maxMemory) * 100
        } else {
          1
        }
        val currentSizeQueue = queue.size
        val newQueSize = currentSizeQueue + (currentSizeQueue * queueSizeThreshold) / 100
        if (newQueSize <= maxSizeQueue && newQueSize > capacity
          && driverMemoryUsedPct < USE_DRIVER_MEMORY_THRESHOLD) {
          queue.setCapacity(newQueSize)
          newQueueCapacity = newQueSize
        }
        if (startCheckQueueCapacityThread) {
          startCheckQueueCapacityThread = false
          checkQueueCapacityThread.scheduleWithFixedDelay(new Runnable {
            override def run(): Unit = Utils.tryLogNonFatalError {
              setQueueInitialCapacity
            }
          }, CHECK_QUEUE_SIZE_SCHEDULER_INTERVAL,
            CHECK_QUEUE_SIZE_SCHEDULER_INTERVAL, TimeUnit.MINUTES)
        }
    }
  }

  private def dispatch(): Unit = LiveListenerBus.withinListenerThread.withValue(true) {
    var next: SparkListenerEvent = eventQueue.take()
    while (next != POISON_PILL) {
      val ctx = processingTime.time()
      try {
        super.postToAll(next)
      } finally {
        ctx.stop()
      }
      eventCount.decrementAndGet()
      next = eventQueue.take()
    }
    eventCount.decrementAndGet()
  }

  override protected def getTimer(listener: SparkListenerInterface): Option[Timer] = {
    metrics.getTimerForListenerClass(listener.getClass.asSubclass(classOf[SparkListenerInterface]))
  }

  /**
   * Start an asynchronous thread to dispatch events to the underlying listeners.
   *
   * @param sc Used to stop the SparkContext in case the async dispatcher fails.
   */
  private[scheduler] def start(sc: SparkContext): Unit = {
    if (started.compareAndSet(false, true)) {
      this.sc = sc
      dispatchThread.start()
    } else {
      throw new IllegalStateException(s"$name already started!")
    }
  }

  /**
   * Stop the listener bus. It will wait until the queued events have been processed, but new
   * events will be dropped.
   */
  private[scheduler] def stop(): Unit = {
    if (!started.get()) {
      throw new IllegalStateException(s"Attempted to stop $name that has not yet started!")
    }
    if (stopped.compareAndSet(false, true)) {
      eventCount.incrementAndGet()
      eventQueue.put(POISON_PILL)
    }
    // this thread might be trying to stop itself as part of error handling -- we can't join
    // in that case.
    if (Thread.currentThread() != dispatchThread) {
      dispatchThread.join()
    }
  }

  def post(event: SparkListenerEvent): Unit = {
    if (stopped.get()) {
      return
    }

    eventCount.incrementAndGet()
    if (addEventToQueue(event)) {
      return
    }

    eventCount.decrementAndGet()
    droppedEvents.inc()
    droppedEventsCounter.incrementAndGet()
    if (logDroppedEvent.compareAndSet(false, true)) {
      // Only log the following message once to avoid duplicated annoying logs.
      logError(s"Dropping event from queue $name. " +
        "This likely means one of the listeners is too slow and cannot keep up with " +
        "the rate at which tasks are being started by the scheduler.")
    }
    logTrace(s"Dropping event $event")

    val droppedEventsCount = droppedEventsCounter.get
    val droppedCountIncreased = droppedEventsCount - lastDroppedEventsCounter
    val lastReportTime = lastReportTimestamp.get
    val curTime = System.currentTimeMillis()
    // Don't log too frequently
    if (droppedCountIncreased > 0 && curTime - lastReportTime >= LOGGING_INTERVAL) {
      // There may be multiple threads trying to logging dropped events,
      // Use 'compareAndSet' to make sure only one thread can win.
      if (lastReportTimestamp.compareAndSet(lastReportTime, curTime)) {
        val previous = new java.util.Date(lastReportTime)
        lastDroppedEventsCounter = droppedEventsCount
        logWarning(s"Dropped $droppedCountIncreased events from $name since " +
          s"${if (lastReportTime == 0) "the application started" else s"$previous"}.")
      }
    }
  }

  /**
   * For testing only. Wait until there are no more events in the queue.
   *
   * @return true if the queue is empty.
   */
  def waitUntilEmpty(deadline: Long): Boolean = {
    while (eventCount.get() != 0) {
      if (System.currentTimeMillis > deadline) {
        return false
      }
      Thread.sleep(10)
    }
    true
  }

  override def removeListenerOnError(listener: SparkListenerInterface): Unit = {
    // the listener failed in an unrecoverably way, we want to remove it from the entire
    // LiveListenerBus (potentially stopping a queue if it is empty)
    bus.removeListener(listener)
  }

}

private object AsyncEventQueue {

  val POISON_PILL = new SparkListenerEvent() { }

  val LOGGING_INTERVAL = 60 * 1000

  val MIN_SIZE_QUEUE_THRESHOLD_PERCENTAGE = 10

  val CHECK_QUEUE_SIZE_SCHEDULER_INTERVAL = 10

  val USE_DRIVER_MEMORY_THRESHOLD = 90
}
