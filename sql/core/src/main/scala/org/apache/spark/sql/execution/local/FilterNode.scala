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

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Attribute, Expression}
import org.apache.spark.sql.catalyst.expressions.codegen.GeneratePredicate

case class FilterNode(condition: Expression, child: LocalNode) extends UnaryLocalNode {

  private[this] var predicate: (InternalRow) => Boolean = _

  override def output: Seq[Attribute] = child.output

  override def open(): Unit = {
    child.open()
    predicate = GeneratePredicate.generate(condition, child.output)
  }

  override def next(): Boolean = {
    var found = false
    while (!found && child.next()) {
      found = predicate.apply(child.fetch())
    }
    found
  }

  override def fetch(): InternalRow = child.fetch()

  override def close(): Unit = child.close()
}
