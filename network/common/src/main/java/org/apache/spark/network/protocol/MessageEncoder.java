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

package org.apache.spark.network.protocol;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;

import com.google.common.primitives.Ints;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encoder used by the server side to encode server-to-client responses.
 * This encoder is stateless so it is safe to be shared by multiple threads.
 */
@ChannelHandler.Sharable
public final class MessageEncoder extends MessageToMessageEncoder<Message> {

  private final Logger logger = LoggerFactory.getLogger(MessageEncoder.class);

  /***
   * Encodes a Message by invoking its encode() method. For non-data messages, we will add one
   * ByteBuf to 'out' containing the total frame length, the message type, and the message itself.
   * In the case of a ChunkFetchSuccess, we will also add the ManagedBuffer corresponding to the
   * data to 'out', in order to enable zero-copy transfer.
   */
  @Override
  public void encode(ChannelHandlerContext ctx, Message in, List<Object> out) {
    Object body = null;
    long bodyLength = 0;

    // Only ChunkFetchSuccesses have data besides the header.
    // The body is used in order to enable zero-copy transfer for the payload.
    if (in instanceof ChunkFetchSuccess) {
      ChunkFetchSuccess resp = (ChunkFetchSuccess) in;
      try {
        bodyLength = resp.buffer.size();
        body = resp.buffer.convertToNetty();
      } catch (Exception e) {
        // Re-encode this message as BlockFetchFailure.
        logger.error(String.format("Error opening block %s for client %s",
          resp.streamChunkId, ctx.channel().remoteAddress()), e);
        encode(ctx, new ChunkFetchFailure(resp.streamChunkId, e.getMessage()), out);
        return;
      }
    }

    Message.Type msgType = in.type();
    // All messages have the frame length, message type, and message itself.
    int headerLength = 8 + msgType.encodedLength() + in.encodedLength();
    long frameLength = headerLength + bodyLength;
    ByteBuf header = ctx.alloc().heapBuffer(headerLength);
    header.writeLong(frameLength);
    msgType.encode(header);
    in.encode(header);
    assert header.writableBytes() == 0;

    if (body != null && bodyLength > 0) {
      out.add(new MessageWithHeader(header, headerLength, body, bodyLength));
    } else {
      out.add(header);
    }
  }

  /**
   * A wrapper message that holds two separate pieces (a header and a body) to avoid
   * copying the body's content.
   */
  private static class MessageWithHeader extends AbstractReferenceCounted implements FileRegion {

    private final ByteBuf header;
    private final int headerLength;
    private final Object body;
    private final long bodyLength;
    private int bytesTransferred;

    MessageWithHeader(ByteBuf header, int headerLength, Object body, long bodyLength) {
      this.header = header;
      this.headerLength = headerLength;
      this.body = body;
      this.bodyLength = bodyLength;
    }

    @Override
    public long count() {
      return headerLength + bodyLength;
    }

    @Override
    public long position() {
      return 0;
    }

    @Override
    public long transfered() {
      long total = bytesTransferred;
      if (body instanceof FileRegion) {
        total += ((FileRegion)body).transfered();
      }
      return total;
    }

    @Override
    public long transferTo(WritableByteChannel target, long position) throws IOException {
      Preconditions.checkArgument(position >= 0 && position < count(), "Invalid position.");

      if (position < headerLength && position >= 0) {
        header.skipBytes(Ints.checkedCast(position));
        int remaining = header.readableBytes();
        target.write(header.nioBuffer());
        bytesTransferred += remaining;
      }

      long bodyPos = position > headerLength ? position - headerLength : 0;
      if (body instanceof FileRegion) {
        ((FileRegion)body).transferTo(target, bodyPos);
      } else if (body instanceof ByteBuf) {
        ByteBuf buf = (ByteBuf) body;
        buf.skipBytes(Ints.checkedCast(bodyPos));
        target.write(buf.nioBuffer());
        bytesTransferred += bodyLength;
      } else {
        throw new IllegalArgumentException(
          String.format("Body type %s not supported!", body.getClass().getName()));
      }

      return transfered();
    }

    @Override
    protected void deallocate() {
      header.release();
      ReferenceCountUtil.release(body);
    }

  }

}
