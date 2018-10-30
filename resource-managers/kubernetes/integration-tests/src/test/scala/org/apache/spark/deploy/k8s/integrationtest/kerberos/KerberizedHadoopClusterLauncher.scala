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
package org.apache.spark.deploy.k8s.integrationtest.kerberos

import io.fabric8.kubernetes.client.KubernetesClient

import org.apache.spark.internal.Logging

/**
 * This class is responsible for launching a psuedo-distributed, single noded,
 * kerberized, Hadoop cluster to test secure HDFS interaction. Because each node:
 * kdc, data node, and name node rely on Persistent Volumes and Config Maps to be set,
 * and a particular order in pod-launching, this class leverages Watchers and thread locks
 * to ensure that order is always preserved and the cluster is the same for every run.
 */
private[spark] class KerberizedHadoopClusterLauncher(
    labels: Map[String, String],
    kubernetesClient: KubernetesClient,
    namespace: String)
  extends Logging {

  def launchKerberizedCluster(kerberosUtils: KerberosUtils): Unit = {
    // These Utils allow for each step in this launch process to re-use
    // common functionality for setting up hadoop nodes.
    // Launches persistent volumes and its claims for sharing keytabs across pods
    val pvWatcherCache = new KerberosPVWatcherCache(kerberosUtils, labels)
    pvWatcherCache.deploy(kerberosUtils.getNNStorage)
    pvWatcherCache.deploy(kerberosUtils.getKTStorage)
    pvWatcherCache.stopWatch()

    // Launches config map for the files in HADOOP_CONF_DIR
    val cmWatcherCache = new KerberosCMWatcherCache(kerberosUtils)
    cmWatcherCache.deploy(kerberosUtils.getConfigMap)
    cmWatcherCache.stopWatch()

    // Launches the Hadoop cluster pods: KDC --> NN --> DN1
    val podWatcherCache = new KerberosPodWatcherCache(kerberosUtils, labels)
    podWatcherCache.deploy(kerberosUtils.getKDC)
    podWatcherCache.deploy(kerberosUtils.getNN)
    podWatcherCache.deploy(kerberosUtils.getDN)
    podWatcherCache.stopWatch()

    // Launch the Data populator pod to populate HDFS
    val jobWatcherCache = new KerberosJobWatcherCache(kerberosUtils, labels)
    jobWatcherCache.deploy(kerberosUtils.getDP)
    jobWatcherCache.stopWatch()
   }
}
