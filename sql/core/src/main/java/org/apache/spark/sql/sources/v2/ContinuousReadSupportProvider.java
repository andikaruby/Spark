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

package org.apache.spark.sql.sources.v2;

import org.apache.spark.annotation.InterfaceStability;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Utils;
import org.apache.spark.sql.sources.v2.reader.streaming.ContinuousReadSupport;
import org.apache.spark.sql.types.StructType;

/**
 * A mix-in interface for {@link DataSourceV2}. Data sources can implement this interface to
 * provide data reading ability for continuous stream processing.
 *
 * This interface is used to return {@link ContinuousReadSupport} instances when end users run
 * {@code SparkSession.readStream.format(...).option(...).load()} with a continuous trigger.
 */
@InterfaceStability.Evolving
public interface ContinuousReadSupportProvider extends DataSourceV2 {

  /**
   * Returns a {@link ContinuousReadSupport} instance to scan the data from this streaming data
   * source with a user specified schema.
   *
   * By default this method throws {@link UnsupportedOperationException}, implementations should
   * override this method to handle user specified schema.
   *
   * @param schema the user provided schema.
   * @param checkpointLocation a path to Hadoop FS scratch space that can be used for failure
   *                           recovery. Readers for the same logical source in the same query
   *                           will be given the same checkpointLocation.
   * @param options the options for the returned data source reader, which is an immutable
   *                case-insensitive string-to-string map.
   */
  default ContinuousReadSupport getContinuousReadSupport(
      StructType schema,
      String checkpointLocation,
      DataSourceOptions options) {
    return DataSourceV2Utils.failForUserSpecifiedSchema(this);
  }

  /**
   * Returns a {@link ContinuousReadSupport} instance to scan the data from this streaming data
   * source.
   *
   * @param checkpointLocation a path to Hadoop FS scratch space that can be used for failure
   *                           recovery. Readers for the same logical source in the same query
   *                           will be given the same checkpointLocation.
   * @param options the options for the returned data source reader, which is an immutable
   *                case-insensitive string-to-string map.
   */
  ContinuousReadSupport getContinuousReadSupport(
      String checkpointLocation,
      DataSourceOptions options);
}
