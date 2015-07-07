/**
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

import org.apache.spark.{Logging, SparkEnv, TaskContext}
import org.apache.spark.storage.{LocalCheckpointBlockId, StorageLevel}

/**
 * An implementation of checkpointing that writes the RDD data to a local file system.
 *
 * Local checkpointing trades off fault tolerance for performance by skipping the expensive
 * step of replicating the checkpointed data in a reliable storage. This is useful for use
 * cases where RDDs build up long lineages that need to be truncated often (e.g. GraphX).
 */
private[spark] class LocalRDDCheckpointData[T: ClassTag](@transient rdd: RDD[T])
  extends RDDCheckpointData[T](rdd) with Logging {

  protected override def doCheckpoint(): CheckpointRDD[T] = {

    // Put each partition into the disk store
    // TODO: if it's already in disk store, just use the existing values
    val rddId = rdd.id
    val persistPartition = (taskContext: TaskContext, values: Iterator[T]) => {
      SparkEnv.get.blockManager.putIterator(
        LocalCheckpointBlockId(rddId, taskContext.partitionId()),
        values,
        StorageLevel.DISK_ONLY)
    }
    rdd.context.runJob(rdd, persistPartition)

    // Return an RDD that reads these blocks back
    new LocalCheckpointRDD[T](rdd)
  }

}
