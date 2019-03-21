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

package org.apache.spark.sql.catalyst.plans.logical

import org.apache.spark.sql.catalyst.expressions.Attribute

/**
 * A general hint for the child that is not yet resolved. This node is generated by the parser and
 * should be removed This node will be eliminated post analysis.
 * @param name the name of the hint
 * @param parameters the parameters of the hint
 * @param child the [[LogicalPlan]] on which this hint applies
 */
case class UnresolvedHint(name: String, parameters: Seq[Any], child: LogicalPlan)
  extends UnaryNode {

  override lazy val resolved: Boolean = false
  override def output: Seq[Attribute] = child.output
}

/**
 * A resolved hint node. The analyzer should convert all [[UnresolvedHint]] into [[ResolvedHint]].
 * This node will be eliminated before optimization starts.
 */
case class ResolvedHint(child: LogicalPlan, hints: HintInfo = HintInfo())
  extends UnaryNode {

  override def output: Seq[Attribute] = child.output

  override def doCanonicalize(): LogicalPlan = child.canonicalized
}

/**
 * Hint that is associated with a [[Join]] node, with [[HintInfo]] on its left child and on its
 * right child respectively.
 */
case class JoinHint(leftHint: Option[HintInfo], rightHint: Option[HintInfo]) {

  override def toString: String = {
    Seq(
      leftHint.map("leftHint=" + _),
      rightHint.map("rightHint=" + _))
      .filter(_.isDefined).map(_.get).mkString(", ")
  }
}

object JoinHint {
  val NONE = JoinHint(None, None)
}

/**
 * The hint attributes to be applied on a specific node.
 *
 * @param strategy The preferred join strategy.
 */
case class HintInfo(strategy: Option[JoinStrategyHint] = None) {

  /**
   * Combine two [[HintInfo]]s into one [[HintInfo]], in which the new strategy will the strategy
   * in this [[HintInfo]] if defined, otherwise the strategy in the other [[HintInfo]].
   */
  def merge(other: HintInfo): HintInfo = {
    HintInfo(strategy = this.strategy.orElse(other.strategy))
  }

  override def toString: String = {
    val hints = scala.collection.mutable.ArrayBuffer.empty[String]
    if (strategy.isDefined) {
      hints += s"strategy=${strategy.get}"
    }

    if (hints.isEmpty) "none" else hints.mkString("(", ", ", ")")
  }
}

sealed abstract class JoinStrategyHint {

  def displayName: String
  def hintAliases: Set[String]

  override def toString: String = displayName
}

/**
 * The enumeration of join strategy hints.
 *
 * The hinted strategy will be used for the join with which it is associated if doable. In case
 * of contradicting strategy hints specified for each side of the join, hints are prioritized as
 * BROADCAST over SHUFFLE_MERGE over SHUFFLE_HASH over SHUFFLE_REPLICATE_NL.
 */
object JoinStrategyHint {

  val strategies: Set[JoinStrategyHint] = Set(
    BROADCAST,
    SHUFFLE_MERGE,
    SHUFFLE_HASH,
    SHUFFLE_REPLICATE_NL)
}

case object BROADCAST extends JoinStrategyHint {
  override def displayName: String = "broadcast-hash"
  override def hintAliases: Set[String] = Set(
    "BROADCAST",
    "BROADCASTJOIN",
    "MAPJOIN")
}

case object SHUFFLE_MERGE extends JoinStrategyHint {
  override def displayName: String = "shuffle-merge"
  override def hintAliases: Set[String] = Set(
    "SHUFFLE_MERGE",
    "MERGE",
    "MERGEJOIN")
}

case object SHUFFLE_HASH extends JoinStrategyHint {
  override def displayName: String = "shuffle-hash"
  override def hintAliases: Set[String] = Set(
    "SHUFFLE_HASH")
}

case object SHUFFLE_REPLICATE_NL extends JoinStrategyHint {
  override def displayName: String = "shuffle-replicate-nested-loop"
  override def hintAliases: Set[String] = Set(
    "SHUFFLE_REPLICATE_NL")
}
