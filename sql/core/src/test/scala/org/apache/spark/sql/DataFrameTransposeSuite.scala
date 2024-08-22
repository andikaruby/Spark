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

import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.types._

class DataFrameTransposeSuite extends QueryTest with SharedSparkSession {
  import testImplicits._

  // scalastyle:off println
  test("transpose") {
    checkAnswer(
      salary.transpose(),
      Row("salary", 2000.0, 1000.0) :: Nil
    )
  }

  test("transpose with index column specified") {
    checkAnswer(
      salary.transpose(Some($"salary")),
      Row("personId", 1, 0) :: Nil
    )
  }

  test("transpose frame with repeated first column values") {
    val df = Seq(("test1", "1"), ("test1", "2")).toDF("s", "id")
    print(df.transpose().show())
    //    checkAnswer(
    //      df.transpose(),
    //      Row("id", 1, 2) :: Nil
    //    )
  }


  test("transpose empty frame that has column names") {
    val schema = StructType(Seq(
         StructField("id", IntegerType, nullable = true),
         StructField("name", StringType, nullable = true)
    ))
    val emptyDF = spark.createDataFrame(spark.sparkContext.emptyRDD[Row], schema)
    println(emptyDF.transpose().show())
    //    checkAnswer(
    //      repeatedDf.transpose(),
    //      Row("id", 1) :: Nil
    //    )
  }

  test("transpose empty frame that has no column names") {
    val emptyDF = spark.emptyDataFrame
    println(emptyDF.transpose().show())
    //    checkAnswer(
    //      repeatedDf.transpose(),
    //      Row("id", 1) :: Nil
    //    )
  }

  test("transpose frame with columns of mismatch types") {
    val exception = intercept[IllegalArgumentException] {
      person.transpose()
    }
    assert(exception.getMessage.contains("No common type found"))
  }

  test("transpose - correct column ordering") {
    val df = Seq(("test2", "1"), ("test1", "2")).toDF("s", "id")
    print(df.transpose().show())
    //    checkAnswer(
    //      df.transpose(),
    //      Row("id", 2, 1) :: Nil
    //    )
  }
}
