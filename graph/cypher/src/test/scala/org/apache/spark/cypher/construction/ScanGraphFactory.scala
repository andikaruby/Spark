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
 *
 */

package org.apache.spark.cypher.construction

import java.time.{LocalDate, LocalDateTime}

import org.apache.spark.cypher.SparkTable.DataFrameTable
import org.apache.spark.cypher.conversions.TemporalConversions._
import org.apache.spark.cypher.conversions.TypeConversions._
import org.apache.spark.cypher.{SparkCypherSession, SparkEntityTable}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Dataset, Row}
import org.opencypher.okapi.api.graph.Pattern
import org.opencypher.okapi.api.io.conversion.{ElementMapping, NodeMappingBuilder, RelationshipMappingBuilder}
import org.opencypher.okapi.api.schema.PropertyKeys.PropertyKeys
import org.opencypher.okapi.impl.exception.IllegalArgumentException
import org.opencypher.okapi.impl.temporal.Duration
import org.opencypher.okapi.relational.api.graph.RelationalCypherGraph
import org.opencypher.okapi.relational.impl.graph.ScanGraph
import org.opencypher.okapi.testing.propertygraph.{CreateGraphFactory, CypherTestGraphFactory, InMemoryTestGraph}

import scala.collection.JavaConverters._

object ScanGraphFactory extends CypherTestGraphFactory[SparkCypherSession] {

  def encodeIdColumns(df: Dataset[Row], mapping: ElementMapping): Dataset[Row] = {
    val idCols = mapping.allSourceIdKeys.map { columnName =>
      val dataType = df.schema.fields(df.schema.fieldIndex(columnName)).dataType
      dataType match {
        case LongType => df.col(columnName).cast(StringType).cast(BinaryType)
        case IntegerType => df.col(columnName).cast(StringType).cast(BinaryType)
        case StringType => df.col(columnName).cast(BinaryType)
        case BinaryType => df.col(columnName)
        case unsupportedType => throw IllegalArgumentException(
          expected = s"Column `$columnName` should have a valid identifier data type, such as [`$BinaryType`, `$StringType`, `$LongType`, `$IntegerType`]",
          actual = s"Unsupported column type `$unsupportedType`"
        )
      }
    }
    val remainingCols = mapping.allSourceKeys.filterNot(mapping.allSourceIdKeys.contains).map(df.col)
    val colsToSelect = idCols ++ remainingCols
    df.select(colsToSelect: _*)
  }


  def initGraph(createQuery: String)
    (implicit sparkCypher: SparkCypherSession): RelationalCypherGraph[DataFrameTable] = {
    apply(CreateGraphFactory(createQuery))
  }

  val tableEntityIdKey = "___id"
  val tableEntityStartNodeKey = "___source"
  val tableEntityEndNodeKey = "___target"

  override def apply(propertyGraph: InMemoryTestGraph, additionalPattern: Seq[Pattern] = Seq.empty)
    (implicit sparkCypher: SparkCypherSession): ScanGraph[DataFrameTable] = {
    require(additionalPattern.isEmpty, "Additional pattern input not yet supported.")
    val schema = computeSchema(propertyGraph)

    val nodeScans = schema.labelCombinations.combos.map { labels =>
      val propKeys = schema.nodePropertyKeys(labels)

      val idStructField = Seq(StructField(tableEntityIdKey, LongType, nullable = false))
      val structType = StructType(idStructField ++ getPropertyStructFields(propKeys))

      val header = Seq(tableEntityIdKey) ++ propKeys.keys
      val rows = propertyGraph.nodes
        .filter(_.labels == labels)
        .map { node =>
          val propertyValues = propKeys.map(key =>
            node.properties.unwrap.get(key._1) match {
              case Some(date: LocalDate) => java.sql.Date.valueOf(date)
              case Some(localDateTime: LocalDateTime) => java.sql.Timestamp.valueOf(localDateTime)
              case Some(dur: Duration) => dur.toCalendarInterval
              case Some(other) => other
              case None => null
            }
          )
          Row.fromSeq(Seq(node.id) ++ propertyValues)
        }

      val records = sparkCypher.sparkSession.createDataFrame(rows.asJava, structType).toDF(header: _*)

      val nodeMapping = NodeMappingBuilder
        .on(tableEntityIdKey)
        .withImpliedLabels(labels.toSeq: _*)
        .withPropertyKeys(propKeys.keys.toSeq: _*)
        .build

      val encodedRecords = encodeIdColumns(records, nodeMapping)

      SparkEntityTable(nodeMapping, encodedRecords)
    }

    val relScans = schema.relationshipTypes.map { relType =>
      val propKeys = schema.relationshipPropertyKeys(relType)

      val idStructFields = Seq(
        StructField(tableEntityIdKey, LongType, nullable = false),
        StructField(tableEntityStartNodeKey, LongType, nullable = false),
        StructField(tableEntityEndNodeKey, LongType, nullable = false))
      val structType = StructType(idStructFields ++ getPropertyStructFields(propKeys))

      val header = Seq(tableEntityIdKey, tableEntityStartNodeKey, tableEntityEndNodeKey) ++ propKeys.keys
      val rows = propertyGraph.relationships
        .filter(_.relType == relType)
        .map { rel =>
          val propertyValues = propKeys.map(key => rel.properties.unwrap.getOrElse(key._1, null))
          Row.fromSeq(Seq(rel.id, rel.startId, rel.endId) ++ propertyValues)
        }

      val records = sparkCypher.sparkSession.createDataFrame(rows.asJava, structType).toDF(header: _*)

      val relationshipMapping = RelationshipMappingBuilder
        .on(tableEntityIdKey)
        .from(tableEntityStartNodeKey)
        .to(tableEntityEndNodeKey)
        .relType(relType)
        .withPropertyKeys(propKeys.keys.toSeq: _*)
        .build

      val encodedRecords = encodeIdColumns(records, relationshipMapping)

      SparkEntityTable(relationshipMapping, encodedRecords)
    }

    new ScanGraph(nodeScans.toSeq ++ relScans, schema)
  }

  override def name: String = getClass.getSimpleName

  protected def getPropertyStructFields(propKeys: PropertyKeys): Seq[StructField] = {
    propKeys.foldLeft(Seq.empty[StructField]) {
      case (fields, key) => fields :+ StructField(key._1, key._2.getSparkType, key._2.isNullable)
    }
  }
}
