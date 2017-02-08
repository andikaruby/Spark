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

package org.apache.spark.internal.config

import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.collection.mutable.HashMap

import org.apache.spark.{SparkConf, SparkFunSuite}
import org.apache.spark.network.util.ByteUnit
import org.apache.spark.util.SparkConfWithEnv

class ConfigEntrySuite extends SparkFunSuite {

  private val PREFIX = "spark.ConfigEntrySuite"

  private def testKey(name: String): String = s"$PREFIX.$name"

  test("conf entry: int") {
    val conf = new SparkConf()
    val iConf = ConfigBuilder(testKey("int")).intConf.createWithDefault(1)
    assert(conf.get(iConf) === 1)
    conf.set(iConf, 2)
    assert(conf.get(iConf) === 2)
  }

  test("conf entry: long") {
    val conf = new SparkConf()
    val lConf = ConfigBuilder(testKey("long")).longConf.createWithDefault(0L)
    conf.set(lConf, 1234L)
    assert(conf.get(lConf) === 1234L)
  }

  test("conf entry: double") {
    val conf = new SparkConf()
    val dConf = ConfigBuilder(testKey("double")).doubleConf.createWithDefault(0.0)
    conf.set(dConf, 20.0)
    assert(conf.get(dConf) === 20.0)
  }

  test("conf entry: boolean") {
    val conf = new SparkConf()
    val bConf = ConfigBuilder(testKey("boolean")).booleanConf.createWithDefault(false)
    assert(!conf.get(bConf))
    conf.set(bConf, true)
    assert(conf.get(bConf))
  }

  test("conf entry: optional") {
    val conf = new SparkConf()
    val optionalConf = ConfigBuilder(testKey("optional")).intConf.createOptional
    assert(conf.get(optionalConf) === None)
    conf.set(optionalConf, 1)
    assert(conf.get(optionalConf) === Some(1))
  }

  test("conf entry: fallback") {
    val conf = new SparkConf()
    val parentConf = ConfigBuilder(testKey("parent")).intConf.createWithDefault(1)
    val confWithFallback = ConfigBuilder(testKey("fallback")).fallbackConf(parentConf)
    assert(conf.get(confWithFallback) === 1)
    conf.set(confWithFallback, 2)
    assert(conf.get(parentConf) === 1)
    assert(conf.get(confWithFallback) === 2)
  }

  test("conf entry: time") {
    val conf = new SparkConf()
    val time = ConfigBuilder(testKey("time")).timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("1h")
    assert(conf.get(time) === 3600L)
    conf.set(time.key, "1m")
    assert(conf.get(time) === 60L)
  }

  test("conf entry: bytes") {
    val conf = new SparkConf()
    val bytes = ConfigBuilder(testKey("bytes")).bytesConf(ByteUnit.KiB)
      .createWithDefaultString("1m")
    assert(conf.get(bytes) === 1024L)
    conf.set(bytes.key, "1k")
    assert(conf.get(bytes) === 1L)
  }

  test("conf entry: string seq") {
    val conf = new SparkConf()
    val seq = ConfigBuilder(testKey("seq")).stringConf.toSequence.createWithDefault(Seq())
    conf.set(seq.key, "1,,2, 3 , , 4")
    assert(conf.get(seq) === Seq("1", "2", "3", "4"))
    conf.set(seq, Seq("1", "2"))
    assert(conf.get(seq) === Seq("1", "2"))
  }

  test("conf entry: int seq") {
    val conf = new SparkConf()
    val seq = ConfigBuilder(testKey("intSeq")).intConf.toSequence.createWithDefault(Seq())
    conf.set(seq.key, "1,,2, 3 , , 4")
    assert(conf.get(seq) === Seq(1, 2, 3, 4))
    conf.set(seq, Seq(1, 2))
    assert(conf.get(seq) === Seq(1, 2))
  }

  test("conf entry: transformation") {
    val conf = new SparkConf()
    val transformationConf = ConfigBuilder(testKey("transformation"))
      .stringConf
      .transform(_.toLowerCase())
      .createWithDefault("FOO")

    assert(conf.get(transformationConf) === "foo")
    conf.set(transformationConf, "BAR")
    assert(conf.get(transformationConf) === "bar")
  }

  test("conf entry: checkValue()") {
    def createConf(default: Int): ConfigEntry[Int] =
      ConfigBuilder(testKey("checkValue"))
        .intConf
        .checkValue(value => value >= 0, "value must be non-negative")
        .createWithDefault(default)

    // this succeeds
    val conf = createConf(10)

    // this fails because valueConverter() calls checkValue()
    val e1 = intercept[IllegalArgumentException] { conf.valueConverter("-1") }
    assert(e1.getMessage == "value must be non-negative")

    // this fails because createWithDefault() calls valueConverter() which calls checkValue()
    val e2 = intercept[IllegalArgumentException] { createConf(-1) }
    assert(e2.getMessage == "value must be non-negative")
  }

  test("conf entry: valid values check") {
    val conf = new SparkConf()
    val enum = ConfigBuilder(testKey("enum"))
      .stringConf
      .checkValues(Set("a", "b", "c"))
      .createWithDefault("a")
    assert(conf.get(enum) === "a")

    conf.set(enum, "b")
    assert(conf.get(enum) === "b")

    conf.set(enum, "d")
    val enumError = intercept[IllegalArgumentException] {
      conf.get(enum)
    }
    assert(enumError.getMessage === s"The value of ${enum.key} should be one of a, b, c, but was d")
  }

  test("conf entry: conversion error") {
    val conf = new SparkConf()
    val conversionTest = ConfigBuilder(testKey("conversionTest")).doubleConf.createOptional
    conf.set(conversionTest.key, "abc")
    val conversionError = intercept[IllegalArgumentException] {
      conf.get(conversionTest)
    }
    assert(conversionError.getMessage === s"${conversionTest.key} should be double, but was abc")
  }

  test("default value handling is null-safe") {
    val conf = new SparkConf()
    val stringConf = ConfigBuilder(testKey("string")).stringConf.createWithDefault(null)
    assert(conf.get(stringConf) === null)
  }

  test("variable expansion of spark config entries") {
    val env = Map("ENV1" -> "env1")
    val conf = new SparkConfWithEnv(env)

    val stringConf = ConfigBuilder(testKey("stringForExpansion"))
      .stringConf
      .createWithDefault("string1")
    val optionalConf = ConfigBuilder(testKey("optionForExpansion"))
      .stringConf
      .createOptional
    val intConf = ConfigBuilder(testKey("intForExpansion"))
      .intConf
      .createWithDefault(42)
    val fallbackConf = ConfigBuilder(testKey("fallbackForExpansion"))
      .fallbackConf(intConf)

    val refConf = ConfigBuilder(testKey("configReferenceTest"))
      .stringConf
      .createWithDefault(null)

    def ref(entry: ConfigEntry[_]): String = "${" + entry.key + "}"

    def testEntryRef(entry: ConfigEntry[_], expected: String): Unit = {
      conf.set(refConf, ref(entry))
      assert(conf.get(refConf) === expected)
    }

    testEntryRef(stringConf, "string1")
    testEntryRef(intConf, "42")
    testEntryRef(fallbackConf, "42")

    testEntryRef(optionalConf, ref(optionalConf))

    conf.set(optionalConf, ref(stringConf))
    testEntryRef(optionalConf, "string1")

    conf.set(optionalConf, ref(fallbackConf))
    testEntryRef(optionalConf, "42")

    // Default string values with variable references.
    val parameterizedStringConf = ConfigBuilder(testKey("stringWithParams"))
      .stringConf
      .createWithDefault(ref(stringConf))
    assert(conf.get(parameterizedStringConf) === conf.get(stringConf))

    // Make sure SparkConf's env override works.
    conf.set(refConf, "${env:ENV1}")
    assert(conf.get(refConf) === env("ENV1"))

    // Conf with null default value is not expanded.
    val nullConf = ConfigBuilder(testKey("nullString"))
      .stringConf
      .createWithDefault(null)
    testEntryRef(nullConf, ref(nullConf))
  }

}
