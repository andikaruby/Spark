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

package org.apache.spark.mllib.fpm

import java.{lang => jl, util => ju}
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

import org.apache.spark.Logging
import org.apache.spark.annotation.Experimental
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.api.java.JavaSparkContext.fakeClassTag
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

/**
 *
 * :: Experimental ::
 *
 * A parallel PrefixSpan algorithm to mine sequential pattern.
 * The PrefixSpan algorithm is described in
 * [[http://doi.org/10.1109/ICDE.2001.914830]].
 *
 * @param minSupport the minimal support level of the sequential pattern, any pattern appears
 *                   more than  (minSupport * size-of-the-dataset) times will be output
 * @param maxPatternLength the maximal length of the sequential pattern, any pattern appears
 *                         less than maxPatternLength will be output
 * @param maxLocalProjDBSize The maximum number of items allowed in a projected database before
 *                           local processing. If a projected database exceeds this size, another
 *                           iteration of distributed PrefixSpan is run.
 *
 * @see [[https://en.wikipedia.org/wiki/Sequential_Pattern_Mining Sequential Pattern Mining
 *       (Wikipedia)]]
 */
@Experimental
class PrefixSpan private (
    private var minSupport: Double,
    private var maxPatternLength: Int,
    private var maxLocalProjDBSize: Long) extends Logging with Serializable {
  import PrefixSpan._

  /**
   * Constructs a default instance with default parameters
   * {minSupport: `0.1`, maxPatternLength: `10`, maxLocalProjDBSize: `32000000L`}.
   */
  def this() = this(0.1, 10, 32000000L)

  /**
   * Get the minimal support (i.e. the frequency of occurrence before a pattern is considered
   * frequent).
   */
  def getMinSupport: Double = minSupport

  /**
   * Sets the minimal support level (default: `0.1`).
   */
  def setMinSupport(minSupport: Double): this.type = {
    require(minSupport >= 0 && minSupport <= 1,
      s"The minimum support value must be in [0, 1], but got $minSupport.")
    this.minSupport = minSupport
    this
  }

  /**
   * Gets the maximal pattern length (i.e. the length of the longest sequential pattern to consider.
   */
  def getMaxPatternLength: Double = maxPatternLength

  /**
   * Sets maximal pattern length (default: `10`).
   */
  def setMaxPatternLength(maxPatternLength: Int): this.type = {
    // TODO: support unbounded pattern length when maxPatternLength = 0
    require(maxPatternLength >= 1,
      s"The maximum pattern length value must be greater than 0, but got $maxPatternLength.")
    this.maxPatternLength = maxPatternLength
    this
  }

  /**
   * Gets the maximum number of items allowed in a projected database before local processing.
   */
  def getMaxLocalProjDBSize: Long = maxLocalProjDBSize

  /**
   * Sets the maximum number of items allowed in a projected database before local processing
   * (default: `32000000L`).
   */
  def setMaxLocalProjDBSize(maxLocalProjDBSize: Long): this.type = {
    require(maxLocalProjDBSize >= 0L,
      s"The maximum local projected database size must be nonnegative, but got $maxLocalProjDBSize")
    this.maxLocalProjDBSize = maxLocalProjDBSize
    this
  }

  /**
   * Finds the complete set of frequent sequential patterns in the input sequences of itemsets.
   * @param data sequences of itemsets.
   * @return a [[PrefixSpanModel]] that contains the frequent patterns
   */
  def run[Item: ClassTag](data: RDD[Array[Array[Item]]]): PrefixSpanModel[Item] = {
    if (data.getStorageLevel == StorageLevel.NONE) {
      logWarning("Input data is not cached.")
    }

    val totalCount = data.count()
    logInfo(s"number of sequences: $totalCount")
    val minCount = math.ceil(minSupport * totalCount).toLong
    logInfo(s"minimum count for a frequent pattern: $minCount")

    // Find frequent items.
    val freqItemAndCounts = data.flatMap { itemsets =>
        val uniqItems = mutable.Set.empty[Item]
        itemsets.foreach { _.foreach { item =>
          uniqItems += item
        }}
        uniqItems.toIterator.map((_, 1L))
      }.reduceByKey(_ + _)
      .filter { case (_, count) =>
        count >= minCount
      }.collect()
    val freqItems = freqItemAndCounts.sortBy(-_._2).map(_._1)
    logInfo(s"number of frequent items: ${freqItems.size}")

    // Keep only frequent items from input sequences and convert them to internal storage.
    val itemToInt = freqItems.zipWithIndex.toMap
    val dataInternalRepr = data.map { itemsets =>
      val allItems = mutable.ArrayBuilder.make[Int]
      allItems += 0
      itemsets.foreach { itemsets =>
        val items = mutable.ArrayBuilder.make[Int]
        itemsets.foreach { item =>
          if (itemToInt.contains(item)) {
            items += itemToInt(item) + 1 // using 1-indexing in internal format
          }
        }
        val result = items.result()
        if (result.nonEmpty) {
          allItems ++= result.sorted
        }
        allItems += 0
      }
      allItems.result()
    }.persist(StorageLevel.MEMORY_AND_DISK)

    val results = genFreqPatterns(dataInternalRepr, minCount, maxPatternLength, maxLocalProjDBSize)

    def toPublicRepr(pattern: Array[Int]): Array[Array[Item]] = {
      val sequenceBuilder = mutable.ArrayBuilder.make[Array[Item]]
      val itemsetBuilder = mutable.ArrayBuilder.make[Item]
      val n = pattern.length
      var i = 1
      while (i < n) {
        val x = pattern(i)
        if (x == 0) {
          sequenceBuilder += itemsetBuilder.result()
          itemsetBuilder.clear()
        } else {
          itemsetBuilder += freqItems(x - 1) // using 1-indexing in internal format
        }
        i += 1
      }
      sequenceBuilder.result()
    }

    val freqSequences = results.map { case (seq: Array[Int], count: Long) =>
      new FreqSequence(toPublicRepr(seq), count)
    }
    new PrefixSpanModel(freqSequences)
  }

  /**
   * A Java-friendly version of [[run()]] that reads sequences from a [[JavaRDD]] and returns
   * frequent sequences in a [[PrefixSpanModel]].
   * @param data ordered sequences of itemsets stored as Java Iterable of Iterables
   * @tparam Item item type
   * @tparam Itemset itemset type, which is an Iterable of Items
   * @tparam Sequence sequence type, which is an Iterable of Itemsets
   * @return a [[PrefixSpanModel]] that contains the frequent sequential patterns
   */
  def run[Item, Itemset <: jl.Iterable[Item], Sequence <: jl.Iterable[Itemset]](
      data: JavaRDD[Sequence]): PrefixSpanModel[Item] = {
    implicit val tag = fakeClassTag[Item]
    run(data.rdd.map(_.asScala.map(_.asScala.toArray).toArray))
  }

}

@Experimental
object PrefixSpan extends Logging {

  /**
   * Find the complete set of frequent sequential patterns in the input sequences.
   * @param data ordered sequences of itemsets. We represent a sequence internally as Array[Int],
   *             where each itemset is represented by a contiguous sequence of distinct and ordered
   *             positive integers. We use 0 as the delimiter at itemset boundaries, including the
   *             first and the last position.
   * @return an RDD of (frequent sequential pattern, count) pairs,
   * @see [[Postfix]]
   */
  private[fpm] def genFreqPatterns(
      data: RDD[Array[Int]],
      minCount: Long,
      maxPatternLength: Int,
      maxLocalProjDBSize: Long): RDD[(Array[Int], Long)] = {
    val sc = data.sparkContext

    if (data.getStorageLevel == StorageLevel.NONE) {
      logWarning("Input data is not cached.")
    }

    val postfixes = data.map(items => new Postfix(items))

    // Local frequent patterns (prefixes) and their counts.
    val localFreqPatterns = mutable.ArrayBuffer.empty[(Array[Int], Long)]
    // Prefixes whose projected databases are small.
    val smallPrefixes = mutable.Map.empty[Int, Prefix]
    val emptyPrefix = Prefix.empty
    // Prefixes whose projected databases are large.
    var largePrefixes = mutable.Map(emptyPrefix.id -> emptyPrefix)
    while (largePrefixes.nonEmpty) {
      val numLocalFreqPatterns = localFreqPatterns.length
      logInfo(s"number of local frequent patterns: $numLocalFreqPatterns")
      if (localFreqPatterns.length > 1000000) {
        logWarning(
          s"""
             | Collected $numLocalFreqPatterns local frequent patterns. You may want to consider:
             |   1. increase minSupport,
             |   2. decrease maxPatternLength,
             |   3. increase maxLocalProjDBSize.
           """.stripMargin)
      }
      logInfo(s"number of small prefixes: ${smallPrefixes.size}")
      logInfo(s"number of large prefixes: ${largePrefixes.size}")
      val largePrefixArray = largePrefixes.values.toArray
      val freqPrefixes = postfixes.flatMap { postfix =>
          largePrefixArray.flatMap { prefix =>
            postfix.project(prefix).genPrefixItems.map { case (item, postfixSize) =>
              ((prefix.id, item), (1L, postfixSize))
            }
          }
        }.reduceByKey { case ((c0, s0), (c1, s1)) =>
          (c0 + c1, s0 + s1)
        }.filter { case (_, (c, _)) => c >= minCount }
        .collect()
      val newLargePrefixes = mutable.Map.empty[Int, Prefix]
      freqPrefixes.foreach { case ((id, item), (count, projDBSize)) =>
        val newPrefix = largePrefixes(id) :+ item
        localFreqPatterns += ((newPrefix.items :+ 0, count))
        if (newPrefix.length < maxPatternLength) {
          if (projDBSize > maxLocalProjDBSize) {
            newLargePrefixes += newPrefix.id -> newPrefix
          } else {
            smallPrefixes += newPrefix.id -> newPrefix
          }
        }
      }
      largePrefixes = newLargePrefixes
    }

    // Switch to local processing.
    val bcSmallPrefixes = sc.broadcast(smallPrefixes)
    val distributedFreqPattern = postfixes.flatMap { postfix =>
      bcSmallPrefixes.value.values.map { prefix =>
        (prefix.id, postfix.project(prefix).compressed)
      }.filter(_._2.nonEmpty)
    }.groupByKey().flatMap { case (id, projPostfixes) =>
      val prefix = bcSmallPrefixes.value(id)
      val localPrefixSpan = new LocalPrefixSpan(minCount, maxPatternLength - prefix.length)
      // TODO: We collect projected postfixes into memory. We should also compare the performance
      // TODO: of keeping them on shuffle files.
      localPrefixSpan.run(projPostfixes.toArray).map { case (pattern, count) =>
        (prefix.items ++ pattern, count)
      }
    }

    // Union local frequent patterns and distributed ones.
    val freqPatterns = (sc.parallelize(localFreqPatterns, 1) ++ distributedFreqPattern)
      .persist(StorageLevel.MEMORY_AND_DISK)
    freqPatterns
  }

  /**
   * Represents a prefix.
   * @param items: items in this prefix, using the internal format
   * @param length: length of this prefix, not counting 0
   */
  private[fpm] class Prefix private (val items: Array[Int], val length: Int) extends Serializable {

    /** A unique id for this prefix. */
    val id: Int = Prefix.nextId

    /** Expands this prefix by the input item. */
    def :+(item: Int): Prefix = {
      require(item != 0)
      if (item < 0) {
        new Prefix(items :+ -item, length + 1)
      } else {
        new Prefix(items ++ Array(0, item), length + 1)
      }
    }
  }

  private[fpm] object Prefix {
    /** Internal counter to generate unique IDs. */
    private val counter: AtomicInteger = new AtomicInteger(-1)

    /** Gets the next unique ID. */
    private def nextId: Int = counter.incrementAndGet()

    /** An empty [[Prefix]] instance. */
    val empty: Prefix = new Prefix(Array.empty, 0)
  }

  /**
   * An internal representation of a postfix from some projection.
   * We use one int array to store the items, which might also contains other items from the
   * original sequence.
   * Items are represented by positive integers, and items in each itemset must be distinct and
   * ordered.
   * we use 0 as the delimiter between itemsets.
   * For example, a sequence `<(12)(31)1>` is represented by `[0, 1, 2, 0, 1, 3, 0, 1, 0]`.
   * The postfix of this sequence w.r.t. to prefix `<1>` is `<(_2)(13)1>`.
   * We may reuse the original items array `[0, 1, 2, 0, 1, 3, 0, 1, 0]` to represent the postfix,
   * and mark the start index of the postfix, which is `2` in this example.
   * So the active items in this postfix are `[2, 0, 1, 3, 0, 1, 0]`.
   * We also remember the start indices of partial projections, the ones that split an itemset.
   * For example, another possible partial projection w.r.t. `<1>` is <(_3)1>`.
   * We remember the start indices of partial projections, which is `[2, 5]` in this example.
   * This data structure makes it easier to do projections.
   *
   * @param items an int array containing this postfix with 0 as the delimiter
   * @param partialStarts start indices of possible partial projections, strictly increasing
   */
  private[fpm] class Postfix(
      val items: Array[Int],
      val start: Int = 0,
      val partialStarts: Array[Int] = Array.empty) extends Serializable {

    require(items.last == 0, "The last item in a postfix must be zero.")

    /**
     * Start index of the first full itemset contained in this postfix.
     */
    private[this] def fullStart: Int = {
      var i = start
      while (items(i) != 0) {
        i += 1
      }
      i
    }

    /**
     * Generates length-1 prefix items of this postfix with the corresponding postfix sizes.
     * There are two types of prefix items:
     *   a) the item can be assembled to the last itemset of the prefix, where we flip the sign in
     *      the output,
     *   b) the item can be appended to the prefix.
     * @return an iterator of (prefix item, corresponding postfix size)
     */
    def genPrefixItems: Iterator[(Int, Long)] = {
      val n1 = items.length - 1
      // For each unique item (subject to sign) in this sequence, we output exact one split.
      // TODO: use PrimitiveKeyOpenHashMap
      val prefixes = mutable.Map.empty[Int, Long]
      // a) items that can be assembled to the last itemset of the prefix
      partialStarts.foreach { start =>
        var i = start
        var x = -items(i)
        while (x != 0) {
          if (!prefixes.contains(x)) {
            prefixes(x) = n1 - i
          }
          i += 1
          x = -items(i)
        }
      }
      // b) items that can be appended to the prefix
      var i = fullStart
      while (i < n1) {
        val x = items(i)
        if (x != 0 && !prefixes.contains(x)) {
          prefixes(x) = n1 - i
        }
        i += 1
      }
      prefixes.toIterator
    }

    /** Tests whether this postfix is non-empty. */
    def nonEmpty: Boolean = items.length > start + 1

    /**
     * Projects this postfix with respect to the input prefix item.
     * @param prefix prefix item. If prefix is positive, we match items in any full itemset; if it
     *               is negative, we do partial projections.
     * @return the projected postfix
     */
    def project(prefix: Int): Postfix = {
      require(prefix != 0)
      val n1 = items.length - 1
      var matched = false
      var newStart = n1
      val newPartialStarts = mutable.ArrayBuilder.make[Int]
      if (prefix < 0) {
        // Search for partial projections.
        val target = -prefix
        partialStarts.foreach { start =>
          var i = start
          var x = items(i)
          while (x != target && x != 0) {
            i += 1
            x = items(i)
          }
          if (x == target) {
            i += 1
            if (!matched) {
              newStart = i
              matched = false
            }
            if (items(i) != 0) {
              newPartialStarts += i
            }
          }
        }
      } else {
        // Search for items in full itemsets.
        // Though the items are ordered in each itemsets, they should be small in practice.
        // So a sequential scan is sufficient here, compared to bisection search.
        val target = prefix
        var i = fullStart
        while (i < n1) {
          val x = items(i)
          if (x == target) {
            if (!matched) {
              newStart = i
              matched = true
            }
            if (items(i + 1) != 0) {
              newPartialStarts += i + 1
            }
          }
          i += 1
        }
      }
      new Postfix(items, newStart, newPartialStarts.result())
    }

    /**
     * Projects this postfix with respect to the input prefix.
     */
    private def project(prefix: Array[Int]): Postfix = {
      var partial = true
      var cur = this
      var i = 0
      val np = prefix.length
      while (i < np && cur.nonEmpty) {
        val x = prefix(i)
        if (x == 0) {
          partial = false
        } else {
          if (partial) {
            cur = cur.project(-x)
          } else {
            cur = cur.project(x)
            partial = true
          }
        }
        i += 1
      }
      cur
    }

    /**
     * Projects this postfix with respect to the input prefix.
     */
    def project(prefix: Prefix): Postfix = project(prefix.items)

    /**
     * Returns the same sequence with compressed storage if possible.
     */
    def compressed: Postfix = {
      if (start > 0) {
        new Postfix(items.slice(start, items.length), 0, partialStarts.map(_ - start))
      } else {
        this
      }
    }
  }

  /**
   * Represents a frequence sequence.
   * @param sequence a sequence of itemsets stored as an Array of Arrays
   * @param freq frequency
   * @tparam Item item type
   */
  class FreqSequence[Item](val sequence: Array[Array[Item]], val freq: Long) extends Serializable {
    /**
     * Returns sequence as a Java List of lists for Java users.
     */
    def javaSequence: ju.List[ju.List[Item]] = sequence.map(_.toList.asJava).toList.asJava
  }
}

/**
 * Model fitted by [[PrefixSpan]]
 * @param freqSequences frequent sequences
 * @tparam Item item type
 */
class PrefixSpanModel[Item](val freqSequences: RDD[PrefixSpan.FreqSequence[Item]])
  extends Serializable
