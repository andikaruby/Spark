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

package org.apache.spark.sql.parquet

import scala.collection.mutable.{Buffer, ArrayBuffer, HashMap}
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.runtimeMirror

import parquet.io.api.{PrimitiveConverter, GroupConverter, Binary, Converter}
import parquet.schema.MessageType

import org.apache.spark.sql.catalyst.types._
import org.apache.spark.sql.catalyst.expressions.{GenericRow, Row, Attribute}
import org.apache.spark.sql.parquet.CatalystConverter.FieldType
import org.apache.spark.util.Utils

private[parquet] object CatalystConverter {
  // The type internally used for fields
  type FieldType = StructField

  // This is mostly Parquet convention (see, e.g., `ConversionPatterns`).
  // Note that "array" for the array elements is chosen by ParquetAvro.
  // Using a different value will result in Parquet silently dropping columns.
  val ARRAY_ELEMENTS_SCHEMA_NAME = "array"
  val MAP_KEY_SCHEMA_NAME = "key"
  val MAP_VALUE_SCHEMA_NAME = "value"
  val MAP_SCHEMA_NAME = "map"

  protected[parquet] def createConverter(
      field: FieldType,
      fieldIndex: Int,
      parent: CatalystConverter): Converter = {
    val fieldType: DataType = field.dataType
    fieldType match {
      // For native JVM types we use a converter with native arrays
      case ArrayType(elementType: NativeType) => {
        new CatalystNativeArrayConverter(elementType, fieldIndex, parent)
      }
      // This is for other types of arrays, including those with nested fields
      case ArrayType(elementType: DataType) => {
        new CatalystArrayConverter(elementType, fieldIndex, parent)
      }
      case StructType(fields: Seq[StructField]) => {
        new CatalystStructConverter(fields, fieldIndex, parent)
      }
      case MapType(keyType: DataType, valueType: DataType) => {
        new CatalystMapConverter(
          Seq(
            new FieldType(MAP_KEY_SCHEMA_NAME, keyType, false),
            new FieldType(MAP_VALUE_SCHEMA_NAME, valueType, true)),
            fieldIndex,
            parent)
      }
      case ctype: NativeType => {
        // note: for some reason matching for StringType fails so use this ugly if instead
        if (ctype == StringType) {
          new CatalystPrimitiveStringConverter(parent, fieldIndex)
        } else {
          new CatalystPrimitiveConverter(parent, fieldIndex)
        }
      }
      case _ => throw new RuntimeException(
        s"unable to convert datatype ${field.dataType.toString} in CatalystConverter")
    }
  }

  protected[parquet] def createRootConverter(parquetSchema: MessageType): CatalystConverter = {
    val attributes = ParquetTypesConverter.convertToAttributes(parquetSchema)
    // For non-nested types we use the optimized Row converter
    if (attributes.forall(a => ParquetTypesConverter.isPrimitiveType(a.dataType))) {
      new MutableRowGroupConverter(attributes)
    } else {
      new CatalystGroupConverter(attributes)
    }
  }
}

private[parquet] trait CatalystConverter {
  // the number of fields this group has
  protected[parquet] val size: Int

  // the index of this converter in the parent
  protected[parquet] val index: Int

  // the parent converter
  protected[parquet] val parent: CatalystConverter

  // for child converters to update upstream values
  protected[parquet] def updateField(fieldIndex: Int, value: Any): Unit

  protected[parquet] def updateBoolean(fieldIndex: Int, value: Boolean): Unit =
    updateField(fieldIndex, value)

  protected[parquet] def updateInt(fieldIndex: Int, value: Int): Unit =
    updateField(fieldIndex, value)

  protected[parquet] def updateLong(fieldIndex: Int, value: Long): Unit =
    updateField(fieldIndex, value)

  protected[parquet] def updateDouble(fieldIndex: Int, value: Double): Unit =
    updateField(fieldIndex, value)

  protected[parquet] def updateFloat(fieldIndex: Int, value: Float): Unit =
    updateField(fieldIndex, value)

  protected[parquet] def updateBinary(fieldIndex: Int, value: Binary): Unit =
    updateField(fieldIndex, value.getBytes)

  protected[parquet] def updateString(fieldIndex: Int, value: Binary): Unit =
    updateField(fieldIndex, value.toStringUsingUTF8)

  protected[parquet] def isRootConverter: Boolean = parent == null

  protected[parquet] def clearBuffer(): Unit

  // Should be only called in root group converter!
  def getCurrentRecord: Row
}

/**
 * A `parquet.io.api.GroupConverter` that is able to convert a Parquet record
 * to a [[org.apache.spark.sql.catalyst.expressions.Row]] object.
 *
 * @param schema The corresponding Catalyst schema in the form of a list of attributes.
 */
private[parquet] class CatalystGroupConverter(
    protected[parquet] val schema: Seq[FieldType],
    protected[parquet] val index: Int,
    protected[parquet] val parent: CatalystConverter,
    protected[parquet] var current: ArrayBuffer[Any],
    protected[parquet] var buffer: ArrayBuffer[Row])
  extends GroupConverter with CatalystConverter {

  def this(schema: Seq[FieldType], index: Int, parent: CatalystConverter) =
    this(
      schema,
      index,
      parent,
      current=null,
      buffer=new ArrayBuffer[Row](
        CatalystArrayConverter.INITIAL_ARRAY_SIZE))

  // This constructor is used for the root converter only
  def this(attributes: Seq[Attribute]) =
    this(attributes.map(a => new FieldType(a.name, a.dataType, a.nullable)), 0, null)

  protected [parquet] val converters: Array[Converter] =
    schema.map(field =>
      CatalystConverter.createConverter(field, schema.indexOf(field), this))
    .toArray

  override val size = schema.size

  // Should be only called in root group converter!
  def getCurrentRecord: Row = {
    assert(isRootConverter, "getCurrentRecord should only be called in root group converter!")
    // TODO: use iterators if possible
    new GenericRow(current.toArray)
  }

  override def getConverter(fieldIndex: Int): Converter = converters(fieldIndex)

  // for child converters to update upstream values
  override protected[parquet] def updateField(fieldIndex: Int, value: Any): Unit = {
    current.update(fieldIndex, value)
  }

  override protected[parquet] def clearBuffer(): Unit = {
    // TODO: reuse buffer?
    buffer = new ArrayBuffer[Row](CatalystArrayConverter.INITIAL_ARRAY_SIZE)
  }

  override def start(): Unit = {
    // TODO: reuse buffer?
    // Allocate new array in the root converter (others will be called clearBuffer() on)
    current = ArrayBuffer.fill(schema.length)(null)
    converters.foreach {
      converter => if (!converter.isPrimitive) {
        converter.asInstanceOf[CatalystConverter].clearBuffer
      }
    }
  }

  // TODO: think about reusing the buffer
  override def end(): Unit = {
    if (!isRootConverter) {
      assert(current!=null) // there should be no empty groups
      buffer.append(new GenericRow(current.toArray))
      // TODO: use iterators if possible, avoid Row wrapping
      parent.updateField(index, new GenericRow(buffer.toArray.asInstanceOf[Array[Any]]))
    }
  }
}

/**
 * A `parquet.io.api.GroupConverter` that is able to convert a Parquet record
 * to a [[org.apache.spark.sql.catalyst.expressions.Row]] object. Note that his
 * converter is optimized for rows of primitive types (non-nested records).
 */
private[parquet] class MutableRowGroupConverter(
    protected[parquet] val schema: Seq[FieldType],
    protected[parquet] var current: ParquetRelation.RowType)
  extends GroupConverter with CatalystConverter {

  // This constructor is used for the root converter only
  def this(attributes: Seq[Attribute]) =
    this(
      attributes.map(a => new FieldType(a.name, a.dataType, a.nullable)),
      new ParquetRelation.RowType(attributes.length))

  protected [parquet] val converters: Array[Converter] =
    schema.map(field =>
      CatalystConverter.createConverter(field, schema.indexOf(field), this))
      .toArray

  override val size = schema.size

  override val index = 0

  override val parent = null

  // Should be only called in root group converter!
  def getCurrentRecord: ParquetRelation.RowType = current

  override def getConverter(fieldIndex: Int): Converter = converters(fieldIndex)

  // for child converters to update upstream values
  override protected[parquet] def updateField(fieldIndex: Int, value: Any): Unit = {
    throw new UnsupportedOperationException // child converters should use the
    // specific update methods below
  }

  override protected[parquet] def clearBuffer(): Unit = {}

  override def start(): Unit = {
    var i = 0
    while (i < schema.length) {
      current.setNullAt(i)
      i = i + 1
    }
  }

  override def end(): Unit = {}

  // Overriden here to avoid auto-boxing for primitive types
  override protected[parquet] def updateBoolean(fieldIndex: Int, value: Boolean): Unit =
    current.setBoolean(fieldIndex, value)

  override protected[parquet] def updateInt(fieldIndex: Int, value: Int): Unit =
    current.setInt(fieldIndex, value)

  override protected[parquet] def updateLong(fieldIndex: Int, value: Long): Unit =
    current.setLong(fieldIndex, value)

  override protected[parquet] def updateDouble(fieldIndex: Int, value: Double): Unit =
    current.setDouble(fieldIndex, value)

  override protected[parquet] def updateFloat(fieldIndex: Int, value: Float): Unit =
    current.setFloat(fieldIndex, value)

  override protected[parquet] def updateBinary(fieldIndex: Int, value: Binary): Unit =
    current.update(fieldIndex, value.getBytes)

  override protected[parquet] def updateString(fieldIndex: Int, value: Binary): Unit =
    current.setString(fieldIndex, value.toStringUsingUTF8)
}

/**
 * A `parquet.io.api.PrimitiveConverter` that converts Parquet types to Catalyst types.
 *
 * @param parent The parent group converter.
 * @param fieldIndex The index inside the record.
 */
private[parquet] class CatalystPrimitiveConverter(
    parent: CatalystConverter,
    fieldIndex: Int) extends PrimitiveConverter {
  // TODO: consider refactoring these together with ParquetTypesConverter
  override def addBinary(value: Binary): Unit =
    parent.updateBinary(fieldIndex, value)

  override def addBoolean(value: Boolean): Unit =
    parent.updateBoolean(fieldIndex, value)

  override def addDouble(value: Double): Unit =
    parent.updateDouble(fieldIndex, value)

  override def addFloat(value: Float): Unit =
    parent.updateFloat(fieldIndex, value)

  override def addInt(value: Int): Unit =
    parent.updateInt(fieldIndex, value)

  override def addLong(value: Long): Unit =
    parent.updateLong(fieldIndex, value)
}

/**
 * A `parquet.io.api.PrimitiveConverter` that converts Parquet strings (fixed-length byte arrays)
 * into Catalyst Strings.
 *
 * @param parent The parent group converter.
 * @param fieldIndex The index inside the record.
 */
private[parquet] class CatalystPrimitiveStringConverter(
    parent: CatalystConverter,
    fieldIndex: Int)
  extends CatalystPrimitiveConverter(parent, fieldIndex) {
  override def addBinary(value: Binary): Unit =
    parent.updateString(fieldIndex, value)
}

object CatalystArrayConverter {
  val INITIAL_ARRAY_SIZE = 20
}

/**
 * A `parquet.io.api.GroupConverter` that converts a single-element groups that
 * match the characteristics of an array (see
 * [[org.apache.spark.sql.parquet.ParquetTypesConverter]]) into an
 * [[org.apache.spark.sql.catalyst.types.ArrayType]].
 *
 * @param elementType The type of the array elements
 * @param index The position of this (array) field inside its parent converter
 * @param parent The parent converter
 * @param buffer A data buffer
 */
private[parquet] class CatalystArrayConverter(
    val elementType: DataType,
    val index: Int,
    protected[parquet] val parent: CatalystConverter,
    protected[parquet] var buffer: Buffer[Any])
  extends GroupConverter with CatalystConverter {
  // TODO: In the future consider using native arrays instead of buffer for
  // primitive types for performance reasons

  def this(elementType: DataType, index: Int, parent: CatalystConverter) =
    this(
      elementType,
      index,
      parent,
      new ArrayBuffer[Any](CatalystArrayConverter.INITIAL_ARRAY_SIZE))

  protected[parquet] val converter: Converter = CatalystConverter.createConverter(
    new CatalystConverter.FieldType(
      CatalystConverter.ARRAY_ELEMENTS_SCHEMA_NAME,
      elementType,
      false),
    fieldIndex=0,
    parent=this)

  override def getConverter(fieldIndex: Int): Converter = converter

  // arrays have only one (repeated) field, which is its elements
  override val size = 1

  override protected[parquet] def updateField(fieldIndex: Int, value: Any): Unit = {
    // fieldIndex is ignored (assumed to be zero but not checked)
    buffer += value
  }

  override protected[parquet] def clearBuffer(): Unit = {
    // TODO: reuse buffer?
    buffer = new ArrayBuffer[Any](CatalystArrayConverter.INITIAL_ARRAY_SIZE)
  }

  override def start(): Unit = {
    if (!converter.isPrimitive) {
      converter.asInstanceOf[CatalystConverter].clearBuffer
    }
  }

  // TODO: think about reusing the buffer
  override def end(): Unit = {
    assert(parent != null)
    // TODO: use iterators if possible, avoid Row wrapping
    parent.updateField(index, new GenericRow(buffer.toArray))
    clearBuffer()
  }

  // Should be only called in root group converter!
  override def getCurrentRecord: Row = throw new UnsupportedOperationException
}

private[parquet] class CatalystNativeArrayConverter[T <: NativeType](
    val elementType: NativeType,
    val index: Int,
    protected[parquet] val parent: CatalystConverter,
    protected[parquet] var capacity: Int = CatalystArrayConverter.INITIAL_ARRAY_SIZE)
  extends GroupConverter with CatalystConverter {

  // similar comment as in [[Decoder]]: this should probably be in NativeType
  private val classTag = {
    val mirror = runtimeMirror(Utils.getSparkClassLoader)
    ClassTag[T#JvmType](mirror.runtimeClass(elementType.tag.tpe))
  }

  private var buffer: Array[T#JvmType] = classTag.newArray(capacity)

  private var elements: Int = 0

  protected[parquet] val converter: Converter = CatalystConverter.createConverter(
    new CatalystConverter.FieldType(
      CatalystConverter.ARRAY_ELEMENTS_SCHEMA_NAME,
      elementType,
      false),
    fieldIndex=0,
    parent=this)

  override def getConverter(fieldIndex: Int): Converter = converter

  // arrays have only one (repeated) field, which is its elements
  override val size = 1

  override protected[parquet] def updateField(fieldIndex: Int, value: Any): Unit =
    throw new UnsupportedOperationException

  // Overriden here to avoid auto-boxing for primitive types
  override protected[parquet] def updateBoolean(fieldIndex: Int, value: Boolean): Unit = {
    checkGrowBuffer()
    buffer(elements) = value.asInstanceOf[T#JvmType]
    elements += 1
  }

  override protected[parquet] def updateInt(fieldIndex: Int, value: Int): Unit = {
    checkGrowBuffer()
    buffer(elements) = value.asInstanceOf[T#JvmType]
    elements += 1
  }

  override protected[parquet] def updateLong(fieldIndex: Int, value: Long): Unit = {
    checkGrowBuffer()
    buffer(elements) = value.asInstanceOf[T#JvmType]
    elements += 1
  }

  override protected[parquet] def updateDouble(fieldIndex: Int, value: Double): Unit = {
    checkGrowBuffer()
    buffer(elements) = value.asInstanceOf[T#JvmType]
    elements += 1
  }

  override protected[parquet] def updateFloat(fieldIndex: Int, value: Float): Unit = {
    checkGrowBuffer()
    buffer(elements) = value.asInstanceOf[T#JvmType]
    elements += 1
  }

  override protected[parquet] def updateBinary(fieldIndex: Int, value: Binary): Unit = {
    checkGrowBuffer()
    buffer(elements) = value.getBytes.asInstanceOf[T#JvmType]
    elements += 1
  }

  override protected[parquet] def updateString(fieldIndex: Int, value: Binary): Unit = {
    checkGrowBuffer()
    buffer(elements) = value.toStringUsingUTF8.asInstanceOf[T#JvmType]
    elements += 1
  }

  override protected[parquet] def clearBuffer(): Unit = {
    elements = 0
  }

  override def start(): Unit = {}

  override def end(): Unit = {
    assert(parent != null)
    parent.updateField(
      index,
      new GenericRow {
        // TODO: it would be much nicer to use a view here but GenericRow requires an Array
        // TODO: we should avoid using GenericRow as a wrapper but [[GetField]] current
        // requires that
        override val values = buffer.slice(0, elements).map(_.asInstanceOf[Any])
      })
    clearBuffer()
  }

  // Should be only called in root group converter!
  override def getCurrentRecord: Row = throw new UnsupportedOperationException

  private def checkGrowBuffer(): Unit = {
    if (elements >= capacity) {
      val newCapacity = 2 * capacity
      val tmp: Array[T#JvmType] = classTag.newArray(newCapacity)
      Array.copy(buffer, 0, tmp, 0, capacity)
      buffer = tmp
      capacity = newCapacity
    }
  }
}

// this is for multi-element groups of primitive or complex types
// that have repetition level optional or required (so struct fields)
private[parquet] class CatalystStructConverter(
    override protected[parquet] val schema: Seq[FieldType],
    override protected[parquet] val index: Int,
    override protected[parquet] val parent: CatalystConverter)
  extends CatalystGroupConverter(schema, index, parent) {

  override protected[parquet] def clearBuffer(): Unit = {}

  // TODO: think about reusing the buffer
  override def end(): Unit = {
    assert(!isRootConverter)
    // TODO: use iterators if possible, avoid Row wrapping!
    parent.updateField(index, new GenericRow(current.toArray))
  }

  // Should be only called in root group converter!
  override def getCurrentRecord: Row = throw new UnsupportedOperationException
}

private[parquet] class CatalystMapConverter(
    protected[parquet] val schema: Seq[FieldType],
    override protected[parquet] val index: Int,
    override protected[parquet] val parent: CatalystConverter)
  extends GroupConverter with CatalystConverter {

  private val map = new HashMap[Any, Any]()

  private val keyValueConverter = new GroupConverter with CatalystConverter {
    private var currentKey: Any = null
    private var currentValue: Any = null
    val keyConverter = CatalystConverter.createConverter(schema(0), 0, this)
    val valueConverter = CatalystConverter.createConverter(schema(1), 1, this)

    override def getConverter(fieldIndex: Int): Converter = {
      if (fieldIndex == 0) keyConverter else valueConverter
    }

    override def end(): Unit = CatalystMapConverter.this.map += currentKey -> currentValue

    override def start(): Unit = {
      currentKey = null
      currentValue = null
    }

    override protected[parquet] val size: Int = 2
    override protected[parquet] val index: Int = 0
    override protected[parquet] val parent: CatalystConverter = CatalystMapConverter.this

    override protected[parquet] def updateField(fieldIndex: Int, value: Any): Unit = {
      fieldIndex match {
        case 0 =>
          currentKey = value
        case 1 =>
          currentValue = value
        case _ =>
          new RuntimePermission(s"trying to update Map with fieldIndex $fieldIndex")
      }
    }

    override protected[parquet] def clearBuffer(): Unit = {}
    override def getCurrentRecord: Row = throw new UnsupportedOperationException
  }

  override protected[parquet] val size: Int = 1

  override protected[parquet] def clearBuffer(): Unit = {}

  override def start(): Unit = {
    map.clear()
  }

  // TODO: think about reusing the buffer
  override def end(): Unit = {
    parent.updateField(index, map.toMap)
  }

  override def getConverter(fieldIndex: Int): Converter = keyValueConverter

  override def getCurrentRecord: Row = throw new UnsupportedOperationException

  override protected[parquet] def updateField(fieldIndex: Int, value: Any): Unit =
    throw new UnsupportedOperationException
}



