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

package org.apache.spark.sql.json

import scala.collection.mutable.HashSet

import org.apache.spark.sql.catalyst.analysis.Star
import org.apache.spark.sql.catalyst.expressions.{Attribute, AttributeReference}
import org.apache.spark.sql.catalyst.plans.logical.LeafNode
import org.apache.spark.sql.catalyst.types._
import org.apache.spark.sql.execution.{ExistingRdd, SparkLogicalPlan}
import org.apache.spark.sql.json.JsonTable.enforceCorrectType
import org.apache.spark.sql.QueryTest
import org.apache.spark.sql.test.TestSQLContext
import org.apache.spark.sql.test.TestSQLContext._

protected case class Schema(output: Seq[Attribute]) extends LeafNode

class JsonSuite extends QueryTest {
  import TestJsonData._
  TestJsonData

  test("Primitive field and type inferring") {
    val jsonSchemaRDD = jsonRDD(primitiveFieldAndType)

    val expectedSchema =
      AttributeReference("bigInteger", DecimalType, true)() ::
      AttributeReference("boolean", BooleanType, true)() ::
      AttributeReference("double", DoubleType, true)() ::
      AttributeReference("integer", IntegerType, true)() ::
      AttributeReference("long", LongType, true)() ::
      AttributeReference("null", StringType, true)() ::
      AttributeReference("string", StringType, true)() :: Nil

    comparePlans(Schema(expectedSchema), Schema(jsonSchemaRDD.logicalPlan.output))

    jsonSchemaRDD.registerAsTable("jsonTable")

    checkAnswer(
      sql("select * from jsonTable"),
      (BigDecimal("92233720368547758070"),
      true,
      1.7976931348623157E308,
      10,
      21474836470L,
      null,
      "this is a simple string.") :: Nil
    )
  }

  test("Complex field and type inferring") {
    val jsonSchemaRDD = jsonRDD(complexFieldAndType)

    val expectedSchema =
      AttributeReference("arrayOfArray1", ArrayType(ArrayType(StringType)), true)() ::
      AttributeReference("arrayOfArray2", ArrayType(ArrayType(DoubleType)), true)() ::
      AttributeReference("arrayOfBigInteger", ArrayType(DecimalType), true)() ::
      AttributeReference("arrayOfBoolean", ArrayType(BooleanType), true)() ::
      AttributeReference("arrayOfDouble", ArrayType(DoubleType), true)() ::
      AttributeReference("arrayOfInteger", ArrayType(IntegerType), true)() ::
      AttributeReference("arrayOfLong", ArrayType(LongType), true)() ::
      AttributeReference("arrayOfNull", ArrayType(StringType), true)() ::
      AttributeReference("arrayOfString", ArrayType(StringType), true)() ::
      AttributeReference("arrayOfStruct", ArrayType(
        StructType(StructField("field1", BooleanType, true) ::
                   StructField("field2", StringType, true) :: Nil)), true)() ::
      AttributeReference("struct", StructType(
        StructField("field1", BooleanType, true) ::
        StructField("field2", DecimalType, true) :: Nil), true)() ::
      AttributeReference("structWithArrayFields", StructType(
        StructField("field1", ArrayType(IntegerType), true) ::
        StructField("field2", ArrayType(StringType), true) :: Nil), true)() :: Nil

    comparePlans(Schema(expectedSchema), Schema(jsonSchemaRDD.logicalPlan.output))

    jsonSchemaRDD.registerAsTable("jsonTable")

    // Access elements of a primitive array.
    checkAnswer(
      sql("select arrayOfString[0], arrayOfString[1], arrayOfString[2] from jsonTable"),
      ("str1", "str2", null) :: Nil
    )

    // Access an array of null values.
    checkAnswer(
      sql("select arrayOfNull from jsonTable"),
      Seq(Seq(null, null, null, null)) :: Nil
    )

    // Access elements of a BigInteger array (we use DecimalType internally).
    checkAnswer(
      sql("select arrayOfBigInteger[0], arrayOfBigInteger[1], arrayOfBigInteger[2] from jsonTable"),
      (BigDecimal("922337203685477580700"), BigDecimal("-922337203685477580800"), null) :: Nil
    )

    // Access elements of an array of arrays.
    checkAnswer(
      sql("select arrayOfArray1[0], arrayOfArray1[1] from jsonTable"),
      (Seq("1", "2", "3"), Seq("str1", "str2")) :: Nil
    )

    // Access elements of an array of arrays.
    checkAnswer(
      sql("select arrayOfArray2[0], arrayOfArray2[1] from jsonTable"),
      (Seq(1.0, 2.0, 3.0), Seq(1.1, 2.1, 3.1)) :: Nil
    )

    // Access elements of an array inside a filed with the type of ArrayType(ArrayType).
    checkAnswer(
      sql("select arrayOfArray1[1][1], arrayOfArray2[1][1] from jsonTable"),
      ("str2", 2.1) :: Nil
    )

    // Access elements of an array of structs.
    checkAnswer(
      sql("select arrayOfStruct[0], arrayOfStruct[1], arrayOfStruct[2] from jsonTable"),
      (true :: "str1" :: Nil, false :: null :: Nil, null) :: Nil
    )

    /*
    // Right now, "field1" and "field2" are treated as aliases. We should fix it.
    // TODO: Re-enable the following test.
    checkAnswer(
      sql("select arrayOfStruct[0].field1, arrayOfStruct[0].field2 from jsonTable"),
      (true, "str1") :: Nil
    )
    */

    /*
    // Right now, the analyzer cannot resolve arrayOfStruct.field1 and arrayOfStruct.field2.
    // TODO: Re-enable the following test.
    // Getting all values of a specific field from an array of structs.
    checkAnswer(
      sql("select arrayOfStruct.field1, arrayOfStruct.field2 from jsonTable"),
      (Seq(true, false), Seq("str1", null)) :: Nil
    )
    */

    // Access a struct and fields inside of it.
    checkAnswer(
      sql("select struct, struct.field1, struct.field2 from jsonTable"),
      (
        Seq(true, BigDecimal("92233720368547758070")),
        true,
        BigDecimal("92233720368547758070")) :: Nil
    )

    // Access an array field of a struct.
    checkAnswer(
      sql("select structWithArrayFields.field1, structWithArrayFields.field2 from jsonTable"),
      (Seq(4, 5, 6), Seq("str1", "str2")) :: Nil
    )

    // Access elements of an array field of a struct.
    checkAnswer(
      sql("select structWithArrayFields.field1[1], structWithArrayFields.field2[3] from jsonTable"),
      (5, null) :: Nil
    )
  }

  test("Type conflict in primitive field values") {
    val jsonSchemaRDD = jsonRDD(primitiveFieldValueTypeConflict)

    val expectedSchema =
      AttributeReference("num_bool", StringType, true)() ::
      AttributeReference("num_num_1", LongType, true)() ::
      AttributeReference("num_num_2", DecimalType, true)() ::
      AttributeReference("num_num_3", DoubleType, true)() ::
      AttributeReference("num_str", StringType, true)() ::
      AttributeReference("str_bool", StringType, true)() :: Nil

    comparePlans(Schema(expectedSchema), Schema(jsonSchemaRDD.logicalPlan.output))

    jsonSchemaRDD.registerAsTable("jsonTable")

    checkAnswer(
      sql("select * from jsonTable"),
      ("true", 11L, null, 1.1, "13.1", "str1") ::
      ("12", null, BigDecimal("21474836470.9"), null, null, "true") ::
      ("false", 21474836470L, BigDecimal("92233720368547758070"), 100, "str1", "false") ::
      (null, 21474836570L, BigDecimal(1.1), 21474836470L, "92233720368547758070", null) :: Nil
    )

    // Number and Boolean conflict: resolve the type as number in this query.
    checkAnswer(
      sql("select num_bool - 10 from jsonTable where num_bool > 11"),
      2
    )

    /*
    // Right now, the analyzer does not promote strings in a boolean expreesion.
    // Number and Boolean conflict: resolve the type as boolean in this query.
    // TODO: Re-enable this test
    checkAnswer(
      sql("select num_bool from jsonTable where NOT num_bool"),
      false
    )
    */

    /*
    // Right now, the analyzer does not know that num_bool should be treated as a boolean.
    // Number and Boolean conflict: resolve the type as boolean in this query.
    // TODO: Re-enable this test
    checkAnswer(
      sql("select num_bool from jsonTable where num_bool"),
      true
    )
    */

    // Widening to LongType
    checkAnswer(
      sql("select num_num_1 - 100 from jsonTable where num_num_1 > 11"),
      Seq(21474836370L) :: Seq(21474836470L) :: Nil
    )

    checkAnswer(
      sql("select num_num_1 - 100 from jsonTable where num_num_1 > 10"),
      Seq(-89) :: Seq(21474836370L) :: Seq(21474836470L) :: Nil
    )

    // Widening to DecimalType
    checkAnswer(
      sql("select num_num_2 + 1.2 from jsonTable where num_num_2 > 1.1"),
      Seq(BigDecimal("21474836472.1")) :: Seq(BigDecimal("92233720368547758071.2")) :: Nil
    )

    // Widening to DoubleType
    checkAnswer(
      sql("select num_num_3 + 1.2 from jsonTable where num_num_3 > 1.1"),
      Seq(101.2) :: Seq(21474836471.2) :: Nil
    )

    /*
    // Right now, we have a parsing error.
    // Number and String conflict: resolve the type as number in this query.
    // TODO: Re-enable this test
    checkAnswer(
      sql("select num_str + 1.2 from jsonTable where num_str > 92233720368547758060"),
      BigDecimal("92233720368547758061.2")
    )

    // The plan of the following DSL is
    // Project [(CAST(num_str#65:4, DoubleType) + 1.2) AS num#78]
    //  Filter (CAST(CAST(num_str#65:4, DoubleType), DecimalType) > 92233720368547758060)
    //    ExistingRdd [num_bool#61,num_num_1#62L,num_num_2#63,num_num_3#64,num_str#65,str_bool#66]
    // We should directly cast num_str to DecimalType and also need to do the right type promotion
    // in the Project.
    checkAnswer(
      jsonSchemaRDD.
        where('num_str > BigDecimal("92233720368547758060")).
        select('num_str + 1.2 as Symbol("num")),
      BigDecimal("92233720368547758061.2")
    )
    */

    // Number and String conflict: resolve the type as number in this query.
    checkAnswer(
      sql("select num_str + 1.2 from jsonTable where num_str > 14"),
      92233720368547758071.2
      // Seq(14.3) :: Seq(92233720368547758071.2) :: Nil
    )

    /*
    // The following test will fail. The type of num_str is StringType.
    // So, to evaluate num_str + 1.2, we first need to use Cast to convert the type.
    // In our test data, one value of num_str is 13.1.
    // The result of (CAST(num_str#65:4, DoubleType) + 1.2) for this value is 14.299999999999999,
    // which is not 14.3.
    // Number and String conflict: resolve the type as number in this query.
    checkAnswer(
      sql("select num_str + 1.2 from jsonTable where num_str > 13"),
      Seq(14.3) :: Seq(92233720368547758071.2) :: Nil
    )
    */

    // String and Boolean conflict: resolve the type as string.
    checkAnswer(
      sql("select * from jsonTable where str_bool = 'str1'"),
      ("true", 11L, null, 1.1, "13.1", "str1") :: Nil
    )

    // TODO: Need to test converting str_bool to boolean values.
    // Right now, it has the same issues with tests above on num_bool.
  }

  test("Type conflict in complex field values") {
    val jsonSchemaRDD = jsonRDD(complexFieldValueTypeConflict)

    val expectedSchema =
      AttributeReference("array", ArrayType(IntegerType), true)() ::
      AttributeReference("num_struct", StringType, true)() ::
      AttributeReference("str_array", StringType, true)() ::
      AttributeReference("struct_array", StringType, true)() ::
      AttributeReference("struct", StructType(
        StructField("field", StringType, true) :: Nil), true)() :: Nil

    comparePlans(Schema(expectedSchema), Schema(jsonSchemaRDD.logicalPlan.output))

    jsonSchemaRDD.registerAsTable("jsonTable")

    checkAnswer(
      sql("select * from jsonTable"),
      (Seq(), "11", "[1,2,3]", "[]", Seq(null)) ::
      (null, """{"field":false}""", null, "{}", null) ::
      (Seq(4, 5, 6), null, "str", "[7,8,9]", Seq(null)) ::
      (Seq(7), "{}","[str1,str2,33]", """{"field":true}""", Seq("str")) :: Nil
    )
  }

  test("Type conflict in array elements") {
    val jsonSchemaRDD = jsonRDD(arrayElementTypeConflict)

    val expectedSchema =
      AttributeReference("array", ArrayType(StringType), true)() :: Nil

    comparePlans(Schema(expectedSchema), Schema(jsonSchemaRDD.logicalPlan.output))

    jsonSchemaRDD.registerAsTable("jsonTable")

    checkAnswer(
      sql("select * from jsonTable"),
      Seq(Seq("1", "1.1", "true", null, "[]", "{}", "[2,3,4]",
        """{"field":str}""")) :: Nil
    )

    // Treat an element as a number.
    checkAnswer(
      sql("select array[0] + 1 from jsonTable"),
      2
    )
  }

  test("Handle missing fields") {
    val jsonSchemaRDD = jsonRDD(missingFields)

    val expectedSchema =
      AttributeReference("a", BooleanType, true)() ::
      AttributeReference("b", LongType, true)() ::
      AttributeReference("c", ArrayType(IntegerType), true)() ::
      AttributeReference("e", StringType, true)() ::
      AttributeReference("d", StructType(
        StructField("field", BooleanType, true) :: Nil), true)() :: Nil

    comparePlans(Schema(expectedSchema), Schema(jsonSchemaRDD.logicalPlan.output))

    jsonSchemaRDD.registerAsTable("jsonTable")
  }

  test("Type promotion") {
    def checkTypePromotion(expected: Any, actual: Any) {
      assert(expected.getClass == actual.getClass,
        s"Failed to promote ${actual.getClass} to ${expected.getClass}.")
      assert(expected == actual,
        s"Promoted value ${actual}(${actual.getClass}) does not equal the expected value " +
          s"${expected}(${expected.getClass}).")
    }

    val intNumber: Int = 2147483647
    checkTypePromotion(intNumber, enforceCorrectType(intNumber, IntegerType))
    checkTypePromotion(intNumber.toLong, enforceCorrectType(intNumber, LongType))
    checkTypePromotion(intNumber.toDouble, enforceCorrectType(intNumber, DoubleType))
    checkTypePromotion(BigDecimal(intNumber), enforceCorrectType(intNumber, DecimalType))

    val longNumber: Long = 9223372036854775807L
    checkTypePromotion(longNumber, enforceCorrectType(longNumber, LongType))
    checkTypePromotion(longNumber.toDouble, enforceCorrectType(longNumber, DoubleType))
    checkTypePromotion(BigDecimal(longNumber), enforceCorrectType(longNumber, DecimalType))

    val doubleNumber: Double = 1.7976931348623157E308d
    checkTypePromotion(doubleNumber.toDouble, enforceCorrectType(doubleNumber, DoubleType))
    checkTypePromotion(BigDecimal(doubleNumber), enforceCorrectType(doubleNumber, DecimalType))
  }

  test("Automatically update the schema when we have missing fields") {
    val partialSchema =
      StructType(StructField("b", LongType, true) :: StructField("e", StringType, true) :: Nil)

    val json = missingFields

    val jsonTable: JsonTable =
      new JsonTable(TestSQLContext, partialSchema, true, json)

    jsonTable.registerAsTable("jsonTable")

    checkAnswer(
      sql("select * from jsonTable"),
      (null, null) ::
      (21474836470L, null) ::
      (null, null) ::
      (null, null) ::
      (null, "str") :: Nil
    )

    // Update the schema
    jsonTable.adjustSchema()

    val expectedSchema =
      AttributeReference("a", BooleanType, true)() ::
      AttributeReference("b", LongType, true)() ::
      AttributeReference("c", ArrayType(IntegerType), true)() ::
      AttributeReference("e", StringType, true)() ::
      AttributeReference("d", StructType(
        StructField("field", BooleanType, true) :: Nil), true)() :: Nil

    // We should get the entire schema for this JsonTable.
    comparePlans(Schema(expectedSchema), Schema(jsonTable.logicalPlan.output))

    // The catalog should be updated.
    comparePlans(Schema(expectedSchema),
      Schema(TestSQLContext.catalog.lookupRelation(None, "jsonTable", None).output))

    checkAnswer(
      jsonTable.select(Star(None)),
      (true, null, null, null, null) ::
      (null, 21474836470L, null, null, null) ::
      (null, null, Seq(33, 44), null, null) ::
      (null, null, null, null, Seq(true)) ::
      (null, null, null, "str", null) :: Nil
    )
  }

  test("Automatically update the schema when we have a wrong type for a primitive field") {
    val partialSchema =
      StructType(StructField("b", IntegerType, true) :: StructField("e", StringType, true) :: Nil)

    val json = missingFields

    val jsonTable: JsonTable =
      new JsonTable(TestSQLContext, partialSchema, true, json)

    jsonTable.registerAsTable("jsonTable")

    // Select all columns of the partial schema. The result is wrong because the type of b is
    // IntegerType, but it should be LongType. When we have the wrong type, we return a null.
    checkAnswer(
      sql("select * from jsonTable"),
      (null, null) ::
      (null, null) ::
      (null, null) ::
      (null, null) ::
      (null, "str") :: Nil
    )

    // Update the schema
    jsonTable.adjustSchema()

    val expectedSchema =
      AttributeReference("a", BooleanType, true)() ::
      AttributeReference("b", LongType, true)() ::
      AttributeReference("c", ArrayType(IntegerType), true)() ::
      AttributeReference("e", StringType, true)() ::
      AttributeReference("d", StructType(
        StructField("field", BooleanType, true) :: Nil), true)() :: Nil

    // We should get the entire schema for this JsonTable.
    comparePlans(Schema(expectedSchema), Schema(jsonTable.logicalPlan.output))
    // The catalog should be updated.
    comparePlans(Schema(expectedSchema),
      Schema(TestSQLContext.catalog.lookupRelation(None, "jsonTable", None).output))

    checkAnswer(
      jsonTable.select(Star(None)),
      (true, null, null, null, null) ::
      (null, 21474836470L, null, null, null) ::
      (null, null, Seq(33, 44), null, null) ::
      (null, null, null, null, Seq(true)) ::
      (null, null, null, "str", null) :: Nil
    )
  }
}
