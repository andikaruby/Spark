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

package org.apache.spark.rdd

import scala.language.higherKinds
import scala.reflect.ClassTag

import org.apache.spark.util.collection.ImmutableBitSet
import org.apache.spark.util.collection.ImmutableLongOpenHashSet
import org.apache.spark.util.collection.ImmutableVector
import org.apache.spark.util.collection.PrimitiveKeyOpenHashMap

import IndexedRDD.Id
import IndexedRDDPartition.Index

private[spark] object IndexedRDDPartition {
  type Index = ImmutableLongOpenHashSet

  // Same as apply(iter, (a, b) => b)
  def apply[V: ClassTag](iter: Iterator[(Id, V)]): IndexedRDDPartition[V] = {
    val map = new PrimitiveKeyOpenHashMap[Id, V]
    iter.foreach { pair =>
      map(pair._1) = pair._2
    }
    new IndexedRDDPartition(
      ImmutableLongOpenHashSet.fromLongOpenHashSet(map.keySet),
      ImmutableVector.fromArray(map._values),
      map.keySet.getBitSet.toImmutableBitSet)
  }

  def apply[V: ClassTag](iter: Iterator[(Id, V)], mergeFunc: (V, V) => V)
    : IndexedRDDPartition[V] = {
    val map = new PrimitiveKeyOpenHashMap[Id, V]
    iter.foreach { pair =>
      map.setMerge(pair._1, pair._2, mergeFunc)
    }
    new IndexedRDDPartition(
      ImmutableLongOpenHashSet.fromLongOpenHashSet(map.keySet),
      ImmutableVector.fromArray(map._values),
      map.keySet.getBitSet.toImmutableBitSet)
  }
}

private[spark] trait IndexedRDDPartitionLike[@specialized(Long, Int, Double) V] {
  def index: Index
  def values: ImmutableVector[V]
  def mask: ImmutableBitSet

  val capacity: Int = index.capacity

  def size: Int = mask.cardinality()

  /** Return the value for the given key. */
  def apply(k: Id): V = values(index.getPos(k))

  // /** Return the value for the given key, or None if it is not defined. */
  // def get(k: Id): Option[V] = {
  //   val pos = index.getPos(k)
  //   if (pos != -1 && mask.get(pos)) Some(values(pos))
  //   else None
  // }

  def isDefined(k: Id): Boolean = {
    val pos = index.getPos(k)
    pos >= 0 && mask.get(pos)
  }

  def iterator: Iterator[(Id, V)] =
    mask.iterator.map(ind => (index.getValue(ind), values(ind)))
}

private[spark] class IndexedRDDPartition[@specialized(Long, Int, Double) V](
    val index: Index,
    val values: ImmutableVector[V],
    val mask: ImmutableBitSet)
   (implicit val vTag: ClassTag[V])
  extends IndexedRDDPartitionLike[V]
  with IndexedRDDPartitionOps[V, IndexedRDDPartition] {

  def self: IndexedRDDPartition[V] = this

  def withIndex(index: Index): IndexedRDDPartition[V] = {
    new IndexedRDDPartition(index, values, mask)
  }

  def withValues[V2: ClassTag](values: ImmutableVector[V2]): IndexedRDDPartition[V2] = {
    new IndexedRDDPartition(index, values, mask)
  }

  def withMask(mask: ImmutableBitSet): IndexedRDDPartition[V] = {
    new IndexedRDDPartition(index, values, mask)
  }
}
