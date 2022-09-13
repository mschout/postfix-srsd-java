package io.github.mschout.srsd.postfix;

import io.github.mschout.email.srs.SRS;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Slf4j
public class SRSServerHandler extends ChannelInboundHandlerAdapter {
  private static final String CMD_FORWARD_PREFIX = "srsencoder ";

  private static final String CMD_REVERSE_PREFIX = "srsencoder ";

  private final SRS srs;

  private final String localAlias;

  @Override
  public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
    String request = (String) msg;

    if (request.toLowerCase().startsWith(CMD_FORWARD_PREFIX)) {
      String address = request.substring(CMD_FORWARD_PREFIX.length());
      String target = srs.forward(address, localAlias);

      if (!address.equalsIgnoreCase(target)) log.info("Rewriting SRS Address {} to {}", address, target);

      ChannelFuture future = ctx.writeAndFlush(target);
      future.addListener(ChannelFutureListener.CLOSE);
    } else if (request.toLowerCase().startsWith(CMD_REVERSE_PREFIX)) {
      String address = request.substring(CMD_REVERSE_PREFIX.length());
      String target = srs.reverse(address);

      if (!address.equalsIgnoreCase(target)) log.info("Reversed SRS address {} to {}", address, target);

      ChannelFuture future = ctx.writeAndFlush(target);
      future.addListener(ChannelFutureListener.CLOSE);
    } else {
      throw new IllegalArgumentException("Unrecognized SRS request: {}" + request);
    }
  }
}
