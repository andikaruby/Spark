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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import javax.annotation.Nullable;

import scala.None$;
import scala.Option;
import scala.Product2;
import scala.Tuple2;
import scala.collection.Iterator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.spark.Partitioner;
import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.shuffle.SupportsTransferTo;
import org.apache.spark.api.shuffle.ShuffleMapOutputWriter;
import org.apache.spark.api.shuffle.ShufflePartitionWriter;
import org.apache.spark.api.shuffle.ShuffleWriteSupport;
import org.apache.spark.api.shuffle.TransferrableWritableByteChannel;
import org.apache.spark.internal.config.package$;
import org.apache.spark.scheduler.MapStatus;
import org.apache.spark.scheduler.MapStatus$;
import org.apache.spark.serializer.Serializer;
import org.apache.spark.serializer.SerializerInstance;
import org.apache.spark.shuffle.ShuffleWriteMetricsReporter;
import org.apache.spark.shuffle.ShuffleWriter;
import org.apache.spark.storage.*;
import org.apache.spark.util.Utils;

/**
 * This class implements sort-based shuffle's hash-style shuffle fallback path. This write path
 * writes incoming records to separate files, one file per reduce partition, then concatenates these
 * per-partition files to form a single output file, regions of which are served to reducers.
 * Records are not buffered in memory. It writes output in a format
 * that can be served / consumed via {@link org.apache.spark.shuffle.IndexShuffleBlockResolver}.
 * <p>
 * This write path is inefficient for shuffles with large numbers of reduce partitions because it
 * simultaneously opens separate serializers and file streams for all partitions. As a result,
 * {@link SortShuffleManager} only selects this write path when
 * <ul>
 *    <li>no map-side combine is specified, and</li>
 *    <li>the number of partitions is less than or equal to
 *      <code>spark.shuffle.sort.bypassMergeThreshold</code>.</li>
 * </ul>
 *
 * This code used to be part of {@link org.apache.spark.util.collection.ExternalSorter} but was
 * refactored into its own class in order to reduce code complexity; see SPARK-7855 for details.
 * <p>
 * There have been proposals to completely remove this code path; see SPARK-6026 for details.
 */
final class BypassMergeSortShuffleWriter<K, V> extends ShuffleWriter<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(BypassMergeSortShuffleWriter.class);

  private final int fileBufferSize;
  private final boolean transferToEnabled;
  private final int numPartitions;
  private final BlockManager blockManager;
  private final Partitioner partitioner;
  private final ShuffleWriteMetricsReporter writeMetrics;
  private final int shuffleId;
  private final int mapId;
  private final Serializer serializer;
  private final ShuffleWriteSupport shuffleWriteSupport;

  /** Array of file writers, one for each partition */
  private DiskBlockObjectWriter[] partitionWriters;
  private FileSegment[] partitionWriterSegments;
  @Nullable private MapStatus mapStatus;
  private long[] partitionLengths;

  /**
   * Are we in the process of stopping? Because map tasks can call stop() with success = true
   * and then call stop() with success = false if they get an exception, we want to make sure
   * we don't try deleting files, etc twice.
   */
  private boolean stopping = false;

  BypassMergeSortShuffleWriter(
      BlockManager blockManager,
      BypassMergeSortShuffleHandle<K, V> handle,
      int mapId,
      SparkConf conf,
      ShuffleWriteMetricsReporter writeMetrics,
      ShuffleWriteSupport shuffleWriteSupport) {
    // Use getSizeAsKb (not bytes) to maintain backwards compatibility if no units are provided
    this.fileBufferSize = (int) (long) conf.get(package$.MODULE$.SHUFFLE_FILE_BUFFER_SIZE()) * 1024;
    this.transferToEnabled = conf.getBoolean("spark.file.transferTo", true);
    this.blockManager = blockManager;
    final ShuffleDependency<K, V, V> dep = handle.dependency();
    this.mapId = mapId;
    this.shuffleId = dep.shuffleId();
    this.partitioner = dep.partitioner();
    this.numPartitions = partitioner.numPartitions();
    this.writeMetrics = writeMetrics;
    this.serializer = dep.serializer();
    this.shuffleWriteSupport = shuffleWriteSupport;
  }

  @Override
  public void write(Iterator<Product2<K, V>> records) throws IOException {
    assert (partitionWriters == null);
    ShuffleMapOutputWriter mapOutputWriter = shuffleWriteSupport
        .createMapOutputWriter(shuffleId, mapId, numPartitions);
    try {
      if (!records.hasNext()) {
        partitionLengths = new long[numPartitions];
        mapOutputWriter.commitAllPartitions();
        mapStatus = MapStatus$.MODULE$.apply(
            blockManager.shuffleServerId(),
            partitionLengths);
        return;
      }
      final SerializerInstance serInstance = serializer.newInstance();
      final long openStartTime = System.nanoTime();
      partitionWriters = new DiskBlockObjectWriter[numPartitions];
      partitionWriterSegments = new FileSegment[numPartitions];
      for (int i = 0; i < numPartitions; i++) {
        final Tuple2<TempShuffleBlockId, File> tempShuffleBlockIdPlusFile =
            blockManager.diskBlockManager().createTempShuffleBlock();
        final File file = tempShuffleBlockIdPlusFile._2();
        final BlockId blockId = tempShuffleBlockIdPlusFile._1();
        partitionWriters[i] =
            blockManager.getDiskWriter(blockId, file, serInstance, fileBufferSize, writeMetrics);
      }
      // Creating the file to write to and creating a disk writer both involve interacting with
      // the disk, and can take a long time in aggregate when we open many files, so should be
      // included in the shuffle write time.
      writeMetrics.incWriteTime(System.nanoTime() - openStartTime);

      while (records.hasNext()) {
        final Product2<K, V> record = records.next();
        final K key = record._1();
        partitionWriters[partitioner.getPartition(key)].write(key, record._2());
      }

      for (int i = 0; i < numPartitions; i++) {
        try (DiskBlockObjectWriter writer = partitionWriters[i]) {
          partitionWriterSegments[i] = writer.commitAndGet();
        }
      }

      partitionLengths = writePartitionedData(mapOutputWriter);
      mapOutputWriter.commitAllPartitions();
      mapStatus = MapStatus$.MODULE$.apply(
          blockManager.shuffleServerId(),
          partitionLengths);
    } catch (Exception e) {
      try {
        mapOutputWriter.abort(e);
      } catch (Exception e2) {
        logger.error("Failed to abort the writer after failing to write map output.", e2);
      }
      throw e;
    }
  }

  @VisibleForTesting
  long[] getPartitionLengths() {
    return partitionLengths;
  }

  /**
   * Concatenate all of the per-partition files into a single combined file.
   *
   * @return array of lengths, in bytes, of each partition of the file (used by map output tracker).
   */
  private long[] writePartitionedData(ShuffleMapOutputWriter mapOutputWriter) throws IOException {
    // Track location of the partition starts in the output file
    final long[] lengths = new long[numPartitions];
    if (partitionWriters == null) {
      // We were passed an empty iterator
      return lengths;
    }
    final long writeStartTime = System.nanoTime();
    try {
      for (int i = 0; i < numPartitions; i++) {
        final File file = partitionWriterSegments[i].file();
        ShufflePartitionWriter writer = mapOutputWriter.getPartitionWriter(i);
        if (file.exists()) {
          boolean copyThrewException = true;
          if (transferToEnabled) {
            FileInputStream in = new FileInputStream(file);
            TransferrableWritableByteChannel outputChannel = null;
            try (FileChannel inputChannel = in.getChannel()) {
              if (writer instanceof SupportsTransferTo) {
                outputChannel = ((SupportsTransferTo) writer).openTransferrableChannel();
              } else {
                // Use default transferrable writable channel anyways in order to have parity with
                // UnsafeShuffleWriter.
                outputChannel = new DefaultTransferrableWritableByteChannel(
                    Channels.newChannel(writer.openStream()));
              }
              outputChannel.transferFrom(inputChannel, 0L, inputChannel.size());
              copyThrewException = false;
            } finally {
              Closeables.close(in, copyThrewException);
              Closeables.close(outputChannel, copyThrewException);
            }
          } else {
            FileInputStream in = new FileInputStream(file);
            OutputStream outputStream = null;
            try {
              outputStream = writer.openStream();
              Utils.copyStream(in, outputStream, false, false);
              copyThrewException = false;
            } finally {
              Closeables.close(in, copyThrewException);
              Closeables.close(outputStream, copyThrewException);
            }
          }
          if (!file.delete()) {
            logger.error("Unable to delete file for partition {}", i);
          }
        }
        lengths[i] = writer.getNumBytesWritten();
      }
    } finally {
      writeMetrics.incWriteTime(System.nanoTime() - writeStartTime);
    }
    partitionWriters = null;
    return lengths;
  }

  @Override
  public Option<MapStatus> stop(boolean success) {
    if (stopping) {
      return None$.empty();
    } else {
      stopping = true;
      if (success) {
        if (mapStatus == null) {
          throw new IllegalStateException("Cannot call stop(true) without having called write()");
        }
        return Option.apply(mapStatus);
      } else {
        // The map task failed, so delete our output data.
        if (partitionWriters != null) {
          try {
            for (DiskBlockObjectWriter writer : partitionWriters) {
              // This method explicitly does _not_ throw exceptions:
              File file = writer.revertPartialWritesAndClose();
              if (!file.delete()) {
                logger.error("Error while deleting file {}", file.getAbsolutePath());
              }
            }
          } finally {
            partitionWriters = null;
          }
        }
        return None$.empty();
      }
    }
  }
}
