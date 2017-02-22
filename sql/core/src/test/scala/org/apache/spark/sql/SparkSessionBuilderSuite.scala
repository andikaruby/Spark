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

package org.apache.spark.sql

import org.apache.spark.{SparkConf, SparkContext, SparkFunSuite}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.test.SharedSQLContext

/**
 * Test cases for the builder pattern of [[SparkSession]].
 */
class SparkSessionBuilderSuite extends SparkFunSuite with SharedSQLContext {

  private var initialSession: SparkSession = _

  override lazy val sparkContext: SparkContext = {
    initialSession = SparkSession.builder()
      .master("local")
      .config("spark.ui.enabled", value = false)
      .config("some-config", "v2")
      .getOrCreate()
    initialSession.sparkContext
  }

  test("create with config options and propagate them to SparkContext and SparkSession") {
    // Creating a new session with config - this works by just calling the lazy val
    sparkContext
    assert(initialSession.sparkContext.conf.get("some-config") == "v2")
    assert(initialSession.conf.get("some-config") == "v2")
    SparkSession.clearDefaultSession()
  }

  test("use global default session") {
    val session = SparkSession.builder().getOrCreate()
    assert(SparkSession.builder().getOrCreate() == session)
    SparkSession.clearDefaultSession()
  }

  test("config options are propagated to existing SparkSession") {
    val session1 = SparkSession.builder().config("spark-config1", "a").getOrCreate()
    assert(session1.conf.get("spark-config1") == "a")
    val session2 = SparkSession.builder().config("spark-config1", "b").getOrCreate()
    assert(session1 == session2)
    assert(session1.conf.get("spark-config1") == "b")
    SparkSession.clearDefaultSession()
  }

  test("use session from active thread session and propagate config options") {
    val defaultSession = SparkSession.builder().getOrCreate()
    val activeSession = defaultSession.newSession()
    SparkSession.setActiveSession(activeSession)
    val session = SparkSession.builder().config("spark-config2", "a").getOrCreate()

    assert(activeSession != defaultSession)
    assert(session == activeSession)
    assert(session.conf.get("spark-config2") == "a")
    SparkSession.clearActiveSession()

    assert(SparkSession.builder().getOrCreate() == defaultSession)
    SparkSession.clearDefaultSession()
  }

  test("create a new session if the default session has been stopped") {
    val defaultSession = SparkSession.builder().getOrCreate()
    SparkSession.setDefaultSession(defaultSession)
    defaultSession.stop()
    val newSession = SparkSession.builder().master("local").getOrCreate()
    assert(newSession != defaultSession)
    newSession.stop()
  }

  test("create a new session if the active thread session has been stopped") {
    val activeSession = SparkSession.builder().master("local").getOrCreate()
    SparkSession.setActiveSession(activeSession)
    activeSession.stop()
    val newSession = SparkSession.builder().master("local").getOrCreate()
    assert(newSession != activeSession)
    newSession.stop()
  }

  test("create SparkContext first then SparkSession") {
    sparkContext.stop()
    val conf = new SparkConf().setAppName("test").setMaster("local").set("key1", "value1")
    val sparkContext2 = new SparkContext(conf)
    val session = SparkSession.builder().config("key2", "value2").getOrCreate()
    assert(session.conf.get("key1") == "value1")
    assert(session.conf.get("key2") == "value2")
    assert(session.sparkContext.conf.get("key1") == "value1")
    assert(session.sparkContext.conf.get("key2") == "value2")
    assert(session.sparkContext.conf.get("spark.app.name") == "test")
    session.stop()
  }

  test("SPARK-15887: hive-site.xml should be loaded") {
    val session = SparkSession.builder().master("local").getOrCreate()
    assert(session.sessionState.newHadoopConf().get("hive.in.test") == "true")
    assert(session.sparkContext.hadoopConfiguration.get("hive.in.test") == "true")
    session.stop()
  }

  test("SPARK-15991: Set global Hadoop conf") {
    val session = SparkSession.builder().master("local").getOrCreate()
    val mySpecialKey = "my.special.key.15991"
    val mySpecialValue = "msv"
    try {
      session.sparkContext.hadoopConfiguration.set(mySpecialKey, mySpecialValue)
      assert(session.sessionState.newHadoopConf().get(mySpecialKey) == mySpecialValue)
    } finally {
      session.sparkContext.hadoopConfiguration.unset(mySpecialKey)
      session.stop()
    }
  }

  test("fork new session and inherit a copy of the session state") {
    val activeSession = SparkSession.builder().master("local").getOrCreate()
    val forkedSession = activeSession.cloneSession()

    assert(forkedSession ne activeSession)
    assert(forkedSession.sessionState ne activeSession.sessionState)
    assert(forkedSession.conf ne activeSession.conf)
    // the rest of copying is tested individually for each field

    forkedSession.stop()
  }

  test("fork new session and inherit RuntimeConfig options") {
    val activeSession = SparkSession
      .builder()
      .master("local")
      .getOrCreate()

    val key = "spark-config-clone"
    activeSession.conf.set(key, "active")

    // inheritance
    val forkedSession = activeSession.cloneSession()
    assert(forkedSession ne activeSession)
    assert(forkedSession.conf ne activeSession.conf)
    assert(forkedSession.conf.get(key) == "active")

    // independence
    forkedSession.conf.set(key, "forked")
    assert(activeSession.conf.get(key) == "active")
    activeSession.conf.set(key, "dontcopyme")
    assert(forkedSession.conf.get(key) == "forked")

    forkedSession.stop()
  }

  test("fork new session and inherit function registry and udf") {
    val activeSession = SparkSession.builder().master("local").getOrCreate()
    activeSession.udf.register("strlenScala", (_: String).length + (_: Int))
    val forkedSession = activeSession.cloneSession()

    // inheritance
    assert(forkedSession ne activeSession)
    assert(forkedSession.sessionState.functionRegistry ne
      activeSession.sessionState.functionRegistry)
    assert(forkedSession.sessionState.functionRegistry.lookupFunction("strlenScala").nonEmpty)

    // independence
    forkedSession.sessionState.functionRegistry.dropFunction("strlenScala")
    assert(activeSession.sessionState.functionRegistry.lookupFunction("strlenScala").nonEmpty)
    activeSession.udf.register("addone", (_: Int) + 1)
    assert(forkedSession.sessionState.functionRegistry.lookupFunction("addone").isEmpty)

    forkedSession.stop()
  }

  test("fork new session and inherit experimental methods") {
    object DummyRule1 extends Rule[LogicalPlan] {
      def apply(p: LogicalPlan): LogicalPlan = p
    }
    object DummyRule2 extends Rule[LogicalPlan] {
      def apply(p: LogicalPlan): LogicalPlan = p
    }
    val optimizations = List(DummyRule1, DummyRule2)

    val activeSession = SparkSession.builder().master("local").getOrCreate()
    activeSession.experimental.extraOptimizations = optimizations

    val forkedSession = activeSession.cloneSession()

    // inheritance
    assert(forkedSession ne activeSession)
    assert(forkedSession.experimental.extraOptimizations.toSet ==
      activeSession.experimental.extraOptimizations.toSet)

    // independence
    forkedSession.experimental.extraOptimizations = List(DummyRule2)
    assert(activeSession.experimental.extraOptimizations == optimizations)
    activeSession.experimental.extraOptimizations = List(DummyRule1)
    assert(forkedSession.experimental.extraOptimizations == List(DummyRule2))

    forkedSession.stop()
  }

  test("fork new session and run query on inherited table") {
    def checkTableExists(sparkSession: SparkSession): Unit = {
      QueryTest.checkAnswer(sparkSession.sql(
        """
          |SELECT x.str, COUNT(*)
          |FROM df x JOIN df y ON x.str = y.str
          |GROUP BY x.str
        """.stripMargin),
        Row("1", 1) :: Row("2", 1) :: Row("3", 1) :: Nil)
    }

    val activeSession = SparkSession.builder().master("local").getOrCreate()
    SparkSession.setActiveSession(activeSession)
    import activeSession.implicits._

    Seq(1, 2, 3).map(i => (i, i.toString)).toDF("int", "str").createOrReplaceTempView("df")
    checkTableExists(activeSession)

    val forkedSession = activeSession.cloneSession()
    SparkSession.setActiveSession(forkedSession)
    checkTableExists(forkedSession)

    SparkSession.clearActiveSession()
    forkedSession.stop()
  }
}
