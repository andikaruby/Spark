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

package org.apache.spark.sql

import org.apache.spark.annotation.Experimental
import org.apache.spark.sql.streaming.ContinuousQuery

/**
 * :: Experimental ::
 * A writer to consume data generated by a [[ContinuousQuery]]. Each partition will use a new
 * deserialized instance, so you usually should do the initialization work in the `open` method.
 *
 * @since 2.0.0
 */
@Experimental
abstract class ForeachWriter[T] extends Serializable {

  /**
   * Called when starting to process one partition of new data in the executor side. `version` is
   * for data deduplication. When recovering from a failure, some data may be processed twice. But
   * it's guarantee that they will be opened with the same "version".
   *
   * If this method finds this is a partition from a duplicated data set, it can return `false` to
   * skip the further data processing. However, `close` still will be called for cleaning up
   * resources.
   *
   * @param partitionId the partition id.
   * @param version a unique id for data deduplication.
   * @return a flat that indicates if the data should be processed.
   */
  def open(partitionId: Long, version: Long): Boolean

  /**
   * Called to process the data in the executor side.
   */
  def process(value: T): Unit

  /**
   * Called when stopping to process one partition of new data in the executor side. This is
   * guaranteed to be called when a `Throwable` is thrown during processing data. However,
   * `close` won't be called in the following cases:
   *  - JVM crashes without throwing a `Throwable`
   *  - `open` throws a `Throwable`.
   *
   * @param errorOrNull the error thrown during processing data or null if nothing is thrown.
   */
  def close(errorOrNull: Throwable): Unit
}
