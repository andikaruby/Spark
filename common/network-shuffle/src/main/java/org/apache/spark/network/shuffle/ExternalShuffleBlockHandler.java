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

package org.apache.spark.network.shuffle;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.spark.network.buffer.ManagedBuffer;
import org.apache.spark.network.client.RpcResponseCallback;
import org.apache.spark.network.client.TransportClient;
import org.apache.spark.network.server.OneForOneStreamManager;
import org.apache.spark.network.server.RpcHandler;
import org.apache.spark.network.server.StreamManager;
import org.apache.spark.network.shuffle.ExternalShuffleBlockResolver.AppExecId;
import org.apache.spark.network.shuffle.protocol.*;
import org.apache.spark.network.util.NettyUtils;
import org.apache.spark.network.util.TransportConf;


/**
 * RPC Handler for a server which can serve shuffle blocks from outside of an Executor process.
 *
 * Handles registering executors and opening shuffle blocks from them. Shuffle blocks are registered
 * with the "one-for-one" strategy, meaning each Transport-layer Chunk is equivalent to one Spark-
 * level shuffle block.
 */
public class ExternalShuffleBlockHandler extends RpcHandler {
  private final Logger logger = LoggerFactory.getLogger(ExternalShuffleBlockHandler.class);

  @VisibleForTesting
  final ExternalShuffleBlockResolver blockManager;
  private final OneForOneStreamManager streamManager;
  // Shuffle service metrics setup
  private final ShuffleMetrics metrics;
  private final Timer timeDelayForOpenBlockRequest;
  private final Timer timeDelayForRegisterExecutorRequest;
  private final Meter transferBlockRate;

  public ExternalShuffleBlockHandler(TransportConf conf, File registeredExecutorFile)
    throws IOException {
    this(new OneForOneStreamManager(),
      new ExternalShuffleBlockResolver(conf, registeredExecutorFile));
  }

  /** Enables mocking out the StreamManager and BlockManager. */
  @VisibleForTesting
  public ExternalShuffleBlockHandler(
      OneForOneStreamManager streamManager,
      ExternalShuffleBlockResolver blockManager) {
    this.metrics = new ShuffleMetrics();
    this.timeDelayForOpenBlockRequest = metrics.timeDelayForOpenBlockRequest;
    this.timeDelayForRegisterExecutorRequest = metrics.timeDelayForRegisterExecutorRequest;
    this.transferBlockRate = metrics.transferBlockRate;
    this.streamManager = streamManager;
    this.blockManager = blockManager;
  }

  @Override
  public void receive(TransportClient client, ByteBuffer message, RpcResponseCallback callback) {
    BlockTransferMessage msgObj = BlockTransferMessage.Decoder.fromByteBuffer(message);
    handleMessage(msgObj, client, callback);
  }

  protected void handleMessage(
      BlockTransferMessage msgObj,
      TransportClient client,
      RpcResponseCallback callback) {
    if (msgObj instanceof OpenBlocks) {
      // Reset transferred block size metrics as zero
      final Timer.Context responseDelayContext = timeDelayForOpenBlockRequest.time();
      OpenBlocks msg = (OpenBlocks) msgObj;
      checkAuth(client, msg.appId);

      List<ManagedBuffer> blocks = Lists.newArrayList();
      long totalBlockSize = 0;
      for (String blockId : msg.blockIds) {
        final ManagedBuffer block = blockManager.getBlockData(msg.appId, msg.execId, blockId);
        totalBlockSize += block.size();
        blocks.add(block);
      }
      long streamId = streamManager.registerStream(client.getClientId(), blocks.iterator());
      logger.trace("Registered streamId {} with {} buffers for client {} from host {}",
          streamId,
          msg.blockIds.length,
          client.getClientId(),
          NettyUtils.getRemoteAddress(client.getChannel()));
      callback.onSuccess(new StreamHandle(streamId, msg.blockIds.length).toByteBuffer());
      transferBlockRate.mark(totalBlockSize / 1024 / 1024);
      responseDelayContext.stop();

    } else if (msgObj instanceof RegisterExecutor) {
      final Timer.Context responseDelayContext = timeDelayForRegisterExecutorRequest.time();
      RegisterExecutor msg = (RegisterExecutor) msgObj;
      checkAuth(client, msg.appId);
      blockManager.registerExecutor(msg.appId, msg.execId, msg.executorInfo);
      callback.onSuccess(ByteBuffer.wrap(new byte[0]));
      responseDelayContext.stop();

    } else {
      throw new UnsupportedOperationException("Unexpected message: " + msgObj);
    }
  }

  public MetricSet getAllMetrics() {
    return metrics;
  }

  public long getRegisteredExecutorsSize() {
    return blockManager.getRegisteredExecutorsSize();
  }

  public long getTotalShuffleRequests() {
    return timeDelayForOpenBlockRequest.getCount() + timeDelayForOpenBlockRequest.getCount();
  }

  @Override
  public StreamManager getStreamManager() {
    return streamManager;
  }

  /**
   * Removes an application (once it has been terminated), and optionally will clean up any
   * local directories associated with the executors of that application in a separate thread.
   */
  public void applicationRemoved(String appId, boolean cleanupLocalDirs) {
    blockManager.applicationRemoved(appId, cleanupLocalDirs);
  }

  /**
   * Register an (application, executor) with the given shuffle info.
   *
   * The "re-" is meant to highlight the intended use of this method -- when this service is
   * restarted, this is used to restore the state of executors from before the restart.  Normal
   * registration will happen via a message handled in receive()
   *
   * @param appExecId
   * @param executorInfo
   */
  public void reregisterExecutor(AppExecId appExecId, ExecutorShuffleInfo executorInfo) {
    blockManager.registerExecutor(appExecId.appId, appExecId.execId, executorInfo);
  }

  public void close() {
    blockManager.close();
  }

  private void checkAuth(TransportClient client, String appId) {
    if (client.getClientId() != null && !client.getClientId().equals(appId)) {
      throw new SecurityException(String.format(
        "Client for %s not authorized for application %s.", client.getClientId(), appId));
    }
  }

  /**
   * A simple class to wrap all shuffle service wrapper metrics
   */
  private class ShuffleMetrics implements MetricSet {
    private final Map<String, Metric> allMetrics;
    private final Timer timeDelayForOpenBlockRequest = new Timer();
    private final Timer timeDelayForRegisterExecutorRequest = new Timer();
    private final Meter transferBlockRate = new Meter();

    private ShuffleMetrics() {
      allMetrics = new HashMap<>();
      allMetrics.put("timeDelayForOpenBlockRequest", timeDelayForOpenBlockRequest);
      allMetrics.put("timeDelayForRegisterExecutorRequest", timeDelayForRegisterExecutorRequest);
      allMetrics.put("transferBlockRate", transferBlockRate);
    }

    @Override
    public Map<String, Metric> getMetrics() {
      return allMetrics;
    }
  }

}
