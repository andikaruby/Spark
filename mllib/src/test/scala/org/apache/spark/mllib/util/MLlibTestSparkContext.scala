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

package org.apache.spark.mllib.util

import java.io.File

import org.apache.hadoop.fs.Path
import org.scalatest.Suite

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.util.TempDirectory
import org.apache.spark.sql.SQLContext
import org.apache.spark.util.Utils

trait MLlibTestSparkContext extends TempDirectory { self: Suite =>
  @transient var sc: SparkContext = _
  @transient var sqlContext: SQLContext = _
  @transient var checkpointDir: String = _

  override def beforeAll() {
    super.beforeAll()
    val conf = new SparkConf()
      .setMaster("local[2]")
      .setAppName("MLlibUnitTest")
    sc = new SparkContext(conf)
    SQLContext.clearActive()
    sqlContext = new SQLContext(sc)
    SQLContext.setActive(sqlContext)
    checkpointDir = Utils.createDirectory(tempDir.getCanonicalPath, "checkpoints").toString
    sc.setCheckpointDir(checkpointDir)
  }

  override def afterAll() {
    try {
      Utils.deleteRecursively(new File(checkpointDir))
      sqlContext = null
      SQLContext.clearActive()
      if (sc != null) {
        sc.stop()
      }
      sc = null
    } finally {
      super.afterAll()
    }
  }
}
