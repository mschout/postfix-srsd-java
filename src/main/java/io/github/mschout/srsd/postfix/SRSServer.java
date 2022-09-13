package io.github.mschout.srsd.postfix;

import io.github.mschout.email.srs.SRS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Builder
public class SRSServer {
  private final String socketPath;

  private final List<String> secrets;

  private final String localAlias;

  public void run() throws InterruptedException {
    // Set up Netty to use SlF4j
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

    var srs = SRS.guardedSRS(secrets);

    ServerBootstrap bootstrap = SRSServerFactory.createServer(srs, localAlias).option(ChannelOption.SO_BACKLOG, 128);

    //.childOption(ChannelOption.SO_KEEPALIVE, true); ???

    try {
      bootstrap.bind(new DomainSocketAddress(socketPath)).sync().channel().closeFuture().sync();
    } finally {
      if (bootstrap != null) {
        bootstrap.config().group().shutdownGracefully();
        bootstrap.config().childGroup().shutdownGracefully();
      }
    }
  }
}
