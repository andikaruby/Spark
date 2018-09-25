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

package org.apache.spark.sql.catalyst

import java.io._
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.spark.SparkEnv
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types.{NumericType, StringType}
import org.apache.spark.unsafe.types.UTF8String
import org.apache.spark.util.Utils
import org.apache.spark.util.Utils.logWarning

package object util extends Logging {

  /** Silences output to stderr or stdout for the duration of f */
  def quietly[A](f: => A): A = {
    val origErr = System.err
    val origOut = System.out
    try {
      System.setErr(new PrintStream(new OutputStream {
        def write(b: Int) = {}
      }))
      System.setOut(new PrintStream(new OutputStream {
        def write(b: Int) = {}
      }))

      f
    } finally {
      System.setErr(origErr)
      System.setOut(origOut)
    }
  }

  def fileToString(file: File, encoding: String = "UTF-8"): String = {
    val inStream = new FileInputStream(file)
    val outStream = new ByteArrayOutputStream
    try {
      var reading = true
      while ( reading ) {
        inStream.read() match {
          case -1 => reading = false
          case c => outStream.write(c)
        }
      }
      outStream.flush()
    }
    finally {
      inStream.close()
    }
    new String(outStream.toByteArray, encoding)
  }

  def resourceToBytes(
      resource: String,
      classLoader: ClassLoader = Utils.getSparkClassLoader): Array[Byte] = {
    val inStream = classLoader.getResourceAsStream(resource)
    val outStream = new ByteArrayOutputStream
    try {
      var reading = true
      while ( reading ) {
        inStream.read() match {
          case -1 => reading = false
          case c => outStream.write(c)
        }
      }
      outStream.flush()
    }
    finally {
      inStream.close()
    }
    outStream.toByteArray
  }

  def resourceToString(
      resource: String,
      encoding: String = "UTF-8",
      classLoader: ClassLoader = Utils.getSparkClassLoader): String = {
    new String(resourceToBytes(resource, classLoader), encoding)
  }

  def stringToFile(file: File, str: String): File = {
    val out = new PrintWriter(file)
    out.write(str)
    out.close()
    file
  }

  def sideBySide(left: String, right: String): Seq[String] = {
    sideBySide(left.split("\n"), right.split("\n"))
  }

  def sideBySide(left: Seq[String], right: Seq[String]): Seq[String] = {
    val maxLeftSize = left.map(_.length).max
    val leftPadded = left ++ Seq.fill(math.max(right.size - left.size, 0))("")
    val rightPadded = right ++ Seq.fill(math.max(left.size - right.size, 0))("")

    leftPadded.zip(rightPadded).map {
      case (l, r) => (if (l == r) " " else "!") + l + (" " * ((maxLeftSize - l.length) + 3)) + r
    }
  }

  def stackTraceToString(t: Throwable): String = {
    val out = new java.io.ByteArrayOutputStream
    val writer = new PrintWriter(out)
    t.printStackTrace(writer)
    writer.flush()
    new String(out.toByteArray, StandardCharsets.UTF_8)
  }

  def stringOrNull(a: AnyRef): String = if (a == null) null else a.toString

  def benchmark[A](f: => A): A = {
    val startTime = System.nanoTime()
    val ret = f
    val endTime = System.nanoTime()
    // scalastyle:off println
    println(s"${(endTime - startTime).toDouble / 1000000}ms")
    // scalastyle:on println
    ret
  }

  // Replaces attributes, string literals, complex type extractors with their pretty form so that
  // generated column names don't contain back-ticks or double-quotes.
  def usePrettyExpression(e: Expression): Expression = e transform {
    case a: Attribute => new PrettyAttribute(a)
    case Literal(s: UTF8String, StringType) => PrettyAttribute(s.toString, StringType)
    case Literal(v, t: NumericType) if v != null => PrettyAttribute(v.toString, t)
    case e: GetStructField =>
      val name = e.name.getOrElse(e.childSchema(e.ordinal).name)
      PrettyAttribute(usePrettyExpression(e.child).sql + "." + name, e.dataType)
    case e: GetArrayStructFields =>
      PrettyAttribute(usePrettyExpression(e.child) + "." + e.field.name, e.dataType)
  }

  def quoteIdentifier(name: String): String = {
    // Escapes back-ticks within the identifier name with double-back-ticks, and then quote the
    // identifier with back-ticks.
    "`" + name.replace("`", "``") + "`"
  }

  def toPrettySQL(e: Expression): String = usePrettyExpression(e).sql


  def escapeSingleQuotedString(str: String): String = {
    val builder = StringBuilder.newBuilder

    str.foreach {
      case '\'' => builder ++= s"\\\'"
      case ch => builder += ch
    }

    builder.toString()
  }

  /**
   * The performance overhead of creating and logging strings for wide schemas can be large. To
   * limit the impact, we bound the number of fields to include by default. This can be overridden
   * by setting the 'spark.debug.maxToStringFields' conf in SparkEnv.
   */
  val DEFAULT_MAX_TO_STRING_FIELDS = 25

  private[spark] def maxNumToStringFields = {
    if (SparkEnv.get != null) {
      SparkEnv.get.conf.getInt("spark.debug.maxToStringFields", DEFAULT_MAX_TO_STRING_FIELDS)
    } else {
      DEFAULT_MAX_TO_STRING_FIELDS
    }
  }

  /** Whether we have warned about plan string truncation yet. */
  private val truncationWarningPrinted = new AtomicBoolean(false)

  /**
   * Format a sequence with semantics similar to calling .mkString(). Any elements beyond
   * maxNumToStringFields will be dropped and replaced by a "... N more fields" placeholder.
   *
   * @return the trimmed and formatted string.
   */
  def truncatedString[T](
      seq: Seq[T],
      start: String,
      sep: String,
      end: String,
      maxNumFields: Int = maxNumToStringFields): String = {
    if (seq.length > maxNumFields) {
      if (truncationWarningPrinted.compareAndSet(false, true)) {
        logWarning(
          "Truncated the string representation of a plan since it was too large. This " +
            "behavior can be adjusted by setting 'spark.debug.maxToStringFields' in SparkEnv.conf.")
      }
      val numFields = math.max(0, maxNumFields - 1)
      seq.take(numFields).mkString(
        start, sep, sep + "... " + (seq.length - numFields) + " more fields" + end)
    } else {
      seq.mkString(start, sep, end)
    }
  }

  /** Shorthand for calling truncatedString() without start or end strings. */
  def truncatedString[T](seq: Seq[T], sep: String): String = truncatedString(seq, "", sep, "")

  /* FIX ME
  implicit class debugLogging(a: Any) {
    def debugLogging() {
      org.apache.log4j.Logger.getLogger(a.getClass.getName).setLevel(org.apache.log4j.Level.DEBUG)
    }
  } */
}
