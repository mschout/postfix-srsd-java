package io.github.mschout.srsd.postfix;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main Postfix SRSD class.
 */
@Command(name = "postfix-srsd")
@Slf4j
public class App implements Runnable {
  @Option(names = { "--socket-path" }, required = true)
  private String socketPath;

  @Option(names = { "--secret-file" }, required = true)
  private String secretFile;

  @Option(names = { "--local-alias" }, required = true)
  private String localAlias;

  @Option(names = { "--log-file" })
  private String logFile;

  @Option(names = { "--syslog-host" })
  private String syslogHost = "localhost";

  @Option(names = { "--syslog-port" })
  private Integer syslogPort = 514;

  @Option(names = { "--syslog-facility" })
  private String syslogFacility = "MAIL";

  @Option(names = { "--log-level" })
  private String logLevel = "INFO";

  public static void main(String[] args) {
    CommandLine app = new CommandLine(new App());
    app.execute(args);
  }

  @SneakyThrows
  @Override
  public void run() {
    configureLogback();

    var secrets = Files.asCharSource(new File(secretFile), StandardCharsets.UTF_8).readLines();

    SRSServer.builder().socketPath(socketPath).secrets(secrets).localAlias(localAlias).build().run();
  }

  private void configureLogback() {
    if (Strings.isNullOrEmpty(logFile)) return;

    var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    var rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAndStopAllAppenders(); // only use our own appender if any other appender was added

    rootLogger.setLevel(Level.toLevel(logLevel));

    if (logFile.equalsIgnoreCase("SYSLOG")) {
      var appender = new SyslogAppender();
      appender.setSyslogHost(syslogHost);
      appender.setPort(syslogPort);
      appender.setFacility(syslogFacility);
      appender.setSuffixPattern("[%thread] %-5level %logger{36} - %msg%n");
      appender.setContext(loggerContext);
      appender.start();

      rootLogger.addAppender(appender);

      log.info("Logback initialized using syslog facility {} at level {}", syslogFacility, rootLogger.getLevel().toString());
    } else if (logFile.equals("-")) {
      // console appender
      var appender = new ConsoleAppender<ILoggingEvent>();
      appender.setContext(loggerContext);
      appender.setEncoder(buildPatternLayoutEncoder(loggerContext));
      appender.setName("CONSOLE");
      appender.start();

      rootLogger.addAppender(appender);
    } else {
      // Log to a file.
      var appender = new FileAppender<ILoggingEvent>();
      appender.setContext(loggerContext);
      appender.setName("file");
      appender.setFile(logFile);
      appender.setAppend(true);
      appender.setEncoder(buildPatternLayoutEncoder(loggerContext));
      appender.start();

      rootLogger.addAppender(appender);

      log.info("Logback initialized using log file {} at level {}", logFile, rootLogger.getLevel().toString());
    }
  }

  private LayoutWrappingEncoder<ILoggingEvent> buildPatternLayoutEncoder(LoggerContext loggerContext) {
    var encoder = new LayoutWrappingEncoder<ILoggingEvent>();
    encoder.setContext(loggerContext);

    var layout = new PatternLayout();
    layout.setPattern("%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
    layout.setContext(loggerContext);
    layout.start();

    encoder.setLayout(layout);

    return encoder;
  }
}
