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
package org.apache.spark.streaming.util

import java.io._
import java.net.URI
import java.nio.ByteBuffer

import scala.util.Try

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataOutputStream, FileSystem}

/**
 * A writer for writing byte-buffers to a write ahead log file.
 */
private[streaming] class WriteAheadLogWriter(path: String, hadoopConf: Configuration)
  extends Closeable {

  private lazy val stream = HdfsUtils.getOutputStream(path, hadoopConf)

  private lazy val hadoopFlushMethod = {
    val cls = classOf[FSDataOutputStream]
    Try(cls.getMethod("hflush")).orElse(Try(cls.getMethod("sync"))).toOption
  }

  private var nextOffset = getPosition()
  private var closed = false


  /** Write the bytebuffer to the log file */
  def write(data: ByteBuffer): FileSegment = synchronized {
    assertOpen()
    data.rewind() // Rewind to ensure all data in the buffer is retrieved
    val lengthToWrite = data.remaining()
    val segment = new FileSegment(path, nextOffset, lengthToWrite)
    stream.writeInt(lengthToWrite)
    if (data.hasArray) {
      stream.write(data.array())
    } else {
      // If the buffer is not backed by an array we need to write the data byte by byte
      while (data.hasRemaining) {
        stream.write(data.get())
      }
    }
    flush()
    nextOffset = getPosition()
    segment
  }

  override private[streaming] def close(): Unit = synchronized {
    closed = true
    stream.close()
  }


  private def getPosition(): Long = {
    stream.getPos()
  }

  private def flush() {
    hadoopFlushMethod.foreach {
      _.invoke(stream)
    }
  }

  private def assertOpen() {
    HdfsUtils.checkState(!closed, "Stream is closed. Create a new Writer to write to file.")
  }
}
