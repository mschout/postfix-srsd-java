package io.github.mschout.srsd.postfix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.mschout.email.srs.SRS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.unix.DomainSocketAddress;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class SRSServerFactoryTest {
  private static final String LOCAL_ALIAS = "forwarder.example.com";

  private static final String SRS_ADDRESS = "SRS0=HHH=TT=example.com=user@" + LOCAL_ALIAS;

  private SRS srs;

  private ServerBootstrap bootstrap;

  private Channel serverChannel;

  @BeforeEach
  void setUp() {
    srs = mock(SRS.class);
  }

  @AfterEach
  void shutDownServer() throws InterruptedException {
    if (serverChannel != null) serverChannel.close().sync();

    if (bootstrap != null) {
      bootstrap.config().group().shutdownGracefully(0, 0, TimeUnit.SECONDS);
      bootstrap.config().childGroup().shutdownGracefully(0, 0, TimeUnit.SECONDS);
    }
  }

  private static byte[] netstring(String payload) {
    byte[] data = payload.getBytes(StandardCharsets.UTF_8);
    return (data.length + ":" + payload + ",").getBytes(StandardCharsets.UTF_8);
  }

  private static String readNetstring(InputStream in) throws IOException {
    var length = new StringBuilder();

    int b;
    while ((b = in.read()) != ':') {
      if (b == -1) throw new IOException("EOF while reading netstring length");
      length.append((char) b);
    }

    byte[] payload = in.readNBytes(Integer.parseInt(length.toString()));

    assertThat((char) in.read()).isEqualTo(',');

    return new String(payload, StandardCharsets.UTF_8);
  }

  @Test
  void tcpServerRewritesForwardAddress() throws Exception {
    when(srs.forward("user@example.com", LOCAL_ALIAS)).thenReturn(SRS_ADDRESS);

    bootstrap = SRSServerFactory.createTCPSocketServer(srs, LOCAL_ALIAS);
    serverChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
    int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

    try (Socket socket = new Socket("127.0.0.1", port)) {
      socket.setSoTimeout(10_000);
      socket.getOutputStream().write(netstring("srsencoder user@example.com"));

      assertThat(readNetstring(socket.getInputStream())).isEqualTo("OK " + SRS_ADDRESS);
    }
  }

  @Test
  void tcpServerHandlesMultipleRequestsOnOneConnection() throws Exception {
    when(srs.forward("user@example.com", LOCAL_ALIAS)).thenReturn(SRS_ADDRESS);
    when(srs.isSRS(SRS_ADDRESS)).thenReturn(true);
    when(srs.reverse(SRS_ADDRESS)).thenReturn("user@example.com");

    bootstrap = SRSServerFactory.createTCPSocketServer(srs, LOCAL_ALIAS);
    serverChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
    int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

    try (Socket socket = new Socket("127.0.0.1", port)) {
      socket.setSoTimeout(10_000);

      socket.getOutputStream().write(netstring("srsencoder user@example.com"));
      assertThat(readNetstring(socket.getInputStream())).isEqualTo("OK " + SRS_ADDRESS);

      socket.getOutputStream().write(netstring("srsdecoder " + SRS_ADDRESS));
      assertThat(readNetstring(socket.getInputStream())).isEqualTo("OK user@example.com");
    }
  }

  @Test
  void unixSocketServerRewritesReverseAddress(@TempDir Path tempDir) throws Exception {
    assumeTrue(
        Epoll.isAvailable() || KQueue.isAvailable(),
        "requires a native transport (epoll or kqueue)");

    when(srs.isSRS(SRS_ADDRESS)).thenReturn(true);
    when(srs.reverse(SRS_ADDRESS)).thenReturn("user@example.com");

    Path socketPath = tempDir.resolve("srsd.sock");

    bootstrap = SRSServerFactory.createUnixSocketServer(srs, LOCAL_ALIAS);
    serverChannel = bootstrap.bind(new DomainSocketAddress(socketPath.toString())).sync().channel();

    try (SocketChannel client = SocketChannel.open(UnixDomainSocketAddress.of(socketPath))) {
      client.write(ByteBuffer.wrap(netstring("srsdecoder " + SRS_ADDRESS)));

      assertThat(readNetstring(Channels.newInputStream(client))).isEqualTo("OK user@example.com");
    }
  }

  @Test
  void invalidSRSKeyIsReportedThroughTheServer() throws Exception {
    when(srs.forward("user@example.com", LOCAL_ALIAS))
        .thenThrow(new InvalidKeyException("bad key"));

    bootstrap = SRSServerFactory.createTCPSocketServer(srs, LOCAL_ALIAS);
    serverChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
    int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

    try (Socket socket = new Socket("127.0.0.1", port)) {
      socket.setSoTimeout(10_000);
      socket.getOutputStream().write(netstring("srsencoder user@example.com"));

      assertThat(readNetstring(socket.getInputStream())).isEqualTo("NOTFOUND bad key");
    }
  }
}
