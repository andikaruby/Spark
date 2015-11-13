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

import scala.reflect.ClassTag

import org.apache.hadoop.fs.Path

import org.apache.spark._
import org.apache.spark.util.{CheckpointingIterator, SerializableConfiguration}

/**
 * An implementation of checkpointing that writes the RDD data to reliable storage.
 * This allows drivers to be restarted on failure with previously computed state.
 */
private[spark] class ReliableRDDCheckpointData[T: ClassTag](@transient private val rdd: RDD[T])
  extends RDDCheckpointData[T](rdd) with Logging {

  import CheckpointState._

  // The directory to which the associated RDD has been checkpointed to
  // This is assumed to be a non-local path that points to some reliable storage
  private val cpDir: String = {
    val _cpDir = ReliableRDDCheckpointData.checkpointPath(rdd.context, rdd.id)
      .map(_.toString)
      .getOrElse {
        throw new SparkException("Checkpoint dir must be specified.")
      }
    val path = new Path(_cpDir)
    val fs = new Path(_cpDir).getFileSystem(rdd.context.hadoopConfiguration)
    if (!fs.mkdirs(path)) {
      throw new SparkException("Failed to create checkpoint path " + path)
    }
    _cpDir
  }

  private val broadcastedConf = rdd.context.broadcast(
    new SerializableConfiguration(rdd.context.hadoopConfiguration))

  /**
   * Return the directory to which this RDD was checkpointed.
   * If the RDD is not checkpointed yet, return None.
   */
  def getCheckpointDir: Option[String] = RDDCheckpointData.synchronized {
    if (isCheckpointed) {
      Some(cpDir.toString)
    } else {
      None
    }
  }

  /**
   * Return an Iterator that will checkpoint the data when consuming the original Iterator if
   * this RDD has not yet been checkpointed. Otherwise, just return the original Iterator.
   *
   * Note: this is called in executor.
   */
  def getCheckpointIterator(
      rdd: RDD[T],
      values: Iterator[T],
      context: TaskContext,
      partitionIndex: Int): Iterator[T] = {
    if (cpState == Initialized) {
      CheckpointingIterator[T](
        rdd,
        values,
        cpDir,
        broadcastedConf,
        partitionIndex,
        context)
    } else {
      values
    }
  }

  /**
   * Materialize this RDD and write its content to a reliable DFS.
   * This is called immediately after the first action invoked on this RDD has completed.
   */
  protected override def doCheckpoint(): CheckpointRDD[T] = {

    // Create the output path for the checkpoint
    val path = new Path(cpDir)
    val fs = path.getFileSystem(rdd.context.hadoopConfiguration)
    if (!fs.mkdirs(path)) {
      throw new SparkException(s"Failed to create checkpoint path $cpDir")
    }

    val checkpointedPartitionFiles = fs.listStatus(path).map(_.getPath.getName).toSet
    // Not all actions compute all partitions of the RDD (e.g. take). For correctness, we
    // must checkpoint any missing partitions.
    val missingPartitionIndices = rdd.partitions.map(_.index).filter { i =>
      !checkpointedPartitionFiles(ReliableCheckpointRDD.checkpointFileName(i))
    }
    if (missingPartitionIndices.nonEmpty) {
      rdd.context.runJob(
        rdd,
        ReliableCheckpointRDD.writeCheckpointFile[T](cpDir, broadcastedConf) _,
        missingPartitionIndices)
    }

    val newRDD = new ReliableCheckpointRDD[T](rdd.context, cpDir)
    if (newRDD.partitions.length != rdd.partitions.length) {
      throw new SparkException(
        s"Checkpoint RDD $newRDD(${newRDD.partitions.length}) has different " +
          s"number of partitions from original RDD $rdd(${rdd.partitions.length})")
    }

    // Optionally clean our checkpoint files if the reference is out of scope
    if (rdd.conf.getBoolean("spark.cleaner.referenceTracking.cleanCheckpoints", false)) {
      rdd.context.cleaner.foreach { cleaner =>
        cleaner.registerRDDCheckpointDataForCleanup(newRDD, rdd.id)
      }
    }

    logInfo(s"Done checkpointing RDD ${rdd.id} to $cpDir, new parent is RDD ${newRDD.id}")

    newRDD
  }

}

private[spark] object ReliableRDDCheckpointData extends Logging {

  /** Return the path of the directory to which this RDD's checkpoint data is written. */
  def checkpointPath(sc: SparkContext, rddId: Int): Option[Path] = {
    sc.checkpointDir.map { dir => new Path(dir, s"rdd-$rddId") }
  }

  /** Clean up the files associated with the checkpoint data for this RDD. */
  def cleanCheckpoint(sc: SparkContext, rddId: Int): Unit = {
    checkpointPath(sc, rddId).foreach { path =>
      val fs = path.getFileSystem(sc.hadoopConfiguration)
      if (fs.exists(path)) {
        if (!fs.delete(path, true)) {
          logWarning(s"Error deleting ${path.toString()}")
        }
      }
    }
  }
}
