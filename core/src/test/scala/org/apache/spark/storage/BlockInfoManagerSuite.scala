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

package org.apache.spark.storage

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions

import org.scalatest.BeforeAndAfterEach
import org.scalatest.time.SpanSugar._

import org.apache.spark.{SparkFunSuite, TaskContext, TaskContextImpl}


class BlockInfoManagerSuite extends SparkFunSuite with BeforeAndAfterEach {

  private implicit val ec = ExecutionContext.global
  private var blockInfoManager: BlockInfoManager = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    blockInfoManager = new BlockInfoManager()
  }

  override protected def afterEach(): Unit = {
    blockInfoManager = null
    super.afterEach()
  }

  private implicit def stringToBlockId(str: String): BlockId = {
    TestBlockId(str)
  }

  private def newBlockInfo(): BlockInfo = {
    new BlockInfo(StorageLevel.MEMORY_ONLY, tellMaster = false)
  }

  private def withTaskId[T](taskAttemptId: Long)(block: => T): T = {
    try {
      TaskContext.setTaskContext(new TaskContextImpl(0, 0, taskAttemptId, 0, null, null))
      block
    } finally {
      TaskContext.unset()
    }
  }

  test("initial memory usage") {
    assert(blockInfoManager.getNumberOfMapEntries === 0)
    assert(blockInfoManager.size === 0)
  }

  test("get non-existent block") {
    assert(blockInfoManager.get("non-existent-block").isEmpty)
    assert(blockInfoManager.getAndLockForReading("non-existent-block").isEmpty)
    assert(blockInfoManager.getAndLockForWriting("non-existent-block").isEmpty)
  }

  test("basic putAndLockForWritingIfAbsent") {
    val blockInfo = newBlockInfo()
    withTaskId(1) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", blockInfo))
      assert(blockInfoManager.get("block").get eq blockInfo)
      assert(!blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
      assert(blockInfoManager.get("block").get eq blockInfo)
      assert(blockInfo.readerCount === 0)
      assert(blockInfo.writerTask === 1)
      blockInfoManager.releaseLock("block")
      assert(blockInfo.readerCount === 0)
      assert(blockInfo.writerTask === -1)
    }
    assert(blockInfoManager.size === 1)
    assert(blockInfoManager.getNumberOfMapEntries === 1)
  }

  test("read locks are reentrant") {
    withTaskId(1) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
      blockInfoManager.releaseLock("block")
      assert(blockInfoManager.getAndLockForReading("block").isDefined)
      assert(blockInfoManager.getAndLockForReading("block").isDefined)
      assert(blockInfoManager.get("block").get.readerCount === 2)
      assert(blockInfoManager.get("block").get.writerTask === -1)
      blockInfoManager.releaseLock("block")
      assert(blockInfoManager.get("block").get.readerCount === 1)
      blockInfoManager.releaseLock("block")
      assert(blockInfoManager.get("block").get.readerCount === 0)
    }
  }

  test("multiple tasks can hold read locks") {
    withTaskId(0) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
      blockInfoManager.releaseLock("block")
    }
    withTaskId(1) { assert(blockInfoManager.getAndLockForReading("block").isDefined) }
    withTaskId(2) { assert(blockInfoManager.getAndLockForReading("block").isDefined) }
    withTaskId(3) { assert(blockInfoManager.getAndLockForReading("block").isDefined) }
    withTaskId(4) { assert(blockInfoManager.getAndLockForReading("block").isDefined) }
    assert(blockInfoManager.get("block").get.readerCount === 4)
  }

  test("single task can hold write lock") {
    withTaskId(0) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
      blockInfoManager.releaseLock("block")
    }
    withTaskId(1) {
      assert(blockInfoManager.getAndLockForWriting("block").isDefined)
      assert(blockInfoManager.get("block").get.writerTask === 1)
    }
    withTaskId(2) {
      assert(blockInfoManager.getAndLockForWriting("block", blocking = false).isEmpty)
      assert(blockInfoManager.get("block").get.writerTask === 1)
    }
  }

  test("downgrade lock") {
    withTaskId(0) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
      blockInfoManager.downgradeLock("block")
    }
    withTaskId(1) {
      assert(blockInfoManager.getAndLockForReading("block").isDefined)
    }
    assert(blockInfoManager.get("block").get.readerCount === 2)
    assert(blockInfoManager.get("block").get.writerTask === -1)
  }

  test("write lock will block readers") {
    withTaskId(0) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
    }
    val get1Future = Future {
      withTaskId(1) {
        blockInfoManager.getAndLockForReading("block")
      }
    }
    val get2Future = Future {
      withTaskId(2) {
        blockInfoManager.getAndLockForReading("block")
      }
    }
    Thread.sleep(300)  // Hack to try to ensure that both future tasks are waiting
    withTaskId(0) {
      blockInfoManager.releaseLock("block")
    }
    assert(Await.result(get1Future, 1.seconds).isDefined)
    assert(Await.result(get2Future, 1.seconds).isDefined)
    assert(blockInfoManager.get("block").get.readerCount === 2)
  }

  test("read locks will block writer") {
    withTaskId(0) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
      blockInfoManager.releaseLock("block")
      blockInfoManager.getAndLockForReading("block")
    }
    val write1Future = Future {
      withTaskId(1) {
        blockInfoManager.getAndLockForWriting("block")
      }
    }
    val write2Future = Future {
      withTaskId(2) {
        blockInfoManager.getAndLockForWriting("block")
      }
    }
    Thread.sleep(300)  // Hack to try to ensure that both future tasks are waiting
    withTaskId(0) {
      blockInfoManager.releaseLock("block")
    }
    assert(
      Await.result(Future.firstCompletedOf(Seq(write1Future, write2Future)), 1.seconds).isDefined)
    val firstWriteWinner = if (write1Future.isCompleted) 1 else 2
    withTaskId(firstWriteWinner) {
      blockInfoManager.releaseLock("block")
    }
    assert(Await.result(write1Future, 1.seconds).isDefined)
    assert(Await.result(write2Future, 1.seconds).isDefined)
  }

  test("removing a block causes blocked callers to receive None") {
    withTaskId(0) {
      assert(blockInfoManager.putAndLockForWritingIfAbsent("block", newBlockInfo()))
    }
    val getFuture = Future {
      withTaskId(1) {
        blockInfoManager.getAndLockForReading("block")
      }
    }
    val writeFuture = Future {
      withTaskId(2) {
        blockInfoManager.getAndLockForWriting("block")
      }
    }
    Thread.sleep(300)  // Hack to try to ensure that both future tasks are waiting
    withTaskId(0) {
      blockInfoManager.remove("block")
    }
    assert(Await.result(getFuture, 1.seconds).isEmpty)
    assert(Await.result(writeFuture, 1.seconds).isEmpty)
    assert(blockInfoManager.getNumberOfMapEntries === 0)
    assert(blockInfoManager.size === 0)
  }
}