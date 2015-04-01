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

package org.apache.spark.sql.execution

import java.util.HashSet

import org.apache.spark.annotation.{AlphaComponent, DeveloperApi}

/**
 * :: DeveloperApi ::
 * Physical execution operators for join operations.
 */
package object joins {

  @DeveloperApi
  sealed trait BuildSide

  @DeveloperApi
  case object BuildRight extends BuildSide

  @DeveloperApi
  case object BuildLeft extends BuildSide

  @AlphaComponent
  class MultiBuild extends HashSet[Int] with BuildSide {
    def this(indices: Seq[Int]) = {
      this()
      indices.foreach(index => this.add(index))
    }
  }
}
