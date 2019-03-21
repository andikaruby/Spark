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

package org.apache.spark.serializer

import org.roaringbitmap.RoaringBitmap

import org.apache.spark.internal.config.Kryo._

class UnsafeKryoSerializerSuite extends KryoSerializerSuite {

  // This test suite should run all tests in KryoSerializerSuite with kryo unsafe.

  override def beforeAll() {
    conf.set(KRYO_USE_UNSAFE, true)
    super.beforeAll()
  }

  override def afterAll() {
    conf.set(KRYO_USE_UNSAFE, false)
    super.afterAll()
  }

  test("SPARK-27216: kryo serialization with RoaringBitmap") {
    val expected = new RoaringBitmap
    expected.add(1787)

    conf.set(KRYO_USE_UNSAFE, false)
    val safeSer = new KryoSerializer(conf).newInstance()
    var actual : RoaringBitmap = safeSer.deserialize(safeSer.serialize(expected))
    assert(actual === expected)

    conf.set(KRYO_USE_UNSAFE, true)
    val unsafeSer = new KryoSerializer(conf).newInstance()
    actual = unsafeSer.deserialize(unsafeSer.serialize(expected))
    assert(actual !== expected)
  }
}
