package io.github.mschout.srsd.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class NetStringDecoder extends ReplayingDecoder<Void> {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    System.err.println("Readable bytes " + in.readableBytes());

    // Minimum netstring must be at least 3 chars long: "0:,"
    if (in.readableBytes() < 3) return;

    final int sizeLength = in.bytesBefore((byte) ':');

    System.err.println("Size Length: " + sizeLength);

    // if we haven't received the size delimiter yet return
    if (sizeLength < 0) return;

    final int dataLength = readLength(in, sizeLength);

    System.err.println("Size: " + dataLength);

    // read the ':'
    in.readBytes(1);

    String value = in.readCharSequence(dataLength, StandardCharsets.UTF_8).toString();

    System.err.println("Value: " + value);

    in.readBytes(1);

    out.add(value);
  }

  private int readLength(ByteBuf buffer, final int length) {
    byte[] data = new byte[length];

    buffer.readBytes(data);

    return Integer.parseInt(new String(data, StandardCharsets.UTF_8));
  }
}
