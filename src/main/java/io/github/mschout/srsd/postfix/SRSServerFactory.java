package io.github.mschout.srsd.postfix;

import io.github.mschout.email.srs.SRS;
import io.github.mschout.srsd.protocol.NetStringDecoder;
import io.github.mschout.srsd.protocol.NetStringEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SRSServerFactory {

  public static ServerBootstrap createServer(SRS srs, String localAlias) {
    if (Epoll.isAvailable()) {
      return createEPollServer(srs, localAlias);
    } else if (KQueue.isAvailable()) {
      return createKQueueServer(srs, localAlias);
    } else {
      throw new UnsupportedOperationException("Neither KQueue nor Epoll is available on this platform");
    }
  }

  public static ServerBootstrap createEPollServer(SRS srs, String localAlias) {
    log.info("Creating Epoll based SRS Server");

    EventLoopGroup bossGroup = new EpollEventLoopGroup();
    EventLoopGroup workerGroup = new EpollEventLoopGroup();

    return new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(EpollServerDomainSocketChannel.class)
      .childHandler(
        new ChannelInitializer<EpollDomainSocketChannel>() {

          @Override
          protected void initChannel(@NotNull EpollDomainSocketChannel ch) {
            ch.pipeline().addLast(new NetStringDecoder(), new NetStringEncoder(), new SRSServerHandler(srs, localAlias));
          }
        }
      );
  }

  public static ServerBootstrap createKQueueServer(SRS srs, String localAlias) {
    log.info("Creating KQueue based SRS Server");

    EventLoopGroup bossGroup = new KQueueEventLoopGroup();
    EventLoopGroup workerGroup = new KQueueEventLoopGroup();

    return new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(KQueueServerDomainSocketChannel.class)
      .childHandler(
        new ChannelInitializer<KQueueDomainSocketChannel>() {

          @Override
          protected void initChannel(@NotNull KQueueDomainSocketChannel ch) {
            ch.pipeline().addLast(new NetStringDecoder(), new NetStringEncoder(), new SRSServerHandler(srs, localAlias));
          }
        }
      );
  }
}
