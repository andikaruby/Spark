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

package org.apache.spark.deploy.rest

import scala.collection.Map
import scala.collection.mutable

import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST._

import org.apache.spark.{Logging, SparkException}
import org.apache.spark.util.{JsonProtocol, Utils}

/**
 * A field used in a SubmitRestProtocolMessage.
 * Three special fields ACTION, SPARK_VERSION, and MESSAGE are common across all messages.
 */
private[spark] abstract class SubmitRestProtocolField
private[spark] object SubmitRestProtocolField {
  def isActionField(field: String): Boolean = field == "ACTION"
  def isSparkVersionField(field: String): Boolean = field == "SPARK_VERSION"
  def isMessageField(field: String): Boolean = field == "MESSAGE"
}

/**
 * All possible values of the ACTION field.
 */
private[spark] object SubmitRestProtocolAction extends Enumeration {
  type SubmitRestProtocolAction = Value
  val SUBMIT_DRIVER_REQUEST, SUBMIT_DRIVER_RESPONSE = Value
  val KILL_DRIVER_REQUEST, KILL_DRIVER_RESPONSE = Value
  val DRIVER_STATUS_REQUEST, DRIVER_STATUS_RESPONSE = Value
  val ERROR = Value
}
import SubmitRestProtocolAction.SubmitRestProtocolAction

/**
 * A general message exchanged in the stable application submission REST protocol.
 *
 * The message is represented by a set of fields in the form of key value pairs.
 * Each message must contain an ACTION field, which should have only one possible value
 * for each type of message. For compatibility with older versions of Spark, existing
 * fields must not be removed or modified, though new fields can be added as necessary.
 */
private[spark] abstract class SubmitRestProtocolMessage(
    action: SubmitRestProtocolAction,
    actionField: SubmitRestProtocolField,
    requiredFields: Seq[SubmitRestProtocolField]) {

  import SubmitRestProtocolField._

  private val className = Utils.getFormattedClassName(this)
  protected val fields = new mutable.HashMap[SubmitRestProtocolField, String]

  // Set the action field
  fields(actionField) = action.toString

  /** Return all fields currently set in this message. */
  def getFields: Map[SubmitRestProtocolField, String] = fields

  /** Return the value of the given field. If the field is not present, return null. */
  def getField(key: SubmitRestProtocolField): String = getFieldOption(key).orNull

  /** Return the value of the given field. If the field is not present, throw an exception. */
  def getFieldNotNull(key: SubmitRestProtocolField): String = {
    getFieldOption(key).getOrElse {
      throw new IllegalArgumentException(s"Field $key is not set in message $className")
    }
  }

  /** Return the value of the given field as an option. */
  def getFieldOption(key: SubmitRestProtocolField): Option[String] = fields.get(key)

  /** Assign the given value to the field, overriding any existing value. */
  def setField(key: SubmitRestProtocolField, value: String): this.type = {
    if (key == actionField) {
      throw new SparkException("Setting the ACTION field is only allowed during instantiation.")
    }
    fields(key) = value
    this
  }

  /** Assign the given value to the field only if the value is not null. */
  def setFieldIfNotNull(key: SubmitRestProtocolField, value: String): this.type = {
    if (value != null) {
      setField(key, value)
    }
    this
  }

  /**
   * Validate that all required fields are set and the value of the action field is as expected.
   * If any of these conditions are not met, throw an IllegalArgumentException.
   */
  def validate(): this.type = {
    if (!fields.contains(actionField)) {
      throw new IllegalArgumentException(s"The action field is missing from message $className.")
    }
    if (fields(actionField) != action.toString) {
      throw new IllegalArgumentException(
        s"Expected action $action in message $className, but actual was ${fields(actionField)}.")
    }
    val missingFields = requiredFields.filterNot(fields.contains)
    if (missingFields.nonEmpty) {
      val missingFieldsString = missingFields.mkString(", ")
      throw new IllegalArgumentException(
        s"The following fields are missing from message $className: $missingFieldsString.")
    }
    this
  }

  /** Return the JSON representation of this message. */
  def toJson: String = {
    val jsonFields = sortedFields
      .filter { case (_, v) => v != null }
      .map { case (k, v) => JField(k.toString, JString(v)) }
      .toList
    pretty(render(JObject(jsonFields)))
  }

  /**
   * Return a list of (field, value) pairs with the following ordering:
   * ACTION < SPARK_VERSION < * < MESSAGE
   */
  protected def sortedFields: Seq[(SubmitRestProtocolField, String)] = {
    fields.toSeq.sortBy { case (k, _) =>
      k.toString match {
        case x if isActionField(x) => 0
        case x if isSparkVersionField(x) => 1
        case x if isMessageField(x) => Int.MaxValue
        case _ => 2
      }
    }
  }
}

private[spark] object SubmitRestProtocolMessage {
  import SubmitRestProtocolField._
  import SubmitRestProtocolAction._

  /**
   * Construct a SubmitRestProtocolMessage from JSON.
   * This uses the ACTION field to determine the type of the message to reconstruct.
   * If such a field does not exist in the JSON, throw an exception.
   */
  def fromJson(json: String): SubmitRestProtocolMessage = {
    val fields = JsonProtocol.mapFromJson(parse(json))
    val action = fields
      .flatMap { case (k, v) => if (isActionField(k)) Some(v) else None }
      .headOption
      .getOrElse { throw new IllegalArgumentException(s"ACTION not found in message:\n$json") }
    SubmitRestProtocolAction.withName(action) match {
      case SUBMIT_DRIVER_REQUEST => SubmitDriverRequestMessage.fromFields(fields)
      case SUBMIT_DRIVER_RESPONSE => SubmitDriverResponseMessage.fromFields(fields)
      case KILL_DRIVER_REQUEST => KillDriverRequestMessage.fromFields(fields)
      case KILL_DRIVER_RESPONSE => KillDriverResponseMessage.fromFields(fields)
      case DRIVER_STATUS_REQUEST => DriverStatusRequestMessage.fromFields(fields)
      case DRIVER_STATUS_RESPONSE => DriverStatusResponseMessage.fromFields(fields)
      case ERROR => ErrorMessage.fromFields(fields)
    }
  }
}

/**
 * A trait that holds common methods for SubmitRestProtocolField companion objects.
 *
 * It is necessary to keep track of all fields that belong to this object in order to
 * reconstruct the fields from their names.
 */
private[spark] trait SubmitRestProtocolFieldCompanion {
  val requiredFields: Seq[SubmitRestProtocolField]
  val optionalFields: Seq[SubmitRestProtocolField]

  /** Listing of all fields indexed by the field's string representation. */
  private lazy val allFieldsMap: Map[String, SubmitRestProtocolField] = {
    (requiredFields ++ optionalFields).map { f => (f.toString, f) }.toMap
  }

  /** Return a SubmitRestProtocolField from its string representation. */
  def withName(field: String): SubmitRestProtocolField = {
    allFieldsMap.get(field).getOrElse {
      throw new IllegalArgumentException(s"Unknown field $field")
    }
  }
}

/**
 * A trait that holds common methods for SubmitRestProtocolMessage companion objects.
 */
private[spark] trait SubmitRestProtocolMessageCompanion extends Logging {
  import SubmitRestProtocolField._

  /** Construct a new message of the relevant type. */
  protected def newMessage(): SubmitRestProtocolMessage

  /** Return a field of the relevant type from the field's string representation. */
  protected def fieldWithName(field: String): SubmitRestProtocolField

  /** Construct a SubmitRestProtocolMessage from the set of fields provided. */
  def fromFields(fields: Map[String, String]): SubmitRestProtocolMessage = {
    val message = newMessage()
    fields.foreach { case (k, v) =>
      try {
        // The ACTION field is already set on instantiation
        if (!isActionField(k)) {
          message.setField(fieldWithName(k), v)
        }
      } catch {
        case e: IllegalArgumentException =>
          logWarning(s"Unexpected field $k in message ${Utils.getFormattedClassName(this)}")
      }
    }
    message
  }
}
