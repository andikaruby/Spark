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

package org.apache.spark

import java.io.Serializable

import org.apache.spark.annotation.DeveloperApi

/**
 * A factory to create [[TaskEvaluator]]. Spark serializes and sends [[TaskEvaluatorFactory]]
 * to executors, and then creates [[TaskEvaluator]] via the factory at the executor side.
 */
@DeveloperApi
trait TaskEvaluatorFactory[T, U] extends Serializable {

  /**
   * Creates a task evaluator. Each RDD partition will create one task evaluator instance, which
   * means one task evaluator instance will be used by only one thread.
   */
  def createEvaluator(): TaskEvaluator[T, U]
}
