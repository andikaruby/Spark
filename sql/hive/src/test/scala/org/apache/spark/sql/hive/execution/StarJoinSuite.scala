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

package org.apache.spark.sql.hive.execution

import org.apache.spark.sql.{Row, _}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.hive.test.TestHiveSingleton
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SQLTestUtils

class StarJoinSuite extends QueryTest with SQLTestUtils with TestHiveSingleton {

   def createTables(): Unit = {
     // Creates tables in a star schema relationship i.e.
     //
     // d1 - f1 - d2
     //      |
     //      d3 - s3
     //
     // Table f1 is the fact table. Tables d1, d2, and d3 are the dimension tables.
     // Dimension d3 is further joined/normalized into table s3.
     // Tables' cardinality: f1 > d1 > d2 > d3 > s3
     sql("create table f1 (f1_fk1 int, f1_fk2 varchar(5), f1_fk3 int, f1_c4 int) using parquet")
     sql("create table d1 (d1_pk1 int, d1_c2 int, d1_c3 int, d1_c4 int) using parquet")
     sql("create table d2 (d2_c2 int, d2_pk1 varchar(5), d2_c3 int, d2_c4 varchar(5))" +
       " using parquet")
     sql("create table d3 (d3_fk1 int, d3_c2 int, d3_pk1 int, d3_c4 int) using parquet")
     sql("create table s3 (s3_pk1 int, s3_c2 int, s3_c3 int, s3_c4 int) using parquet")

     sql("insert into f1 values (1, '2', 3, 4), (2, '1', 2, 3), (3, '3', 1, 2), " +
       "(1, '2', 4, 1), (2, '1', 3, 2), (3, '3', 2, 3)")
     sql("insert into d1 values (1, 2, 3, 3), (2, 1, 2, 3), (3, 3, 1, 2), (4, 2, 4, 3)")
     sql("insert into d2 values (1, '2', 3, '4'), (2, '1', 2, '3'), (3, '3', 1, '3')")
     sql("insert into d3 values (1, 2, 3, 2), (2, 1, 2, 2), (3, 3, 1, 2), (1, 2, 4, 3)," +
       "(2, 1, 5, 3)")
     sql("insert into s3 values (1, 3, 3, 4), (2, 3, null, 3)")

     // Additional tables to test stats availability
     sql("create table d3_ns (d3_fk1 int, d3_c2 int, d3_pk1 int, d3_c4 int) using parquet")
     sql("create table d3_ss (d3_fk1 int, d3_c2 int, d3_pk1 int, d3_c4 int) using parquet")
     sql("create table f11 (f1_fk1 int, f1_fk2 varchar(5), f1_fk3 int, f1_c4 int) using parquet")

     sql("insert into d3_ns values (1, 2, 3, 4), (2, 1, 2, 4), (3, 3, 1, 2), (1, 2, 4, 4)," +
       "(2, 1, 5, 2)")
     sql("insert into d3_ss values (1, 2, 3, 4), (2, 1, 2, 4), (3, 3, 1, 2), (1, 2, 4, 4)," +
       "(2, 1, 5, 2)")
     sql("insert into f11 values (1, '2', 3, 4), (2, '1', 2, 3), (3, '3', 1, 2)," +
       "(1, '2', 4, 1), (2, '1', 3, 2), (3, '3', 2, 3)")
   }

  def runStats(): Unit = {
    // Run statistics
    sql("ANALYZE TABLE f1 COMPUTE STATISTICS FOR COLUMNS f1_fk1, f1_fk2, f1_fk3, f1_c4")
    sql("ANALYZE TABLE d1 COMPUTE STATISTICS FOR COLUMNS d1_pk1, d1_c2, d1_c3, d1_c4")
    sql("ANALYZE TABLE d2 COMPUTE STATISTICS FOR COLUMNS d2_c2, d2_pk1, d2_c3, d2_c4")
    sql("ANALYZE TABLE d3 COMPUTE STATISTICS FOR COLUMNS d3_fk1, d3_c2, d3_pk1, d3_c4")
    sql("ANALYZE TABLE s3 COMPUTE STATISTICS FOR COLUMNS s3_pk1, s3_c2, s3_c3, s3_c4")

    sql("ANALYZE TABLE f11 COMPUTE STATISTICS FOR COLUMNS f1_fk1, f1_fk2, f1_fk3, f1_c4")
    sql("ANALYZE TABLE d3_ss COMPUTE STATISTICS")
  }

  // Given two semantically equivalent queries, query and equivQuery, this function
  // executes the queries with and without star join enabled and compares their
  // execution plans and query results.
  def verifyStarJoinPlans(query: DataFrame, equivQuery: DataFrame, rows: List[Row]): Unit = {
    var equivQryPlan: Option[LogicalPlan] = None
    var qryPlan: Option[LogicalPlan] = None

    withSQLConf(SQLConf.STARJOIN_OPTIMIZATION.key -> "true") {
      qryPlan = Some(query.queryExecution.optimizedPlan)
      checkAnswer(query, rows)
    }

    withSQLConf(SQLConf.STARJOIN_OPTIMIZATION.key -> "false") {
      equivQryPlan = Some(equivQuery.queryExecution.optimizedPlan)
      checkAnswer(equivQuery, rows)
    }

    comparePlans(qryPlan.get, equivQryPlan.get)
  }

  test("SPARK-17791: Test qualifying star joins") {
    withTable("f1", "d1", "d2", "d3", "s3", "f11", "d3_ns", "d3_ss") {
      createTables()
      runStats()

      // Test 1: Selective equi star join on all dimensions.
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      d3 - s3
      //
      // Positional/default join reordering: d1, f1, d2, d3, s3
      // Star join reordering: f1, d2, d1, d3, s3
      val query1 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d2, f1, d3, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison with the positional join reordering.
      // Default join reordering: f1, d2, d1, d3, s3
      val equivQuery1 = sql(
        """
          | select f1_fk1, f1_fk3
          | from f1, d2, d1, d3, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Run the queries and compare the results.
      verifyStarJoinPlans(query1, equivQuery1, Row(1, 3) :: Nil)

      // Test 2: Star join on a subset of dimensions due to inequality joins
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      |
      //      | (<)
      //      d3 - s3
      //        (=)
      //
      // Default join reordering: d1, f1, d2, d3, s3
      // Star join reordering: f1, d2, d1, s3, d3
      val query2 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, f1, d2, s3, d3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 <= d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query
      // Default join reordering: f1, d2, d1, s3, d3
      val equivQuery2 = sql(
        """
          | select f1_fk1, f1_fk3
          | from f1, d2, d1, s3, d3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 <= d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query2, equivQuery2, Row(1, 3) :: Nil)

      // Test 3:  Star join on a subset of dimensions since join column is not unique
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      s3 - d3
      //
      // Default join reordering: d1, f1, d2, s3, d3
      // Star join reordering: f1, d2, d1, s3, d3
      val query3 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, f1, d2, s3, d3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = s3_c2
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query
      // Default join reordering: f1, d2, d1, s3, d3
      val equivQuery3 = sql(
        """
          | select f1_fk1, f1_fk3
          | from f1, d2, d1, s3, d3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = s3_c2
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query3, equivQuery3, Row(1, 3) :: Nil)

      // Test 4:  Star join on a subset of dimensions since join column is nullable
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      s3 - d3
      //
      // Default join reordering: d1, f1, d2, s3, d3
      // Star join reordering: f1, d2, d1, s3, d3
      val query4 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, f1, d2, s3, d3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = s3_c3
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query
      // Default join reordering: f1, d2, d1, s3, d3
      val equivQuery4 = sql(
        """
          | select f1_fk1, f1_fk3
          | from f1, d2, d1, s3, d3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = s3_c3
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query4, equivQuery4, Row(1, 3) :: Nil)
    }
  }

  test("SPARK-17791: unit tests") {
    withTable("f1", "d1", "d2", "d3", "s3", "f11", "d3_ns", "d3_ss") {
      createTables()
      runStats()

      /*
      val query5 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_c4 and d2_c2 < 4
          | and f1_fk1 = d1_c4
          | and f1_fk3 = d3_c4
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin).show()
      */
    }
  }

      test("SPARK-17791: Test non qualifying star joins") {
    // Covered tests cases:
    // 1. Table stats not available for some of the joined tables
    // 2. Column stats not available for some of the joined tables
    // 3. Join with complex plans i.e. statistics are not available
    // 4. Comparable fact tables sizes
    // 5. No RI joins
    // 6. Complex join predicates
    // 7. Less than two dimensions
    // 8. Expanding star join
    // 9. Non selective star join

    withTable("f1", "d1", "d2", "d3", "s3", "f11", "d3_ns", "d3_ss") {
      createTables()
      runStats()

      // Test 1: Table stats not available for some joined tables
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      d3_ns - s3
      //
      // Positional/default join reordering: d1, d3_ns, f1, d2, s3
      // Star join reordering: empty
      val query1 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3_ns, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison with the positional join reordering.
      // Default join reordering: d1, d3_ns, f1, d2, s3
      val equivQuery1 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3_ns, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Run the queries and compare the results.
      verifyStarJoinPlans(query1, equivQuery1, Row(1, 3) :: Nil)

      // Test 2: Column stats not available for some joined tables
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      d3_ss - s3
      //
      // Positional/default join reordering: d1, d3_ss, f1, d2, s3
      // Star join reordering: empty
      val query2 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3_ss, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery2 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3_ss, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query2, equivQuery2, Row(1, 3) :: Nil)

      // Test 3: Join with complex plans i.e. statistics are not available
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      (sub-query) - s3
      //
      // Positional/default join reordering: d1, sq, f1, d2, s3
      // Star join reordering: empty
      val query3 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, (select d3_pk1 as pk, d3_fk1 as fk from d3 limit 2) sq, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = sq.pk
          | and sq.fk = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery3 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, (select d3_pk1 as pk, d3_fk1 as fk from d3 limit 2) sq, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = sq.pk
          | and sq.fk = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query3, equivQuery3, Row(1, 3) :: Nil)

      // Test 4: Comparable fact table sizes
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      f11 - s3
      //
      // Positional/default join reordering: d1, f11, f1, d2, s3
      // Star join reordering: empty
      val query4 = sql(
        """
          | select f1.f1_fk1, f1.f1_fk3
          | from d1, f11, f1, d2, s3
          | where f1.f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1.f1_fk1 = d1_pk1
          | and f1.f1_fk3 = f11.f1_c4
          | and f11.f1_fk1 = s3_pk1
          | order by f1.f1_fk1, f1.f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery4 = sql(
        """
          | select f1.f1_fk1, f1.f1_fk3
          | from d1, f11, f1, d2, s3
          | where f1.f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1.f1_fk1 = d1_pk1
          | and f1.f1_fk3 = f11.f1_c4
          | and f11.f1_fk1 = s3_pk1
          | order by f1.f1_fk1, f1.f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query4, equivQuery4, Row(1, 3) :: Nil)

      // Test 5: No RI joins
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      d3 - s3
      //
      // Positional/default join reordering: d1, d3, f1, d2, s3
      // Star join reordering: empty
      val query5 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_c4 and d2_c2 < 4
          | and f1_fk1 = d1_c4
          | and f1_fk3 = d3_c4
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery5 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_c4 and d2_c2 < 4
          | and f1_fk1 = d1_c4
          | and f1_fk3 = d3_c4
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query5, equivQuery5, Row(3, 2) :: Nil)

      // Test 6: Complex join predicates i.e. stats are not available
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      d3 - s3
      //
      // Positional/default join reordering: d1, d3, f1, d2, s3
      // Star join reordering: empty
      val query6 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and abs(f1_fk1) = abs(d1_pk1)
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery6 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and abs(f1_fk1) = abs(d1_pk1)
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query6, equivQuery6, Row(1, 3) :: Nil)

      // Test 7: Less than two dimensions
      // Star join:
      //   (<)  (=)
      // d1 - f1 - d2
      //      |(<)
      //      d3 - s3
      //
      // Positional/default join reordering: d1, d3, f1, d2, s3
      // Star join reordering: empty
      val query7 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 < d1_pk1
          | and f1_fk3 < d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery7 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_pk1 and d2_c2 < 2
          | and f1_fk1 < d1_pk1
          | and f1_fk3 < d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query7, equivQuery7, Row(1, 3) :: Nil)

      // Test 8: Expanding star join
      // Star join:
      //   (<)  (<)
      // d1 - f1 - d2
      //      | (<)
      //      d3 - s3
      //
      // Positional/default join reordering: d1, d3, f1, d2, s3
      // Star join reordering: empty
      val query8 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 < d2_pk1
          | and f1_fk1 < d1_pk1
          | and f1_fk3 < d3_pk1
          | and d3_fk1 < s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery8 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 < d2_pk1
          | and f1_fk1 < d1_pk1
          | and f1_fk3 < d3_pk1
          | and d3_fk1 < s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query8, equivQuery8, Row(1, 3) :: Nil)

      // Test 9: Non selective star join
      // Star join:
      //   (=)  (=)
      // d1 - f1 - d2
      //      | (=)
      //      d3 - s3
      //
      // Positional/default join reordering: d1, d3, f1, d2, s3
      // Star join reordering: empty
      val query9 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_pk1
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      // Equivalent query for comparison
      val equivQuery9 = sql(
        """
          | select f1_fk1, f1_fk3
          | from d1, d3, f1, d2, s3
          | where f1_fk2 = d2_pk1
          | and f1_fk1 = d1_pk1
          | and f1_fk3 = d3_pk1
          | and d3_fk1 = s3_pk1
          | order by f1_fk1, f1_fk3
          | limit 1
        """.stripMargin)

      verifyStarJoinPlans(query9, equivQuery9, Row(1, 3) :: Nil)
    }
  }

   test("SPARK-17791: Miscellaneous tests with star join reordering") {
     withTable("f1", "d1", "d2", "d3", "s3", "f11", "d3_ns", "d3_ss") {
       createTables()
       runStats()

       // Star join reordering with uncorrelated subquery predicates
       // on the fact table.
       // Star join: f1, d2, d1, d3, s3
       // Default join reordering: d1, f1, d2, d3, s3
       val query1 = sql(
         """
           | select f1_fk1, f1_fk3
           | from d1, d2, f1, d3, s3
           | where f1_fk2 = d2_pk1 and d2_c2 <= 2
           | and f1_fk1 = d1_pk1
           | and f1_c4 IN (select s3_c4 from s3 limit 2)
           | and f1_fk3 = d3_pk1
           | and d3_fk1 = s3_pk1
           | limit 1
         """.stripMargin)

       // Equivalent query
       // Default reordering: f1, d2, d1, d3, s3
       val equivQuery1 = sql(
         """
           | select f1_fk1, f1_fk3
           | from f1, d2, d1, d3, s3
           | where f1_fk2 = d2_pk1 and d2_c2 <= 2
           | and f1_fk1 = d1_pk1
           | and f1_c4 IN (select s3_c4 from s3 limit 2)
           | and f1_fk3 = d3_pk1
           | and d3_fk1 = s3_pk1
           | limit 1
         """.stripMargin)

       verifyStarJoinPlans(query1, equivQuery1, Row(1, 3) :: Nil)

       // Star join reordering with correlated subquery predicates
       // on the fact table.
       // Star join: f1, d2, d1, d3, s3
       // Default join reordering: d1, f1, d2, d3, s3
       val query2 = sql(
         """
           | select f1_fk1, f1_fk3
           | from d1, d2, f1, d3, s3
           | where f1_fk2 = d2_pk1 and d2_c2 <= 2
           | and f1_fk1 = d1_pk1
           | and f1_c4 IN (select s3_c4 from s3 where f1_fk3 = s3_c3)
           | and f1_fk3 = d3_pk1
           | and d3_fk1 = s3_pk1
           | limit 1
         """.stripMargin)

       // Equivalent query
       // Default reordering: f1, d2, d1, d3, s3
       val equivQuery2 = sql(
         """
           | select f1_fk1, f1_fk3
           | from f1, d2, d1, d3, s3
           | where f1_fk2 = d2_pk1 and d2_c2 <= 2
           | and f1_fk1 = d1_pk1
           | and f1_c4 IN (select s3_c4 from s3 where f1_fk3 = s3_c3)
           | and f1_fk3 = d3_pk1
           | and d3_fk1 = s3_pk1
           | limit 1
         """.stripMargin)

       verifyStarJoinPlans(query2, equivQuery2, Row(1, 3) :: Nil)
     }
  }
}
