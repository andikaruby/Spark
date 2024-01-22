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

package org.apache.spark.sql.execution.python

import java.io.DataOutputStream
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext

import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.ipc.ArrowStreamWriter

import org.apache.spark.TaskContext
import org.apache.spark.api.python.{BasePythonRunner, ChainedPythonFunctions}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.execution.arrow
import org.apache.spark.sql.execution.arrow.ArrowWriter
import org.apache.spark.sql.execution.metric.SQLMetric
import org.apache.spark.sql.execution.python.TransformWithStateInPandasPythonRunner.{InType, OutType}
import org.apache.spark.sql.execution.streaming.StatefulProcessorHandleImpl
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.vectorized.ColumnarBatch
import org.apache.spark.util.ThreadUtils

class TransformWithStateInPandasPythonRunner(
    funcs: Seq[(ChainedPythonFunctions, Long)],
    evalType: Int,
    argOffsets: Array[Array[Int]],
    _schema: StructType,
    processorHandle: StatefulProcessorHandleImpl,
    _timeZoneId: String,
    initialWorkerConf: Map[String, String],
    override val pythonMetrics: Map[String, SQLMetric],
    jobArtifactUUID: Option[String])
  extends BasePythonRunner[InType, OutType](funcs.map(_._1), evalType, argOffsets, jobArtifactUUID)
    with PythonArrowInput[InType]
    with BasicPythonArrowOutput
    with Logging {

  private val sqlConf = SQLConf.get
  private val arrowMaxRecordsPerBatch = sqlConf.arrowMaxRecordsPerBatch

  private var stateSocketSocketPort: Int = 0

  override protected val workerConf: Map[String, String] = initialWorkerConf +
    (SQLConf.ARROW_EXECUTION_MAX_RECORDS_PER_BATCH.key -> arrowMaxRecordsPerBatch.toString)

  private val arrowWriter: arrow.ArrowWriter = ArrowWriter.create(root)

  // Use lazy val to initialize the fields before these are accessed in [[PythonArrowInput]]'s
  // constructor.
  override protected lazy val schema: StructType = _schema
  override protected lazy val timeZoneId: String = _timeZoneId
  override protected val errorOnDuplicatedFieldNames: Boolean = true
  override protected val largeVarTypes: Boolean = sqlConf.arrowUseLargeVarTypes

  override protected def handleMetadataBeforeExec(stream: DataOutputStream): Unit = {
    super.handleMetadataBeforeExec(stream)
    // Also write the port number for state server
    stream.writeInt(stateSocketSocketPort)
  }

  override def compute(
      inputIterator: Iterator[InType],
      partitionIndex: Int,
      context: TaskContext): Iterator[OutType] = {
    // var stateServerSocketChannel: ServerSocketChannel = null
    var stateServerSocket: ServerSocket = null

    var failed = false
    try {
      stateServerSocket = new ServerSocket( /* port = */0,
        /* backlog = */1)
      stateSocketSocketPort = stateServerSocket.getLocalPort
      logWarning(s"opened socket on $stateSocketSocketPort")
    } catch {
      case e: Exception =>
        failed = true
        throw e
    } finally {
      if (failed) {
        closeServerSocketChannelSilently(stateServerSocket)
      }
    }

    val executor = ThreadUtils.newDaemonSingleThreadExecutor("stateConnectionListenerThread")
    val executionContext = ExecutionContext.fromExecutor(executor)

    executionContext.execute(
      new TransformWithStateInPandasStateServer(stateServerSocket, processorHandle))

    context.addTaskCompletionListener[Unit] { _ =>
      logWarning(s"completion listener called")
      executor.awaitTermination(10, TimeUnit.SECONDS)
      executor.shutdownNow()
      closeServerSocketChannelSilently(stateServerSocket)
    }

    super.compute(inputIterator, partitionIndex, context)
  }

  private def closeServerSocketChannelSilently(stateServerSocket: ServerSocket): Unit = {
    try {
      logWarning(s"closing the state server socket")
      stateServerSocket.close()
    } catch {
      case e: Exception =>
        logError(s"failed to close state server socket", e)
    }
  }

  override protected def writeUDF(dataOut: DataOutputStream): Unit = {
    PythonUDFRunner.writeUDFs(dataOut, funcs, argOffsets, None)
  }

  override protected def writeNextInputToArrowStream(
      root: VectorSchemaRoot,
      writer: ArrowStreamWriter,
      dataOut: DataOutputStream,
      inputIterator: Iterator[InType]): Boolean = {

    if (inputIterator.hasNext) {
      val startData = dataOut.size()
      val next = inputIterator.next()
      val nextBatch = next._2

      while (nextBatch.hasNext) {
        arrowWriter.write(nextBatch.next())
      }

      arrowWriter.finish()
      writer.writeBatch()
      arrowWriter.reset()
      val deltaData = dataOut.size() - startData
      pythonMetrics("pythonDataSent") += deltaData
      true
    } else {
      super[PythonArrowInput].close()
      false
    }
  }
}

object TransformWithStateInPandasPythonRunner {
  type InType = (InternalRow, Iterator[InternalRow])
  type OutType = ColumnarBatch
}

