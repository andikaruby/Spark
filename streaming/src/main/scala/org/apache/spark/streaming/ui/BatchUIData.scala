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


package org.apache.spark.streaming.ui

import scala.collection.mutable

import org.apache.spark.streaming.Time
import org.apache.spark.streaming.scheduler.{BatchInfo, OutputOperationInfo, StreamInputInfo}
import org.apache.spark.streaming.ui.StreamingJobProgressListener._

private[ui] case class OutputOpIdAndSparkJobId(outputOpId: OutputOpId, sparkJobId: SparkJobId)

private[ui] case class BatchUIData(
    val batchTime: Time,
    val streamIdToInputInfo: Map[Int, StreamInputInfo],
    val submissionTime: Long,
    val processingStartTime: Option[Long],
    val processingEndTime: Option[Long],
    val numOutputOp: Int,
    var outputOperations: mutable.HashMap[OutputOpId, OutputOperationUIData] = mutable.HashMap(),
    var outputOpIdSparkJobIdPairs: Seq[OutputOpIdAndSparkJobId] = Seq.empty) {

  /**
   * Time taken for the first job of this batch to start processing from the time this batch
   * was submitted to the streaming scheduler. Essentially, it is
   * `processingStartTime` - `submissionTime`.
   */
  def schedulingDelay: Option[Long] = processingStartTime.map(_ - submissionTime)

  /**
   * Time taken for the all jobs of this batch to finish processing from the time they started
   * processing. Essentially, it is `processingEndTime` - `processingStartTime`.
   */
  def processingDelay: Option[Long] = {
    for (start <- processingStartTime;
         end <- processingEndTime)
      yield end - start
  }

  /**
   * Time taken for all the jobs of this batch to finish processing from the time they
   * were submitted.  Essentially, it is `processingDelay` + `schedulingDelay`.
   */
  def totalDelay: Option[Long] = processingEndTime.map(_ - submissionTime)

  /**
   * The number of recorders received by the receivers in this batch.
   */
  def numRecords: Long = streamIdToInputInfo.values.map(_.numRecords).sum

  /**
   * Update an output operation information of this batch.
   */
  def updateOutputOperationInfo(outputOperationInfo: OutputOperationInfo): Unit = {
    assert(batchTime == outputOperationInfo.batchTime)
    if (outputOperationInfo.startTime.nonEmpty) {
      // This is a start info
      assert(!outputOperations.contains(outputOperationInfo.id))
      outputOperations(outputOperationInfo.id) =
        OutputOperationUIData(
          outputOperationInfo.id,
          outputOperationInfo.name,
          outputOperationInfo.description,
          outputOperationInfo.startTime,
          None,
          None)
    } else {
      // This is a completed info, we only need endTime and failureReason
      assert(outputOperations.contains(outputOperationInfo.id))
      outputOperations(outputOperationInfo.id).endTime = outputOperationInfo.endTime
      outputOperations(outputOperationInfo.id).failureReason = outputOperationInfo.failureReason
    }
  }

  /**
   * Return the number of failed output operations.
   */
  def numFailedOutputOp: Int = outputOperations.values.count(_.failureReason.nonEmpty)

  /**
   * Return the number of running output operations.
   */
  def numActiveOutputOp: Int = outputOperations.values.count(_.endTime.isEmpty)

  /**
   * Return the number of completed output operations.
   */
  def numCompletedOutputOp: Int = outputOperations.values.count {
      op => op.failureReason.isEmpty && op.endTime.nonEmpty
    }

}

private[ui] object BatchUIData {

  def apply(batchInfo: BatchInfo): BatchUIData = {
    new BatchUIData(
      batchInfo.batchTime,
      batchInfo.streamIdToInputInfo,
      batchInfo.submissionTime,
      batchInfo.processingStartTime,
      batchInfo.processingEndTime,
      batchInfo.numOutputOp
    )
  }
}

private[ui] case class OutputOperationUIData(
    id: OutputOpId,
    name: String,
    description: String,
    startTime: Option[Long],
    var endTime: Option[Long],
    var failureReason: Option[String]) {

  def duration: Option[Long] = for (s <- startTime; e <- endTime) yield e - s
}
