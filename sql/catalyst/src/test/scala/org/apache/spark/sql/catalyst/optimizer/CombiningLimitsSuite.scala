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

import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules._
import org.apache.spark.sql.catalyst.dsl.plans._
import org.apache.spark.sql.catalyst.dsl.expressions._

class CombiningLimitsSuite extends OptimizerTest {

  object Optimize extends RuleExecutor[LogicalPlan] {
    val batches =
      Batch("Combine Limit", FixedPoint(2),
        CombineLimits) ::
      Batch("Constant Folding", FixedPoint(3),
        NullPropagation,
        ConstantFolding,
        BooleanSimplification) :: Nil
  }

  val testRelation = LocalRelation('a.int, 'b.int, 'c.int)

  test("limits: combines two limits") {
    val originalQuery =
      testRelation
        .select('a)
        .limit(10).analyze
        .limit(5).analyze

    val optimized = Optimize(originalQuery)
    val correctAnswer =
      testRelation
        .select('a)
        .limit(5).analyze

    comparePlans(optimized, correctAnswer)
  }

  test("limits: combines three limits") {
    val originalQuery =
      testRelation
        .select('a)
        .limit(2).analyze
        .limit(7).analyze
        .limit(5).analyze

    val optimized = Optimize(originalQuery)
    val correctAnswer =
      testRelation
        .select('a)
        .limit(2).analyze

    comparePlans(optimized, correctAnswer)
  }
}
