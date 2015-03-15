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

package org.apache.spark.api.python

import java.io.{DataOutput, DataInput}
import java.util.ArrayList

import com.google.common.base.Charsets.UTF_8

import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.{SparkContext, SparkException}

import scala.collection.JavaConversions


/**
 * A class to test Pyrolite serialization on the Scala side, that will be deserialized
 * in Python
 * @param str
 * @param int
 * @param double
 */
case class TestWritable(var str: String, var int: Int, var double: Double) extends Writable {
  def this() = this("", 0, 0.0)

  def getStr = str
  def setStr(str: String) { this.str = str }
  def getInt = int
  def setInt(int: Int) { this.int = int }
  def getDouble = double
  def setDouble(double: Double) { this.double = double }

  def write(out: DataOutput) = {
    out.writeUTF(str)
    out.writeInt(int)
    out.writeDouble(double)
  }

  def readFields(in: DataInput) = {
    str = in.readUTF()
    int = in.readInt()
    double = in.readDouble()
  }
}

private[python] class TestInputKeyConverter extends Converter[Any, Any] {
  override def convert(obj: Any) = {
    obj.asInstanceOf[IntWritable].get().toChar
  }
}

private[python] class TestInputValueConverter extends Converter[Any, Any] {
  import collection.JavaConversions._
  override def convert(obj: Any) = {
    val m = obj.asInstanceOf[MapWritable]
    seqAsJavaList(m.keySet.map(w => w.asInstanceOf[DoubleWritable].get()).toSeq)
  }
}

private[python] class TestOutputKeyConverter extends Converter[Any, Any] {
  override def convert(obj: Any) = {
    new Text(obj.asInstanceOf[Int].toString)
  }
}

private[python] class TestOutputValueConverter extends Converter[Any, Any] {
  import collection.JavaConversions._
  override def convert(obj: Any) = {
    new DoubleWritable(obj.asInstanceOf[java.util.Map[Double, _]].keySet().head)
  }
}

/**
 * This object contains method to generate SequenceFile test data and write it to a
 * given directory (probably a temp directory)
 */
object WriteInputFormatTestDataGenerator {

  def main(args: Array[String]) {
    val path = args(0)
    val sc = new JavaSparkContext("local[4]", "test-writables")
    generateData(path, sc)
  }

  def generateData(path: String, jsc: JavaSparkContext) {
    val sc = jsc.sc

    val basePath = s"$path/sftestdata/"
    val textPath = s"$basePath/sftext/"
    val intPath = s"$basePath/sfint/"
    val doublePath = s"$basePath/sfdouble/"
    val arrPath = s"$basePath/sfarray/"
    val narrPath = s"$basePath/sfnarray/"
    val mapPath = s"$basePath/sfmap/"
    val classPath = s"$basePath/sfclass/"
    val bytesPath = s"$basePath/sfbytes/"
    val boolPath = s"$basePath/sfbool/"
    val nullPath = s"$basePath/sfnull/"

    /*
     * Create test data for IntWritable, DoubleWritable, Text, BytesWritable,
     * BooleanWritable and NullWritable
     */
    val intKeys = Seq((1, "aa"), (2, "bb"), (2, "aa"), (3, "cc"), (2, "bb"), (1, "aa"))
    sc.parallelize(intKeys).saveAsSequenceFile(intPath)
    sc.parallelize(intKeys.map{ case (k, v) => (k.toDouble, v) }).saveAsSequenceFile(doublePath)
    sc.parallelize(intKeys.map{ case (k, v) => (k.toString, v) }).saveAsSequenceFile(textPath)
    sc.parallelize(intKeys.map{ case (k, v) => (k, v.getBytes(UTF_8)) }
      ).saveAsSequenceFile(bytesPath)
    val bools = Seq((1, true), (2, true), (2, false), (3, true), (2, false), (1, false))
    sc.parallelize(bools).saveAsSequenceFile(boolPath)
    sc.parallelize(intKeys).map{ case (k, v) =>
      (new IntWritable(k), NullWritable.get())
    }.saveAsSequenceFile(nullPath)

    // Create test data for DoubleArrayWritable
    val data = Seq(
      (1, Array()),
      (2, Array(3.0, 4.0, 5.0)),
      (3, Array(4.0, 5.0, 6.0))
    )
    sc.parallelize(data, numSlices = 2)
      .map{ case (k, v) =>
        val va = new DoubleArrayWritable
        va.set(v.map(new DoubleWritable(_)))
        (new IntWritable(k), va)
    }.saveAsNewAPIHadoopFile[SequenceFileOutputFormat[IntWritable, DoubleArrayWritable]](arrPath)

    // Create test data for NestedDoubleArrayWritable
    val nestedDoubleArrayData =
      Seq((1, Array(Array(-9.9, -8.8), Array(-7.7, -6.6, -5.5), Array(-4.4, -3.3, -2.2, -1.1))),
          (2, Array(Array(1.1, 2.2, 3.3, 4.4, 5.5), Array(6.6), Array(7.7, 8.8))))

    val ndaConverter = new NestedDoubleArrayToWritableConverter

    sc.parallelize(nestedDoubleArrayData, numSlices = 2)
      .map{ case (k, v) =>
      val nested = ndaConverter.convert(v)
       (new IntWritable(k), nested)
    }.saveAsNewAPIHadoopFile[SequenceFileOutputFormat[IntWritable, NestedDoubleArrayWritable]](
        narrPath)


    // Create test data for MapWritable, with keys DoubleWritable and values Text
    val mapData = Seq(
      (1, Map()),
      (2, Map(1.0 -> "cc")),
      (3, Map(2.0 -> "dd")),
      (2, Map(1.0 -> "aa")),
      (1, Map(3.0 -> "bb"))
    )
    sc.parallelize(mapData, numSlices = 2).map{ case (i, m) =>
      val mw = new MapWritable()
      m.foreach { case (k, v) =>
        mw.put(new DoubleWritable(k), new Text(v))
      }
      (new IntWritable(i), mw)
    }.saveAsSequenceFile(mapPath)

    // Create test data for arbitrary custom writable TestWritable
    val testClass = Seq(
      ("1", TestWritable("test1", 1, 1.0)),
      ("2", TestWritable("test2", 2, 2.3)),
      ("3", TestWritable("test3", 3, 3.1)),
      ("5", TestWritable("test56", 5, 5.5)),
      ("4", TestWritable("test4", 4, 4.2))
    )
    val rdd = sc.parallelize(testClass, numSlices = 2).map{ case (k, v) => (new Text(k), v) }
    rdd.saveAsNewAPIHadoopFile(classPath,
      classOf[Text], classOf[TestWritable],
      classOf[SequenceFileOutputFormat[Text, TestWritable]])
  }
}
