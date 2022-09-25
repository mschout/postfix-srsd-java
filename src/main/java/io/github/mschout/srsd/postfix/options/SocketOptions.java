package io.github.mschout.srsd.postfix.options;

import com.google.common.base.Strings;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

@Getter
@SuppressWarnings("unused")
public class SocketOptions {
  @ArgGroup(exclusive = false)
  private TCPSocketOptions tcpSocket;

  @Option(names = "--socket", required = true, description = "Unix socket file path to listen on.")
  private String socketPath;

  public SocketType getSocketType() {
    return !Strings.isNullOrEmpty(socketPath) ? SocketType.UNIX : SocketType.TCP;
  }

  public static class TCPSocketOptions {
    @Option(names = "--host", description = "TCP Hostname to bind to", defaultValue = "localhost")
    @Getter
    private String hostName;

    @Option(names = "--port", required = true, description = "TCP Port to listen on")
    @Getter
    private Integer port;
  }

  public enum SocketType {
    TCP,
    UNIX
  }
}
