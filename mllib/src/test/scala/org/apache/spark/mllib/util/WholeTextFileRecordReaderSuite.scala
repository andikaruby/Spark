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

package org.apache.spark.mllib.util

import java.io.DataOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.immutable.IndexedSeq

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

import org.apache.hadoop.io.Text

import org.apache.spark.mllib.util.MLUtils._
import org.apache.spark.SparkContext

/**
 * Tests the correctness of [[org.apache.spark.mllib.input.WholeTextFileRecordReader]]. A temporary
 * directory is created as fake input. Temporal storage would be deleted in the end.
 */
class WholeTextFileRecordReaderSuite extends FunSuite with BeforeAndAfterAll {
  private var sc: SparkContext = _

  override def beforeAll() {
    sc = new SparkContext("local", "test")
  }

  override def afterAll() {
    sc.stop()
  }

  private def createNativeFile(inputDir: Path, fileName: String, contents: Array[Byte]) = {
    val out = new DataOutputStream(new FileOutputStream(s"${inputDir.toString}/$fileName"))
    out.write(contents, 0, contents.length)
    out.close()
  }

  /**
   * This code will test the behaviors of WholeTextFileRecordReader based on local disk. There are
   * three aspects to check:
   *   1) Whether all files are read;
   *   2) Whether paths are read correctly;
   *   3) Does the contents be the same.
   */
  test("Correctness of WholeTextFileRecordReader.") {

    val dir = Files.createTempDirectory("wholeFiles")
    println(s"Local disk address is ${dir.toString}.")

    WholeTextFileRecordReaderSuite.files.foreach { case (filename, contents) =>
      createNativeFile(dir, filename, contents)
    }

    val res = wholeTextFile(sc, dir.toString).collect()

    assert(res.size === WholeTextFileRecordReaderSuite.fileNames.size,
      "Number of files read out does not fit with the actual value.")

    for ((filename, contents) <- res) {
      val shortName = filename.split('/').last
      assert(WholeTextFileRecordReaderSuite.fileNames.contains(shortName),
        s"Missing file name $filename.")
      assert(contents === new Text(WholeTextFileRecordReaderSuite.files(shortName)).toString,
        s"file $filename contents can not match.")
    }

    WholeTextFileRecordReaderSuite.fileNames.foreach { filename =>
      Files.deleteIfExists(Paths.get(s"${dir.toString}/$filename"))
    }
    Files.deleteIfExists(dir)
  }
}

/**
 * Files to be tested are defined here.
 */
object WholeTextFileRecordReaderSuite {
  private val testWords: IndexedSeq[Byte] = "Spark is easy to use.\n".map(_.toByte)

  private val fileNames = Array("part-00000", "part-00001", "part-00002")
  private val fileLengths = Array(10, 100, 1000)

  private val files = fileLengths.zip(fileNames).map { case (upperBound, filename) =>
    filename -> Stream.continually(testWords.toList.toStream).flatten.take(upperBound).toArray
  }.toMap
}
