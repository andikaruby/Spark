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
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules._

class NullFilteringSuite extends PlanTest {

  object Optimize extends RuleExecutor[LogicalPlan] {
    val batches = Batch("NullFiltering", Once, NullFiltering) ::
      Batch("CombineFilters", FixedPoint(5), CombineFilters) :: Nil
  }

  def checkNullability(query: LogicalPlan): Unit = {
    val constraints = query.constraints
    val output = query.output

    val notNullOutput = query.constraints
      .filter(_.isInstanceOf[IsNotNull])
      .flatMap(_.references)

    notNullOutput.foreach { o =>
      if (query.outputSet.contains(o)) {
        assert(query.output.exists(q => o.exprId == q.exprId && !q.nullable))
      }
    }
  }

  val testRelation = LocalRelation('a.int, 'b.int, 'c.int)

  test("filter: filter out nulls in condition") {
    val originalQuery = testRelation.where('a === 1).analyze
    val correctAnswer = testRelation.where(IsNotNull('a) && 'a === 1).analyze
    val optimized = Optimize.execute(originalQuery)
    comparePlans(optimized, correctAnswer)
  }

  test("single inner join: filter out nulls on either side on equi-join keys") {
    val x = testRelation.subquery('x)
    val y = testRelation.subquery('y)
    val originalQuery = x.join(y,
      condition = Some(("x.a".attr === "y.a".attr) && ("x.b".attr === 1) && ("y.c".attr > 5)))
      .analyze
    checkNullability(originalQuery)
    val left = x.where(IsNotNull('a) && IsNotNull('b))
    val right = y.where(IsNotNull('a) && IsNotNull('c))
    val correctAnswer = left.join(right,
      condition = Some(("x.a".attr === "y.a".attr) && ("x.b".attr === 1) && ("y.c".attr > 5)))
      .analyze
    val optimized = Optimize.execute(originalQuery)
    comparePlans(optimized, correctAnswer)
    checkNullability(optimized)
  }

  test("single inner join: filter out nulls on either side on non equal keys") {
    val x = testRelation.subquery('x)
    val y = testRelation.subquery('y)
    val originalQuery = x.join(y,
      condition = Some(("x.a".attr =!= "y.a".attr) && ("x.b".attr === 1) && ("y.c".attr > 5)))
      .analyze
    checkNullability(originalQuery)
    val left = x.where(IsNotNull('a) && IsNotNull('b))
    val right = y.where(IsNotNull('a) && IsNotNull('c))
    val correctAnswer = left.join(right,
      condition = Some(("x.a".attr =!= "y.a".attr) && ("x.b".attr === 1) && ("y.c".attr > 5)))
      .analyze
    val optimized = Optimize.execute(originalQuery)
    comparePlans(optimized, correctAnswer)
    checkNullability(optimized)
  }

  test("single inner join with pre-existing filters: filter out nulls on either side") {
    val x = testRelation.subquery('x)
    val y = testRelation.subquery('y)
    val originalQuery = x.where('b > 5).join(y.where('c === 10),
      condition = Some("x.a".attr === "y.a".attr)).analyze
    checkNullability(originalQuery)
    val left = x.where(IsNotNull('a) && IsNotNull('b) && 'b > 5)
    val right = y.where(IsNotNull('a) && IsNotNull('c) && 'c === 10)
    val correctAnswer = left.join(right,
      condition = Some("x.a".attr === "y.a".attr)).analyze
    val optimized = Optimize.execute(originalQuery)
    comparePlans(optimized, correctAnswer)
    checkNullability(optimized)
  }

  test("single outer join: no null filters are generated") {
    val x = testRelation.subquery('x)
    val y = testRelation.subquery('y)
    val originalQuery = x.join(y, FullOuter,
      condition = Some("x.a".attr === "y.a".attr)).analyze
    checkNullability(originalQuery)
    val optimized = Optimize.execute(originalQuery)
    comparePlans(optimized, originalQuery)
    checkNullability(optimized)
  }

  test("multiple inner joins: filter out nulls on all sides on equi-join keys") {
    val t1 = testRelation.subquery('t1)
    val t2 = testRelation.subquery('t2)
    val t3 = testRelation.subquery('t3)
    val t4 = testRelation.subquery('t4)

    val originalQuery = t1
      .join(t2, condition = Some("t1.b".attr === "t2.b".attr))
      .join(t3, condition = Some("t2.b".attr === "t3.b".attr))
      .join(t4, condition = Some("t3.b".attr === "t4.b".attr)).analyze
    checkNullability(originalQuery)
    val correctAnswer = t1.where(IsNotNull('b))
      .join(t2.where(IsNotNull('b)), condition = Some("t1.b".attr === "t2.b".attr))
      .join(t3.where(IsNotNull('b)), condition = Some("t2.b".attr === "t3.b".attr))
      .join(t4.where(IsNotNull('b)), condition = Some("t3.b".attr === "t4.b".attr)).analyze
    val optimized = Optimize.execute(originalQuery)
    comparePlans(optimized, correctAnswer)
    checkNullability(correctAnswer)
    checkNullability(optimized)
  }
}
