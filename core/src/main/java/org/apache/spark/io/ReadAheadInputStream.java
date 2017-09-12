/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.io;

import com.google.common.base.Preconditions;
import org.apache.spark.util.ThreadUtils;

import javax.annotation.concurrent.GuardedBy;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link InputStream} implementation which asynchronously reads ahead from the underlying input
 * stream when specified amount of data has been read from the current buffer. It does it by maintaining
 * two buffer - active buffer and read ahead buffer. Active buffer contains data which should be returned
 * when a read() call is issued. The read ahead buffer is used to asynchronously read from the underlying
 * input stream and once the current active buffer is exhausted, we flip the two buffers so that we can
 * start reading from the read ahead buffer without being blocked in disk I/O.
 */
public class ReadAheadInputStream extends InputStream {

  private ReentrantLock stateChangeLock = new ReentrantLock();

  @GuardedBy("stateChangeLock")
  private ByteBuffer activeBuffer;

  @GuardedBy("stateChangeLock")
  private ByteBuffer readAheadBuffer;

  @GuardedBy("stateChangeLock")
  private boolean endOfStream;

  @GuardedBy("stateChangeLock")
  // true if async read is in progress
  private boolean readInProgress;

  @GuardedBy("stateChangeLock")
  // true if read is aborted due to an exception in reading from underlying input stream.
  private boolean readAborted;

  @GuardedBy("stateChangeLock")
  private Exception readException;

  // If the remaining data size in the current buffer is below this threshold,
  // we issue an async read from the underlying input stream.
  private final int readAheadThresholdInBytes;

  private final InputStream underlyingInputStream;

  private final ExecutorService executorService = ThreadUtils.newDaemonSingleThreadExecutor("read-ahead");

  private final Condition asyncReadComplete = stateChangeLock.newCondition();

  private static final ThreadLocal<byte[]> oneByte = ThreadLocal.withInitial(() -> new byte[1]);

  /**
   * Creates a <code>ReadAheadInputStream</code> with the specified buffer size and read-ahead
   * threshold
   *
   * @param   inputStream                 The underlying input stream.
   * @param   bufferSizeInBytes           The buffer size.
   * @param   readAheadThresholdInBytes   If the active buffer has less data than the read-ahead
   *                                      threshold, an async read is triggered.
   */
  public ReadAheadInputStream(InputStream inputStream, int bufferSizeInBytes, int readAheadThresholdInBytes) {
    Preconditions.checkArgument(bufferSizeInBytes > 0,
            "bufferSizeInBytes should be greater than 0, but the value is " + bufferSizeInBytes);
    Preconditions.checkArgument(readAheadThresholdInBytes > 0 &&
                    readAheadThresholdInBytes < bufferSizeInBytes,
            "readAheadThresholdInBytes should be greater than 0 and less than bufferSizeInBytes, but the" +
                    "value is " + readAheadThresholdInBytes );
    activeBuffer = ByteBuffer.allocate(bufferSizeInBytes);
    readAheadBuffer = ByteBuffer.allocate(bufferSizeInBytes);
    this.readAheadThresholdInBytes = readAheadThresholdInBytes;
    this.underlyingInputStream = inputStream;
    activeBuffer.flip();
    readAheadBuffer.flip();
  }

  private boolean isEndOfStream() {
    return (!activeBuffer.hasRemaining() && !readAheadBuffer.hasRemaining() && endOfStream);
  }

  private void readAsync(final ByteBuffer byteBuffer) throws IOException {
    stateChangeLock.lock();
    final byte[] arr = byteBuffer.array();
    try {
      if (endOfStream || readInProgress) {
        return;
      }
      byteBuffer.position(0);
      byteBuffer.flip();
      readInProgress = true;
    } finally {
      stateChangeLock.unlock();
    }
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        // Please note that it is safe to release the lock and read into the read ahead buffer
        // because either of following two conditions will hold - 1. The active buffer has
        // data available to read so the reader will not read from the read ahead buffer.
        // 2. This is the first time read is called or the active buffer is exhausted,
        // in that case the reader waits for this async read to complete.
        // So there is no race condition in both the situations.
        boolean handled = false;
        int read = 0;
        Exception exception = null;
        try {
          while (true) {
            read = underlyingInputStream.read(arr);
            if (0 != read) break;
          }
          handled = true;
        } catch (Exception ex) {
          exception = ex;
        } finally {
          stateChangeLock.lock();
          if (read < 0 || (exception instanceof EOFException) ) {
            endOfStream = true;
          } else if (!handled) {
            readAborted = true;
            readException = exception != null ? exception: new Exception("Unknown exception in ReadAheadInputStream");
          } else {
            byteBuffer.limit(read);
          }
          readInProgress = false;
          signalAsyncReadComplete();
          stateChangeLock.unlock();
        }
      }
    });
  }


  private void signalAsyncReadComplete() {
    stateChangeLock.lock();
    try {
      asyncReadComplete.signalAll();
    } finally {
      stateChangeLock.unlock();
    }
  }

  private void waitForAsyncReadComplete() throws IOException {
    stateChangeLock.lock();
    try {
      while (readInProgress)
      asyncReadComplete.await();
    } catch (InterruptedException e) {
      throw  new InterruptedIOException(e.getMessage());
    } finally {
      stateChangeLock.unlock();
    }
  }

  @Override
  public int read() throws IOException {
    int val = read(oneByte.get(), 0, 1);
    if (val == -1) {
      return -1;
    }
    return oneByte.get()[0] & 0xFF;
  }

  @Override
  public int read(byte[] b, int offset, int len) throws IOException {
    if (offset < 0 || len < 0 || offset + len < 0 || offset + len > b.length) {
      throw new IndexOutOfBoundsException();
    }
    if (len == 0) {
      return 0;
    }
    stateChangeLock.lock();
    try {
      return readInternal(b, offset, len);
    } finally {
      stateChangeLock.unlock();
    }
  }

  /**
   * Internal read function which should be called only from read() api. The assumption is that
   * the stateChangeLock is already acquired in the caller before calling this function.
   */
  private int readInternal(byte[] b, int offset, int len) throws IOException {
    assert (stateChangeLock.isLocked());
    if (!activeBuffer.hasRemaining()) {
      if (!readInProgress) {
        // This condition will only be triggered for the first time read is called.
        readAsync(activeBuffer);
      }
      waitForAsyncReadComplete();
    }
    if (readAborted) {
      throw new IOException(readException);
    }
    if (isEndOfStream()) {
      return -1;
    }
    len = Math.min(len, activeBuffer.remaining());
    activeBuffer.get(b, offset, len);

    if (activeBuffer.remaining() <= readAheadThresholdInBytes && !readAheadBuffer.hasRemaining()) {
      readAsync(readAheadBuffer);
    }
    if (!activeBuffer.hasRemaining()) {
      ByteBuffer temp = activeBuffer;
      activeBuffer = readAheadBuffer;
      readAheadBuffer = temp;
    }
    return len;
  }

  @Override
  public int available() throws IOException {
    stateChangeLock.lock();
    // Make sure we have no integer overflow.
    try {
      return (int) Math.min((long) Integer.MAX_VALUE,
              (long) activeBuffer.remaining() + readAheadBuffer.remaining());
    } finally {
      stateChangeLock.unlock();
    }
  }

  @Override
  public long skip(long n) throws IOException {
    if (n <= 0L) {
      return 0L;
    }
    stateChangeLock.lock();
    long skipped;
    try {
      skipped = skipInternal(n);
    } finally {
      stateChangeLock.unlock();
    }
    return skipped;
  }

  /**
   * Internal skip function which should be called only from skip() api. The assumption is that
   * the stateChangeLock is already acquired in the caller before calling this function.
   */
  private long skipInternal(long n) throws IOException {
    assert (stateChangeLock.isLocked());
    if (readInProgress) {
      waitForAsyncReadComplete();
    }
    if (available() >= n) {
      // we can skip from the internal buffers
      int toSkip = (int)n;
      if (toSkip <= activeBuffer.remaining()) {
        // Only skipping from active buffer is sufficient
        activeBuffer.position(toSkip + activeBuffer.position());
        return n;
      }
      // We need to skip from both active buffer and read ahead buffer
      toSkip -= activeBuffer.remaining();
      activeBuffer.position(0);
      activeBuffer.flip();
      readAheadBuffer.position(toSkip + readAheadBuffer.position());
      // flip the active and read ahead buffer
      ByteBuffer temp = activeBuffer;
      activeBuffer = readAheadBuffer;
      readAheadBuffer = temp;
      readAsync(readAheadBuffer);
      return n;
    }
    int skippedBytes = available();
    long toSkip = n - skippedBytes;
    activeBuffer.position(0);
    activeBuffer.flip();
    readAheadBuffer.position(0);
    readAheadBuffer.flip();
    long skippedFromInputStream = underlyingInputStream.skip(toSkip);
    readAsync(activeBuffer);
    return skippedBytes + skippedFromInputStream;
  }

  @Override
  public void close() throws IOException {
    executorService.shutdown();
    try {
      executorService.awaitTermination(10, TimeUnit.SECONDS);
      stateChangeLock.lock();
      underlyingInputStream.close();
    } catch (InterruptedException e) {
      throw new InterruptedIOException(e.getMessage());
    } finally {
      stateChangeLock.unlock();
    }
  }
}
