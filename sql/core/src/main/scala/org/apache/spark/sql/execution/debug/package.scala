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

package org.apache.spark.sql.execution

import java.io.StringWriter
import java.util.Collections

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.internal.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Attribute, Expression}
import org.apache.spark.sql.catalyst.expressions.BindReferences.bindReferences
import org.apache.spark.sql.catalyst.expressions.codegen.{ByteCodeStats, CodeFormatter, CodegenContext, CodeGenerator, ExprCode}
import org.apache.spark.sql.catalyst.json.{JacksonGenerator, JSONOptions}
import org.apache.spark.sql.catalyst.plans.logical.{DebugInlineColumnsCount, LogicalPlan}
import org.apache.spark.sql.catalyst.plans.physical.Partitioning
import org.apache.spark.sql.catalyst.trees.TreeNodeRef
import org.apache.spark.sql.catalyst.util.{ArrayData, MapData, StringConcat}
import org.apache.spark.sql.execution.adaptive.{AdaptiveSparkPlanExec, QueryStageExec}
import org.apache.spark.sql.execution.streaming.{StreamExecution, StreamingQueryWrapper}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.streaming.StreamingQuery
import org.apache.spark.sql.types.{ArrayType, DataType, MapType, StructType, VariantType}
import org.apache.spark.sql.vectorized.ColumnarBatch
import org.apache.spark.unsafe.types.VariantVal
import org.apache.spark.util.{AccumulatorV2, LongAccumulator, Utils}

/**
 * Contains methods for debugging query execution.
 *
 * Usage:
 * {{{
 *   import org.apache.spark.sql.execution.debug._
 *   sql("SELECT 1").debug()
 *   sql("SELECT 1").debugCodegen()
 * }}}
 *
 * or for streaming case (structured streaming):
 * {{{
 *   import org.apache.spark.sql.execution.debug._
 *   val query = df.writeStream.<...>.start()
 *   query.debugCodegen()
 * }}}
 *
 * Note that debug in structured streaming is not supported, because it doesn't make sense for
 * streaming to execute batch once while main query is running concurrently.
 */
package object debug {

  /** Helper function to evade the println() linter. */
  private def debugPrint(msg: String): Unit = {
    // scalastyle:off println
    println(msg)
    // scalastyle:on println
  }

  /**
   * Get WholeStageCodegenExec subtrees and the codegen in a query plan into one String
   *
   * @param plan the query plan for codegen
   * @return single String containing all WholeStageCodegen subtrees and corresponding codegen
   */
  def codegenString(plan: SparkPlan): String = {
    val concat = new StringConcat()
    writeCodegen(concat.append, plan)
    concat.toString
  }

  def writeCodegen(append: String => Unit, plan: SparkPlan): Unit = {
    val codegenSeq = codegenStringSeq(plan)
    append(s"Found ${codegenSeq.size} WholeStageCodegen subtrees.\n")
    for (((subtree, code, codeStats), i) <- codegenSeq.zipWithIndex) {
      val usedConstPoolRatio = if (codeStats.maxConstPoolSize > 0) {
        val rt = 100.0 * codeStats.maxConstPoolSize / CodeGenerator.MAX_JVM_CONSTANT_POOL_SIZE
        "(%.2f%% used)".format(rt)
      } else {
        ""
      }
      val codeStatsStr = s"maxMethodCodeSize:${codeStats.maxMethodCodeSize}; " +
        s"maxConstantPoolSize:${codeStats.maxConstPoolSize}$usedConstPoolRatio; " +
        s"numInnerClasses:${codeStats.numInnerClasses}"
      append(s"== Subtree ${i + 1} / ${codegenSeq.size} ($codeStatsStr) ==\n")
      append(subtree)
      append("\nGenerated code:\n")
      append(s"$code\n")
    }
  }

  /**
   * Get WholeStageCodegenExec subtrees and the codegen in a query plan
   *
   * @param plan the query plan for codegen
   * @return Sequence of WholeStageCodegen subtrees and corresponding codegen
   */
  def codegenStringSeq(plan: SparkPlan): Seq[(String, String, ByteCodeStats)] = {
    val codegenSubtrees = new collection.mutable.HashSet[WholeStageCodegenExec]()

    def findSubtrees(plan: SparkPlan): Unit = {
      plan foreach {
        case s: WholeStageCodegenExec =>
          codegenSubtrees += s
        case p: AdaptiveSparkPlanExec =>
          // Find subtrees from current executed plan of AQE.
          findSubtrees(p.executedPlan)
        case s: QueryStageExec =>
          findSubtrees(s.plan)
        case s =>
          s.subqueries.foreach(findSubtrees)
      }
    }

    findSubtrees(plan)
    codegenSubtrees.toSeq.sortBy(_.codegenStageId).map { subtree =>
      val (_, source) = subtree.doCodeGen()
      val codeStats = try {
        CodeGenerator.compile(source)._2
      } catch {
        case NonFatal(_) =>
          ByteCodeStats.UNAVAILABLE
      }
      (subtree.toString, CodeFormatter.format(source), codeStats)
    }
  }

  /**
   * Get WholeStageCodegenExec subtrees and the codegen in a query plan into one String
   *
   * @param query the streaming query for codegen
   * @return single String containing all WholeStageCodegen subtrees and corresponding codegen
   */
  def codegenString(query: StreamingQuery): String = {
    val w = asStreamExecution(query)
    if (w.lastExecution != null) {
      codegenString(w.lastExecution.executedPlan)
    } else {
      "No physical plan. Waiting for data."
    }
  }

  /**
   * Get WholeStageCodegenExec subtrees and the codegen in a query plan
   *
   * @param query the streaming query for codegen
   * @return Sequence of WholeStageCodegen subtrees and corresponding codegen
   */
  def codegenStringSeq(query: StreamingQuery): Seq[(String, String, ByteCodeStats)] = {
    val w = asStreamExecution(query)
    if (w.lastExecution != null) {
      codegenStringSeq(w.lastExecution.executedPlan)
    } else {
      Seq.empty
    }
  }

  private def asStreamExecution(query: StreamingQuery): StreamExecution = query match {
    case wrapper: StreamingQueryWrapper => wrapper.streamingQuery
    case q: StreamExecution => q
    case _ => throw new IllegalArgumentException("Parameter should be an instance of " +
      "StreamExecution!")
  }

  /**
   * Augments [[Dataset]]s with debug methods.
   */
  implicit class DebugQuery(query: Dataset[_]) extends Logging {
    def debug(): Unit = {
      val visited = new collection.mutable.HashSet[TreeNodeRef]()
      val debugPlan = query.queryExecution.executedPlan transform {
        case s: SparkPlan if !visited.contains(new TreeNodeRef(s)) =>
          visited += new TreeNodeRef(s)
          DebugExec(s)
      }
      debugPrint(s"Results returned: ${debugPlan.execute().count()}")
      debugPlan.foreach {
        case d: DebugExec => d.dumpStats()
        case _ =>
      }
    }

    /**
     * Prints to stdout all the generated code found in this plan (i.e. the output of each
     * WholeStageCodegen subtree).
     */
    def debugCodegen(): Unit = {
      debugPrint(codegenString(query.queryExecution.executedPlan))
    }

    /**
     * Counts the occurrence of values for the specified column combinations and periodically
     * prints the results to stdout. Results will not have perfect accuracy because it only
     * maintains the top K values. This is useful for identifying which values are creating skew
     * in a column.
     * @param columns The combination of columns to count the value occurrences for
     */
    def inlineColumnsCount(columns: Column *): Dataset[_] = {
      val plan = DebugInlineColumnsCount(query.logicalPlan, columns.map(_.expr))
      Dataset.ofRows(query.sparkSession, plan)
    }
  }

  implicit class DebugStreamQuery(query: StreamingQuery) extends Logging {
    def debugCodegen(): Unit = {
      debugPrint(codegenString(query))
    }
  }

  class SetAccumulator[T] extends AccumulatorV2[T, java.util.Set[T]] {
    private val _set = Collections.synchronizedSet(new java.util.HashSet[T]())

    override def isZero: Boolean = _set.isEmpty

    override def copy(): AccumulatorV2[T, java.util.Set[T]] = {
      val newAcc = new SetAccumulator[T]()
      newAcc._set.addAll(_set)
      newAcc
    }

    override def reset(): Unit = _set.clear()

    override def add(v: T): Unit = _set.add(v)

    override def merge(other: AccumulatorV2[T, java.util.Set[T]]): Unit = {
      _set.addAll(other.value)
    }

    override def value: java.util.Set[T] = _set
  }

  case class DebugExec(child: SparkPlan) extends UnaryExecNode with CodegenSupport {
    def output: Seq[Attribute] = child.output

    /**
     * A collection of metrics for each column of output.
     */
    case class ColumnMetrics() {
      val elementTypes = new SetAccumulator[String]
      sparkContext.register(elementTypes)
    }

    val tupleCount: LongAccumulator = sparkContext.longAccumulator

    val numColumns: Int = child.output.size
    val columnStats: Array[ColumnMetrics] = Array.fill(child.output.size)(new ColumnMetrics())

    def dumpStats(): Unit = {
      debugPrint(s"== ${child.simpleString(SQLConf.get.maxToStringFields)} ==")
      debugPrint(s"Tuples output: ${tupleCount.value}")
      child.output.zip(columnStats).foreach { case (attr, metric) =>
        // This is called on driver. All accumulator updates have a fixed value. So it's safe to use
        // `asScala` which accesses the internal values using `java.util.Iterator`.
        val actualDataTypes = metric.elementTypes.value.asScala.mkString("{", ",", "}")
        debugPrint(s" ${attr.name} ${attr.dataType}: $actualDataTypes")
      }
    }

    protected override def doExecute(): RDD[InternalRow] = {
      val evaluatorFactory = new DebugEvaluatorFactory(tupleCount, numColumns,
        columnStats.map(_.elementTypes), output)
      if (conf.usePartitionEvaluator) {
        child.execute().mapPartitionsWithEvaluator(evaluatorFactory)
      } else {
        child.execute().mapPartitionsWithIndex { (index, iter) =>
          evaluatorFactory.createEvaluator().eval(index, iter)
        }
      }
    }

    override def outputPartitioning: Partitioning = child.outputPartitioning

    override def inputRDDs(): Seq[RDD[InternalRow]] = {
      child.asInstanceOf[CodegenSupport].inputRDDs()
    }

    override def doProduce(ctx: CodegenContext): String = {
      child.asInstanceOf[CodegenSupport].produce(ctx, this)
    }

    override def doConsume(ctx: CodegenContext, input: Seq[ExprCode], row: ExprCode): String = {
      consume(ctx, input)
    }

    override def doExecuteBroadcast[T](): Broadcast[T] = {
      child.executeBroadcast()
    }

    override def doExecuteColumnar(): RDD[ColumnarBatch] = {
      child.executeColumnar()
    }

    override def supportsColumnar: Boolean = child.supportsColumnar

    override protected def withNewChildInternal(newChild: SparkPlan): DebugExec =
      copy(child = newChild)
  }

  case class DebugInlineColumnsCountExec(
    child: SparkPlan,
    sampleColumns: Seq[Expression]) extends UnaryExecNode {

    private val jsonOptions = new JSONOptions(Map.empty[String, String], "UTC")

    private val accumulator = new DebugAccumulator
    accumulator.register(
      session.sparkContext,
      Some(s"${child.nodeName} top values for ${sampleColumns.mkString(",")}"))

    override protected def withNewChildInternal(newChild: SparkPlan): DebugInlineColumnsCountExec =
      copy(child = newChild)

    override protected def doExecute(): RDD[InternalRow] = {
      val exprs = bindReferences[Expression](sampleColumns, child.output)

      child.execute().mapPartitions { iter =>
        iter.map { row =>
          val sampleVals = exprs.map { expr => valToString(expr.dataType, expr.eval(row)) }
          accumulator.add(sampleVals.mkString(","))
          row
        }
      }
    }

    private def valToString(dataType: DataType, value: Any): String = {
      dataType match {
        case s: StructType =>
          Utils.tryWithResource(new StringWriter()) { writer =>
            val gen = new JacksonGenerator(s, writer, jsonOptions)
            gen.write(value.asInstanceOf[InternalRow])
            gen.flush()
            writer.toString
          }
        case a: ArrayType =>
          Utils.tryWithResource(new StringWriter()) { writer =>
            val gen = new JacksonGenerator(a, writer, jsonOptions)
            gen.write(value.asInstanceOf[ArrayData])
            gen.flush()
            writer.toString
          }
        case m: MapType =>
          Utils.tryWithResource(new StringWriter()) { writer =>
            val gen = new JacksonGenerator(m, writer, jsonOptions)
            gen.write(value.asInstanceOf[MapData])
            gen.flush()
            writer.toString
          }
        case v: VariantType =>
          Utils.tryWithResource(new StringWriter()) { writer =>
            val gen = new JacksonGenerator(v, writer, jsonOptions)
            gen.write(value.asInstanceOf[VariantVal])
            gen.flush()
            writer.toString
          }
        case _ => value.toString
      }
    }

    override def output: Seq[Attribute] = child.output
  }

  object DebugPlanner extends SparkStrategy {
    override def apply(plan: LogicalPlan): Seq[SparkPlan] = {
      plan match {
        case DebugInlineColumnsCount(child, sampleColumns) =>
          DebugInlineColumnsCountExec(planLater(child), sampleColumns) :: Nil
        case _ => Nil
      }
    }
  }

  class DebugAccumulator extends AccumulatorV2[String, Map[String, Long]]  {
    private val keyToCount = mutable.Map.empty[String, Long]
    private val countToKeys = mutable.TreeMap.empty[Long, mutable.Set[String]]

    /**
     * Returns if this accumulator is zero value or not. e.g. for a counter accumulator, 0 is zero
     * value; for a list accumulator, Nil is zero value.
     */
    override def isZero: Boolean = this.synchronized { keyToCount.isEmpty }

    /**
     * Creates a new copy of this accumulator.
     */
    override def copy(): DebugAccumulator = {
      val newAcc = new DebugAccumulator()
      newAcc.merge(this)
      newAcc
    }

    /**
     * Resets this accumulator, which is zero value. i.e. call `isZero` must
     * return true.
     */
    override def reset(): Unit = this.synchronized { keyToCount.clear() }

    /**
     * Takes the inputs and accumulates.
     */
    override def add(v: String): Unit = add(v, 1)

    private def add(v: String, add: Long): Unit = this.synchronized {
      val count = keyToCount.getOrElse(v, 0L) + add
      keyToCount.put(v, count)

      val keys = countToKeys.getOrElseUpdate(count, mutable.Set[String]())
      keys.add(v)

      // TODO make 10 configurable
      if (keyToCount.size > 10) {
        dropSmallest()
      }
    }

    private def dropSmallest(): Unit = {
      val keys = countToKeys.head._2
      val keyToDrop = keys.head

      keys.remove(keyToDrop)
      keyToCount.remove(keyToDrop)

      if (keys.isEmpty) {
        countToKeys.remove(countToKeys.head._1)
      }
    }

    /**
     * Merges another same-type accumulator into this one and update its state, i.e. this should be
     * merge-in-place.
     */
    override def merge(other: AccumulatorV2[String, Map[String, Long]]): Unit = this.synchronized {
      other match {
        case o: DebugAccumulator => o.keyToCount.foreach { case (k, v) => add(k, v) }
        case _ =>
          throw new UnsupportedOperationException(s"Cannot merge with ${other.getClass.getName}")
      }
    }

    /**
     * Defines the current value of this accumulator
     */
    override def value: Map[String, Long] = this.synchronized {
      keyToCount.toMap
    }
  }
}
