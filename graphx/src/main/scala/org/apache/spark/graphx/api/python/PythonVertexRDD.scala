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

package org.apache.spark.graphx.api.python

import java.util.{ArrayList => JArrayList, List => JList, Map => JMap}

import org.apache.spark.api.java.JavaRDD
import org.apache.spark.graphx.VertexRDD
import org.apache.spark.rdd.RDD

//class PythonVertexRDD (
//    parent: JavaRDD[Array[Byte]],
//    command: Array[Byte],
//    envVars: JMap[String, String],
//    pythonIncludes: JList[String],
//    preservePartitoning: Boolean,
//    pythonExec: String,
//    broadcastVars: JList[Broadcast[Array[Byte]]],
//    accumulator: Accumulator[JList[Array[Byte]]],
//    targetStorageLevel: String = "MEMORY_ONLY")
//  extends RDD[Array[Byte]](parent) {

class PythonVertexRDD(parent: RDD[_], schema: String) extends Serializable {


//  val bufferSize = conf.getInt("spark.buffer.size", DEFAULT_SPARK_BUFFER_SIZE)
//  val reuse_worker = conf.getBoolean("spark.python.worker.reuse", true)

//  /**
//   * :: DeveloperApi ::
//   * Implemented by subclasses to compute a given partition.
//   */
//  override def compute(split: Partition, context: TaskContext): Iterator[Array[Byte]] = {
//      null
//  }

  /**
   * Implemented by subclasses to return the set of partitions in this RDD. This method will only
   * be called once, so it is safe to implement a time-consuming computation in it.
   */
//  override def getPartitions: Array[Partition] = ???

//  def this(parent: JavaRDD[Array[Byte]], command: String, preservePartitioning: Boolean) {
//    this(parent, null, null, preservePartitioning, "MEMORY_ONLY")
//    System.out.println("PythonVertexRDD constructor")
//  }

//  val asJavaRDD : JavaRDD[Array[Byte]] = JavaRDD.fromRDD(this)

  def toVertexRDD[VD](pyRDD: RDD[_], schema: String): JavaRDD[Array[Byte]] = {
//    new VertexRDD[VD](PythonRDD.pythonToJava(pyRDD, true), StorageLevel.MEMORY_ONLY)
    System.out.println("In PythonVertexRDD.toVertexRDD()")
    val propertySchema = new VertexSchema(schema)
    vertices = new VertexRDD[VertexSchema](pyRDD.mapPartitions())
    null
  }
}

object PythonVertexRDD {
  val DEFAULT_SPARK_BUFFER_SIZE = 65536

  def toVertexRDD(parent: RDD[_], schema: String) : JavaRDD[Array[Byte]] = {
    val pyRDD = new PythonVertexRDD(parent, schema)
    pyRDD.toVertexRDD(parent, schema)
  }
}

class VertexSchema(val schemaString: String) {

  /**
   * The vertex property schema is
   * @param schemaString
   * @return
   */
  def fromString(schemaString: String) : List[String] =
    schemaString.split(" ").toList

}
