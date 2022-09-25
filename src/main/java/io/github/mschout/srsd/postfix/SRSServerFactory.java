package io.github.mschout.srsd.postfix;

import io.github.mschout.email.srs.SRS;
import io.github.mschout.netty.codec.netstring.ByteToNetstringDecoder;
import io.github.mschout.netty.codec.netstring.NetstringToByteEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SRSServerFactory {
  private static final int MAX_FRAME_SIZE = 256 * 1024;

  public static ServerBootstrap createUnixSocketServer(SRS srs, String localAlias) {
    ServerBootstrap bootstrap;

    if (Epoll.isAvailable()) {
      bootstrap = createEPollServer(srs, localAlias).option(ChannelOption.SO_BACKLOG, 128);
    } else if (KQueue.isAvailable()) {
      bootstrap = createKQueueServer(srs, localAlias);
    } else {
      throw new UnsupportedOperationException("Neither KQueue nor Epoll is available on this platform");
    }

    return bootstrap;
  }

  public static ServerBootstrap createTCPSocketServer(SRS srs, String localAlias) {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    ServerBootstrap bootstrap = new ServerBootstrap();

    bootstrap
      .group(bossGroup, workerGroup)
      .channel(NioServerSocketChannel.class)
      .childHandler(buildChildHandler(SocketChannel.class, srs, localAlias));

    return bootstrap;
  }

  private static ServerBootstrap createEPollServer(SRS srs, String localAlias) {
    log.info("Creating Epoll based SRS Server");

    EventLoopGroup bossGroup = new EpollEventLoopGroup();
    EventLoopGroup workerGroup = new EpollEventLoopGroup();

    return new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(EpollServerDomainSocketChannel.class)
      .childHandler(buildChildHandler(EpollDomainSocketChannel.class, srs, localAlias));
  }

  private static ServerBootstrap createKQueueServer(SRS srs, String localAlias) {
    log.info("Creating KQueue based SRS Server");

    EventLoopGroup bossGroup = new KQueueEventLoopGroup();
    EventLoopGroup workerGroup = new KQueueEventLoopGroup();

    return new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(KQueueServerDomainSocketChannel.class)
      .childHandler(buildChildHandler(KQueueDomainSocketChannel.class, srs, localAlias));
  }

  private static <T extends Channel> ChannelInitializer<T> buildChildHandler(Class<T> ignoredClazz, SRS srs, String localAlias) {
    return new ChannelInitializer<>() {

      @Override
      protected void initChannel(@NotNull T channel) {
        channel
          .pipeline()
          .addLast(
            new ByteToNetstringDecoder(MAX_FRAME_SIZE, StandardCharsets.UTF_8),
            new NetstringToByteEncoder(StandardCharsets.UTF_8),
            new SRSServerHandler(srs, localAlias)
          );
      }
    };
  }
}
