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

import org.apache.spark.sql.Encoder
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.encoders.{encoderFor, ExpressionEncoder}
import org.apache.spark.sql.catalyst.expressions.{GenericInternalRow, UnsafeProjection, UnsafeRow}
import org.apache.spark.sql.execution.streaming.TransformWithStateKeyValueRowSchemaUtils._
import org.apache.spark.sql.execution.streaming.state.StateStoreErrors
import org.apache.spark.sql.types._

object TransformWithStateKeyValueRowSchemaUtils {
  def getCompositeKeySchema(
      groupingKeySchema: StructType,
      userKeySchema: StructType): StructType = {
    new StructType()
      .add("key", new StructType(groupingKeySchema.fields))
      .add("userKey", new StructType(userKeySchema.fields))
  }

  def getSingleKeyTTLRowSchema(keySchema: StructType): StructType =
    new StructType()
      .add("expirationMs", LongType)
      .add("groupingKey", keySchema)

  def getCompositeKeyTTLRowSchema(
      groupingKeySchema: StructType,
      userKeySchema: StructType): StructType =
    new StructType()
      .add("expirationMs", LongType)
      .add("groupingKey", new StructType(groupingKeySchema.fields))
      .add("userKey", new StructType(userKeySchema.fields))

  def getValueRowSchemaWithTTL(valueRowSchema: StructType): StructType =
    valueRowSchema.add("ttlExpirationMs", LongType)

  def getValueSchemaWithTTL(schema: StructType, hasTTL: Boolean): StructType = {
    val valSchema = if (hasTTL) {
      new StructType(schema.fields).add("ttlExpirationMs", LongType)
    } else schema
    new StructType()
      .add("value", valSchema)
  }
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
 * @param keyEncoder - SQL encoder for the grouping key, key type is implicit
 * @param valEncoder - SQL encoder for value of type `S`
 * @param stateName - name of logical state partition
 * @tparam V - value type
 */
class StateTypesEncoder[V](
    keyEncoder: ExpressionEncoder[Any],
    valEncoder: Encoder[V],
    stateName: String,
    hasTtl: Boolean) {

  /** Variables reused for value conversions between spark sql and object */
  private val keySerializer = keyEncoder.createSerializer()
  private val valExpressionEnc = encoderFor(valEncoder)
  private val objToRowSerializer = valExpressionEnc.createSerializer()
  private val rowToObjDeserializer = valExpressionEnc.resolveAndBind().createDeserializer()
  private val valueTTLProjection =
    UnsafeProjection.create(valEncoder.schema.add("ttlExpirationMs", LongType))

  // TODO: validate places that are trying to encode the key and check if we can eliminate/
  // add caching for some of these calls.
  def encodeGroupingKey(): UnsafeRow = {
    val keyOption = ImplicitGroupingKeyTracker.getImplicitKeyOption
    if (keyOption.isEmpty) {
      throw StateStoreErrors.implicitKeyNotFound(stateName)
    }

    keySerializer.apply(keyOption.get).asInstanceOf[UnsafeRow]
  }

  /**
   * Encode the specified value in Spark UnsafeRow with no ttl.
   */
  def encodeValue(value: V): UnsafeRow = {
    objToRowSerializer.apply(value).asInstanceOf[UnsafeRow]
  }

  /**
   * Encode the specified value in Spark UnsafeRow
   * with provided ttl expiration.
   */
  def encodeValue(value: V, expirationMs: Long): UnsafeRow = {
    val objRow: InternalRow = objToRowSerializer.apply(value)
    val newValArr: Array[Any] =
      objRow.toSeq(valEncoder.schema).toArray :+ expirationMs

    valueTTLProjection.apply(new GenericInternalRow(newValArr))
  }

  def decodeValue(row: UnsafeRow): V = {
    rowToObjDeserializer.apply(row)
  }

  /**
   * Decode the ttl information out of Value row. If the ttl has
   * not been set (-1L specifies no user defined value), the API will
   * return None.
   */
  def decodeTtlExpirationMs(row: UnsafeRow): Option[Long] = {
    // ensure ttl has been set
    assert(hasTtl)
    val expirationMs = row.getLong(valEncoder.schema.length)
    if (expirationMs == -1) {
      None
    } else {
      Some(expirationMs)
    }
  }

  def isExpired(row: UnsafeRow, batchTimestampMs: Long): Boolean = {
    val expirationMs = decodeTtlExpirationMs(row)
    expirationMs.exists(StateTTL.isExpired(_, batchTimestampMs))
  }
}

object StateTypesEncoder {
  def apply[V](
      keyEncoder: ExpressionEncoder[Any],
      valEncoder: Encoder[V],
      stateName: String,
      hasTtl: Boolean = false): StateTypesEncoder[V] = {
    new StateTypesEncoder[V](keyEncoder, valEncoder, stateName, hasTtl)
  }
}

class CompositeKeyStateEncoder[K, V](
    keyEncoder: ExpressionEncoder[Any],
    userKeyEnc: Encoder[K],
    valEncoder: Encoder[V],
    stateName: String,
    hasTtl: Boolean = false)
  extends StateTypesEncoder[V](keyEncoder, valEncoder, stateName, hasTtl) {
  import org.apache.spark.sql.execution.streaming.TransformWithStateKeyValueRowSchemaUtils._

  /** Encoders */
  private val userKeyExpressionEnc = encoderFor(userKeyEnc)

  /** Schema */
  private val schemaForGroupingKeyRow = new StructType().add("key", keyEncoder.schema)
  private val schemaForUserKeyRow = new StructType().add("userKey", userKeyEnc.schema)
  private val schemaForCompositeKeyRow =
    getCompositeKeySchema(keyEncoder.schema, userKeyEnc.schema)

  /** Projection */
  private val userKeyProjection = UnsafeProjection.create(schemaForUserKeyRow)
  private val groupingKeyProjection = UnsafeProjection.create(schemaForGroupingKeyRow)
  private val compositeKeyProjection = UnsafeProjection.create(schemaForCompositeKeyRow)

  /** Serializer */
  private val groupingKeySerializer = keyEncoder.createSerializer()
  private val userKeySerializer = userKeyExpressionEnc.createSerializer()

  /** Deserializer */
  private val userKeyRowToObjDeserializer =
    userKeyExpressionEnc.resolveAndBind().createDeserializer()

  override def encodeGroupingKey(): UnsafeRow = {
    val keyOption = ImplicitGroupingKeyTracker.getImplicitKeyOption
    if (keyOption.isEmpty) {
      throw StateStoreErrors.implicitKeyNotFound(stateName)
    }
    val groupingKey = keyOption.get
    val groupingKeyRow = groupingKeySerializer.apply(groupingKey)

    // Create the final unsafeRow mapping column name "key" to the keyRow
    groupingKeyProjection(InternalRow(groupingKeyRow))
  }

  def encodeUserKey(userKey: K): UnsafeRow = {
    val userKeyRow = userKeySerializer.apply(userKey)

    // Create the final unsafeRow mapping column name "userKey" to the userKeyRow
    userKeyProjection(InternalRow(userKeyRow))
  }


  /**
   * Grouping key and user key are encoded as a row of `schemaForCompositeKeyRow` schema.
   * Grouping key will be encoded in `RocksDBStateEncoder` as the prefix column.
   */
  def encodeCompositeKey(userKey: K): UnsafeRow = {
    val keyOption = ImplicitGroupingKeyTracker.getImplicitKeyOption
    if (keyOption.isEmpty) {
      throw StateStoreErrors.implicitKeyNotFound(stateName)
    }
    val groupingKey = keyOption.get

    val keyRow = groupingKeySerializer.apply(groupingKey)
    val userKeyRow = userKeySerializer.apply(userKey)

    // Create the final unsafeRow combining the keyRow and userKeyRow
    compositeKeyProjection(InternalRow(keyRow, userKeyRow))
  }

  def decodeUserKey(row: UnsafeRow): K = {
    userKeyRowToObjDeserializer.apply(row)
  }

  /**
   * The input row is of composite Key schema.
   * Only user key is returned though grouping key also exist in the row.
   */
  def decodeCompositeKey(row: UnsafeRow): K = {
    userKeyRowToObjDeserializer.apply(row.getStruct(1, userKeyEnc.schema.length))
  }
}

class SingleKeyTTLEncoder(
  keyExprEnc: ExpressionEncoder[Any]) {

  private val TTLKeySchema = getSingleKeyTTLRowSchema(keyExprEnc.schema)
  def encodeTTLRow(expirationMs: Long, groupingKey: UnsafeRow): UnsafeRow = {
    UnsafeProjection.create(TTLKeySchema).apply(
      InternalRow(expirationMs, groupingKey.asInstanceOf[InternalRow]))
  }
}

class CompositeKeyTTLEncoder[K](
  keyExprEnc: ExpressionEncoder[Any],
  userKeyEnc: Encoder[K]) {

  private val TTLKeySchema = getCompositeKeyTTLRowSchema(
    keyExprEnc.schema, userKeyEnc.schema)
  def encodeTTLRow(
      expirationMs: Long,
      groupingKey: UnsafeRow,
      userKey: UnsafeRow): UnsafeRow = {
    UnsafeProjection.create(TTLKeySchema).apply(
      new GenericInternalRow(
        Array[Any](expirationMs,
        groupingKey.getStruct(0, keyExprEnc.schema.length)
          .asInstanceOf[InternalRow],
        userKey.getStruct(0, userKeyEnc.schema.length)
          .asInstanceOf[InternalRow])))
  }
}
