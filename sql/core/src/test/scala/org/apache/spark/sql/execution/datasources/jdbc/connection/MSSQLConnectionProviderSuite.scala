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

package org.apache.spark.sql.execution.datasources.jdbc.connection

class MSSQLConnectionProviderSuite extends ConnectionProviderSuiteBase {
  test("setAuthenticationConfigIfNeeded default parser must set authentication if not set") {
    val driver = registerDriver(MSSQLConnectionProvider.driverClass)
    val defaultProvider = new MSSQLConnectionProvider(
      driver, options("jdbc:sqlserver://localhost/mssql"))
    val customProvider = new MSSQLConnectionProvider(
      driver, options("jdbc:sqlserver://localhost/mssql;jaasConfigurationName=custommssql"))

    testProviders(defaultProvider, customProvider)
  }

  test("setAuthenticationConfigIfNeeded custom parser must set authentication if not set") {
    val parserMethod = "IntentionallyNotExistingMethod"
    val driver = registerDriver(MSSQLConnectionProvider.driverClass)
    val defaultProvider = new MSSQLConnectionProvider(
      driver, options("jdbc:sqlserver://localhost/mssql"), parserMethod)
    val customProvider = new MSSQLConnectionProvider(
      driver,
      options("jdbc:sqlserver://localhost/mssql;jaasConfigurationName=custommssql"),
      parserMethod)

    testProviders(defaultProvider, customProvider)
  }

  private def testProviders(
      defaultProvider: SecureConnectionProvider,
      customProvider: SecureConnectionProvider) = {
    assert(defaultProvider.appEntry !== customProvider.appEntry)
    testSecureConnectionProvider(defaultProvider)
    testSecureConnectionProvider(customProvider)
  }
}
