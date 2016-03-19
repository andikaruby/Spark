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

import java.io.File

import scala.collection.mutable
import scala.util.Random

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.scalatest.{BeforeAndAfter, PrivateMethodTester}
import org.scalatest.concurrent.Eventually._
import org.scalatest.time.SpanSugar._

import org.apache.spark.{SparkConf, SparkContext, SparkFunSuite}
import org.apache.spark.LocalSparkContext._
import org.apache.spark.sql.catalyst.expressions.{GenericInternalRow, UnsafeProjection, UnsafeRow}
import org.apache.spark.sql.catalyst.util.quietly
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String
import org.apache.spark.util.Utils

class StateStoreSuite extends SparkFunSuite with BeforeAndAfter with PrivateMethodTester {
  type MapType = mutable.HashMap[UnsafeRow, UnsafeRow]

  import HDFSBackedStateStoreProvider._
  import StateStoreCoordinatorSuite._
  import StateStoreSuite._

  private val tempDir = Utils.createTempDir().toString
  private val keySchema = StructType(Seq(StructField("key", StringType, true)))
  private val valueSchema = StructType(Seq(StructField("value", IntegerType, true)))

  after {
    StateStore.stop()
  }

  test("update, remove, commit, and all data iterator") {
    val provider = newStoreProvider()

    // Verify state before starting a new set of updates
    assert(provider.latestIterator().isEmpty)

    val store = provider.getStore(0)
    assert(!store.hasCommitted)
    intercept[IllegalStateException] {
      store.iterator()
    }
    intercept[IllegalStateException] {
      store.updates()
    }

    // Verify state after updating
    update(store, "a", 1)
    intercept[IllegalStateException] {
      store.iterator()
    }
    intercept[IllegalStateException] {
      store.updates()
    }
    assert(provider.latestIterator().isEmpty)

    // Make updates, commit and then verify state
    update(store, "b", 2)
    update(store, "aa", 3)
    remove(store, _.startsWith("a"))
    assert(store.commit() === 1)

    assert(store.hasCommitted)
    assert(rowsToSet(store.iterator()) === Set("b" -> 2))
    assert(rowsToSet(provider.latestIterator()) === Set("b" -> 2))
    assert(fileExists(provider, version = 1, isSnapshot = false))

    assert(getDataFromFiles(provider) === Set("b" -> 2))

    // Trying to get newer versions should fail
    intercept[Exception] {
      provider.getStore(2)
    }
    intercept[Exception] {
      getDataFromFiles(provider, 2)
    }

    // New updates to the reloaded store with new version, and does not change old version
    val reloadedProvider = new HDFSBackedStateStoreProvider(
      store.id, keySchema, valueSchema, new SparkConf, new Configuration)
    val reloadedStore = reloadedProvider.getStore(1)
    update(reloadedStore, "c", 4)
    assert(reloadedStore.commit() === 2)
    assert(rowsToSet(reloadedStore.iterator()) === Set("b" -> 2, "c" -> 4))
    assert(getDataFromFiles(provider) === Set("b" -> 2, "c" -> 4))
    assert(getDataFromFiles(provider, version = 1) === Set("b" -> 2))
    assert(getDataFromFiles(provider, version = 2) === Set("b" -> 2, "c" -> 4))
  }

  test("updates iterator with all combos of updates and removes") {
    val provider = newStoreProvider()
    var currentVersion: Int = 0
    def withStore(body: StateStore => Unit): Unit = {
      val store = provider.getStore(currentVersion)
      body(store)
      currentVersion += 1
    }

    // New data should be seen in updates as value added, even if they had multiple updates
    withStore { store =>
      update(store, "a", 1)
      update(store, "aa", 1)
      update(store, "aa", 2)
      store.commit()
      assert(updatesToSet(store.updates()) === Set(Added("a", 1), Added("aa", 2)))
      assert(rowsToSet(store.iterator()) === Set("a" -> 1, "aa" -> 2))
    }

    // Multiple updates to same key should be collapsed in the updates as a single value update
    // Keys that have not been updated should not appear in the updates
    withStore { store =>
      update(store, "a", 4)
      update(store, "a", 6)
      store.commit()
      assert(updatesToSet(store.updates()) === Set(Updated("a", 6)))
      assert(rowsToSet(store.iterator()) === Set("a" -> 6, "aa" -> 2))
    }

    // Keys added, updated and finally removed before commit should not appear in updates
    withStore { store =>
      update(store, "b", 4)     // Added, finally removed
      update(store, "bb", 5)    // Added, updated, finally removed
      update(store, "bb", 6)
      remove(store, _.startsWith("b"))
      store.commit()
      assert(updatesToSet(store.updates()) === Set.empty)
      assert(rowsToSet(store.iterator()) === Set("a" -> 6, "aa" -> 2))
    }

    // Removed data should be seen in updates as a key removed
    // Removed, but re-added data should be seen in updates as a value update
    withStore { store =>
      remove(store, _.startsWith("a"))
      update(store, "a", 10)
      store.commit()
      assert(updatesToSet(store.updates()) === Set(Updated("a", 10), Removed("aa")))
      assert(rowsToSet(store.iterator()) === Set("a" -> 10))
    }
  }

  test("cancel") {
    val provider = newStoreProvider()
    val store = provider.getStore(0)
    update(store, "a", 1)
    store.commit()
    assert(rowsToSet(store.iterator()) === Set("a" -> 1))

    // cancelUpdates should not change the data in the files
    val store1 = provider.getStore(1)
    update(store1, "b", 1)
    store1.cancel()
    assert(getDataFromFiles(provider) === Set("a" -> 1))
  }

  test("getStore with unexpected versions") {
    val provider = newStoreProvider()

    intercept[IllegalArgumentException] {
      provider.getStore(-1)
    }

    // Prepare some data in the stoer
    val store = provider.getStore(0)
    update(store, "a", 1)
    assert(store.commit() === 1)
    assert(rowsToSet(store.iterator()) === Set("a" -> 1))

    intercept[IllegalStateException] {
      provider.getStore(2)
    }

    // Update store version with some data
    val store1 = provider.getStore(1)
    update(store1, "b", 1)
    assert(store1.commit() === 2)
    assert(rowsToSet(store1.iterator()) === Set("a" -> 1, "b" -> 1))
    assert(getDataFromFiles(provider) === Set("a" -> 1, "b" -> 1))

    // Overwrite the version with other data
    val store2 = provider.getStore(1)
    update(store2, "c", 1)
    assert(store2.commit() === 2)
    assert(rowsToSet(store2.iterator()) === Set("a" -> 1, "c" -> 1))
    assert(getDataFromFiles(provider) === Set("a" -> 1, "c" -> 1))
  }

  test("snapshotting") {
    val provider = newStoreProvider(maxDeltaChainForSnapshots = 5)

    var currentVersion = 0
    def updateVersionTo(targetVersion: Int): Unit = {
      for (i <- currentVersion + 1 to targetVersion) {
        val store = provider.getStore(currentVersion)
        update(store, "a", i)
        store.commit()
        currentVersion += 1
      }
      require(currentVersion === targetVersion)
    }

    updateVersionTo(2)
    require(getDataFromFiles(provider) === Set("a" -> 2))
    provider.doMaintenance()               // should not generate snapshot files
    assert(getDataFromFiles(provider) === Set("a" -> 2))

    for (i <- 1 to currentVersion) {
      assert(fileExists(provider, i, isSnapshot = false))  // all delta files present
      assert(!fileExists(provider, i, isSnapshot = true))  // no snapshot files present
    }

    // After version 6, snapshotting should generate one snapshot file
    updateVersionTo(6)
    require(getDataFromFiles(provider) === Set("a" -> 6), "store not updated correctly")
    provider.doMaintenance()       // should generate snapshot files
    assert(getDataFromFiles(provider) === Set("a" -> 6), "snapshotting messed up the data")
    assert(getDataFromFiles(provider) === Set("a" -> 6))

    val snapshotVersion = (0 to 6).find(version => fileExists(provider, version, isSnapshot = true))
    assert(snapshotVersion.nonEmpty, "snapshot file not generated")

    // After version 20, snapshotting should generate newer snapshot files
    updateVersionTo(20)
    require(getDataFromFiles(provider) === Set("a" -> 20), "store not updated correctly")
    provider.doMaintenance()       // do snapshot
    assert(getDataFromFiles(provider) === Set("a" -> 20), "snapshotting messed up the data")
    assert(getDataFromFiles(provider) === Set("a" -> 20))

    val latestSnapshotVersion = (0 to 20).filter(version =>
      fileExists(provider, version, isSnapshot = true)).lastOption
    assert(latestSnapshotVersion.nonEmpty, "no snapshot file found")
    assert(latestSnapshotVersion.get > snapshotVersion.get, "newer snapshot not generated")

  }

  test("cleaning") {
    val provider = newStoreProvider(maxDeltaChainForSnapshots = 5)

    for (i <- 1 to 20) {
      val store = provider.getStore(i - 1)
      update(store, "a", i)
      store.commit()
      provider.doMaintenance() // do cleanup
    }
    require(
      rowsToSet(provider.latestIterator()) === Set("a" -> 20),
      "store not updated correctly")

    assert(!fileExists(provider, version = 1, isSnapshot = false)) // first file should be deleted

    // last couple of versions should be retrievable
    assert(getDataFromFiles(provider, 20) === Set("a" -> 20))
    assert(getDataFromFiles(provider, 19) === Set("a" -> 19))
  }

  test("StateStore.get") {
    quietly {
      val dir = Utils.createDirectory(tempDir, Random.nextString(5)).toString
      val storeId = StateStoreId(dir, 0, 0)

      // Verify that trying to get incorrect versions throw errors
      intercept[IllegalArgumentException] {
        StateStore.get(storeId, keySchema, valueSchema, -1, new Configuration)
      }
      assert(!StateStore.isLoaded(storeId)) // version -1 should not attempt to load the store

      intercept[IllegalStateException] {
        StateStore.get(storeId, keySchema, valueSchema, 1, new Configuration)
      }

      // Increase version of the store
      val store0 = StateStore.get(storeId, keySchema, valueSchema, 0, new Configuration)
      assert(store0.version === 0)
      update(store0, "a", 1)
      store0.commit()

      assert(StateStore.get(storeId, keySchema, valueSchema, 1, new Configuration).version == 1)
      assert(StateStore.get(storeId, keySchema, valueSchema, 0, new Configuration).version == 0)

      // Verify that you can remove the store and still reload and use it
      StateStore.remove(storeId)
      assert(!StateStore.isLoaded(storeId))

      val store1 = StateStore.get(storeId, keySchema, valueSchema, 1, new Configuration)
      assert(StateStore.isLoaded(storeId))
      update(store1, "a", 2)
      assert(store1.commit() === 2)
      assert(rowsToSet(store1.iterator()) === Set("a" -> 2))
    }
  }

  test("background management") {
    val conf = new SparkConf()
      .setMaster("local")
      .setAppName("test")
      .set(StateStore.MAINTENANCE_INTERVAL_CONFIG, "10ms")
    val opId = 0
    val dir = Utils.createDirectory(tempDir, Random.nextString(5)).toString
    val storeId = StateStoreId(dir, opId, 0)
    val provider = new HDFSBackedStateStoreProvider(
      storeId, keySchema, valueSchema, conf, new Configuration)

    quietly {
      withSpark(new SparkContext(conf)) { sc =>
        withCoordinator(sc) { coordinator =>
          for (i <- 1 to 20) {
            val store = StateStore.get(storeId, keySchema, valueSchema, i - 1, new Configuration)
            update(store, "a", i)
            store.commit()
          }

          // Background management should clean up and generate snapshots
          eventually(timeout(4 seconds)) {
            // Earliest delta file should get cleaned up
            assert(!fileExists(provider, 1, isSnapshot = false), "earliest file not deleted")

            // Some snapshots should have been generated
            val snapshotVersions = (0 to 20).filter { version =>
              fileExists(provider, version, isSnapshot = true)
            }
            assert(snapshotVersions.nonEmpty, "no snapshot file found")
          }

          // If driver decides to deactivate all instances of the store, then this instance
          // should be unloaded
          coordinator.deactivateInstances(dir)
          eventually(timeout(4 seconds)) {
            assert(!StateStore.isLoaded(storeId))
          }

          // Reload the store and verify
          StateStore.get(storeId, keySchema, valueSchema, 20, new Configuration)
          assert(StateStore.isLoaded(storeId))

          // If some other executor loads the store, then this instance should be unloaded
          coordinator.reportActiveInstance(storeId, "other-host", "other-exec")
          eventually(timeout(4 seconds)) {
            assert(!StateStore.isLoaded(storeId))
          }

          // Reload the store and verify
          StateStore.get(storeId, keySchema, valueSchema, 20, new Configuration)
          assert(StateStore.isLoaded(storeId))
        }
      }

      // Verify if instance is unloaded if SparkContext is stopped
      eventually(timeout(4 seconds)) {
        assert(!StateStore.isLoaded(storeId))
      }
    }
  }

  def getDataFromFiles(
      provider: HDFSBackedStateStoreProvider,
    version: Int = -1): Set[(String, Int)] = {
    val reloadedProvider = new HDFSBackedStateStoreProvider(
      provider.id, keySchema, valueSchema, new SparkConf, new Configuration)
    if (version < 0) {
      reloadedProvider.latestIterator().map(rowsToStringInt).toSet
    } else {
      reloadedProvider.iterator(version).map(rowsToStringInt).toSet
    }
  }

  def assertMap(
      testMapOption: Option[MapType],
      expectedMap: Map[String, Int]): Unit = {
    assert(testMapOption.nonEmpty, "no map present")
    val convertedMap = testMapOption.get.map(rowsToStringInt)
    assert(convertedMap === expectedMap)
  }

  def fileExists(
      provider: HDFSBackedStateStoreProvider,
      version: Long,
      isSnapshot: Boolean): Boolean = {
    val method = PrivateMethod[Path]('baseDir)
    val basePath = provider invokePrivate method()
    val fileName = if (isSnapshot) s"$version.snapshot" else s"$version.delta"
    val filePath = new File(basePath.toString, fileName)
    filePath.exists
  }

  def storeLoaded(storeId: StateStoreId): Boolean = {
    val method = PrivateMethod[mutable.HashMap[StateStoreId, StateStore]]('loadedStores)
    val loadedStores = StateStore invokePrivate method()
    loadedStores.contains(storeId)
  }

  def unloadStore(storeId: StateStoreId): Boolean = {
    val method = PrivateMethod('remove)
    StateStore invokePrivate method(storeId)
  }

  def newStoreProvider(
      opId: Long = Random.nextLong,
      partition: Int = 0,
      maxDeltaChainForSnapshots: Int = MAX_DELTA_CHAIN_FOR_SNAPSHOTS_DEFAULT
    ): HDFSBackedStateStoreProvider = {
    val dir = Utils.createDirectory(tempDir, Random.nextString(5)).toString
    val sparkConf = new SparkConf()
      .set(MAX_DELTA_CHAIN_FOR_SNAPSHOTS_CONF, maxDeltaChainForSnapshots.toString)
    new HDFSBackedStateStoreProvider(
      StateStoreId(dir, opId, partition),
      keySchema,
      valueSchema,
      sparkConf,
      new Configuration())
  }

  def remove(store: StateStore, condition: String => Boolean): Unit = {
    store.remove(row => condition(rowToString(row)))
  }

  private def update(store: StateStore, key: String, value: Int): Unit = {
    store.update(stringToRow(key), _ => intToRow(value))
  }
}

private[state] object StateStoreSuite {

  /** Trait and classes mirroring [[StoreUpdate]] for testing store updates iterator */
  trait TestUpdate
  case class Added(key: String, value: Int) extends TestUpdate
  case class Updated(key: String, value: Int) extends TestUpdate
  case class Removed(key: String) extends TestUpdate

  val strProj = UnsafeProjection.create(Array[DataType](StringType))
  val intProj = UnsafeProjection.create(Array[DataType](IntegerType))

  def stringToRow(s: String): UnsafeRow = {
    strProj.apply(new GenericInternalRow(Array[Any](UTF8String.fromString(s)))).copy()
  }

  def intToRow(i: Int): UnsafeRow = {
    intProj.apply(new GenericInternalRow(Array[Any](i))).copy()
  }

  def rowToString(row: UnsafeRow): String = {
    row.getUTF8String(0).toString
  }

  def rowToInt(row: UnsafeRow): Int = {
    row.getInt(0)
  }

  def rowsToIntInt(row: (UnsafeRow, UnsafeRow)): (Int, Int) = {
    (rowToInt(row._1), rowToInt(row._2))
  }


  def rowsToStringInt(row: (UnsafeRow, UnsafeRow)): (String, Int) = {
    (rowToString(row._1), rowToInt(row._2))
  }

  def rowsToSet(iterator: Iterator[(UnsafeRow, UnsafeRow)]): Set[(String, Int)] = {
    iterator.map(rowsToStringInt).toSet
  }

  def updatesToSet(iterator: Iterator[StoreUpdate]): Set[TestUpdate] = {
    iterator.map { _ match {
      case ValueAdded(key, value) => Added(rowToString(key), rowToInt(value))
      case ValueUpdated(key, value) => Updated(rowToString(key), rowToInt(value))
      case KeyRemoved(key) => Removed(rowToString(key))
    }}.toSet
  }
}
