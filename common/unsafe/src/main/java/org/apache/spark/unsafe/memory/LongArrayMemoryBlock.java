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

package org.apache.spark.unsafe.memory;

import org.apache.spark.unsafe.Platform;

/**
 * A consecutive block of memory, starting at a {@link MemoryLocation} with a fixed size.
 */
public class LongArrayMemoryBlock extends MemoryLocation implements MemoryBlock {

  private final long size;

  /**
   * Optional page number; used when this MemoryBlock represents a page allocated by a
   * TaskMemoryManager. This field is public so that it can be modified by the TaskMemoryManager,
   * which lives in a different package.
   */
  private int pageNumber = -1;

  public LongArrayMemoryBlock(long[] obj, long offset, long size) {
    super(obj, offset);
    this.size = size;
  }

  /**
   * Returns the size of the memory block.
   */
  public long size() {
    return size;
  }

  @Override
  public void setPageNumber(int aPageNum) {
    this.pageNumber = aPageNum;
  }

  @Override
  public int getPageNumber() {
    return this.pageNumber;
  }

  public long[] getLongArray() { return (long[])this.obj; }

  /**
   * Creates a memory block pointing to the memory used by the long array.
   */
  public static LongArrayMemoryBlock fromLongArray(final long[] array) {
    return new LongArrayMemoryBlock(array, Platform.LONG_ARRAY_OFFSET, array.length*8);
  }
}
