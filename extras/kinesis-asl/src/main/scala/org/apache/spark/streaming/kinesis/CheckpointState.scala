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
package org.apache.spark.streaming.kinesis

import org.apache.spark.Logging
import org.apache.spark.streaming.util.Clock
import org.apache.spark.streaming.util.ManualClock
import org.apache.spark.streaming.util.SystemClock

/**
 * This is a helper class for managing checkpoint clocks.
 *
 * @param checkpoint interval in millis
 * @param current clock.  if none specified, will default to current SystemClock
 */
private[kinesis] class CheckpointState(
    checkpointIntervalMillis: Long, 
    currentClock: Clock = new SystemClock())
  extends Logging {
  /**
   * Initialize the checkpoint clock using the given currentClock + checkpointIntervalMillis
   */
  val checkpointClock = new ManualClock()
  checkpointClock.setTime(currentClock.currentTime() + checkpointIntervalMillis)

  /**
   * Check if it's time to checkpoint based on the current time and the derived time 
   *   for the next checkpoint
   *
   * @return true if it's time to checkpoint
   */
  def shouldCheckpoint(): Boolean = {
    new SystemClock().currentTime() > checkpointClock.currentTime()
  }

  /**
   * Advance the checkpoint clock by the checkpoint interval.
   */
  def advanceCheckpoint() = {
    checkpointClock.addToTime(checkpointIntervalMillis)
  }
}
