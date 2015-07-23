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

package org.apache.spark.ml.feature

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.types._

class RFormulaParserSuite extends SparkFunSuite {
  private def checkParse(
      formula: String,
      label: String,
      terms: Seq[String],
      schema: StructType = null) {
    val parsed = RFormulaParser.parse(formula).fit(schema)
    assert(parsed.label == label)
    assert(parsed.terms == terms)
  }

  test("parse simple formulas") {
    checkParse("y ~ x", "y", Seq("x"))
    checkParse("y ~ x + x", "y", Seq("x"))
    checkParse("y ~   ._foo  ", "y", Seq("._foo"))
    checkParse("resp ~ A_VAR + B + c123", "resp", Seq("A_VAR", "B", "c123"))
  }

  test("parse dot") {
    val schema = (new StructType)
      .add("a", "int", true, Metadata.empty)
      .add("b", "long", false, Metadata.empty)
      .add("c", "string", true, Metadata.empty)
    checkParse("a ~ .", "a", Seq("b", "c"), schema)
  }

  test("parse deletion") {
    val schema = (new StructType)
      .add("a", "int", true, Metadata.empty)
      .add("b", "long", false, Metadata.empty)
      .add("c", "string", true, Metadata.empty)
    checkParse("a ~ c - b", "a", Seq("c"), schema)
  }

  test("parse additions and deletions in order") {
    val schema = (new StructType)
      .add("a", "int", true, Metadata.empty)
      .add("b", "long", false, Metadata.empty)
      .add("c", "string", true, Metadata.empty)
    checkParse("a ~ . - b + . - c", "a", Seq("b"), schema)
  }
}
