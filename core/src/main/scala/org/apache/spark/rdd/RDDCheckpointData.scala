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

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import org.apache.spark._
import org.apache.spark.scheduler.{ResultTask, ShuffleMapTask}
import org.apache.spark.util.{CheckpointingIterator, SerializableConfiguration}

/**
 * Enumeration to manage state transitions of an RDD through checkpointing
 * [ Initialized --> marked for checkpointing --> checkpointing in progress --> checkpointed ]
 */
private[spark] object CheckpointState extends Enumeration {
  type CheckpointState = Value
  val Initialized, MarkedForCheckpoint, CheckpointingInProgress, Checkpointed = Value
}

/**
 * This class contains all the information related to RDD checkpointing. Each instance of this
 * class is associated with a RDD. It manages process of checkpointing of the associated RDD,
 * as well as, manages the post-checkpoint state by providing the updated partitions,
 * iterator and preferred locations of the checkpointed RDD.
 */
private[spark] class RDDCheckpointData[T: ClassTag](@transient rdd: RDD[T])
  extends Logging with Serializable {

  import CheckpointState._

  // Because SparkContext is transient in RDD, so we can't get the id and checkpointDir later.
  // So keep a copy of the id and checkpointDir.
  // The id of RDD
  val rddId: Int = rdd.id

  // The path the checkpoint data will write to.
  val checkpointDir = rdd.context.checkpointDir
  @transient var checkpointPath: Path = null
  @transient var fs: FileSystem = null
  if (checkpointDir.isDefined) {
    checkpointPath = new Path(checkpointDir.get, "rdd-" + rddId)
    fs = checkpointPath.getFileSystem(rdd.context.hadoopConfiguration)
    if (!fs.mkdirs(checkpointPath)) {
      throw new SparkException("Failed to create checkpoint path " + checkpointPath)
    }
  }

  val broadcastedConf = rdd.context.broadcast(
    new SerializableConfiguration(rdd.context.hadoopConfiguration))

  // The checkpoint state of the associated RDD.
  var cpState = Initialized

  // The file to which the associated RDD has been checkpointed to
  @transient var cpFile: Option[String] = None

  // The CheckpointRDD created from the checkpoint file, that is, the new parent the associated RDD.
  var cpRDD: Option[RDD[T]] = None

  // Mark the RDD for checkpointing
  def markForCheckpoint() {
    RDDCheckpointData.synchronized {
      if (cpState == Initialized) cpState = MarkedForCheckpoint
    }
  }

  // Is the RDD already checkpointed
  def isCheckpointed: Boolean = {
    RDDCheckpointData.synchronized { cpState == Checkpointed }
  }

  // Get the file to which this RDD was checkpointed to as an Option
  def getCheckpointFile: Option[String] = {
    RDDCheckpointData.synchronized { cpFile }
  }

  // Get the iterator used to write checkpoint data to HDFS
  def getCheckpointIterator(
    rddIterator: Iterator[T],
    context: TaskContext,
    partitionId: Int): Iterator[T] = {
    RDDCheckpointData.synchronized {
      if (cpState == MarkedForCheckpoint) {
        // Create the output path for the checkpoint
        val path = new Path(checkpointDir.get, "rdd-" + rddId)
        CheckpointingIterator[T, Iterator[T]](
          rddIterator,
          path.toString,
          broadcastedConf,
          partitionId,
          context)
      } else {
        rddIterator
      }
    }
  }

  // Do the checkpointing of the RDD. Called after the first job using that RDD is over.
  def doCheckpoint() {
    // If it is marked for checkpointing AND checkpointing is not already in progress,
    // then set it to be in progress, else return
    RDDCheckpointData.synchronized {
      if (cpState == MarkedForCheckpoint) {
        cpState = CheckpointingInProgress
      } else {
        return
      }
    }

    val path = checkpointPath
    val newRDD = new CheckpointRDD[T](rdd.context, path.toString)
    if (rdd.conf.getBoolean("spark.cleaner.referenceTracking.cleanCheckpoints", false)) {
      rdd.context.cleaner.foreach { cleaner =>
        cleaner.registerRDDCheckpointDataForCleanup(newRDD, rddId)
      }
    }
    if (newRDD.partitions.length != rdd.partitions.length) {
      throw new SparkException(
        "Checkpoint RDD " + newRDD + "(" + newRDD.partitions.length + ") has different " +
          "number of partitions than original RDD " + rdd + "(" + rdd.partitions.length + ")")
    }

    // Change the dependencies and partitions of the RDD
    RDDCheckpointData.synchronized {
      cpFile = Some(path.toString)
      cpRDD = Some(newRDD)
      rdd.markCheckpointed(newRDD)   // Update the RDD's dependencies and partitions
      cpState = Checkpointed
    }
    logInfo("Done checkpointing RDD " + rddId + " to " + path + ", new parent is RDD " + newRDD.id)
  }

  // Get preferred location of a split after checkpointing
  def getPreferredLocations(split: Partition): Seq[String] = {
    RDDCheckpointData.synchronized {
      cpRDD.get.preferredLocations(split)
    }
  }

  def getPartitions: Array[Partition] = {
    RDDCheckpointData.synchronized {
      cpRDD.get.partitions
    }
  }

  def checkpointRDD: Option[RDD[T]] = {
    RDDCheckpointData.synchronized {
      cpRDD
    }
  }
}

private[spark] object RDDCheckpointData {
  def rddCheckpointDataPath(sc: SparkContext, rddId: Int): Option[Path] = {
    sc.checkpointDir.map { dir => new Path(dir, "rdd-" + rddId) }
  }

  def clearRDDCheckpointData(sc: SparkContext, rddId: Int): Unit = {
    rddCheckpointDataPath(sc, rddId).foreach { path =>
      val fs = path.getFileSystem(sc.hadoopConfiguration)
      if (fs.exists(path)) {
        fs.delete(path, true)
      }
    }
  }
}
