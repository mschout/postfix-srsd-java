package io.github.mschout.srsd.postfix;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.read.ListAppender;
import io.github.mschout.srsd.postfix.options.LogOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

class LoggingConfigurationTest {
  private Logger rootLogger;

  private Level savedLevel;

  private final List<Appender<ILoggingEvent>> savedAppenders = new ArrayList<>();

  @BeforeEach
  void saveRootLoggerState() {
    var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    savedLevel = rootLogger.getLevel();
    rootLogger.iteratorForAppenders().forEachRemaining(savedAppenders::add);
  }

  @AfterEach
  void restoreRootLoggerState() {
    rootLogger.detachAndStopAllAppenders();
    rootLogger.setLevel(savedLevel);
    for (Appender<ILoggingEvent> appender : savedAppenders) {
      if (!appender.isStarted()) appender.start();
      rootLogger.addAppender(appender);
    }
    savedAppenders.clear();
  }

  private LogOptions options(String... args) {
    return CommandLine.populateCommand(new LogOptions(), args);
  }

  private <T> T findAppender(Class<T> type) {
    var appenders = rootLogger.iteratorForAppenders();
    while (appenders.hasNext()) {
      Appender<ILoggingEvent> appender = appenders.next();
      if (type.isInstance(appender)) return type.cast(appender);
    }
    throw new AssertionError("No appender of type " + type.getSimpleName() + " on root logger");
  }

  @Test
  void doesNothingWhenLogFileIsNotGiven() {
    var marker = new ListAppender<ILoggingEvent>();
    marker.setName("marker");
    marker.start();
    rootLogger.addAppender(marker);

    LoggingConfiguration.configureLogging(options());

    assertThat(rootLogger.getAppender("marker")).isSameAs(marker);
    assertThat(marker.isStarted()).isTrue();
  }

  @Test
  void doesNothingWhenLogFileIsEmpty() {
    var marker = new ListAppender<ILoggingEvent>();
    marker.setName("marker");
    marker.start();
    rootLogger.addAppender(marker);

    LoggingConfiguration.configureLogging(options("--log-file", ""));

    assertThat(rootLogger.getAppender("marker")).isSameAs(marker);
  }

  @Test
  void replacesExistingAppenders() {
    var existing = new ListAppender<ILoggingEvent>();
    existing.setName("existing");
    existing.start();
    rootLogger.addAppender(existing);

    LoggingConfiguration.configureLogging(options("--log-file", "-"));

    assertThat(rootLogger.getAppender("existing")).isNull();
    assertThat(existing.isStarted()).isFalse();
  }

  @Test
  void configuresSyslogAppenderCaseInsensitively() {
    LoggingConfiguration.configureLogging(
        options(
            "--log-file", "syslog",
            "--syslog-host", "127.0.0.1",
            "--syslog-port", "10514",
            "--syslog-facility", "LOCAL0",
            "--log-level", "WARN"));

    var appender = findAppender(SyslogAppender.class);
    assertThat(appender.getSyslogHost()).isEqualTo("127.0.0.1");
    assertThat(appender.getPort()).isEqualTo(10514);
    assertThat(appender.getFacility()).isEqualTo("LOCAL0");
    assertThat(appender.isStarted()).isTrue();
    assertThat(rootLogger.getLevel()).isEqualTo(Level.WARN);
  }

  @Test
  void configuresConsoleAppenderForDash() {
    LoggingConfiguration.configureLogging(options("--log-file", "-", "--log-level", "DEBUG"));

    var appender = rootLogger.getAppender("CONSOLE");
    assertThat(appender).isInstanceOf(ConsoleAppender.class);
    assertThat(appender.isStarted()).isTrue();
    assertThat(rootLogger.getLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  void configuresFileAppenderWithDefaultLogLevel(@TempDir Path tempDir) throws Exception {
    Path logFile = tempDir.resolve("srsd.log");

    LoggingConfiguration.configureLogging(options("--log-file", logFile.toString()));

    var appender = (FileAppender<ILoggingEvent>) rootLogger.getAppender("file");
    assertThat(appender.getFile()).isEqualTo(logFile.toString());
    assertThat(appender.isAppend()).isTrue();
    assertThat(appender.isStarted()).isTrue();
    assertThat(rootLogger.getLevel()).isEqualTo(Level.INFO);

    // configureLogging logs an initialization message through the new appender
    assertThat(Files.readString(logFile)).contains("Logback initialized using log file");
  }
}
