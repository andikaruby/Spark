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

package org.apache.spark.api.python

import java.io.{DataInputStream, File, InputStream, IOException, OutputStreamWriter}
import java.net.{InetAddress, ServerSocket, Socket, SocketException}

import scala.collection.JavaConversions._
import scala.io.Source

import org.apache.spark._

private[spark] class PythonWorkerFactory(pythonExec: String, envVars: Map[String, String])
  extends Logging {

  // Because forking processes from Java is expensive, we prefer to launch a single Python daemon
  // (pyspark/daemon.py) and tell it to fork new workers for our tasks. This daemon currently
  // only works on UNIX-based systems now because it uses signals for child management, so we can
  // also fall back to launching workers (pyspark/worker.py) directly.
  val useDaemon = !System.getProperty("os.name").startsWith("Windows")

  var daemon: Process = null
  val daemonHost = InetAddress.getByAddress(Array(127, 0, 0, 1))
  var daemonPort: Int = 0

  val pythonPath = PythonUtils.mergePythonPaths(
    PythonUtils.sparkPythonPath, envVars.getOrElse("PYTHONPATH", ""))

  def create(): Socket = {
    if (useDaemon) {
      createThroughDaemon()
    } else {
      createSimpleWorker()
    }
  }

  /**
   * Connect to a worker launched through pyspark/daemon.py, which forks python processes itself
   * to avoid the high cost of forking from Java. This currently only works on UNIX-based systems.
   */
  private def createThroughDaemon(): Socket = {
    synchronized {
      // Start the daemon if it hasn't been started
      startDaemon()

      // Attempt to connect, restart and retry once if it fails
      try {
        new Socket(daemonHost, daemonPort)
      } catch {
        case exc: SocketException => {
          logWarning("Python daemon unexpectedly quit, attempting to restart")
          stopDaemon()
          startDaemon()
          new Socket(daemonHost, daemonPort)
        }
        case e: Throwable => throw e
      }
    }
  }

  /**
   * Launch a worker by executing worker.py directly and telling it to connect to us.
   */
  private def createSimpleWorker(): Socket = {
    var serverSocket: ServerSocket = null
    try {
      serverSocket = new ServerSocket(0, 1, InetAddress.getByAddress(Array(127, 0, 0, 1)))

      // Create and start the worker
      val pb = new ProcessBuilder(Seq(pythonExec, "-u", "-m", "pyspark.worker"))
      val workerEnv = pb.environment()
      workerEnv.putAll(envVars)
      workerEnv.put("PYTHONPATH", pythonPath)
      val worker = pb.start()

      // Redirect worker stdout and stderr
      redirectWorkerStreams(worker.getInputStream, worker.getErrorStream)

      // Tell the worker our port
      val out = new OutputStreamWriter(worker.getOutputStream)
      out.write(serverSocket.getLocalPort + "\n")
      out.flush()

      // Wait for it to connect to our socket
      serverSocket.setSoTimeout(10000)
      try {
        return serverSocket.accept()
      } catch {
        case e: Exception =>
          throw new SparkException("Python worker did not connect back in time", e)
      }
    } finally {
      if (serverSocket != null) {
        serverSocket.close()
      }
    }
    null
  }

  private def startDaemon() {
    synchronized {
      // Is it already running?
      if (daemon != null) {
        return
      }

      try {
        // Create and start the daemon
        val pb = new ProcessBuilder(Seq(pythonExec, "-u", "-m", "pyspark.daemon"))
        val workerEnv = pb.environment()
        workerEnv.putAll(envVars)
        workerEnv.put("PYTHONPATH", pythonPath)
        daemon = pb.start()

        val in = new DataInputStream(daemon.getInputStream)
        daemonPort = in.readInt()

        // Redirect worker stdout and stderr
        redirectWorkerStreams(in, daemon.getErrorStream)

      } catch {
        case e: Throwable =>
          val stderr = Option(daemon)
            .map { d => Source.fromInputStream(d.getErrorStream).getLines().toSeq }
            .getOrElse(Seq.empty)
          val stderrString = stderr.mkString("  ", "\n  ", "")

          stopDaemon()

          if (!stderr.isEmpty) {
            var errorMessage = "\n"
            errorMessage += "Error from python worker:\n"
            errorMessage += stderrString + "\n"
            errorMessage += "PYTHONPATH was " + pythonpath + "\n"
            errorMessage += e.toString

            // Append error message from python daemon, but keep original stack trace
            val wrappedException = new SparkException(errorMessage)
            wrappedException.setStackTrace(e.getStackTrace)
            throw wrappedException
          } else {
            throw e
          }
      }

      // Important: don't close daemon's stdin (daemon.getOutputStream) so it can correctly
      // detect our disappearance.
    }
  }

  /**
   * Redirect a worker's stdout and stderr to our stderr in the background.
   */
  private def redirectWorkerStreams(stdout: InputStream, stderr: InputStream) {
    try {
      redirectStream("stdout reader for " + pythonExec, stdout)
      redirectStream("stderr reader for " + pythonExec, stderr)
    } catch {
      case e: Throwable =>
        logWarning("Exception in redirecting worker streams")
    }
  }

  /**
   * Redirect the given input stream to our stderr in a separate thread.
   */
  private def redirectStream(threadName: String, in: InputStream) {
    new Thread(threadName) {
      setDaemon(true)
      override def run() {
        scala.util.control.Exception.ignoring(classOf[IOException]) {
          // FIXME: We copy the stream on the level of bytes to avoid encoding problems.
          val buf = new Array[Byte](1024)
          var len = in.read(buf)
          while (len != -1) {
            System.err.write(buf, 0, len)
            len = in.read(buf)
          }
        }
      }
    }.start()
  }

  private def stopDaemon() {
    synchronized {
      // Request shutdown of existing daemon by sending SIGTERM
      if (daemon != null) {
        daemon.destroy()
      }

      daemon = null
      daemonPort = 0
    }
  }

  def stop() {
    stopDaemon()
  }
}
