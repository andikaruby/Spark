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

package org.apache.spark.shuffle.sort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import scala.Product2;
import scala.Tuple2;
import scala.collection.Iterator;

import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.spark.Partitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.TaskContext;
import org.apache.spark.executor.ShuffleWriteMetrics;
import org.apache.spark.serializer.Serializer;
import org.apache.spark.serializer.SerializerInstance;
import org.apache.spark.storage.*;
import org.apache.spark.util.Utils;

/**
 * This class implements sort-based shuffle's hash-style shuffle fallback path. This write path
 * writes incoming records to separate files, one file per reduce partition, then concatenates these
 * per-partition files to form a single output file, regions of which are served to reducers.
 * Records are not buffered in memory. This is essentially identical to
 * {@link org.apache.spark.shuffle.hash.HashShuffleWriter}, except that it writes output in a format
 * that can be served / consumed via {@link org.apache.spark.shuffle.IndexShuffleBlockResolver}.
 * <p>
 * This write path is inefficient for shuffles with large numbers of reduce partitions because it
 * simultaneously opens separate serializers and file streams for all partitions. As a result,
 * {@link SortShuffleManager} only selects this write path when
 * <ul>
 *    <li>no Ordering is specified,</li>
 *    <li>no Aggregator is specific, and</li>
 *    <li>the number of partitions is less than
 *      <code>spark.shuffle.sort.bypassMergeThreshold</code>.</li>
 * </ul>
 *
 * This code used to be part of {@link org.apache.spark.util.collection.ExternalSorter} but was
 * refactored into its own class in order to reduce code complexity; see SPARK-7855 for details.
 * <p>
 * There have been proposals to completely remove this code path; see SPARK-6026 for details.
 */
final class BypassMergeSortShuffleWriter<K, V> implements SortShuffleFileWriter<K, V> {

  private final Logger logger = LoggerFactory.getLogger(BypassMergeSortShuffleWriter.class);

  private final int fileBufferSize;
  private final boolean transferToEnabled;
  private final int numPartitions;
  private final BlockManager blockManager;
  private final Partitioner partitioner;
  private final ShuffleWriteMetrics writeMetrics;
  private final Serializer serializer;

  /** Array of file writers, one for each partition */
  private BlockObjectWriter[] partitionWriters;

  public BypassMergeSortShuffleWriter(
      SparkConf conf,
      BlockManager blockManager,
      Partitioner partitioner,
      ShuffleWriteMetrics writeMetrics,
      Serializer serializer) {
    // Use getSizeAsKb (not bytes) to maintain backwards compatibility if no units are provided
    this.fileBufferSize = (int) conf.getSizeAsKb("spark.shuffle.file.buffer", "32k") * 1024;
    this.transferToEnabled = conf.getBoolean("spark.file.transferTo", true);
    this.numPartitions = partitioner.numPartitions();
    this.blockManager = blockManager;
    this.partitioner = partitioner;
    this.writeMetrics = writeMetrics;
    this.serializer = serializer;
  }

  @Override
  public void insertAll(Iterator<Product2<K, V>> records) throws IOException {
    assert (partitionWriters == null);
    if (!records.hasNext()) {
      return;
    }
    final SerializerInstance serInstance = serializer.newInstance();
    final long openStartTime = System.nanoTime();
    partitionWriters = new BlockObjectWriter[numPartitions];
    for (int i = 0; i < numPartitions; i++) {
      final Tuple2<TempShuffleBlockId, File> tempShuffleBlockIdPlusFile =
        blockManager.diskBlockManager().createTempShuffleBlock();
      final File file = tempShuffleBlockIdPlusFile._2();
      final BlockId blockId = tempShuffleBlockIdPlusFile._1();
      // Note that we purposely do not call open() on the disk writers here; DiskBlockObjectWriter
      // will automatically open() itself if necessary. This is an optimization to avoid file
      // creation and truncation for empty partitions; this optimization probably doesn't make sense
      // for most realistic production workloads, but it can make a large difference when playing
      // around with Spark SQL queries in spark-shell on toy datasets: if you performed a query over
      // an extremely small number of records then Spark SQL's default parallelism of 200 would
      // result in slower out-of-the-box performance due to these constant-factor overheads. This
      // optimization speeds up local microbenchmarking and SQL unit tests.
      partitionWriters[i] =
        blockManager.getDiskWriter(blockId, file, serInstance, fileBufferSize, writeMetrics).open();
    }
    // Creating the file to write to and creating a disk writer both involve interacting with
    // the disk, and can take a long time in aggregate when we open many files, so should be
    // included in the shuffle write time.
    writeMetrics.incShuffleWriteTime(System.nanoTime() - openStartTime);

    while (records.hasNext()) {
      final Product2<K, V> record = records.next();
      final K key = record._1();
      partitionWriters[partitioner.getPartition(key)].write(key, record._2());
    }

    for (BlockObjectWriter writer : partitionWriters) {
      writer.commitAndClose();
    }
  }

  @Override
  public long[] writePartitionedFile(
      BlockId blockId,
      TaskContext context,
      File outputFile) throws IOException {
    // Track location of the partition starts in the output file
    final long[] lengths = new long[numPartitions];
    if (partitionWriters == null) {
      // We were passed an empty iterator
      return lengths;
    }

    final FileOutputStream out = new FileOutputStream(outputFile, true);
    final long writeStartTime = System.nanoTime();
    boolean threwException = true;
    try {
      for (int i = 0; i < numPartitions; i++) {
        if (partitionWriters[i].fileSegment().length() == 0) {
          // In insertAll(), we didn't create empty files for empty reduce partitions; this branch
          // handles that case. Since we'll be skipping deletion of these files, verify that they
          // don't exist:
          assert(!partitionWriters[i].fileSegment().file().exists());
          continue;
        }
        final FileInputStream in = new FileInputStream(partitionWriters[i].fileSegment().file());
        boolean copyThrewException = true;
        try {
          lengths[i] = Utils.copyStream(in, out, false, transferToEnabled);
          copyThrewException = false;
        } finally {
          Closeables.close(in, copyThrewException);
        }
        if (!blockManager.diskBlockManager().getFile(partitionWriters[i].blockId()).delete()) {
          logger.error("Unable to delete file for partition {}", i);
        }
      }
      threwException = false;
    } finally {
      Closeables.close(out, threwException);
      writeMetrics.incShuffleWriteTime(System.nanoTime() - writeStartTime);
    }
    partitionWriters = null;
    return lengths;
  }

  @Override
  public void stop() throws IOException {
    if (partitionWriters != null) {
      try {
        final DiskBlockManager diskBlockManager = blockManager.diskBlockManager();
        for (BlockObjectWriter writer : partitionWriters) {
          // This method explicitly does _not_ throw exceptions:
          writer.revertPartialWritesAndClose();
          final File file = diskBlockManager.getFile(writer.blockId());
          if (file.exists() && !file.delete()) {
            logger.error("Error while deleting file for block {}", writer.blockId());
          }
        }
      } finally {
        partitionWriters = null;
      }
    }
  }
}
