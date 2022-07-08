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

package org.apache.spark.sql.catalyst.optimizer

import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.dsl.plans._
import org.apache.spark.sql.catalyst.expressions.{Alias, And, Or}
import org.apache.spark.sql.catalyst.expressions.Literal.{FalseLiteral, TrueLiteral}
import org.apache.spark.sql.catalyst.plans.PlanTest
import org.apache.spark.sql.catalyst.plans.logical.{LocalRelation, LogicalPlan}
import org.apache.spark.sql.catalyst.rules.RuleExecutor

class OptimizeRandSuite extends PlanTest {

  object Optimize extends RuleExecutor[LogicalPlan] {
    val batches =
      Batch("ConstantFolding", FixedPoint(10),
        ConstantFolding,
        BooleanSimplification,
        OptimizeRand,
        PruneFilters) :: Nil
  }

  val testRelation = LocalRelation($"a".int, $"b".int, $"c".int)
  val x = testRelation.where($"a".attr.in(1, 3, 5)).subquery("x")
  val literal10d = doubleToLiteral(10d)
  val rand5 = rand(5)

  test("Optimize binary comparison with rand") {

    // Optimize Rand to true literals.
    Seq(
      literal10d > rand5,
      literal10d >= rand5,
      rand5 < literal10d,
      rand5 <= literal10d
    ).foreach { comparison =>
      val plan = testRelation.select(comparison.as("flag")).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = testRelation.select(Alias(TrueLiteral, "flag")()).analyze
      comparePlans(actual, correctAnswer)
    }

    // Optimize Rand to false literals.
    Seq(
      literal10d <= rand5,
      literal10d < rand5,
      rand5 >= literal10d,
      rand5 > literal10d
    ).foreach { comparison =>
      val plan = testRelation.select(comparison.as("flag")).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = testRelation.select(Alias(FalseLiteral, "flag")()).analyze
      comparePlans(actual, correctAnswer)
    }
  }

  test("Prune filter conditions with rand") {

    // Optimize Rand to true literals.
    Seq(
      literal10d > rand5,
      literal10d >= rand5,
      rand5 < literal10d,
      rand5 <= literal10d
    ).foreach { condition =>
      val plan = x.where(condition).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = x.analyze
      comparePlans(actual, correctAnswer)
    }

    // Optimize Rand to false literals.
    Seq(
      literal10d <= rand5,
      literal10d < rand5,
      rand5 >= literal10d,
      rand5 > literal10d
    ).foreach { condition =>
      val plan = x.where(condition).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = testRelation.analyze
      comparePlans(actual, correctAnswer)
    }
  }

  test("Constant folding with rand") {

    Seq(
      And(literal10d > rand5, literal10d >= rand5),
      And(rand5 < literal10d, rand5 <= literal10d)
    ).foreach { condition =>
      val plan = x.where(condition).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = x.analyze
      comparePlans(actual, correctAnswer)
    }

    Seq(
      Or(literal10d <= rand5, literal10d < rand5),
      Or(rand5 >= literal10d, rand5 > literal10d)
    ).foreach { condition =>
      val plan = x.where(condition).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = testRelation.analyze
      comparePlans(actual, correctAnswer)
    }
  }

  test("Simplify filter conditions with rand") {
    val aIsNotNull = $"a".isNotNull

    Seq(
      And(literal10d > rand5, aIsNotNull),
      And(literal10d >= rand5, aIsNotNull),
      And(rand5 < literal10d, aIsNotNull),
      And(rand5 <= literal10d, aIsNotNull)
    ).foreach { condition =>
      val plan = testRelation.where(condition).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = testRelation.where(condition.right).analyze
      comparePlans(actual, correctAnswer)
    }

    Seq(
      Or(literal10d <= rand5, aIsNotNull),
      Or(literal10d < rand5, aIsNotNull),
      Or(rand5 >= literal10d, aIsNotNull),
      Or(rand5 > literal10d, aIsNotNull)
    ).foreach { condition =>
      val plan = testRelation.where(condition).analyze
      val actual = Optimize.execute(plan)
      val correctAnswer = testRelation.where(condition.right).analyze
      comparePlans(actual, correctAnswer)
    }
  }

}
