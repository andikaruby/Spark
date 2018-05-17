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

package org.apache.spark.sql.execution.streaming.state

import java.io._
import java.util
import java.util.Locale

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal

import com.google.common.io.ByteStreams
import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

import org.apache.spark.{SparkConf, SparkEnv}
import org.apache.spark.internal.Logging
import org.apache.spark.io.LZ4CompressionCodec
import org.apache.spark.sql.catalyst.expressions.UnsafeRow
import org.apache.spark.sql.execution.streaming.CheckpointFileManager
import org.apache.spark.sql.execution.streaming.CheckpointFileManager.CancellableFSDataOutputStream
import org.apache.spark.sql.types.StructType
import org.apache.spark.util.{SizeEstimator, Utils}


/**
 * An implementation of [[StateStoreProvider]] and [[StateStore]] in which all the data is backed
 * by files in a HDFS-compatible file system. All updates to the store has to be done in sets
 * transactionally, and each set of updates increments the store's version. These versions can
 * be used to re-execute the updates (by retries in RDD operations) on the correct version of
 * the store, and regenerate the store version.
 *
 * Usage:
 * To update the data in the state store, the following order of operations are needed.
 *
 * // get the right store
 * - val store = StateStore.get(
 *      StateStoreId(checkpointLocation, operatorId, partitionId), ..., version, ...)
 * - store.put(...)
 * - store.remove(...)
 * - store.commit()    // commits all the updates to made; the new version will be returned
 * - store.iterator()  // key-value data after last commit as an iterator
 * - store.updates()   // updates made in the last commit as an iterator
 *
 * Fault-tolerance model:
 * - Every set of updates is written to a delta file before committing.
 * - The state store is responsible for managing, collapsing and cleaning up of delta files.
 * - Multiple attempts to commit the same version of updates may overwrite each other.
 *   Consistency guarantees depend on whether multiple attempts have the same updates and
 *   the overwrite semantics of underlying file system.
 * - Background maintenance of files ensures that last versions of the store is always recoverable
 * to ensure re-executed RDD operations re-apply updates on the correct past version of the
 * store.
 */
private[state] class HDFSBackedStateStoreProvider extends StateStoreProvider with Logging {

  // ConcurrentHashMap is used because it generates fail-safe iterators on filtering
  // - The iterator is weakly consistent with the map, i.e., iterator's data reflect the values in
  //   the map when the iterator was created
  // - Any updates to the map while iterating through the filtered iterator does not throw
  //   java.util.ConcurrentModificationException
  type MapType = java.util.concurrent.ConcurrentHashMap[UnsafeRow, UnsafeRow]

  /** Implementation of [[StateStore]] API which is backed by a HDFS-compatible file system */
  class HDFSBackedStateStore(val version: Long, mapToUpdate: MapType)
    extends StateStore {

    /** Trait and classes representing the internal state of the store */
    trait STATE
    case object UPDATING extends STATE
    case object COMMITTED extends STATE
    case object ABORTED extends STATE

    private val newVersion = version + 1
    @volatile private var state: STATE = UPDATING
    private val finalDeltaFile: Path = deltaFile(newVersion)
    private lazy val deltaFileStream = fm.createAtomic(finalDeltaFile, overwriteIfPossible = true)
    private lazy val compressedStream = compressStream(deltaFileStream)

    override def id: StateStoreId = HDFSBackedStateStoreProvider.this.stateStoreId

    override def get(key: UnsafeRow): UnsafeRow = {
      mapToUpdate.get(key)
    }

    override def put(key: UnsafeRow, value: UnsafeRow): Unit = {
      verify(state == UPDATING, "Cannot put after already committed or aborted")
      val keyCopy = key.copy()
      val valueCopy = value.copy()
      mapToUpdate.put(keyCopy, valueCopy)
      deltaFileHandler.writeKeyValue(compressedStream, keyCopy, valueCopy)
    }

    override def remove(key: UnsafeRow): Unit = {
      verify(state == UPDATING, "Cannot remove after already committed or aborted")
      val prevValue = mapToUpdate.remove(key)
      if (prevValue != null) {
        deltaFileHandler.writeRemove(compressedStream, key)
      }
    }

    override def getRange(
        start: Option[UnsafeRow],
        end: Option[UnsafeRow]): Iterator[UnsafeRowPair] = {
      verify(state == UPDATING, "Cannot getRange after already committed or aborted")
      iterator()
    }

    /** Commit all the updates that have been made to the store, and return the new version. */
    override def commit(): Long = {
      verify(state == UPDATING, "Cannot commit after already committed or aborted")

      try {
        commitUpdates(newVersion, mapToUpdate, compressedStream)
        state = COMMITTED
        logInfo(s"Committed version $newVersion for $this to file $finalDeltaFile")
        newVersion
      } catch {
        case NonFatal(e) =>
          throw new IllegalStateException(
            s"Error committing version $newVersion into $this", e)
      }
    }

    /** Abort all the updates made on this store. This store will not be usable any more. */
    override def abort(): Unit = {
      // This if statement is to ensure that files are deleted only if there are changes to the
      // StateStore. We have two StateStores for each task, one which is used only for reading, and
      // the other used for read+write. We don't want the read-only to delete state files.
      if (state == UPDATING) {
        state = ABORTED
        deltaFileHandler.cancelFile(compressedStream, deltaFileStream)
      } else {
        state = ABORTED
      }
      logInfo(s"Aborted version $newVersion for $this")
    }

    /**
     * Get an iterator of all the store data.
     * This can be called only after committing all the updates made in the current thread.
     */
    override def iterator(): Iterator[UnsafeRowPair] = {
      val unsafeRowPair = new UnsafeRowPair()
      mapToUpdate.entrySet.asScala.iterator.map { entry =>
        unsafeRowPair.withRows(entry.getKey, entry.getValue)
      }
    }

    override def metrics: StateStoreMetrics = {
      StateStoreMetrics(mapToUpdate.size(), SizeEstimator.estimate(mapToUpdate), Map.empty)
    }

    /**
     * Whether all updates have been committed
     */
    override def hasCommitted: Boolean = {
      state == COMMITTED
    }

    override def toString(): String = {
      s"HDFSStateStore[id=(op=${id.operatorId},part=${id.partitionId}),dir=$baseDir]"
    }
  }

  /** Get the state store for making updates to create a new `version` of the store. */
  override def getStore(version: Long): StateStore = synchronized {
    require(version >= 0, "Version cannot be less than 0")
    val newMap = new MapType()
    if (version > 0) {
      newMap.putAll(loadMap(version))
    }
    val store = new HDFSBackedStateStore(version, newMap)
    logInfo(s"Retrieved version $version of ${HDFSBackedStateStoreProvider.this} for update")
    store
  }

  override def init(
      stateStoreId: StateStoreId,
      keySchema: StructType,
      valueSchema: StructType,
      indexOrdinal: Option[Int], // for sorting the data
      storeConf: StateStoreConf,
      hadoopConf: Configuration): Unit = {
    this.stateStoreId_ = stateStoreId
    this.keySchema = keySchema
    this.valueSchema = valueSchema
    this.storeConf = storeConf
    this.hadoopConf = hadoopConf
    this.numberOfVersionsToRetainInMemory = storeConf.maxVersionsToRetainInMemory
    fm.mkdirs(baseDir)
  }

  override def stateStoreId: StateStoreId = stateStoreId_

  /** Do maintenance backing data files, including creating snapshots and cleaning up old files */
  override def doMaintenance(): Unit = {
    try {
      doSnapshot()
      cleanup()
    } catch {
      case NonFatal(e) =>
        logWarning(s"Error performing snapshot and cleaning up $this")
    }
  }

  override def close(): Unit = {
    loadedMaps.values.asScala.foreach(_.clear())
  }

  override def supportedCustomMetrics: Seq[StateStoreCustomMetric] = {
    Nil
  }

  override def toString(): String = {
    s"HDFSStateStoreProvider[" +
      s"id = (op=${stateStoreId.operatorId},part=${stateStoreId.partitionId}),dir = $baseDir]"
  }

  /* Internal fields and methods */

  @volatile private var stateStoreId_ : StateStoreId = _
  @volatile private var keySchema: StructType = _
  @volatile private var valueSchema: StructType = _
  @volatile private var storeConf: StateStoreConf = _
  @volatile private var hadoopConf: Configuration = _
  @volatile private var numberOfVersionsToRetainInMemory: Int = _

  private lazy val loadedMaps = new util.TreeMap[Long, MapType](Ordering[Long].reverse)
  private lazy val baseDir = stateStoreId.storeCheckpointLocation()
  private lazy val fm = CheckpointFileManager.create(baseDir, hadoopConf)
  private lazy val sparkConf = Option(SparkEnv.get).map(_.conf).getOrElse(new SparkConf)

  private val deltaFileHandler: DeltaFileHandler = new DeltaFileHandler
  private val snapshotFileHandler: SnapshotFileHandler = new SnapshotFileHandler

  private case class StoreFile(version: Long, path: Path, isSnapshot: Boolean)

  private def commitUpdates(newVersion: Long, map: MapType, output: DataOutputStream): Unit = {
    synchronized {
      deltaFileHandler.finalizeWriting(output)
      putStateIntoStateCacheMap(newVersion, map)
    }
  }

  /**
   * Get iterator of all the data of the latest version of the store.
   * Note that this will look up the files to determined the latest known version.
   */
  private[state] def latestIterator(): Iterator[UnsafeRowPair] = synchronized {
    val versionsInFiles = fetchFiles().map(_.version).toSet
    val versionsLoaded = loadedMaps.keySet.asScala
    val allKnownVersions = versionsInFiles ++ versionsLoaded
    val unsafeRowTuple = new UnsafeRowPair()
    if (allKnownVersions.nonEmpty) {
      loadMap(allKnownVersions.max).entrySet().iterator().asScala.map { entry =>
        unsafeRowTuple.withRows(entry.getKey, entry.getValue)
      }
    } else Iterator.empty
  }

  /** This method is intended to be only used for unit test(s). DO NOT TOUCH ELEMENTS IN MAP! */
  private[state] def getLoadedMaps(): util.SortedMap[Long, MapType] = synchronized {
    // shallow copy as a minimal guard
    loadedMaps.clone().asInstanceOf[util.SortedMap[Long, MapType]]
  }

  private def putStateIntoStateCacheMap(newVersion: Long, map: MapType): Unit = synchronized {
    if (numberOfVersionsToRetainInMemory <= 0) {
      if (loadedMaps.size() > 0) loadedMaps.clear()
      return
    }

    while (loadedMaps.size() > numberOfVersionsToRetainInMemory) {
      loadedMaps.remove(loadedMaps.lastKey())
    }

    val size = loadedMaps.size()
    if (size == numberOfVersionsToRetainInMemory) {
      val versionIdForLastKey = loadedMaps.lastKey()
      if (versionIdForLastKey > newVersion) {
        // this is the only case which we can avoid putting, because new version will be placed to
        // the last key and it should be evicted right away
        return
      } else if (versionIdForLastKey < newVersion) {
        // this case needs removal of the last key before putting new one
        loadedMaps.remove(versionIdForLastKey)
      }
    }

    loadedMaps.put(newVersion, map)
  }

  /** Load the required version of the map data from the backing files */
  private def loadMap(version: Long): MapType = {

    // Shortcut if the map for this version is already there to avoid a redundant put.
    val loadedCurrentVersionMap = synchronized { Option(loadedMaps.get(version)) }
    if (loadedCurrentVersionMap.isDefined) {
      return loadedCurrentVersionMap.get
    }

    logWarning(s"The state for version $version doesn't exist in loadedMaps. " +
      "Reading snapshot file and delta files if needed..." +
      "Note that this is normal for the first batch of starting query.")

    val (result, elapsedMs) = Utils.timeTakenMs {
      val snapshotCurrentVersionMap = snapshotFileHandler.readFromFile(version)
      if (snapshotCurrentVersionMap.isDefined) {
        synchronized { putStateIntoStateCacheMap(version, snapshotCurrentVersionMap.get) }
        return snapshotCurrentVersionMap.get
      }

      // Find the most recent map before this version that we can.
      // [SPARK-22305] This must be done iteratively to avoid stack overflow.
      var lastAvailableVersion = version
      var lastAvailableMap: Option[MapType] = None
      while (lastAvailableMap.isEmpty) {
        lastAvailableVersion -= 1

        if (lastAvailableVersion <= 0) {
          // Use an empty map for versions 0 or less.
          lastAvailableMap = Some(new MapType)
        } else {
          lastAvailableMap =
            synchronized { Option(loadedMaps.get(lastAvailableVersion)) }
              .orElse(snapshotFileHandler.readFromFile(lastAvailableVersion))
        }
      }

      // Load all the deltas from the version after the last available one up to the target version.
      // The last available version is the one with a full snapshot, so it doesn't need deltas.
      val resultMap = new MapType(lastAvailableMap.get)
      for (deltaVersion <- lastAvailableVersion + 1 to version) {
        deltaFileHandler.updateFromDeltaFile(deltaVersion, resultMap)
      }

      synchronized { putStateIntoStateCacheMap(version, resultMap) }
      resultMap
    }

    logDebug(s"Loading state for $version takes $elapsedMs ms.")

    result
  }

  /**
   * Helper to manipulate HDFS backed state store file.
   *
   * Key/Value is written to store file as follow:
   *
   * Key Size (Int) | Key (bytes) | Value Size (Int) | Value (bytes)
   *
   * and removal of key is written to store file as follow:
   *
   * Key Size (Int) | Key (bytes) | -1 (magic number for tombstone)
   *
   * Magic number "-1" is written at the end of file to represent EOF, which enables to determine
   * whether the file is finalized or not.
   */
  private trait HDFSBackedStateStoreFileOps {
    val MAGIC_NUMBER_VALUE_END_OF_FILE: Int = -1
    val MAGIC_NUMBER_VALUE_TOMBSTONE: Int = -1

    def writeKeyValue(output: DataOutputStream, key: UnsafeRow, value: UnsafeRow): Unit = {
      val keyBytes = key.getBytes()
      val valueBytes = value.getBytes()
      output.writeInt(keyBytes.size)
      output.write(keyBytes)
      output.writeInt(valueBytes.size)
      output.write(valueBytes)
    }

    def writeRemove(output: DataOutputStream, key: UnsafeRow): Unit = {
      val keyBytes = key.getBytes()
      output.writeInt(keyBytes.size)
      output.write(keyBytes)
      output.writeInt(MAGIC_NUMBER_VALUE_TOMBSTONE)
    }

    def finalizeWriting(output: DataOutputStream): Unit = {
      output.writeInt(MAGIC_NUMBER_VALUE_END_OF_FILE)
      output.close()
    }

    def readSize(input: DataInputStream): Int = {
      input.readInt()
    }

    def readKeyData(input: DataInputStream, schema: StructType, size: Int): UnsafeRow = {
      val rowBuffer = new Array[Byte](size)
      ByteStreams.readFully(input, rowBuffer, 0, size)

      val row = new UnsafeRow(schema.fields.length)
      row.pointTo(rowBuffer, size)

      row
    }

    def readValueData(input: DataInputStream, schema: StructType, size: Int): UnsafeRow = {
      val valueRowBuffer = new Array[Byte](size)
      ByteStreams.readFully(input, valueRowBuffer, 0, size)

      val row = new UnsafeRow(schema.fields.length)

      // If valueSize in existing file is not multiple of 8, floor it to multiple of 8.
      // This is a workaround for the following:
      // Prior to Spark 2.3 mistakenly append 4 bytes to the value row in
      // `RowBasedKeyValueBatch`, which gets persisted into the checkpoint data
      row.pointTo(valueRowBuffer, (size / 8) * 8)

      row
    }

    def updateMapFromFile(fileToRead: Path, map: MapType, enableDeletingRow: Boolean): Unit = {
      var input: DataInputStream = null
      val sourceStream = fm.open(fileToRead)
      try {
        input = decompressStream(sourceStream)
        var eof = false

        while(!eof) {
          val keySize = readSize(input)
          if (keySize == MAGIC_NUMBER_VALUE_END_OF_FILE) {
            eof = true
          } else if (keySize < 0) {
            throw new IOException(
              s"Error reading file $fileToRead of $this: key size cannot be $keySize")
          } else {
            val keyRow = readKeyData(input, keySchema, keySize)

            val valueSize = readSize(input)
            if (enableDeletingRow && valueSize == MAGIC_NUMBER_VALUE_TOMBSTONE) {
              map.remove(keyRow)
            } else if (valueSize < 0) {
              throw new IOException(
                s"Error reading file $fileToRead of $this: value size cannot be $valueSize")
            } else {
              val valueRow = readValueData(input, valueSchema, valueSize)
              map.put(keyRow, valueRow)
            }
          }
        }
      } finally {
        if (input != null) input.close()
      }
    }

    def dumpMapToFile(fileToWrite: Path, map: MapType): Unit = {
      var rawOutput: CancellableFSDataOutputStream = null
      var output: DataOutputStream = null
      try {
        rawOutput = fm.createAtomic(fileToWrite, overwriteIfPossible = true)
        output = compressStream(rawOutput)
        val iter = map.entrySet().iterator()
        while(iter.hasNext) {
          val entry = iter.next()
          writeKeyValue(output, entry.getKey, entry.getValue)
        }
        finalizeWriting(output)
      } catch {
        case e: Throwable =>
          cancelFile(compressedStream = output, rawStream = rawOutput)
          throw e
      }
    }

    /**
     * Try to cancel the underlying stream and safely close the compressed stream.
     *
     * @param compressedStream the compressed stream.
     * @param rawStream the underlying stream which needs to be cancelled.
     */
    def cancelFile(compressedStream: DataOutputStream,
                   rawStream: CancellableFSDataOutputStream): Unit = {
      try {
        if (rawStream != null) rawStream.cancel()
        IOUtils.closeQuietly(compressedStream)
      } catch {
        case e: FSError if e.getCause.isInstanceOf[IOException] =>
        // Closing the compressedStream causes the stream to write/flush flush data into the
        // rawStream. Since the rawStream is already closed, there may be errors.
        // Usually its an IOException. However, Hadoop's RawLocalFileSystem wraps
        // IOException into FSError.
      }
    }

  }

  /** Handler of delta file. Write operations are defined in [[HDFSBackedStateStoreFileOps]]. */
  private class DeltaFileHandler extends HDFSBackedStateStoreFileOps {
    def updateFromDeltaFile(version: Long, map: MapType): Unit = {
      val fileToRead = deltaFile(version)
      try {
        updateMapFromFile(fileToRead, map, enableDeletingRow = true)
      } catch {
        case f: FileNotFoundException =>
          throw new IllegalStateException(
            s"Error reading delta file $fileToRead of $this: $fileToRead does not exist", f)
      }
      logInfo(s"Read delta file for version $version of $this from $fileToRead")
    }
  }

  /** Handler of snapshot file. */
  private class SnapshotFileHandler extends HDFSBackedStateStoreFileOps {
    def readFromFile(version: Long): Option[MapType] = {
      val fileToRead = snapshotFile(version)
      val map = new MapType()

      try {
        updateMapFromFile(fileToRead, map, enableDeletingRow = false)
        logInfo(s"Read snapshot file for version $version of $this from $fileToRead")
        Some(map)
      } catch {
        case _: FileNotFoundException =>
          None
      }
    }

    def writeToFile(version: Long, map: MapType): Unit = {
      val targetFile = snapshotFile(version)
      dumpMapToFile(targetFile, map)
      logInfo(s"Written snapshot file for version $version of $this at $targetFile")
    }
  }

  /** Perform a snapshot of the store to allow delta files to be consolidated */
  private def doSnapshot(): Unit = {
    try {
      val (files, e1) = Utils.timeTakenMs(fetchFiles())
      logDebug(s"fetchFiles() took $e1 ms.")

      if (files.nonEmpty) {
        val lastVersion = files.last.version
        val deltaFilesForLastVersion =
          filesForVersion(files, lastVersion).filter(_.isSnapshot == false)
        synchronized { Option(loadedMaps.get(lastVersion)) } match {
          case Some(map) =>
            if (deltaFilesForLastVersion.size > storeConf.minDeltasForSnapshot) {
              val (_, e2) = Utils.timeTakenMs(snapshotFileHandler.writeToFile(lastVersion, map))
              logDebug(s"writeSnapshotFile() took $e2 ms.")
            }
          case None =>
            // The last map is not loaded, probably some other instance is in charge
        }
      }
    } catch {
      case NonFatal(e) =>
        logWarning(s"Error doing snapshots for $this", e)
    }
  }

  /**
   * Clean up old snapshots and delta files that are not needed any more. It ensures that last
   * few versions of the store can be recovered from the files, so re-executed RDD operations
   * can re-apply updates on the past versions of the store.
   */
  private[state] def cleanup(): Unit = {
    try {
      val (files, e1) = Utils.timeTakenMs(fetchFiles())
      logDebug(s"fetchFiles() took $e1 ms.")

      if (files.nonEmpty) {
        val earliestVersionToRetain = files.last.version - storeConf.minVersionsToRetain
        if (earliestVersionToRetain > 0) {
          val earliestFileToRetain = filesForVersion(files, earliestVersionToRetain).head
          val filesToDelete = files.filter(_.version < earliestFileToRetain.version)
          val (_, e2) = Utils.timeTakenMs {
            filesToDelete.foreach { f =>
              fm.delete(f.path)
            }
          }
          logDebug(s"deleting files took $e2 ms.")
          logInfo(s"Deleted files older than ${earliestFileToRetain.version} for $this: " +
            filesToDelete.mkString(", "))
        }
      }
    } catch {
      case NonFatal(e) =>
        logWarning(s"Error cleaning up files for $this", e)
    }
  }

  /** Files needed to recover the given version of the store */
  private def filesForVersion(allFiles: Seq[StoreFile], version: Long): Seq[StoreFile] = {
    require(version >= 0)
    require(allFiles.exists(_.version == version))

    val latestSnapshotFileBeforeVersion = allFiles
      .filter(_.isSnapshot == true)
      .takeWhile(_.version <= version)
      .lastOption
    val deltaBatchFiles = latestSnapshotFileBeforeVersion match {
      case Some(snapshotFile) =>

        val deltaFiles = allFiles.filter { file =>
          file.version > snapshotFile.version && file.version <= version
        }.toList
        verify(
          deltaFiles.size == version - snapshotFile.version,
          s"Unexpected list of delta files for version $version for $this: $deltaFiles"
        )
        deltaFiles

      case None =>
        allFiles.takeWhile(_.version <= version)
    }
    latestSnapshotFileBeforeVersion.toSeq ++ deltaBatchFiles
  }

  /** Fetch all the files that back the store */
  private def fetchFiles(): Seq[StoreFile] = {
    val files: Seq[FileStatus] = try {
      fm.list(baseDir)
    } catch {
      case _: java.io.FileNotFoundException =>
        Seq.empty
    }
    val versionToFiles = new mutable.HashMap[Long, StoreFile]
    files.foreach { status =>
      val path = status.getPath
      val nameParts = path.getName.split("\\.")
      if (nameParts.size == 2) {
        val version = nameParts(0).toLong
        nameParts(1).toLowerCase(Locale.ROOT) match {
          case "delta" =>
            // ignore the file otherwise, snapshot file already exists for that batch id
            if (!versionToFiles.contains(version)) {
              versionToFiles.put(version, StoreFile(version, path, isSnapshot = false))
            }
          case "snapshot" =>
            versionToFiles.put(version, StoreFile(version, path, isSnapshot = true))
          case _ =>
            logWarning(s"Could not identify file $path for $this")
        }
      }
    }
    val storeFiles = versionToFiles.values.toSeq.sortBy(_.version)
    logDebug(s"Current set of files for $this: ${storeFiles.mkString(", ")}")
    storeFiles
  }

  private def compressStream(outputStream: DataOutputStream): DataOutputStream = {
    val compressed = new LZ4CompressionCodec(sparkConf).compressedOutputStream(outputStream)
    new DataOutputStream(compressed)
  }

  private def decompressStream(inputStream: DataInputStream): DataInputStream = {
    val compressed = new LZ4CompressionCodec(sparkConf).compressedInputStream(inputStream)
    new DataInputStream(compressed)
  }

  private def deltaFile(version: Long): Path = {
    new Path(baseDir, s"$version.delta")
  }

  private def snapshotFile(version: Long): Path = {
    new Path(baseDir, s"$version.snapshot")
  }

  private def verify(condition: => Boolean, msg: String): Unit = {
    if (!condition) {
      throw new IllegalStateException(msg)
    }
  }
}
