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

package org.apache.spark.sql.fuzzing

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicInteger

import scala.reflect.runtime.{universe => ru}
import scala.util.Random
import scala.util.control.NonFatal

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.analysis.UnresolvedException
import org.apache.spark.sql.test.TestSQLContext
import org.apache.spark.sql.types._
import org.apache.spark.util.Utils

class RandomDataFrameGenerator(seed: Long, sqlContext: SQLContext) {

  private val rand = new Random(seed)
  private val nextId = new AtomicInteger()

  private def hasRandomDataGenerator(dataType: DataType): Boolean = {
    RandomDataGenerator.forType(dataType).isDefined
  }

  def randomChoice[T](values: Seq[T]): T = {
    values(rand.nextInt(values.length))
  }

  private val simpleTypes: Set[DataType] = {
    DataTypeTestUtils.atomicTypes
      .filter(hasRandomDataGenerator)
      // Ignore decimal type since it can lead to OOM (see SPARK-9303). TODO: It would be better to
      // only generate limited precision decimals instead.
      .filterNot(_.isInstanceOf[DecimalType])
  }

  private val arrayTypes: Set[DataType] = {
    DataTypeTestUtils.atomicArrayTypes
      .filter(hasRandomDataGenerator)
      // See above comment about DecimalType
      .filterNot(_.elementType.isInstanceOf[DecimalType]).toSet
  }

  private def randomStructField(
      allowComplexTypes: Boolean = false,
      allowSpacesInColumnName: Boolean = false): StructField = {
    val name = "c" + nextId.getAndIncrement + (if (allowSpacesInColumnName) " space" else "")
    val candidateTypes: Seq[DataType] = Seq(
      simpleTypes,
      arrayTypes.filter(_ => allowComplexTypes),
      // This does not allow complex types, limiting the depth of recursion:
      if (allowComplexTypes) {
        Set[DataType](randomStructType(numCols = rand.nextInt(2) + 1))
      } else {
        Set[DataType]()
      }
    ).flatten
    val dataType = randomChoice(candidateTypes)
    val nullable = rand.nextBoolean()
    StructField(name, dataType, nullable)
  }

  private def randomStructType(
      numCols: Int,
      allowComplexTypes: Boolean = false,
      allowSpacesInColumnNames: Boolean = false): StructType = {
    StructType(Array.fill(numCols)(randomStructField(allowComplexTypes, allowSpacesInColumnNames)))
  }

  def randomDataFrame(
      numCols: Int,
      numRows: Int,
      allowComplexTypes: Boolean = false,
      allowSpacesInColumnNames: Boolean = false): DataFrame = {
    val schema = randomStructType(numCols, allowComplexTypes, allowSpacesInColumnNames)
    val rows = sqlContext.sparkContext.parallelize(1 to numRows).mapPartitions { iter =>
      val rowGenerator = RandomDataGenerator.forType(schema, nullable = false, seed = Some(42)).get
      iter.map(_ => rowGenerator().asInstanceOf[Row])
    }
    sqlContext.createDataFrame(rows, schema)
  }

}


/**
 * This test suite generates random data frames, then applies random sequences of operations to
 * them in order to construct random queries. We don't have a source of truth for these random
 * queries but nevertheless they are still useful for testing that we don't crash in bad ways.
 */
class DataFrameFuzzingSuite extends SparkFunSuite {

  val tempDir = Utils.createTempDir()

  private val dataGenerator = new RandomDataFrameGenerator(123, TestSQLContext)

  def randomChoice[T](values: Seq[T]): T = {
    values(Random.nextInt(values.length))
  }


  val m = ru.runtimeMirror(this.getClass.getClassLoader)

  val whitelistedParameterTypes = Set(
    m.universe.typeOf[DataFrame],
    m.universe.typeOf[Seq[Column]],
    m.universe.typeOf[Column],
    m.universe.typeOf[String],
    m.universe.typeOf[Seq[String]]
  )

  val dataFrameTransformations = {
    val dfType = m.universe.typeOf[DataFrame]
    dfType.members
      .filter(_.isPublic)
      .filter(_.isMethod)
      .map(_.asMethod)
      .filter(_.returnType =:= dfType)
      .filterNot(_.isConstructor)
      .filter { m =>
        m.paramss.flatten.forall { p =>
          whitelistedParameterTypes.exists { t => p.typeSignature <:< t }
        }
      }
      .filterNot(_.name.toString == "drop") // since this can lead to a DataFrame with no columns
      .filterNot(_.name.toString == "describe") // since we cannot run all queries on describe output
      .filterNot(_.name.toString == "dropDuplicates")
      .toSeq
  }

  def getRandomColumnName(df: DataFrame): String = {
    randomChoice(df.columns.zip(df.schema).map { case (colName, field) =>
      field.dataType match {
        case StructType(fields) =>
           colName + "." + randomChoice(fields.map(_.name))
        case _ => colName
      }
    })
  }

  def applyRandomTransformationToDataFrame(df: DataFrame): DataFrame = {
    val method = randomChoice(dataFrameTransformations)
    val params = method.paramss.flatten // We don't use multiple parameter lists
    val paramTypes = params.map(_.typeSignature)
    val paramValues = paramTypes.map { t =>
      if (t =:= m.universe.typeOf[DataFrame]) {
        randomChoice(Seq(
          df,
          dataGenerator.randomDataFrame(numCols = Random.nextInt(4) + 1, numRows = 100)
        )) // ++ Try(applyRandomTransformationToDataFrame(df)).toOption.toSeq)
      } else if (t =:= m.universe.typeOf[Column]) {
        df.col(getRandomColumnName(df))
      } else if (t =:= m.universe.typeOf[String]) {
        getRandomColumnName(df)
      } else if (t <:< m.universe.typeOf[Seq[Column]]) {
        Seq.fill(Random.nextInt(2) + 1)(df.col(getRandomColumnName(df)))
      } else if (t <:< m.universe.typeOf[Seq[String]]) {
        Seq.fill(Random.nextInt(2) + 1)(getRandomColumnName(df))
      } else {
        sys.error("ERROR!")
      }
    }
    val reflectedMethod: ru.MethodMirror = m.reflect(df).reflectMethod(method)
    println("Applying method " + method + " with values " + paramValues)
    try {
      reflectedMethod.apply(paramValues: _*).asInstanceOf[DataFrame]
    } catch {
      case e: InvocationTargetException =>
        throw e.getCause
    }
  }

  //TestSQLContext.conf.setConf(SQLConf.DATAFRAME_RETAIN_GROUP_COLUMNS, false)
//  TestSQLContext.conf.setConf(SQLConf.UNSAFE_ENABLED, true)
  TestSQLContext.conf.setConf(SQLConf.SORTMERGE_JOIN, true)
  TestSQLContext.conf.setConf(SQLConf.CODEGEN_ENABLED, true)

  TestSQLContext.conf.setConf(SQLConf.SHUFFLE_PARTITIONS, 10)


  for (_ <- 1 to 10000) {
    println("-" * 80)
    try {
      val df = dataGenerator.randomDataFrame(numCols = Random.nextInt(4) + 1, numRows = 20)
      val df2 = applyRandomTransformationToDataFrame(applyRandomTransformationToDataFrame(df))
      try {
        df2.collectAsList()
      } catch {
        case NonFatal(e) =>
          println(df2.queryExecution)
          println(df)
          println(df.collectAsList())
          throw new Exception(e)
      }
    } catch {
      case e: UnresolvedException[_] =>
        println("skipped due to unresolved")
      case e: AnalysisException =>
        println("Skipped")
      case e: IllegalArgumentException if e.getMessage.contains("number of columns doesn't match") =>
      case e: IllegalArgumentException if e.getMessage.contains("Unsupported join type") =>


      //      case e: IllegalArgumentException =>
//        println(e)
//        println("Skipped due to IOE")
    }
  }

}
