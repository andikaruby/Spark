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

package org.apache.spark.sql.execution.streaming

import java.io.File

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.catalyst.util.stringToFile
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSQLContext

class OffsetSeqLogSuite extends SparkFunSuite with SharedSQLContext {

  /** test string offset type */
  case class StringOffset(override val json: String) extends Offset

  test("OffsetSeqMetadata - deserialization") {
    val key = SQLConf.SHUFFLE_PARTITIONS.key

    def getConfWith(shufflePartitions: Int): Map[String, String] = {
      Map(key -> shufflePartitions.toString)
    }

    // None set
    assert(new OffsetSeqMetadata(0, 0, Map.empty) === OffsetSeqMetadata("""{}"""))

    // One set
    assert(new OffsetSeqMetadata(1, 0, Map.empty) ===
      OffsetSeqMetadata("""{"batchWatermarkMs":1}"""))
    assert(new OffsetSeqMetadata(0, 2, Map.empty) ===
      OffsetSeqMetadata("""{"batchTimestampMs":2}"""))
    assert(OffsetSeqMetadata(0, 0, getConfWith(shufflePartitions = 2)) ===
      OffsetSeqMetadata(s"""{"conf": {"$key":2}}"""))

    // Two set
    assert(new OffsetSeqMetadata(1, 2, Map.empty) ===
      OffsetSeqMetadata("""{"batchWatermarkMs":1,"batchTimestampMs":2}"""))
    assert(OffsetSeqMetadata(1, 0, getConfWith(shufflePartitions = 3)) ===
      OffsetSeqMetadata(s"""{"batchWatermarkMs":1,"conf": {"$key":3}}"""))
    assert(OffsetSeqMetadata(0, 2, getConfWith(shufflePartitions = 3)) ===
      OffsetSeqMetadata(s"""{"batchTimestampMs":2,"conf": {"$key":3}}"""))

    // Three set
    assert(OffsetSeqMetadata(1, 2, getConfWith(shufflePartitions = 3)) ===
      OffsetSeqMetadata(s"""{"batchWatermarkMs":1,"batchTimestampMs":2,"conf": {"$key":3}}"""))

    // All set
    assert(OffsetSeqMetadata(1, 2, getConfWith(shufflePartitions = 3), Map(0L -> 1000L)) ===
      OffsetSeqMetadata(
        s"""{"batchWatermarkMs":1,"batchTimestampMs":2,"conf": {"$key":3},
           |"operatorWatermarks": {"0": 1000}}""".stripMargin))

    // Drop unknown fields
    assert(OffsetSeqMetadata(1, 2, getConfWith(shufflePartitions = 3)) ===
      OffsetSeqMetadata(
        s"""{"batchWatermarkMs":1,"batchTimestampMs":2,"conf": {"$key":3}},"unknown":1"""))
  }

  test("OffsetSeqLog - serialization - deserialization") {
    withTempDir { temp =>
      val dir = new File(temp, "dir") // use non-existent directory to test whether log make the dir
      val metadataLog = new OffsetSeqLog(spark, dir.getAbsolutePath)
      val batch0 = OffsetSeq.fill(LongOffset(0), LongOffset(1), LongOffset(2))
      val batch1 = OffsetSeq.fill(StringOffset("one"), StringOffset("two"), StringOffset("three"))

      val batch0Serialized = OffsetSeq.fill(batch0.offsets.flatMap(_.map(o =>
        SerializedOffset(o.json))): _*)

      val batch1Serialized = OffsetSeq.fill(batch1.offsets.flatMap(_.map(o =>
        SerializedOffset(o.json))): _*)

      assert(metadataLog.add(0, batch0))
      assert(metadataLog.getLatest() === Some(0 -> batch0Serialized))
      assert(metadataLog.get(0) === Some(batch0Serialized))

      assert(metadataLog.add(1, batch1))
      assert(metadataLog.get(0) === Some(batch0Serialized))
      assert(metadataLog.get(1) === Some(batch1Serialized))
      assert(metadataLog.getLatest() === Some(1 -> batch1Serialized))
      assert(metadataLog.get(None, Some(1)) ===
        Array(0 -> batch0Serialized, 1 -> batch1Serialized))

      // Adding the same batch does nothing
      metadataLog.add(1, OffsetSeq.fill(LongOffset(3)))
      assert(metadataLog.get(0) === Some(batch0Serialized))
      assert(metadataLog.get(1) === Some(batch1Serialized))
      assert(metadataLog.getLatest() === Some(1 -> batch1Serialized))
      assert(metadataLog.get(None, Some(1)) ===
        Array(0 -> batch0Serialized, 1 -> batch1Serialized))
    }
  }

  test("deserialization log written by future version") {
    withTempDir { dir =>
      stringToFile(new File(dir, "0"), "v99999")
      val log = new OffsetSeqLog(spark, dir.getCanonicalPath)
      val e = intercept[IllegalStateException] {
        log.get(0)
      }
      Seq(
        s"maximum supported log version is v${OffsetSeqLog.VERSION}, but encountered v99999",
        "produced by a newer version of Spark and cannot be read by this version"
      ).foreach { message =>
        assert(e.getMessage.contains(message))
      }
    }
  }

  test("read Spark 2.1.0 log format") {
    val (batchId, offsetSeq) = readFromResource("offset-log-version-2.1.0")
    assert(batchId === 0)
    assert(offsetSeq.offsets === Seq(
      Some(SerializedOffset("""{"logOffset":345}""")),
      Some(SerializedOffset("""{"topic-0":{"0":1}}"""))
    ))
    assert(offsetSeq.metadata === Some(OffsetSeqMetadata(0L, 1480981499528L)))
  }

  private def readFromResource(dir: String): (Long, OffsetSeq) = {
    val input = getClass.getResource(s"/structured-streaming/$dir")
    val log = new OffsetSeqLog(spark, input.toString)
    log.getLatest().get
  }
}
