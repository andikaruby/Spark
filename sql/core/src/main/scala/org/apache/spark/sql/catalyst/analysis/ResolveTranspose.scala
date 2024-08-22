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

package org.apache.spark.sql.catalyst.analysis

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Alias, Ascending, Attribute, AttributeReference, Cast, NamedExpression, SortOrder}
import org.apache.spark.sql.catalyst.plans.logical.{LocalRelation, LogicalPlan, Project, Sort, Transpose}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.catalyst.trees.TreePattern.TRANSPOSE
import org.apache.spark.sql.types.{DataType, StringType}
import org.apache.spark.unsafe.types.UTF8String


class ResolveTranspose(sparkSession: SparkSession) extends Rule[LogicalPlan] {

  private def leastCommonType(dataTypes: Seq[DataType]): DataType = {
    dataTypes.reduce { (dt1, dt2) =>
      TypeCoercion.findTightestCommonType(dt1, dt2).getOrElse {
        throw new IllegalArgumentException(s"No common type found for $dt1 and $dt2")
      }
    }
  }

  private def transposeMatrix(
    fullCollectedRows: Array[InternalRow],
    nonIndexColumnNames: Seq[String],
    nonIndexColumnDataTypes: Seq[DataType]): Array[Array[Any]] = {
    // scalastyle:off println

    // Construct the original matrix
    val originalMatrixNumCols = fullCollectedRows.head.numFields - 1
    val originalMatrixNumRows = fullCollectedRows.length
//    println(s"originalMatrixNumCols ${originalMatrixNumCols}")
//    println(s"originalMatrixNumRows ${originalMatrixNumRows}")

    val originalMatrix = Array.ofDim[Any](originalMatrixNumRows, originalMatrixNumCols)
    for (i <- 0 until originalMatrixNumRows) {
      val row = fullCollectedRows(i)
      for (j <- 0 until originalMatrixNumCols) {
        originalMatrix(i)(j) = row.get(j + 1, nonIndexColumnDataTypes(j))
      }
    }
//    println(s"originalMatrix:")
//    printMatrix(originalMatrix)

    // Transpose the original matrix
    val transposedMatrix = Array.ofDim[Any](originalMatrixNumCols, originalMatrixNumRows)
    for (i <- 0 until originalMatrixNumRows) {
      for (j <- 0 until originalMatrixNumCols) {
//        println(s"Transposing element at row $i, col $j: ${originalMatrix(i)(j)}")
        transposedMatrix(j)(i) = originalMatrix(i)(j)
//        println(s"transposedMatrix row $j, col $i: ${transposedMatrix(j)(i)}")
      }
    }

//    println(s"transposedMatrix:")
//    printMatrix(transposedMatrix)

    // Insert nonIndexColumnNames as first "column"
    val finalMatrix = Array.ofDim[Any](originalMatrixNumCols, originalMatrixNumRows + 1)
    for (i <- 0 until originalMatrixNumCols) {
      finalMatrix(i)(0) = UTF8String.fromString(nonIndexColumnNames(i))
    }
    for (i <- 0 until originalMatrixNumCols) {
      for (j <- 1 until originalMatrixNumRows + 1) {
        finalMatrix(i)(j) = transposedMatrix(i)(j - 1)
      }
    }

//    println(s"finalMatrix:")
//    printMatrix(finalMatrix)

    finalMatrix
  }

  private def printMatrix(matrix: Array[Array[Any]]): Unit = {
    matrix.foreach { row =>
      println(row.mkString("[", ", ", "]"))
    }
  }

  override def apply(plan: LogicalPlan): LogicalPlan = plan.resolveOperatorsWithPruning(
    _.containsPattern(TRANSPOSE)) {
    case t @ Transpose(indexColumn, child, _, _) if !t.resolved =>
      // Cast the index column to StringType
      val indexColumnAsString = Alias(
        Cast(indexColumn, StringType),
        indexColumn.asInstanceOf[Attribute].name)().asInstanceOf[NamedExpression]

      // Cast non-index columns to the least common type
      val nonIndexColumnsAttr = child.output.filterNot(
        _.exprId == indexColumn.asInstanceOf[Attribute].exprId)
      val nonIndexTypes = nonIndexColumnsAttr.map(_.dataType)
      val commonType = leastCommonType(nonIndexTypes)
      val nonIndexColumnsAsLCT = nonIndexColumnsAttr.map { attr =>
        Alias(Cast(attr, commonType), attr.name)()
      }

      // Sort by index column values, and collect the casted frame
      val allCastCols = indexColumnAsString +: nonIndexColumnsAsLCT
      val sortedChild = Sort(
        Seq(SortOrder(indexColumn.asInstanceOf[Attribute], Ascending)),
        global = true,
        child
      )
      val projectAllCastCols = Project(allCastCols, sortedChild)
      val queryExecution = sparkSession.sessionState.executePlan(projectAllCastCols)
      val fullCollectedRows = queryExecution.executedPlan.executeCollect()

      if (fullCollectedRows.isEmpty) {
        // Return a DataFrame with a single column "key" containing non-index column names
        val keyAttr = AttributeReference("key", StringType, nullable = false)()
        val keyValues = nonIndexColumnsAttr.map(
          _.name).map(name => UTF8String.fromString(name))
        val keyRows = keyValues.map(value => InternalRow(value))

        LocalRelation(Seq(keyAttr), keyRows)
      } else {

        // Transpose the matrix
        val nonIndexColumnNames = nonIndexColumnsAttr.map(_.name)
        val nonIndexColumnDataTypes = projectAllCastCols.output.tail.map(attr => attr.dataType)
        val transposedMatrix = transposeMatrix(
          fullCollectedRows, nonIndexColumnNames, nonIndexColumnDataTypes)
        val transposedInternalRows = transposedMatrix.map { row =>
          InternalRow.fromSeq(row.toIndexedSeq)
        }

        // Construct output attributes
        val keyAttr = AttributeReference("key", StringType, nullable = false)()
//        println(s"keyAttr: ${keyAttr}")
        val transposedColumnNames = fullCollectedRows.map { row => row.getString(0) }
        val valueAttrs = transposedColumnNames.map { value =>
          AttributeReference(
            value,
            commonType
          )()
        }
//        println(s"valueAttrs: ${valueAttrs}")

        LocalRelation(
          (keyAttr +: valueAttrs).toIndexedSeq, transposedInternalRows.toIndexedSeq)
      }
  }
}
