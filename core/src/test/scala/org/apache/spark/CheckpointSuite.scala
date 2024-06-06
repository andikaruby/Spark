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

package org.apache.spark

import java.io.File

import scala.reflect.ClassTag

import com.google.common.io.ByteStreams
import org.apache.hadoop.fs.Path

import org.apache.spark.internal.config.CACHE_CHECKPOINT_PREFERRED_LOCS_EXPIRE_TIME
import org.apache.spark.internal.config.UI._
import org.apache.spark.io.CompressionCodec
import org.apache.spark.rdd._
import org.apache.spark.shuffle.FetchFailedException
import org.apache.spark.storage.{BlockId, StorageLevel, TestBlockId}
import org.apache.spark.util.ArrayImplicits._
import org.apache.spark.util.Utils

trait RDDCheckpointTester { self: SparkFunSuite =>

  protected val partitioner = new HashPartitioner(2)

  private def defaultCollectFunc[T](rdd: RDD[T]): Any = rdd.collect()

  /** Implementations of this trait must implement this method */
  protected def sparkContext: SparkContext

  /**
   * Test checkpointing of the RDD generated by the given operation. It tests whether the
   * serialized size of the RDD is reduce after checkpointing or not. This function should be called
   * on all RDDs that have a parent RDD (i.e., do not call on ParallelCollection, BlockRDD, etc.).
   *
   * @param op an operation to run on the RDD
   * @param reliableCheckpoint if true, use reliable checkpoints, otherwise use local checkpoints
   * @param collectFunc a function for collecting the values in the RDD, in case there are
   *                    non-comparable types like arrays that we want to convert to something
   *                    that supports ==
   */
  protected def testRDD[U: ClassTag](
      op: (RDD[Int]) => RDD[U],
      reliableCheckpoint: Boolean,
      collectFunc: RDD[U] => Any = defaultCollectFunc[U] _): Unit = {
    // Generate the final RDD using given RDD operation
    val baseRDD = generateFatRDD()
    val operatedRDD = op(baseRDD)
    val parentDependency = operatedRDD.dependencies.headOption.orNull
    val rddType = operatedRDD.getClass.getSimpleName
    val numPartitions = operatedRDD.partitions.length

    // Force initialization of all the data structures in RDDs
    // Without this, serializing the RDD will give a wrong estimate of the size of the RDD
    initializeRdd(operatedRDD)

    val partitionsBeforeCheckpoint = operatedRDD.partitions

    // Find serialized sizes before and after the checkpoint
    logInfo("RDD before checkpoint: " + operatedRDD + "\n" + operatedRDD.toDebugString)
    val (rddSizeBeforeCheckpoint, partitionSizeBeforeCheckpoint) = getSerializedSizes(operatedRDD)
    checkpoint(operatedRDD, reliableCheckpoint)
    val result = collectFunc(operatedRDD)
    operatedRDD.collect() // force re-initialization of post-checkpoint lazy variables
    val (rddSizeAfterCheckpoint, partitionSizeAfterCheckpoint) = getSerializedSizes(operatedRDD)
    logInfo("RDD after checkpoint: " + operatedRDD + "\n" + operatedRDD.toDebugString)

    // Test whether the checkpoint file has been created
    if (reliableCheckpoint) {
      assert(operatedRDD.getCheckpointFile.nonEmpty)
      val recoveredRDD = sparkContext.checkpointFile[U](operatedRDD.getCheckpointFile.get)
      assert(collectFunc(recoveredRDD) === result)
      assert(recoveredRDD.partitioner === operatedRDD.partitioner)
    }

    // Test whether dependencies have been changed from its earlier parent RDD
    assert(operatedRDD.dependencies.head != parentDependency)

    // Test whether the partitions have been changed from its earlier partitions
    assert(operatedRDD.partitions.toList != partitionsBeforeCheckpoint.toList)

    // Test whether the partitions have been changed to the new Hadoop partitions
    assert(operatedRDD.partitions.toList === operatedRDD.checkpointData.get.getPartitions.toList)

    // Test whether the number of partitions is same as before
    assert(operatedRDD.partitions.length === numPartitions)

    // Test whether the data in the checkpointed RDD is same as original
    assert(collectFunc(operatedRDD) === result)

    // Test whether serialized size of the RDD has reduced.
    logInfo("Size of " + rddType +
      " [" + rddSizeBeforeCheckpoint + " --> " + rddSizeAfterCheckpoint + "]")
    assert(
      rddSizeAfterCheckpoint < rddSizeBeforeCheckpoint,
      "Size of " + rddType + " did not reduce after checkpointing " +
        " [" + rddSizeBeforeCheckpoint + " --> " + rddSizeAfterCheckpoint + "]"
    )
  }

  /**
   * Test whether checkpointing of the parent of the generated RDD also
   * truncates the lineage or not. Some RDDs like CoGroupedRDD hold on to its parent
   * RDDs partitions. So even if the parent RDD is checkpointed and its partitions changed,
   * the generated RDD will remember the partitions and therefore potentially the whole lineage.
   * This function should be called only those RDD whose partitions refer to parent RDD's
   * partitions (i.e., do not call it on simple RDDs).
   *
   * @param op an operation to run on the RDD
   * @param reliableCheckpoint if true, use reliable checkpoints, otherwise use local checkpoints
   * @param collectFunc a function for collecting the values in the RDD, in case there are
   *                    non-comparable types like arrays that we want to convert to something
   *                    that supports ==
   */
  protected def testRDDPartitions[U: ClassTag](
      op: (RDD[Int]) => RDD[U],
      reliableCheckpoint: Boolean,
      collectFunc: RDD[U] => Any = defaultCollectFunc[U] _): Unit = {
    // Generate the final RDD using given RDD operation
    val baseRDD = generateFatRDD()
    val operatedRDD = op(baseRDD)
    val parentRDDs = operatedRDD.dependencies.map(_.rdd)
    val rddType = operatedRDD.getClass.getSimpleName

    // Force initialization of all the data structures in RDDs
    // Without this, serializing the RDD will give a wrong estimate of the size of the RDD
    initializeRdd(operatedRDD)

    // Find serialized sizes before and after the checkpoint
    logInfo("RDD after checkpoint: " + operatedRDD + "\n" + operatedRDD.toDebugString)
    val (rddSizeBeforeCheckpoint, partitionSizeBeforeCheckpoint) = getSerializedSizes(operatedRDD)
    // checkpoint the parent RDD, not the generated one
    parentRDDs.foreach { rdd =>
      checkpoint(rdd, reliableCheckpoint)
    }
    val result = collectFunc(operatedRDD) // force checkpointing
    operatedRDD.collect() // force re-initialization of post-checkpoint lazy variables
    val (rddSizeAfterCheckpoint, partitionSizeAfterCheckpoint) = getSerializedSizes(operatedRDD)
    logInfo("RDD after checkpoint: " + operatedRDD + "\n" + operatedRDD.toDebugString)

    // Test whether the data in the checkpointed RDD is same as original
    assert(collectFunc(operatedRDD) === result)

    // Test whether serialized size of the partitions has reduced
    logInfo("Size of partitions of " + rddType +
      " [" + partitionSizeBeforeCheckpoint + " --> " + partitionSizeAfterCheckpoint + "]")
    assert(
      partitionSizeAfterCheckpoint < partitionSizeBeforeCheckpoint,
      "Size of " + rddType + " partitions did not reduce after checkpointing parent RDDs" +
        " [" + partitionSizeBeforeCheckpoint + " --> " + partitionSizeAfterCheckpoint + "]"
    )
  }

  /**
   * Get serialized sizes of the RDD and its partitions, in order to test whether the size shrinks
   * upon checkpointing. Ignores the checkpointData field, which may grow when we checkpoint.
   */
  private def getSerializedSizes(rdd: RDD[_]): (Int, Int) = {
    val rddSize = Utils.serialize(rdd).length
    val rddCpDataSize = Utils.serialize(rdd.checkpointData).length
    val rddPartitionSize = Utils.serialize(rdd.partitions).length
    val rddDependenciesSize = Utils.serialize(rdd.dependencies).length

    // Print detailed size, helps in debugging
    logInfo("Serialized sizes of " + rdd +
      ": RDD = " + rddSize +
      ", RDD checkpoint data = " + rddCpDataSize +
      ", RDD partitions = " + rddPartitionSize +
      ", RDD dependencies = " + rddDependenciesSize
    )
    // this makes sure that serializing the RDD's checkpoint data does not
    // serialize the whole RDD as well
    assert(
      rddSize > rddCpDataSize,
      "RDD's checkpoint data (" + rddCpDataSize + ") is equal or larger than the " +
        "whole RDD with checkpoint data (" + rddSize + ")"
    )
    (rddSize - rddCpDataSize, rddPartitionSize)
  }

  /**
   * Serialize and deserialize an object. This is useful to verify the objects
   * contents after deserialization (e.g., the contents of an RDD split after
   * it is sent to an executor along with a task)
   */
  protected def serializeDeserialize[T](obj: T): T = {
    val bytes = Utils.serialize(obj)
    Utils.deserialize[T](bytes)
  }

  /**
   * Recursively force the initialization of the all members of an RDD and it parents.
   */
  private def initializeRdd(rdd: RDD[_]): Unit = {
    rdd.partitions // forces the initialization of the partitions
    rdd.dependencies.map(_.rdd).foreach(initializeRdd)
  }

  /** Checkpoint the RDD either locally or reliably. */
  protected def checkpoint(rdd: RDD[_], reliableCheckpoint: Boolean): Unit = {
    if (reliableCheckpoint) {
      rdd.checkpoint()
    } else {
      rdd.localCheckpoint()
    }
  }

  /** Run a test twice, once for local checkpointing and once for reliable checkpointing. */
  protected def runTest(
      name: String,
      skipLocalCheckpoint: Boolean = false
    )(body: Boolean => Unit): Unit = {
    test(name + " [reliable checkpoint]")(body(true))
    if (!skipLocalCheckpoint) {
      test(name + " [local checkpoint]")(body(false))
    }
  }

  /**
   * Generate an RDD such that both the RDD and its partitions have large size.
   */
  protected def generateFatRDD(): RDD[Int] = {
    new FatRDD(sparkContext.makeRDD(1 to 100, 4)).map(x => x)
  }

  /**
   * Generate an pair RDD (with partitioner) such that both the RDD and its partitions
   * have large size.
   */
  protected def generateFatPairRDD(): RDD[(Int, Int)] = {
    new FatPairRDD(sparkContext.makeRDD(1 to 100, 4), partitioner).mapValues(x => x)
  }
}

/**
 * Test suite for end-to-end checkpointing functionality.
 * This tests both reliable checkpoints and local checkpoints.
 */
class CheckpointSuite extends SparkFunSuite with RDDCheckpointTester with LocalSparkContext {
  private var checkpointDir: File = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    checkpointDir = File.createTempFile("temp", "", Utils.createTempDir())
    checkpointDir.delete()
    sc = new SparkContext("local", "test")
    sc.setCheckpointDir(checkpointDir.toString)
  }

  override def afterEach(): Unit = {
    try {
      Utils.deleteRecursively(checkpointDir)
    } finally {
      super.afterEach()
    }
  }

  override def sparkContext: SparkContext = sc

  runTest("basic checkpointing") { reliableCheckpoint: Boolean =>
    val parCollection = sc.makeRDD(1 to 4)
    val flatMappedRDD = parCollection.flatMap(x => 1 to x)
    checkpoint(flatMappedRDD, reliableCheckpoint)
    assert(flatMappedRDD.dependencies.head.rdd === parCollection)
    val result = flatMappedRDD.collect()
    assert(flatMappedRDD.dependencies.head.rdd != parCollection)
    assert(flatMappedRDD.collect() === result)
  }

  runTest("checkpointing partitioners", skipLocalCheckpoint = true) { _: Boolean =>

    def testPartitionerCheckpointing(
        partitioner: Partitioner,
        corruptPartitionerFile: Boolean = false
      ): Unit = {
      val rddWithPartitioner = sc.makeRDD(1 to 4).map { _ -> 1 }.partitionBy(partitioner)
      rddWithPartitioner.checkpoint()
      rddWithPartitioner.count()
      assert(rddWithPartitioner.getCheckpointFile.get.nonEmpty,
        "checkpointing was not successful")

      if (corruptPartitionerFile) {
        // Overwrite the partitioner file with garbage data
        val checkpointDir = new Path(rddWithPartitioner.getCheckpointFile.get)
        val fs = checkpointDir.getFileSystem(sc.hadoopConfiguration)
        val partitionerFile = fs.listStatus(checkpointDir)
            .find(_.getPath.getName.contains("partitioner"))
            .map(_.getPath)
        require(partitionerFile.nonEmpty, "could not find the partitioner file for testing")
        val output = fs.create(partitionerFile.get, true)
        output.write(100)
        output.close()
      }

      val newRDD = sc.checkpointFile[(Int, Int)](rddWithPartitioner.getCheckpointFile.get)
      assert(newRDD.collect().toSet === rddWithPartitioner.collect().toSet, "RDD not recovered")

      if (!corruptPartitionerFile) {
        assert(newRDD.partitioner != None, "partitioner not recovered")
        assert(newRDD.partitioner === rddWithPartitioner.partitioner,
          "recovered partitioner does not match")
      } else {
        assert(newRDD.partitioner == None, "partitioner unexpectedly recovered")
      }
    }

    testPartitionerCheckpointing(partitioner)

    // Test that corrupted partitioner file does not prevent recovery of RDD
    testPartitionerCheckpointing(partitioner, corruptPartitionerFile = true)
  }

  runTest("RDDs with one-to-one dependencies") { reliableCheckpoint: Boolean =>
    testRDD(_.map(x => x.toString), reliableCheckpoint)
    testRDD(_.flatMap(x => 1 to x), reliableCheckpoint)
    testRDD(_.filter(_ % 2 == 0), reliableCheckpoint)
    testRDD(_.sample(false, 0.5, 0), reliableCheckpoint)
    testRDD(_.glom(), reliableCheckpoint)
    testRDD(_.mapPartitions(_.map(_.toString)), reliableCheckpoint)
    testRDD(_.map(x => (x % 2, 1)).reduceByKey(_ + _).mapValues(_.toString), reliableCheckpoint)
    testRDD(_.map(x => (x % 2, 1)).reduceByKey(_ + _).flatMapValues(x => 1 to x),
      reliableCheckpoint)
    testRDD(_.pipe(Seq("cat")), reliableCheckpoint)
  }

  runTest("ParallelCollectionRDD") { reliableCheckpoint: Boolean =>
    val parCollection = sc.makeRDD(1 to 4, 2)
    val numPartitions = parCollection.partitions.length
    checkpoint(parCollection, reliableCheckpoint)
    assert(parCollection.dependencies === Nil)
    val result = parCollection.collect()
    if (reliableCheckpoint) {
      assert(sc.checkpointFile[Int](parCollection.getCheckpointFile.get).collect() === result)
    }
    assert(parCollection.dependencies != Nil)
    assert(parCollection.partitions.length === numPartitions)
    assert(parCollection.partitions.toList ===
      parCollection.checkpointData.get.getPartitions.toList)
    assert(parCollection.collect() === result)
  }

  runTest("BlockRDD") { reliableCheckpoint: Boolean =>
    val blockId = TestBlockId("id")
    val blockManager = SparkEnv.get.blockManager
    blockManager.putSingle(blockId, "test", StorageLevel.MEMORY_ONLY)
    val blockRDD = new BlockRDD[String](sc, Array(blockId))
    val numPartitions = blockRDD.partitions.length
    checkpoint(blockRDD, reliableCheckpoint)
    val result = blockRDD.collect()
    if (reliableCheckpoint) {
      assert(sc.checkpointFile[String](blockRDD.getCheckpointFile.get).collect() === result)
    }
    assert(blockRDD.dependencies != Nil)
    assert(blockRDD.partitions.length === numPartitions)
    assert(blockRDD.partitions.toList === blockRDD.checkpointData.get.getPartitions.toList)
    assert(blockRDD.collect() === result)
  }

  runTest("ShuffleRDD") { reliableCheckpoint: Boolean =>
    testRDD(rdd => {
      // Creating ShuffledRDD directly as PairRDDFunctions.combineByKey produces a MapPartitionedRDD
      new ShuffledRDD[Int, Int, Int](rdd.map(x => (x % 2, 1)), partitioner)
    }, reliableCheckpoint)
  }

  runTest("UnionRDD") { reliableCheckpoint: Boolean =>
    def otherRDD: RDD[Int] = sc.makeRDD(1 to 10, 1)
    testRDD(_.union(otherRDD), reliableCheckpoint)
    testRDDPartitions(_.union(otherRDD), reliableCheckpoint)
  }

  runTest("CartesianRDD") { reliableCheckpoint: Boolean =>
    def otherRDD: RDD[Int] = sc.makeRDD(1 to 10, 1)
    testRDD(new CartesianRDD(sc, _, otherRDD), reliableCheckpoint)
    testRDDPartitions(new CartesianRDD(sc, _, otherRDD), reliableCheckpoint)

    // Test that the CartesianRDD updates parent partitions (CartesianRDD.s1/s2) after
    // the parent RDD has been checkpointed and parent partitions have been changed.
    // Note that this test is very specific to the current implementation of CartesianRDD.
    val ones = sc.makeRDD(1 to 100, 10).map(x => x)
    checkpoint(ones, reliableCheckpoint)
    val cartesian = new CartesianRDD(sc, ones, ones)
    val splitBeforeCheckpoint =
      serializeDeserialize(cartesian.partitions.head.asInstanceOf[CartesianPartition])
    cartesian.count() // do the checkpointing
    val splitAfterCheckpoint =
      serializeDeserialize(cartesian.partitions.head.asInstanceOf[CartesianPartition])
    assert(
      (splitAfterCheckpoint.s1.getClass != splitBeforeCheckpoint.s1.getClass) &&
        (splitAfterCheckpoint.s2.getClass != splitBeforeCheckpoint.s2.getClass),
      "CartesianRDD.s1 and CartesianRDD.s2 not updated after parent RDD is checkpointed"
    )
  }

  runTest("CoalescedRDD") { reliableCheckpoint: Boolean =>
    testRDD(_.coalesce(2), reliableCheckpoint)
    testRDDPartitions(_.coalesce(2), reliableCheckpoint)

    // Test that the CoalescedRDDPartition updates parent partitions (CoalescedRDDPartition.parents)
    // after the parent RDD has been checkpointed and parent partitions have been changed.
    // Note that this test is very specific to the current implementation of
    // CoalescedRDDPartitions.
    val ones = sc.makeRDD(1 to 100, 10).map(x => x)
    checkpoint(ones, reliableCheckpoint)
    val coalesced = new CoalescedRDD(ones, 2)
    val splitBeforeCheckpoint =
      serializeDeserialize(coalesced.partitions.head.asInstanceOf[CoalescedRDDPartition])
    coalesced.count() // do the checkpointing
    val splitAfterCheckpoint =
      serializeDeserialize(coalesced.partitions.head.asInstanceOf[CoalescedRDDPartition])
    assert(
      splitAfterCheckpoint.parents.head.getClass != splitBeforeCheckpoint.parents.head.getClass,
      "CoalescedRDDPartition.parents not updated after parent RDD is checkpointed"
    )
  }

  runTest("CoGroupedRDD") { reliableCheckpoint: Boolean =>
    val longLineageRDD1 = generateFatPairRDD()

    // Collect the RDD as sequences instead of arrays to enable equality tests in testRDD
    val seqCollectFunc = (rdd: RDD[(Int, Array[Iterable[Int]])]) =>
      rdd.map{case (p, a) => (p, a.toSeq)}.collect(): Any

    testRDD(rdd => {
      CheckpointSuite.cogroup(longLineageRDD1, rdd.map(x => (x % 2, 1)), partitioner)
    }, reliableCheckpoint, seqCollectFunc)

    val longLineageRDD2 = generateFatPairRDD()
    testRDDPartitions(rdd => {
      CheckpointSuite.cogroup(
        longLineageRDD2, sc.makeRDD(1 to 2, 2).map(x => (x % 2, 1)), partitioner)
    }, reliableCheckpoint, seqCollectFunc)
  }

  runTest("ZippedPartitionsRDD") { reliableCheckpoint: Boolean =>
    testRDD(rdd => rdd.zip(rdd.map(x => x)), reliableCheckpoint)
    testRDDPartitions(rdd => rdd.zip(rdd.map(x => x)), reliableCheckpoint)

    // Test that ZippedPartitionsRDD updates parent partitions after parent RDDs have
    // been checkpointed and parent partitions have been changed.
    // Note that this test is very specific to the implementation of ZippedPartitionsRDD.
    val rdd = generateFatRDD()
    val zippedRDD = rdd.zip(rdd.map(x => x)).asInstanceOf[ZippedPartitionsRDD2[_, _, _]]
    checkpoint(zippedRDD.rdd1, reliableCheckpoint)
    checkpoint(zippedRDD.rdd2, reliableCheckpoint)
    val partitionBeforeCheckpoint =
      serializeDeserialize(zippedRDD.partitions.head.asInstanceOf[ZippedPartitionsPartition])
    zippedRDD.count()
    val partitionAfterCheckpoint =
      serializeDeserialize(zippedRDD.partitions.head.asInstanceOf[ZippedPartitionsPartition])
    assert(
      partitionAfterCheckpoint.partitions(0).getClass !=
        partitionBeforeCheckpoint.partitions(0).getClass &&
      partitionAfterCheckpoint.partitions(1).getClass !=
        partitionBeforeCheckpoint.partitions(1).getClass,
      "ZippedPartitionsRDD partition 0 (or 1) not updated after parent RDDs are checkpointed"
    )
  }

  runTest("PartitionerAwareUnionRDD") { reliableCheckpoint: Boolean =>
    testRDD(rdd => {
      new PartitionerAwareUnionRDD[(Int, Int)](sc, Array(
        generateFatPairRDD(),
        rdd.map(x => (x % 2, 1)).reduceByKey(partitioner, _ + _)
      ).toImmutableArraySeq)
    }, reliableCheckpoint)

    testRDDPartitions(rdd => {
      new PartitionerAwareUnionRDD[(Int, Int)](sc, Array(
        generateFatPairRDD(),
        rdd.map(x => (x % 2, 1)).reduceByKey(partitioner, _ + _)
      ).toImmutableArraySeq)
    }, reliableCheckpoint)

    // Test that the PartitionerAwareUnionRDD updates parent partitions
    // (PartitionerAwareUnionRDD.parents) after the parent RDD has been checkpointed and parent
    // partitions have been changed. Note that this test is very specific to the current
    // implementation of PartitionerAwareUnionRDD.
    val pairRDD = generateFatPairRDD()
    checkpoint(pairRDD, reliableCheckpoint)
    val unionRDD = new PartitionerAwareUnionRDD(sc, Seq(pairRDD))
    val partitionBeforeCheckpoint = serializeDeserialize(
      unionRDD.partitions.head.asInstanceOf[PartitionerAwareUnionRDDPartition])
    pairRDD.count()
    val partitionAfterCheckpoint = serializeDeserialize(
      unionRDD.partitions.head.asInstanceOf[PartitionerAwareUnionRDDPartition])
    assert(
      partitionBeforeCheckpoint.parents.head.getClass !=
        partitionAfterCheckpoint.parents.head.getClass,
      "PartitionerAwareUnionRDDPartition.parents not updated after parent RDD is checkpointed"
    )
  }

  runTest("CheckpointRDD with zero partitions") { reliableCheckpoint: Boolean =>
    val rdd = new BlockRDD[Int](sc, Array.empty[BlockId])
    assert(rdd.partitions.length === 0)
    assert(rdd.isCheckpointed === false)
    assert(rdd.isCheckpointedAndMaterialized === false)
    checkpoint(rdd, reliableCheckpoint)
    assert(rdd.isCheckpointed === false)
    assert(rdd.isCheckpointedAndMaterialized === false)
    assert(rdd.count() === 0)
    assert(rdd.isCheckpointed)
    assert(rdd.isCheckpointedAndMaterialized)
    assert(rdd.partitions.length === 0)
  }

  runTest("checkpointAllMarkedAncestors") { reliableCheckpoint: Boolean =>
    testCheckpointAllMarkedAncestors(reliableCheckpoint, checkpointAllMarkedAncestors = true)
    testCheckpointAllMarkedAncestors(reliableCheckpoint, checkpointAllMarkedAncestors = false)
  }

  private def testCheckpointAllMarkedAncestors(
      reliableCheckpoint: Boolean, checkpointAllMarkedAncestors: Boolean): Unit = {
    sc.setLocalProperty(RDD.CHECKPOINT_ALL_MARKED_ANCESTORS, checkpointAllMarkedAncestors.toString)
    try {
      val rdd1 = sc.parallelize(1 to 10)
      checkpoint(rdd1, reliableCheckpoint)
      val rdd2 = rdd1.map(_ + 1)
      checkpoint(rdd2, reliableCheckpoint)
      rdd2.count()
      assert(rdd1.isCheckpointed === checkpointAllMarkedAncestors)
      assert(rdd2.isCheckpointed)
    } finally {
      sc.setLocalProperty(RDD.CHECKPOINT_ALL_MARKED_ANCESTORS, null)
    }
  }
}

/** RDD partition that has large serialized size. */
class FatPartition(val partition: Partition) extends Partition {
  val bigData = new Array[Byte](10000)
  def index: Int = partition.index
}

/** RDD that has large serialized size. */
class FatRDD(parent: RDD[Int]) extends RDD[Int](parent) {
  val bigData = new Array[Byte](100000)

  protected def getPartitions: Array[Partition] = {
    parent.partitions.map(p => new FatPartition(p))
  }

  def compute(split: Partition, context: TaskContext): Iterator[Int] = {
    parent.compute(split.asInstanceOf[FatPartition].partition, context)
  }
}

/** Pair RDD that has large serialized size. */
class FatPairRDD(parent: RDD[Int], _partitioner: Partitioner) extends RDD[(Int, Int)](parent) {
  val bigData = new Array[Byte](100000)

  protected def getPartitions: Array[Partition] = {
    parent.partitions.map(p => new FatPartition(p))
  }

  @transient override val partitioner = Some(_partitioner)

  def compute(split: Partition, context: TaskContext): Iterator[(Int, Int)] = {
    parent.compute(split.asInstanceOf[FatPartition].partition, context).map(x => (x, x))
  }
}

object CheckpointSuite {
  // This is a custom cogroup function that does not use mapValues like
  // the PairRDDFunctions.cogroup()
  def cogroup[K: ClassTag, V: ClassTag](first: RDD[(K, V)], second: RDD[(K, V)], part: Partitioner)
    : RDD[(K, Array[Iterable[V]])] = {
    new CoGroupedRDD[K](
      Seq(first.asInstanceOf[RDD[(K, _)]], second.asInstanceOf[RDD[(K, _)]]),
      part
    ).asInstanceOf[RDD[(K, Array[Iterable[V]])]]
  }
}

class CheckpointStorageSuite extends SparkFunSuite with LocalSparkContext {

  test("checkpoint compression") {
    withTempDir { checkpointDir =>
      val conf = new SparkConf()
        .set("spark.checkpoint.compress", "true")
        .set(UI_ENABLED.key, "false")
      sc = new SparkContext("local", "test", conf)
      sc.setCheckpointDir(checkpointDir.toString)
      val rdd = sc.makeRDD(1 to 20, numSlices = 1)
      rdd.checkpoint()
      assert(rdd.collect().toSeq === (1 to 20))

      // Verify that RDD is checkpointed
      assert(rdd.firstParent.isInstanceOf[ReliableCheckpointRDD[_]])

      val checkpointPath = new Path(rdd.getCheckpointFile.get)
      val fs = checkpointPath.getFileSystem(sc.hadoopConfiguration)
      val checkpointFile =
        fs.listStatus(checkpointPath).map(_.getPath).find(_.getName.startsWith("part-")).get

      // Verify the checkpoint file is compressed, in other words, can be decompressed
      val compressedInputStream = CompressionCodec.createCodec(conf)
        .compressedInputStream(fs.open(checkpointFile))
      try {
        ByteStreams.toByteArray(compressedInputStream)
      } finally {
        compressedInputStream.close()
      }

      // Verify that the compressed content can be read back
      assert(rdd.collect().toSeq === (1 to 20))
    }
  }

  test("cache checkpoint preferred location") {
    withTempDir { checkpointDir =>
      val conf = new SparkConf()
        .set(CACHE_CHECKPOINT_PREFERRED_LOCS_EXPIRE_TIME.key, "10")
        .set(UI_ENABLED.key, "false")
      sc = new SparkContext("local", "test", conf)
      sc.setCheckpointDir(checkpointDir.toString)
      val rdd = sc.makeRDD(1 to 20, numSlices = 1)
      rdd.checkpoint()
      assert(rdd.collect().toSeq === (1 to 20))

      // Verify that RDD is checkpointed
      assert(rdd.firstParent.isInstanceOf[ReliableCheckpointRDD[_]])
      val checkpointedRDD = rdd.firstParent.asInstanceOf[ReliableCheckpointRDD[_]]
      val partition = checkpointedRDD.partitions(0)
      assert(!checkpointedRDD.cachedPreferredLocations.asMap.containsKey(partition))

      val preferredLoc = checkpointedRDD.preferredLocations(partition)
      assert(checkpointedRDD.cachedPreferredLocations.asMap.containsKey(partition))
      assert(preferredLoc == checkpointedRDD.cachedPreferredLocations.get(partition))
    }
  }

  test("SPARK-31484: checkpoint should not fail in retry") {
    withTempDir { checkpointDir =>
      val conf = new SparkConf()
        .set(UI_ENABLED.key, "false")
      sc = new SparkContext("local[1]", "test", conf)
      sc.setCheckpointDir(checkpointDir.toString)
      val rdd = sc.makeRDD(1 to 200, numSlices = 4).repartition(1).mapPartitions { iter =>
        iter.map { i =>
          if (i > 100 && TaskContext.get().stageAttemptNumber() == 0) {
            // throw new SparkException("Make first attempt failed.")
            // Throw FetchFailedException to explicitly trigger stage resubmission.
            // A normal exception will only trigger task resubmission in the same stage.
            throw new FetchFailedException(null, 0, 0L, 0, 0, "Fake")
          } else {
            i
          }
        }
      }
      rdd.checkpoint()
      assert(rdd.collect().toSeq === (1 to 200))
      // Verify that RDD is checkpointed
      assert(rdd.firstParent.isInstanceOf[ReliableCheckpointRDD[_]])
    }
  }

  test("SPARK-48268: checkpoint directory via configuration") {
    withTempDir { checkpointDir =>
      val conf = new SparkConf()
        .set("spark.checkpoint.dir", checkpointDir.toString)
        .set(UI_ENABLED.key, "false")
      sc = new SparkContext("local", "test", conf)
      val parCollection = sc.makeRDD(1 to 4)
      val flatMappedRDD = parCollection.flatMap(x => 1 to x)
      flatMappedRDD.checkpoint()
      assert(flatMappedRDD.dependencies.head.rdd === parCollection)
      val result = flatMappedRDD.collect()
      assert(flatMappedRDD.dependencies.head.rdd != parCollection)
      assert(flatMappedRDD.collect() === result)
    }
  }
}
