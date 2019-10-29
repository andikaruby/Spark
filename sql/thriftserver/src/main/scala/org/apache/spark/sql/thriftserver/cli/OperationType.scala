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

package org.apache.spark.sql.thriftserver.cli

import org.apache.spark.sql.thriftserver.cli.thrift.TOperationType


/**
 * OperationType.
 */
private[thriftserver] abstract class OperationType {
  def toTOperationType: TOperationType
}

private[thriftserver] case object EXECUTE_STATEMENT extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.EXECUTE_STATEMENT
}

private[thriftserver] case object GET_TYPE_INFO extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.GET_TYPE_INFO
}

private[thriftserver] case object GET_CATALOGS extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.GET_CATALOGS
}

private[thriftserver] case object GET_SCHEMAS extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.GET_SCHEMAS
}

private[thriftserver] case object GET_TABLES extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.GET_TABLES
}

private[thriftserver] case object GET_TABLE_TYPES extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.GET_TABLE_TYPES
}

private[thriftserver] case object GET_COLUMNS extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.GET_COLUMNS
}

private[thriftserver] case object GET_FUNCTIONS extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.GET_FUNCTIONS
}

private[thriftserver] case object UNKNOWN_OPERATION extends OperationType {
  override def toTOperationType: TOperationType = TOperationType.UNKNOWN
}

private[thriftserver] object OperationType {
  def getOperationType(tOperationType: TOperationType): OperationType =
    tOperationType match {
      case TOperationType.EXECUTE_STATEMENT => EXECUTE_STATEMENT
      case TOperationType.GET_TYPE_INFO => GET_TYPE_INFO
      case TOperationType.GET_CATALOGS => GET_CATALOGS
      case TOperationType.GET_SCHEMAS => GET_SCHEMAS
      case TOperationType.GET_TABLES => GET_TABLES
      case TOperationType.GET_TABLE_TYPES => GET_TABLE_TYPES
      case TOperationType.GET_COLUMNS => GET_COLUMNS
      case TOperationType.GET_FUNCTIONS => GET_FUNCTIONS
      case _ => UNKNOWN_OPERATION
    }
}
