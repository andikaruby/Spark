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

package org.apache.spark.network.buffer;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Preconditions;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.IllegalReferenceCountException;

import org.apache.spark.network.util.LimitedInputStream;

public class InputStreamManagedBuffer extends ManagedBuffer {
  private final LimitedInputStream inputStream;
  private final long limit;
  private boolean hasRead = false;
  private ChunkedByteBuffer buffer = null;
  private final AbstractReferenceCounted referenceCounter = new AbstractReferenceCounted() {
    @Override
    protected void deallocate() {
      try {
        buffer = null;
        inputStream.close();
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  };

  public InputStreamManagedBuffer(InputStream in, long byteCount) {
    this(in, byteCount, true);
  }

  public InputStreamManagedBuffer(InputStream in, long byteCount, boolean closeWrappedStream) {
    this.inputStream = new LimitedInputStream(in, byteCount, closeWrappedStream);
    this.limit = byteCount;
  }

  public long size() {
    return limit;
  }

  public ChunkedByteBuffer nioByteBuffer() throws IOException {
    ensureAccessible();
    if (hasRead) return buffer;
    hasRead = true;
    buffer = ChunkedByteBuffer.wrap(inputStream, 32 * 1024);
    return buffer;
  }

  public InputStream createInputStream() throws IOException {
    ensureAccessible();
    Preconditions.checkState(!hasRead, "nioByteBuffer has been called!");
    return inputStream;
  }

  public ManagedBuffer retain() {
    ensureAccessible();
    referenceCounter.retain();
    return this;
  }

  public ManagedBuffer release() {
    ensureAccessible();
    referenceCounter.release();
    return this;
  }

  public Object convertToNetty() throws IOException {
    ensureAccessible();
    throw new UnsupportedOperationException("convertToNetty");
  }

  /**
   * Should be called by every method that tries to access the buffers content to check
   * if the buffer was released before.
   */
  protected final void ensureAccessible() {
    if (referenceCounter.refCnt() == 0) throw new IllegalReferenceCountException(0);
  }
}
