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

package org.apache.spark.sql.proto

import com.google.protobuf.{ByteString, DynamicMessage, Message}

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.{RandomDataGenerator, Row}
import org.apache.spark.sql.catalyst.{CatalystTypeConverters, InternalRow, NoopFilters, OrderedFilters, StructFilters}
import org.apache.spark.sql.catalyst.expressions.{ExpressionEvalHelper, GenericInternalRow, Literal}
import org.apache.spark.sql.catalyst.util.{ArrayBasedMapData, GenericArrayData, MapData}
import org.apache.spark.sql.catalyst.util.RebaseDateTime.RebaseSpec
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.proto.utils.{ProtoOptions, ProtoUtils, SchemaConverters}
import org.apache.spark.sql.sources.{EqualTo, Not}
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

class ProtoCatalystDataConversionSuite extends SparkFunSuite
  with SharedSparkSession
  with ExpressionEvalHelper {

  private def checkResult(data: Literal, descFilePath: String,
                          messageName: String, expected: Any): Unit = {

    checkEvaluation(
      ProtoDataToCatalyst(CatalystDataToProto(data, Some(descFilePath),
        Some(messageName)), descFilePath, messageName, Map.empty),
      prepareExpectedResult(expected))
  }

  protected def checkUnsupportedRead(data: Literal, descFilePath: String,
                                     actualSchema: String, badSchema: String): Unit = {

    val binary = CatalystDataToProto(data, Some(descFilePath), Some(actualSchema))

    intercept[Exception] {
      ProtoDataToCatalyst(binary, descFilePath, badSchema,
        Map("mode" -> "FAILFAST")).eval()
    }

    val expected = {
      val protoOptions = ProtoOptions(Map("mode" -> "PERMISSIVE"))
      val descriptor = ProtoUtils.buildDescriptor(descFilePath, badSchema)
      val expectedSchema = protoOptions.schema.getOrElse(descriptor)
      SchemaConverters.toSqlType(expectedSchema).dataType match {
        case st: StructType => Row.fromSeq((0 until st.length).map(_ => null))
        case _ => null
      }
    }

    checkEvaluation(ProtoDataToCatalyst(binary, descFilePath, badSchema,
      Map("mode" -> "PERMISSIVE")), expected)
  }

  protected def prepareExpectedResult(expected: Any): Any = expected match {
    // Spark byte and short both map to proto int
    case b: Byte => b.toInt
    case s: Short => s.toInt
    case row: GenericInternalRow => InternalRow.fromSeq(row.values.map(prepareExpectedResult))
    case array: GenericArrayData => new GenericArrayData(array.array.map(prepareExpectedResult))
    case map: MapData =>
      val keys = new GenericArrayData(
        map.keyArray().asInstanceOf[GenericArrayData].array.map(prepareExpectedResult))
      val values = new GenericArrayData(
        map.valueArray().asInstanceOf[GenericArrayData].array.map(prepareExpectedResult))
      new ArrayBasedMapData(keys, values)
    case other => other
  }

  private val testingTypes = Seq(
    StructType(StructField("bool_type", BooleanType, nullable = false) :: Nil),
    StructType(StructField("int32_type", IntegerType, nullable = false) :: Nil),
    StructType(StructField("double_type", DoubleType, nullable = false) :: Nil),
    StructType(StructField("float_type", FloatType, nullable = false) :: Nil),
    StructType(StructField("bytes_type", BinaryType, nullable = false) :: Nil),
    StructType(StructField("string_type", StringType, nullable = false) :: Nil),
    StructType(StructField("int32_type", ByteType, nullable = false) :: Nil),
    StructType(StructField("int32_type", ShortType, nullable = false) :: Nil),
  )

  private val catalystTypesToProtoMessages: Map[DataType, String] = Map(
    BooleanType -> "BooleanMsg",
    IntegerType -> "IntegerMsg",
    DoubleType -> "DoubleMsg",
    FloatType -> "FloatMsg",
    BinaryType -> "BytesMsg",
    StringType -> "StringMsg",
    ByteType -> "IntegerMsg",
    ShortType -> "IntegerMsg"
  )

  testingTypes.foreach { dt =>
    val seed = scala.util.Random.nextLong()
    val filePath = testFile("protobuf/catalyst_types.desc").replace("file:/", "/")
    test(s"single $dt with seed $seed") {
      val rand = new scala.util.Random(seed)
      val data = RandomDataGenerator.forType(dt, rand = rand).get.apply()
      val converter = CatalystTypeConverters.createToCatalystConverter(dt)
      val input = Literal.create(converter(data), dt)
      checkResult(input, filePath, catalystTypesToProtoMessages(dt.fields(0).dataType),
        input.eval())
    }
  }

  private def checkDeserialization(
                                    descFilePath: String,
                                    messageName: String,
                                    data: Message,
                                    expected: Option[Any],
                                    filters: StructFilters = new NoopFilters): Unit = {

    val descriptor = ProtoUtils.buildDescriptor(descFilePath, messageName)
    val dataType = SchemaConverters.toSqlType(descriptor).dataType

    val deserializer = new ProtoDeserializer(
      descriptor,
      dataType,
      false,
      RebaseSpec(SQLConf.LegacyBehaviorPolicy.CORRECTED),
      filters)

    val dynMsg = DynamicMessage.parseFrom(descriptor, data.toByteArray)
    val deserialized = deserializer.deserialize(dynMsg)
    expected match {
      case None => assert(deserialized.isEmpty)
      case Some(d) =>
        assert(checkResult(d, deserialized.get, dataType, exprNullable = false))
    }
  }

  test("Handle unsupported input of record type") {
    val testFileDesc = testFile("protobuf/catalyst_types.desc").replace("file:/", "/")
    val actualSchema = StructType(Seq(
      StructField("col_0", StringType, nullable = false),
      StructField("col_1", IntegerType, nullable = false),
      StructField("col_2", FloatType, nullable = false),
      StructField("col_3", BooleanType, nullable = false),
      StructField("col_4", DoubleType, nullable = false)))

    val seed = scala.util.Random.nextLong()
    withClue(s"create random record with seed $seed") {
      val data = RandomDataGenerator.randomRow(new scala.util.Random(seed), actualSchema)
      val converter = CatalystTypeConverters.createToCatalystConverter(actualSchema)
      val input = Literal.create(converter(data), actualSchema)
      checkUnsupportedRead(input, testFileDesc, "Actual", "Bad")
    }
  }

  test("filter push-down to Proto deserializer") {

    val testFileDesc = testFile("protobuf/catalyst_types.desc").replace("file:/", "/")
    val sqlSchema = new StructType()
      .add("name", "string")
      .add("age", "int")

    val descriptor = ProtoUtils.buildDescriptor(testFileDesc, "Person")
    val dynamicMessage = DynamicMessage.newBuilder(descriptor)
      .setField(descriptor.findFieldByName("name"), "Maxim")
      .setField(descriptor.findFieldByName("age"), 39)
      .build()

    val expectedRow = Some(InternalRow(UTF8String.fromString("Maxim"), 39))
    checkDeserialization(testFileDesc, "Person", dynamicMessage, expectedRow)
    checkDeserialization(
      testFileDesc,
      "Person",
      dynamicMessage,
      expectedRow,
      new OrderedFilters(Seq(EqualTo("age", 39)), sqlSchema))

    checkDeserialization(
      testFileDesc,
      "Person",
      dynamicMessage,
      None,
      new OrderedFilters(Seq(Not(EqualTo("name", "Maxim"))), sqlSchema))
  }

  test("ProtoDeserializer with binary type") {

    val testFileDesc = testFile("protobuf/catalyst_types.desc").replace(
      "file:/", "/")
    val bb = java.nio.ByteBuffer.wrap(Array[Byte](97, 48, 53))

    val descriptor = ProtoUtils.buildDescriptor(testFileDesc, "BytesMsg")

    val dynamicMessage = DynamicMessage.newBuilder(descriptor)
      .setField(descriptor.findFieldByName("bytes_type"), ByteString.copyFrom(bb))
      .build()

    val expected = InternalRow(Array[Byte](97, 48, 53))
    checkDeserialization(testFileDesc, "BytesMsg", dynamicMessage, Some(expected))
  }
}