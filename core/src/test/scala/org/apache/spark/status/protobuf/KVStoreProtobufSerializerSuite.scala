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

package org.apache.spark.status.protobuf

import java.util.Date

import org.apache.spark.{JobExecutionStatus, SparkFunSuite}
import org.apache.spark.executor.ExecutorMetrics
import org.apache.spark.metrics.ExecutorMetricType
import org.apache.spark.status.{ExecutorStageSummaryWrapper, JobDataWrapper, TaskDataWrapper}
import org.apache.spark.status.api.v1.{AccumulableInfo, ExecutorStageSummary, JobData}

class KVStoreProtobufSerializerSuite extends SparkFunSuite {
  private val serializer = new KVStoreProtobufSerializer()

  test("Job data") {
    val input = new JobDataWrapper(
      new JobData(
        jobId = 1,
        name = "test",
        description = Some("test description"),
        submissionTime = Some(new Date(123456L)),
        completionTime = Some(new Date(654321L)),
        stageIds = Seq(1, 2, 3, 4),
        jobGroup = Some("group"),
        status = JobExecutionStatus.UNKNOWN,
        numTasks = 2,
        numActiveTasks = 3,
        numCompletedTasks = 4,
        numSkippedTasks = 5,
        numFailedTasks = 6,
        numKilledTasks = 7,
        numCompletedIndices = 8,
        numActiveStages = 9,
        numCompletedStages = 10,
        numSkippedStages = 11,
        numFailedStages = 12,
        killedTasksSummary = Map("a" -> 1, "b" -> 2)),
      Set(1, 2),
      Some(999)
    )

    val bytes = serializer.serialize(input)
    val result = serializer.deserialize(bytes, classOf[JobDataWrapper])
    assert(result.info.jobId == input.info.jobId)
    assert(result.info.description == input.info.description)
    assert(result.info.submissionTime == input.info.submissionTime)
    assert(result.info.completionTime == input.info.completionTime)
    assert(result.info.stageIds == input.info.stageIds)
    assert(result.info.jobGroup == input.info.jobGroup)
    assert(result.info.status == input.info.status)
    assert(result.info.numTasks == input.info.numTasks)
    assert(result.info.numActiveTasks == input.info.numActiveTasks)
    assert(result.info.numCompletedTasks == input.info.numCompletedTasks)
    assert(result.info.numSkippedTasks == input.info.numSkippedTasks)
    assert(result.info.numFailedTasks == input.info.numFailedTasks)
    assert(result.info.numKilledTasks == input.info.numKilledTasks)
    assert(result.info.numCompletedIndices == input.info.numCompletedIndices)
    assert(result.info.numActiveStages == input.info.numActiveStages)
    assert(result.info.numCompletedStages == input.info.numCompletedStages)
    assert(result.info.numSkippedStages == input.info.numSkippedStages)
    assert(result.info.numFailedStages == input.info.numFailedStages)
    assert(result.info.killedTasksSummary == input.info.killedTasksSummary)
    assert(result.skippedStages == input.skippedStages)
    assert(result.sqlExecutionId == input.sqlExecutionId)
  }

  test("Task Data") {
    val accumulatorUpdates = Seq(
      new AccumulableInfo(1L, "duration", Some("update"), "value1"),
      new AccumulableInfo(2L, "duration2", None, "value2")
    )
    val input = new TaskDataWrapper(
      taskId = 1,
      index = 2,
      attempt = 3,
      partitionId = 4,
      launchTime = 5L,
      resultFetchStart = 6L,
      duration = 10000L,
      executorId = "executor_id_1",
      host = "host_name",
      status = "SUCCESS",
      taskLocality = "LOCAL",
      speculative = true,
      accumulatorUpdates = accumulatorUpdates,
      errorMessage = Some("error"),
      hasMetrics = true,
      executorDeserializeTime = 7L,
      executorDeserializeCpuTime = 8L,
      executorRunTime = 9L,
      executorCpuTime = 10L,
      resultSize = 11L,
      jvmGcTime = 12L,
      resultSerializationTime = 13L,
      memoryBytesSpilled = 14L,
      diskBytesSpilled = 15L,
      peakExecutionMemory = 16L,
      inputBytesRead = 17L,
      inputRecordsRead = 18L,
      outputBytesWritten = 19L,
      outputRecordsWritten = 20L,
      shuffleRemoteBlocksFetched = 21L,
      shuffleLocalBlocksFetched = 22L,
      shuffleFetchWaitTime = 23L,
      shuffleRemoteBytesRead = 24L,
      shuffleRemoteBytesReadToDisk = 25L,
      shuffleLocalBytesRead = 26L,
      shuffleRecordsRead = 27L,
      shuffleBytesWritten = 28L,
      shuffleWriteTime = 29L,
      shuffleRecordsWritten = 30L,
      stageId = 31,
      stageAttemptId = 32)

    val bytes = serializer.serialize(input)
    val result = serializer.deserialize(bytes, classOf[TaskDataWrapper])
    assert(result.accumulatorUpdates.length == input.accumulatorUpdates.length)
    result.accumulatorUpdates.zip(input.accumulatorUpdates).foreach { case (a1, a2) =>
      assert(a1.id == a2.id)
      assert(a1.name == a2.name)
      assert(a1.update.getOrElse("") == a2.update.getOrElse(""))
      assert(a1.update == a2.update)
    }
    assert(result.taskId == input.taskId)
    assert(result.index == input.index)
    assert(result.attempt == input.attempt)
    assert(result.partitionId == input.partitionId)
    assert(result.launchTime == input.launchTime)
    assert(result.resultFetchStart == input.resultFetchStart)
    assert(result.duration == input.duration)
    assert(result.executorId == input.executorId)
    assert(result.host == input.host)
    assert(result.status == input.status)
    assert(result.taskLocality == input.taskLocality)
    assert(result.speculative == input.speculative)
    assert(result.errorMessage == input.errorMessage)
    assert(result.hasMetrics == input.hasMetrics)
    assert(result.executorDeserializeTime == input.executorDeserializeTime)
    assert(result.executorDeserializeCpuTime == input.executorDeserializeCpuTime)
    assert(result.executorRunTime == input.executorRunTime)
    assert(result.executorCpuTime == input.executorCpuTime)
    assert(result.resultSize == input.resultSize)
    assert(result.jvmGcTime == input.jvmGcTime)
    assert(result.resultSerializationTime == input.resultSerializationTime)
    assert(result.memoryBytesSpilled == input.memoryBytesSpilled)
    assert(result.diskBytesSpilled == input.diskBytesSpilled)
    assert(result.peakExecutionMemory == input.peakExecutionMemory)
    assert(result.inputBytesRead == input.inputBytesRead)
    assert(result.inputRecordsRead == input.inputRecordsRead)
    assert(result.outputBytesWritten == input.outputBytesWritten)
    assert(result.outputRecordsWritten == input.outputRecordsWritten)
    assert(result.shuffleRemoteBlocksFetched == input.shuffleRemoteBlocksFetched)
    assert(result.shuffleLocalBlocksFetched == input.shuffleLocalBlocksFetched)
    assert(result.shuffleFetchWaitTime == input.shuffleFetchWaitTime)
    assert(result.shuffleRemoteBytesRead == input.shuffleRemoteBytesRead)
    assert(result.shuffleRemoteBytesReadToDisk == input.shuffleRemoteBytesReadToDisk)
    assert(result.shuffleLocalBytesRead == input.shuffleLocalBytesRead)
    assert(result.shuffleRecordsRead == input.shuffleRecordsRead)
    assert(result.shuffleBytesWritten == input.shuffleBytesWritten)
    assert(result.shuffleWriteTime == input.shuffleWriteTime)
    assert(result.shuffleRecordsWritten == input.shuffleRecordsWritten)
    assert(result.stageId == input.stageId)
    assert(result.stageAttemptId == input.stageAttemptId)
  }

  test("Executor Stage Summary") {
    val peakMemoryMetrics =
      Some(new ExecutorMetrics(Array(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 1024L)))
    val info = new ExecutorStageSummary(
      taskTime = 1L,
      failedTasks = 2,
      succeededTasks = 3,
      killedTasks = 4,
      inputBytes = 5L,
      inputRecords = 6L,
      outputBytes = 7L,
      outputRecords = 8L,
      shuffleRead = 9L,
      shuffleReadRecords = 10L,
      shuffleWrite = 11L,
      shuffleWriteRecords = 12L,
      memoryBytesSpilled = 13L,
      diskBytesSpilled = 14L,
      isBlacklistedForStage = true,
      peakMemoryMetrics = peakMemoryMetrics,
      isExcludedForStage = false)
    val input = new ExecutorStageSummaryWrapper(
      stageId = 1,
      stageAttemptId = 2,
      executorId = "executor_id_1",
      info = info)
    val bytes = serializer.serialize(input)
    val result = serializer.deserialize(bytes, classOf[ExecutorStageSummaryWrapper])
    assert(result.stageId == input.stageId)
    assert(result.stageAttemptId == input.stageAttemptId)
    assert(result.executorId == input.executorId)
    assert(result.info.taskTime == input.info.taskTime)
    assert(result.info.failedTasks == input.info.failedTasks)
    assert(result.info.succeededTasks == input.info.succeededTasks)
    assert(result.info.killedTasks == input.info.killedTasks)
    assert(result.info.inputBytes == input.info.inputBytes)
    assert(result.info.inputRecords == input.info.inputRecords)
    assert(result.info.outputBytes == input.info.outputBytes)
    assert(result.info.outputRecords == input.info.outputRecords)
    assert(result.info.shuffleRead == input.info.shuffleRead)
    assert(result.info.shuffleReadRecords == input.info.shuffleReadRecords)
    assert(result.info.shuffleWrite == input.info.shuffleWrite)
    assert(result.info.shuffleWriteRecords == input.info.shuffleWriteRecords)
    assert(result.info.memoryBytesSpilled == input.info.memoryBytesSpilled)
    assert(result.info.diskBytesSpilled == input.info.diskBytesSpilled)
    assert(result.info.isBlacklistedForStage == input.info.isBlacklistedForStage)
    assert(result.info.isExcludedForStage == input.info.isExcludedForStage)
    assert(result.info.peakMemoryMetrics.isDefined)
    ExecutorMetricType.metricToOffset.foreach { case (name, index) =>
      result.info.peakMemoryMetrics.get.getMetricValue(name) ==
        input.info.peakMemoryMetrics.get.getMetricValue(name)
    }
  }
}
