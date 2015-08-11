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

package org.apache.spark.sql.sources

import java.io.File

import org.scalatest.BeforeAndAfterAll

import org.apache.spark.sql.{SaveMode, AnalysisException, Row}
import org.apache.spark.util.Utils

class InsertSuite extends DataSourceTest with BeforeAndAfterAll {
  private lazy val sparkContext = caseInsensitiveContext.sparkContext
  private var path: File = null

  override def beforeAll(): Unit = {
    super.beforeAll()
    path = Utils.createTempDir()
    val rdd = sparkContext.parallelize((1 to 10).map(i => s"""{"a":$i, "b":"str$i"}"""))
    caseInsensitiveContext.read.json(rdd).registerTempTable("jt")
    caseInsensitiveContext.sql(
      s"""
        |CREATE TEMPORARY TABLE jsonTable (a int, b string)
        |USING org.apache.spark.sql.json.DefaultSource
        |OPTIONS (
        |  path '${path.toString}'
        |)
      """.stripMargin)
  }

  override def afterAll(): Unit = {
    caseInsensitiveContext.dropTempTable("jsonTable")
    caseInsensitiveContext.dropTempTable("jt")
    Utils.deleteRecursively(path)
    super.afterAll()
  }

  test("Simple INSERT OVERWRITE a JSONRelation") {
    caseInsensitiveContext.sql(
      s"""
        |INSERT OVERWRITE TABLE jsonTable SELECT a, b FROM jt
      """.stripMargin)

    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i, s"str$i"))
    )
  }

  test("PreInsert casting and renaming") {
    caseInsensitiveContext.sql(
      s"""
        |INSERT OVERWRITE TABLE jsonTable SELECT a * 2, a * 4 FROM jt
      """.stripMargin)

    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i * 2, s"${i * 4}"))
    )

    caseInsensitiveContext.sql(
      s"""
        |INSERT OVERWRITE TABLE jsonTable SELECT a * 4 AS A, a * 6 as c FROM jt
      """.stripMargin)

    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i * 4, s"${i * 6}"))
    )
  }

  test("SELECT clause generating a different number of columns is not allowed.") {
    val message = intercept[RuntimeException] {
      caseInsensitiveContext.sql(
        s"""
        |INSERT OVERWRITE TABLE jsonTable SELECT a FROM jt
      """.stripMargin)
    }.getMessage
    assert(
      message.contains("generates the same number of columns as its schema"),
      "SELECT clause generating a different number of columns should not be not allowed."
    )
  }

  test("INSERT OVERWRITE a JSONRelation multiple times") {
    caseInsensitiveContext.sql(
      s"""
         |INSERT OVERWRITE TABLE jsonTable SELECT a, b FROM jt
    """.stripMargin)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i, s"str$i"))
    )

    // Writing the table to less part files.
    val rdd1 = sparkContext.parallelize((1 to 10).map(i => s"""{"a":$i, "b":"str$i"}"""), 5)
    caseInsensitiveContext.read.json(rdd1).registerTempTable("jt1")
    caseInsensitiveContext.sql(
      s"""
         |INSERT OVERWRITE TABLE jsonTable SELECT a, b FROM jt1
    """.stripMargin)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i, s"str$i"))
    )

    // Writing the table to more part files.
    val rdd2 = sparkContext.parallelize((1 to 10).map(i => s"""{"a":$i, "b":"str$i"}"""), 10)
    caseInsensitiveContext.read.json(rdd2).registerTempTable("jt2")
    caseInsensitiveContext.sql(
      s"""
         |INSERT OVERWRITE TABLE jsonTable SELECT a, b FROM jt2
    """.stripMargin)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i, s"str$i"))
    )

    caseInsensitiveContext.sql(
      s"""
         |INSERT OVERWRITE TABLE jsonTable SELECT a * 10, b FROM jt1
    """.stripMargin)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i * 10, s"str$i"))
    )

    caseInsensitiveContext.dropTempTable("jt1")
    caseInsensitiveContext.dropTempTable("jt2")
  }

  test("INSERT INTO JSONRelation for now") {
    caseInsensitiveContext.sql(
      s"""
      |INSERT OVERWRITE TABLE jsonTable SELECT a, b FROM jt
    """.stripMargin)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      caseInsensitiveContext.sql("SELECT a, b FROM jt").collect()
    )

    caseInsensitiveContext.sql(
      s"""
         |INSERT INTO TABLE jsonTable SELECT a, b FROM jt
    """.stripMargin)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      caseInsensitiveContext.sql("SELECT a, b FROM jt UNION ALL SELECT a, b FROM jt").collect()
    )
  }

  test("save directly to the path of a JSON table") {
    caseInsensitiveContext.table("jt").selectExpr("a * 5 as a", "b")
      .write.mode(SaveMode.Overwrite).json(path.toString)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i * 5, s"str$i"))
    )

    caseInsensitiveContext.table("jt").write.mode(SaveMode.Overwrite).json(path.toString)
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
      (1 to 10).map(i => Row(i, s"str$i"))
    )
  }

  test("it is not allowed to write to a table while querying it.") {
    val message = intercept[AnalysisException] {
      caseInsensitiveContext.sql(
        s"""
        |INSERT OVERWRITE TABLE jsonTable SELECT a, b FROM jsonTable
      """.stripMargin)
    }.getMessage
    assert(
      message.contains("Cannot insert overwrite into table that is also being read from."),
      "INSERT OVERWRITE to a table while querying it should not be allowed.")
  }

  test("Caching")  {
    // write something to the jsonTable
    caseInsensitiveContext.sql(
      s"""
         |INSERT OVERWRITE TABLE jsonTable SELECT a, b FROM jt
      """.stripMargin)
    // Cached Query Execution
    caseInsensitiveContext.cacheTable("jsonTable")
    assertCached(caseInsensitiveContext.sql("SELECT * FROM jsonTable"))
    checkAnswer(
      caseInsensitiveContext.sql("SELECT * FROM jsonTable"),
      (1 to 10).map(i => Row(i, s"str$i")))

    assertCached(caseInsensitiveContext.sql("SELECT a FROM jsonTable"))
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a FROM jsonTable"),
      (1 to 10).map(Row(_)).toSeq)

    assertCached(caseInsensitiveContext.sql("SELECT a FROM jsonTable WHERE a < 5"))
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a FROM jsonTable WHERE a < 5"),
      (1 to 4).map(Row(_)).toSeq)

    assertCached(caseInsensitiveContext.sql("SELECT a * 2 FROM jsonTable"))
    checkAnswer(
      caseInsensitiveContext.sql("SELECT a * 2 FROM jsonTable"),
      (1 to 10).map(i => Row(i * 2)).toSeq)

    assertCached(caseInsensitiveContext.sql(
      "SELECT x.a, y.a FROM jsonTable x JOIN jsonTable y ON x.a = y.a + 1"), 2)
    checkAnswer(caseInsensitiveContext.sql(
      "SELECT x.a, y.a FROM jsonTable x JOIN jsonTable y ON x.a = y.a + 1"),
      (2 to 10).map(i => Row(i, i - 1)).toSeq)

    // Insert overwrite and keep the same schema.
    caseInsensitiveContext.sql(
      s"""
        |INSERT OVERWRITE TABLE jsonTable SELECT a * 2, b FROM jt
      """.stripMargin)
    // jsonTable should be recached.
    assertCached(caseInsensitiveContext.sql("SELECT * FROM jsonTable"))
    // TODO we need to invalidate the cached data in InsertIntoHadoopFsRelation
//    // The cached data is the new data.
//    checkAnswer(
//      caseInsensitiveContext.sql("SELECT a, b FROM jsonTable"),
//      caseInsensitiveContext.sql("SELECT a * 2, b FROM jt").collect())
//
//    // Verify uncaching
//    caseInsensitiveContext.uncacheTable("jsonTable")
//    assertCached(caseInsensitiveContext.sql("SELECT * FROM jsonTable"), 0)
  }

  test("it's not allowed to insert into a relation that is not an InsertableRelation") {
    caseInsensitiveContext.sql(
      """
        |CREATE TEMPORARY TABLE oneToTen
        |USING org.apache.spark.sql.sources.SimpleScanSource
        |OPTIONS (
        |  From '1',
        |  To '10'
        |)
      """.stripMargin)

    checkAnswer(
      caseInsensitiveContext.sql("SELECT * FROM oneToTen"),
      (1 to 10).map(Row(_)).toSeq
    )

    val message = intercept[AnalysisException] {
      caseInsensitiveContext.sql(
        s"""
        |INSERT OVERWRITE TABLE oneToTen SELECT CAST(a AS INT) FROM jt
        """.stripMargin)
    }.getMessage
    assert(
      message.contains("does not allow insertion."),
      "It is not allowed to insert into a table that is not an InsertableRelation."
    )

    caseInsensitiveContext.dropTempTable("oneToTen")
  }
}
