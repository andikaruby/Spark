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

import org.apache.spark.sql.catalyst.expressions.{Attribute, AttributeSet, Expression}

/**
 * Logical nodes specific to PySpark.
 */

/**
 * FlatMap groups using a udf: pandas.Dataframe -> pandas.DataFrame.
 * This is used by DataFrame.groupby().apply().
 */
case class FlatMapGroupsInPandas(
  groupingAttributes: Seq[Attribute],
  functionExpr: Expression,
  output: Seq[Attribute],
  child: LogicalPlan) extends UnaryNode {
  /**
   * This is needed because output attributes are considered `references` when
   * passed through the constructor.
   *
   * Without this, catalyst will complain that output attributes are missing
   * from the input.
   */
  override val producedAttributes = AttributeSet(output)
}
