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

package org.apache.spark.streaming

import org.apache.spark.{HashPartitioner, Partitioner}
import org.apache.spark.annotation.Experimental
import org.apache.spark.api.java.{JavaPairRDD, JavaUtils, Optional}
import org.apache.spark.api.java.function.{Function3 => JFunction3, Function4 => JFunction4}
import org.apache.spark.rdd.RDD
import org.apache.spark.util.SparkClosureCleaner

/**
 * :: Experimental ::
 * Abstract class representing all the specifications of the DStream transformation
 * `mapWithState` operation of a
 * [[org.apache.spark.streaming.dstream.PairDStreamFunctions pair DStream]] (Scala) or a
 * [[org.apache.spark.streaming.api.java.JavaPairDStream JavaPairDStream]] (Java).
 * Use `org.apache.spark.streaming.StateSpec.function()` factory methods
 * to create instances of this class.
 *
 * Example in Scala:
 * {{{
 *    // A mapping function that maintains an integer state and return a String
 *    def mappingFunction(key: String, value: Option[Int], state: State[Int]): Option[String] = {
 *      // Use state.exists(), state.get(), state.update() and state.remove()
 *      // to manage state, and return the necessary string
 *    }
 *
 *    val spec = StateSpec.function(mappingFunction).numPartitions(10)
 *
 *    val mapWithStateDStream = keyValueDStream.mapWithState[StateType, MappedType](spec)
 * }}}
 *
 * Example in Java:
 * {{{
 *   // A mapping function that maintains an integer state and return a string
 *   Function3<String, Optional<Integer>, State<Integer>, String> mappingFunction =
 *       new Function3<String, Optional<Integer>, State<Integer>, String>() {
 *           @Override
 *           public Optional<String> call(Optional<Integer> value, State<Integer> state) {
 *               // Use state.exists(), state.get(), state.update() and state.remove()
 *               // to manage state, and return the necessary string
 *           }
 *       };
 *
 *    JavaMapWithStateDStream<String, Integer, Integer, String> mapWithStateDStream =
 *        keyValueDStream.mapWithState(StateSpec.function(mappingFunc));
 * }}}
 *
 * @tparam KeyType    Class of the state key
 * @tparam ValueType  Class of the state value
 * @tparam StateType  Class of the state data
 * @tparam MappedType Class of the mapped elements
 */
@Experimental
sealed abstract class StateSpec[KeyType, ValueType, StateType, MappedType] extends Serializable {

  /**
   * Set the RDD containing the initial states that will be used by `mapWithState`
   */
  def initialState(rdd: RDD[(KeyType, StateType)]): this.type

  /**
   * Set the RDD containing the initial states that will be used by `mapWithState`
   */
  def initialState(javaPairRDD: JavaPairRDD[KeyType, StateType]): this.type

  /**
   * Set the number of partitions by which the state RDDs generated by `mapWithState`
   * will be partitioned. Hash partitioning will be used.
   */
  def numPartitions(numPartitions: Int): this.type

  /**
   * Set the partitioner by which the state RDDs generated by `mapWithState` will be partitioned.
   */
  def partitioner(partitioner: Partitioner): this.type

  /**
   * Set the duration after which the state of an idle key will be removed. A key and its state is
   * considered idle if it has not received any data for at least the given duration. The
   * mapping function will be called one final time on the idle states that are going to be
   * removed; [[org.apache.spark.streaming.State State.isTimingOut()]] set
   * to `true` in that call.
   */
  def timeout(idleDuration: Duration): this.type
}


/**
 * :: Experimental ::
 * Builder object for creating instances of `org.apache.spark.streaming.StateSpec`
 * that is used for specifying the parameters of the DStream transformation `mapWithState`
 * that is used for specifying the parameters of the DStream transformation
 * `mapWithState` operation of a
 * [[org.apache.spark.streaming.dstream.PairDStreamFunctions pair DStream]] (Scala) or a
 * [[org.apache.spark.streaming.api.java.JavaPairDStream JavaPairDStream]] (Java).
 *
 * Example in Scala:
 * {{{
 *    // A mapping function that maintains an integer state and return a String
 *    def mappingFunction(key: String, value: Option[Int], state: State[Int]): Option[String] = {
 *      // Use state.exists(), state.get(), state.update() and state.remove()
 *      // to manage state, and return the necessary string
 *    }
 *
 *    val spec = StateSpec.function(mappingFunction).numPartitions(10)
 *
 *    val mapWithStateDStream = keyValueDStream.mapWithState[StateType, MappedType](spec)
 * }}}
 *
 * Example in Java:
 * {{{
 *   // A mapping function that maintains an integer state and return a string
 *   Function3<String, Optional<Integer>, State<Integer>, String> mappingFunction =
 *       new Function3<String, Optional<Integer>, State<Integer>, String>() {
 *           @Override
 *           public Optional<String> call(Optional<Integer> value, State<Integer> state) {
 *               // Use state.exists(), state.get(), state.update() and state.remove()
 *               // to manage state, and return the necessary string
 *           }
 *       };
 *
 *    JavaMapWithStateDStream<String, Integer, Integer, String> mapWithStateDStream =
 *        keyValueDStream.mapWithState(StateSpec.function(mappingFunc));
 *}}}
 */
@Experimental
object StateSpec {
  /**
   * Create a [[org.apache.spark.streaming.StateSpec StateSpec]] for setting all the specifications
   * of the `mapWithState` operation on a
   * [[org.apache.spark.streaming.dstream.PairDStreamFunctions pair DStream]].
   *
   * @param mappingFunction The function applied on every data item to manage the associated state
   *                         and generate the mapped data
   * @tparam KeyType      Class of the keys
   * @tparam ValueType    Class of the values
   * @tparam StateType    Class of the states data
   * @tparam MappedType   Class of the mapped data
   */
  def function[KeyType, ValueType, StateType, MappedType](
      mappingFunction: (Time, KeyType, Option[ValueType], State[StateType]) => Option[MappedType]
    ): StateSpec[KeyType, ValueType, StateType, MappedType] = {
    SparkClosureCleaner.clean(mappingFunction, checkSerializable = true)
    new StateSpecImpl(mappingFunction)
  }

  /**
   * Create a [[org.apache.spark.streaming.StateSpec StateSpec]] for setting all the specifications
   * of the `mapWithState` operation on a
   * [[org.apache.spark.streaming.dstream.PairDStreamFunctions pair DStream]].
   *
   * @param mappingFunction The function applied on every data item to manage the associated state
   *                         and generate the mapped data
   * @tparam ValueType    Class of the values
   * @tparam StateType    Class of the states data
   * @tparam MappedType   Class of the mapped data
   */
  def function[KeyType, ValueType, StateType, MappedType](
      mappingFunction: (KeyType, Option[ValueType], State[StateType]) => MappedType
    ): StateSpec[KeyType, ValueType, StateType, MappedType] = {
    SparkClosureCleaner.clean(mappingFunction, checkSerializable = true)
    val wrappedFunction =
      (time: Time, key: KeyType, value: Option[ValueType], state: State[StateType]) => {
        Some(mappingFunction(key, value, state))
      }
    new StateSpecImpl(wrappedFunction)
  }

  /**
   * Create a [[org.apache.spark.streaming.StateSpec StateSpec]] for setting all
   * the specifications of the `mapWithState` operation on a
   * [[org.apache.spark.streaming.api.java.JavaPairDStream JavaPairDStream]].
   *
   * @param mappingFunction The function applied on every data item to manage the associated
   *                        state and generate the mapped data
   * @tparam KeyType      Class of the keys
   * @tparam ValueType    Class of the values
   * @tparam StateType    Class of the states data
   * @tparam MappedType   Class of the mapped data
   */
  def function[KeyType, ValueType, StateType, MappedType](mappingFunction:
      JFunction4[Time, KeyType, Optional[ValueType], State[StateType], Optional[MappedType]]):
    StateSpec[KeyType, ValueType, StateType, MappedType] = {
    val wrappedFunc = (time: Time, k: KeyType, v: Option[ValueType], s: State[StateType]) => {
      val t = mappingFunction.call(time, k, JavaUtils.optionToOptional(v), s)
      if (t.isPresent) {
        Some(t.get)
      } else {
        None
      }
    }
    StateSpec.function(wrappedFunc)
  }

  /**
   * Create a [[org.apache.spark.streaming.StateSpec StateSpec]] for setting all the specifications
   * of the `mapWithState` operation on a
   * [[org.apache.spark.streaming.api.java.JavaPairDStream JavaPairDStream]].
   *
   * @param mappingFunction The function applied on every data item to manage the associated
   *                        state and generate the mapped data
   * @tparam ValueType    Class of the values
   * @tparam StateType    Class of the states data
   * @tparam MappedType   Class of the mapped data
   */
  def function[KeyType, ValueType, StateType, MappedType](
      mappingFunction: JFunction3[KeyType, Optional[ValueType], State[StateType], MappedType]):
    StateSpec[KeyType, ValueType, StateType, MappedType] = {
    val wrappedFunc = (k: KeyType, v: Option[ValueType], s: State[StateType]) => {
      mappingFunction.call(k, JavaUtils.optionToOptional(v), s)
    }
    StateSpec.function(wrappedFunc)
  }
}


/** Internal implementation of [[org.apache.spark.streaming.StateSpec]] interface. */
private[streaming]
case class StateSpecImpl[K, V, S, T](
    function: (Time, K, Option[V], State[S]) => Option[T]) extends StateSpec[K, V, S, T] {

  require(function != null)

  @volatile private var partitioner: Partitioner = null
  @volatile private var initialStateRDD: RDD[(K, S)] = null
  @volatile private var timeoutInterval: Duration = null

  override def initialState(rdd: RDD[(K, S)]): this.type = {
    this.initialStateRDD = rdd
    this
  }

  override def initialState(javaPairRDD: JavaPairRDD[K, S]): this.type = {
    this.initialStateRDD = javaPairRDD.rdd
    this
  }

  override def numPartitions(numPartitions: Int): this.type = {
    this.partitioner(new HashPartitioner(numPartitions))
    this
  }

  override def partitioner(partitioner: Partitioner): this.type = {
    this.partitioner = partitioner
    this
  }

  override def timeout(interval: Duration): this.type = {
    this.timeoutInterval = interval
    this
  }

  // ================= Private Methods =================

  private[streaming] def getFunction(): (Time, K, Option[V], State[S]) => Option[T] = function

  private[streaming] def getInitialStateRDD(): Option[RDD[(K, S)]] = Option(initialStateRDD)

  private[streaming] def getPartitioner(): Option[Partitioner] = Option(partitioner)

  private[streaming] def getTimeoutInterval(): Option[Duration] = Option(timeoutInterval)
}
