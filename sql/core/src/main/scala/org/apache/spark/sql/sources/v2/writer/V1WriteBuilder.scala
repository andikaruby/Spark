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

package org.apache.spark.sql.sources.v2.writer

import org.apache.spark.annotation.{Experimental, Unstable}
import org.apache.spark.sql.sources.CreatableRelationProvider

/**
 * A trait that should be implemented by V1 DataSources that would like to leverage the DataSource
 * V2 write code paths.
 *
 * @since 3.0.0
 */
@Experimental
@Unstable
trait V1WriteBuilder extends WriteBuilder {

  /**
   * Creates a [[CreatableRelationProvider]] that allows saving a DataFrame to a
   * a destination (using data source-specific parameters).
   *
   * The relation will receive a string to string map of options that will be case sensitive,
   * therefore the implementation of the data source should be able to handle case insensitive
   * option checking.
   *
   * @since 3.0.0
   */
  def buildForV1Write(): CreatableRelationProvider = {
    throw new UnsupportedOperationException(getClass.getName + " does not support batch write")
  }
}
