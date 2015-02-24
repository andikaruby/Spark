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

import org.apache.spark.rdd.RDD
import org.apache.spark.util.CallSite

private[spark] class ResultStage(
    override val id: Int,
    override val rdd: RDD[_],
    override val numTasks: Int,
    override val parents: List[Stage],
    override val jobId: Int,
    override val callSite: CallSite)
  extends Stage(id, rdd, numTasks, parents, jobId, callSite) {

  /**
   * For stages that are the final (consists of only ResultTasks), links to the active job for
   * this results stage.
   */
  var resultOfJob: Option[ActiveJob] = None

  override def toString: String = "ResultStage " + id

  override def isAvailable: Boolean= true
}
