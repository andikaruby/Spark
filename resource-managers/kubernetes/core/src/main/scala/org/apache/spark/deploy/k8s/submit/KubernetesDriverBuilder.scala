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

import java.io.File

import io.fabric8.kubernetes.client.KubernetesClient

import org.apache.spark.{SparkConf, SparkException}
import org.apache.spark.deploy.k8s._
import org.apache.spark.deploy.k8s.features._
import org.apache.spark.deploy.k8s.features.bindings.{JavaDriverFeatureStep, PythonDriverFeatureStep, RDriverFeatureStep}
import org.apache.spark.internal.Logging

private[spark] class KubernetesDriverBuilder (
    provideBasicStep: (KubernetesConf[KubernetesDriverSpecificConf]) => BasicDriverFeatureStep =
      new BasicDriverFeatureStep(_),
    provideCredentialsStep: (KubernetesConf[KubernetesDriverSpecificConf])
      => DriverKubernetesCredentialsFeatureStep =
      new DriverKubernetesCredentialsFeatureStep(_),
    provideServiceStep: (KubernetesConf[KubernetesDriverSpecificConf]) => DriverServiceFeatureStep =
      new DriverServiceFeatureStep(_),
    provideSecretsStep: (KubernetesConf[_ <: KubernetesRoleSpecificConf]
      => MountSecretsFeatureStep) =
      new MountSecretsFeatureStep(_),
    provideEnvSecretsStep: (KubernetesConf[_ <: KubernetesRoleSpecificConf]
      => EnvSecretsFeatureStep) =
      new EnvSecretsFeatureStep(_),
    provideLocalDirsStep: (KubernetesConf[_ <: KubernetesRoleSpecificConf])
      => LocalDirsFeatureStep =
      new LocalDirsFeatureStep(_),
    provideVolumesStep: (KubernetesConf[_ <: KubernetesRoleSpecificConf]
      => MountVolumesFeatureStep) =
      new MountVolumesFeatureStep(_),
    providePythonStep: (
      KubernetesConf[KubernetesDriverSpecificConf]
      => PythonDriverFeatureStep) =
      new PythonDriverFeatureStep(_),
    provideRStep: (
      KubernetesConf[KubernetesDriverSpecificConf]
        => RDriverFeatureStep) =
    new RDriverFeatureStep(_),
    provideJavaStep: (
      KubernetesConf[KubernetesDriverSpecificConf]
        => JavaDriverFeatureStep) =
    new JavaDriverFeatureStep(_),
    provideTemplateVolumeStep: (KubernetesConf[_ <: KubernetesRoleSpecificConf]
      => TemplateVolumeStep) =
    new TemplateVolumeStep(_),
    provideInitialSpec: KubernetesConf[KubernetesDriverSpecificConf]
      => KubernetesDriverSpec =
      KubernetesDriverSpec.initialSpec) {

  def buildFromFeatures(
    kubernetesConf: KubernetesConf[KubernetesDriverSpecificConf]): KubernetesDriverSpec = {
    val baseFeatures = Seq(
      provideBasicStep(kubernetesConf),
      provideCredentialsStep(kubernetesConf),
      provideServiceStep(kubernetesConf),
      provideLocalDirsStep(kubernetesConf))

    val secretFeature = if (kubernetesConf.roleSecretNamesToMountPaths.nonEmpty) {
      Seq(provideSecretsStep(kubernetesConf))
    } else Nil
    val envSecretFeature = if (kubernetesConf.roleSecretEnvNamesToKeyRefs.nonEmpty) {
      Seq(provideEnvSecretsStep(kubernetesConf))
    } else Nil
    val volumesFeature = if (kubernetesConf.roleVolumes.nonEmpty) {
      Seq(provideVolumesStep(kubernetesConf))
    } else Nil
    val templateVolumeFeature = if (
      kubernetesConf.get(Config.KUBERNETES_EXECUTOR_PODTEMPLATE_FILE).isDefined) {
      Seq(provideTemplateVolumeStep(kubernetesConf))
    } else Nil

    val bindingsStep = kubernetesConf.roleSpecificConf.mainAppResource.map {
        case JavaMainAppResource(_) =>
          provideJavaStep(kubernetesConf)
        case PythonMainAppResource(_) =>
          providePythonStep(kubernetesConf)
        case RMainAppResource(_) =>
          provideRStep(kubernetesConf)}
      .getOrElse(provideJavaStep(kubernetesConf))

    val allFeatures = (baseFeatures :+ bindingsStep) ++
      secretFeature ++ envSecretFeature ++ volumesFeature ++ templateVolumeFeature

    var spec = provideInitialSpec(kubernetesConf)
    for (feature <- allFeatures) {
      val configuredPod = feature.configurePod(spec.pod)
      val addedSystemProperties = feature.getAdditionalPodSystemProperties()
      val addedResources = feature.getAdditionalKubernetesResources()
      spec = KubernetesDriverSpec(
        configuredPod,
        spec.driverKubernetesResources ++ addedResources,
        spec.systemProperties ++ addedSystemProperties)
    }
    spec
  }
}

private[spark] object KubernetesDriverBuilder extends Logging {
  def apply(kubernetesClient: KubernetesClient, conf: SparkConf): KubernetesDriverBuilder = {
    conf.get(Config.KUBERNETES_DRIVER_PODTEMPLATE_FILE)
      .map(new File(_))
      .map(file => new KubernetesDriverBuilder(provideInitialSpec = conf => {
        try {
          val sparkPod = KubernetesUtils.loadPodFromTemplate(
            kubernetesClient,
            file,
            Constants.DRIVER_CONTAINER_NAME)
          KubernetesDriverSpec.initialSpec(conf).copy(pod = sparkPod)
        } catch {
          case e: Exception =>
            logError(
              s"Encountered exception while attempting to load initial pod spec from file", e)
            throw new SparkException("Could not load driver pod from template file.", e)
        }
      }))
      .getOrElse(new KubernetesDriverBuilder())
  }
}
