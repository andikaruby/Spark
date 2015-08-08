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

package org.apache.spark.sql.execution.joins

import java.util.NoSuchElementException

import org.apache.spark.sql.catalyst.InternalRow

/**
 * An internal iterator interface which presents a more restrictive API than
 * [[scala.collection.Iterator]].
 *
 * One major departure from the Scala iterator API is the fusing of the `hasNext()` and `next()`
 * calls: Scala's iterator allows users to call `hasNext()` without immediately advancing the
 * iterator to consume the next row, whereas RowIterator combines these calls into a single
 * [[advanceNext()]] method.
 *
 * In some cases, significant work may need to be performed in order to determine whether there is a
 * next element (for example, a `filter`ed iterator may need to consume many elements of its parent
 * iterator in order to determine whether there is a next row). As a result, many Scala iterators
 * perform implicit internal buffering, which can cause problems with iterators that return the same
 * mutable Row on every `next()` call. If we call `.filter()` on a Scala iterator of InternalRow,
 * calling `hasNext()` may mutate the row that has already been returned from the iterator. This
 * can cause problems unless the caller expects to immediately call `next()` after `hasNext()`
 * returned true.
 *
 * We can guard against this anomaly by automatically copying rows before returning them to a Scala
 * iterator; RowIterator's [[toScala]] method returns a wrapper which automatically performs this
 * defensive copying. These copies carry a performance penalty, though, so ideally we should avoid
 * this. The `RowIterator.fromScala` method wraps a Scala iterator behind our more restrictive
 * iterator interface. As an optimization, calling `RowIterator.fromScala` on a wrapped RowIterator
 * will return the underlying RowIterator, avoiding the copying. Thus, by gradually re-writing
 * operators to use our [[RowIterator]] wrappers we can safely remove this defensive row copying.
 */
private[sql] abstract class RowIterator {
  /**
   * Advance this iterator by a single row. Returns `false` if this iterator has no more rows
   * and `true` otherwise. If this returns `true`, then the new row can be retrieved by calling
   * [[getRow]].
   */
  def advanceNext(): Boolean

  /**
   * Retrieve the row from this iterator. This method is idempotent. It is illegal to call this
   * method after [[advanceNext()]] has returned `false`.
   */
  def getRow: InternalRow

  /**
   * Convert this RowIterator into a [[scala.collection.Iterator]].
   */
  def toScala: Iterator[InternalRow] = new RowIteratorToScala(this)
}

object RowIterator {
  def fromScala(scalaIter: Iterator[InternalRow]): RowIterator = {
    scalaIter match {
      case wrappedRowIter: RowIteratorToScala if !wrappedRowIter._wasUsed => wrappedRowIter.rowIter
      case _ => new RowIteratorFromScala(scalaIter)
    }
  }
}

private final class RowIteratorToScala(val rowIter: RowIterator) extends Iterator[InternalRow] {
  var _wasUsed: Boolean = false
  private [this] var _hasNext: Boolean = false
  override def hasNext: Boolean = {
    if (!_wasUsed) {
      _hasNext = rowIter.advanceNext()
      _wasUsed = true
    }
    _hasNext
  }
  override def next(): InternalRow = {
    if (!hasNext) throw new NoSuchElementException
    val row: InternalRow = rowIter.getRow.copy()
    _hasNext = rowIter.advanceNext()
    row
  }
}

private final class RowIteratorFromScala(scalaIter: Iterator[InternalRow]) extends RowIterator {
  private[this] var _next: InternalRow = null
  override def advanceNext(): Boolean = {
    if (scalaIter.hasNext) {
      _next = scalaIter.next()
      true
    } else {
      _next = null
      false
    }
  }
  override def getRow: InternalRow = _next
  override def toScala: Iterator[InternalRow] = scalaIter
}
