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

import org.apache.spark.annotation.Experimental
import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.param._
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.{ArrayType, DataType, StringType}

/**
 * :: Experimental ::
 * A feature transformer that converts the input array of strings into an array of n-grams. Null
 * values in the input array are ignored.
 * It returns an array of n-grams where each n-gram is represented by a space-separated string of
 * words.
 */
@Experimental
class NGram(override val uid: String)
  extends UnaryTransformer[Seq[String], Seq[String], NGram] {

  def this() = this(Identifiable.randomUID("ngram"))

  /**
   * Minimum n-gram length, >= 1.
   * Defauult: 2, bigram features
   * @group param
   */
  val NGramLength: IntParam = new IntParam(this, "NGramLength", "number elements per n-gram (>=1)",
    ParamValidators.gtEq(1))

  /** @group setParam */
  def setNGramLength(value: Int): this.type = set(NGramLength, value)

  /** @group getParam */
  def getNGramLength: Int = $(NGramLength)

  setDefault(NGramLength -> 2)

  override protected def createTransformFunc: Seq[String] => Seq[String] = {
    val minLength = $(NGramLength)
    _.sliding(minLength).map(_.mkString(" ")).toSeq
  }

  override protected def validateInputType(inputType: DataType): Unit = {
    require(
      inputType.sameType(ArrayType(StringType)),
      s"Input type must be ArrayType(StringType) but got $inputType.")
  }

  override protected def outputDataType: DataType = new ArrayType(StringType, false)
}
