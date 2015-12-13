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


class IntersectNodeSuite extends LocalNodeTest {

  test("basic") {
    val n = 100
    val leftData = (1 to n).filter { i => i % 2 == 0 }.map { i => (i, i) }.toArray
    val rightData = (1 to n).filter { i => i % 3 == 0 }.map { i => (i, i) }.toArray
    val leftNode = DummyNode(kvIntAttributes, leftData)
    val rightNode = DummyNode(kvIntAttributes, rightData)
    val intersectNode = new IntersectNode(conf, leftNode, rightNode)
    val expectedOutput = leftData.intersect(rightData)
    val actualOutput = intersectNode.collect().map { case row =>
      (row.getInt(0), row.getInt(1))
    }
    assert(actualOutput === expectedOutput)
  }

}
