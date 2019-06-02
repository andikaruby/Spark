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

import org.apache.spark.annotation.Unstable
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.analysis.Analyzer
import org.apache.spark.sql.catalyst.catalog.ExternalCatalogWithListener
import org.apache.spark.sql.catalyst.optimizer.Optimizer
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.{SparkOptimizer, SparkPlanner}
import org.apache.spark.sql.execution.analysis.DetectAmbiguousSelfJoin
import org.apache.spark.sql.execution.datasources._
import org.apache.spark.sql.execution.datasources.v2.TableCapabilityCheck
import org.apache.spark.sql.hive.client.HiveClient
import org.apache.spark.sql.internal.{BaseSessionStateBuilder, SessionResourceLoader, SessionState}

/**
 * Builder that produces a Hive-aware `SessionState`.
 */
@Unstable
class HiveSessionStateBuilder(session: SparkSession, parentState: Option[SessionState] = None)
  extends BaseSessionStateBuilder(session, parentState) {

  private def externalCatalog: ExternalCatalogWithListener = session.sharedState.externalCatalog

  /**
   * Create a Hive aware resource loader.
   */
  override protected lazy val resourceLoader: HiveSessionResourceLoader = {
    new HiveSessionResourceLoader(
      session, () => externalCatalog.unwrapped.asInstanceOf[HiveExternalCatalog].client)
  }

  /**
   * Create a [[HiveSessionCatalog]].
   */
  override protected lazy val catalog: HiveSessionCatalog = {
    val catalog = new HiveSessionCatalog(
      () => externalCatalog,
      () => session.sharedState.globalTempViewManager,
      new HiveMetastoreCatalog(session),
      functionRegistry,
      conf,
      SessionState.newHadoopConf(session.sparkContext.hadoopConfiguration, conf),
      sqlParser,
      resourceLoader)
    parentState.foreach(_.catalog.copyStateTo(catalog))
    catalog
  }

  protected lazy val mvCatalog: HiveMvCatalog =
    session.sharedState.mvCatalog.asInstanceOf[HiveMvCatalog]

  /**
   * A logical query plan `Analyzer` with rules specific to Hive.
   */
  override protected def analyzer: Analyzer = new Analyzer(catalog, v2SessionCatalog, conf) {
    override val extendedResolutionRules: Seq[Rule[LogicalPlan]] =
      new ResolveHiveSerdeTable(session) +:
        new FindDataSourceTable(session) +:
        new ResolveSQLOnFile(session) +:
        new FallBackFileSourceV2(session) +:
        DataSourceResolution(conf, this.catalogManager) +:
        customResolutionRules

    override val postHocResolutionRules: Seq[Rule[LogicalPlan]] =
      new DetectAmbiguousSelfJoin(conf) +:
        new DetermineTableStats(session) +:
        RelationConversions(conf, catalog) +:
        PreprocessTableCreation(session) +:
        PreprocessTableInsertion(conf) +:
        DataSourceAnalysis(conf) +:
        HiveAnalysis +:
        customPostHocResolutionRules

    override val extendedCheckRules: Seq[LogicalPlan => Unit] =
      PreWriteCheck +:
        PreReadCheck +:
        TableCapabilityCheck +:
        customCheckRules
  }


  /**
    * Logical query plan optimizer.
    *
    * Note: this depends on the `conf`, `catalog` and `experimentalMethods` fields.
    */
  override protected def optimizer: Optimizer = {
    new SparkOptimizer(catalog, experimentalMethods) {
      override def defaultBatches: Seq[Batch] = {
        if (conf.mvReplacementEnabeld) {
          var newBatches: List[Batch] = List()
          val oldBatches = super.defaultBatches.toArray
          oldBatches.foreach(oldBatch => {
            var batch = oldBatch
            /*
            Why MV rule is added to "Operator Optimization after Inferring Filters" batch
            query: select * from a join b where a.id = b.id and b.id = 1
            scala> df.queryExecution.optimizedPlan
              res0: org.apache.spark.sql.catalyst.plans.logical.LogicalPlan =
              Join Inner, (id#0 = id#3)
              :- Filter (isnotnull(id#0) && (id#0 = 5))
              :  +- Relation[id#0,col1#1,col2#2] csv
              +- Filter (isnotnull(id#3) && (id#3 = 5))
                 +- Relation[id#3,col1#4,col2#5] csv
              In the above query though an explicit filter was not part of a, it got introduced due to
              InferFiltersFromConstraints, as a result mv of a table got used
             */
            if (oldBatch.name.equalsIgnoreCase("Operator Optimization after Inferring Filters")) {
              val newRules =
                SubstituteMaterializedOSView(session, mvCatalog) +: oldBatch.rules.toArray
              batch = Batch("Operator Optimization after Inferring Filters",
                oldBatch.strategy, newRules: _*)
            }
            newBatches :+= batch
          })
          newBatches
        } else {
          super.defaultBatches
        }
      }

      /**
      All methods overriden in the super class(BaseSessionStateBuilder)
        needs to be overriden in similar manner(copied) since we are creating
        new instance of SparkOptimizer
        */
      override def extendedOperatorOptimizationRules: Seq[Rule[LogicalPlan]] =
        super.extendedOperatorOptimizationRules ++ customOperatorOptimizationRules
    }
  }

  /**
   * Planner that takes into account Hive-specific strategies.
   */
  override protected def planner: SparkPlanner = {
    new SparkPlanner(session.sparkContext, conf, experimentalMethods) with HiveStrategies {
      override val sparkSession: SparkSession = session

      override def extraPlanningStrategies: Seq[Strategy] =
        super.extraPlanningStrategies ++ customPlanningStrategies ++ Seq(HiveTableScans, Scripts)
    }
  }

  override protected def newBuilder: NewBuilder = new HiveSessionStateBuilder(_, _)
}

class HiveSessionResourceLoader(
    session: SparkSession,
    clientBuilder: () => HiveClient)
  extends SessionResourceLoader(session) {
  private lazy val client = clientBuilder()
  override def addJar(path: String): Unit = {
    client.addJar(path)
    super.addJar(path)
  }
}
