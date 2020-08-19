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

package org.apache.spark

import _root_.io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite

import org.apache.spark.internal.Logging
import org.apache.spark.resource.ResourceProfile

/** Manages a local `sc` `SparkContext` variable, correctly stopping it after each test. */
trait LocalSparkContext extends Logging
  with BeforeAndAfterEach with BeforeAndAfterAll { self: Suite =>

  private var _conf: SparkConf = new SparkConf()

  @transient var sc: SparkContext = _
  @transient private var _sc: SparkContext = _

  /**
   * Currently, we are focusing on the reconstruction of LocalSparkContext, so this method
   * was created temporarily. When the migration work is completed, this method will be
   * renamed to `sc` and the variable `sc` will be deleted.
   */
  def sparkContext: SparkContext = {
    if (_sc == null) {
      _sc = new SparkContext(_conf)
    }
    _sc
  }

  def setConf(pairs: (String, String)*): Unit = {
    if (_sc != null) {
      logWarning("Because SparkContext already initialized, " +
        "since configurations won't take effect in this case.")
    }
    pairs.foreach { case (k, v) => _conf.set(k, v) }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
  }

  override def afterEach(): Unit = {
    try {
      resetSparkContext()
    } finally {
      super.afterEach()
    }
  }

  def resetSparkContext(): Unit = {
    LocalSparkContext.stop(sc)
    LocalSparkContext.stop(_sc)
    ResourceProfile.clearDefaultProfile()
    _sc = null
    _conf = new SparkConf()
  }

}

object LocalSparkContext {
  def stop(sc: SparkContext): Unit = {
    if (sc != null) {
      sc.stop()
    }
    // To avoid RPC rebinding to the same port, since it doesn't unbind immediately on shutdown
    System.clearProperty("spark.driver.port")
  }

  /** Runs `f` by passing in `sc` and ensures that `sc` is stopped. */
  def withSpark[T](sc: SparkContext)(f: SparkContext => T): T = {
    try {
      f(sc)
    } finally {
      stop(sc)
    }
  }

}
