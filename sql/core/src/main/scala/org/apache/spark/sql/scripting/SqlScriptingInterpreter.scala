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

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.catalyst.analysis.UnresolvedIdentifier
import org.apache.spark.sql.catalyst.plans.logical.{CompoundBody, CompoundPlanStatement, CreateVariable, DropVariable, IfElseStatement, LogicalPlan, SingleStatement, WhileStatement}
import org.apache.spark.sql.catalyst.trees.Origin

/**
 * SQL scripting interpreter - builds SQL script execution plan.
 *
 * @param session
 *   Spark session that SQL script is executed within.
 */
case class SqlScriptingInterpreter(session: SparkSession) {

  /**
   * Execute the provided CompoundBody and return the result.
   *
   * @param compoundBody
   *   CompoundBody to execute.
   * @return Execution result of the last statement in the given [[CompoundBody]].
   *         It is returned as a sequence of rows.
   */
  def execute(compoundBody: CompoundBody): Seq[Row] = {
    val resultsIter = executeInternal(compoundBody)
    resultsIter.foldLeft(Array.empty[Row])((_, next) => next).toSeq
  }

  /**
   * Build execution plan and return statements that need to be executed,
   *   wrapped in the execution node.
   *
   * @param compound
   *   CompoundBody for which to build the plan.
   * @return
   *   Iterator through collection of statements to be executed.
   */
  private def buildExecutionPlan(compound: CompoundBody): Iterator[CompoundStatementExec] = {
    transformTreeIntoExecutable(compound).asInstanceOf[CompoundBodyExec].getTreeIterator
  }

  /**
   * Fetch the name of the Create Variable plan.
   * @param plan
   *   Plan to fetch the name from.
   * @return
   *   Name of the variable.
   */
  private def getDeclareVarNameFromPlan(plan: LogicalPlan): Option[UnresolvedIdentifier] =
    plan match {
      case CreateVariable(name: UnresolvedIdentifier, _, _) => Some(name)
      case _ => None
    }

  /**
   * Transform the parsed tree to the executable node.
   *
   * @param node
   *   Root node of the parsed tree.
   * @return
   *   Executable statement.
   */
  private def transformTreeIntoExecutable(node: CompoundPlanStatement): CompoundStatementExec =
    node match {
      case body: CompoundBody =>
        // TODO [SPARK-48530]: Current logic doesn't support scoped variables and shadowing.
        val variables = body.collection.flatMap {
          case st: SingleStatement => getDeclareVarNameFromPlan(st.parsedPlan)
          case _ => None
        }
        val dropVariables = variables
          .map(varName => DropVariable(varName, ifExists = true))
          .map(new SingleStatementExec(_, Origin(), isInternal = true))
          .reverse
        new CompoundBodyExec(
          body.collection.map(st => transformTreeIntoExecutable(st)) ++ dropVariables,
          session)
      case IfElseStatement(conditions, conditionalBodies, elseBody) =>
        val conditionsExec = conditions.map(condition =>
          new SingleStatementExec(condition.parsedPlan, condition.origin))
        val conditionalBodiesExec = conditionalBodies.map(body =>
          transformTreeIntoExecutable(body).asInstanceOf[CompoundBodyExec])
        val unconditionalBodiesExec = elseBody.map(body =>
          transformTreeIntoExecutable(body).asInstanceOf[CompoundBodyExec])
        new IfElseStatementExec(
          conditionsExec, conditionalBodiesExec, unconditionalBodiesExec, session)
      case WhileStatement(condition, body, _) =>
        val conditionExec =
          new SingleStatementExec(condition.parsedPlan, condition.origin, isInternal = false)
        val bodyExec =
          transformTreeIntoExecutable(body).asInstanceOf[CompoundBodyExec]
        new WhileStatementExec(conditionExec, bodyExec, session)
      case sparkStatement: SingleStatement =>
        new SingleStatementExec(
          sparkStatement.parsedPlan,
          sparkStatement.origin,
          shouldCollectResult = true)
    }

  /**
   * Execute the provided CompoundBody and return all results.
   *
   * @param compoundBody
   *   CompoundBody to execute.
   * @return Results from all leaf statements.
   */
  private[scripting] def executeInternal(compoundBody: CompoundBody): Iterator[Array[Row]] = {
    val execNodesIter = buildExecutionPlan(compoundBody)
    execNodesIter.flatMap {
      case statement: SingleStatementExec if statement.shouldCollectResult => statement.result
      case _ => None
    }
  }
}
