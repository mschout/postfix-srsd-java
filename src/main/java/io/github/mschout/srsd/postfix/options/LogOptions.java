package io.github.mschout.srsd.postfix.options;

import lombok.Getter;
import picocli.CommandLine.Option;

@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "unused"})
@Getter
public class LogOptions {
  // Assigned reflectively by picocli when --log-file is given
  @Option(names = {"--log-file"})
  private String logFile;

  @Option(names = {"--syslog-host"})
  private String syslogHost = "localhost";

  @Option(names = {"--syslog-port"})
  private Integer syslogPort = 514;

  @Option(names = {"--syslog-facility"})
  private String syslogFacility = "MAIL";

  @Option(names = {"--log-level"})
  private String logLevel = "INFO";
}
