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

package org.apache.spark.sql.execution.columnar.compression

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.catalyst.expressions.GenericMutableRow
import org.apache.spark.sql.execution.columnar._
import org.apache.spark.sql.execution.columnar.ColumnarTestUtils._
import org.apache.spark.sql.types.IntegralType

class IntegralDeltaSuite extends SparkFunSuite {
  val nullValue = -1
  testIntegralDelta(new IntColumnStats, INT, IntDelta)
  testIntegralDelta(new LongColumnStats, LONG, LongDelta)

  def testIntegralDelta[I <: IntegralType](
      columnStats: ColumnStats,
      columnType: NativeColumnType[I],
      scheme: CompressionScheme) {

    def skeleton(input: Seq[I#InternalType]) {
      // -------------
      // Tests encoder
      // -------------

      val builder = TestCompressibleColumnBuilder(columnStats, columnType, scheme)
      val deltas = if (input.isEmpty) {
        Seq.empty[Long]
      } else {
        (input.tail, input.init).zipped.map {
          case (x: Int, y: Int) => (x - y).toLong
          case (x: Long, y: Long) => x - y
        }
      }

      input.map { value =>
        val row = new GenericMutableRow(1)
        columnType.setField(row, 0, value)
        builder.appendFrom(row, 0)
      }

      val buffer = builder.build()
      // Column type ID + null count + null positions
      val headerSize = CompressionScheme.columnHeaderSize(buffer)

      // Compression scheme ID + compressed contents
      val compressedSize = 4 + (if (deltas.isEmpty) {
        0
      } else {
        val oneBoolean = columnType.defaultSize
        1 + oneBoolean + deltas.map {
          d => if (math.abs(d) <= Byte.MaxValue) 1 else 1 + oneBoolean
        }.sum
      })

      // 4 extra bytes for compression scheme type ID
      assertResult(headerSize + compressedSize, "Wrong buffer capacity")(buffer.capacity)

      buffer.position(headerSize)
      assertResult(scheme.typeId, "Wrong compression scheme ID")(buffer.getInt())

      if (input.nonEmpty) {
        assertResult(Byte.MinValue, "The first byte should be an escaping mark")(buffer.get())
        assertResult(input.head, "The first value is wrong")(columnType.extract(buffer))

        (input.tail, deltas).zipped.foreach { (value, delta) =>
          if (math.abs(delta) <= Byte.MaxValue) {
            assertResult(delta, "Wrong delta")(buffer.get())
          } else {
            assertResult(Byte.MinValue, "Expecting escaping mark here")(buffer.get())
            assertResult(value, "Wrong value")(columnType.extract(buffer))
          }
        }
      }

      // -------------
      // Tests decoder
      // -------------

      // Rewinds, skips column header and 4 more bytes for compression scheme ID
      buffer.rewind().position(headerSize + 4)

      val decoder = scheme.decoder(buffer, columnType)
      val mutableRow = new GenericMutableRow(1)

      if (input.nonEmpty) {
        input.foreach{
          assert(decoder.hasNext)
          assertResult(_, "Wrong decoded value") {
            decoder.next(mutableRow, 0)
            columnType.getField(mutableRow, 0)
          }
        }
      }
      assert(!decoder.hasNext)
    }

    def skeletonForDecompress(input: Seq[I#InternalType]) {
      val builder = TestCompressibleColumnBuilder(columnStats, columnType, scheme)
      val row = new GenericMutableRow(1)
      val nullRow = new GenericMutableRow(1)
      nullRow.setNullAt(0)
      input.map { value =>
        if (value == nullValue) {
          builder.appendFrom(nullRow, 0)
        } else {
          columnType.setField(row, 0, value)
          builder.appendFrom(row, 0)
        }
      }
      val buffer = builder.build()

      // ----------------
      // Tests decompress
      // ----------------
      // Rewinds, skips column header and 4 more bytes for compression scheme ID
      val headerSize = CompressionScheme.columnHeaderSize(buffer)
      buffer.position(headerSize)
      assertResult(scheme.typeId, "Wrong compression scheme ID")(buffer.getInt())

      val decoder = scheme.decoder(buffer, columnType)
      val (decodeBuffer, nullsBuffer) = decoder.decompress(input.length)

      if (input.nonEmpty) {
        val numNulls = ByteBufferHelper.getInt(nullsBuffer)
        var cntNulls = 0
        var nullPos = if (numNulls == 0) -1 else ByteBufferHelper.getInt(nullsBuffer)
        input.zipWithIndex.foreach {
          case (expected: Any, index: Int) if expected == nullValue =>
            assertResult(index, "Wrong null position") {
              nullPos
            }
            decodeBuffer.position(decodeBuffer.position + columnType.defaultSize)
            cntNulls += 1
            if (cntNulls < numNulls) {
              nullPos = ByteBufferHelper.getInt(nullsBuffer)
            }
          case (expected: Int, index: Int) =>
            assertResult(expected, s"Wrong ${index}-th decoded int value") {
              ByteBufferHelper.getInt(decodeBuffer)
            }
          case (expected: Long, index: Int) =>
            assertResult(expected, s"Wrong ${index}-th decoded long value") {
              ByteBufferHelper.getLong(decodeBuffer)
            }
          case _ =>
            fail("Unsupported type")
        }
      }
      assert(!decodeBuffer.hasRemaining)
    }

    test(s"$scheme: empty column") {
      skeleton(Seq.empty)
    }

    test(s"$scheme: simple case") {
      val input = columnType match {
        case INT => Seq(2: Int, 1: Int, 2: Int, 130: Int)
        case LONG => Seq(2: Long, 1: Long, 2: Long, 130: Long)
      }

      skeleton(input.map(_.asInstanceOf[I#InternalType]))
    }

    test(s"$scheme: long random series") {
      // Have to workaround with `Any` since no `ClassTag[I#JvmType]` available here.
      val input = Array.fill[Any](10000)(makeRandomValue(columnType))
      skeleton(input.map(_.asInstanceOf[I#InternalType]))
    }

    test(s"$scheme: empty column for decompress()") {
      skeletonForDecompress(Seq.empty)
    }

    test(s"$scheme: simple case for decompress()") {
      val input = columnType match {
        case INT => Seq(2: Int, 1: Int, 2: Int, 130: Int)
        case LONG => Seq(2: Long, 1: Long, 2: Long, 130: Long)
      }

      skeletonForDecompress(input.map(_.asInstanceOf[I#InternalType]))
    }

    test(s"$scheme: simple case with null for decompress()") {
      val input = columnType match {
        case INT => Seq(2: Int, 1: Int, 2: Int, nullValue: Int, 5: Int)
        case LONG => Seq(2: Long, 1: Long, 2: Long, nullValue: Long, 5: Long)
      }

      skeletonForDecompress(input.map(_.asInstanceOf[I#InternalType]))
    }
  }
}
