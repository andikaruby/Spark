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

package org.apache.spark.sql.hive

import java.net.URI
import java.util.{ArrayList => JArrayList}
import java.util.Properties
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hive.common.`type`.HiveDecimal
import org.apache.hadoop.hive.conf.HiveConf
import org.apache.hadoop.hive.ql.Context
import org.apache.hadoop.hive.ql.metadata.{Hive, Partition, Table}
import org.apache.hadoop.hive.ql.plan.{CreateTableDesc, FileSinkDesc, TableDesc}
import org.apache.hadoop.hive.ql.processors._
import org.apache.hadoop.hive.ql.stats.StatsSetupConst
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory
import org.apache.hadoop.hive.serde2.objectinspector.primitive.{HiveDecimalObjectInspector, PrimitiveObjectInspectorFactory}
import org.apache.hadoop.hive.serde2.objectinspector.{PrimitiveObjectInspector, ObjectInspector}
import org.apache.hadoop.hive.serde2.typeinfo.{TypeInfo, TypeInfoFactory}
import org.apache.hadoop.hive.serde2.{Deserializer, ColumnProjectionUtils}
import org.apache.hadoop.hive.serde2.{io => hiveIo}
import org.apache.hadoop.{io => hadoopIo}
import org.apache.hadoop.mapred.InputFormat
import org.apache.spark.sql.catalyst.types.decimal.Decimal
import scala.collection.JavaConversions._
import scala.language.implicitConversions

import org.apache.spark.sql.catalyst.types.DecimalType

class HiveFunctionWrapper(var functionClassName: String) extends java.io.Serializable {
  // for Serialization
  def this() = this(null)

  import org.apache.spark.util.Utils._
  def createFunction[UDFType <: AnyRef](): UDFType = {
    getContextOrSparkClassLoader
      .loadClass(functionClassName).newInstance.asInstanceOf[UDFType]
  }
}

/**
 * A compatibility layer for interacting with Hive version 0.12.0.
 */
private[hive] object HiveShim {
  val version = "0.12.0"

  def getTableDesc(
    serdeClass: Class[_ <: Deserializer],
    inputFormatClass: Class[_ <: InputFormat[_, _]],
    outputFormatClass: Class[_],
    properties: Properties) = {
    new TableDesc(serdeClass, inputFormatClass, outputFormatClass, properties)
  }

  def getStringWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.STRING,
      if (value == null) null else new hadoopIo.Text(value.asInstanceOf[String]))

  def getIntWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.INT,
      if (value == null) null else new hadoopIo.IntWritable(value.asInstanceOf[Int]))

  def getDoubleWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.DOUBLE,
      if (value == null) null else new hiveIo.DoubleWritable(value.asInstanceOf[Double]))

  def getBooleanWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.BOOLEAN,
      if (value == null) null else new hadoopIo.BooleanWritable(value.asInstanceOf[Boolean]))

  def getLongWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.LONG,
      if (value == null) null else new hadoopIo.LongWritable(value.asInstanceOf[Long]))

  def getFloatWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.FLOAT,
      if (value == null) null else new hadoopIo.FloatWritable(value.asInstanceOf[Float]))

  def getShortWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.SHORT,
      if (value == null) null else new hiveIo.ShortWritable(value.asInstanceOf[Short]))

  def getByteWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.BYTE,
      if (value == null) null else new hiveIo.ByteWritable(value.asInstanceOf[Byte]))

  def getBinaryWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.BINARY,
      if (value == null) null else new hadoopIo.BytesWritable(value.asInstanceOf[Array[Byte]]))

  def getDateWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.DATE,
      if (value == null) null else new hiveIo.DateWritable(value.asInstanceOf[java.sql.Date]))

  def getTimestampWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.TIMESTAMP,
      if (value == null) {
        null
      } else {
        new hiveIo.TimestampWritable(value.asInstanceOf[java.sql.Timestamp])
      })

  def getDecimalWritableConstantObjectInspector(value: Any): ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.DECIMAL,
      if (value == null) {
        null
      } else {
        new hiveIo.HiveDecimalWritable(
          HiveShim.createDecimal(value.asInstanceOf[Decimal].toBigDecimal.underlying()))
      })

  def getPrimitiveNullWritableConstantObjectInspector: ObjectInspector =
    PrimitiveObjectInspectorFactory.getPrimitiveWritableConstantObjectInspector(
      PrimitiveCategory.VOID, null)

  def createDriverResultsArray = new JArrayList[String]

  def processResults(results: JArrayList[String]) = results

  def getStatsSetupConstTotalSize = StatsSetupConst.TOTAL_SIZE

  def getStatsSetupConstRawDataSize = StatsSetupConst.RAW_DATA_SIZE

  def createDefaultDBIfNeeded(context: HiveContext) = {  }

  def getCommandProcessor(cmd: Array[String], conf: HiveConf) = {
    CommandProcessorFactory.get(cmd(0), conf)
  }

  def createDecimal(bd: java.math.BigDecimal): HiveDecimal = {
    new HiveDecimal(bd)
  }

  def appendReadColumns(conf: Configuration, ids: Seq[Integer], names: Seq[String]) {
    ColumnProjectionUtils.appendReadColumnIDs(conf, ids)
    ColumnProjectionUtils.appendReadColumnNames(conf, names)
  }

  def getExternalTmpPath(context: Context, uri: URI) = {
    context.getExternalTmpFileURI(uri)
  }

  def getDataLocationPath(p: Partition) = p.getPartitionPath

  def getAllPartitionsOf(client: Hive, tbl: Table) =  client.getAllPartitionsForPruner(tbl)

  def compatibilityBlackList = Seq(
    "decimal_.*",
    "udf7",
    "drop_partitions_filter2",
    "show_.*",
    "serde_regex",
    "udf_to_date",
    "udaf_collect_set",
    "udf_concat"
  )

  def setLocation(tbl: Table, crtTbl: CreateTableDesc): Unit = {
    tbl.setDataLocation(new Path(crtTbl.getLocation()).toUri())
  }

  def decimalMetastoreString(decimalType: DecimalType): String = "decimal"

  def decimalTypeInfo(decimalType: DecimalType): TypeInfo =
    TypeInfoFactory.decimalTypeInfo

  def decimalTypeInfoToCatalyst(inspector: PrimitiveObjectInspector): DecimalType = {
    DecimalType.Unlimited
  }

  def toCatalystDecimal(hdoi: HiveDecimalObjectInspector, data: Any): Decimal = {
    Decimal(hdoi.getPrimitiveJavaObject(data).bigDecimalValue())
  }
}

class ShimFileSinkDesc(var dir: String, var tableInfo: TableDesc, var compressed: Boolean)
  extends FileSinkDesc(dir, tableInfo, compressed) {
}
