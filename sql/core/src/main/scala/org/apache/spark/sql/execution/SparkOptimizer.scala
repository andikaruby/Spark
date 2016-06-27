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

import org.apache.spark.sql.{ExperimentalMethods, SparkSession}
import org.apache.spark.sql.catalyst.analysis.RewriteDistinctAggregates
import org.apache.spark.sql.catalyst.catalog.SessionCatalog
import org.apache.spark.sql.catalyst.optimizer.Optimizer
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.python.ExtractPythonUDFFromAggregate
import org.apache.spark.sql.internal.SQLConf

class SparkOptimizer(
    sparkSession: SparkSession,
    catalog: SessionCatalog,
    conf: SQLConf,
    experimentalMethods: ExperimentalMethods)
  extends Optimizer(catalog, conf) {

  override def batches: Seq[Batch] = super.batches :+
    Batch("Metadata Only Optimization", Once, MetadataOnlyOptimizer(sparkSession, catalog)) :+
    Batch("Extract Python UDF from Aggregate", Once, ExtractPythonUDFFromAggregate) :+
    Batch("User Provided Optimizers", fixedPoint, experimentalMethods.extraOptimizations: _*)
}
