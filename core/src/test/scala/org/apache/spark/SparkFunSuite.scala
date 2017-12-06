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

// scalastyle:off
import java.io.File

import org.apache.spark.internal.Logging
import org.apache.spark.util.AccumulatorContext
import org.scalatest.{BeforeAndAfterAll, FunSuite, Outcome}

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Base abstract class for all unit tests in Spark for handling common functionality.
 */
abstract class SparkFunSuite
  extends FunSuite
  with BeforeAndAfterAll
  with Logging {
// scalastyle:on

  val threadWhiteList = Set(
    /**
      * Netty related threads.
      */
    "netty.*",

    /**
      * A Single-thread singleton EventExecutor inside netty which creates such threads.
      */
    "globalEventExecutor.*",

    /**
      * Netty creates such threads.
      * Checks if a thread is alive periodically and runs a task when a thread dies.
      */
    "threadDeathWatcher.*"
  )
  var beforeAllTestThreadNames: Set[String] = Set.empty

  protected override def beforeAll(): Unit = {
    beforeAllTestThreadNames = runningThreadNames()
    super.beforeAll()
  }

  protected override def afterAll(): Unit = {
    try {
      // Avoid leaking map entries in tests that use accumulators without SparkContext
      AccumulatorContext.clear()
    } finally {
      super.afterAll()
      printRemainingThreadNames()
    }
  }

  // helper function
  protected final def getTestResourceFile(file: String): File = {
    new File(getClass.getClassLoader.getResource(file).getFile)
  }

  protected final def getTestResourcePath(file: String): String = {
    getTestResourceFile(file).getCanonicalPath
  }

  private def runningThreadNames(): Set[String] = {
    Thread.getAllStackTraces.keySet().map(_.getName).toSet
  }

  private def printRemainingThreadNames(): Unit = {
    val remainingThreadNames = runningThreadNames.diff(beforeAllTestThreadNames)
      .filterNot { s => threadWhiteList.exists(s.matches(_)) }
    if (!remainingThreadNames.isEmpty) {
      val suiteName = this.getClass.getName
      val shortSuiteName = suiteName.replaceAll("org.apache.spark", "o.a.s")
      logWarning(s"\n\n===== POSSIBLE THREAD LEAK IN SUITE $shortSuiteName, " +
        s"thread names: ${remainingThreadNames.mkString(", ")} =====\n")
    }
  }

  /**
   * Log the suite name and the test name before and after each test.
   *
   * Subclasses should never override this method. If they wish to run
   * custom code before and after each test, they should mix in the
   * {{org.scalatest.BeforeAndAfter}} trait instead.
   */
  final protected override def withFixture(test: NoArgTest): Outcome = {
    val testName = test.text
    val suiteName = this.getClass.getName
    val shortSuiteName = suiteName.replaceAll("org.apache.spark", "o.a.s")
    try {
      logInfo(s"\n\n===== TEST OUTPUT FOR $shortSuiteName: '$testName' =====\n")
      test()
    } finally {
      logInfo(s"\n\n===== FINISHED $shortSuiteName: '$testName' =====\n")
    }
  }

}
