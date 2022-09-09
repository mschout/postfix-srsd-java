package io.github.mschout.srsd.postfix;

import io.github.mschout.email.srs.SRS;
import io.github.mschout.srsd.protocol.NetStringDecoder;
import io.github.mschout.srsd.protocol.NetStringEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class SRSServer {
  private final int port;

  public void run() throws InterruptedException {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    // TODO - these should be configurable
    var srs = SRS.guardedSRS(List.of("secrets"));
    var localAlias = "example.com";

    try {
      ServerBootstrap bootstrap = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(
          new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) {
              ch.pipeline().addLast(new NetStringDecoder(), new NetStringEncoder(), new SRSServerHandler(srs, localAlias));
            }
          }
        )
        .option(ChannelOption.SO_BACKLOG, 128);
      //.childOption(ChannelOption.SO_KEEPALIVE, true); ???

      bootstrap.bind(port).sync().channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
