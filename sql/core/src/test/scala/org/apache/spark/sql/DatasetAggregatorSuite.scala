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

package org.apache.spark.sql

import org.apache.spark.sql.catalyst.encoders.Encoder
import org.apache.spark.sql.functions._

import scala.language.postfixOps

import org.apache.spark.sql.test.SharedSQLContext

import org.apache.spark.sql.expressions.Aggregator

/** An `Aggregator` that adds up any numeric type returned by the given function. */
class SumOf[I, N : Numeric](f: I => N) extends Aggregator[I, N, N] {
  val numeric = implicitly[Numeric[N]]

  override def zero: N = numeric.zero

  override def reduce(b: N, a: I): N = numeric.plus(b, f(a))

  override def merge(b1: N, b2: N): N = numeric.plus(b1, b2)

  override def present(reduction: N): N = reduction
}

object TypedAverage extends Aggregator[(String, Int), (Long, Long), Double] {
  override def zero: (Long, Long) = (0, 0)

  override def reduce(countAndSum: (Long, Long), input: (String, Int)): (Long, Long) = {
    (countAndSum._1 + 1, countAndSum._2 + input._2)
  }

  override def merge(b1: (Long, Long), b2: (Long, Long)): (Long, Long) = {
    (b1._1 + b2._1, b1._2 + b2._2)
  }

  override def present(countAndSum: (Long, Long)): Double = countAndSum._2 / countAndSum._1
}

object ComplexResultAgg extends Aggregator[(String, Int), (Long, Long), (Long, Long)] {

  override def zero: (Long, Long) = (0, 0)

  override def reduce(countAndSum: (Long, Long), input: (String, Int)): (Long, Long) = {
    (countAndSum._1 + 1, countAndSum._2 + input._2)
  }

  override def merge(b1: (Long, Long), b2: (Long, Long)): (Long, Long) = {
    (b1._1 + b2._1, b1._2 + b2._2)
  }

  override def present(reduction: (Long, Long)): (Long, Long) = reduction
}

class DatasetAggregatorSuite extends QueryTest with SharedSQLContext {

  import testImplicits._

  def sum[I, N : Numeric : Encoder](f: I => N): TypedColumn[I, N] =
    new SumOf(f).toColumn

  test("typed aggregation: TypedAggregator") {
    val ds = Seq(("a", 10), ("a", 20), ("b", 1), ("b", 2), ("c", 1)).toDS()

    checkAnswer(
      ds.groupBy(_._1).agg(sum(_._2)),
      ("a", 30), ("b", 3), ("c", 1))
  }

  test("typed aggregation: TypedAggregator, expr, expr") {
    val ds = Seq(("a", 10), ("a", 20), ("b", 1), ("b", 2), ("c", 1)).toDS()

    checkAnswer(
      ds.groupBy(_._1).agg(
        sum(_._2),
        expr("sum(_2)").as[Int],
        count("*")),
      ("a", 30, 30, 2L), ("b", 3, 3, 2L), ("c", 1, 1, 1L))
  }

  test("typed aggregation: complex case") {
    val ds = Seq("a" -> 1, "a" -> 3, "b" -> 3).toDS()

    checkAnswer(
      ds.groupBy(_._1).agg(
        expr("avg(_2)").as[Double],
        TypedAverage.toColumn),
      ("a", 2.0, 2.0), ("b", 3.0, 3.0))
  }

  test("typed aggregation: complex result type") {
    val ds = Seq("a" -> 1, "a" -> 3, "b" -> 3).toDS()

    checkAnswer(
      ds.groupBy(_._1).agg(
        expr("avg(_2)").as[Double],
        ComplexResultAgg.toColumn),
      ("a", 2.0, (2L, 4L)), ("b", 3.0, (1L, 3L)))
  }
}
