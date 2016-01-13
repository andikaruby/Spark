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

import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.mutable.ArrayBuffer

import org.apache.spark.Logging
import org.apache.spark.rdd.{RDD, RDDOperationScope}
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.sql.catalyst.{CatalystTypeConverters, InternalRow}
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen._
import org.apache.spark.sql.catalyst.plans.QueryPlan
import org.apache.spark.sql.catalyst.plans.physical._
import org.apache.spark.sql.execution.metric.{LongSQLMetric, SQLMetric}
import org.apache.spark.sql.types.DataType

/**
 * The base class for physical operators.
 */
abstract class SparkPlan extends QueryPlan[SparkPlan] with Logging with Serializable {

  /**
   * A handle to the SQL Context that was used to create this plan.   Since many operators need
   * access to the sqlContext for RDD operations or configuration this field is automatically
   * populated by the query planning infrastructure.
   */
  @transient
  protected[spark] final val sqlContext = SQLContext.getActive().getOrElse(null)

  protected def sparkContext = sqlContext.sparkContext

  // sqlContext will be null when we are being deserialized on the slaves.  In this instance
  // the value of subexpressionEliminationEnabled will be set by the desserializer after the
  // constructor has run.
  val subexpressionEliminationEnabled: Boolean = if (sqlContext != null) {
    sqlContext.conf.subexpressionEliminationEnabled
  } else {
    false
  }

  /**
   * Whether the "prepare" method is called.
   */
  private val prepareCalled = new AtomicBoolean(false)

  /** Overridden make copy also propogates sqlContext to copied plan. */
  override def makeCopy(newArgs: Array[AnyRef]): SparkPlan = {
    SQLContext.setActive(sqlContext)
    super.makeCopy(newArgs)
  }

  /**
   * Return all metadata that describes more details of this SparkPlan.
   */
  private[sql] def metadata: Map[String, String] = Map.empty

  /**
   * Return all metrics containing metrics of this SparkPlan.
   */
  private[sql] def metrics: Map[String, SQLMetric[_, _]] = Map.empty

  /**
   * Return a LongSQLMetric according to the name.
   */
  private[sql] def longMetric(name: String): LongSQLMetric =
    metrics(name).asInstanceOf[LongSQLMetric]

  // TODO: Move to `DistributedPlan`
  /** Specifies how data is partitioned across different nodes in the cluster. */
  def outputPartitioning: Partitioning = UnknownPartitioning(0) // TODO: WRONG WIDTH!

  /** Specifies any partition requirements on the input data for this operator. */
  def requiredChildDistribution: Seq[Distribution] =
    Seq.fill(children.size)(UnspecifiedDistribution)

  /** Specifies how data is ordered in each partition. */
  def outputOrdering: Seq[SortOrder] = Nil

  /** Specifies sort order for each partition requirements on the input data for this operator. */
  def requiredChildOrdering: Seq[Seq[SortOrder]] = Seq.fill(children.size)(Nil)

  /**
   * Returns the result of this query as an RDD[InternalRow] by delegating to doExecute
   * after adding query plan information to created RDDs for visualization.
   * Concrete implementations of SparkPlan should override doExecute instead.
   */
  final def execute(): RDD[InternalRow] = {
    RDDOperationScope.withScope(sparkContext, nodeName, false, true) {
      prepare()

      if (sqlContext.conf.wholeStageEnabled && supportCodeGen
        // Expression with CodegenFallback does not work with whole stage codegen
        && !expressions.exists(_.find(_.isInstanceOf[CodegenFallback]).isDefined)
        // Whole stage codegen is only used when there are at least two levels of operators that
        // support it (save at least one projection/iterator).
        && children.exists(_.supportCodeGen)) {
        try {
          doCodeGen()
        } catch {
          case e: CompileFailure =>
            if (isTesting) {
              throw e
            } else {
              // Fallback if fail to compile the generated code.
              doExecute()
            }
        }
      } else {
        doExecute()
      }
    }
  }

  /**
   * Prepare a SparkPlan for execution. It's idempotent.
   */
  final def prepare(): Unit = {
    if (prepareCalled.compareAndSet(false, true)) {
      doPrepare()
      children.foreach(_.prepare())
    }
  }

  /**
   * Overridden by concrete implementations of SparkPlan. It is guaranteed to run before any
   * `execute` of SparkPlan. This is helpful if we want to set up some state before executing the
   * query, e.g., `BroadcastHashJoin` uses it to broadcast asynchronously.
   *
   * Note: the prepare method has already walked down the tree, so the implementation doesn't need
   * to call children's prepare methods.
   */
  protected def doPrepare(): Unit = {}

  /**
   * Overridden by concrete implementations of SparkPlan.
   * Produces the result of the query as an RDD[InternalRow]
   */
  protected def doExecute(): RDD[InternalRow]

  /**
    * Whether this SparkPlan support whole stage codegen or not.
    */
  protected def supportCodeGen: Boolean = false

  class CompileFailure(e: Exception) extends Exception {}

  /**
    * Returns an RDD of InternalRow that using generated code to process them.
    *
    * Here is the call graph of three SparkPlan, A and B support codegen, but C does not.
    *
    *   SparkPlan A            SparkPlan B          SparkPlan C
    * ===============================================================
    *
    * -> doExecute()
    *     |
    *  doCodeGen()
    *     |
    *  produce()
    *     |
    *  doProduce() -------->   produce()
    *                             |
    *                          doProduce()  -------> produce()
    *                                                   |
    *                                                doProduce() (fetch row from upstream)
    *                                                   |
    *                                                consume()
    *                          doConsume()  ------------|
    *                             |
    *  doConsume()  <-----    consume()
    *     |
    *   consume() (omit the rows)
    *
    * SparkPlan A and B should override doProduce() and doConsume().
    *
    * doCodeGen() will create a CodeGenContext, which will hold a list of variables for input,
    * used to generated code for BoundReference.
    */
  protected def doCodeGen(): RDD[InternalRow] = {
    val ctx = new CodeGenContext
    val (rdd, code) = produce(ctx, this)
    val exprType: String = classOf[Expression].getName
    val references = ctx.references.toArray
    val source = s"""
      public Object generate($exprType[] exprs) {
       return new GeneratedIterator(exprs);
      }

      class GeneratedIterator extends org.apache.spark.sql.execution.BufferedRowIterator {

       private $exprType[] expressions;
       ${ctx.declareMutableStates()}
       private UnsafeRow unsafeRow = new UnsafeRow(${output.length});

       public GeneratedIterator($exprType[] exprs) {
         expressions = exprs;
         ${ctx.initMutableStates()}
       }

       protected void processNext() {
         $code
       }
      }
     """
    // try to compile, will fallback if fail
    // println(s"${CodeFormatter.format(source)}")
    try {
      CodeGenerator.compile(source)
    } catch {
      case e: Exception =>
        throw new CompileFailure(e)
    }

    rdd.mapPartitions { iter =>
      val clazz = CodeGenerator.compile(source)
      val buffer = clazz.generate(references).asInstanceOf[BufferedRowIterator]
      buffer.process(iter)
      new Iterator[InternalRow] {
        override def hasNext: Boolean = buffer.hasNext
        override def next: InternalRow = buffer.next()
      }
    }
  }

  /**
    * Which SparkPlan is calling produce() of this one. It's itself for the first SparkPlan.
    */
  private var parent: SparkPlan = null

  /**
    * Returns an input RDD of InternalRow and Java source code to process them.
    */
  def produce(ctx: CodeGenContext, parent: SparkPlan): (RDD[InternalRow], String) = {
    this.parent = parent
    doProduce(ctx)
  }

  /**
    * Generate the Java source code to process, should be overrided by subclass to support codegen.
    *
    * doProduce() usually generate the framework, for example, aggregation could generate this:
    *
    *   if (!initialized) {
    *     # create a hash map, then build the aggregation hash map
    *     # call child.produce()
    *     initialized = true;
    *   }
    *   while (hashmap.hasNext()) {
    *     row = hashmap.next();
    *     # build the aggregation results
    *     # create varialbles for results
    *     # call consume(), wich will call parent.doConsume()
    *   }
    */
  protected def doProduce(ctx: CodeGenContext): (RDD[InternalRow], String) = {
    val exprs = output.zipWithIndex.map(x => new BoundReference(x._2, x._1.dataType, true))
    val columns = exprs.map(_.gen(ctx))
    val code = s"""
       |  while (input.hasNext()) {
       |   InternalRow i = (InternalRow) input.next();
       |   ${columns.map(_.code).mkString("\n")}
       |   ${consume(ctx, columns)}
       | }
     """.stripMargin
    (doExecute(), code)
  }

  /**
    * Consume the columns generated from current SparkPlan, call it's parent or create an iterator.
    */
  protected def consume(
      ctx: CodeGenContext,
      columns: Seq[GeneratedExpressionCode]): String = {

    assert(columns.length == output.length)
    // Save the generated columns, will be used to generate BoundReference by parent SparkPlan.
    ctx.currentVars = columns.toArray

    if (parent eq this) {
      // This is the first SparkPlan, omit the rows.
      if (columns.nonEmpty) {
        val colExprs = output.zipWithIndex.map { case (attr, i) =>
          BoundReference(i, attr.dataType, attr.nullable)
        }
        val code = GenerateUnsafeProjection.createCode(ctx, colExprs, false)
        s"""
           | ${code.code.trim}
           | currentRow = ${code.value};
           | return;
     """.stripMargin
      } else {
        // There is no columns
        s"""
           | currentRow = unsafeRow;
           | return;
       """.stripMargin
      }

    } else {
      parent.doConsume(ctx, this)
    }
  }

  /**
    * Generate the Java source code to process the rows from child SparkPlan.
    *
    * This should be override by subclass to support codegen.
    *
    * For example, Filter will generate the code like this:
    *
    *   # code to evaluate the predicate expression, result is isNull1 and value2
    *   if (isNull1 || value2) {
    *     # call consume(), which will call parent.doConsume()
    *   }
    */
  def doConsume(
    ctx: CodeGenContext,
    child: SparkPlan): String = {
    throw new UnsupportedOperationException
  }

  /**
   * Runs this query returning the result as an array.
   */
  def executeCollect(): Array[InternalRow] = {
    execute().map(_.copy()).collect()
  }

  /**
   * Runs this query returning the result as an array, using external Row format.
   */
  def executeCollectPublic(): Array[Row] = {
    val converter = CatalystTypeConverters.createToScalaConverter(schema)
    executeCollect().map(converter(_).asInstanceOf[Row])
  }

  /**
   * Runs this query returning the first `n` rows as an array.
   *
   * This is modeled after RDD.take but never runs any job locally on the driver.
   */
  def executeTake(n: Int): Array[InternalRow] = {
    if (n == 0) {
      return new Array[InternalRow](0)
    }

    val childRDD = execute().map(_.copy())

    val buf = new ArrayBuffer[InternalRow]
    val totalParts = childRDD.partitions.length
    var partsScanned = 0
    while (buf.size < n && partsScanned < totalParts) {
      // The number of partitions to try in this iteration. It is ok for this number to be
      // greater than totalParts because we actually cap it at totalParts in runJob.
      var numPartsToTry = 1L
      if (partsScanned > 0) {
        // If we didn't find any rows after the first iteration, just try all partitions next.
        // Otherwise, interpolate the number of partitions we need to try, but overestimate it
        // by 50%.
        if (buf.size == 0) {
          numPartsToTry = totalParts - 1
        } else {
          numPartsToTry = (1.5 * n * partsScanned / buf.size).toInt
        }
      }
      numPartsToTry = math.max(0, numPartsToTry)  // guard against negative num of partitions

      val left = n - buf.size
      val p = partsScanned.until(math.min(partsScanned + numPartsToTry, totalParts).toInt)
      val sc = sqlContext.sparkContext
      val res = sc.runJob(childRDD, (it: Iterator[InternalRow]) => it.take(left).toArray, p)

      res.foreach(buf ++= _.take(n - buf.size))
      partsScanned += p.size
    }

    buf.toArray
  }

  private[this] def isTesting: Boolean = sys.props.contains("spark.testing")

  protected def newMutableProjection(
      expressions: Seq[Expression], inputSchema: Seq[Attribute]): () => MutableProjection = {
    log.debug(s"Creating MutableProj: $expressions, inputSchema: $inputSchema")
    try {
      GenerateMutableProjection.generate(expressions, inputSchema)
    } catch {
      case e: Exception =>
        if (isTesting) {
          throw e
        } else {
          log.error("Failed to generate mutable projection, fallback to interpreted", e)
          () => new InterpretedMutableProjection(expressions, inputSchema)
        }
    }
  }

  protected def newPredicate(
      expression: Expression, inputSchema: Seq[Attribute]): (InternalRow) => Boolean = {
    try {
      GeneratePredicate.generate(expression, inputSchema)
    } catch {
      case e: Exception =>
        if (isTesting) {
          throw e
        } else {
          log.error("Failed to generate predicate, fallback to interpreted", e)
          InterpretedPredicate.create(expression, inputSchema)
        }
    }
  }

  protected def newOrdering(
      order: Seq[SortOrder], inputSchema: Seq[Attribute]): Ordering[InternalRow] = {
    try {
      GenerateOrdering.generate(order, inputSchema)
    } catch {
      case e: Exception =>
        if (isTesting) {
          throw e
        } else {
          log.error("Failed to generate ordering, fallback to interpreted", e)
          new InterpretedOrdering(order, inputSchema)
        }
    }
  }

  /**
   * Creates a row ordering for the given schema, in natural ascending order.
   */
  protected def newNaturalAscendingOrdering(dataTypes: Seq[DataType]): Ordering[InternalRow] = {
    val order: Seq[SortOrder] = dataTypes.zipWithIndex.map {
      case (dt, index) => new SortOrder(BoundReference(index, dt, nullable = true), Ascending)
    }
    newOrdering(order, Seq.empty)
  }
}

private[sql] trait LeafNode extends SparkPlan {
  override def children: Seq[SparkPlan] = Nil
  override def producedAttributes: AttributeSet = outputSet
}

private[sql] trait UnaryNode extends SparkPlan {
  def child: SparkPlan

  override def children: Seq[SparkPlan] = child :: Nil

  override def outputPartitioning: Partitioning = child.outputPartitioning
}

private[sql] trait BinaryNode extends SparkPlan {
  def left: SparkPlan
  def right: SparkPlan

  override def children: Seq[SparkPlan] = Seq(left, right)
}
