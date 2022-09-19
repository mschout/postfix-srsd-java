package io.github.mschout.srsd.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetStringDecoder extends ByteToMessageDecoder {
  private static final int MAX_FRAME_SIZE = 8192;

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    int readableBytes = in.readableBytes();
    log.debug("ReadableBytes: " + readableBytes);

    in.markReaderIndex();

    if (readableBytes > MAX_FRAME_SIZE) throw new TooLongFrameException("Frame too big " + readableBytes + " > " + MAX_FRAME_SIZE);

    // Minimum netstring must be at least 3 chars long: "0:,"
    if (readableBytes < 3) return;

    final int sizeLength = in.bytesBefore((byte) ':');

    // if we haven't received the size delimiter yet return
    if (sizeLength < 0) return;

    final int dataLength = readLength(in, sizeLength);

    if (dataLength > MAX_FRAME_SIZE) throw new TooLongFrameException("Frame too big " + dataLength + " > " + MAX_FRAME_SIZE);

    // NOTE: if we bail out early from here on out, we must call .resetReaderIndex()
    if (readableBytes < dataLength + 2) { // ":" plus netstring value plus ","
      log.debug("We have not yet received the complete netstring");
      in.resetReaderIndex();
      return;
    }

    log.debug("Netstring Length is: {}", dataLength);

    // we should have the entire payload now

    in.skipBytes(1); // skip the ':'

    String value = in.readCharSequence(dataLength, StandardCharsets.UTF_8).toString();
    log.debug("Read netstring {}", value);

    in.skipBytes(1); // skip the ','

    out.add(value);
  }

  private int readLength(ByteBuf buffer, final int length) {
    byte[] data = new byte[length];

    buffer.readBytes(data);

    return Integer.parseInt(new String(data, StandardCharsets.UTF_8));
  }
}
