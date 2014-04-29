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

package org.apache.spark.util

import java.io.IOException

import scala.io.Source
import scala.util.Try

import org.apache.hadoop.fs.Path
import org.scalatest.{BeforeAndAfter, FunSuite}

import org.apache.spark.SparkConf
import org.apache.spark.io.CompressionCodec

/**
 * Test writing files through the FileLogger.
 */
class FileLoggerSuite extends FunSuite with BeforeAndAfter {
  private val fileSystem = Utils.getHadoopFileSystem("/")
  private val allCompressionCodecs = Seq[String](
    "org.apache.spark.io.LZFCompressionCodec",
    "org.apache.spark.io.SnappyCompressionCodec"
  )
  private val logDir = "/tmp/test-file-logger"
  private val logDirPath = new Path(logDir)

  after {
    Try { fileSystem.delete(logDirPath, true) }
    Try { fileSystem.delete(new Path("falafel"), true) }
  }

  test("Simple logging") {
    testSingleFile()
  }

  test ("Simple logging with compression") {
    allCompressionCodecs.foreach { codec =>
      testSingleFile(Some(codec))
    }
  }

  test("Logging multiple files") {
    testMultipleFiles()
  }

  test("Logging multiple files with compression") {
    allCompressionCodecs.foreach { codec =>
      testMultipleFiles(Some(codec))
    }
  }

  test("Logging when directory already exists") {
    // Create the logging directory multiple times
    new FileLogger(logDir, new SparkConf, overwrite = true)
    new FileLogger(logDir, new SparkConf, overwrite = true)
    new FileLogger(logDir, new SparkConf, overwrite = true)

    // If overwrite is not enabled, an exception should be thrown
    intercept[IOException] { new FileLogger(logDir, new SparkConf, overwrite = false) }
  }


  /* ----------------- *
   * Actual test logic *
   * ----------------- */

  /**
   * Test logging to a single file.
   */
  private def testSingleFile(codecName: Option[String] = None) {
    val conf = getLoggingConf(codecName)
    val codec = codecName.map { c => CompressionCodec.createCodec(conf) }
    val logger =
      if (codecName.isDefined) {
        new FileLogger(logDir, conf, compress = true)
      } else {
        new FileLogger(logDir, conf)
      }
    assert(fileSystem.exists(logDirPath))
    assert(fileSystem.getFileStatus(logDirPath).isDir)
    assert(fileSystem.listStatus(logDirPath).size === 0)

    logger.newFile()
    val files = fileSystem.listStatus(logDirPath)
    assert(files.size === 1)
    val firstFile = files.head
    val firstFilePath = firstFile.getPath

    logger.log("hello")
    logger.flush()
    assert(readFileContent(firstFilePath, codec) === "hello")

    logger.log(" world")
    logger.close()
    assert(readFileContent(firstFilePath, codec) === "hello world")
  }

  /**
   * Test logging to multiple files.
   */
  private def testMultipleFiles(codecName: Option[String] = None) {
    val conf = getLoggingConf(codecName)
    val codec = codecName.map { c => CompressionCodec.createCodec(conf) }
    val logger =
      if (codecName.isDefined) {
        new FileLogger(logDir, conf, compress = true)
      } else {
        new FileLogger(logDir, conf)
      }
    assert(fileSystem.exists(logDirPath))
    assert(fileSystem.getFileStatus(logDirPath).isDir)
    assert(fileSystem.listStatus(logDirPath).size === 0)

    logger.newFile("Jean_Valjean")
    logger.logLine("Who am I?")
    logger.logLine("Destiny?")
    logger.newFile("John_Valjohn")
    logger.logLine("One")
    logger.logLine("Two three four...")
    logger.close()
    assert(readFileContent(new Path(logDir + "/Jean_Valjean"), codec) === "Who am I?\nDestiny?")
    assert(readFileContent(new Path(logDir + "/John_Valjohn"), codec) === "One\nTwo three four...")
  }

  private def readFileContent(logPath: Path, codec: Option[CompressionCodec] = None): String = {
    val fstream = fileSystem.open(logPath)
    val cstream = codec.map(_.compressedInputStream(fstream)).getOrElse(fstream)
    Source.fromInputStream(cstream).getLines().mkString("\n")
  }

  private def getLoggingConf(codecName: Option[String]) = {
    val conf = new SparkConf
    codecName.foreach { c => conf.set("spark.io.compression.codec", c) }
    conf
  }

}
