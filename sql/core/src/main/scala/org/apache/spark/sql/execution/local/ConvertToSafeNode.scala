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

package org.apache.spark.sql.execution.local

import org.apache.spark.sql.SQLConf
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Attribute, FromUnsafeProjection, Projection}

case class ConvertToSafeNode(conf: SQLConf, child: LocalNode) extends UnaryLocalNode(conf) {

  override def output: Seq[Attribute] = child.output

  private[this] var convertToSafe: Projection = _

  override def open(): Unit = {
    child.open()
    convertToSafe = FromUnsafeProjection(child.output.map(_.dataType))
  }

  override def next(): Boolean = child.next()

  override def fetch(): InternalRow = convertToSafe(child.fetch())

  override def close(): Unit = child.close()
}
