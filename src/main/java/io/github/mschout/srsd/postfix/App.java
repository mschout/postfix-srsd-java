package io.github.mschout.srsd.postfix;

import com.google.common.io.Files;
import io.github.mschout.srsd.postfix.options.LogOptions;
import io.github.mschout.srsd.postfix.options.SocketOptions;
import java.io.File;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main Postfix SRSD class.
 */
@SuppressWarnings({ "FieldMayBeFinal", "CanBeFinal", "unused" })
@Slf4j
@Command(name = "postfix-srsd", mixinStandardHelpOptions = true, description = "SRS encoder and decoder daemon for postfix.")
public class App implements Runnable {
  @ArgGroup(multiplicity = "1")
  SocketOptions socketOptions;

  @Option(
    names = { "--secret-file" },
    required = true,
    description = "File containing SRS encoder secrets.  First line is the default secret, additional lines are valid secrets for verifying addresses."
  )
  private String secretFile;

  @Option(names = { "--local-alias" }, required = true)
  private String localAlias;

  @ArgGroup(exclusive = false, multiplicity = "1")
  LogOptions logOptions;

  // TODO - allow specifying max command length (SRS max frame size)

  public static void main(String[] args) {
    CommandLine app = new CommandLine(new App());
    app.execute(args);
  }

  @SneakyThrows
  @Override
  public void run() {
    LoggingConfiguration.configureLogging(this.logOptions);

    log.info("Local alias: {}", localAlias);

    var secrets = Files.asCharSource(new File(secretFile), StandardCharsets.UTF_8).readLines();

    SRSServer.builder().socketOptions(socketOptions).secrets(secrets).localAlias(localAlias).build().run();
  }
}
