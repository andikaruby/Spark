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

package org.apache.spark;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

public class SharedSparkSession {

  public transient SparkSession spark;
  public transient JavaSparkContext jsc;

  @Before
  public void setUp() throws IOException {
    spark = SparkSession.builder()
      .master("local")
      .appName("shared-spark-session")
      .getOrCreate();
    jsc = new JavaSparkContext(spark.sparkContext());

    customSetUp();
    customSetUpWithException();
  }

  public void customSetUp() {}

  // TODO: Remove this once we have a way to use customSetUp that Exception
  public void customSetUpWithException() throws IOException {}

  @After
  public void tearDown() {
    spark.stop();
    spark = null;

    customTearDown();
  }

  public void customTearDown() {}
}
