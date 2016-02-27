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

package org.apache.spark.sql.hive

import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.ParserInterface
import org.apache.spark.sql.catalyst.analysis.{Analyzer, Catalog, FunctionRegistry, OverrideCatalog}
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.execution.{python, SparkPlanner}
import org.apache.spark.sql.execution.datasources._
import org.apache.spark.sql.internal.{SessionState, SQLConf}


/**
 * A class that holds all session-specific state in a given [[HiveContext]].
 */
private[hive] class HiveSessionState(ctx: HiveContext) extends SessionState(ctx) {

  /**
   * A metadata catalog that points to the Hive metastore.
   */
  val metastoreCatalog: HiveMetastoreCatalog = {
    new HiveMetastoreCatalog(ctx.metadataHive, ctx) with OverrideCatalog
  }

  override lazy val conf: SQLConf = new SQLConf {
    override def caseSensitiveAnalysis: Boolean = getConf(SQLConf.CASE_SENSITIVE, false)
  }

  override lazy val catalog: Catalog = metastoreCatalog

  /**
   * Internal catalog for managing functions registered by the user.
   * Note that HiveUDFs will be overridden by functions registered in this context.
   */
  override lazy val functionRegistry: FunctionRegistry = {
    val registry = new HiveFunctionRegistry(FunctionRegistry.builtin.copy(), ctx.executionHive)
    // The Hive UDF current_database() is foldable, will be evaluated by optimizer,
    // but the optimizer can't access the SessionState of metadataHive.
    registry.registerFunction("current_database", (e: Seq[Expression]) => new CurrentDatabase(ctx))
    registry
  }

  /**
   * An analyzer that uses the Hive metastore.
   */
  override val analyzer: Analyzer = {
    new Analyzer(metastoreCatalog, functionRegistry, conf) {
      override val extendedResolutionRules =
        metastoreCatalog.ParquetConversions ::
        metastoreCatalog.CreateTables ::
        metastoreCatalog.PreInsertionCasts ::
        python.ExtractPythonUDFs ::
        PreInsertCastAndRename ::
        (if (conf.runSQLOnFile) new ResolveDataSource(ctx) :: Nil else Nil)

      override val extendedCheckRules = Seq(PreWriteCheck(catalog))
    }
  }

  /**
   * Parser for HiveQl query texts.
   */
  override val sqlParser: ParserInterface = new HiveQl(conf)

  /**
   * Planner that takes into account Hive-specific strategies.
   */
  override val planner: SparkPlanner = {
    new SparkPlanner(ctx) with HiveStrategies {
      override val hiveContext = ctx

      override def strategies: Seq[Strategy] = {
        ctx.experimental.extraStrategies ++ Seq(
          DataSourceStrategy,
          HiveCommandStrategy(ctx),
          HiveDDLStrategy,
          DDLStrategy,
          SpecialLimits,
          InMemoryScans,
          HiveTableScans,
          DataSinks,
          Scripts,
          Aggregation,
          LeftSemiJoin,
          EquiJoinSelection,
          BasicOperators,
          BroadcastNestedLoop,
          CartesianProduct,
          DefaultJoin
        )
      }
    }
  }

}
