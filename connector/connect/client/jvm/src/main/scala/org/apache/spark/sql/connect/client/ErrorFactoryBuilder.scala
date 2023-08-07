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
package org.apache.spark.sql.connect.client

import scala.collection.mutable
import scala.reflect.ClassTag

private[client] class ErrorFactoryBuilder {

  private val constructors = mutable.Map.empty[String, (String, Throwable) => Throwable]

  /**
   * registerConstructors register throwableCtr in ErrorFactoryBuilder.
   * @param throwableCtr the constructor that construct Throwable based on message and cause.
   * @return this
   */
  def registerConstructor[T <: Throwable: ClassTag](
      throwableCtr: (String, Throwable) => T
  ): ErrorFactoryBuilder = {
    val className = implicitly[reflect.ClassTag[T]].runtimeClass.getName
    assert(!constructors.contains(className))
    constructors(className) = throwableCtr
    this
  }

  def build(): Map[String, (String, Throwable) => Throwable] = {
    constructors.toMap
  }
}
