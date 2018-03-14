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
package org.apache.spark.deploy.k8s.submit

import java.io.StringWriter
import java.util.{Collections, UUID}
import java.util.Properties

import com.google.common.primitives.Longs
import io.fabric8.kubernetes.api.model._
import io.fabric8.kubernetes.client.KubernetesClient
import scala.collection.mutable
import scala.util.control.NonFatal

import org.apache.spark.SparkConf
import org.apache.spark.deploy.SparkApplication
import org.apache.spark.deploy.k8s.Config._
import org.apache.spark.deploy.k8s.Constants._
import org.apache.spark.deploy.k8s.SparkKubernetesClientFactory
import org.apache.spark.deploy.k8s.submit.steps.DriverConfigurationStep
import org.apache.spark.internal.Logging
import org.apache.spark.util.Utils

/**
 * Encapsulates arguments to the submission client.
 *
 * @param mainAppResource the main application resource if any
 * @param mainClass the main class of the application to run
 * @param driverArgs arguments to the driver
 */
private[spark] case class ClientArguments(
     mainAppResource: Option[MainAppResource],
     mainClass: String,
     driverArgs: Array[String])

private[spark] object ClientArguments {

  def fromCommandLineArgs(args: Array[String]): ClientArguments = {
    var mainAppResource: Option[MainAppResource] = None
    var mainClass: Option[String] = None
    val driverArgs = mutable.ArrayBuffer.empty[String]

    args.sliding(2, 2).toList.foreach {
      case Array("--primary-java-resource", primaryJavaResource: String) =>
        mainAppResource = Some(JavaMainAppResource(primaryJavaResource))
      case Array("--main-class", clazz: String) =>
        mainClass = Some(clazz)
      case Array("--arg", arg: String) =>
        driverArgs += arg
      case other =>
        val invalid = other.mkString(" ")
        throw new RuntimeException(s"Unknown arguments: $invalid")
    }

    require(mainClass.isDefined, "Main class must be specified via --main-class")

    ClientArguments(
      mainAppResource,
      mainClass.get,
      driverArgs.toArray)
  }
}

/**
 * Submits a Spark application to run on Kubernetes by creating the driver pod and starting a
 * watcher that monitors and logs the application status. Waits for the application to terminate if
 * spark.kubernetes.submission.waitAppCompletion is true.
 *
 * @param submissionSteps steps that collectively configure the driver
 * @param sparkConf the submission client Spark configuration
 * @param kubernetesClient the client to talk to the Kubernetes API server
 * @param waitForAppCompletion a flag indicating whether the client should wait for the application
 *                             to complete
 * @param appName the application name
 * @param watcher a watcher that monitors and logs the application status
 */
private[spark] class Client(
    submissionSteps: Seq[DriverConfigurationStep],
    sparkConf: SparkConf,
    kubernetesClient: KubernetesClient,
    waitForAppCompletion: Boolean,
    appName: String,
    watcher: LoggingPodStatusWatcher,
    kubernetesResourceNamePrefix: String) extends Logging {

   /**
    * Run command that initializes a DriverSpec that will be updated after each
    * DriverConfigurationStep in the sequence that is passed in. The final KubernetesDriverSpec
    * will be used to build the Driver Container, Driver Pod, and Kubernetes Resources
    */
  def run(): Unit = {
    var currentDriverSpec = KubernetesDriverSpec.initialSpec(sparkConf)
    // submissionSteps contain steps necessary to take, to resolve varying
    // client arguments that are passed in, created by orchestrator
    for (nextStep <- submissionSteps) {
      currentDriverSpec = nextStep.configureDriver(currentDriverSpec)
    }
    val configMapName = s"$kubernetesResourceNamePrefix-driver-conf-map"
    val driverExtraJavaOpts =
      sparkConf.get(org.apache.spark.internal.config.DRIVER_JAVA_OPTIONS).getOrElse("")
    val configMap = buildConfigMap(
      configMapName,
      currentDriverSpec.driverSparkConf,
      driverExtraJavaOpts)
    // The include of the ENV_VAR for "SPARK_CONF_DIR" is to allow for the
    // Spark command builder to pickup on the Java Options present in the ConfigMap
    val resolvedDriverContainer = new ContainerBuilder(currentDriverSpec.driverContainer)
      .addNewEnv()
        .withName(SPARK_CONF_DIR_ENV)
        .withValue(SPARK_CONF_PATH)
        .endEnv()
      .addNewVolumeMount()
        .withName(SPARK_CONF_VOLUME)
        .withMountPath(SPARK_CONF_DIR)
        .endVolumeMount()
      .build()
    val resolvedDriverPod = new PodBuilder(currentDriverSpec.driverPod)
      .editSpec()
        .addToContainers(resolvedDriverContainer)
        .addNewVolume()
          .withName(SPARK_CONF_VOLUME)
          .withNewConfigMap()
            .withName(configMapName)
            .endConfigMap()
          .endVolume()
        .endSpec()
      .build()

    Utils.tryWithResource(
      kubernetesClient
        .pods()
        .withName(resolvedDriverPod.getMetadata.getName)
        .watch(watcher)) { _ =>
      val createdDriverPod = kubernetesClient.pods().create(resolvedDriverPod)
      try {
        if (currentDriverSpec.otherKubernetesResources.nonEmpty) {
          val otherKubernetesResources =
            currentDriverSpec.otherKubernetesResources ++ Seq(configMap)
          addDriverOwnerReference(createdDriverPod, otherKubernetesResources)
          kubernetesClient.resourceList(otherKubernetesResources: _*).createOrReplace()
        }
      } catch {
        case NonFatal(e) =>
          kubernetesClient.pods().delete(createdDriverPod)
          throw e
      }

      if (waitForAppCompletion) {
        logInfo(s"Waiting for application $appName to finish...")
        watcher.awaitCompletion()
        logInfo(s"Application $appName finished.")
      } else {
        logInfo(s"Deployed Spark application $appName into Kubernetes.")
      }
    }
  }

  // Add a OwnerReference to the given resources making the driver pod an owner of them so when
  // the driver pod is deleted, the resources are garbage collected.
  private def addDriverOwnerReference(driverPod: Pod, resources: Seq[HasMetadata]): Unit = {
    val driverPodOwnerReference = new OwnerReferenceBuilder()
      .withName(driverPod.getMetadata.getName)
      .withApiVersion(driverPod.getApiVersion)
      .withUid(driverPod.getMetadata.getUid)
      .withKind(driverPod.getKind)
      .withController(true)
      .build()
    resources.foreach { resource =>
      val originalMetadata = resource.getMetadata
      originalMetadata.setOwnerReferences(Collections.singletonList(driverPodOwnerReference))
    }
  }

  // Build a Config Map that will house both the properties and the java options in a single file
  private def buildConfigMap(
    configMapName: String,
    conf: SparkConf,
    driverJavaOps: String): ConfigMap = {
    val properties = new Properties()
    conf
      .remove(org.apache.spark.internal.config.DRIVER_JAVA_OPTIONS)
      .getAll.foreach { case (k, v) =>
      properties.setProperty(k, v)
    }
    val propertiesWriter = new StringWriter()
    properties.store(propertiesWriter,
      s"Java properties built from Kubernetes config map with name: $configMapName")

    val namespace = conf.get(KUBERNETES_NAMESPACE)
    new ConfigMapBuilder()
      .withNewMetadata()
        .withName(configMapName)
        .withNamespace(namespace)
        .endMetadata()
      .addToData(SPARK_CONF_FILE_NAME, propertiesWriter.toString + driverJavaOps)
      .build()
  }
}

/**
 * Main class and entry point of application submission in KUBERNETES mode.
 */
private[spark] class KubernetesClientApplication extends SparkApplication {

  override def start(args: Array[String], conf: SparkConf): Unit = {
    val parsedArguments = ClientArguments.fromCommandLineArgs(args)
    run(parsedArguments, conf)
  }

  private def run(clientArguments: ClientArguments, sparkConf: SparkConf): Unit = {
    val namespace = sparkConf.get(KUBERNETES_NAMESPACE)
    // For constructing the app ID, we can't use the Spark application name, as the app ID is going
    // to be added as a label to group resources belonging to the same application. Label values are
    // considerably restrictive, e.g. must be no longer than 63 characters in length. So we generate
    // a unique app ID (captured by spark.app.id) in the format below.
    val kubernetesAppId = s"spark-${UUID.randomUUID().toString.replaceAll("-", "")}"
    val launchTime = System.currentTimeMillis()
    val waitForAppCompletion = sparkConf.get(WAIT_FOR_APP_COMPLETION)
    val appName = sparkConf.getOption("spark.app.name").getOrElse("spark")
    val kubernetesResourceNamePrefix = {
      s"$appName-$launchTime".toLowerCase.replaceAll("\\.", "-")
    }
    // The master URL has been checked for validity already in SparkSubmit.
    // We just need to get rid of the "k8s://" prefix here.
    val master = sparkConf.get("spark.master").substring("k8s://".length)
    val loggingInterval = if (waitForAppCompletion) Some(sparkConf.get(REPORT_INTERVAL)) else None

    val watcher = new LoggingPodStatusWatcherImpl(kubernetesAppId, loggingInterval)

    val orchestrator = new DriverConfigOrchestrator(
      kubernetesAppId,
      kubernetesResourceNamePrefix,
      clientArguments.mainAppResource,
      appName,
      clientArguments.mainClass,
      clientArguments.driverArgs,
      sparkConf)

    Utils.tryWithResource(SparkKubernetesClientFactory.createKubernetesClient(
      master,
      Some(namespace),
      KUBERNETES_AUTH_SUBMISSION_CONF_PREFIX,
      sparkConf,
      None,
      None)) { kubernetesClient =>
        val client = new Client(
          orchestrator.getAllConfigurationSteps,
          sparkConf,
          kubernetesClient,
          waitForAppCompletion,
          appName,
          watcher,
          kubernetesResourceNamePrefix)
        client.run()
    }
  }
}
