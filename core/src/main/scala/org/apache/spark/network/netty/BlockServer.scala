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

package org.apache.spark.network.netty

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.{ChannelFuture, ChannelInitializer, ChannelOption}
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.oio.OioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.oio.OioServerSocketChannel
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.util.CharsetUtil

import org.apache.spark.{Logging, SparkConf}
import org.apache.spark.util.Utils

/**
 * Server for serving Spark data blocks. This should be used together with [[BlockFetchingClient]].
 *
 * Protocol for requesting blocks: specify one block id per line.
 *
 * Protocol for sending blocks: for each block,
 */
private[spark]
class BlockServer(conf: SparkConf, pResolver: PathResolver) extends Logging {

  // TODO: Allow random port selection
  val port: Int = conf.getInt("spark.shuffle.io.port", 12345)

  private var bootstrap: ServerBootstrap = _
  private var channelFuture: ChannelFuture = _

  /** Initialize the server. */
  def init(): Unit = {
    bootstrap = new ServerBootstrap
    val bossThreadFactory = Utils.namedThreadFactory("spark-shuffle-server-boss")
    val workerThreadFactory = Utils.namedThreadFactory("spark-shuffle-server-worker")

    def initNio(): Unit = {
      val bossGroup = new NioEventLoopGroup(0, bossThreadFactory)
      val workerGroup = new NioEventLoopGroup(0, workerThreadFactory)
      bootstrap.group(bossGroup, workerGroup).channel(classOf[NioServerSocketChannel])
    }
    def initOio(): Unit = {
      val bossGroup = new OioEventLoopGroup(0, bossThreadFactory)
      val workerGroup = new OioEventLoopGroup(0, workerThreadFactory)
      bootstrap.group(bossGroup, workerGroup).channel(classOf[OioServerSocketChannel])
    }
    def initEpoll(): Unit = {
      val bossGroup = new EpollEventLoopGroup(0, bossThreadFactory)
      val workerGroup = new EpollEventLoopGroup(0, workerThreadFactory)
      bootstrap.group(bossGroup, workerGroup).channel(classOf[EpollServerSocketChannel])
    }

    conf.get("spark.shuffle.io.mode", "auto").toLowerCase match {
      case "nio" => initNio()
      case "oio" => initOio()
      case "epoll" => initEpoll()
      case "auto" =>
        // For auto mode, first try epoll (only available on Linux), then nio.
        try {
          initEpoll()
        } catch {
          case e: Throwable => initNio()
        }
    }

    // Use pooled buffers to reduce temporary buffer allocation
    bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
    bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)

    // Various (advanced) user-configured settings.
    conf.getOption("spark.shuffle.io.backLog").foreach { backLog =>
      bootstrap.option[java.lang.Integer](ChannelOption.SO_BACKLOG, backLog.toInt)
    }
    // Note: the optimal size for receive buffer and send buffer should be
    //  latency * network_bandwidth.
    // Assuming latency = 1ms, network_bandwidth = 10Gbps
    // buffer size should be ~ 1.25MB
    conf.getOption("spark.shuffle.io.receiveBuffer").foreach { receiveBuf =>
      bootstrap.option[java.lang.Integer](ChannelOption.SO_RCVBUF, receiveBuf.toInt)
    }
    conf.getOption("spark.shuffle.io.sendBuffer").foreach { sendBuf =>
      bootstrap.option[java.lang.Integer](ChannelOption.SO_SNDBUF, sendBuf.toInt)
    }

    bootstrap.childHandler(new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel): Unit = {
        ch.pipeline
          .addLast("frameDecoder", new LineBasedFrameDecoder(1024))  // max block id length 1024
          .addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8))

        ch.pipeline
          .addLast("handler", new BlockServerHandler(pResolver))
      }
    })

    channelFuture = bootstrap.bind(new InetSocketAddress(port))
    channelFuture.sync()

    val addr = channelFuture.channel.localAddress.asInstanceOf[InetSocketAddress]
    println("address: " + addr.getAddress + "  port: " + addr.getPort)
  }

  /** Shutdown the server. */
  def stop(): Unit = {
    if (channelFuture != null) {
      channelFuture.channel().close().awaitUninterruptibly()
      channelFuture = null
    }
    if (bootstrap != null && bootstrap.group() != null) {
      bootstrap.group().shutdownGracefully()
    }
    if (bootstrap != null && bootstrap.childGroup() != null) {
      bootstrap.childGroup().shutdownGracefully()
    }
    bootstrap = null
  }
}


object BlockServer {
  def main(args: Array[String]): Unit = {
    new BlockServer(new SparkConf, null).init()
    Thread.sleep(100000)
  }
}
