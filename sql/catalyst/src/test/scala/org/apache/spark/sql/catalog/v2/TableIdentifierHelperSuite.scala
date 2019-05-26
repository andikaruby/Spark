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
package org.apache.spark.sql.catalog.v2

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.catalyst.{CatalogTableIdentifier, TableIdentifier}
import org.apache.spark.sql.catalyst.parser.CatalystSqlParser
import org.apache.spark.sql.catalyst.plans.SQLHelper
import org.apache.spark.sql.util.CaseInsensitiveStringMap

class TableIdentifierHelperSuite extends SparkFunSuite with SQLHelper {
  import CatalystSqlParser._

  private val testCat = {
    val newCatalog = new TestTableCatalog
    newCatalog.initialize("testcat", CaseInsensitiveStringMap.empty())
    newCatalog
  }

  private def findCatalog(name: String): CatalogPlugin = name match {
    case "testcat" =>
      testCat
    case _ =>
      throw new CatalogNotFoundException(s"$name not found")
  }

  test("with catalog lookup function") {
    val helper = new TableIdentifierHelper {
      override def lookupCatalog: Option[String => CatalogPlugin] = Some(findCatalog(_))
    }
    import helper._

    assert(parseMultipartIdentifier("testcat.v2tbl").asCatalogTableIdentifier ===
      CatalogTableIdentifier(testCat, Identifier.of(Array.empty, "v2tbl")))
    assert(parseMultipartIdentifier("db.tbl").asCatalogTableIdentifier ===
      TableIdentifier("tbl", Some("db")))

  }

  test("without catalog lookup function") {
    val helper = new TableIdentifierHelper {
      override def lookupCatalog: Option[String => CatalogPlugin] = None
    }
    import helper._

    assert(parseMultipartIdentifier("testcat.v2tbl").asCatalogTableIdentifier ===
      TableIdentifier("v2tbl", Some("testcat")))
  }
}
