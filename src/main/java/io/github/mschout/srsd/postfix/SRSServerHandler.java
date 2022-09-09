package io.github.mschout.srsd.postfix;

import io.github.mschout.email.srs.SRS;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SRSServerHandler extends ChannelInboundHandlerAdapter {
  private final SRS srs;

  private final String localAlias;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    String request = (String) msg;

    ChannelFuture future = ctx.writeAndFlush(srs.forward(request, localAlias));

    future.addListener(ChannelFutureListener.CLOSE);

    System.out.println("Request: " + request);
  }
}
