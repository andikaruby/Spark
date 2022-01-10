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

package org.apache.spark.sql.errors

import org.apache.spark.{SparkException, SparkIllegalArgumentException, SparkUnsupportedOperationException}
import org.apache.spark.sql.{DataFrame, QueryTest}
import org.apache.spark.sql.test.SharedSparkSession

class QueryExecutionErrorsSuite extends QueryTest with SharedSparkSession {
  import testImplicits._

  private def getAesInputs(): (DataFrame, DataFrame) = {
    val encryptedText16 = "4Hv0UKCx6nfUeAoPZo1z+w=="
    val encryptedText24 = "NeTYNgA+PCQBN50DA//O2w=="
    val encryptedText32 = "9J3iZbIxnmaG+OIA9Amd+A=="
    val encryptedEmptyText16 = "jmTOhz8XTbskI/zYFFgOFQ=="
    val encryptedEmptyText24 = "9RDK70sHNzqAFRcpfGM5gQ=="
    val encryptedEmptyText32 = "j9IDsCvlYXtcVJUf4FAjQQ=="

    val df1 = Seq("Spark", "").toDF
    val df2 = Seq(
      (encryptedText16, encryptedText24, encryptedText32),
      (encryptedEmptyText16, encryptedEmptyText24, encryptedEmptyText32)
    ).toDF("value16", "value24", "value32")

    (df1, df2)
  }

  test("SPARK-37858: invalid AES key length") {
    val (df1, df2) = getAesInputs()
    def checkInvalidKeyLength(df: => DataFrame): Unit = {
      val e = intercept[SparkException] {
        df.collect
      }.getCause
      assert(e.isInstanceOf[SparkIllegalArgumentException])
      assert(e.getMessage.contains(
        "The key length of aes_encrypt/aes_decrypt should be one of 16, 24 or 32 bytes"))
    }

    // Encryption failure - invalid key length
    checkInvalidKeyLength(df1.selectExpr("aes_encrypt(value, '12345678901234567')"))
    checkInvalidKeyLength(df1.selectExpr("aes_encrypt(value, binary('123456789012345'))"))
    checkInvalidKeyLength(df1.selectExpr("aes_encrypt(value, binary(''))"))

    // Decryption failure - invalid key length
    Seq("value16", "value24", "value32").foreach { colName =>
      checkInvalidKeyLength(df2.selectExpr(
        s"aes_decrypt(unbase64($colName), '12345678901234567')"))
      checkInvalidKeyLength(df2.selectExpr(
        s"aes_decrypt(unbase64($colName), binary('123456789012345'))"))
      checkInvalidKeyLength(df2.selectExpr(
        s"aes_decrypt(unbase64($colName), '')"))
      checkInvalidKeyLength(df2.selectExpr(
        s"aes_decrypt(unbase64($colName), binary(''))"))
    }
  }

  test("SPARK-37858: unsupported AES modes") {
    val key16 = "abcdefghijklmnop"
    val key32 = "abcdefghijklmnop12345678ABCDEFGH"
    val (df1, df2) = getAesInputs()
    def checkUnsupportedMode(df: => DataFrame): Unit = {
      val e = intercept[SparkException] {
        df.collect
      }.getCause
      assert(e.isInstanceOf[SparkUnsupportedOperationException])
      assert(e.getMessage.matches("""The AES mode \w+ with the padding \w+ is not supported"""))
    }

    // Unsupported AES mode and padding in encrypt
    checkUnsupportedMode(df1.selectExpr(s"aes_encrypt(value, '$key16', 'CBC')"))
    checkUnsupportedMode(df1.selectExpr(s"aes_encrypt(value, '$key16', 'ECB', 'NoPadding')"))

    // Unsupported AES mode and padding in decrypt
    checkUnsupportedMode(df2.selectExpr(s"aes_decrypt(value16, '$key16', 'GSM')"))
    checkUnsupportedMode(df2.selectExpr(s"aes_decrypt(value16, '$key16', 'GCM', 'PKCS')"))
    checkUnsupportedMode(df2.selectExpr(s"aes_decrypt(value32, '$key32', 'ECB', 'None')"))
  }
}
