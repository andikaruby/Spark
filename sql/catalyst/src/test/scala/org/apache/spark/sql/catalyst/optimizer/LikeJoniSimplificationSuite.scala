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
import org.apache.spark.sql.catalyst.plans.PlanTest
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules._
import org.apache.spark.sql.types.{BooleanType, StringType}

class LikeJoniSimplificationSuite extends PlanTest {
  object Optimize extends RuleExecutor[LogicalPlan] {
    val batches =
      Batch("Like Simplification", Once,
        LikeSimplification) :: Nil
  }

  val testRelation = LocalRelation('a.string)

  test("simplify Like into StartsWith") {
    val originalQuery =
      testRelation
        .where(('a jlike "abc%") || ('a jlike "abc\\%"))

    val optimized = Optimize.execute(originalQuery.analyze)
    val correctAnswer = testRelation
      .where(StartsWith('a, "abc") || ('a jlike "abc\\%"))
      .analyze

    comparePlans(optimized, correctAnswer)
  }

  test("simplify Like into EndsWith") {
    val originalQuery =
      testRelation
        .where('a jlike "%xyz")

    val optimized = Optimize.execute(originalQuery.analyze)
    val correctAnswer = testRelation
      .where(EndsWith('a, "xyz"))
      .analyze

    comparePlans(optimized, correctAnswer)
  }

  test("simplify Like into startsWith and EndsWith") {
    val originalQuery =
      testRelation
        .where(('a jlike "abc\\%def") || ('a jlike "abc%def"))

    val optimized = Optimize.execute(originalQuery.analyze)
    val correctAnswer = testRelation
      .where(('a jlike "abc\\%def") ||
        (Length('a) >= 6 && (StartsWith('a, "abc") && EndsWith('a, "def"))))
      .analyze

    comparePlans(optimized, correctAnswer)
  }

  test("simplify Like into Contains") {
    val originalQuery =
      testRelation
        .where(('a jlike "%mn%") || ('a jlike "%mn\\%"))

    val optimized = Optimize.execute(originalQuery.analyze)
    val correctAnswer = testRelation
      .where(Contains('a, "mn") || ('a jlike "%mn\\%"))
      .analyze

    comparePlans(optimized, correctAnswer)
  }

  test("simplify Like into EqualTo") {
    val originalQuery =
      testRelation
        .where(('a jlike "") || ('a jlike "abc"))

    val optimized = Optimize.execute(originalQuery.analyze)
    val correctAnswer = testRelation
      .where(('a === "") || ('a === "abc"))
      .analyze

    comparePlans(optimized, correctAnswer)
  }

  test("null pattern") {
    val originalQuery = testRelation.where('a jlike Literal(null, StringType)).analyze
    val optimized = Optimize.execute(originalQuery)
    comparePlans(optimized, testRelation.where(Literal(null, BooleanType)).analyze)
  }

  test("test like escape syntax") {
    val originalQuery1 = testRelation.where('a.jlike("abc#%", '#'))
    val optimized1 = Optimize.execute(originalQuery1.analyze)
    comparePlans(optimized1, originalQuery1.analyze)

    val originalQuery2 = testRelation.where('a.jlike("abc#%abc", '#'))
    val optimized2 = Optimize.execute(originalQuery2.analyze)
    comparePlans(optimized2, originalQuery2.analyze)
  }

  test("SPARK-33677: LikeSimplification should be skipped if pattern contains any escapeChar") {
    val originalQuery1 =
      testRelation
        .where(('a jlike "abc%") || ('a jlike "\\abc%"))
    val optimized1 = Optimize.execute(originalQuery1.analyze)
    val correctAnswer1 = testRelation
      .where(StartsWith('a, "abc") || ('a jlike "\\abc%"))
      .analyze
    comparePlans(optimized1, correctAnswer1)

    val originalQuery2 =
      testRelation
        .where(('a jlike "%xyz") || ('a jlike "%xyz\\"))
    val optimized2 = Optimize.execute(originalQuery2.analyze)
    val correctAnswer2 = testRelation
      .where(EndsWith('a, "xyz") || ('a jlike "%xyz\\"))
      .analyze
    comparePlans(optimized2, correctAnswer2)

    val originalQuery3 =
      testRelation
        .where(('a jlike("@bc%def", '@')) || ('a jlike "abc%def"))
    val optimized3 = Optimize.execute(originalQuery3.analyze)
    val correctAnswer3 = testRelation
      .where(('a jlike("@bc%def", '@')) ||
        (Length('a) >= 6 && (StartsWith('a, "abc") && EndsWith('a, "def"))))
      .analyze
    comparePlans(optimized3, correctAnswer3)

    val originalQuery4 =
      testRelation
        .where(('a jlike "%mn%") || ('a jlike("%mn%", '%')))
    val optimized4 = Optimize.execute(originalQuery4.analyze)
    val correctAnswer4 = testRelation
      .where(Contains('a, "mn") || ('a jlike("%mn%", '%')))
      .analyze
    comparePlans(optimized4, correctAnswer4)

    val originalQuery5 =
      testRelation
        .where(('a jlike "abc") || ('a jlike("abbc", 'b')))
    val optimized5 = Optimize.execute(originalQuery5.analyze)
    val correctAnswer5 = testRelation
      .where(('a === "abc") || ('a jlike("abbc", 'b')))
      .analyze
    comparePlans(optimized5, correctAnswer5)
  }

}
