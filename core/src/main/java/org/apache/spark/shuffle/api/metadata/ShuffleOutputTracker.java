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

package org.apache.spark.shuffle.api.metadata;

import org.apache.spark.annotation.Private;

/**
 * :: Private ::
 *
 * A plugin that can monitor the storage of shuffle data from map tasks, and can provide
 * metadata to shuffle readers to aid their reading of shuffle blocks in reduce tasks.
 * <p>
 * {@link MapOutputMetadata} instances provided from the plugin tree's implementation of
 * {@link org.apache.spark.shuffle.api.ShuffleMapOutputWriter} are sent to this module's map output
 * metadata registration method in {@link #registerMapOutput(int, int, long, MapOutputMetadata)}.
 * <p>
 * Implementations MUST be thread-safe. Spark will invoke methods in this module in parallel.
 * <p>
 * A singleton instance of this module is instantiated on the driver via
 * {@link org.apache.spark.shuffle.api.ShuffleDriverComponents#shuffleOutputTracker()}.
 */
@Private
public interface ShuffleOutputTracker {

  /**
   * Called when a new shuffle stage is going to be run.
   *
   * @param shuffleId the unique identifier for the new shuffle stage
   */
  void registerShuffle(int shuffleId);

  /**
   * Called when the shuffle with the given id is unregistered because it will no longer
   * be used by Spark tasks.
   *
   * @param shuffleId the unique identifier for the shuffle stage to be unregistered
   */
  void unregisterShuffle(int shuffleId, boolean blocking);

  /**
   * Called when a map task completes, and the map output writer has provided metadata to be
   * persisted by this shuffle output tracker.
   *
   * @param shuffleId         the unique identifier for the shuffle stage that the map task is a
   *                          part of
   * @param mapIndex          the map index of the map task in its shuffle map stage - not
   *                          necessarily unique across multiple attempts of this task
   * @param mapId             the identifier for this map task, which is unique even across
   *                          multiple attempts at this task
   * @param mapOutputMetadata metadata about the map output data's storage returned by the map
   *                          task's writer
   *
   */
  void registerMapOutput(
      int shuffleId, int mapIndex, long mapId, MapOutputMetadata mapOutputMetadata);

  /**
   * Called when the given map output is discarded, and will not longer be used in future Spark
   * shuffles.
   *
   * @param shuffleId         the unique identifier for the shuffle stage that the map task is a
   *                          part of
   * @param mapIndex          the map index of the map task which is having its output being
   *                          discarded - not necessarily unique across multiple attempts of this
   *                          task
   * @param mapId             the identifier for the map task which is having its output being
   *                          discarded, which is unique even across multiple attempts at this task
   */
  void removeMapOutput(int shuffleId, int mapIndex, long mapId);
}
