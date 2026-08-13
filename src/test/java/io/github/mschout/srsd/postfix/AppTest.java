package io.github.mschout.srsd.postfix;

import static io.github.mschout.srsd.postfix.Netstrings.netstring;
import static io.github.mschout.srsd.postfix.Netstrings.readNetstring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mschout.srsd.postfix.options.SocketOptions;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AppTest {
  private App parse(String... args) {
    App app = new App();
    new CommandLine(app).parseArgs(args);
    return app;
  }

  @Test
  void parsesUnixSocketOptions() {
    App app =
        parse(
            "--socket", "/run/srsd.sock",
            "--secret-file", "/etc/srs-secrets",
            "--local-alias", "example.com",
            "--log-level", "DEBUG");

    assertThat(app.socketOptions.getSocketType()).isEqualTo(SocketOptions.SocketType.UNIX);
    assertThat(app.socketOptions.getSocketPath()).isEqualTo("/run/srsd.sock");
  }

  @Test
  void parsesTCPSocketOptionsWithDefaultHost() {
    App app =
        parse(
            "--port", "10001",
            "--secret-file", "/etc/srs-secrets",
            "--local-alias", "example.com",
            "--log-level", "INFO");

    assertThat(app.socketOptions.getSocketType()).isEqualTo(SocketOptions.SocketType.TCP);
    assertThat(app.socketOptions.getTcpSocket().getHostName()).isEqualTo("localhost");
    assertThat(app.socketOptions.getTcpSocket().getPort()).isEqualTo(10001);
  }

  @Test
  void parsesTCPSocketOptionsWithExplicitHost() {
    App app =
        parse(
            "--host", "0.0.0.0",
            "--port", "10001",
            "--secret-file", "/etc/srs-secrets",
            "--local-alias", "example.com",
            "--log-level", "INFO");

    assertThat(app.socketOptions.getTcpSocket().getHostName()).isEqualTo("0.0.0.0");
  }

  @Test
  void runStartsServerFromSecretFile(@TempDir Path tempDir) throws Exception {
    Path secretFile = tempDir.resolve("srs-secrets");
    Files.write(secretFile, List.of("t0ps3cr3t", "0lds3cr3t"));

    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }

    App app =
        parse(
            "--host", "127.0.0.1",
            "--port", String.valueOf(port),
            "--secret-file", secretFile.toString(),
            "--local-alias", "example.com",
            "--log-level", "INFO");

    Thread serverThread = new Thread(app, "app-test-server");
    // run() sneaky-throws the InterruptedException raised when the test interrupts the server
    serverThread.setUncaughtExceptionHandler((thread, e) -> {});
    serverThread.start();

    try (Socket socket = connectWithRetry(port)) {
      socket.setSoTimeout(10_000);

      socket.getOutputStream().write(netstring("srsencoder user@remote.example.net"));
      String response = readNetstring(socket.getInputStream());

      assertThat(response).startsWith("OK SRS0=").endsWith("@example.com");
    } finally {
      serverThread.interrupt();
      serverThread.join(10_000);
    }

    assertThat(serverThread.isAlive()).isFalse();
  }

  @Test
  void executeReportsUsageErrorForInvalidArguments() {
    int exitCode = new CommandLine(new App()).execute("--no-such-option");

    assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
  }

  @Test
  void mainHandlesInvalidArgumentsWithoutThrowing() {
    // execute() inside main catches the picocli usage error and prints it rather than throwing
    App.main(new String[] {"--no-such-option"});
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

  @Test
  void rejectsMissingSocketOptions() {
    assertThatThrownBy(
            () ->
                parse(
                    "--secret-file", "/etc/srs-secrets",
                    "--local-alias", "example.com",
                    "--log-level", "INFO"))
        .isInstanceOf(CommandLine.MissingParameterException.class);
  }

  @Test
  void rejectsBothUnixAndTCPSocketOptions() {
    assertThatThrownBy(
            () ->
                parse(
                    "--socket", "/run/srsd.sock",
                    "--port", "10001",
                    "--secret-file", "/etc/srs-secrets",
                    "--local-alias", "example.com",
                    "--log-level", "INFO"))
        .isInstanceOf(CommandLine.MutuallyExclusiveArgsException.class);
  }
}
