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

package org.apache.spark.sql.execution.command.v1

import org.apache.spark.sql.execution.command

/**
 * This base suite contains unified tests for the `SHOW VARIABLES` command that checks V1
 * table catalogs. The tests that cannot run for all V1 catalogs are located in more
 * specific test suites:
 *
 *   - V1 In-Memory catalog: `org.apache.spark.sql.execution.command.v1.ShowVariablesSuite`
 *   - V1 Hive External catalog:
 *     `org.apache.spark.sql.hive.execution.command.ShowVariablesSuite`
 */
trait ShowVariablesSuiteBase extends command.ShowVariablesSuiteBase
  with command.TestsV1AndV2Commands

/**
 * The class contains tests for the `SHOW VARIABLES` command to check
 * V1 In-Memory table catalog.
 */
class ShowVariablesSuite extends ShowVariablesSuiteBase with CommandSuiteBase {
  override def commandVersion: String = super[ShowVariablesSuiteBase].commandVersion
}
