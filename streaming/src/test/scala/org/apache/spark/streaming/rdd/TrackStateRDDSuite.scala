package org.apache.spark.streaming.rdd

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import org.scalatest.BeforeAndAfterAll

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.State
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext, SparkFunSuite}

class TrackStateRDDSuite extends SparkFunSuite with BeforeAndAfterAll {

  private var sc = new SparkContext(
    new SparkConf().setMaster("local").setAppName("TrackStateRDDSuite"))

  override def afterAll(): Unit = {
    sc.stop()
  }

  test("creation from pair RDD") {
    val data = Seq((1, "1"), (2, "2"), (3, "3"))
    val partitioner = new HashPartitioner(10)
    val rdd = TrackStateRDD.createFromPairRDD[Int, Int, String, Int](
      sc.parallelize(data), partitioner, 123)
    assertRDD[Int, Int, String, Int](rdd, data.map { x => (x._1, x._2, 123)}.toSet, Set.empty)
    assert(rdd.partitions.size === partitioner.numPartitions)

    assert(rdd.partitioner === Some(partitioner))
  }

  test("updating state on existing TrackStateRDD") {
    val initStates = Seq(("k1", 0), ("k2", 0))
    val initTime = 123
    val initStateWthTime = initStates.map { x => (x._1, x._2, initTime) }.toSet
    val partitioner = new HashPartitioner(2)
    val initStateRDD = TrackStateRDD.createFromPairRDD[String, Int, Int, Int](
      sc.parallelize(initStates), partitioner, initTime).persist()
    assertRDD(initStateRDD, initStateWthTime, Set.empty)

    val updateTime = 345

    /**
     * Test that the test state RDD, when operated with new data,
     * creates a new state RDD with expected states
     */
    def testStateUpdates(
        testStateRDD: TrackStateRDD[String, Int, Int, Int],
        testData: Seq[(String, Int)],
        expectedStates: Set[(String, Int, Int)]): TrackStateRDD[String, Int, Int, Int] = {

      // Persist the test TrackStateRDD so that its not recomputed while doing the next operation.
      // This is to make sure that we only track which state keys are being touched in the next op.
      testStateRDD.persist().count()

      // To track which keys are being touched
      TrackStateRDDSuite.touchedStateKeys.clear()

      val trackingFunc = (key: String, data: Option[Int], state: State[Int]) => {

        // Track the key that has been touched
        TrackStateRDDSuite.touchedStateKeys += key

        // If the data is 0, do not do anything with the state
        // else if the data is 1, increment the state if it exists, or set new state to 0
        // else if the data is 2, remove the state if it exists
        data match {
          case Some(1) =>
            if (state.exists()) { state.update(state.get + 1) }
            else state.update(0)
          case Some(2) =>
            state.remove()
          case _ =>
        }
        None.asInstanceOf[Option[Int]]  // Do not return anything, not being tested
      }
      val newDataRDD = sc.makeRDD(testData).partitionBy(testStateRDD.partitioner.get)

      // Assert that the new state RDD has expected state data
      val newStateRDD = assertOperation(
        testStateRDD, newDataRDD, trackingFunc, updateTime, expectedStates, Set.empty)

      // Assert that the function was called only for the keys present in the data
      assert(TrackStateRDDSuite.touchedStateKeys.size === testData.size,
        "More number of keys are being touched than that is expected")
      assert(TrackStateRDDSuite.touchedStateKeys.toSet === testData.toMap.keys,
        "Keys not in the data are being touched unexpectedly")

      // Assert that the test RDD's data has not changed
      assertRDD(initStateRDD, initStateWthTime, Set.empty)
      newStateRDD
    }


    // Test no-op, no state should change
    testStateUpdates(initStateRDD, Seq(), initStateWthTime)   // should not scan any state
    testStateUpdates(
      initStateRDD, Seq(("k1", 0)), initStateWthTime)         // should not update existing state
    testStateUpdates(
      initStateRDD, Seq(("k3", 0)), initStateWthTime)         // should not create new state

    // Test creation of new state
    val rdd1 = testStateUpdates(initStateRDD, Seq(("k3", 1)), // should create k3's state as 0
      Set(("k1", 0, initTime), ("k2", 0, initTime), ("k3", 0, updateTime)))

    val rdd2 = testStateUpdates(rdd1, Seq(("k4", 1)),         // should create k4's state as 0
      Set(("k1", 0, initTime), ("k2", 0, initTime), ("k3", 0, updateTime), ("k4", 0, updateTime)))

    // Test updating of state
    val rdd3 = testStateUpdates(
      initStateRDD, Seq(("k1", 1)),                   // should increment k1's state 0 -> 1
      Set(("k1", 1, updateTime), ("k2", 0, initTime)))

    val rdd4 = testStateUpdates(
      rdd3, Seq(("x", 0), ("k2", 1), ("k2", 1), ("k3", 1)),     // should update k2, 0 -> 2 and create k3, 0
      Set(("k1", 1, updateTime), ("k2", 2, updateTime), ("k3", 0, updateTime)))

    val rdd5 = testStateUpdates(
      rdd4, Seq(("k3", 1)),                           // should update k3's state 0 -> 2
      Set(("k1", 1, updateTime), ("k2", 2, updateTime), ("k3", 1, updateTime)))

    // Test removing of state
    val rdd6 = testStateUpdates(                      // should remove k1's state
      initStateRDD, Seq(("k1", 2)), Set(("k2", 0, initTime)))

    val rdd7 = testStateUpdates(                      // should remove k2's state
      rdd6, Seq(("k2", 2), ("k0", 2), ("k3", 1)), Set(("k3", 0, updateTime)))

    val rdd8 = testStateUpdates(
      rdd7, Seq(("k3", 2)), Set()                     //
    )
  }

  private def assertOperation[K: ClassTag, V: ClassTag, S: ClassTag, T: ClassTag](
      testStateRDD: TrackStateRDD[K, V, S, T],
      newDataRDD: RDD[(K, V)],
      trackStateFunc: (K, Option[V], State[S]) => Option[T],
      currentTime: Long,
      expectedStates: Set[(K, S, Int)],
      expectedEmittedRecords: Set[T]): TrackStateRDD[K, V, S, T] = {

    val partitionedNewDataRDD = if (newDataRDD.partitioner != testStateRDD.partitioner) {
      newDataRDD.partitionBy(testStateRDD.partitioner.get)
    } else {
      newDataRDD
    }

    val newStateRDD = new TrackStateRDD[K, V, S, T](
      testStateRDD, newDataRDD, trackStateFunc, currentTime, None)

    // Persist to make sure that it gets computed only once and we can track precisely how many
    // state keys the computing touched
    newStateRDD.persist()
    assertRDD(newStateRDD, expectedStates, expectedEmittedRecords)
    newStateRDD
  }

  private def assertRDD[K: ClassTag, V: ClassTag, S: ClassTag, T: ClassTag](
      trackStateRDD: TrackStateRDD[K, V, S, T],
      expectedStates: Set[(K, S, Int)],
      expectedEmittedRecords: Set[T]): Unit = {
    val states = trackStateRDD.flatMap { _.stateMap.getAll() }.collect().toSet
    val emittedRecords = trackStateRDD.flatMap { _.emittedRecords }.collect().toSet
    assert(states === expectedStates, "states after track state operation were not as expected")
    assert(emittedRecords === expectedEmittedRecords,
      "emitted records after track state operation were not as expected")
  }
}

object TrackStateRDDSuite {
  private val touchedStateKeys = new ArrayBuffer[String]()
}