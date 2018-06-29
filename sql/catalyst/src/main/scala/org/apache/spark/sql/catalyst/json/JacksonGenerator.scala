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

package org.apache.spark.sql.catalyst.json

import java.io.Writer
import java.nio.charset.StandardCharsets

import com.fasterxml.jackson.core._

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.SpecializedGetters
import org.apache.spark.sql.catalyst.util.{ArrayData, DateTimeUtils, MapData}
import org.apache.spark.sql.types._

/**
 * `JackGenerator` can only be initialized with a `StructType` or a `MapType`.
 * Once it is initialized with `StructType`, it can be used to write out a struct or an array of
 * struct. Once it is initialized with `MapType`, it can be used to write out a map or an array
 * of map. An exception will be thrown if trying to write out a struct if it is initialized with
 * a `MapType`, and vice verse.
 */
private[sql] class JacksonGenerator(
    dataType: DataType,
    writer: Writer,
    options: JSONOptions) {
  // A `ValueWriter` is responsible for writing a field of an `InternalRow` to appropriate
  // JSON data. Here we are using `SpecializedGetters` rather than `InternalRow` so that
  // we can directly access data in `ArrayData` without the help of `SpecificMutableRow`.
  private type ValueWriter = (SpecializedGetters, Int) => Unit

  // A `ValueReader` is responsible for reading a field of an `InternalRow` to a String.
  // The only purpose of this is to read the key values for a map so that they can be
  // written as JSON filed names.
  private type ValueReader = (SpecializedGetters, Int) => String

  // `JackGenerator` can only be initialized with a `StructType` or a `MapType`.
  require(dataType.isInstanceOf[StructType] || dataType.isInstanceOf[MapType],
    "JacksonGenerator only supports to be initialized with a StructType " +
      s"or MapType but got ${dataType.simpleString}")

  // `ValueWriter`s for all fields of the schema
  private lazy val rootFieldWriters: Array[ValueWriter] = dataType match {
    case st: StructType => st.map(_.dataType).map(makeWriter).toArray
    case _ => throw new UnsupportedOperationException(
      s"Initial type ${dataType.simpleString} must be a struct")
  }

  // `ValueWriter` for array data storing rows of the schema.
  private lazy val arrElementWriter: ValueWriter = dataType match {
    case st: StructType =>
      (arr: SpecializedGetters, i: Int) => {
        writeObject(writeFields(arr.getStruct(i, st.length), st, rootFieldWriters))
      }
    case mt: MapType =>
      (arr: SpecializedGetters, i: Int) => {
        writeObject(writeMapData(arr.getMap(i), mapKeyReader, mapValueWriter))
      }
  }

  private lazy val mapValueWriter: ValueWriter = dataType match {
    case mt: MapType => makeWriter(mt.valueType)
    case _ => throw new UnsupportedOperationException(
      s"Initial type ${dataType.simpleString} must be a map")
  }

  private lazy val mapKeyReader: ValueReader = dataType match {
    case mt: MapType => makeReader(mt.keyType)
    case _ => throw new UnsupportedOperationException(
      s"Initial type ${dataType.simpleString} must be a map")
  }

  private val gen = new JsonFactory().createGenerator(writer).setRootValueSeparator(null)

  private val lineSeparator: String = options.lineSeparatorInWrite

  private def makeWriter(dataType: DataType): ValueWriter = dataType match {
    case NullType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNull()

    case BooleanType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeBoolean(row.getBoolean(ordinal))

    case ByteType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNumber(row.getByte(ordinal))

    case ShortType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNumber(row.getShort(ordinal))

    case IntegerType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNumber(row.getInt(ordinal))

    case LongType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNumber(row.getLong(ordinal))

    case FloatType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNumber(row.getFloat(ordinal))

    case DoubleType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNumber(row.getDouble(ordinal))

    case StringType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeString(row.getUTF8String(ordinal).toString)

    case TimestampType =>
      (row: SpecializedGetters, ordinal: Int) =>
        val timestampString =
          options.timestampFormat.format(DateTimeUtils.toJavaTimestamp(row.getLong(ordinal)))
        gen.writeString(timestampString)

    case DateType =>
      (row: SpecializedGetters, ordinal: Int) =>
        val dateString =
          options.dateFormat.format(DateTimeUtils.toJavaDate(row.getInt(ordinal)))
        gen.writeString(dateString)

    case BinaryType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeBinary(row.getBinary(ordinal))

    case dt: DecimalType =>
      (row: SpecializedGetters, ordinal: Int) =>
        gen.writeNumber(row.getDecimal(ordinal, dt.precision, dt.scale).toJavaBigDecimal)

    case st: StructType =>
      val fieldWriters = st.map(_.dataType).map(makeWriter)
      (row: SpecializedGetters, ordinal: Int) =>
        writeObject(writeFields(row.getStruct(ordinal, st.length), st, fieldWriters))

    case at: ArrayType =>
      val elementWriter = makeWriter(at.elementType)
      (row: SpecializedGetters, ordinal: Int) =>
        writeArray(writeArrayData(row.getArray(ordinal), elementWriter))

    case mt: MapType =>
      val keyReader = makeReader(mt.keyType)
      val valueWriter = makeWriter(mt.valueType)
      (row: SpecializedGetters, ordinal: Int) =>
        writeObject(writeMapData(row.getMap(ordinal), keyReader, valueWriter))

    // For UDT values, they should be in the SQL type's corresponding value type.
    // We should not see values in the user-defined class at here.
    // For example, VectorUDT's SQL type is an array of double. So, we should expect that v is
    // an ArrayData at here, instead of a Vector.
    case t: UserDefinedType[_] =>
      makeWriter(t.sqlType)

    case _ =>
      (row: SpecializedGetters, ordinal: Int) =>
        val v = row.get(ordinal, dataType)
        sys.error(s"Failed to convert value $v (class of ${v.getClass}}) " +
          s"with the type of $dataType to JSON.")
  }

  private def makeReader(dataType: DataType): ValueReader = dataType match {

    case TimestampType =>
      (row: SpecializedGetters, ordinal: Int) =>
        options.timestampFormat.format(DateTimeUtils.toJavaTimestamp(row.getLong(ordinal)))

    case DateType =>
      (row: SpecializedGetters, ordinal: Int) =>
        options.dateFormat.format(DateTimeUtils.toJavaDate(row.getInt(ordinal)))

    case _ =>
      (row: SpecializedGetters, ordinal: Int) =>
        row.get(ordinal, dataType).toString

  }

  private def writeObject(f: => Unit): Unit = {
    gen.writeStartObject()
    f
    gen.writeEndObject()
  }

  private def writeFields(
      row: InternalRow, schema: StructType, fieldWriters: Seq[ValueWriter]): Unit = {
    var i = 0
    while (i < row.numFields) {
      val field = schema(i)
      if (!row.isNullAt(i)) {
        gen.writeFieldName(field.name)
        fieldWriters(i).apply(row, i)
      }
      i += 1
    }
  }

  private def writeArray(f: => Unit): Unit = {
    gen.writeStartArray()
    f
    gen.writeEndArray()
  }

  private def writeArrayData(
      array: ArrayData, fieldWriter: ValueWriter): Unit = {
    var i = 0
    while (i < array.numElements()) {
      if (!array.isNullAt(i)) {
        fieldWriter.apply(array, i)
      } else {
        gen.writeNull()
      }
      i += 1
    }
  }

  private def writeMapData(
      map: MapData, keyReader: ValueReader, valueWriter: ValueWriter): Unit = {
    val keyArray = map.keyArray()
    val valueArray = map.valueArray()
    var i = 0
    while (i < map.numElements()) {
      gen.writeFieldName(keyReader.apply(keyArray, i))
      if (!valueArray.isNullAt(i)) {
        valueWriter.apply(valueArray, i)
      } else {
        gen.writeNull()
      }
      i += 1
    }
  }

  def close(): Unit = gen.close()

  def flush(): Unit = gen.flush()

  /**
   * Transforms a single `InternalRow` to JSON object using Jackson.
   * This api calling will be validated through accessing `rootFieldWriters`.
   *
   * @param row The row to convert
   */
  def write(row: InternalRow): Unit = {
    writeObject(writeFields(
      fieldWriters = rootFieldWriters,
      row = row,
      schema = dataType.asInstanceOf[StructType]))
  }

  /**
   * Transforms multiple `InternalRow`s or `MapData`s to JSON array using Jackson
   *
   * @param array The array of rows or maps to convert
   */
  def write(array: ArrayData): Unit = writeArray(writeArrayData(array, arrElementWriter))

  /**
   * Transforms a single `MapData` to JSON object using Jackson
   * This api calling will will be validated through accessing `mapElementWriter`.
   *
   * @param map a map to convert
   */
  def write(map: MapData): Unit = {
    writeObject(writeMapData(
      keyReader = mapKeyReader,
      valueWriter = mapValueWriter,
      map = map))
  }

  def writeLineEnding(): Unit = {
    // Note that JSON uses writer with UTF-8 charset. This string will be written out as UTF-8.
    gen.writeRaw(lineSeparator)
  }
}
