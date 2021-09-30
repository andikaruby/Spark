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
package org.apache.spark.sql.execution.datasources.v2.jdbc

import java.sql.{Connection, DriverManager, SQLFeatureNotSupportedException}
import java.util.Properties

import org.apache.log4j.Level

import org.apache.spark.SparkConf
import org.apache.spark.sql.{AnalysisException, QueryTest, Row}
import org.apache.spark.sql.catalyst.analysis.{NoSuchNamespaceException, TableAlreadyExistsException}
import org.apache.spark.sql.catalyst.parser.ParseException
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types._
import org.apache.spark.util.Utils

class JDBCTableCatalogSuite extends QueryTest with SharedSparkSession {

  val tempDir = Utils.createTempDir()
  val url = s"jdbc:h2:${tempDir.getCanonicalPath};user=testUser;password=testPass"
  val defaultMetadata = new MetadataBuilder().putLong("scale", 0).build()
  var conn: java.sql.Connection = null

  override def sparkConf: SparkConf = super.sparkConf
    .set("spark.sql.catalog.h2", classOf[JDBCTableCatalog].getName)
    .set("spark.sql.catalog.h2.url", url)
    .set("spark.sql.catalog.h2.driver", "org.h2.Driver")

  private def withConnection[T](f: Connection => T): T = {
    val conn = DriverManager.getConnection(url, new Properties())
    try {
      f(conn)
    } finally {
      conn.close()
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Utils.classForName("org.h2.Driver")
    withConnection { conn =>
      conn.prepareStatement("""CREATE SCHEMA "test"""").executeUpdate()
      conn.prepareStatement(
        """CREATE TABLE "test"."people" (name TEXT(32) NOT NULL, id INTEGER NOT NULL)""")
        .executeUpdate()
    }
  }

  override def afterAll(): Unit = {
    Utils.deleteRecursively(tempDir)
    super.afterAll()
  }

  test("show tables") {
    checkAnswer(sql("SHOW TABLES IN h2.test"), Seq(Row("test", "people", false)))
    // Check not existing namespace
    checkAnswer(sql("SHOW TABLES IN h2.bad_test"), Seq())
  }

  test("drop a table and test whether the table exists") {
    withConnection { conn =>
      conn.prepareStatement("""CREATE TABLE "test"."to_drop" (id INTEGER)""").executeUpdate()
    }
    checkAnswer(sql("SHOW TABLES IN h2.test"),
      Seq(Row("test", "to_drop", false), Row("test", "people", false)))
    sql("DROP TABLE h2.test.to_drop")
    checkAnswer(sql("SHOW TABLES IN h2.test"), Seq(Row("test", "people", false)))
    Seq(
      "h2.test.not_existing_table" ->
        "Table or view not found: h2.test.not_existing_table",
      "h2.bad_test.not_existing_table" ->
        "Table or view not found: h2.bad_test.not_existing_table"
    ).foreach { case (table, expectedMsg) =>
      val msg = intercept[AnalysisException] {
        sql(s"DROP TABLE $table")
      }.getMessage
      assert(msg.contains(expectedMsg))
    }
  }

  test("rename a table") {
    withTable("h2.test.dst_table") {
      withConnection { conn =>
        conn.prepareStatement("""CREATE TABLE "test"."src_table" (id INTEGER)""").executeUpdate()
      }
      checkAnswer(
        sql("SHOW TABLES IN h2.test"),
        Seq(Row("test", "src_table", false), Row("test", "people", false)))
      sql("ALTER TABLE h2.test.src_table RENAME TO test.dst_table")
      checkAnswer(
        sql("SHOW TABLES IN h2.test"),
        Seq(Row("test", "dst_table", false), Row("test", "people", false)))
    }
    // Rename not existing table or namespace
    val exp1 = intercept[AnalysisException] {
      sql("ALTER TABLE h2.test.not_existing_table RENAME TO test.dst_table")
    }
    assert(exp1.getMessage.contains(
      "Table or view not found: h2.test.not_existing_table"))
    val exp2 = intercept[AnalysisException] {
      sql("ALTER TABLE h2.bad_test.not_existing_table RENAME TO test.dst_table")
    }
    assert(exp2.getMessage.contains(
      "Table or view not found: h2.bad_test.not_existing_table"))
    // Rename to an existing table
    withTable("h2.test.dst_table") {
      withConnection { conn =>
        conn.prepareStatement("""CREATE TABLE "test"."dst_table" (id INTEGER)""").executeUpdate()
      }
      withTable("h2.test.src_table") {
        withConnection { conn =>
          conn.prepareStatement("""CREATE TABLE "test"."src_table" (id INTEGER)""").executeUpdate()
        }
        val exp = intercept[TableAlreadyExistsException] {
          sql("ALTER TABLE h2.test.src_table RENAME TO test.dst_table")
        }
        assert(exp.getMessage.contains(
          "Failed table renaming from test.src_table to test.dst_table"))
        assert(exp.cause.get.getMessage.contains("Table \"dst_table\" already exists"))
      }
    }
  }

  test("load a table") {
    val t = spark.table("h2.test.people")
    val expectedSchema = new StructType()
      .add("NAME", StringType, true, defaultMetadata)
      .add("ID", IntegerType, true, defaultMetadata)
    assert(t.schema === expectedSchema)
    Seq("h2.test.not_existing_table", "h2.bad_test.not_existing_table").foreach { table =>
      val msg = intercept[AnalysisException] {
        spark.table(table).schema
      }.getMessage
      assert(msg.contains("Table or view not found"))
    }
  }

  test("create a table") {
    withTable("h2.test.new_table") {
      sql("CREATE TABLE h2.test.new_table(i INT, j STRING)")
      checkAnswer(
        sql("SHOW TABLES IN h2.test"),
        Seq(Row("test", "people", false), Row("test", "new_table", false)))
    }
    withTable("h2.test.new_table") {
      sql("CREATE TABLE h2.test.new_table(i INT, j STRING)")
      val msg = intercept[AnalysisException] {
        sql("CREATE TABLE h2.test.new_table(i INT, j STRING)")
      }.getMessage
      assert(msg.contains("Table test.new_table already exists"))
    }
    val exp = intercept[NoSuchNamespaceException] {
      sql("CREATE TABLE h2.bad_test.new_table(i INT, j STRING)")
    }
    assert(exp.getMessage.contains("Failed table creation: bad_test.new_table"))
    assert(exp.cause.get.getMessage.contains("Schema \"bad_test\" not found"))
  }

  test("ALTER TABLE ... add column") {
    val tableName = "h2.test.alt_table"
    withTable(tableName) {
      sql(s"CREATE TABLE $tableName (ID INTEGER)")
      sql(s"ALTER TABLE $tableName ADD COLUMNS (C1 INTEGER, C2 STRING)")
      var t = spark.table(tableName)
      var expectedSchema = new StructType()
        .add("ID", IntegerType, true, defaultMetadata)
        .add("C1", IntegerType, true, defaultMetadata)
        .add("C2", StringType, true, defaultMetadata)
      assert(t.schema === expectedSchema)
      sql(s"ALTER TABLE $tableName ADD COLUMNS (c3 DOUBLE)")
      t = spark.table(tableName)
      expectedSchema = expectedSchema.add("c3", DoubleType, true, defaultMetadata)
      assert(t.schema === expectedSchema)
      // Add already existing column
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $tableName ADD COLUMNS (c3 DOUBLE)")
      }.getMessage
      assert(msg.contains("Cannot add column, because c3 already exists"))
    }
    // Add a column to not existing table and namespace
    Seq("h2.test.not_existing_table", "h2.bad_test.not_existing_table").foreach { table =>
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $table ADD COLUMNS (C4 STRING)")
      }.getMessage
      assert(msg.contains("Table not found"))
    }
  }

  test("ALTER TABLE ... rename column") {
    val tableName = "h2.test.alt_table"
    withTable(tableName) {
      sql(s"CREATE TABLE $tableName (id INTEGER, C0 INTEGER)")
      sql(s"ALTER TABLE $tableName RENAME COLUMN id TO C")
      val t = spark.table(tableName)
      val expectedSchema = new StructType()
        .add("C", IntegerType, true, defaultMetadata)
        .add("C0", IntegerType, true, defaultMetadata)
      assert(t.schema === expectedSchema)
      // Rename to already existing column
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $tableName RENAME COLUMN C TO C0")
      }.getMessage
      assert(msg.contains("Cannot rename column, because C0 already exists"))
    }
    // Rename a column in not existing table and namespace
    Seq("h2.test.not_existing_table", "h2.bad_test.not_existing_table").foreach { table =>
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $table RENAME COLUMN ID TO C")
      }.getMessage
      assert(msg.contains("Table not found"))
    }
  }

  test("ALTER TABLE ... drop column") {
    val tableName = "h2.test.alt_table"
    withTable(tableName) {
      sql(s"CREATE TABLE $tableName (C1 INTEGER, C2 INTEGER, c3 INTEGER)")
      sql(s"ALTER TABLE $tableName DROP COLUMN C1")
      sql(s"ALTER TABLE $tableName DROP COLUMN c3")
      val t = spark.table(tableName)
      val expectedSchema = new StructType().add("C2", IntegerType, true, defaultMetadata)
      assert(t.schema === expectedSchema)
      // Drop not existing column
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $tableName DROP COLUMN bad_column")
      }.getMessage
      assert(msg.contains("Missing field bad_column in table h2.test.alt_table"))
    }
    // Drop a column to not existing table and namespace
    Seq("h2.test.not_existing_table", "h2.bad_test.not_existing_table").foreach { table =>
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $table DROP COLUMN C1")
      }.getMessage
      assert(msg.contains("Table not found"))
    }
  }

  test("ALTER TABLE ... update column type") {
    val tableName = "h2.test.alt_table"
    withTable(tableName) {
      sql(s"CREATE TABLE $tableName (ID INTEGER, deptno INTEGER)")
      sql(s"ALTER TABLE $tableName ALTER COLUMN id TYPE DOUBLE")
      sql(s"ALTER TABLE $tableName ALTER COLUMN deptno TYPE DOUBLE")
      val t = spark.table(tableName)
      val expectedSchema = new StructType()
        .add("ID", DoubleType, true, defaultMetadata)
        .add("deptno", DoubleType, true, defaultMetadata)
      assert(t.schema === expectedSchema)
      // Update not existing column
      val msg1 = intercept[AnalysisException] {
        sql(s"ALTER TABLE $tableName ALTER COLUMN bad_column TYPE DOUBLE")
      }.getMessage
      assert(msg1.contains("Missing field bad_column in table h2.test.alt_table"))
      // Update column to wrong type
      val msg2 = intercept[ParseException] {
        sql(s"ALTER TABLE $tableName ALTER COLUMN id TYPE bad_type")
      }.getMessage
      assert(msg2.contains("DataType bad_type is not supported"))
    }
    // Update column type in not existing table and namespace
    Seq("h2.test.not_existing_table", "h2.bad_test.not_existing_table").foreach { table =>
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $table ALTER COLUMN id TYPE DOUBLE")
      }.getMessage
      assert(msg.contains("Table not found"))
    }
  }

  test("ALTER TABLE ... update column nullability") {
    val tableName = "h2.test.alt_table"
    withTable(tableName) {
      sql(s"CREATE TABLE $tableName (ID INTEGER NOT NULL, deptno INTEGER NOT NULL)")
      sql(s"ALTER TABLE $tableName ALTER COLUMN ID DROP NOT NULL")
      sql(s"ALTER TABLE $tableName ALTER COLUMN deptno DROP NOT NULL")
      val t = spark.table(tableName)
      val expectedSchema = new StructType()
        .add("ID", IntegerType, true, defaultMetadata)
        .add("deptno", IntegerType, true, defaultMetadata)
      assert(t.schema === expectedSchema)
      // Update nullability of not existing column
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $tableName ALTER COLUMN bad_column DROP NOT NULL")
      }.getMessage
      assert(msg.contains("Missing field bad_column in table h2.test.alt_table"))
    }
    // Update column nullability in not existing table and namespace
    Seq("h2.test.not_existing_table", "h2.bad_test.not_existing_table").foreach { table =>
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $table ALTER COLUMN ID DROP NOT NULL")
      }.getMessage
      assert(msg.contains("Table not found"))
    }
  }

  test("ALTER TABLE ... update column comment not supported") {
    val tableName = "h2.test.alt_table"
    withTable(tableName) {
      sql(s"CREATE TABLE $tableName (ID INTEGER)")
      val exp = intercept[AnalysisException] {
        sql(s"ALTER TABLE $tableName ALTER COLUMN ID COMMENT 'test'")
      }
      assert(exp.getMessage.contains("Failed table altering: test.alt_table"))
      assert(exp.cause.get.getMessage.contains("Unsupported TableChange"))
      // Update comment for not existing column
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $tableName ALTER COLUMN bad_column COMMENT 'test'")
      }.getMessage
      assert(msg.contains("Missing field bad_column in table h2.test.alt_table"))
    }
    // Update column comments in not existing table and namespace
    Seq("h2.test.not_existing_table", "h2.bad_test.not_existing_table").foreach { table =>
      val msg = intercept[AnalysisException] {
        sql(s"ALTER TABLE $table ALTER COLUMN ID COMMENT 'test'")
      }.getMessage
      assert(msg.contains("Table not found"))
    }
  }

  test("ALTER TABLE case sensitivity") {
    val tableName = "h2.test.alt_table"
    withTable(tableName) {
      sql(s"CREATE TABLE $tableName (c1 INTEGER NOT NULL, c2 INTEGER)")
      var t = spark.table(tableName)
      var expectedSchema = new StructType()
        .add("c1", IntegerType, true, defaultMetadata)
        .add("c2", IntegerType, true, defaultMetadata)
      assert(t.schema === expectedSchema)

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "true") {
        val msg = intercept[AnalysisException] {
          sql(s"ALTER TABLE $tableName RENAME COLUMN C2 TO c3")
        }.getMessage
        assert(msg.contains("Missing field C2 in table h2.test.alt_table"))
      }

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "false") {
        sql(s"ALTER TABLE $tableName RENAME COLUMN C2 TO c3")
        expectedSchema = new StructType()
          .add("c1", IntegerType, true, defaultMetadata)
          .add("c3", IntegerType, true, defaultMetadata)
        t = spark.table(tableName)
        assert(t.schema === expectedSchema)
      }

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "true") {
        val msg = intercept[AnalysisException] {
          sql(s"ALTER TABLE $tableName DROP COLUMN C3")
        }.getMessage
        assert(msg.contains("Missing field C3 in table h2.test.alt_table"))
      }

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "false") {
        sql(s"ALTER TABLE $tableName DROP COLUMN C3")
        expectedSchema = new StructType().add("c1", IntegerType, true, defaultMetadata)
        t = spark.table(tableName)
        assert(t.schema === expectedSchema)
      }

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "true") {
        val msg = intercept[AnalysisException] {
          sql(s"ALTER TABLE $tableName ALTER COLUMN C1 TYPE DOUBLE")
        }.getMessage
        assert(msg.contains("Missing field C1 in table h2.test.alt_table"))
      }

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "false") {
        sql(s"ALTER TABLE $tableName ALTER COLUMN C1 TYPE DOUBLE")
        expectedSchema = new StructType().add("c1", DoubleType, true, defaultMetadata)
        t = spark.table(tableName)
        assert(t.schema === expectedSchema)
      }

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "true") {
        val msg = intercept[AnalysisException] {
          sql(s"ALTER TABLE $tableName ALTER COLUMN C1 DROP NOT NULL")
        }.getMessage
        assert(msg.contains("Missing field C1 in table h2.test.alt_table"))
      }

      withSQLConf(SQLConf.CASE_SENSITIVE.key -> "false") {
        sql(s"ALTER TABLE $tableName ALTER COLUMN C1 DROP NOT NULL")
        expectedSchema = new StructType().add("c1", DoubleType, true, defaultMetadata)
        t = spark.table(tableName)
        assert(t.schema === expectedSchema)
      }
    }
  }

  test("CREATE TABLE with table comment") {
    withTable("h2.test.new_table") {
      val logAppender = new LogAppender("table comment")
      withLogAppender(logAppender) {
        sql("CREATE TABLE h2.test.new_table(i INT, j STRING) COMMENT 'this is a comment'")
      }
      val createCommentWarning = logAppender.loggingEvents
        .filter(_.getLevel == Level.WARN)
        .map(_.getRenderedMessage)
        .exists(_.contains("Cannot create JDBC table comment"))
      assert(createCommentWarning === false)
    }
  }

  test("CREATE TABLE with table property") {
    withTable("h2.test.new_table") {
      val m = intercept[AnalysisException] {
        sql("CREATE TABLE h2.test.new_table(i INT, j STRING)" +
          " TBLPROPERTIES('ENGINE'='tableEngineName')")
      }.cause.get.getMessage
      assert(m.contains("\"TABLEENGINENAME\" not found"))
    }
  }

  test("CREATE INDEX") {
    withTable("h2.test.new_table") {
      sql("CREATE TABLE h2.test.new_table(col1 INT, col2 STRING)")
      val e1 = intercept[SQLFeatureNotSupportedException] {
        sql("CREATE index i1 ON h2.test.new_table (col1)")
      }.getMessage
      assert(e1.contains("IndexName i1"))
      assert(e1.contains("indexType "))
      assert(e1.contains("columns ArrayBuffer(col1)"))

      val e2 = intercept[SQLFeatureNotSupportedException] {
        sql("CREATE index i1 ON h2.test.new_table" +
          " (col1 OPTIONS (fpp=0.1, numItems=10000000)," +
          " col2 OPTIONS (fpp=0.2, numItems=20000000)) OPTIONS (fpp=0.3, numItems=30000000) ")
      }.getMessage
      assert(e2.contains("IndexName i1"))
      assert(e2.contains("columns ArrayBuffer(col1, col2)"))
      assert(e2.contains("columnProperties ArrayBuffer(" +
        "Map(fpp -> 0.1, numItems -> 10000000), Map(fpp -> 0.2, numItems -> 20000000))"))
      assert(e2.contains("properties Map(fpp -> 0.3, numItems -> 30000000)"))
    }
  }
}
