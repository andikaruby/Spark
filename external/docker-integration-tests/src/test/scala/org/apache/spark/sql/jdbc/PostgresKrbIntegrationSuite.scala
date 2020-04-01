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

package org.apache.spark.sql.jdbc

import javax.security.auth.login.Configuration

import com.spotify.docker.client.messages.{ContainerConfig, HostConfig}

import org.apache.spark.sql.execution.datasources.jdbc.connection.SecureConnectionProvider
import org.apache.spark.tags.DockerTest

@DockerTest
class PostgresKrbIntegrationSuite extends DockerKrbJDBCIntegrationSuite {
  override protected val userName = s"postgres/$dockerIp"
  override protected val keytabFileName = "postgres.keytab"

  override val db = new DatabaseOnDocker {
    override val imageName = "postgres:12.0"
    override val env = Map(
      "POSTGRES_PASSWORD" -> "rootpass"
    )
    override val usesIpc = false
    override val jdbcPort = 5432

    override def getJdbcUrl(ip: String, port: Int): String =
      s"jdbc:postgresql://$ip:$port/postgres?user=$principal&gsslib=gssapi"

    override def getEntryPoint: Option[String] = None

    override def getStartupProcessName: Option[String] = None

    override def beforeContainerStart(
        hostConfigBuilder: HostConfig.Builder,
        containerConfigBuilder: ContainerConfig.Builder): Unit = {
      def replaceIp(s: String): String = s.replace("__IP_ADDRESS_REPLACE_ME__", dockerIp)
      copyExecutableResource("postgres_krb_setup.sh", initDbDir, replaceIp)

      hostConfigBuilder.appendBinds(
        HostConfig.Bind.from(initDbDir.getAbsolutePath)
          .to("/docker-entrypoint-initdb.d").readOnly(true).build()
      )
    }
  }

  override protected def setAuthentication(keytabFile: String, principal: String): Unit = {
    val config = new SecureConnectionProvider.JDBCConfiguration(
      Configuration.getConfiguration, "pgjdbc", keytabFile, principal)
    Configuration.setConfiguration(config)
  }
}
