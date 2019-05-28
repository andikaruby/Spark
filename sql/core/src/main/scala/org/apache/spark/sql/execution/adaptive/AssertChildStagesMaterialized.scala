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

package org.apache.spark.sql.execution.adaptive

import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.SparkPlan

// A sanity check rule to make sure we are running query stage optimizer rules on a sub-tree of
// query plan with all child query stages materialized.
object AssertChildStagesMaterialized extends Rule[SparkPlan] {
  override def apply(plan: SparkPlan): SparkPlan = plan.transform {
    case q: QueryStageExec if !q.materialize().isCompleted =>
      throw new IllegalArgumentException(
        s"The input query stages should all be materialized, but the below one is not.\n ${q.plan}")
  }
}
