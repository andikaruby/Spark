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

package org.apache.spark.deploy

import java.io.File
import java.net.URL
import java.net.URLClassLoader

import org.apache.spark.executor.ExecutorURLClassLoader

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map

object SparkSubmit {
  val YARN = 1
  val STANDALONE = 2
  val MESOS = 4
  val LOCAL = 8
  val ALL_CLUSTER_MGRS = YARN | STANDALONE | MESOS | LOCAL

  var clusterManager: Int = LOCAL

  def main(args: Array[String]) {
    val appArgs = new SparkSubmitArguments(args)
    val (childArgs, classpath, sysProps, mainClass) = createLaunchEnv(appArgs)
    launch(childArgs, classpath, sysProps, mainClass)
  }

  /**
   * @return
   *         a tuple containing the arguments for the child, a list of classpath
   *         entries for the child, and the main class for the child
   */
  def createLaunchEnv(appArgs: SparkSubmitArguments): (ArrayBuffer[String],
      ArrayBuffer[String], Map[String, String], String) = {
    if (appArgs.master.startsWith("yarn")) {
      clusterManager = YARN
    } else if (appArgs.master.startsWith("spark")) {
      clusterManager = STANDALONE
    } else if (appArgs.master.startsWith("mesos")) {
      clusterManager = MESOS
    } else if (appArgs.master.startsWith("local")) {
      clusterManager = LOCAL
    } else {
      System.err.println("master must start with yarn, mesos, spark, or local")
      System.exit(1)
    }

    // Because "yarn-standalone" and "yarn-client" encapsulate both the master
    // and deploy mode, we have some logic to infer the master and deploy mode
    // from each other if only one is specified, or exit early if they are at odds.
    if (appArgs.deployMode == null && appArgs.master == "yarn-standalone") {
      appArgs.deployMode = "cluster"
    }
    if (appArgs.deployMode == "cluster" && appArgs.master == "yarn-client") {
      System.err.println("Deploy mode \"cluster\" and master \"yarn-client\" are at odds")
      System.exit(1)
    }
    if (appArgs.deployMode == "client" && appArgs.master == "yarn-standalone") {
      System.err.println("Deploy mode \"client\" and master \"yarn-standalone\" are at odds")
      System.exit(1)
    }
    if (appArgs.deployMode == "cluster" && appArgs.master.startsWith("yarn")) {
      appArgs.master = "yarn-standalone"
    }
    if (appArgs.deployMode != "cluster" && appArgs.master.startsWith("yarn")) {
      appArgs.master = "yarn-client"
    }

    val deployOnCluster = Option(appArgs.deployMode).getOrElse("client") == "cluster"

    val childClasspath = new ArrayBuffer[String]()
    val childArgs = new ArrayBuffer[String]()
    val sysProps = new HashMap[String, String]()
    var childMainClass = ""

    if (clusterManager == MESOS && deployOnCluster) {
      System.err.println("Mesos does not support running the driver on the cluster")
      System.exit(1)
    }

    if (!deployOnCluster) {
      childMainClass = appArgs.mainClass
      childClasspath += appArgs.primaryResource
    } else if (clusterManager == YARN) {
      childMainClass = "org.apache.spark.deploy.yarn.Client"
      childArgs += ("--jar", appArgs.primaryResource)
      childArgs += ("--class", appArgs.mainClass)
    }

    val options = List[OptionAssigner](
      new OptionAssigner(appArgs.master, ALL_CLUSTER_MGRS, false, sysProp = "spark.master"),
      new OptionAssigner(appArgs.driverMemory, YARN, true, clOption = "--master-memory"),
      new OptionAssigner(appArgs.name, YARN, true, clOption = "--name"),
      new OptionAssigner(appArgs.queue, YARN, true, clOption = "--queue"),
      new OptionAssigner(appArgs.queue, YARN, false, sysProp = "spark.yarn.queue"),
      new OptionAssigner(appArgs.numExecutors, YARN, true, clOption = "--num-workers"),
      new OptionAssigner(appArgs.numExecutors, YARN, false, sysProp = "spark.worker.instances"),
      new OptionAssigner(appArgs.executorMemory, YARN, true, clOption = "--worker-memory"),
      new OptionAssigner(appArgs.executorMemory, STANDALONE | MESOS | YARN, false,
        sysProp = "spark.executor.memory"),
      new OptionAssigner(appArgs.driverMemory, STANDALONE, true, clOption = "--memory"),
      new OptionAssigner(appArgs.executorCores, YARN, true, clOption = "--worker-cores"),
      new OptionAssigner(appArgs.executorCores, YARN, false, sysProp = "spark.executor.cores"),
      new OptionAssigner(appArgs.driverCores, STANDALONE, true, clOption = "--cores"),
      new OptionAssigner(appArgs.totalExecutorCores, STANDALONE | MESOS, true,
        sysProp = "spark.cores.max"),
      new OptionAssigner(appArgs.files, YARN, false, sysProp = "spark.yarn.dist.files"),
      new OptionAssigner(appArgs.files, YARN, true, clOption = "--files"),
      new OptionAssigner(appArgs.archives, YARN, false, sysProp = "spark.yarn.dist.archives"),
      new OptionAssigner(appArgs.archives, YARN, true, clOption = "--archives"),
      new OptionAssigner(appArgs.jars, YARN, true, clOption = "--addJars")
    )

    // more jars
    if (appArgs.jars != null && !deployOnCluster) {
      for (jar <- appArgs.jars.split(",")) {
        childClasspath += jar
      }
    }

    for (opt <- options) {
      if (opt.value != null && deployOnCluster == opt.deployOnCluster &&
        (clusterManager & opt.clusterManager) != 0) {
        if (opt.clOption != null) {
          childArgs += (opt.clOption, opt.value)
        } else if (opt.sysProp != null) {
          sysProps.put(opt.sysProp, opt.value)
        }
      }
    }

    if (deployOnCluster && clusterManager == STANDALONE) {
      if (appArgs.supervise) {
        childArgs += "--supervise"
      }

      childMainClass = "org.apache.spark.deploy.Client"
      childArgs += "launch"
      childArgs += (appArgs.master, appArgs.primaryResource, appArgs.mainClass)
    }

    // args
    if (appArgs.childArgs != null) {
      if (!deployOnCluster || clusterManager == STANDALONE) {
        childArgs ++= appArgs.childArgs
      } else if (clusterManager == YARN) {
        for (arg <- appArgs.childArgs) {
          childArgs += ("--args", arg)
        }
      }
    }

    (childArgs, childClasspath, sysProps, childMainClass)
  }

  def launch(childArgs: ArrayBuffer[String], childClasspath: ArrayBuffer[String],
      sysProps: Map[String, String], childMainClass: String) {
    val loader = new ExecutorURLClassLoader(new Array[URL](0),
      Thread.currentThread.getContextClassLoader)
    Thread.currentThread.setContextClassLoader(loader)

    for (jar <- childClasspath) {
      addJarToClasspath(jar, loader)
    }

    for ((key, value) <- sysProps) {
      System.setProperty(key, value)
    }

    val mainClass = Class.forName(childMainClass, true, loader)
    val mainMethod = mainClass.getMethod("main", new Array[String](0).getClass)
    mainMethod.invoke(null, childArgs.toArray)
  }

  def addJarToClasspath(localJar: String, loader: ExecutorURLClassLoader) {
    val localJarFile = new File(localJar)
    if (!localJarFile.exists()) {
      System.err.println("Jar does not exist: " + localJar + ". Skipping.")
    }

    val url = localJarFile.getAbsoluteFile.toURI.toURL
    loader.addURL(url)
  }
}

private[spark] class OptionAssigner(val value: String,
  val clusterManager: Int,
  val deployOnCluster: Boolean,
  val clOption: String = null,
  val sysProp: String = null
) { }
