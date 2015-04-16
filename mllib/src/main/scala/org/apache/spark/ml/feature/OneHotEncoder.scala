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

import org.apache.spark.annotation.AlphaComponent
import org.apache.spark.ml.UnaryTransformer
import org.apache.spark.ml.attribute.NominalAttribute
import org.apache.spark.mllib.linalg.{Vector, Vectors, VectorUDT}
import org.apache.spark.ml.param._
import org.apache.spark.ml.param.shared.{HasInputCol, HasOutputCol}
import org.apache.spark.ml.util.SchemaUtils
import org.apache.spark.sql.types.{DataType, DoubleType, StructType}

/**
 * A one-hot encoder that maps a column of label indices to a column of binary vectors, with
 * at most a single one-value. By default, the binary vector has an element for each category, so
 * with 5 categories, an input value of 2.0 would map to an output vector of
 * (0.0, 0.0, 1.0, 0.0, 0.0). If includeFirst is set to false, the first category is omitted, so the
 * output vector for the previous example would be (0.0, 1.0, 0.0, 0.0) and an input value
 * of 0.0 would map to a vector of all zeros.  Omitting the first category enables the vector
 * columns to be independent.
 */
@AlphaComponent
class OneHotEncoder extends UnaryTransformer[Double, Vector, OneHotEncoder]
  with HasInputCol with HasOutputCol {

  /**
   * Whether to include a component in the encoded vectors for the first category, defaults to true.
   * @group param
   */
  final val includeFirst: Param[Boolean] =
    new Param[Boolean](this, "includeFirst", "include first category")
  setDefault(includeFirst -> true)

  /**
   * The names of the categories. Used to identify them in the attributes of the output column.
   * This is a required parameter.
   * @group param
   */
  final val labelNames: Param[Array[String]] =
    new Param[Array[String]](this, "labelNames", "categorical label names")

  /** @group setParam */
  def setIncludeFirst(value: Boolean): this.type = set(includeFirst, value)

  /** @group setParam */
  def setLabelNames(value: Array[String]): this.type = set(labelNames, value)

  /** @group setParam */
  override def setInputCol(value: String): this.type = set(inputCol, value)

  /** @group setParam */
  override def setOutputCol(value: String): this.type = set(outputCol, value)

  override def transformSchema(schema: StructType, paramMap: ParamMap): StructType = {
    val map = extractParamMap(paramMap)
    SchemaUtils.checkColumnType(schema, map(inputCol), DoubleType)
    val inputFields = schema.fields
    val outputColName = map(outputCol)
    require(inputFields.forall(_.name != outputColName),
      s"Output column $outputColName already exists.")
    require(map.contains(labelNames), "OneHotEncoder missing category names")
    val categories = map(labelNames)
    val attrValues = (if (map(includeFirst)) categories else categories.drop(1)).toArray
    val attr = NominalAttribute.defaultAttr.withName(outputColName).withValues(attrValues)
    val outputFields = inputFields :+ attr.toStructField()
    StructType(outputFields)
  }

  protected def createTransformFunc(paramMap: ParamMap): (Double) => Vector = {
    val map = extractParamMap(paramMap)
    val first = map(includeFirst)
    val vecLen = if (first) map(labelNames).length else map(labelNames).length - 1
    val oneValue = Array(1.0)
    val emptyValues = Array[Double]()
    val emptyIndices = Array[Int]()
    label: Double => {
      val values = if (first || label != 0.0) oneValue else emptyValues
      val indices = if (first) {
        Array(label.toInt)
      } else if (label != 0.0) {
        Array(label.toInt - 1)
      } else {
        emptyIndices
      }
      Vectors.sparse(vecLen, indices, values)
    }
  }

  /**
   * Returns the data type of the output column.
   */
  protected def outputDataType: DataType = new VectorUDT
}
