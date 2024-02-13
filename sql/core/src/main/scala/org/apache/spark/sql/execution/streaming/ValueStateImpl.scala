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

import scala.concurrent.duration.Duration

import org.apache.commons.lang3.SerializationUtils

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.logical.{NoTTL, ProcessingTimeTTL}
import org.apache.spark.sql.execution.streaming.state.{StateStore, StateStoreErrors}
import org.apache.spark.sql.streaming.{TTLMode, ValueState}
import org.apache.spark.sql.types._

/**
 * Class that provides a concrete implementation for a single value state associated with state
 * variables used in the streaming transformWithState operator.
 * @param store - reference to the StateStore instance to be used for storing state
 * @param stateName - name of logical state partition
 * @param keyEnc - Spark SQL encoder for key
 * @tparam K - data type of key
 * @tparam S - data type of object that will be stored
 */
class ValueStateImpl[S](
    store: StateStore,
    stateName: String,
    keyExprEnc: ExpressionEncoder[Any],
    ttlMode: TTLMode = TTLMode.NoTTL(),
    ttl: Duration = Duration.Zero) extends ValueState[S] with Logging {

  // TODO: validate places that are trying to encode the key and check if we can eliminate/
  // add caching for some of these calls.
  private def encodeKey(): UnsafeRow = {
    val keyOption = ImplicitGroupingKeyTracker.getImplicitKeyOption
    if (!keyOption.isDefined) {
      throw StateStoreErrors.implicitKeyNotFound(stateName)
    }

    val toRow = keyExprEnc.createSerializer()
    val keyByteArr = toRow
      .apply(keyOption.get).asInstanceOf[UnsafeRow].getBytes()

    val schemaForKeyRow: StructType = new StructType().add("key", BinaryType)
    val keyEncoder = UnsafeProjection.create(schemaForKeyRow)
    val keyRow = keyEncoder(InternalRow(keyByteArr))
    keyRow
  }

  private def getKeyBytes(): Array[Byte] = {
    val keyOption = ImplicitGroupingKeyTracker.getImplicitKeyOption
    if (!keyOption.isDefined) {
      throw StateStoreErrors.implicitKeyNotFound(stateName)
    }

    val toRow = keyExprEnc.createSerializer()
    toRow
      .apply(keyOption.get).asInstanceOf[UnsafeRow].getBytes()
  }
  private def addTTL(ttlByteArr: Array[Byte]): Unit = {
    store.createColFamilyIfAbsent("ttl_")
    val keyBytes = getKeyBytes()
    val schemaForTTLKeyRow: StructType = new StructType()
      .add("key", BinaryType).add("stateName", BinaryType)
    val ttlKeyRowEncoder = UnsafeProjection.create(schemaForTTLKeyRow)
    val ttlKeyRow = ttlKeyRowEncoder(InternalRow(keyBytes, stateName.getBytes))

    val schemaForTTLRow: StructType = new StructType()
      .add("ttl", BinaryType)
    val ttlRowEncoder = UnsafeProjection.create(schemaForTTLRow)
    val ttlRow = ttlRowEncoder(InternalRow(ttlByteArr))
    store.put(ttlRow, ttlKeyRow, "ttl_")
  }

  private def encodeValue(value: S): UnsafeRow = {
    val ttlForVal = ttlMode match {
      case NoTTL => -1L
      case ProcessingTimeTTL => System.currentTimeMillis() + ttl.toMillis
    }
    val schemaForValueRow: StructType = new StructType().add("value", BinaryType)
      .add("ttl", BinaryType)
    val valueByteArr = SerializationUtils.serialize(value.asInstanceOf[Serializable])
    val ttlByteArr = SerializationUtils.serialize(ttlForVal)
    ttlMode match {
      case ProcessingTimeTTL => addTTL(ttlByteArr)
      case _ =>
    }
    val valueEncoder = UnsafeProjection.create(schemaForValueRow)
    val valueRow = valueEncoder(InternalRow(valueByteArr, ttlByteArr))
    valueRow
  }

  /** Function to check if state exists. Returns true if present and false otherwise */
  override def exists(): Boolean = {
    get() != null
  }

  /** Function to return Option of value if exists and None otherwise */
  override def getOption(): Option[S] = {
    val retRow = getImpl()
    if (retRow != null) {
      val resState = SerializationUtils
        .deserialize(retRow.getBinary(0))
        .asInstanceOf[S]
      ttlMode match {
        case NoTTL =>
          Some(resState)
        case ProcessingTimeTTL =>
          val ttlForVal = SerializationUtils
            .deserialize(retRow.getBinary(1))
            .asInstanceOf[Long]
          if (ttlForVal <= System.currentTimeMillis()) {
            logDebug(s"Value is expired for state $stateName")
            store.remove(encodeKey(), stateName)
            None
          } else {
            Some(resState)
          }
      }
    } else {
      None
    }
  }

  /** Function to return associated value with key if exists and null otherwise */
  override def get(): S = {
    val retRow = getImpl()
    if (retRow != null) {
      val resState = SerializationUtils
        .deserialize(retRow.getBinary(0))
        .asInstanceOf[S]
      ttlMode match {
        case NoTTL =>
          resState
        case ProcessingTimeTTL =>
          val ttlForVal = SerializationUtils
            .deserialize(retRow.getBinary(1))
            .asInstanceOf[Long]
          if (ttlForVal <= System.currentTimeMillis()) {
            logDebug(s"Value is expired for state $stateName")
            store.remove(encodeKey(), stateName)
            null.asInstanceOf[S]
          } else {
            resState
          }
      }
    } else {
      null.asInstanceOf[S]
    }
  }

  private def getImpl(): UnsafeRow = {
    store.get(encodeKey(), stateName)
  }

  /** Function to update and overwrite state associated with given key */
  override def update(newState: S): Unit = {
    store.put(encodeKey(), encodeValue(newState), stateName)
  }

  /** Function to remove state for given key */
  override def remove(): Unit = {
    store.remove(encodeKey(), stateName)
  }
}
