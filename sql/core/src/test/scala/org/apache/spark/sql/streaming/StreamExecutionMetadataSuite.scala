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

package org.apache.spark.sql.streaming

import org.apache.spark.sql.execution.streaming.StreamExecutionMetadata
import org.apache.spark.util.Utils

class StreamExecutionMetadataSuite extends StreamTest {

  private def newMetadataDir =
    Utils.createTempDir(namePrefix = "streaming.metadata").getCanonicalPath

  test("stream execution metadata") {
    assert(StreamExecutionMetadata(0, 0) ===
      StreamExecutionMetadata("""{}"""))
    assert(StreamExecutionMetadata(1, 0) ===
      StreamExecutionMetadata("""{"batchWatermarkMs":1}"""))
    assert(StreamExecutionMetadata(0, 2) ===
      StreamExecutionMetadata("""{"batchTimestampMs":2}"""))
    assert(StreamExecutionMetadata(1, 2) ===
      StreamExecutionMetadata(
        """{"batchWatermarkMs":1,"batchTimestampMs":2}"""))
  }
}
