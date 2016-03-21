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

import javax.annotation.concurrent.GuardedBy

import org.apache.spark.internal.Logging
import org.apache.spark.storage.BlockId
import org.apache.spark.storage.memory.MemoryStore

/**
 * Performs bookkeeping for managing an adjustable-size pool of memory that is used for storage
 * (caching).
 *
 * @param lock a [[MemoryManager]] instance to synchronize on
 */
private[memory] class StorageMemoryPool(lock: Object) extends MemoryPool(lock) with Logging {

  @GuardedBy("lock")
  private[this] var _memoryUsed: Long = 0L

  override def memoryUsed: Long = lock.synchronized {
    _memoryUsed
  }

  private var _memoryStore: MemoryStore = _
  def memoryStore: MemoryStore = {
    if (_memoryStore == null) {
      throw new IllegalStateException("memory store not initialized yet")
    }
    _memoryStore
  }

  /**
   * Set the [[MemoryStore]] used by this manager to evict cached blocks.
   * This must be set after construction due to initialization ordering constraints.
   */
  final def setMemoryStore(store: MemoryStore): Unit = {
    _memoryStore = store
  }

  /**
   * Acquire N bytes of memory to cache the given block, evicting existing ones if necessary.
   *
   * @return whether all N bytes were successfully granted.
   */
  def acquireMemory(blockId: BlockId, numBytes: Long): Boolean = {
    val numBytesToFree = lock.synchronized {
      math.max(0, numBytes - memoryFree)
    }
    acquireMemory(blockId, numBytes, numBytesToFree)
  }

  /**
   * Acquire N bytes of storage memory for the given block, evicting existing ones if necessary.
   *
   * @param blockId the ID of the block we are acquiring storage memory for
   * @param numBytesToAcquire the size of this block
   * @param numBytesToFree the amount of space to be freed through evicting blocks
   * @return whether all N bytes were successfully granted.
   */
  def acquireMemory(
      blockId: BlockId,
      numBytesToAcquire: Long,
      numBytesToFree: Long): Boolean = {
    assert(numBytesToAcquire >= 0)
    assert(numBytesToFree >= 0)
    assert(lock.synchronized(memoryUsed <= poolSize))
    val memoryFreedByEviction = if (numBytesToFree > 0) {
      memoryStore.evictBlocksToFreeSpace(Some(blockId), numBytesToFree)
    } else {
      0
    }
    if (memoryFreedByEviction > numBytesToAcquire) {
      releaseMemory(memoryFreedByEviction - numBytesToAcquire)
    }
    val numBytesToAcquireAfterEviction = Math.max(0L, numBytesToAcquire - memoryFreedByEviction)
    lock.synchronized {
      // NOTE: If the memory store evicts blocks, then those evictions will synchronously call
      // back into this StorageMemoryPool in order to free memory. Therefore, these variables
      // should have been updated.
      val enoughMemory = numBytesToAcquireAfterEviction <= memoryFree
      if (enoughMemory) {
        _memoryUsed += numBytesToAcquireAfterEviction
      } else {
        releaseMemory(memoryFreedByEviction)
      }
      enoughMemory
    }
  }

  def releaseMemory(size: Long): Unit = lock.synchronized {
    if (size > _memoryUsed) {
      logWarning(s"Attempted to release $size bytes of storage " +
        s"memory when we only have ${_memoryUsed} bytes")
      _memoryUsed = 0
    } else {
      _memoryUsed -= size
    }
  }

  def releaseAllMemory(): Unit = lock.synchronized {
    _memoryUsed = 0
  }

  /**
   * Try to shrink the size of this storage memory pool by `spaceToFree` bytes. Return the number
   * of bytes removed from the pool's capacity.
   */
  def shrinkPoolToFreeSpace(spaceToFree: Long): Long = {
    var spaceFreedByReleasingUnusedMemory = 0L
    val remainingSpaceToFree = lock.synchronized {
      // First, shrink the pool by reclaiming free memory:
      spaceFreedByReleasingUnusedMemory = math.min(spaceToFree, memoryFree)
      decrementPoolSize(spaceFreedByReleasingUnusedMemory)
      spaceToFree - spaceFreedByReleasingUnusedMemory
    }
    if (remainingSpaceToFree > 0) {
      // If reclaiming free memory did not adequately shrink the pool, begin evicting blocks:
      val spaceFreedByEviction = memoryStore.evictBlocksToFreeSpace(None, remainingSpaceToFree)
      lock.synchronized {
        releaseMemory(spaceFreedByEviction)
        decrementPoolSize(spaceFreedByEviction)
      }
      spaceFreedByReleasingUnusedMemory + spaceFreedByEviction
    } else {
      spaceFreedByReleasingUnusedMemory
    }
  }
}
