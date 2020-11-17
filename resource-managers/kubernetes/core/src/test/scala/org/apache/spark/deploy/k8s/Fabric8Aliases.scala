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
package org.apache.spark.deploy.k8s

import io.fabric8.kubernetes.api.model.{ConfigMap, ConfigMapList, DoneableConfigMap, DoneablePod, HasMetadata, Pod, PodList}
import io.fabric8.kubernetes.client.{Watch, Watcher}
import io.fabric8.kubernetes.client.dsl.{FilterWatchListDeletable, MixedOperation, NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable, PodResource, Resource}

object Fabric8Aliases {
  type PODS = MixedOperation[Pod, PodList, DoneablePod, PodResource[Pod, DoneablePod]]
  type CONFIG_MAPS = MixedOperation[
    ConfigMap, ConfigMapList, DoneableConfigMap, Resource[ConfigMap, DoneableConfigMap]]
  type LABELED_PODS = FilterWatchListDeletable[
    Pod, PodList, java.lang.Boolean, Watch, Watcher[Pod]]
  type LABELED_CONFIG_MAPS = FilterWatchListDeletable[
    ConfigMap, ConfigMapList, java.lang.Boolean, Watch, Watcher[ConfigMap]]
  type SINGLE_POD = PodResource[Pod, DoneablePod]
  type RESOURCE_LIST = NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable[
    HasMetadata, Boolean]
}
