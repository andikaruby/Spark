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

package org.apache.spark.sql.connector.catalog;

import java.util.Map;

import org.apache.spark.annotation.Experimental;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.analysis.NoSuchPartitionsException;
import org.apache.spark.sql.catalyst.analysis.PartitionsAlreadyExistException;

/**
 * An atomic partition interface of {@link Table} to operate multiple partitions atomically.
 * <p>
 * These APIs are used to modify table partition or partition metadata,
 * they will change the table data as well.
 * ${@link #createPartitions}:
 *     add an array of partitions and any data that their location contains to the table
 * ${@link #dropPartitions}:
 *     remove an array of partitions and any data they contains from the table
 * ${@link #replacePartitionMetadatas}:
 *     point an array of partitions to new locations, which will swap location's data for the other
 *
 * @since 3.1.0
 */
@Experimental
public interface SupportsAtomicPartitionManagement extends SupportsPartitionManagement {

  /**
   * Create an array of partitions atomically in table.
   * <p>
   * If any partition already exists,
   * the operation of createPartitions need to be safely rolled back.
   *
   * @param idents an array of new partition identifiers
   * @param properties the metadata of the partitions
   * @throws PartitionsAlreadyExistException If any partition already exists for the identifier
   * @throws UnsupportedOperationException If partition property is not supported
   */
  void createPartitions(
      InternalRow[] idents,
      Map<String, String>[] properties)
      throws PartitionsAlreadyExistException, UnsupportedOperationException;

  /**
   * Drop an array of partitions atomically from table.
   * <p>
   * If any partition doesn't exists,
   * the operation of dropPartitions need to be safely rolled back.
   *
   * @param idents an array of partition identifiers
   * @throws NoSuchPartitionsException If the partition identifiers to drop doesn't exist
   */
  void dropPartitions(
      InternalRow[] idents) throws NoSuchPartitionsException;

  /**
   * Replace the partition metadata of the existing partitions atomically.
   * <p>
   * If any partition doesn't exists,
   * the operation of replacePartitionMetadatas need to be safely rolled back.
   *
   * @param idents the partition identifier of the existing partitions
   * @param properties the new metadata of the partitions
   * @throws NoSuchPartitionsException If the partition identifiers to alter doesn't exist
   * @throws UnsupportedOperationException If partition property is not supported
   */
  void replacePartitionMetadatas(
      InternalRow[] idents,
      Map<String, String>[] properties)
      throws NoSuchPartitionsException, UnsupportedOperationException;
}
