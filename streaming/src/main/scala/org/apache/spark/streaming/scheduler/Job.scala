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

package org.apache.spark.streaming.scheduler

import org.apache.spark.streaming.Time
import scala.util.Try

/**
 * Class representing a Spark computation. It may contain multiple Spark jobs.
 */
private[streaming]
class Job(val time: Time, func: () => _) {
  private var _id: String = _
  private var _outputOpId: Int = _
  private var isSet = false
  private var _result: Try[_] = null

  def run() {
    _result = Try(func())
  }

  def result: Try[_] = {
    if (_result == null) {
      throw new IllegalStateException("Cannot access result before job finishes")
    }
    _result
  }

  def id: String = {
    if (!isSet) {
      throw new IllegalStateException("Cannot access id before calling setId")
    }
    _id
  }

  def outputOpId: Int = {
    if (!isSet) {
      throw new IllegalStateException("Cannot access number before calling setId")
    }
    _outputOpId
  }

  def setOutputOpId(outputOpId: Int) {
    if (isSet) {
      throw new IllegalStateException("Cannot call setOutputOpId more than once")
    }
    isSet = true
    _id = "streaming job " + time + "." + outputOpId
    _outputOpId = outputOpId
  }

  override def toString: String = id
}
