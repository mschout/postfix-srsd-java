package io.github.mschout.srsd.postfix;

import static io.github.mschout.srsd.postfix.Netstrings.netstring;
import static io.github.mschout.srsd.postfix.Netstrings.readNetstring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** End-to-end tests running the full server with a real SRS instance. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class SRSServerTest {
  private static final String LOCAL_ALIAS = "example.com";

  private static final String REMOTE_ADDRESS = "user@remote.example.net";

  private Thread serverThread;

  @AfterEach
  void stopServer() throws InterruptedException {
    if (serverThread != null) {
      serverThread.interrupt();
      serverThread.join(10_000);
      assertThat(serverThread.isAlive()).isFalse();
    }
  }

  private void startServer(SRSServer server) {
    serverThread =
        new Thread(
            () -> {
              try {
                server.run();
              } catch (InterruptedException ignored) {
                // expected on shutdown
              }
            },
            "srs-server-test");
    serverThread.start();
  }

  private SRSServer buildServer(String... appArgs) {
    App app = new App();
    new CommandLine(app).parseArgs(appArgs);

    return SRSServer.builder()
        .socketOptions(app.socketOptions)
        .secrets(List.of("t0ps3cr3t"))
        .localAlias(LOCAL_ALIAS)
        .build();
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private static Socket connectWithRetry(int port) throws Exception {
    while (true) {
      try {
        return new Socket("127.0.0.1", port);
      } catch (ConnectException e) {
        Thread.sleep(50);
      }
    }
  }

  private static SocketChannel connectWithRetry(Path socketPath) throws Exception {
    while (true) {
      try {
        return SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
      } catch (IOException e) {
        Thread.sleep(50);
      }
    }
  }

  @Test
  void tcpServerForwardsAndReversesWithRealSRS() throws Exception {
    int port = freePort();

    startServer(
        buildServer(
            "--host", "127.0.0.1",
            "--port", String.valueOf(port),
            "--secret-file", "/dev/null",
            "--local-alias", LOCAL_ALIAS,
            "--log-level", "INFO"));

    try (Socket socket = connectWithRetry(port)) {
      socket.setSoTimeout(10_000);

      socket.getOutputStream().write(netstring("srsencoder " + REMOTE_ADDRESS));
      String forwardResponse = readNetstring(socket.getInputStream());
      assertThat(forwardResponse).startsWith("OK SRS0=").endsWith("@" + LOCAL_ALIAS);

      String srsAddress = forwardResponse.substring("OK ".length());
      socket.getOutputStream().write(netstring("srsdecoder " + srsAddress));
      assertThat(readNetstring(socket.getInputStream())).isEqualTo("OK " + REMOTE_ADDRESS);
    }
  }

  @Test
  void unixSocketServerForwardsAndReversesWithRealSRS(@TempDir Path tempDir) throws Exception {
    assumeTrue(
        Epoll.isAvailable() || KQueue.isAvailable(),
        "requires a native transport (epoll or kqueue)");

    Path socketPath = tempDir.resolve("srsd.sock");

    startServer(
        buildServer(
            "--socket",
            socketPath.toString(),
            "--secret-file",
            "/dev/null",
            "--local-alias",
            LOCAL_ALIAS,
            "--log-level",
            "INFO"));

    try (SocketChannel client = connectWithRetry(socketPath)) {
      client.write(ByteBuffer.wrap(netstring("srsencoder " + REMOTE_ADDRESS)));
      String forwardResponse = readNetstring(Channels.newInputStream(client));
      assertThat(forwardResponse).startsWith("OK SRS0=").endsWith("@" + LOCAL_ALIAS);

      String srsAddress = forwardResponse.substring("OK ".length());
      client.write(ByteBuffer.wrap(netstring("srsdecoder " + srsAddress)));
      assertThat(readNetstring(Channels.newInputStream(client))).isEqualTo("OK " + REMOTE_ADDRESS);
    }
  }
}
