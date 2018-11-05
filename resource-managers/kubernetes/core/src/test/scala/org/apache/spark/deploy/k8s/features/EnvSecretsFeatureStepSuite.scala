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
package org.apache.spark.deploy.k8s.features

import io.fabric8.kubernetes.api.model.PodBuilder

import org.apache.spark.{SparkConf, SparkFunSuite}
import org.apache.spark.deploy.k8s._

class EnvSecretsFeatureStepSuite extends SparkFunSuite{
  private val KEY_REF_NAME_FOO = "foo"
  private val KEY_REF_NAME_BAR = "bar"
  private val KEY_REF_KEY_FOO = "key_foo"
  private val KEY_REF_KEY_BAR = "key_bar"
  private val ENV_NAME_FOO = "MY_FOO"
  private val ENV_NAME_BAR = "MY_bar"

  test("sets up all keyRefs") {
    val baseDriverPod = SparkPod.initialPod()
    val envVarsToKeys = Map(
      ENV_NAME_BAR -> s"${KEY_REF_NAME_BAR}:${KEY_REF_KEY_BAR}",
      ENV_NAME_FOO -> s"${KEY_REF_NAME_FOO}:${KEY_REF_KEY_FOO}")
    val sparkConf = new SparkConf(false)
    val kubernetesConf = KubernetesConf(
      sparkConf,
      KubernetesExecutorSpecificConf("1", Some(new PodBuilder().build())),
      "resource-name-prefix",
      "app-id",
      Map.empty,
      Map.empty,
      Map.empty,
      envVarsToKeys,
      Map.empty,
      Nil,
      hadoopConfSpec = None)

    val step = new EnvSecretsFeatureStep(kubernetesConf)
    val driverContainerWithEnvSecrets = step.configurePod(baseDriverPod).container

    val expectedVars =
      Seq(s"${ENV_NAME_BAR}", s"${ENV_NAME_FOO}")

    expectedVars.foreach { envName =>
      assert(KubernetesFeaturesTestUtils.containerHasEnvVar(driverContainerWithEnvSecrets, envName))
    }
  }
}
