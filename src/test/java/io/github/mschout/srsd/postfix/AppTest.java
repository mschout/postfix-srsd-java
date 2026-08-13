package io.github.mschout.srsd.postfix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mschout.srsd.postfix.options.SocketOptions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

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
