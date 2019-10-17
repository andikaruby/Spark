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

package org.apache.spark.sql.kafka010

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.scalatest.time.SpanSugar._

import org.apache.spark.{SparkConf, SparkException, TestUtils}
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.expressions.{AttributeReference, SpecificInternalRow, UnsafeProjection}
import org.apache.spark.sql.execution.streaming.MemoryStream
import org.apache.spark.sql.functions._
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types.{BinaryType, DataType, IntegerType, StringType, StructField, StructType}

abstract class KafkaSinkSuiteBase extends QueryTest with SharedSparkSession with KafkaTest {
  protected var testUtils: KafkaTestUtils = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    testUtils = new KafkaTestUtils(
      withBrokerProps = Map("auto.create.topics.enable" -> "false"))
    testUtils.setup()
  }

  override def afterAll(): Unit = {
    try {
      if (testUtils != null) {
        testUtils.teardown()
        testUtils = null
      }
    } finally {
      super.afterAll()
    }
  }

  private val topicId = new AtomicInteger(0)

  protected def newTopic(): String = s"topic-${topicId.getAndIncrement()}"

  protected def createKafkaReader(topic: String, includeHeaders: Boolean = false): DataFrame = {
    spark.read
      .format("kafka")
      .option("kafka.bootstrap.servers", testUtils.brokerAddress)
      .option("startingOffsets", "earliest")
      .option("endingOffsets", "latest")
      .option("subscribe", topic)
      .option("includeHeaders", includeHeaders.toString)
      .load()
  }
}

class KafkaSinkStreamingSuite extends KafkaSinkSuiteBase with StreamTest {
  import testImplicits._

  override val streamingTimeout = 30.seconds

  test("streaming - write to kafka with topic field") {
    val input = MemoryStream[String]
    val topic = newTopic()
    testUtils.createTopic(topic)

    val writer = createKafkaWriter(
      input.toDF(),
      withTopic = None,
      withOutputMode = Some(OutputMode.Append))(
      withSelectExpr = s"'$topic' as topic", "value")

    val reader = createKafkaReader(topic)
      .selectExpr("CAST(key as STRING) key", "CAST(value as STRING) value")
      .selectExpr("CAST(key as INT) key", "CAST(value as INT) value")
      .as[(Option[Int], Int)]
      .map(_._2)

    try {
      input.addData("1", "2", "3", "4", "5")
      failAfter(streamingTimeout) {
        writer.processAllAvailable()
      }
      checkDatasetUnorderly(reader, 1, 2, 3, 4, 5)
      input.addData("6", "7", "8", "9", "10")
      failAfter(streamingTimeout) {
        writer.processAllAvailable()
      }
      checkDatasetUnorderly(reader, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    } finally {
      writer.stop()
    }
  }

  test("streaming - write aggregation w/o topic field, with topic option") {
    val input = MemoryStream[String]
    val topic = newTopic()
    testUtils.createTopic(topic)

    val writer = createKafkaWriter(
      input.toDF().groupBy("value").count(),
      withTopic = Some(topic),
      withOutputMode = Some(OutputMode.Update()))(
      withSelectExpr = "CAST(value as STRING) key", "CAST(count as STRING) value")

    val reader = createKafkaReader(topic)
      .selectExpr("CAST(key as STRING) key", "CAST(value as STRING) value")
      .selectExpr("CAST(key as INT) key", "CAST(value as INT) value")
      .as[(Int, Int)]

    try {
      input.addData("1", "2", "2", "3", "3", "3")
      failAfter(streamingTimeout) {
        writer.processAllAvailable()
      }
      checkDatasetUnorderly(reader, (1, 1), (2, 2), (3, 3))
      input.addData("1", "2", "3")
      failAfter(streamingTimeout) {
        writer.processAllAvailable()
      }
      checkDatasetUnorderly(reader, (1, 1), (2, 2), (3, 3), (1, 2), (2, 3), (3, 4))
    } finally {
      writer.stop()
    }
  }

  test("streaming - aggregation with topic field and topic option") {
    /* The purpose of this test is to ensure that the topic option
     * overrides the topic field. We begin by writing some data that
     * includes a topic field and value (e.g., 'foo') along with a topic
     * option. Then when we read from the topic specified in the option
     * we should see the data i.e., the data was written to the topic
     * option, and not to the topic in the data e.g., foo
     */
    val input = MemoryStream[String]
    val topic = newTopic()
    testUtils.createTopic(topic)

    val writer = createKafkaWriter(
      input.toDF().groupBy("value").count(),
      withTopic = Some(topic),
      withOutputMode = Some(OutputMode.Update()))(
      withSelectExpr = "'foo' as topic",
        "CAST(value as STRING) key", "CAST(count as STRING) value")

    val reader = createKafkaReader(topic)
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .selectExpr("CAST(key AS INT)", "CAST(value AS INT)")
      .as[(Int, Int)]

    try {
      input.addData("1", "2", "2", "3", "3", "3")
      failAfter(streamingTimeout) {
        writer.processAllAvailable()
      }
      checkDatasetUnorderly(reader, (1, 1), (2, 2), (3, 3))
      input.addData("1", "2", "3")
      failAfter(streamingTimeout) {
        writer.processAllAvailable()
      }
      checkDatasetUnorderly(reader, (1, 1), (2, 2), (3, 3), (1, 2), (2, 3), (3, 4))
    } finally {
      writer.stop()
    }
  }

  test("streaming - sink progress is produced") {
    /* ensure sink progress is correctly produced. */
    val input = MemoryStream[String]
    val topic = newTopic()
    testUtils.createTopic(topic)

    val writer = createKafkaWriter(
      input.toDF(),
      withTopic = Some(topic),
      withOutputMode = Some(OutputMode.Update()))()

    try {
      input.addData("1", "2", "3")
      failAfter(streamingTimeout) {
        writer.processAllAvailable()
      }
      assert(writer.lastProgress.sink.numOutputRows == 3L)
    } finally {
      writer.stop()
    }
  }

  test("streaming - write data with bad schema") {
    val input = MemoryStream[String]
    val topic = newTopic()
    testUtils.createTopic(topic)

    /* No topic field or topic option */
    var writer: StreamingQuery = null
    var ex: Exception = null
    try {
      ex = intercept[StreamingQueryException] {
        writer = createKafkaWriter(input.toDF())(
          withSelectExpr = "value as key", "value"
        )
        input.addData("1", "2", "3", "4", "5")
        writer.processAllAvailable()
      }
    } finally {
      writer.stop()
    }
    assert(ex.getMessage
      .toLowerCase(Locale.ROOT)
      .contains("topic option required when no 'topic' attribute is present"))

    try {
      /* No value field */
      ex = intercept[StreamingQueryException] {
        writer = createKafkaWriter(input.toDF())(
          withSelectExpr = s"'$topic' as topic", "value as key"
        )
        input.addData("1", "2", "3", "4", "5")
        writer.processAllAvailable()
      }
    } finally {
      writer.stop()
    }
    assert(ex.getMessage.toLowerCase(Locale.ROOT).contains(
      "required attribute 'value' not found"))
  }

  test("streaming - write data with valid schema but wrong types") {
    val input = MemoryStream[String]
    val topic = newTopic()
    testUtils.createTopic(topic)

    var writer: StreamingQuery = null
    var ex: Exception = null
    try {
      /* topic field wrong type */
      ex = intercept[StreamingQueryException] {
        writer = createKafkaWriter(input.toDF())(
          withSelectExpr = s"CAST('1' as INT) as topic", "value"
        )
        input.addData("1", "2", "3", "4", "5")
        writer.processAllAvailable()
      }
    } finally {
      writer.stop()
    }
    assert(ex.getMessage.toLowerCase(Locale.ROOT).contains("topic type must be a string"))

    try {
      /* value field wrong type */
      ex = intercept[StreamingQueryException] {
        writer = createKafkaWriter(input.toDF())(
          withSelectExpr = s"'$topic' as topic", "CAST(value as INT) as value"
        )
        input.addData("1", "2", "3", "4", "5")
        writer.processAllAvailable()
      }
    } finally {
      writer.stop()
    }
    assert(ex.getMessage.toLowerCase(Locale.ROOT).contains(
      "value attribute type must be a string or binary"))

    try {
      ex = intercept[StreamingQueryException] {
        /* key field wrong type */
        writer = createKafkaWriter(input.toDF())(
          withSelectExpr = s"'$topic' as topic", "CAST(value as INT) as key", "value"
        )
        input.addData("1", "2", "3", "4", "5")
        writer.processAllAvailable()
      }
    } finally {
      writer.stop()
    }
    assert(ex.getMessage.toLowerCase(Locale.ROOT).contains(
      "key attribute type must be a string or binary"))

    try {
      ex = intercept[StreamingQueryException] {
        /* partition field wrong type */
        writer = createKafkaWriter(input.toDF())(
          withSelectExpr = s"'$topic' as topic", "value", "value as partition"
        )
        input.addData("1", "2", "3", "4", "5")
        writer.processAllAvailable()
      }
    } finally {
      writer.stop()
    }
    assert(ex.getMessage.toLowerCase(Locale.ROOT).contains(
      "partition attribute type must be an int"))
  }

  test("streaming - write to non-existing topic") {
    val input = MemoryStream[String]
    val topic = newTopic()

    var writer: StreamingQuery = null
    var ex: Exception = null
    try {
      ex = intercept[StreamingQueryException] {
        writer = createKafkaWriter(input.toDF(), withTopic = Some(topic))()
        input.addData("1", "2", "3", "4", "5")
        writer.processAllAvailable()
      }
    } finally {
      writer.stop()
    }
    assert(ex.getCause.getCause.getMessage.toLowerCase(Locale.ROOT).contains("job aborted"))
  }

  test("streaming - exception on config serializer") {
    val input = MemoryStream[String]
    var writer: StreamingQuery = null
    var ex: Exception = null
    ex = intercept[StreamingQueryException] {
      writer = createKafkaWriter(
        input.toDF(),
        withOptions = Map("kafka.key.serializer" -> "foo"))()
      input.addData("1")
      writer.processAllAvailable()
    }
    assert(ex.getCause.getMessage.toLowerCase(Locale.ROOT).contains(
      "kafka option 'key.serializer' is not supported"))

    ex = intercept[StreamingQueryException] {
      writer = createKafkaWriter(
        input.toDF(),
        withOptions = Map("kafka.value.serializer" -> "foo"))()
      input.addData("1")
      writer.processAllAvailable()
    }
    assert(ex.getCause.getMessage.toLowerCase(Locale.ROOT).contains(
      "kafka option 'value.serializer' is not supported"))
  }

  private def createKafkaWriter(
      input: DataFrame,
      withTopic: Option[String] = None,
      withOutputMode: Option[OutputMode] = None,
      withOptions: Map[String, String] = Map[String, String]())
      (withSelectExpr: String*): StreamingQuery = {
    var stream: DataStreamWriter[Row] = null
    withTempDir { checkpointDir =>
      var df = input.toDF()
      if (withSelectExpr.length > 0) {
        df = df.selectExpr(withSelectExpr: _*)
      }
      stream = df.writeStream
        .format("kafka")
        .option("checkpointLocation", checkpointDir.getCanonicalPath)
        .option("kafka.bootstrap.servers", testUtils.brokerAddress)
        .option("kafka.max.block.ms", "5000")
        .queryName("kafkaStream")
      withTopic.foreach(stream.option("topic", _))
      withOutputMode.foreach(stream.outputMode(_))
      withOptions.foreach(opt => stream.option(opt._1, opt._2))
    }
    stream.start()
  }
}

abstract class KafkaSinkBatchSuiteBase extends KafkaSinkSuiteBase {
  import testImplicits._

  test("batch - write to kafka") {
    val topic = newTopic()
    testUtils.createTopic(topic, 4)
    val data = Seq(
      Row(topic, "1", Seq(
        Row("a", "b".getBytes(UTF_8))
      ), 0),
      Row(topic, "2", Seq(
        Row("c", "d".getBytes(UTF_8)),
        Row("e", "f".getBytes(UTF_8))
      ), 1),
      Row(topic, "3", Seq(
        Row("g", "h".getBytes(UTF_8)),
        Row("g", "i".getBytes(UTF_8))
      ), 2),
      Row(topic, "4", null, 3),
      Row(topic, "5", Seq(
        Row("j", "k".getBytes(UTF_8)),
        Row("j", "l".getBytes(UTF_8)),
        Row("m", "n".getBytes(UTF_8))
      ), 0)
    )

    val df = spark.createDataFrame(
      spark.sparkContext.parallelize(data),
      StructType(Seq(StructField("topic", StringType), StructField("value", StringType),
        StructField("headers", KafkaRecordToRowConverter.headersType),
        StructField("partition", IntegerType)))
    )

    df.write
      .format("kafka")
      .option("kafka.bootstrap.servers", testUtils.brokerAddress)
      .option("topic", topic)
      .mode("append")
      .save()
    checkAnswer(
      createKafkaReader(topic, includeHeaders = true).selectExpr(
        "CAST(value as STRING) value", "headers", "partition"
      ),
      Row("1", Seq(Row("a", "b".getBytes(UTF_8))), 0) ::
        Row("2", Seq(Row("c", "d".getBytes(UTF_8)), Row("e", "f".getBytes(UTF_8))), 1) ::
        Row("3", Seq(Row("g", "h".getBytes(UTF_8)), Row("g", "i".getBytes(UTF_8))), 2) ::
        Row("4", null, 3) ::
        Row("5", Seq(
          Row("j", "k".getBytes(UTF_8)),
          Row("j", "l".getBytes(UTF_8)),
          Row("m", "n".getBytes(UTF_8))), 0) ::
        Nil
    )
  }

  test("batch - partition column vs default Kafka partitioner") {
    val fixedKey = "fixed_key"
    val nrPartitions = 100

    // default Kafka partitioner calculate partition deterministically based on the key
    val keyTopic = newTopic()
    testUtils.createTopic(keyTopic, nrPartitions)

    Seq((keyTopic, fixedKey, "value"))
      .toDF("topic", "key", "value")
      .write
      .format("kafka")
      .option("kafka.bootstrap.servers", testUtils.brokerAddress)
      .option("topic", keyTopic)
      .mode("append")
      .save()

    // getting the partition corresponding to the fixed key
    val keyPartition = createKafkaReader(keyTopic).select("partition")
      .map(_.getInt(0)).collect().toList.head

    val topic = newTopic()
    testUtils.createTopic(topic, nrPartitions)

    // even values use default kafka partitioner, odd use 'n'
    val df = (0 until 100)
      .map(n => (topic, fixedKey, s"$n", if (n % 2 == 0) None else Some(n)))
      .toDF("topic", "key", "value", "partition")

    df.write
      .format("kafka")
      .option("kafka.bootstrap.servers", testUtils.brokerAddress)
      .option("topic", topic)
      .mode("append")
      .save()

    checkAnswer(
      createKafkaReader(topic).selectExpr(
        "CAST(key as STRING) key", "CAST(value as STRING) value", "partition"
      ),
      (0 until 100)
        .map(n => (fixedKey, s"$n", if (n % 2 == 0) keyPartition else n))
        .toDF("key", "value", "partition")
    )
  }

  test("batch - non-existing partitions trigger standard Kafka exception") {
    val topic = newTopic()
    val producerTimeout = "1000"
    testUtils.createTopic(topic, 4)
    val df = Seq((topic, "1", 5)).toDF("topic", "value", "partition")

    val ex = intercept[SparkException] {
      df.write
        .format("kafka")
        .option("kafka.bootstrap.servers", testUtils.brokerAddress)
        .option("kafka.max.block.ms", producerTimeout) // default (60000 ms) would be too slow
        .option("topic", topic)
        .mode("append")
        .save()
    }
    TestUtils.assertExceptionMsg(
      ex, s"Topic $topic not present in metadata after $producerTimeout ms")
  }

  test("batch - null topic field value, and no topic option") {
    val df = Seq[(String, String)](null.asInstanceOf[String] -> "1").toDF("topic", "value")
    val ex = intercept[SparkException] {
      df.write
        .format("kafka")
        .option("kafka.bootstrap.servers", testUtils.brokerAddress)
        .mode("append")
        .save()
    }
    TestUtils.assertExceptionMsg(ex, "null topic present in the data")
  }

  protected def testUnsupportedSaveModes(msg: (SaveMode) => String): Unit = {
    val topic = newTopic()
    testUtils.createTopic(topic)
    val df = Seq[(String, String)](null.asInstanceOf[String] -> "1").toDF("topic", "value")

    Seq(SaveMode.Ignore, SaveMode.Overwrite).foreach { mode =>
      val ex = intercept[AnalysisException] {
        df.write
          .format("kafka")
          .option("kafka.bootstrap.servers", testUtils.brokerAddress)
          .mode(mode)
          .save()
      }
      TestUtils.assertExceptionMsg(ex, msg(mode))
    }
  }

  test("SPARK-20496: batch - enforce analyzed plans") {
    val inputEvents =
      spark.range(1, 1000)
        .select(to_json(struct("*")) as 'value)

    val topic = newTopic()
    testUtils.createTopic(topic)
    // used to throw UnresolvedException
    inputEvents.write
      .format("kafka")
      .option("kafka.bootstrap.servers", testUtils.brokerAddress)
      .option("topic", topic)
      .mode("append")
      .save()
  }
}

class KafkaSinkBatchSuiteV1 extends KafkaSinkBatchSuiteBase {
  override protected def sparkConf: SparkConf =
    super
      .sparkConf
      .set(SQLConf.USE_V1_SOURCE_LIST, "kafka")

  test("batch - unsupported save modes") {
    testUnsupportedSaveModes((mode) => s"Save mode ${mode.name} not allowed for Kafka")
  }
}

class KafkaSinkBatchSuiteV2 extends KafkaSinkBatchSuiteBase {
  override protected def sparkConf: SparkConf =
    super
      .sparkConf
      .set(SQLConf.USE_V1_SOURCE_LIST, "")

  test("batch - unsupported save modes") {
    testUnsupportedSaveModes((mode) => s"cannot be written with ${mode.name} mode")
  }

  test("generic - write big data with small producer buffer") {
    /* This test ensures that we understand the semantics of Kafka when
    * is comes to blocking on a call to send when the send buffer is full.
    * This test will configure the smallest possible producer buffer and
    * indicate that we should block when it is full. Thus, no exception should
    * be thrown in the case of a full buffer.
    */
    val topic = newTopic()
    testUtils.createTopic(topic, 1)
    val options = new java.util.HashMap[String, Object]
    options.put("bootstrap.servers", testUtils.brokerAddress)
    options.put("buffer.memory", "16384") // min buffer size
    options.put("block.on.buffer.full", "true")
    options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer].getName)
    options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer].getName)
    val inputSchema = Seq(AttributeReference("value", BinaryType)())
    val data = new Array[Byte](15000) // large value
    val writeTask = new KafkaWriteTask(options, inputSchema, Some(topic))
    try {
      val fieldTypes: Array[DataType] = Array(BinaryType)
      val converter = UnsafeProjection.create(fieldTypes)
      val row = new SpecificInternalRow(fieldTypes)
      row.update(0, data)
      val iter = Seq.fill(1000)(converter.apply(row)).iterator
      writeTask.execute(iter)
    } finally {
      writeTask.close()
    }
  }
}
