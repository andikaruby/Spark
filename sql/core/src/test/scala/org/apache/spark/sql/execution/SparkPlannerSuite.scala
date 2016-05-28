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

package org.apache.spark.sql.execution

import org.apache.spark.sql.Strategy
import org.apache.spark.sql.catalyst.plans.logical.{LocalRelation, LogicalPlan, ReturnAnswer, Union}
import org.apache.spark.sql.test.SharedSQLContext

class SparkPlannerSuite extends SharedSQLContext {
  import testImplicits._

  private var planned = 0

  private object MayPlanRecursively extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case ReturnAnswer(child) =>
        planned += 1
        planLater(child) :: planLater(plan) :: Nil
      case Union(children) =>
        planned += 1
        UnionExec(children.map(planLater)) :: planLater(plan) :: Nil
      case LocalRelation(output, data) =>
        planned += 1
        LocalTableScanExec(output, data) :: planLater(plan) :: Nil
      case _ => Nil
    }
  }

  test("Ensure to go down only the first branch, not any other possible branches") {
    try {
      spark.experimental.extraStrategies = MayPlanRecursively :: Nil

      val ds = Seq("a", "b", "c").toDS().union(Seq("d", "e", "f").toDS())

      planned = 0
      assert(ds.collect().toSeq === Seq("a", "b", "c", "d", "e", "f"))
      assert(planned === 4)
    } finally {
      spark.experimental.extraStrategies = Nil
    }
  }
}
