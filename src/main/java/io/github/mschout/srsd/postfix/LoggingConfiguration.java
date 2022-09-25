package io.github.mschout.srsd.postfix;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.google.common.base.Strings;
import io.github.mschout.srsd.postfix.options.LogOptions;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class LoggingConfiguration {

  public static void configureLogging(LogOptions logOptions) {
    if (Strings.isNullOrEmpty(logOptions.getLogFile())) return;

    var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    var rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAndStopAllAppenders(); // only use our own appender if any other appender was added

    rootLogger.setLevel(Level.toLevel(logOptions.getLogLevel()));

    if (logOptions.getLogFile().equalsIgnoreCase("SYSLOG")) {
      var appender = new SyslogAppender();
      appender.setSyslogHost(logOptions.getSyslogHost());
      appender.setPort(logOptions.getSyslogPort());
      appender.setFacility(logOptions.getSyslogFacility());
      appender.setSuffixPattern("[%thread] %-5level %logger{36} - %msg%n");
      appender.setContext(loggerContext);
      appender.start();

      rootLogger.addAppender(appender);

      log.info(
        "Logback initialized using syslog facility {} at level {}",
        logOptions.getSyslogFacility(),
        rootLogger.getLevel().toString()
      );
    } else if (logOptions.getLogFile().equals("-")) {
      // console appender
      var appender = new ConsoleAppender<ILoggingEvent>();
      appender.setContext(loggerContext);
      appender.setEncoder(buildPatternLayoutEncoder());
      appender.setName("CONSOLE");
      appender.start();

      rootLogger.addAppender(appender);

      log.info("Logback initialized using console output at level {}", rootLogger.getLevel().toString());
    } else {
      // Log to a file.
      var appender = new FileAppender<ILoggingEvent>();
      appender.setContext(loggerContext);
      appender.setName("file");
      appender.setFile(logOptions.getLogFile());
      appender.setAppend(true);
      appender.setEncoder(buildPatternLayoutEncoder());
      appender.start();

      rootLogger.addAppender(appender);

      log.info("Logback initialized using log file {} at level {}", logOptions.getLogFile(), rootLogger.getLevel().toString());
    }
  }

  private static LayoutWrappingEncoder<ILoggingEvent> buildPatternLayoutEncoder() {
    var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

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
