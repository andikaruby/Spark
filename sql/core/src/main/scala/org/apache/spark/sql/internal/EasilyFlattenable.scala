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

package org.apache.spark.sql.internal

import org.apache.spark.sql.catalyst.analysis.{UnresolvedAttribute, UnresolvedFunction}
import org.apache.spark.sql.catalyst.expressions.{Alias, AttributeReference, AttributeSet, NamedExpression, UserDefinedExpression}
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, Project}

private[sql] object EasilyFlattenable {
  def unapply(tuple: (LogicalPlan, Seq[NamedExpression])): Option[LogicalPlan] = {
    val (logicalPlan, newProjList) = tuple
    logicalPlan match {
      case p@Project(projList, child) =>

        // In the new column list identify those Named Expressions which are just attributes and
        // hence pass thru
        val (passThruAttribs, tinkeredOrNewNamedExprs) = newProjList.partition {
          case _: AttributeReference => true
          case _ => false
        }
        val currentOutputAttribs = AttributeSet(p.output)

        if (passThruAttribs.size == currentOutputAttribs.size && passThruAttribs.forall(
          currentOutputAttribs.contains) && tinkeredOrNewNamedExprs.nonEmpty) {

          val attribsReassignedInProj = AttributeSet(projList.filter(ne => ne match {
            case _: AttributeReference => false
            case _ => true
          }).map(_.toAttribute)).intersect(AttributeSet(child.output))

          if (tinkeredOrNewNamedExprs.exists(ne => ne.references.exists {
            case attr: AttributeReference => attribsReassignedInProj.contains(attr)
            case _: UnresolvedAttribute => true
          } || ne.collectFirst{
            case u: UnresolvedFunction => u
            case ex if !ex.deterministic => ex
            case ex if ex.isInstanceOf[UserDefinedExpression] => ex
            case u: UnresolvedAttribute => u
          }.nonEmpty)) {
            None
          } else {
            val remappedNewProjList = newProjList.map {
              case attr: AttributeReference => projList.find(
                _.toAttribute.canonicalized == attr.canonicalized).getOrElse(attr)
              case anyOtherExpr =>
                (anyOtherExpr transformUp {
                  case attr: AttributeReference => projList.find(
                    _.toAttribute.canonicalized == attr.canonicalized).map {
                    case al: Alias => al.child
                    case x => x
                  }.getOrElse(attr)
                }).asInstanceOf[NamedExpression]
            }
            Option(p.copy(projectList = remappedNewProjList))
          }
        } else {
          // for now None
          None
        }

      case _ => None
    }
  }
}
