/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.connector.catalog.index;

import java.util.Map;
import java.util.Properties;

import org.apache.spark.annotation.Evolving;
import org.apache.spark.sql.catalyst.analysis.IndexAlreadyExistsException;
import org.apache.spark.sql.catalyst.analysis.NoSuchIndexException;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.connector.catalog.CatalogPlugin;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.expressions.NamedReference;

/**
 * Catalog methods for working with index
 *
 * @since 3.3.0
 */
@Evolving
public interface SupportsIndex extends CatalogPlugin {

  /**
   * Creates an index.
   *
   * @param indexName the name of the index to be created
   * @param indexType the IndexType of the index to be created
   * @param table the table on which index to be created
   * @param columns the columns on which index to be created
   * @param columnProperties the properties of the columns on which index to be created
   * @param properties the properties of the index to be created
   * @throws IndexAlreadyExistsException If the index already exists (optional)
   * @throws UnsupportedOperationException If create index is not a supported operation
   */
  void createIndex(String indexName,
      String indexType,
      Identifier table,
      NamedReference[] columns,
      Map<NamedReference, Properties>[] columnProperties,
      Properties properties)
      throws IndexAlreadyExistsException, UnsupportedOperationException;

  /**
   * drops the index with the given name.
   *
   * @param indexName the name of the index to be dropped.
   * @return true if the index is dropped
   * @throws NoSuchIndexException If the index does not exist (optional)
   * @throws UnsupportedOperationException If drop index is not a supported operation
   */
  boolean dropIndex(String indexName) throws NoSuchIndexException, UnsupportedOperationException;

  /**
   * Checks whether an index exists.
   *
   * @param indexName the name of the index
   * @return true if the index exists, false otherwise
   */
  boolean indexExists(String indexName);

  /**
   * Lists all the indexes in a table.
   *
   * @param table the table to be checked on for indexes
   * @throws NoSuchTableException
   */
  TableIndex[] listIndexes(Identifier table) throws NoSuchTableException;
}
