package io.github.mschout.srsd.postfix;

import io.github.mschout.email.srs.SRS;
import io.github.mschout.srsd.postfix.options.SocketOptions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Builder
@Slf4j
public record SRSServer(SocketOptions socketOptions, List<String> secrets, String localAlias) {
  public void run() throws InterruptedException {
    // Set up Netty to use SlF4j
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

    var srs = SRS.guardedSRS(secrets);

    ServerBootstrap bootstrap = null;

    try {
      ChannelFuture channelFuture;

      if (socketOptions.getSocketType() == SocketOptions.SocketType.UNIX) {
        bootstrap = SRSServerFactory.createUnixSocketServer(srs, localAlias);

        channelFuture = bootstrap.bind(new DomainSocketAddress(socketOptions.getSocketPath()));
      } else { // TCP socket
        bootstrap = SRSServerFactory.createTCPSocketServer(srs, localAlias);

        var tcpSocketOptions = socketOptions.getTcpSocket();
        channelFuture = bootstrap.bind(tcpSocketOptions.getHostName(), tcpSocketOptions.getPort());
      }

      channelFuture.sync().channel().closeFuture().sync();
    } finally {
      log.info("Server shutting down gracefully");
      shutdownBootstrap(bootstrap);
    }
  }

  private void shutdownBootstrap(@Nullable ServerBootstrap bootstrap) {
    if (bootstrap == null) {
      return;
    }

    try (var group = bootstrap.config().group();
        var childGroup = bootstrap.config().childGroup()) {
      group.shutdownGracefully();
      childGroup.shutdownGracefully();
    } catch (Exception e) {
      log.error("Error shutting down server bootstrap", e);
    }
  }
}
