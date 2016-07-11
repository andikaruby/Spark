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

import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.plans.logical.LocalRelation
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSQLContext

class OptimizeMetadataOnlyQuerySuite extends QueryTest with SharedSQLContext {
  import testImplicits._

  override def beforeAll(): Unit = {
    super.beforeAll()
    val data = (1 to 10).map(i => (i, s"data-$i", i % 2, if ((i % 2) == 0) "even" else "odd"))
      .toDF("col1", "col2", "partcol1", "partcol2")
    data.write.partitionBy("partcol1", "partcol2").mode("append").saveAsTable("srcpart")
  }

  override protected def afterAll(): Unit = {
    try {
      sql("DROP TABLE IF EXISTS srcpart")
    } finally {
      super.afterAll()
    }
  }

  private def assertMetadataOnlyQuery(df: DataFrame): Unit = {
    val localRelations = df.queryExecution.optimizedPlan.collect {
      case l @ LocalRelation(_, _) => l
    }
    assert(localRelations.size == 1)
  }

  private def assertNotMetadataOnlyQuery(df: DataFrame): Unit = {
    val localRelations = df.queryExecution.optimizedPlan.collect {
      case l @ LocalRelation(_, _) => l
    }
    assert(localRelations.size == 0)
  }

  private def testMetadataOnly(name: String, sqls: String*): Unit = {
    test(name) {
      withSQLConf(SQLConf.OPTIMIZER_METADATA_ONLY.key -> "true") {
        sqls.foreach { case q => assertMetadataOnlyQuery(sql(q)) }
      }
      withSQLConf(SQLConf.OPTIMIZER_METADATA_ONLY.key -> "false") {
        sqls.foreach { case q => assertNotMetadataOnlyQuery(sql(q)) }
      }
    }
  }

  private def testUnspportedMetadataOnly(name: String, sqls: String*): Unit = {
    test(name) {
      withSQLConf(SQLConf.OPTIMIZER_METADATA_ONLY.key -> "true") {
        sqls.foreach { case q => assertNotMetadataOnlyQuery(sql(q)) }
      }
      withSQLConf(SQLConf.OPTIMIZER_METADATA_ONLY.key -> "false") {
        sqls.foreach { case q => assertNotMetadataOnlyQuery(sql(q)) }
      }
    }
  }

  testMetadataOnly(
    "OptimizeMetadataOnlyQuery test: aggregate expression is partition columns",
    "select partcol1 from srcpart group by partcol1",
    "select partcol2 from srcpart where partcol1 = 0 group by partcol2")

  testMetadataOnly(
    "OptimizeMetadataOnlyQuery test: distinct aggregate function on partition columns",
    "SELECT partcol1, count(distinct partcol2) FROM srcpart group by partcol1",
    "SELECT partcol1, count(distinct partcol2) FROM srcpart where partcol1 = 0 group by partcol1")

  testMetadataOnly(
    "OptimizeMetadataOnlyQuery test: distinct on partition columns",
    "select distinct partcol1, partcol2 from srcpart",
    "select distinct c1 from (select partcol1 + 1 as c1 from srcpart where partcol1 = 0) t")

  testMetadataOnly(
    "OptimizeMetadataOnlyQuery test: aggregate function on partition columns which have same " +
      "result w or w/o DISTINCT keyword.",
    "select max(partcol1) from srcpart",
    "select min(partcol1) from srcpart where partcol1 = 0",
    "select first(partcol1) from srcpart",
    "select last(partcol1) from srcpart where partcol1 = 0",
    "select partcol2, min(partcol1) from srcpart where partcol1 = 0 group by partcol2",
    "select max(c1) from (select partcol1 + 1 as c1 from srcpart where partcol1 = 0) t")

  testUnspportedMetadataOnly(
    "OptimizeMetadataOnlyQuery test: unsupported for non-partition columns",
    "select col1 from srcpart group by col1",
    "select partcol1, max(col1) from srcpart group by partcol1",
    "select partcol1, count(distinct col1) from srcpart group by partcol1",
    "select distinct partcol1, col1 from srcpart")

  testUnspportedMetadataOnly(
    "OptimizeMetadataOnlyQuery test: unsupported for non-distinct aggregate function on " +
    "partition columns",
    "select partcol1, sum(partcol2) from srcpart group by partcol1",
    "select partcol1, count(partcol2) from srcpart group by partcol1")

  testUnspportedMetadataOnly(
    "OptimizeMetadataOnlyQuery test: unsupported for GroupingSet/Union operator",
    "select partcol1, max(partcol2) from srcpart where partcol1 = 0 group by rollup (partcol1)",
    "select partcol2 from (select partcol2 from srcpart where partcol1 = 0 union all " +
      "select partcol2 from srcpart where partcol1 = 1) t group by partcol2")
}
