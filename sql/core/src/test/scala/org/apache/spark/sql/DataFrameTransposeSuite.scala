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

package org.apache.spark.sql

import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types._

class DataFrameTransposeSuite extends QueryTest with SharedSparkSession {
  import testImplicits._

  //
  // Test cases: input parameter
  //

  test("transpose with default index column") {
    checkAnswer(
      salary.transpose(),
      Row("salary", 2000.0, 1000.0) :: Nil
    )
  }

  test("transpose with specified index column") {
    checkAnswer(
      salary.transpose($"salary"),
      Row("personId", 1, 0) :: Nil
    )
  }

  //
  // Test cases: API behavior
  //

  test("enforce least common type for non-index columns") {
    val df = Seq(("x", 1, 10.0), ("y", 2, 20.0)).toDF("name", "id", "value")
    val transposedDf = df.transpose()
    checkAnswer(
      transposedDf,
      Row("id", 1.0, 2.0) :: Row("value", 10.0, 20.0) :: Nil
    )
    // (id,IntegerType) -> (x,DoubleType)
    // (value,DoubleType) -> (y,DoubleType)
    assertResult(DoubleType)(transposedDf.schema("x").dataType)
    assertResult(DoubleType)(transposedDf.schema("y").dataType)

    val exception = intercept[IllegalArgumentException] {
      person.transpose()
    }
    assert(exception.getMessage.contains("No common type found"))
  }

  test("enforce ascending order based on index column values for transposed columns") {
    val transposedDf = person.transpose($"name")
    checkAnswer(
      transposedDf,
      Row("id", 1, 0) :: Row("age", 20, 30)  :: Nil
    )
    // mike, jim -> jim, mike
    assertResult(Array("key", "jim", "mike"))(transposedDf.columns)
  }

  test("enforce AtomicType Attribute for index column values") {
    val exceptionAtomic = intercept[IllegalArgumentException] {
      complexData.transpose($"m")  // (m,MapType(StringType,IntegerType,false))
    }
    assert(exceptionAtomic.getMessage.contains("Index column must be of atomic type, but found"))

    val exceptionAttribute = intercept[IllegalArgumentException] {
      // (s,StructType(StructField(key,IntegerType,false),StructField(value,StringType,true)))
      complexData.transpose($"s.key")
    }
    assert(exceptionAttribute.getMessage.contains("Index column must be an atomic attribute"))
  }

  test("enforce transpose max values") {
    spark.conf.set(SQLConf.DATAFRAME_TRANSPOSE_MAX_VALUES.key, 1)
    intercept[IllegalArgumentException](
      person.transpose($"name")
    )
    spark.conf.set(SQLConf.DATAFRAME_TRANSPOSE_MAX_VALUES.key,
      SQLConf.DATAFRAME_TRANSPOSE_MAX_VALUES.defaultValue.get)
  }

  //
  // Test cases: special frame
  //

  test("transpose empty frame w. column names") {
    val schema = StructType(Seq(
      StructField("id", IntegerType),
      StructField("name", StringType)
    ))
    val emptyDF = spark.createDataFrame(spark.sparkContext.emptyRDD[Row], schema)
    checkAnswer(
      emptyDF.transpose(),
      Row("name") :: Nil
    )
  }

  test("transpose empty frame w/o column names") {
    val emptyDF = spark.emptyDataFrame
    checkAnswer(
      emptyDF.transpose(),
      Nil
    )
  }

  test("transpose frame with only index column") {
    val transposedDf = tableName.transpose()
    checkAnswer(
      transposedDf,
      Nil
    )
    assertResult(Array("key", "test"))(transposedDf.columns)
  }
}
