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

package org.apache.spark.launcher

import java.util.{ List => JList, Map => JMap }

/**
 * Exposes AbstractCommandBuilder to the Nomad tests, so that they can build classpaths the same
 * way other cluster managers do.
 */
private[spark] class TestClasspathBuilder extends AbstractCommandBuilder {

  childEnv.put(CommandBuilderUtils.ENV_SPARK_HOME, sys.props("spark.test.home"))

  override def buildClassPath(extraCp: String): JList[String] =
    super.buildClassPath(extraCp)

  override def buildCommand(env: JMap[String, String]): JList[String] =
    throw new UnsupportedOperationException()

}
