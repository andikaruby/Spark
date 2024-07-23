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

package org.apache.spark.sql.scripting

import scala.collection.mutable

import org.apache.spark.{SparkException, SparkThrowable}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.apache.spark.sql.catalyst.parser.HandlerType.HandlerType
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.trees.{Origin, WithOrigin}

/**
 * Trait for all SQL scripting execution nodes used during interpretation phase.
 */
sealed trait CompoundStatementExec extends Logging {

  /**
   * Whether the statement originates from the SQL script or is created during the interpretation.
   * Example: DropVariable statements are automatically created at the end of each compound.
   */
  val isInternal: Boolean = false

  /**
   * Reset execution of the current node.
   */
  def reset(): Unit
}

/**
 * Leaf node in the execution tree.
 */
trait LeafStatementExec extends CompoundStatementExec {

  /**
  * Execute the statement.
  * @param session Spark session.
  */
  def execute(session: SparkSession): Unit

  /**
   * Whether an error was raised during the execution of this statement.
   */
  var raisedError: Boolean = false

  /**
   * Error state of the statement.
   */
  var errorState: Option[String] = None

  /**
   * Error raised during statement execution.
   */
  var error: Option[SparkThrowable] = None

  var rethrow: Option[Throwable] = None
}

/**
 * Non-leaf node in the execution tree. It is an iterator over executable child nodes.
 */
trait NonLeafStatementExec extends CompoundStatementExec {

  /**
   * Construct the iterator to traverse the tree rooted at this node in an in-order traversal.
   * @return
   *   Tree iterator.
   */
  def getTreeIterator: Iterator[CompoundStatementExec]
}

/**
 * Executable node for SingleStatement.
 * @param parsedPlan
 *   Logical plan of the parsed statement.
 * @param origin
 *   Origin descriptor for the statement.
 * @param isInternal
 *   Whether the statement originates from the SQL script or it is created during the
 *   interpretation. Example: DropVariable statements are automatically created at the end of each
 *   compound.
 * @param shouldCollectResult
 *   Whether we should collect result after statement execution. Example: results from conditions
 *   in if-else or loops should not be collected.
 */
class SingleStatementExec(
    var parsedPlan: LogicalPlan,
    override val origin: Origin,
    override val isInternal: Boolean = false,
    val shouldCollectResult: Boolean = false)
  extends LeafStatementExec with WithOrigin {

  /**
   * Data returned after execution.
   */
  var result: Option[Array[Row]] = None

  /**
   * Get the SQL query text corresponding to this statement.
   * @return
   *   SQL query text.
   */
  def getText: String = {
    assert(origin.sqlText.isDefined && origin.startIndex.isDefined && origin.stopIndex.isDefined)
    origin.sqlText.get.substring(origin.startIndex.get, origin.stopIndex.get + 1)
  }

  override def reset(): Unit = {
    raisedError = false
    errorState = None
    error = None
    result = None // Should we do this?
  }

  def execute(session: SparkSession): Unit = {
    try {
      val rows = Some(Dataset.ofRows(session, parsedPlan).collect())
      if (shouldCollectResult) {
        result = rows
      }
    } catch {
      case e: SparkThrowable =>
        // TODO: check handlers for error conditions
        raisedError = true
        errorState = Some(e.getSqlState)
        error = Some(e)
        e match {
          case throwable: Throwable =>
            rethrow = Some(throwable)
          case _ =>
        }
      case thr: Throwable =>
        raisedError = true
        errorState = Some("UNKNOWN")
        rethrow = Some(thr)
    }
  }
}

/**
 * Executable node for CompoundBody.
 * @param statements
 *   Executable nodes for nested statements within the CompoundBody.
 * @param session
 *   Spark session.
 */
class CompoundBodyExec(
      label: Option[String],
      statements: Seq[CompoundStatementExec],
      conditionHandlerMap: mutable.HashMap[String, ErrorHandlerExec] = mutable.HashMap(),
      session: SparkSession)
  extends NonLeafStatementExec {

  private def getHandler(condition: String): Option[ErrorHandlerExec] = {
    var ret = conditionHandlerMap.get(condition)
    if (ret.isEmpty) {
      ret = conditionHandlerMap.get("UNKNOWN")
    }
    ret
  }

  private def handleError(statement: LeafStatementExec): CompoundStatementExec = {
    if (statement.raisedError) {
      getHandler(statement.errorState.get).foreach { handler =>
        statement.reset() // Clear all flags and result
        handler.reset()
        return handler.getTreeIterator.next()
      }
    }
    statement
  }

  private var localIterator: Iterator[CompoundStatementExec] = statements.iterator
  private var curr: Option[CompoundStatementExec] =
    if (localIterator.hasNext) Some(localIterator.next()) else None

  def getTreeIterator: Iterator[CompoundStatementExec] = treeIterator

  override def reset(): Unit = {
    statements.foreach(_.reset())
    localIterator = statements.iterator
    curr = if (localIterator.hasNext) Some(localIterator.next()) else None
  }

  private lazy val treeIterator: Iterator[CompoundStatementExec] =
    new Iterator[CompoundStatementExec] {
      override def hasNext: Boolean = {
        val childHasNext = curr match {
          case Some(body: NonLeafStatementExec) => body.getTreeIterator.hasNext
          case Some(_: LeafStatementExec) => true
          case None => false
          case _ => throw SparkException.internalError(
            "Unknown statement type encountered during SQL script interpretation.")
        }
        localIterator.hasNext || childHasNext
      }

      @scala.annotation.tailrec
      override def next(): CompoundStatementExec = {
        curr match {
          case None => throw SparkException.internalError(
            "No more elements to iterate through in the current SQL compound statement.")
          case Some(leave: LeaveStatementExec) =>
            if (leave.used) {
              curr = None
              if (label.getOrElse("").equals(leave.getLabel)) {
                leave.execute(session)
              }
            }
            leave
          case Some(statement: LeafStatementExec) =>
            curr = if (localIterator.hasNext) Some(localIterator.next()) else None
            statement.execute(session)  // Execute the leaf statement
            handleError(statement)
          case Some(body: NonLeafStatementExec) =>
            if (body.getTreeIterator.hasNext) {
              val statement = body.getTreeIterator.next()
              handleError(statement.asInstanceOf[LeafStatementExec])
            } else {
              curr = if (localIterator.hasNext) Some(localIterator.next()) else None
              next()
            }
          case _ => throw SparkException.internalError(
            "Unknown statement type encountered during SQL script interpretation.")
        }
      }
    }
}

class ErrorHandlerExec(
      body: CompoundBodyExec,
      handlerType: HandlerType) extends NonLeafStatementExec {

  override def getTreeIterator: Iterator[CompoundStatementExec] = body.getTreeIterator

  def getHandlerType: HandlerType = handlerType

  override def reset(): Unit = body.reset()
}

class LeaveStatementExec(val label: String) extends LeafStatementExec {

  var used: Boolean = false

  def getLabel: String = label

  override def execute(session: SparkSession): Unit = used = true

  override def reset(): Unit = used = false
}
