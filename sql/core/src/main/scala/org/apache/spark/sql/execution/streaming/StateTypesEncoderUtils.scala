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

import org.apache.commons.lang3.SerializationUtils

import org.apache.spark.sql.Encoder
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder.Serializer
import org.apache.spark.sql.catalyst.encoders.encoderFor
import org.apache.spark.sql.catalyst.expressions.{UnsafeProjection, UnsafeRow}
import org.apache.spark.sql.execution.streaming.state.StateStoreErrors
import org.apache.spark.sql.types.{BinaryType, StructType}

object StateKeyValueRowSchema {
  val KEY_ROW_SCHEMA: StructType = new StructType().add("key", BinaryType)
  val VALUE_ROW_SCHEMA: StructType = new StructType().add("value", BinaryType)
}

/**
 * Helper class providing APIs to encode the grouping key, and user provided values
 * to Spark [[UnsafeRow]].
 *
 * CAUTION: StateTypesEncoder class instance is *not* thread-safe.
 * This class reuses the keyProjection and valueProjection for encoding grouping
 * key and state value respectively. As UnsafeProjection is not thread safe, this
 * class is also not thread safe.
 *
 * @param keySerializer - serializer to serialize the grouping key of type `GK`
 *     to an [[InternalRow]]
 * @param stateName - name of logical state partition
 * @tparam GK - grouping key type
 */
class StateTypesEncoder[GK](
    keySerializer: Serializer[GK],
    stateName: String) {
  import org.apache.spark.sql.execution.streaming.StateKeyValueRowSchema._

  private val keyProjection = UnsafeProjection.create(KEY_ROW_SCHEMA)
  private val valueProjection = UnsafeProjection.create(VALUE_ROW_SCHEMA)

  // TODO: validate places that are trying to encode the key and check if we can eliminate/
  // add caching for some of these calls.
  def encodeGroupingKey(): UnsafeRow = {
    val keyOption = ImplicitGroupingKeyTracker.getImplicitKeyOption
    if (keyOption.isEmpty) {
      throw StateStoreErrors.implicitKeyNotFound(stateName)
    }

    val groupingKey = keyOption.get.asInstanceOf[GK]
    val keyByteArr = keySerializer.apply(groupingKey).asInstanceOf[UnsafeRow].getBytes()
    val keyRow = keyProjection(InternalRow(keyByteArr))
    keyRow
  }

  def encodeCompositeKey[K](userKey: K, userKeyEnc: Encoder[K]): UnsafeRow = {
    val keyOption = ImplicitGroupingKeyTracker.getImplicitKeyOption
    if (keyOption.isEmpty) {
      throw StateStoreErrors.implicitKeyNotFound(stateName)
    }
    val groupingKey = keyOption.get.asInstanceOf[GK]
    // get grouping key byte array
    val keyByteArr = keySerializer.apply(groupingKey).asInstanceOf[UnsafeRow].getBytes()
    // get user key byte array
    val userKeySerializer = encoderFor(userKeyEnc).createSerializer()
    val userKeyBytesArr = userKeySerializer.apply(userKey).asInstanceOf[UnsafeRow].getBytes()

    val schemaForCompositeKeyRow: StructType =
      new StructType()
        .add("key", BinaryType)
        .add("userKey", BinaryType)

    val compositeKeyProjection = UnsafeProjection.create(schemaForCompositeKeyRow)
    val compositeKeyRow = compositeKeyProjection(InternalRow(keyByteArr, userKeyBytesArr))
    compositeKeyRow
  }

  def decodeCompositeKey[K](row: UnsafeRow, userKeyEnc: Encoder[K]): K = {
    val bytes = row.getBinary(1)
    val reuseRow = new UnsafeRow(userKeyEnc.schema.fields.length)
    reuseRow.pointTo(bytes, bytes.length)
    val valExpressionEnc = encoderFor(userKeyEnc)
    val rowToObjDeserializer = valExpressionEnc.resolveAndBind().createDeserializer()
    val value = rowToObjDeserializer.apply(reuseRow)
    value
  }

  def encodeValue[S](value: S): UnsafeRow = {
    val valueByteArr = SerializationUtils.serialize(value.asInstanceOf[Serializable])
    val valueRow = valueProjection(InternalRow(valueByteArr))
    valueRow
  }

  def decodeValue[S](row: UnsafeRow): S = {
    SerializationUtils
      .deserialize(row.getBinary(0))
      .asInstanceOf[S]
  }
}

object StateTypesEncoder {
  def apply[GK](
      keySerializer: Serializer[GK],
      stateName: String): StateTypesEncoder[GK] = {
    new StateTypesEncoder[GK](keySerializer, stateName)
  }
}
