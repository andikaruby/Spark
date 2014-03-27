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

import sbt._
import sbt.Keys._

object SparkPluginDef extends Build {
  lazy val root = Project("plugins", file(".")) dependsOn(sparkStyle)
  lazy val sparkStyle = Project("spark-style", file("spark-style"), settings = styleSettings)
  val sparkVersion = "1.0.0-SNAPSHOT"
  // There is actually no need to publish this artifact.
  def styleSettings = Defaults.defaultSettings ++ Seq (
    name                 :=  "spark-style",
    organization         :=  "org.apache.spark",
    version              :=  sparkVersion,
    scalaVersion         :=  "2.10.3",
    scalacOptions        :=  Seq("-unchecked", "-deprecation"),
    libraryDependencies  ++= Dependencies.scalaStyle,
    sbtPlugin            :=  true
  )

  object Dependencies {
    val scalaStyle = Seq("org.scalastyle" %% "scalastyle" % "0.4.0")
  }
}
