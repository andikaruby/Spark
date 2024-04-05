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
package org.apache.spark.sql.catalyst.trees

import scala.collection.mutable
import scala.collection.mutable.Queue

import org.apache.spark.QueryContext
import org.apache.spark.util.ArrayImplicits._

/**
 * Contexts of TreeNodes, including location, SQL text, object type and object name.
 * The only supported object type is "VIEW" now. In the future, we may support SQL UDF or other
 * objects which contain SQL text.
 */
case class Origin(
    line: Option[Int] = None,
    startPosition: Option[Int] = None,
    startIndex: Option[Int] = None,
    stopIndex: Option[Int] = None,
    sqlText: Option[String] = None,
    objectType: Option[String] = None,
    objectName: Option[String] = None,
    stackTrace: Option[Array[StackTraceElement]] = None) {

  lazy val context: QueryContext = if (stackTrace.isDefined) {
    DataFrameQueryContext(stackTrace.get.toImmutableArraySeq)
  } else {
    SQLQueryContext(
      line, startPosition, startIndex, stopIndex, sqlText, objectType, objectName)
  }

  def getQueryContext: Array[QueryContext] = {
    Some(context).filter {
      case s: SQLQueryContext => s.isValid
      case _ => true
    }.toArray
  }
}

/**
 * Helper trait for objects that can be traced back to an [[Origin]].
 */
trait WithOrigin {
  def origin: Origin
}

/**
 * Provides a location for TreeNodes to ask about the context of their origin.  For example, which
 * line of code is currently being parsed.
 */
object CurrentOrigin {
  private val value = new ThreadLocal[Origin]() {
    override def initialValue: Origin = Origin()
  }

  def get: Origin = value.get()
  def set(o: Origin): Unit = value.set(o)

  def reset(): Unit = value.set(Origin())

  def setPosition(line: Int, start: Int): Unit = {
    value.set(
      value.get.copy(line = Some(line), startPosition = Some(start)))
  }

  def withOrigin[A](o: Origin)(f: => A): A = {
    // remember the previous one so it can be reset to this
    // this way withOrigin can be recursive
    val previous = get
    set(o)
    val ret = try f finally { set(previous) }
    ret
  }
}

/**
 * Provides detailed call site information on PySpark.
 * This information is generated in PySpark and stored as Maps within a queue
 * to maintain the order of operations.
 *
 * The queue structure ensures that multiple call sites can be logged in order, allowing
 * for accurate reconstruction of the sequence of operations in multi-threaded or asynchronous
 * execution environments.
 */
object PySparkCurrentOrigin {
  private val pysparkCallSiteQueue = new ThreadLocal[Queue[mutable.Map[String, String]]]() {
    override def initialValue(): Queue[mutable.Map[String, String]] = Queue.empty
  }

  /**
   * Adds a call site information map to the queue.
   *
   * @param fragment The method name within the PySpark code.
   * @param callSite Detailed context, including file name and line number.
   */
  def set(fragment: String, callSite: String): Unit = {
    pysparkCallSiteQueue.get().enqueue(mutable.Map("fragment" -> fragment, "callSite" -> callSite))
  }

  /**
   * Retrieves and removes the earliest call site information map from the queue.
   *
   * @return An Option containing the first Map if available, or None if the queue is empty.
   */
  def pop(): Option[mutable.Map[String, String]] = {
    if (pysparkCallSiteQueue.get().nonEmpty) {
      Some(pysparkCallSiteQueue.get().dequeue()) // Return and remove the first element
    } else {
      None
    }
  }

  // clear() is not needed as dequeue() handles removal
}
