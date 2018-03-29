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
package org.apache.spark.sql.catalyst.expressions.codegen;

import org.apache.spark.sql.types.Decimal;
import org.apache.spark.unsafe.Platform;
import org.apache.spark.unsafe.array.ByteArrayMethods;
import org.apache.spark.unsafe.types.CalendarInterval;
import org.apache.spark.unsafe.types.UTF8String;

/**
 * Base class for writing Unsafe* structures.
 */
public abstract class UnsafeWriter {
  // Keep internal buffer holder
  protected final BufferHolder holder;

  // The offset of the global buffer where we start to write this structure.
  protected int startingOffset;

  protected UnsafeWriter(BufferHolder holder) {
    this.holder = holder;
  }

  /**
   * Accessor methods are delegated from BufferHolder class
   */
  public final BufferHolder getBufferHolder() {
    return holder;
  }

  public final byte[] buffer() {
    return holder.buffer();
  }

  public final void reset() {
    holder.reset();
  }

  public final int totalSize() {
    return holder.totalSize();
  }

  public final void grow(int neededSize) {
    holder.grow(neededSize);
  }

  public final int cursor() {
    return holder.getCursor();
  }

  public final void incrementCursor(int val) {
    holder.incrementCursor(val);
  }

  public abstract void setOffsetAndSizeFromPreviousCursor(int ordinal, int previousCursor);

  protected void _setOffsetAndSizeFromPreviousCursor(int ordinal, int previousCursor) {
    setOffsetAndSize(ordinal, previousCursor, cursor() - previousCursor);
  }

  protected void setOffsetAndSize(int ordinal, int size) {
    setOffsetAndSize(ordinal, cursor(), size);
  }

  protected void setOffsetAndSize(int ordinal, int currentCursor, int size) {
    final long relativeOffset = currentCursor - startingOffset;
    final long offsetAndSize = (relativeOffset << 32) | (long)size;

    write(ordinal, offsetAndSize);
  }

  protected final void zeroOutPaddingBytes(int numBytes) {
    if ((numBytes & 0x07) > 0) {
      Platform.putLong(buffer(), cursor() + ((numBytes >> 3) << 3), 0L);
    }
  }

  public abstract void setNull1Bytes(int ordinal);
  public abstract void setNull2Bytes(int ordinal);
  public abstract void setNull4Bytes(int ordinal);
  public abstract void setNull8Bytes(int ordinal);

  public abstract void write(int ordinal, boolean value);
  public abstract void write(int ordinal, byte value);
  public abstract void write(int ordinal, short value);
  public abstract void write(int ordinal, int value);
  public abstract void write(int ordinal, long value);
  public abstract void write(int ordinal, float value);
  public abstract void write(int ordinal, double value);
  public abstract void write(int ordinal, Decimal input, int precision, int scale);

  public final void write(int ordinal, UTF8String input) {
    final int numBytes = input.numBytes();
    final int roundedSize = ByteArrayMethods.roundNumberOfBytesToNearestWord(numBytes);

    // grow the global buffer before writing data.
    grow(roundedSize);

    zeroOutPaddingBytes(numBytes);

    // Write the bytes to the variable length portion.
    input.writeToMemory(buffer(), cursor());

    setOffsetAndSize(ordinal, numBytes);

    // move the cursor forward.
    incrementCursor(roundedSize);
  }

  public final void write(int ordinal, byte[] input) {
    write(ordinal, input, 0, input.length);
  }

  public final void write(int ordinal, byte[] input, int offset, int numBytes) {
    final int roundedSize = ByteArrayMethods.roundNumberOfBytesToNearestWord(input.length);

    // grow the global buffer before writing data.
    grow(roundedSize);

    zeroOutPaddingBytes(numBytes);

    // Write the bytes to the variable length portion.
    Platform.copyMemory(
      input, Platform.BYTE_ARRAY_OFFSET + offset, buffer(), cursor(), numBytes);

    setOffsetAndSize(ordinal, numBytes);

    // move the cursor forward.
    incrementCursor(roundedSize);
  }

  public final void write(int ordinal, CalendarInterval input) {
    // grow the global buffer before writing data.
    grow(16);

    // Write the months and microseconds fields of Interval to the variable length portion.
    Platform.putLong(buffer(), cursor(), input.months);
    Platform.putLong(buffer(), cursor() + 8, input.microseconds);

    setOffsetAndSize(ordinal, 16);

    // move the cursor forward.
    incrementCursor(16);
  }

  protected final void writeBoolean(long offset, boolean value) {
    Platform.putBoolean(buffer(), offset, value);
  }

  protected final void writeByte(long offset, byte value) {
    Platform.putByte(buffer(), offset, value);
  }

  protected final void writeShort(long offset, short value) {
    Platform.putShort(buffer(), offset, value);
  }

  protected final void writeInt(long offset, int value) {
    Platform.putInt(buffer(), offset, value);
  }

  protected final void writeLong(long offset, long value) {
    Platform.putLong(buffer(), offset, value);
  }

  protected final void writeFloat(long offset, float value) {
    if (Float.isNaN(value)) {
      value = Float.NaN;
    }
    Platform.putFloat(buffer(), offset, value);
  }

  protected final void writeDouble(long offset, double value) {
    if (Double.isNaN(value)) {
      value = Double.NaN;
    }
    Platform.putDouble(buffer(), offset, value);
  }
}
