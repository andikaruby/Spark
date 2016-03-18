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

package org.apache.spark.memory

import org.apache.spark.SparkConf
import org.apache.spark.storage.BlockId

class TestMemoryManager(conf: SparkConf)
  extends MemoryManager(conf, numCores = 1, Long.MaxValue, Long.MaxValue) {

<<<<<<< HEAD
=======
class TestMemoryManager(conf: SparkConf)
  extends MemoryManager(conf, numCores = 1, Long.MaxValue, Long.MaxValue) {

>>>>>>> 022e06d18471bf54954846c815c8a3666aef9fc3
  override private[memory] def acquireExecutionMemory(
      numBytes: Long,
      taskAttemptId: Long,
      memoryMode: MemoryMode): Long = {
    if (oomOnce) {
      oomOnce = false
      0
    } else if (available >= numBytes) {
      available -= numBytes
      numBytes
    } else {
      val grant = available
      available = 0
      grant
    }
  }
  override def acquireStorageMemory(blockId: BlockId, numBytes: Long): Boolean = true
  override def acquireUnrollMemory(blockId: BlockId, numBytes: Long): Boolean = true
  override def releaseStorageMemory(numBytes: Long): Unit = {}
  override private[memory] def releaseExecutionMemory(
      numBytes: Long,
<<<<<<< HEAD
=======
      evictedBlocks: mutable.Buffer[(BlockId, BlockStatus)]): Boolean = true
  override def releaseStorageMemory(numBytes: Long): Unit = {}
  override private[memory] def releaseExecutionMemory(
      numBytes: Long,
>>>>>>> 022e06d18471bf54954846c815c8a3666aef9fc3
      taskAttemptId: Long,
      memoryMode: MemoryMode): Unit = {
    available += numBytes
  }
  override def maxStorageMemory: Long = Long.MaxValue

  private var oomOnce = false
  private var available = Long.MaxValue

  def markExecutionAsOutOfMemoryOnce(): Unit = {
    oomOnce = true
  }

  def limit(avail: Long): Unit = {
    available = avail
  }

}
