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

import java.util.Properties
import scala.language.implicitConversions
import org.apache.hadoop.hive.ql.metadata.Partition
import org.apache.hadoop.hive.ql.plan.{FileSinkDesc, TableDesc}
import scala.collection.JavaConversions._
import org.apache.hadoop.hive.serde2.{Deserializer, ColumnProjectionUtils}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hive.ql.Context
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hive.common.`type`.HiveDecimal
import java.net.URI
import org.apache.hadoop.{io => hadoopIo}
import org.apache.hadoop.hive.ql.stats.StatsSetupConst
import org.apache.hadoop.mapred.InputFormat
import org.apache.hadoop.hive.ql.metadata.Hive
import org.apache.hadoop.hive.ql.metadata.Table
import org.apache.hadoop.hive.ql.processors._
import org.apache.hadoop.hive.conf.HiveConf

/*hive-0.12.0 support shimmer layer*/
object HiveShim {
  val version = "0.12.0"
  val metastoreDecimal = "decimal"
  def getTableDesc(serdeClass: Class[_ <: Deserializer], inputFormatClass: Class[_ <: InputFormat[_, _]], outputFormatClass: Class[_], properties: Properties) = {
    new TableDesc(serdeClass, inputFormatClass, outputFormatClass, properties)
  }
  def getStatsSetupConstTotalSize = StatsSetupConst.TOTAL_SIZE
  def createDefaultDBIfNeeded(context: HiveContext) ={  }

  /*handle the difference in "None" and empty ""*/
  def getEmptyCommentsFieldValue = "None"

  def convertCatalystString2Hive(s: String) = new hadoopIo.Text(s) // TODO why should be Text?

  def getCommandProcessor(cmd: Array[String], conf: HiveConf) =  {
    CommandProcessorFactory.get(cmd(0), conf)
  }

  def createDecimal(bd: java.math.BigDecimal): HiveDecimal = {
    new HiveDecimal(bd)
  }

  def appendReadColumns(conf: Configuration, ids: Seq[Integer], names: Seq[String]) {
    ColumnProjectionUtils.appendReadColumnIDs(conf, ids)
    ColumnProjectionUtils.appendReadColumnNames(conf, names)
  }

  implicit class wrapperToPartition(p: Partition) {
    def getDataLocationPath: Path = p.getPartitionPath
  }
  implicit class wrapperToHive(client: Hive) {
    def getAllPartitionsOf(tbl: Table) = {
      client.getAllPartitionsForPruner(tbl)
    }
  }
}

class ShimContext(conf: Configuration) extends Context(conf) {
  def  getExternalTmpPath(uri: URI): String = {
    super.getExternalTmpFileURI(uri)
  }
}

class ShimFileSinkDesc(var dir: String, var tableInfo: TableDesc, var compressed: Boolean)
  extends FileSinkDesc(dir, tableInfo, compressed) {
}
