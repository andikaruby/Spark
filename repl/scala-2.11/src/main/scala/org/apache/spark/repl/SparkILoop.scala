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

package org.apache.spark.repl

import java.io.BufferedReader

// scalastyle:off println
import scala.Predef.{println => _, _}
// scalastyle:on println
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{ILoop, JPrintWriter}
import scala.tools.nsc.util.stringFromStream
import scala.util.Properties.{javaVersion, javaVmName, versionString}

/**
 *  A Spark-specific interactive shell.
 */
class SparkILoop(in0: Option[BufferedReader], out: JPrintWriter)
    extends ILoop(in0, out) {
  def this(in0: BufferedReader, out: JPrintWriter) = this(Some(in0), out)
  def this() = this(None, new JPrintWriter(Console.out, true))

  override def createInterpreter(): Unit = {
    intp = new SparkILoopInterpreter(settings, out, initializeSpark)
  }

  val initializationCommands: Seq[String] = Seq(
    """
    @transient val spark = if (org.apache.spark.repl.Main.sparkSession != null) {
        org.apache.spark.repl.Main.sparkSession
      } else {
        org.apache.spark.repl.Main.createSparkSession()
      }
    @transient val sc = {
      val _sc = spark.sparkContext
      if (_sc.getConf.getBoolean("spark.ui.reverseProxy", false)) {
        val proxyUrl = _sc.getConf.get("spark.ui.reverseProxyUrl", null)
        if (proxyUrl != null) {
          println(
            s"Spark Context Web UI is available at ${proxyUrl}/proxy/${_sc.applicationId}")
        } else {
          println(s"Spark Context Web UI is available at Spark Master Public URL")
        }
      } else {
        _sc.uiWebUrl.foreach {
          webUrl => println(s"Spark context Web UI available at ${webUrl}")
        }
      }
      println("Spark context available as 'sc' " +
        s"(master = ${_sc.master}, app id = ${_sc.applicationId}).")
      println("Spark session available as 'spark'.")
      _sc
    }
    """,
    "import org.apache.spark.SparkContext._",
    "import spark.implicits._",
    "import spark.sql",
    "import org.apache.spark.sql.functions._"
  )

  def initializeSpark(): Unit = {
    if (!intp.reporter.hasErrors) {
      // `savingReplayStack` removes the commands from session history.
      savingReplayStack {
        initializationCommands.foreach(intp quietRun _)
      }
    } else {
      throw new RuntimeException(s"Scala $versionString interpreter encountered " +
        "errors during initialization")
    }
  }

  /** Print a welcome message */
  override def printWelcome() {
    // Before Scala 2.11.9, `printWelcome()` will be the last thing to be called,
    // so Scala REPL and Spark will be initialized before `printWelcome()`.
    // After Scala 2.11.9, `printWelcome()` will be the first thing to be called,
    // as a result, Scala REPL and Spark will be initialized in `printWelcome()`.
    if (!isInitializeComplete) {
      intp.initializeSynchronous()
    }

    import org.apache.spark.SPARK_VERSION
    echo("""Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version %s
      /_/
         """.format(SPARK_VERSION))
    val welcomeMsg = "Using Scala %s (%s, Java %s)".format(
      versionString, javaVmName, javaVersion)
    echo(welcomeMsg)
    echo("Type in expressions to have them evaluated.")
    echo("Type :help for more information.")
  }

  /** Available commands */
  override def commands: List[LoopCommand] = standardCommands

  override def resetCommand(line: String): Unit = {
    super.resetCommand(line)
    initializeSpark()
    echo("Note that after :reset, state of SparkSession and SparkContext is unchanged.")
  }

  override def replay(): Unit = {
    initializeSpark()
    super.replay()
  }

}

object SparkILoop {

  /**
   * Creates an interpreter loop with default settings and feeds
   * the given code to it as input.
   */
  def run(code: String, sets: Settings = new Settings): String = {
    import java.io.{ BufferedReader, StringReader, OutputStreamWriter }

    stringFromStream { ostream =>
      Console.withOut(ostream) {
        val input = new BufferedReader(new StringReader(code))
        val output = new JPrintWriter(new OutputStreamWriter(ostream), true)
        val repl = new SparkILoop(input, output)

        if (sets.classpath.isDefault) {
          sets.classpath.value = sys.props("java.class.path")
        }
        repl process sets
      }
    }
  }
  def run(lines: List[String]): String = run(lines.map(_ + "\n").mkString)
}
