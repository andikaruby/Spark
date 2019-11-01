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

package org.apache.spark.resource

/**
 * An Executor resource request. This is used in conjunction with the ResourceProfile to
 * programmatically specify the resources needed for an RDD that will be applied at the
 * stage level.
 *
 * This is used to specify what the resource requirements are for an Executor and how
 * Spark can find out specific details about those resources. Not all the parameters are
 * required for every resource type. The resources names supported
 * correspond to the regular Spark configs with the prefix removed. For instance overhead
 * memory in this api is memoryOverhead, which is spark.executor.memoryOverhead with
 * spark.executor removed. Resources like GPUs are resource.gpu
 * (spark configs spark.executor.resource.gpu.*). The amount, discoveryScript, and vendor
 * parameters for resources are all the same parameters a user would specify through the
 * configs: spark.executor.resource.{resourceName}.{amount, discoveryScript, vendor}.
 *
 * For instance, a user wants to allocate an Executor with GPU resources on YARN. The user has
 * to specify the resource name (resource.gpu), the amount or number of GPUs per Executor,
 * units would not be used as its not a memory config, the discovery script would be specified
 * so that when the Executor starts up it can discovery what GPU addresses are available for it to
 * use because YARN doesn't tell Spark that, then vendor would not be used because
 * its specific for Kubernetes.
 *
 * See the configuration and cluster specific docs for more details.
 *
 * There are alternative constructors for working with Java.
 *
 * @param resourceName Name of the resource
 * @param amount Amount requesting
 * @param units Optional units of the amount. For things like Memory, default is no units, only byte
 *              types (b, mb, gb, etc) are currently supported.
 * @param discoveryScript Optional script used to discover the resources. This is required on some
 *                        cluster managers that don't tell Spark the addresses of the resources
 *                        allocated. The script runs on Executors startup to discover the addresses
 *                        of the resources available.
 * @param vendor Optional vendor, required for some cluster managers
 *
 * This api is currently private until the rest of the pieces are in place and then it
 * will become public.
 */
private[spark] class ExecutorResourceRequest(
    val resourceName: String,
    val amount: Int,
    val units: String = "",
    val discoveryScript: String = "",
    val vendor: String = "") extends Serializable {

  def this(resourceName: String, amount: Int) {
    this(resourceName, amount, "", "", "")
  }

  def this(resourceName: String, amount: Int, units: String) {
    this(resourceName, amount, units, "", "")
  }

  def this(resourceName: String, amount: Int, units: String, discoveryScript: String) {
    this(resourceName, amount, units, discoveryScript, "")
  }

  override def toString(): String = {
    s"ExecutorResourceRequest: resourceName = $resourceName, amount = $amount, " +
      s"discoveryScript = $discoveryScript, vendor = $vendor"
  }
}
