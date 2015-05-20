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
package org.apache.spark.status.api.v1

import java.io.OutputStream
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import javax.ws.rs.Produces
import javax.ws.rs.core.{MultivaluedMap, MediaType}
import javax.ws.rs.ext.{Provider, MessageBodyWriter}

class DownloadMessageWriter extends MessageBodyWriter[Object] {

  override def writeTo(t: Object, `type`: Class[_], genericType: Type,
    annotations: Array[Annotation], mediaType: MediaType,
    httpHeaders: MultivaluedMap[String, AnyRef], entityStream: OutputStream): Unit = {
    t match {
      case downloader @ EventLogDownloadResource(_) =>
        downloader.getEventLogs()
    }
  }

  override def getSize(t: Object, `type`: Class[_], genericType: Type,
    annotations: Array[Annotation], mediaType: MediaType): Long = {
    -1L
  }

  override def isWriteable(`type`: Class[_], genericType: Type, annotations: Array[Annotation],
    mediaType: MediaType): Boolean = true
}
