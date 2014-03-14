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
import sbt.Classpaths.publishTask
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
import scala.util.Properties
import org.scalastyle.sbt.ScalastylePlugin.{Settings => ScalaStyleSettings}

import scala.collection.JavaConversions._

// For Sonatype publishing
//import com.jsuereth.pgp.sbtplugin.PgpKeys._

object SparkBuild extends Build {
  // Hadoop version to build against. For example, "1.0.4" for Apache releases, or
  // "2.0.0-mr1-cdh4.2.0" for Cloudera Hadoop. Note that these variables can be set
  // through the environment variables SPARK_HADOOP_VERSION and SPARK_YARN.
  val DEFAULT_HADOOP_VERSION = "1.0.4"

  // Whether the Hadoop version to build against is 2.2.x, or a variant of it. This can be set
  // through the SPARK_IS_NEW_HADOOP environment variable.
  val DEFAULT_IS_NEW_HADOOP = false

  val DEFAULT_YARN = false

  // HBase version; set as appropriate.
  val HBASE_VERSION = "0.94.6"

  // Target JVM version
  val SCALAC_JVM_VERSION = "jvm-1.6"
  val JAVAC_JVM_VERSION = "1.6"

  lazy val root = Project("root", file("."), settings = rootSettings) aggregate(allProjects: _*)

  lazy val core = Project("core", file("core"), settings = coreSettings)

  lazy val repl = Project("repl", file("repl"), settings = replSettings)
    .dependsOn(core, graphx, bagel, mllib)

  lazy val tools = Project("tools", file("tools"), settings = toolsSettings) dependsOn(core) dependsOn(streaming)

  lazy val bagel = Project("bagel", file("bagel"), settings = bagelSettings) dependsOn(core)

  lazy val graphx = Project("graphx", file("graphx"), settings = graphxSettings) dependsOn(core)

  lazy val streaming = Project("streaming", file("streaming"), settings = streamingSettings) dependsOn(core)

  lazy val mllib = Project("mllib", file("mllib"), settings = mllibSettings) dependsOn(core)

  lazy val assemblyProj = Project("assembly", file("assembly"), settings = assemblyProjSettings)
    .dependsOn(core, graphx, bagel, mllib, repl, streaming) dependsOn(maybeYarn: _*) dependsOn(maybeGanglia: _*)

  lazy val assembleDeps = TaskKey[Unit]("assemble-deps", "Build assembly of dependencies and packages Spark projects")

  // A configuration to set an alternative publishLocalConfiguration
  lazy val MavenCompile = config("m2r") extend(Compile)
  lazy val publishLocalBoth = TaskKey[Unit]("publish-local", "publish local for m2 and ivy")
  val sparkHome = System.getProperty("user.dir")

  // Allows build configuration to be set through environment variables
  lazy val hadoopVersion = Properties.envOrElse("SPARK_HADOOP_VERSION", DEFAULT_HADOOP_VERSION)
  lazy val isNewHadoop = Properties.envOrNone("SPARK_IS_NEW_HADOOP") match {
    case None => {
      val isNewHadoopVersion = "2.[2-9]+".r.findFirstIn(hadoopVersion).isDefined
      (isNewHadoopVersion|| DEFAULT_IS_NEW_HADOOP)
    }
    case Some(v) => v.toBoolean
  }

  lazy val isYarnEnabled = Properties.envOrNone("SPARK_YARN") match {
    case None => DEFAULT_YARN
    case Some(v) => v.toBoolean
  }
  lazy val hadoopClient = if (hadoopVersion.startsWith("0.20.") || hadoopVersion == "1.0.0") "hadoop-core" else "hadoop-client"
  val maybeAvro = if (hadoopVersion.startsWith("0.23.") && isYarnEnabled) Seq("org.apache.avro" % "avro" % "1.7.4") else Seq()

  // Include Ganglia integration if the user has enabled Ganglia
  // This is isolated from the normal build due to LGPL-licensed code in the library
  lazy val isGangliaEnabled = Properties.envOrNone("SPARK_GANGLIA_LGPL").isDefined
  lazy val gangliaProj = Project("spark-ganglia-lgpl", file("extras/spark-ganglia-lgpl"), settings = gangliaSettings).dependsOn(core)
  val maybeGanglia: Seq[ClasspathDependency] = if (isGangliaEnabled) Seq(gangliaProj) else Seq()
  val maybeGangliaRef: Seq[ProjectReference] = if (isGangliaEnabled) Seq(gangliaProj) else Seq()

  // Include the Java 8 project if the JVM version is 8+
  lazy val javaVersion = System.getProperty("java.specification.version")
  lazy val isJava8Enabled = javaVersion.toDouble >= "1.8".toDouble
  val maybeJava8Tests = if (isJava8Enabled) Seq[ProjectReference](java8Tests) else Seq[ProjectReference]()
  lazy val java8Tests = Project("java8-tests", file("extras/java8-tests"), settings = java8TestsSettings).
    dependsOn(core) dependsOn(streaming % "compile->compile;test->test")

  // Include the YARN project if the user has enabled YARN
  lazy val yarnAlpha = Project("yarn-alpha", file("yarn/alpha"), settings = yarnAlphaSettings) dependsOn(core)
  lazy val yarn = Project("yarn", file("yarn/stable"), settings = yarnSettings) dependsOn(core)

  lazy val maybeYarn: Seq[ClasspathDependency] = if (isYarnEnabled) Seq(if (isNewHadoop) yarn else yarnAlpha) else Seq()
  lazy val maybeYarnRef: Seq[ProjectReference] = if (isYarnEnabled) Seq(if (isNewHadoop) yarn else yarnAlpha) else Seq()

  lazy val externalTwitter = Project("external-twitter", file("external/twitter"), settings = twitterSettings)
    .dependsOn(streaming % "compile->compile;test->test")

  lazy val externalKafka = Project("external-kafka", file("external/kafka"), settings = kafkaSettings)
    .dependsOn(streaming % "compile->compile;test->test")

  lazy val externalFlume = Project("external-flume", file("external/flume"), settings = flumeSettings)
    .dependsOn(streaming % "compile->compile;test->test")

  lazy val externalZeromq = Project("external-zeromq", file("external/zeromq"), settings = zeromqSettings)
    .dependsOn(streaming % "compile->compile;test->test")

  lazy val externalMqtt = Project("external-mqtt", file("external/mqtt"), settings = mqttSettings)
    .dependsOn(streaming % "compile->compile;test->test")

  lazy val allExternal = Seq[ClasspathDependency](externalTwitter, externalKafka, externalFlume, externalZeromq, externalMqtt)
  lazy val allExternalRefs = Seq[ProjectReference](externalTwitter, externalKafka, externalFlume, externalZeromq, externalMqtt)

  lazy val examples = Project("examples", file("examples"), settings = examplesSettings)
    .dependsOn(core, mllib, graphx, bagel, streaming, externalTwitter) dependsOn(allExternal: _*)

  // Everything except assembly, tools, java8Tests and examples belong to packageProjects
  lazy val packageProjects = Seq[ProjectReference](core, repl, bagel, streaming, mllib, graphx) ++ maybeYarnRef ++ maybeGangliaRef

  lazy val allProjects = packageProjects ++ allExternalRefs ++
    Seq[ProjectReference](examples, tools, assemblyProj) ++ maybeJava8Tests

  def sharedSettings = Defaults.defaultSettings ++ Seq(
    organization       := "org.apache.spark",
    version            := "1.0.0-SNAPSHOT",
    scalaVersion       := "2.10.3",
    scalacOptions := Seq("-Xmax-classfile-name", "120", "-unchecked", "-deprecation",
      "-target:" + SCALAC_JVM_VERSION),
    javacOptions := Seq("-target", JAVAC_JVM_VERSION, "-source", JAVAC_JVM_VERSION),
    unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
    retrieveManaged := true,
    javaHome := Properties.envOrNone("JAVA_HOME").map(file),
    // This is to add convenience of enabling sbt -Dsbt.offline=true for making the build offline.
    offline := "true".equalsIgnoreCase(sys.props("sbt.offline")),
    retrievePattern := "[type]s/[artifact](-[revision])(-[classifier]).[ext]",
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
    testListeners <<= target.map(t => Seq(new eu.henkelmann.sbt.JUnitXmlTestsListener(t.getAbsolutePath))),

    // Fork new JVMs for tests and set Java options for those
    fork := true,
    javaOptions in Test += "-Dspark.home=" + sparkHome,
    javaOptions in Test += "-Dspark.testing=1",
    javaOptions in Test ++= System.getProperties.filter(_._1 startsWith "spark").map { case (k,v) => s"-D$k=$v" }.toSeq,
    javaOptions += "-Xmx3g",
    // Show full stack trace and duration in test cases.
    testOptions in Test += Tests.Argument("-oDF"),
    // Remove certain packages from Scaladoc
    scalacOptions in (Compile,doc) := Seq("-skip-packages", Seq(
      "akka",
      "org.apache.spark.network",
      "org.apache.spark.deploy",
      "org.apache.spark.util.collection"
      ).mkString(":")),

    // Only allow one test at a time, even across projects, since they run in the same JVM
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),

    resolvers ++= Seq(
      "Maven Repository"     at "https://repo.maven.apache.org/maven2",
      "Apache Repository"    at "https://repository.apache.org/content/repositories/releases",
      "JBoss Repository"     at "https://repository.jboss.org/nexus/content/repositories/releases/",
      "MQTT Repository"      at "https://repo.eclipse.org/content/repositories/paho-releases/",
      "Cloudera Repository"  at "https://repository.cloudera.com/artifactory/cloudera-repos/",
      // For Sonatype publishing
      //"sonatype-snapshots"   at "https://oss.sonatype.org/content/repositories/snapshots",
      //"sonatype-staging"     at "https://oss.sonatype.org/service/local/staging/deploy/maven2/",
      // also check the local Maven repository ~/.m2
      Resolver.file("Local Maven Repo", file(Path.userHome + "/.m2/repository"))
    ),

    publishMavenStyle := true,

    //useGpg in Global := true,

    pomExtra := (
      <parent>
        <groupId>org.apache</groupId>
        <artifactId>apache</artifactId>
        <version>13</version>
      </parent>
      <url>http://spark.apache.org/</url>
      <licenses>
        <license>
          <name>Apache 2.0 License</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <connection>scm:git:git@github.com:apache/spark.git</connection>
        <url>scm:git:git@github.com:apache/spark.git</url>
      </scm>
      <developers>
        <developer>
          <id>matei</id>
          <name>Matei Zaharia</name>
          <email>matei.zaharia@gmail.com</email>
          <url>http://www.cs.berkeley.edu/~matei</url>
          <organization>Apache Software Foundation</organization>
          <organizationUrl>http://spark.apache.org</organizationUrl>
        </developer>
      </developers>
      <issueManagement>
        <system>JIRA</system>
        <url>https://spark-project.atlassian.net/browse/SPARK</url>
      </issueManagement>
    ),

    /*
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("sonatype-snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("sonatype-staging"  at nexus + "service/local/staging/deploy/maven2")
    },

    */

    libraryDependencies ++= Seq(
        "io.netty"          % "netty-all"       % "4.0.17.Final",
        "org.eclipse.jetty" % "jetty-server"    % "9.1.3.v20140225",
        "org.eclipse.jetty" % "jetty-util" % "9.1.3.v20140225",
        "org.eclipse.jetty" % "jetty-plus" % "9.1.3.v20140225",
        "org.eclipse.jetty" % "jetty-security" % "9.1.3.v20140225",
        /** Workaround for SPARK-959. Dependency used by org.eclipse.jetty. Fixed in ivy 2.3.0. */
        "org.eclipse.jetty.orbit" % "javax.servlet" % "2.5.0.v201103041518" artifacts Artifact("javax.servlet", "jar", "jar"),
        "org.scalatest"    %% "scalatest"       % "1.9.1"  % "test",
        "org.scalacheck"   %% "scalacheck"      % "1.10.0" % "test",
        "com.novocode"      % "junit-interface" % "0.10"   % "test",
        "org.easymock"      % "easymock"        % "3.1"    % "test",
        "org.mockito"       % "mockito-all"     % "1.8.5"  % "test",
        "commons-io"        % "commons-io"      % "2.4"    % "test"
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
    parallelExecution := true,
    /* Workaround for issue #206 (fixed after SBT 0.11.0) */
    watchTransitiveSources <<= Defaults.inDependencies[Task[Seq[File]]](watchSources.task,
      const(std.TaskExtra.constant(Nil)), aggregate = true, includeRoot = true) apply { _.join.map(_.flatten) },

    otherResolvers := Seq(Resolver.file("dotM2", file(Path.userHome + "/.m2/repository"))),
    publishLocalConfiguration in MavenCompile <<= (packagedArtifacts, deliverLocal, ivyLoggingLevel) map {
      (arts, _, level) => new PublishConfiguration(None, "dotM2", arts, Seq(), level)
    },
    publishMavenStyle in MavenCompile := true,
    publishLocal in MavenCompile <<= publishTask(publishLocalConfiguration in MavenCompile, deliverLocal),
    publishLocalBoth <<= Seq(publishLocal in MavenCompile, publishLocal).dependOn
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++ ScalaStyleSettings

  val slf4jVersion = "1.7.5"

  val excludeNetty = ExclusionRule(organization = "org.jboss.netty")
  val excludeAsm = ExclusionRule(organization = "org.ow2.asm")
  val excludeOldAsm = ExclusionRule(organization = "asm")
  val excludeCommonsLogging = ExclusionRule(organization = "commons-logging")
  val excludeSLF4J = ExclusionRule(organization = "org.slf4j")
  val excludeScalap = ExclusionRule(organization = "org.scala-lang", artifact = "scalap")

  def coreSettings = sharedSettings ++ Seq(
    name := "spark-core",

    libraryDependencies ++= Seq(
        "com.google.guava"           % "guava"            % "14.0.1",
        "com.google.code.findbugs"   % "jsr305"           % "1.3.9",
        "log4j"                      % "log4j"            % "1.2.17",
        "org.slf4j"                  % "slf4j-api"        % slf4jVersion,
        "org.slf4j"                  % "slf4j-log4j12"    % slf4jVersion,
        "org.slf4j"                  % "jul-to-slf4j"     % slf4jVersion,
        "org.slf4j"                  % "jcl-over-slf4j"   % slf4jVersion,
        "commons-daemon"             % "commons-daemon"   % "1.0.10", // workaround for bug HADOOP-9407
        "com.ning"                   % "compress-lzf"     % "1.0.0",
        "org.xerial.snappy"          % "snappy-java"      % "1.0.5",
        "org.spark-project.akka"    %% "akka-remote"      % "2.2.3-shaded-protobuf"  excludeAll(excludeNetty),
        "org.spark-project.akka"    %% "akka-slf4j"       % "2.2.3-shaded-protobuf"  excludeAll(excludeNetty),
        "org.spark-project.akka"    %% "akka-testkit"     % "2.2.3-shaded-protobuf" % "test",
        "org.json4s"                %% "json4s-jackson"   % "3.2.6" excludeAll(excludeScalap),
        "it.unimi.dsi"               % "fastutil"         % "6.4.4",
        "colt"                       % "colt"             % "1.2.0",
        "org.apache.mesos"           % "mesos"            % "0.13.0",
        "commons-net"                % "commons-net"      % "2.2",
        "net.java.dev.jets3t"        % "jets3t"           % "0.7.1" excludeAll(excludeCommonsLogging),
        "org.apache.derby"           % "derby"            % "10.4.2.0"                     % "test",
        "org.apache.hadoop"          % hadoopClient       % hadoopVersion excludeAll(excludeNetty, excludeAsm, excludeCommonsLogging, excludeSLF4J, excludeOldAsm),
        "org.apache.curator"         % "curator-recipes"  % "2.4.0" excludeAll(excludeNetty),
        "com.codahale.metrics"       % "metrics-core"     % "3.0.0",
        "com.codahale.metrics"       % "metrics-jvm"      % "3.0.0",
        "com.codahale.metrics"       % "metrics-json"     % "3.0.0",
        "com.codahale.metrics"       % "metrics-graphite" % "3.0.0",
        "com.twitter"               %% "chill"            % "0.3.1" excludeAll(excludeAsm),
        "com.twitter"                % "chill-java"       % "0.3.1" excludeAll(excludeAsm),
        "com.clearspring.analytics"  % "stream"           % "2.5.1"
      ),
    libraryDependencies ++= maybeAvro
  )

  def rootSettings = sharedSettings ++ Seq(
    publish := {}
  )

 def replSettings = sharedSettings ++ Seq(
    name := "spark-repl",
   libraryDependencies <+= scalaVersion(v => "org.scala-lang"  % "scala-compiler" % v ),
   libraryDependencies <+= scalaVersion(v => "org.scala-lang"  % "jline"          % v ),
   libraryDependencies <+= scalaVersion(v => "org.scala-lang"  % "scala-reflect"  % v )
  )

  def examplesSettings = sharedSettings ++ Seq(
    name := "spark-examples",
    libraryDependencies ++= Seq(
      "com.twitter"          %% "algebird-core"   % "0.1.11",
      "org.apache.hbase" % "hbase" % HBASE_VERSION excludeAll(excludeNetty, excludeAsm, excludeOldAsm, excludeCommonsLogging),
      "org.apache.cassandra" % "cassandra-all" % "1.2.6"
        exclude("com.google.guava", "guava")
        exclude("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru")
        exclude("com.ning","compress-lzf")
        exclude("io.netty", "netty")
        exclude("jline","jline")
        exclude("org.apache.cassandra.deps", "avro")
        excludeAll(excludeSLF4J)
    )
  ) ++ assemblySettings ++ extraAssemblySettings

  def toolsSettings = sharedSettings ++ Seq(
    name := "spark-tools"
  ) ++ assemblySettings ++ extraAssemblySettings

  def graphxSettings = sharedSettings ++ Seq(
    name := "spark-graphx",
    libraryDependencies ++= Seq(
      "org.jblas" % "jblas" % "1.2.3"
    )
  )

  def bagelSettings = sharedSettings ++ Seq(
    name := "spark-bagel"
  )

  def mllibSettings = sharedSettings ++ Seq(
    name := "spark-mllib",
    libraryDependencies ++= Seq(
      "org.jblas" % "jblas" % "1.2.3"
    )
  )

  def streamingSettings = sharedSettings ++ Seq(
    name := "spark-streaming",
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" % "2.4"
    )
  )

  def yarnCommonSettings = sharedSettings ++ Seq(
    unmanagedSourceDirectories in Compile <++= baseDirectory { base =>
      Seq(
         base / "../common/src/main/scala"
      )
    },

    unmanagedSourceDirectories in Test <++= baseDirectory { base =>
      Seq(
         base / "../common/src/test/scala"
      )
    }

  ) ++ extraYarnSettings

  def yarnAlphaSettings = yarnCommonSettings ++ Seq(
    name := "spark-yarn-alpha"
  )

  def yarnSettings = yarnCommonSettings ++ Seq(
    name := "spark-yarn"
  )

  def gangliaSettings = sharedSettings ++ Seq(
    name := "spark-ganglia-lgpl",
    libraryDependencies += "com.codahale.metrics" % "metrics-ganglia" % "3.0.0"
  )

  def java8TestsSettings = sharedSettings ++ Seq(
    name := "java8-tests",
    javacOptions := Seq("-target", "1.8", "-source", "1.8"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")
  )

  // Conditionally include the YARN dependencies because some tools look at all sub-projects and will complain
  // if we refer to nonexistent dependencies (e.g. hadoop-yarn-api from a Hadoop version without YARN).
  def extraYarnSettings = if(isYarnEnabled) yarnEnabledSettings else Seq()

  def yarnEnabledSettings = Seq(
    libraryDependencies ++= Seq(
      // Exclude rule required for all ?
      "org.apache.hadoop" % hadoopClient         % hadoopVersion excludeAll(excludeNetty, excludeAsm, excludeOldAsm),
      "org.apache.hadoop" % "hadoop-yarn-api"    % hadoopVersion excludeAll(excludeNetty, excludeAsm, excludeOldAsm),
      "org.apache.hadoop" % "hadoop-yarn-common" % hadoopVersion excludeAll(excludeNetty, excludeAsm, excludeOldAsm),
      "org.apache.hadoop" % "hadoop-yarn-client" % hadoopVersion excludeAll(excludeNetty, excludeAsm, excludeOldAsm)
    )
  )

  def assemblyProjSettings = sharedSettings ++ Seq(
    libraryDependencies += "net.sf.py4j" % "py4j" % "0.8.1",
    name := "spark-assembly",
    assembleDeps in Compile <<= (packageProjects.map(packageBin in Compile in _) ++ Seq(packageDependency in Compile)).dependOn,
    jarName in assembly <<= version map { v => "spark-assembly-" + v + "-hadoop" + hadoopVersion + ".jar" },
    jarName in packageDependency <<= version map { v => "spark-assembly-" + v + "-hadoop" + hadoopVersion + "-deps.jar" }
  ) ++ assemblySettings ++ extraAssemblySettings

  def extraAssemblySettings() = Seq(
    test in assembly := {},
    mergeStrategy in assembly := {
      case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
      case "log4j.properties" => MergeStrategy.discard
      case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    }
  )

  def twitterSettings() = sharedSettings ++ Seq(
    name := "spark-streaming-twitter",
    libraryDependencies ++= Seq(
      "org.twitter4j" % "twitter4j-stream" % "3.0.3" excludeAll(excludeNetty)
    )
  )

  def kafkaSettings() = sharedSettings ++ Seq(
    name := "spark-streaming-kafka",
    libraryDependencies ++= Seq(
      "com.github.sgroschupf"    % "zkclient"   % "0.1"          excludeAll(excludeNetty),
      "org.apache.kafka"        %% "kafka"      % "0.8.0"
        exclude("com.sun.jdmk", "jmxtools")
        exclude("com.sun.jmx", "jmxri")
        exclude("net.sf.jopt-simple", "jopt-simple")
        excludeAll(excludeNetty, excludeSLF4J)
    )
  )

  def flumeSettings() = sharedSettings ++ Seq(
    name := "spark-streaming-flume",
    libraryDependencies ++= Seq(
      "org.apache.flume" % "flume-ng-sdk" % "1.2.0" % "compile" excludeAll(excludeNetty)
    )
  )

  def zeromqSettings() = sharedSettings ++ Seq(
    name := "spark-streaming-zeromq",
    libraryDependencies ++= Seq(
      "org.spark-project.akka" %% "akka-zeromq" % "2.2.3-shaded-protobuf" excludeAll(excludeNetty)
    )
  )

  def mqttSettings() = streamingSettings ++ Seq(
    name := "spark-streaming-mqtt",
    libraryDependencies ++= Seq("org.eclipse.paho" % "mqtt-client" % "0.4.0")
  )
}
