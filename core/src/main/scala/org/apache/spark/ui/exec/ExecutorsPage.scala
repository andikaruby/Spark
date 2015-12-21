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

package org.apache.spark.ui.exec

import java.net.URLEncoder
import javax.servlet.http.HttpServletRequest

import scala.xml.Node

import org.apache.spark.status.api.v1.ExecutorSummary
import org.apache.spark.ui.{ToolTips, UIUtils, WebUIPage}
import org.apache.spark.util.Utils

// This isn't even used anymore -- but we need to keep it b/c of a MiMa false positive
private[ui] case class ExecutorSummaryInfo(
    id: String,
    hostPort: String,
    rddBlocks: Int,
    memoryUsed: Long,
    diskUsed: Long,
    activeTasks: Int,
    failedTasks: Int,
    completedTasks: Int,
    totalTasks: Int,
    totalDuration: Long,
    totalInputBytes: Long,
    totalShuffleRead: Long,
    totalShuffleWrite: Long,
    maxMemory: Long,
    executorLogs: Map[String, String])


private[ui] class ExecutorsPage(
    parent: ExecutorsTab,
    threadDumpEnabled: Boolean)
  extends WebUIPage("") {
  private val listener = parent.listener

  def render(request: HttpServletRequest): Seq[Node] = {
    val storageStatusList = listener.storageStatusList
    val maxMem = storageStatusList.map(_.maxMem).sum
    val memUsed = storageStatusList.map(_.memUsed).sum
    val diskUsed = storageStatusList.map(_.diskUsed).sum
    val execInfo = for (statusId <- 0 until storageStatusList.size) yield
      ExecutorsPage.getExecInfo(listener, statusId)
    val execInfoSorted = execInfo.sortBy(_.id)
    val logsExist = execInfo.filter(_.executorLogs.nonEmpty).nonEmpty

    val execTable =
      <table class={UIUtils.TABLE_CLASS_STRIPED_SORTABLE}>
        <thead>
          <th>Executor ID</th>
          <th>Address</th>
          <th>RDD Blocks</th>
          <th><span data-toggle="tooltip" title={ToolTips.STORAGE_MEMORY}>Storage Memory</span></th>
          <th>Disk Used</th>
          <th>Active Tasks</th>
          <th>Failed Tasks</th>
          <th>Complete Tasks</th>
          <th>Total Tasks</th>
          <th>Task Time</th>
          <th><span data-toggle="tooltip" title={ToolTips.INPUT}>Input</span></th>
          <th><span data-toggle="tooltip" title={ToolTips.SHUFFLE_READ}>Shuffle Read</span></th>
          <th>
            <!-- Place the shuffle write tooltip on the left (rather than the default position
              of on top) because the shuffle write column is the last column on the right side and
              the tooltip is wider than the column, so it doesn't fit on top. -->
            <span data-toggle="tooltip" data-placement="left" title={ToolTips.SHUFFLE_WRITE}>
              Shuffle Write
            </span>
          </th>
          {if (logsExist) <th class="sorttable_nosort">Logs</th> else Seq.empty}
          {if (threadDumpEnabled) <th class="sorttable_nosort">Thread Dump</th> else Seq.empty}
        </thead>
        <tbody>
          {execInfoSorted.map(execRow(_, logsExist))}
        </tbody>
      </table>

    val content =
      <div class="row-fluid">
        <div class="span12">
          <ul class="unstyled">
            <li><strong>Memory:</strong>
              {Utils.bytesToString(memUsed)} Used
              ({Utils.bytesToString(maxMem)} Total) </li>
            <li><strong>Disk:</strong> {Utils.bytesToString(diskUsed)} Used </li>
          </ul>
        </div>
      </div>
      <div class = "row">
        <div class="span12">
          {execTable}
        </div>
      </div>;

    UIUtils.headerSparkPage("Executors (" + execInfo.size + ")", content, parent)
  }

  /** Render an HTML row representing an executor */
  private def execRow(info: ExecutorSummary, logsExist: Boolean): Seq[Node] = {
    val maximumMemory = info.maxMemory
    val memoryUsed = info.memoryUsed
    val diskUsed = info.diskUsed

    // Determine Color Opacity from 0.5-1
    // activeTasks range from 0 to all cores
    val activeTasksAlpha =
      if (info.totalCores > 0) {
        (info.activeTasks.toDouble / info.totalCores) * 0.5 + 0.5
      } else {
        1
      }
    // failedTasks range max at 10% failure, alpha max = 1
    // completedTasks range ignores 90% of tasks
    val (failedTasksAlpha, completedTasksAlpha) =
      if (info.totalTasks > 0) {
        (math.min(10 * info.failedTasks.toDouble / info.totalTasks, 1) * 0.5 + 0.5,
        math.max(((10 * info.completedTasks.toDouble / info.totalTasks) - 9) * 0.5 + 0.5, 0.5))
      } else {
        (1, 1)
      }
    // totalDuration range from 0 to 50% GC time, alpha max = 1
    val totalDurationAlpha =
      if (info.totalDuration > 0) {
        math.min(info.totalGCTime.toDouble / info.totalDuration + 0.5, 1)
      } else {
        1
      }

    <tr>
      <td>{info.id}</td>
      <td>{info.hostPort}</td>
      <td>{info.rddBlocks}</td>
      <td sorttable_customkey={memoryUsed.toString}>
        {Utils.bytesToString(memoryUsed)} /
        {Utils.bytesToString(maximumMemory)}
      </td>
      <td sorttable_customkey={diskUsed.toString}>
        {Utils.bytesToString(diskUsed)}
      </td>
      <td style={
        if (info.activeTasks > 0) {
          "background:hsla(240, 100%, 50%, " + activeTasksAlpha + ");color:white"
        } else {
          ""
        }
      }>{info.activeTasks}</td>
      <td style={
        if (info.failedTasks > 0) {
          "background:hsla(0, 100%, 50%, " + failedTasksAlpha + ");color:white"
        } else {
          ""
        }
      }>{info.failedTasks}</td>
      <td style={
        if (info.completedTasks > 0 && (info.activeTasks > 0 || info.failedTasks > 0)) {
          "background:hsla(120, 100%, 25%, " + completedTasksAlpha + ");color:white"
        } else {
          ""
        }
      }>{info.completedTasks}</td>
      <td>{info.totalTasks}</td>
      <td sorttable_customkey={info.totalDuration.toString} style={
        // Red if GC time over 10%
        if (10 * info.totalGCTime > info.totalDuration) {
          "background:hsla(0, 100%, 50%, " + totalDurationAlpha + ");color:white"
        } else {
          ""
        }
      }>
        {Utils.msDurationToString(info.totalDuration)}
      </td>
      <td sorttable_customkey={info.totalInputBytes.toString}>
        {Utils.bytesToString(info.totalInputBytes)}
      </td>
      <td sorttable_customkey={info.totalShuffleRead.toString}>
        {Utils.bytesToString(info.totalShuffleRead)}
      </td>
      <td sorttable_customkey={info.totalShuffleWrite.toString}>
        {Utils.bytesToString(info.totalShuffleWrite)}
      </td>
      {
        if (logsExist) {
          <td>
            {
              info.executorLogs.map { case (logName, logUrl) =>
                <div>
                  <a href={logUrl}>
                    {logName}
                  </a>
                </div>
              }
            }
          </td>
        }
      }
      {
        if (threadDumpEnabled) {
          val encodedId = URLEncoder.encode(info.id, "UTF-8")
          <td>
            <a href={s"threadDump/?executorId=${encodedId}"}>Thread Dump</a>
          </td>
        } else {
          Seq.empty
        }
      }
    </tr>
  }

}

private[spark] object ExecutorsPage {
  /** Represent an executor's info as a map given a storage status index */
  def getExecInfo(listener: ExecutorsListener, statusId: Int): ExecutorSummary = {
    val status = listener.storageStatusList(statusId)
    val execId = status.blockManagerId.executorId
    val hostPort = status.blockManagerId.hostPort
    val rddBlocks = status.numBlocks
    val memUsed = status.memUsed
    val maxMem = status.maxMem
    val diskUsed = status.diskUsed
    val totalCores = listener.executorToTotalCores.getOrElse(execId, 0)
    val activeTasks = listener.executorToTasksActive.getOrElse(execId, 0)
    val failedTasks = listener.executorToTasksFailed.getOrElse(execId, 0)
    val completedTasks = listener.executorToTasksComplete.getOrElse(execId, 0)
    val totalTasks = activeTasks + failedTasks + completedTasks
    val totalDuration = listener.executorToDuration.getOrElse(execId, 0L)
    val totalGCTime = listener.executorToJvmGCTime.getOrElse(execId, 0L)
    val totalInputBytes = listener.executorToInputBytes.getOrElse(execId, 0L)
    val totalShuffleRead = listener.executorToShuffleRead.getOrElse(execId, 0L)
    val totalShuffleWrite = listener.executorToShuffleWrite.getOrElse(execId, 0L)
    val executorLogs = listener.executorToLogUrls.getOrElse(execId, Map.empty)

    new ExecutorSummary(
      execId,
      hostPort,
      rddBlocks,
      memUsed,
      diskUsed,
      totalCores,
      activeTasks,
      failedTasks,
      completedTasks,
      totalTasks,
      totalDuration,
      totalGCTime,
      totalInputBytes,
      totalShuffleRead,
      totalShuffleWrite,
      maxMem,
      executorLogs
    )
  }
}
