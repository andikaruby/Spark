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
import org.apache.spark.sql.catalyst.expressions.{Explode, Literal}
import org.apache.spark.sql.catalyst.plans.{Inner, PlanTest}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.RuleExecutor
import org.apache.spark.sql.types.StringType

class ColumnPruningSuite extends PlanTest {

  object Optimize extends RuleExecutor[LogicalPlan] {
    val batches = Batch("Column pruning", FixedPoint(100),
      ColumnPruning) :: Nil
  }

  test("Column pruning for Generate when Generate.join = false") {
    val input = LocalRelation('a.int, 'b.array(StringType))

    val query = input.generate(Explode('b), join = false).analyze

    val optimized = Optimize.execute(query)

    val correctAnswer = input.select('b).generate(Explode('b), join = false).analyze

    comparePlans(optimized, correctAnswer)
  }

  test("Column pruning for Generate when Generate.join = true") {
    val input = LocalRelation('a.int, 'b.int, 'c.array(StringType))

    val query =
      input
        .generate(Explode('c), join = true, outputNames = "explode" :: Nil)
        .select('a, 'explode)
        .analyze

    val optimized = Optimize.execute(query)

    val correctAnswer =
      input
        .select('a, 'c)
        .generate(Explode('c), join = true, outputNames = "explode" :: Nil)
        .select('a, 'explode)
        .analyze

    comparePlans(optimized, correctAnswer)
  }

  test("Turn Generate.join to false if possible") {
    val input = LocalRelation('b.array(StringType))

    val query =
      input
        .generate(Explode('b), join = true, outputNames = "explode" :: Nil)
        .select(('explode + 1).as("result"))
        .analyze

    val optimized = Optimize.execute(query)

    val correctAnswer =
      input
        .generate(Explode('b), join = false, outputNames = "explode" :: Nil)
        .select(('explode + 1).as("result"))
        .analyze

    comparePlans(optimized, correctAnswer)
  }

  test("Column pruning for Project on Sort") {
    val input = LocalRelation('a.int, 'b.string, 'c.double)

    val query = input.orderBy('b.asc).select('a).analyze
    val optimized = Optimize.execute(query)

    val correctAnswer = input.select('a, 'b).orderBy('b.asc).select('a).analyze

    comparePlans(optimized, correctAnswer)
  }

  test("Column pruning for Expand") {
    val input = LocalRelation('a.int, 'b.string, 'c.double)
    val query =
      Aggregate(
        Seq('aa, 'gid),
        Seq(sum('c).as("sum")),
        Expand(
          Seq(
            Seq('a, 'b, 'c, Literal.create(null, StringType), 1),
            Seq('a, 'b, 'c, 'a, 2)),
          Seq('a, 'b, 'c, 'aa.int, 'gid.int),
          input)).analyze
    val optimized = Optimize.execute(query)

    val expected =
      Aggregate(
        Seq('aa, 'gid),
        Seq(sum('c).as("sum")),
        Expand(
          Seq(
            Seq('c, Literal.create(null, StringType), 1),
            Seq('c, 'a, 2)),
          Seq('c, 'aa.int, 'gid.int),
          Project(Seq('c, 'a),
            input))).analyze

    comparePlans(optimized, expected)
  }

  test("Keep broadcast hint when pruning on Join") {
    val input = LocalRelation('key.int, 'value.string)

    val query =
      Project(Seq($"x.key", $"y.key"),
        Join(
          SubqueryAlias("x", input),
          BroadcastHint(SubqueryAlias("y", input)), Inner, None)).analyze

    val optimized = Optimize.execute(query)

    val expected =
      Project(Seq($"x.key", $"y.key"),
        Join(
          Project(Seq($"x.key"), SubqueryAlias("x", input)),
          Project(Seq($"y.key"),
            BroadcastHint(SubqueryAlias("y", input))),
          Inner, None)).analyze

    comparePlans(optimized, expected)

    assert(optimized.collect {
      case b @ BroadcastHint(_) if b.statistics.sizeInBytes == 1 => 1
    }.nonEmpty)
  }

  // todo: add more tests for column pruning
}
