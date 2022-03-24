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

package org.apache.spark.sql.connector.expressions

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow
import org.apache.spark.sql.catalyst.parser.CatalystSqlParser
import org.apache.spark.sql.types.{BooleanType, IntegerType, MetadataBuilder, StructField, StructType}

class AssignDefaultValuesIteratorSuite extends SparkFunSuite {
  private val parser = new CatalystSqlParser()
  private val dataSourceType = "testDataSource"
  private val existsDefault = "EXISTS_DEFAULT"
  private val parseFailure = "fails to parse as a valid literal value"
  private val singleNullRow = new GenericInternalRow(Array[Any](null))
  private val singleIntRow = new GenericInternalRow(Array[Any](42))
  private val intBooleanRow = new GenericInternalRow(Array[Any](42, true))
  private val intBooleanRowOther = new GenericInternalRow(Array[Any](41, false))
  private val singleExistsRow = new GenericInternalRow(Array[Any](true))
  private val singleDoesNotExistsRow = new GenericInternalRow(Array[Any](false))
  private val existsAndDoesNotExistRow = new GenericInternalRow(Array[Any](true, false))
  private val doesNotExistAndExistsRow = new GenericInternalRow(Array[Any](false, true))
  private val singleDoesNotExistRow = new GenericInternalRow(Array[Any](false))
  private val metadataDefaultInt = new MetadataBuilder().putString(existsDefault, "43").build()
  private val metadataDefaultBoolean =
    new MetadataBuilder().putString(existsDefault, "false").build()
  private val metadataDefaultBadToken =
    new MetadataBuilder().putString(existsDefault, "bad").build()
  private val metadataDefaultNonLiteral =
    new MetadataBuilder().putString(existsDefault, "1 + 1").build()

  // This is a helper method to extract GenericInternalRows from the iterator under test.
  private def getNext(it: AssignDefaultValuesIterator): GenericInternalRow =
    it.next.asInstanceOf[GenericInternalRow]

  // This is a helper method to append the values from one row to another.
  private def combine(lhs: GenericInternalRow, rhs: GenericInternalRow): GenericInternalRow = {
    val result = new GenericInternalRow(lhs.numFields + rhs.numFields)
    for (i <- 0 until lhs.numFields) result.update(i, lhs.values(i))
    for (i <- 0 until rhs.numFields) result.update(i + lhs.numFields, rhs.values(i))
    result
  }

  test("Default value not substituted because data source field exists") {
    val dataSchema = StructType(Array(StructField("col", IntegerType, true, metadataDefaultInt)))
    val it = SeparateBooleanExistenceColumnsIterator(
      Iterator(combine(singleIntRow, singleExistsRow)), dataSchema, parser, dataSourceType)
    assert(it.hasNext)
    val result: GenericInternalRow = getNext(it)
    assert(result.numFields == 1)
    assert(result.getInt(0) == 42)
    assert(!it.hasNext)
  }

  test("Default assigned because input field does not exist, swapping NULLs with defaults") {
    val dataSchema = StructType(Array(StructField("col", IntegerType, true, metadataDefaultInt)))
    val it = NullsAsDefaultsIterator(Iterator(singleNullRow), dataSchema, parser, dataSourceType)
    assert(it.hasNext)
    val result: GenericInternalRow = getNext(it)
    assert(result.numFields == 1)
    assert(result.getInt(0) == 43)
    assert(!it.hasNext)
  }

  test("Default assigned because input field does not exist, separate existence column") {
    val dataSchema = StructType(Array(StructField("col", IntegerType, true, metadataDefaultInt)))
    val it = SeparateBooleanExistenceColumnsIterator(
      Iterator(combine(singleIntRow, singleDoesNotExistRow)), dataSchema, parser, dataSourceType)
    assert(it.hasNext)
    val result: GenericInternalRow = getNext(it)
    assert(result.numFields == 1)
    assert(result.getInt(0) == 43)
    assert(!it.hasNext)
  }

  test("Default value substituted in second column") {
    val dataSchema = StructType(Array(
      StructField("col", IntegerType, true, metadataDefaultInt),
      StructField("other", BooleanType, true, metadataDefaultBoolean)))
    val it = SeparateBooleanExistenceColumnsIterator(
      Iterator(combine(intBooleanRow, existsAndDoesNotExistRow)),
      dataSchema,
      parser,
      dataSourceType)
    assert(it.hasNext)
    val result: GenericInternalRow = getNext(it)
    assert(result.numFields == 2)
    assert(result.getInt(0) == 42)
    assert(result.getBoolean(1) == false)
    assert(!it.hasNext)
  }

  test("Default value substituted in multiple rows") {
    val dataSchema = StructType(Array(
      StructField("col", IntegerType, true, metadataDefaultInt),
      StructField("other", BooleanType, true, metadataDefaultBoolean)))
    val it = SeparateBooleanExistenceColumnsIterator(
      Iterator(
        combine(intBooleanRow, existsAndDoesNotExistRow),
        combine(intBooleanRowOther, doesNotExistAndExistsRow)),
      dataSchema,
      parser,
      dataSourceType)
    assert(it.hasNext)
    val result: GenericInternalRow = getNext(it)
    assert(result.numFields == 2)
    assert(result.getInt(0) == 42)
    assert(result.getBoolean(1) == false)
    val other: GenericInternalRow = getNext(it)
    assert(other.numFields == 2)
    assert(other.getInt(0) == 43)
    assert(result.getBoolean(1) == false)
    assert(!it.hasNext)
  }

  test("Default value throws exception because it is unparsable") {
    val dataSchema =
      StructType(Array(StructField("col", IntegerType, true, metadataDefaultBadToken)))
    val it = SeparateBooleanExistenceColumnsIterator(
      Iterator(combine(singleIntRow, singleDoesNotExistsRow)), dataSchema, parser, dataSourceType)
    assert(intercept[AnalysisException] {
      it.next
    }.getMessage.contains(parseFailure))
  }

  test("Default value throws exception because it parses as a non-literal expression") {
    val dataSchema =
      StructType(Array(StructField("col", IntegerType, true, metadataDefaultNonLiteral)))
    val it = SeparateBooleanExistenceColumnsIterator(
      Iterator(combine(singleIntRow, singleDoesNotExistsRow)), dataSchema, parser, dataSourceType)
    assert(intercept[AnalysisException] {
      it.next
    }.getMessage.contains(parseFailure))
  }
}
