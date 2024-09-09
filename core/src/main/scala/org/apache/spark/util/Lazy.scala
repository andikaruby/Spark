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

package org.apache.spark.util

import scala.util.Try

/**
 * Wrapper utility for lazy val, with two differences compared to scala behavir:
 *
 * 1. In scala, when a `lazy val` field is initialized, it grabs the synchronized lock on the
 *    enclosing object instance. This can lead both to performance issues, and deadlocks.
 *    For example:
 *     a) Thread 1 entered a synchronized method, grabbing a coarse lock on the parent object.
 *     b) Thread 2 get spawned off, and tries to initialize a lazy value on the same parent object
 *        This causes scala to also try to grab a lock on the parent object.
 *     c) If thread 1 waits for thread 2 to join, a deadlock occurs.
 *   This Lazy wrapper will only grab a lock on the wrapper itself, and not the parent object.
 *
 * 2. In scala, when a `lazy val` field initialization throws an exception, the field remains
 *    uninitialized, and initialization will be re-attempted on the next access. This also can lead
 *    to performance issues, needlessly computing something towards a failure, and also can lead to
 *    duplicated side effects.
 *    This Lazy wrapper stores the exception in a Try, and will re-throw it on the next access.
 *
 * @param initialize The block of code to initialize the lazy value.
 * @tparam T type of the lazy value.
 */
private[spark] class Lazy[T](initialize: => T) extends Serializable {
  private lazy val tryT: Try[T] = Utils.doTryWithCallerStacktrace { initialize }

  /**
   * Get the lazy value. If the initialization block threw an exception, it will be re-thrown here.
   */
  def get: T = Utils.getTryWithCallerStacktrace(tryT)
}

private[spark] object Lazy {
  /**
   * Create a new Lazy instance.
   *
   * @param initialize The block of code to initialize the lazy value.
   * @tparam T type of the lazy value.
   * @return a new Lazy instance.
   */
  def apply[T](initialize: => T): Lazy[T] = new Lazy(initialize)
}
