package io.github.mschout.srsd.postfix;

import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main Postfix SRSD class.
 */
@Command(name = "postfix-srsd")
public class App implements Runnable {
  @Option(names = { "--socket-path" }, required = true)
  private String socketPath;

  @Option(names = { "--secret-file" }, required = true)
  private String secretFile;

  @Option(names = { "--local-alias" }, required = true)
  private String localAlias;

  public static void main(String[] args) {
    CommandLine app = new CommandLine(new App());
    app.execute(args);
  }

  @SneakyThrows
  @Override
  public void run() {
    List<String> secrets = Files.asCharSource(new File(secretFile), StandardCharsets.UTF_8).readLines();

    SRSServer.builder().socketPath(socketPath).secrets(secrets).localAlias(localAlias).build().run();
  }
}
