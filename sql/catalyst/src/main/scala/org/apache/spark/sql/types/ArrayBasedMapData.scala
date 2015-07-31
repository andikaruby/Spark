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

package org.apache.spark.sql.types

class ArrayBasedMapData(val keyArray: ArrayData, val valueArray: ArrayData) extends MapData {
  require(keyArray.numElements() == valueArray.numElements())

  override def numElements(): Int = keyArray.numElements()

  override def equals(o: Any): Boolean = {
    if (!o.isInstanceOf[ArrayBasedMapData]) {
      return false
    }

    val other = o.asInstanceOf[ArrayBasedMapData]
    if (other eq null) {
      return false
    }

    this.keyArray == other.keyArray && this.valueArray == other.valueArray
  }

  override def hashCode: Int = {
    keyArray.hashCode() * 37 + valueArray.hashCode()
  }
}

object ArrayBasedMapData {
  def apply(keys: Array[Any], values: Array[Any]): ArrayBasedMapData = {
    new ArrayBasedMapData(new GenericArrayData(keys), new GenericArrayData(values))
  }
}
