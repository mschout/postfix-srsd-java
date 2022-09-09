package io.github.mschout.srsd.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.nio.charset.StandardCharsets;

public class NetStringEncoder extends MessageToByteEncoder<String> {

  @Override
  protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
    String data = String.format("%d:%s,", msg.length(), msg);
    out.writeBytes(data.getBytes(StandardCharsets.UTF_8));
  }
}
