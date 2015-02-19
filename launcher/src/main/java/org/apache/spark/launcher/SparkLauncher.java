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

package org.apache.spark.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static org.apache.spark.launcher.CommandBuilderUtils.*;

/**
 * Launcher for Spark applications.
 * <p/>
 * Use this class to start Spark applications programmatically. The class uses a builder pattern
 * to allow clients to configure the Spark application and launch it as a child process.
 * <p/>
 * Note that launching Spark applications using this class will not automatically load environment
 * variables from the "spark-env.sh" or "spark-env.cmd" scripts in the configuration directory.
 */
public class SparkLauncher {

  /** The Spark master. */
  public static final String SPARK_MASTER = "spark.master";

  /** Configuration key for the driver memory. */
  public static final String DRIVER_MEMORY = "spark.driver.memory";
  /** Configuration key for the driver class path. */
  public static final String DRIVER_EXTRA_CLASSPATH = "spark.driver.extraClassPath";
  /** Configuration key for the driver VM options. */
  public static final String DRIVER_EXTRA_JAVA_OPTIONS = "spark.driver.extraJavaOptions";
  /** Configuration key for the driver native library path. */
  public static final String DRIVER_EXTRA_LIBRARY_PATH = "spark.driver.extraLibraryPath";

  /** Configuration key for the executor memory. */
  public static final String EXECUTOR_MEMORY = "spark.executor.memory";
  /** Configuration key for the executor class path. */
  public static final String EXECUTOR_EXTRA_CLASSPATH = "spark.executor.extraClassPath";
  /** Configuration key for the executor VM options. */
  public static final String EXECUTOR_EXTRA_JAVA_OPTIONS = "spark.executor.extraJavaOptions";
  /** Configuration key for the executor native library path. */
  public static final String EXECUTOR_EXTRA_LIBRARY_PATH = "spark.executor.extraLibraryOptions";
  /** Configuration key for the number of executor CPU cores. */
  public static final String EXECUTOR_CORES = "spark.executor.cores";

  private static final String ENV_SPARK_HOME = "SPARK_HOME";
  private static final String DEFAULT_PROPERTIES_FILE = "spark-defaults.conf";
  static final String DEFAULT_MEM = "512m";

  boolean verbose;
  String appName;
  String appResource;
  String deployMode;
  String javaHome;
  String mainClass;
  String master;
  String propertiesFile;
  final List<String> sparkArgs;
  final List<String> appArgs;
  final List<String> jars;
  final List<String> files;
  final List<String> pyFiles;
  final Map<String, String> childEnv;
  final Map<String, String> conf;

  public SparkLauncher() {
    this(Collections.<String, String>emptyMap());
  }

  public SparkLauncher(Map<String, String> env) {
    this.appArgs = new ArrayList<String>();
    this.childEnv = new HashMap<String, String>(env);
    this.conf = new HashMap<String, String>();
    this.files = new ArrayList<String>();
    this.jars = new ArrayList<String>();
    this.pyFiles = new ArrayList<String>();
    this.sparkArgs = new ArrayList<String>();
  }

  /**
   * Set a custom JAVA_HOME for launching the Spark application.
   *
   * @param javaHome Path to the JAVA_HOME to use.
   * @return This launcher.
   */
  public SparkLauncher setJavaHome(String javaHome) {
    checkNotNull(javaHome, "javaHome");
    this.javaHome = javaHome;
    return this;
  }

  /**
   * Set a custom Spark installation location for the application.
   *
   * @param sparkHome Path to the Spark installation to use.
   * @return This launcher.
   */
  public SparkLauncher setSparkHome(String sparkHome) {
    checkNotNull(sparkHome, "sparkHome");
    childEnv.put(ENV_SPARK_HOME, sparkHome);
    return this;
  }

  /**
   * Set a custom properties file with Spark configuration for the application.
   *
   * @param path Path to custom properties file to use.
   * @return This launcher.
   */
  public SparkLauncher setPropertiesFile(String path) {
    checkNotNull(path, "path");
    return this;
  }

  /**
   * Set a single configuration value for the application.
   *
   * @param key Configuration key.
   * @param value The value to use.
   * @return This launcher.
   */
  public SparkLauncher setConf(String key, String value) {
    checkNotNull(key, "key");
    checkNotNull(value, "value");
    checkArgument(key.startsWith("spark."), "'key' must start with 'spark.'");
    conf.put(key, value);
    return this;
  }

  /**
   * Set the application name.
   *
   * @param appName Application name.
   * @return This launcher.
   */
  public SparkLauncher setAppName(String appName) {
    checkNotNull(appName, "appName");
    this.appName = appName;
    return this;
  }

  /**
   * Set the Spark master for the application.
   *
   * @param master Spark master.
   * @return This launcher.
   */
  public SparkLauncher setMaster(String master) {
    checkNotNull(master, "master");
    this.master = master;
    return this;
  }

  /**
   * Set the deploy mode for the application.
   *
   * @param mode Deploy mode.
   * @return This launcher.
   */
  public SparkLauncher setDeployMode(String mode) {
    checkNotNull(mode, "mode");
    this.deployMode = mode;
    return this;
  }

  /**
   * Set the main application resource. This should be the location of a jar file for Scala/Java
   * applications, or a python script for PySpark applications.
   *
   * @param resource Path to the main application resource.
   * @return This launcher.
   */
  public SparkLauncher setAppResource(String resource) {
    checkNotNull(resource, "resource");
    this.appResource = resource;
    return this;
  }

  /**
   * Sets the application class name for Java/Scala applications.
   *
   * @param mainClass Application's main class.
   * @return This launcher.
   */
  public SparkLauncher setMainClass(String mainClass) {
    checkNotNull(mainClass, "mainClass");
    this.mainClass = mainClass;
    return this;
  }

  /**
   * Adds command line arguments for the application.
   *
   * @param args Arguments to pass to the application's main class.
   * @return This launcher.
   */
  public SparkLauncher addAppArgs(String... args) {
    for (String arg : args) {
      checkNotNull(arg, "arg");
      appArgs.add(arg);
    }
    return this;
  }

  /**
   * Adds a jar file to be submitted with the application.
   *
   * @param jar Path to the jar file.
   * @return This launcher.
   */
  public SparkLauncher addJar(String jar) {
    checkNotNull(jar, "jar");
    jars.add(jar);
    return this;
  }

  /**
   * Adds a file to be submitted with the application.
   *
   * @param file Path to the file.
   * @return This launcher.
   */
  public SparkLauncher addFile(String file) {
    checkNotNull(file, "file");
    files.add(file);
    return this;
  }

  /**
   * Adds a python file / zip / egg to be submitted with the application.
   *
   * @param file Path to the file.
   * @return This launcher.
   */
  public SparkLauncher addPyFile(String file) {
    checkNotNull(file, "file");
    pyFiles.add(file);
    return this;
  }

  /**
   * Enables verbose reporting for SparkSubmit.
   *
   * @param verbose Whether to enable verbose output.
   * @return This launcher.
   */
  public SparkLauncher setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  /**
   * Launches a sub-process that will start the configured Spark application.
   *
   * @return A process handle for the Spark app.
   */
  public Process launch() throws IOException {
    Map<String, String> procEnv = new HashMap<String, String>(childEnv);
    List<String> cmd = buildSparkSubmitCommand(procEnv);
    ProcessBuilder pb = new ProcessBuilder(cmd.toArray(new String[cmd.size()]));
    for (Map.Entry<String, String> e : procEnv.entrySet()) {
      pb.environment().put(e.getKey(), e.getValue());
    }
    return pb.start();
  }

  List<String> buildJavaCommand(String extraClassPath) throws IOException {
    List<String> cmd = new ArrayList<String>();
    if (javaHome == null) {
      cmd.add(join(File.separator, System.getProperty("java.home"), "bin", "java"));
    } else {
      cmd.add(join(File.separator, javaHome, "bin", "java"));
    }

    // Load extra JAVA_OPTS from conf/java-opts, if it exists.
    File javaOpts = new File(join(File.separator, getConfDir(), "java-opts"));
    if (javaOpts.isFile()) {
      BufferedReader br = new BufferedReader(new InputStreamReader(
          new FileInputStream(javaOpts), "UTF-8"));
      try {
        String line;
        while ((line = br.readLine()) != null) {
          addOptionString(cmd, line);
        }
      } finally {
        br.close();
      }
    }

    cmd.add("-cp");
    cmd.add(join(File.pathSeparator, buildClassPath(extraClassPath)));
    return cmd;
  }

  /**
   * Adds the default perm gen size option for Spark if the VM requires it and the user hasn't
   * set it.
   */
  void addPermGenSizeOpt(List<String> cmd) {
    // Don't set MaxPermSize for Java 8 and later.
    String[] version = System.getProperty("java.version").split("\\.");
    if (Integer.parseInt(version[0]) > 1 || Integer.parseInt(version[1]) > 7) {
      return;
    }

    for (String arg : cmd) {
      if (arg.startsWith("-XX:MaxPermSize=")) {
        return;
      }
    }

    cmd.add("-XX:MaxPermSize=128m");
  }

  void addOptionString(List<String> cmd, String options) {
    if (!isEmpty(options)) {
      for (String opt : parseOptionString(options)) {
        cmd.add(opt);
      }
    }
  }

  /**
   * Builds the classpath for the application. Returns a list with one classpath entry per element;
   * each entry is formatted in the way expected by <i>java.net.URLClassLoader</i> (more
   * specifically, with trailing slashes for directories).
   */
  List<String> buildClassPath(String appClassPath) throws IOException {
    String sparkHome = getSparkHome();
    String scala = getScalaVersion();

    List<String> cp = new ArrayList<String>();
    addToClassPath(cp, getenv("SPARK_CLASSPATH"));
    addToClassPath(cp, appClassPath);

    addToClassPath(cp, getConfDir());

    boolean prependClasses = !isEmpty(getenv("SPARK_PREPEND_CLASSES"));
    boolean isTesting = "1".equals(getenv("SPARK_TESTING"));
    if (prependClasses || isTesting) {
      List<String> projects = Arrays.asList("core", "repl", "mllib", "bagel", "graphx",
        "streaming", "tools", "sql/catalyst", "sql/core", "sql/hive", "sql/hive-thriftserver",
        "yarn", "launcher");
      if (prependClasses) {
        System.err.println(
          "NOTE: SPARK_PREPEND_CLASSES is set, placing locally compiled Spark classes ahead of " +
          "assembly.");
        for (String project : projects) {
          addToClassPath(cp, String.format("%s/%s/target/scala-%s/classes", sparkHome, project,
            scala));
        }
      }
      if (isTesting) {
        for (String project : projects) {
          addToClassPath(cp, String.format("%s/%s/target/scala-%s/test-classes", sparkHome,
            project, scala));
        }
      }

      // Add this path to include jars that are shaded in the final deliverable created during
      // the maven build. These jars are copied to this directory during the build.
      addToClassPath(cp, String.format("%s/core/target/jars/*", sparkHome));
    }

    String assembly = findAssembly(scala);
    addToClassPath(cp, assembly);

    // When Hive support is needed, Datanucleus jars must be included on the classpath. Datanucleus
    // jars do not work if only included in the uber jar as plugin.xml metadata is lost. Both sbt
    // and maven will populate "lib_managed/jars/" with the datanucleus jars when Spark is built
    // with Hive, so first check if the datanucleus jars exist, and then ensure the current Spark
    // assembly is built for Hive, before actually populating the CLASSPATH with the jars.
    //
    // This block also serves as a check for SPARK-1703, when the assembly jar is built with
    // Java 7 and ends up with too many files, causing issues with other JDK versions.
    boolean needsDataNucleus = false;
    JarFile assemblyJar = null;
    try {
      assemblyJar = new JarFile(assembly);
      needsDataNucleus = assemblyJar.getEntry("org/apache/hadoop/hive/ql/exec/") != null;
    } catch (IOException ioe) {
      if (ioe.getMessage().indexOf("invalid CEN header") > 0) {
        System.err.println(
          "Loading Spark jar failed.\n" +
          "This is likely because Spark was compiled with Java 7 and run\n" +
          "with Java 6 (see SPARK-1703). Please use Java 7 to run Spark\n" +
          "or build Spark with Java 6.");
        System.exit(1);
      } else {
        throw ioe;
      }
    } finally {
      if (assemblyJar != null) {
        try {
          assemblyJar.close();
        } catch (IOException e) {
          // Ignore.
        }
      }
    }

    if (needsDataNucleus) {
      System.err.println("Spark assembly has been built with Hive, including Datanucleus jars " +
        "in classpath.");
      File libdir;
      if (new File(sparkHome, "RELEASE").isFile()) {
        libdir = new File(sparkHome, "lib");
      } else {
        libdir = new File(sparkHome, "lib_managed/jars");
      }

      checkState(libdir.isDirectory(), "Library directory '%s' does not exist.",
        libdir.getAbsolutePath());
      for (File jar : libdir.listFiles()) {
        if (jar.getName().startsWith("datanucleus-")) {
          addToClassPath(cp, jar.getAbsolutePath());
        }
      }
    }

    addToClassPath(cp, getenv("HADOOP_CONF_DIR"));
    addToClassPath(cp, getenv("YARN_CONF_DIR"));
    addToClassPath(cp, getenv("SPARK_DIST_CLASSPATH"));
    return cp;
  }

  /**
   * Adds entries to the classpath.
   *
   * @param cp List where to appended the new classpath entries.
   * @param entries New classpath entries (separated by File.pathSeparator).
   */
  private void addToClassPath(List<String> cp, String entries) {
    if (isEmpty(entries)) {
      return;
    }
    String[] split = entries.split(Pattern.quote(File.pathSeparator));
    for (String entry : split) {
      if (!isEmpty(entry)) {
        if (new File(entry).isDirectory() && !entry.endsWith(File.separator)) {
          entry += File.separator;
        }
        cp.add(entry);
      }
    }
  }

  String getScalaVersion() {
    String scala = getenv("SPARK_SCALA_VERSION");
    if (scala != null) {
      return scala;
    }

    String sparkHome = getSparkHome();
    File scala210 = new File(sparkHome, "assembly/target/scala-2.10");
    File scala211 = new File(sparkHome, "assembly/target/scala-2.11");
    if (scala210.isDirectory() && scala211.isDirectory()) {
      checkState(false,
        "Presence of build for both scala versions (2.10 and 2.11) detected.\n" +
        "Either clean one of them or set SPARK_SCALA_VERSION in your environment.");
    } else if (scala210.isDirectory()) {
      return "2.10";
    } else {
      checkState(scala211.isDirectory(), "Cannot find any assembly build directories.");
      return "2.11";
    }

    throw new IllegalStateException("Should not reach here.");
  }

  SparkLauncher addSparkArgs(String... args) {
    for (String arg : args) {
      sparkArgs.add(arg);
    }
    return this;
  }

  // Visible for testing.
  List<String> buildSparkSubmitArgs() {
    List<String> args = new ArrayList<String>();

    if (verbose) {
      args.add("--verbose");
    }

    if (master != null) {
      args.add("--master");
      args.add(master);
    }

    if (deployMode != null) {
      args.add("--deploy-mode");
      args.add(deployMode);
    }

    if (appName != null) {
      args.add("--name");
      args.add(appName);
    }

    for (Map.Entry<String, String> e : conf.entrySet()) {
      args.add("--conf");
      args.add(String.format("%s=%s", e.getKey(), e.getValue()));
    }

    if (propertiesFile != null) {
      args.add("--properties-file");
      args.add(propertiesFile);
    }

    if (!jars.isEmpty()) {
      args.add("--jars");
      args.add(join(",", jars));
    }

    if (!files.isEmpty()) {
      args.add("--files");
      args.add(join(",", files));
    }

    if (!pyFiles.isEmpty()) {
      args.add("--py-files");
      args.add(join(",", pyFiles));
    }

    if (mainClass != null) {
      args.add("--class");
      args.add(mainClass);
    }

    args.addAll(sparkArgs);
    if (appResource != null) {
      args.add(appResource);
    }
    args.addAll(appArgs);

    return args;
  }

  List<String> buildSparkSubmitCommand(Map<String, String> env) throws IOException {
    // Load the properties file and check whether spark-submit will be running the app's driver
    // or just launching a cluster app. When running the driver, the JVM's argument will be
    // modified to cover the driver's configuration.
    Properties props = loadPropertiesFile();
    boolean isClientMode = isClientMode(props);
    String extraClassPath = isClientMode ? find(DRIVER_EXTRA_CLASSPATH, conf, props) : null;

    List<String> cmd = buildJavaCommand(extraClassPath);
    addOptionString(cmd, System.getenv("SPARK_SUBMIT_OPTS"));
    addOptionString(cmd, System.getenv("SPARK_JAVA_OPTS"));

    if (isClientMode) {
      // Figuring out where the memory value come from is a little tricky due to precedence.
      // Precedence is observed in the following order:
      // - explicit configuration (setConf()), which also covers --driver-memory cli argument.
      // - properties file.
      // - SPARK_DRIVER_MEMORY env variable
      // - SPARK_MEM env variable
      // - default value (512m)
      String memory = firstNonEmpty(find(DRIVER_MEMORY, conf, props),
        System.getenv("SPARK_DRIVER_MEMORY"), System.getenv("SPARK_MEM"), DEFAULT_MEM);
      cmd.add("-Xms" + memory);
      cmd.add("-Xmx" + memory);
      addOptionString(cmd, find(DRIVER_EXTRA_JAVA_OPTIONS, conf, props));
      mergeEnvPathList(env, getLibPathEnvName(), find(DRIVER_EXTRA_LIBRARY_PATH, conf, props));
    }

    addPermGenSizeOpt(cmd);
    cmd.add("org.apache.spark.deploy.SparkSubmit");
    cmd.addAll(buildSparkSubmitArgs());
    return cmd;
  }

  String getSparkHome() {
    String path = getenv(ENV_SPARK_HOME);
    checkState(path != null,
      "Spark home not found; set it explicitly or use the SPARK_HOME environment variable.");
    return path;
  }

  /**
   * Loads the configuration file for the application, if it exists. This is either the
   * user-specified properties file, or the spark-defaults.conf file under the Spark configuration
   * directory.
   */
  Properties loadPropertiesFile() throws IOException {
    Properties props = new Properties();
    File propsFile;
    if (propertiesFile != null) {
      propsFile = new File(propertiesFile);
      checkArgument(propsFile.isFile(), "Invalid properties file '%s'.", propertiesFile);
    } else {
      propsFile = new File(getConfDir(), DEFAULT_PROPERTIES_FILE);
    }

    if (propsFile.isFile()) {
      FileInputStream fd = null;
      try {
        fd = new FileInputStream(propsFile);
        props.load(new InputStreamReader(fd, "UTF-8"));
      } finally {
        if (fd != null) {
          try {
            fd.close();
          } catch (IOException e) {
            // Ignore.
          }
        }
      }
    }

    return props;
  }

  String getenv(String key) {
    return firstNonEmpty(childEnv.get(key), System.getenv(key));
  }

  private boolean isClientMode(Properties userProps) {
    String userMaster = firstNonEmpty(master, (String) userProps.get(SPARK_MASTER));
    return userMaster == null ||
      "client".equals(deployMode) ||
      "yarn-client".equals(userMaster) ||
      (deployMode == null && !userMaster.startsWith("yarn-"));
  }

  private String findAssembly(String scalaVersion) {
    String sparkHome = getSparkHome();
    File libdir;
    if (new File(sparkHome, "RELEASE").isFile()) {
      libdir = new File(sparkHome, "lib");
      checkState(libdir.isDirectory(), "Library directory '%s' does not exist.",
          libdir.getAbsolutePath());
    } else {
      libdir = new File(sparkHome, String.format("assembly/target/scala-%s", scalaVersion));
    }

    final Pattern re = Pattern.compile("spark-assembly.*hadoop.*\\.jar");
    FileFilter filter = new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.isFile() && re.matcher(file.getName()).matches();
      }
    };
    File[] assemblies = libdir.listFiles(filter);
    checkState(assemblies != null && assemblies.length > 0, "No assemblies found in '%s'.", libdir);
    checkState(assemblies.length == 1, "Multiple assemblies found in '%s'.", libdir);
    return assemblies[0].getAbsolutePath();
  }

  private String getConfDir() {
    String confDir = getenv("SPARK_CONF_DIR");
    return confDir != null ? confDir : join(File.separator, getSparkHome(), "conf");
  }

}
