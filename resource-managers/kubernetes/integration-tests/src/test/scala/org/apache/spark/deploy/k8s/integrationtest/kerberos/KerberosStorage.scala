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

import io.fabric8.kubernetes.api.model._
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.batch.Job

private[spark] sealed trait KerberosStorage

private[spark] case class PVStorage(
    name: String,
    persistentVolumeClaim: PersistentVolumeClaim,
    persistentVolume: PersistentVolume)
  extends KerberosStorage

private[spark] case class ServiceStorage(
    name: String,
    podSet: StatefulSet,
    service: Service)
  extends KerberosStorage

private[spark] case class JobStorage(
    resource: Job)
  extends KerberosStorage

private[spark] case class ConfigMapStorage(
    resource: ConfigMap)
  extends KerberosStorage
