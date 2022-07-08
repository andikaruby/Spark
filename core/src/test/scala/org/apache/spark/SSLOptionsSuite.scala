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

package org.apache.spark

import java.io.File
import java.util.UUID
import javax.net.ssl.SSLContext

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.alias.{CredentialProvider, CredentialProviderFactory}

import org.apache.spark.util.SparkConfWithEnv

class SSLOptionsSuite extends SparkFunSuite {

  test("test resolving property file as spark conf ") {
    val keyStorePath = new File(this.getClass.getResource("/keystore").toURI).getAbsolutePath
    val trustStorePath = new File(this.getClass.getResource("/truststore").toURI).getAbsolutePath

    // Pick two cipher suites that the provider knows about
    val sslContext = SSLContext.getInstance("TLSv1.2")
    sslContext.init(null, null, null)
    val algorithms = sslContext
      .getServerSocketFactory
      .getDefaultCipherSuites
      .take(2)
      .toSet

    val conf = new SparkConf
    val hadoopConf = new Configuration()
    conf.set("spark.ssl.enabled", "true")
    conf.set("spark.ssl.keyStore", keyStorePath)
    conf.set("spark.ssl.keyStorePassword", "password")
    conf.set("spark.ssl.keyPassword", "password")
    conf.set("spark.ssl.trustStore", trustStorePath)
    conf.set("spark.ssl.trustStorePassword", "password")
    conf.set("spark.ssl.enabledAlgorithms", algorithms.mkString(","))
    conf.set("spark.ssl.protocol", "TLSv1.2")

    val opts = SSLOptions.parse(conf, hadoopConf, "spark.ssl")

    assert(opts.enabled)
    assert(opts.trustStore.isDefined)
    assert(opts.trustStore.get.getName === "truststore")
    assert(opts.trustStore.get.getAbsolutePath === trustStorePath)
    assert(opts.keyStore.isDefined)
    assert(opts.keyStore.get.getName === "keystore")
    assert(opts.keyStore.get.getAbsolutePath === keyStorePath)
    assert(opts.trustStorePassword === Some("password"))
    assert(opts.keyStorePassword === Some("password"))
    assert(opts.keyPassword === Some("password"))
    assert(opts.protocol === Some("TLSv1.2"))
    assert(opts.enabledAlgorithms === algorithms)
  }

  test("test resolving property with defaults specified ") {
    val keyStorePath = new File(this.getClass.getResource("/keystore").toURI).getAbsolutePath
    val trustStorePath = new File(this.getClass.getResource("/truststore").toURI).getAbsolutePath

    val conf = new SparkConf
    val hadoopConf = new Configuration()
    conf.set("spark.ssl.enabled", "true")
    conf.set("spark.ssl.keyStore", keyStorePath)
    conf.set("spark.ssl.keyStorePassword", "password")
    conf.set("spark.ssl.keyPassword", "password")
    conf.set("spark.ssl.trustStore", trustStorePath)
    conf.set("spark.ssl.trustStorePassword", "password")
    conf.set("spark.ssl.enabledAlgorithms",
      "TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA")
    conf.set("spark.ssl.protocol", "SSLv3")

    val defaultOpts = SSLOptions.parse(conf, hadoopConf, "spark.ssl", defaults = None)
    val opts = SSLOptions.parse(conf, hadoopConf, "spark.ssl.ui", defaults = Some(defaultOpts))

    assert(opts.enabled)
    assert(opts.trustStore.isDefined)
    assert(opts.trustStore.get.getName === "truststore")
    assert(opts.trustStore.get.getAbsolutePath === trustStorePath)
    assert(opts.keyStore.isDefined)
    assert(opts.keyStore.get.getName === "keystore")
    assert(opts.keyStore.get.getAbsolutePath === keyStorePath)
    assert(opts.trustStorePassword === Some("password"))
    assert(opts.keyStorePassword === Some("password"))
    assert(opts.keyPassword === Some("password"))
    assert(opts.protocol === Some("SSLv3"))
    assert(opts.enabledAlgorithms ===
      Set("TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"))
  }

  test("test whether defaults can be overridden ") {
    val keyStorePath = new File(this.getClass.getResource("/keystore").toURI).getAbsolutePath
    val trustStorePath = new File(this.getClass.getResource("/truststore").toURI).getAbsolutePath

    val conf = new SparkConf
    val hadoopConf = new Configuration()
    conf.set("spark.ssl.enabled", "true")
    conf.set("spark.ssl.ui.enabled", "false")
    conf.set("spark.ssl.ui.port", "4242")
    conf.set("spark.ssl.keyStore", keyStorePath)
    conf.set("spark.ssl.keyStorePassword", "password")
    conf.set("spark.ssl.ui.keyStorePassword", "12345")
    conf.set("spark.ssl.keyPassword", "password")
    conf.set("spark.ssl.trustStore", trustStorePath)
    conf.set("spark.ssl.trustStorePassword", "password")
    conf.set("spark.ssl.enabledAlgorithms",
      "TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA")
    conf.set("spark.ssl.ui.enabledAlgorithms", "ABC, DEF")
    conf.set("spark.ssl.protocol", "SSLv3")

    val defaultOpts = SSLOptions.parse(conf, hadoopConf, "spark.ssl", defaults = None)
    val opts = SSLOptions.parse(conf, hadoopConf, "spark.ssl.ui", defaults = Some(defaultOpts))

    assert(opts.enabled === false)
    assert(opts.port === Some(4242))
    assert(opts.trustStore.isDefined)
    assert(opts.trustStore.get.getName === "truststore")
    assert(opts.trustStore.get.getAbsolutePath === trustStorePath)
    assert(opts.keyStore.isDefined)
    assert(opts.keyStore.get.getName === "keystore")
    assert(opts.keyStore.get.getAbsolutePath === keyStorePath)
    assert(opts.trustStorePassword === Some("password"))
    assert(opts.keyStorePassword === Some("12345"))
    assert(opts.keyPassword === Some("password"))
    assert(opts.protocol === Some("SSLv3"))
    assert(opts.enabledAlgorithms === Set("ABC", "DEF"))
  }

  test("variable substitution") {
    val conf = new SparkConfWithEnv(Map(
      "ENV1" -> "val1",
      "ENV2" -> "val2"))
    val hadoopConf = new Configuration()

    conf.set("spark.ssl.enabled", "true")
    conf.set("spark.ssl.keyStore", "${env:ENV1}")
    conf.set("spark.ssl.trustStore", "${env:ENV2}")

    val opts = SSLOptions.parse(conf, hadoopConf, "spark.ssl", defaults = None)
    assert(opts.keyStore === Some(new File("val1")))
    assert(opts.trustStore === Some(new File("val2")))
  }

  test("get password from Hadoop credential provider") {
    val keyStorePath = new File(this.getClass.getResource("/keystore").toURI).getAbsolutePath
    val trustStorePath = new File(this.getClass.getResource("/truststore").toURI).getAbsolutePath

    val conf = new SparkConf
    val hadoopConf = new Configuration()
    val tmpPath = s"localjceks://file${sys.props("java.io.tmpdir")}/test-" +
      s"${UUID.randomUUID().toString}.jceks"
    val provider = createCredentialProvider(tmpPath, hadoopConf)

    conf.set("spark.ssl.enabled", "true")
    conf.set("spark.ssl.keyStore", keyStorePath)
    storePassword(provider, "spark.ssl.keyStorePassword", "password")
    storePassword(provider, "spark.ssl.keyPassword", "password")
    conf.set("spark.ssl.trustStore", trustStorePath)
    storePassword(provider, "spark.ssl.trustStorePassword", "password")
    conf.set("spark.ssl.enabledAlgorithms",
      "TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA")
    conf.set("spark.ssl.protocol", "SSLv3")

    val defaultOpts = SSLOptions.parse(conf, hadoopConf, "spark.ssl", defaults = None)
    val opts = SSLOptions.parse(conf, hadoopConf, "spark.ssl.ui", defaults = Some(defaultOpts))

    assert(opts.enabled)
    assert(opts.trustStore.isDefined)
    assert(opts.trustStore.get.getName === "truststore")
    assert(opts.trustStore.get.getAbsolutePath === trustStorePath)
    assert(opts.keyStore.isDefined)
    assert(opts.keyStore.get.getName === "keystore")
    assert(opts.keyStore.get.getAbsolutePath === keyStorePath)
    assert(opts.trustStorePassword === Some("password"))
    assert(opts.keyStorePassword === Some("password"))
    assert(opts.keyPassword === Some("password"))
    assert(opts.protocol === Some("SSLv3"))
    assert(opts.enabledAlgorithms ===
      Set("TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"))
  }

  private def createCredentialProvider(tmpPath: String, conf: Configuration): CredentialProvider = {
    conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, tmpPath)

    val provider = CredentialProviderFactory.getProviders(conf).get(0)
    if (provider == null) {
      throw new IllegalStateException(s"Fail to get credential provider with path $tmpPath")
    }

    provider
  }

  private def storePassword(
      provider: CredentialProvider,
      passwordKey: String,
      password: String): Unit = {
    provider.createCredentialEntry(passwordKey, password.toCharArray)
    provider.flush()
  }
}
