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

import scala.collection.mutable

import org.apache.spark.SparkConf
import org.apache.spark.storage.{BlockStatus, BlockId}

/**
 * A [[MemoryManager]] that enforces a soft boundary between execution and storage such that
 * either side can borrow memory from the other.
 *
 * The region shared between execution and storage is a fraction of the total heap space
 * configurable through `spark.memory.fraction` (default 0.75). The position of the boundary
 * within this space is further determined by `spark.memory.storageFraction` (default 0.5).
 * This means the size of the storage region is 0.75 * 0.5 = 0.375 of the heap space by default.
 *
 * Storage can borrow as much execution memory as is free until execution reclaims its space.
 * When this happens, cached blocks will be evicted from memory until sufficient borrowed
 * memory is released to satisfy the execution memory request.
 *
 * Similarly, execution can borrow as much storage memory as is free. However, execution
 * memory is *never* evicted by storage due to the complexities involved in implementing this.
 * The implication is that attempts to cache blocks may fail if execution has already eaten
 * up most of the storage space, in which case the new blocks will be evicted immediately
 * according to their respective storage levels.
 *
 * @param minimumStoragePoolSize Size of the storage region, in bytes.
 *                               This region is not statically reserved; execution can borrow from
 *                               it if necessary. Cached blocks can be evicted only if actual
 *                               storage memory usage exceeds this region.
 */
private[spark] class UnifiedMemoryManager private[memory] (
    conf: SparkConf,
    maxMemory: Long,
    private val minimumStoragePoolSize: Long,
    numCores: Int)
  extends MemoryManager(
    conf,
    numCores,
    maxOnHeapExecutionMemory = maxMemory - minimumStoragePoolSize) {

  onHeapExecutionMemoryPool.incrementPoolSize(maxMemory)

  override def maxStorageMemory: Long = synchronized {
    maxMemory - onHeapExecutionMemoryPool.memoryUsed
  }

  private[memory] def acquireOnHeapExecutionMemory(
      numBytes: Long,
      taskAttemptId: Long): Long = synchronized {
    assert(numBytes >= 0)
    val memoryBorrowedByStorage = math.max(0, storageMemoryPool.memoryUsed - minimumStoragePoolSize)
    // If there is not enough free memory AND storage has borrowed some execution memory,
    // then evict as much memory borrowed by storage as needed to grant this request
    if (numBytes > onHeapExecutionMemoryPool.memoryFree && memoryBorrowedByStorage > 0) {
      val spaceReclaimed =
        storageMemoryPool.shrinkPoolByEvictingBlocks(math.min(numBytes, memoryBorrowedByStorage))
      onHeapExecutionMemoryPool.incrementPoolSize(spaceReclaimed)
    }
    onHeapExecutionMemoryPool.acquireMemory(numBytes, taskAttemptId)
  }

  override def acquireStorageMemory(
      blockId: BlockId,
      numBytes: Long,
      evictedBlocks: mutable.Buffer[(BlockId, BlockStatus)]): Boolean = synchronized {
    if (numBytes > storageMemoryPool.memoryFree
        && numBytes <= onHeapExecutionMemoryPool.memoryFree) {
      onHeapExecutionMemoryPool.decrementPoolSize(numBytes)
      storageMemoryPool.incrementPoolSize(numBytes)
    }
    storageMemoryPool.acquireMemory(blockId, numBytes, evictedBlocks)
  }

  override def acquireUnrollMemory(
      blockId: BlockId,
      numBytes: Long,
      evictedBlocks: mutable.Buffer[(BlockId, BlockStatus)]): Boolean = synchronized {
    acquireStorageMemory(blockId, numBytes, evictedBlocks)
  }
}

object UnifiedMemoryManager {

  def apply(conf: SparkConf, numCores: Int): UnifiedMemoryManager = {
    val maxMemory = getMaxMemory(conf)
    new UnifiedMemoryManager(
      conf,
      maxMemory = maxMemory,
      minimumStoragePoolSize =
        (maxMemory * conf.getDouble("spark.memory.storageFraction", 0.5)).toLong,
      numCores = numCores)
  }

  /**
   * Return the total amount of memory shared between execution and storage, in bytes.
   */
  private def getMaxMemory(conf: SparkConf): Long = {
    val systemMaxMemory = conf.getLong("spark.testing.memory", Runtime.getRuntime.maxMemory)
    val memoryFraction = conf.getDouble("spark.memory.fraction", 0.75)
    (systemMaxMemory * memoryFraction).toLong
  }
}
