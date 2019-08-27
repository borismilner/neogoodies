package logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

public class LogHelper {
    private static Logger logger;

    public static Logger getLogger() {
        if (logger == null) {
            init();
        }

        return logger;
    }

    private static void init() {
        String loggerName = "NeoGoodies";

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        File f = new File("src/main/java/logging/log4j2.xml");
        URI fc = f.toURI();
        ctx.setConfigLocation(fc);

        PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(config)
                .withPattern("%d{HH:mm:ss} [%t] %level - %msg%n")
                .build();

        Appender appender = ConsoleAppender.newBuilder()
                .setName(loggerName)
                .setLayout(layout)
                .setConfiguration(config)
                .build();

        appender.start();
        config.addAppender(appender);

        AppenderRef appenderRef = AppenderRef.createAppenderRef("Console-Appender", null, null);
        AppenderRef[] appenderRefs = new AppenderRef[]{appenderRef};

        LoggerConfig loggerConfig = LoggerConfig
                .createLogger(false, Level.DEBUG, loggerName, "true", appenderRefs, null, config, null);
        loggerConfig.addAppender(appender, Level.DEBUG, null);
        config.addLogger(loggerName, loggerConfig);
        ctx.updateLoggers();

        logger = LoggerFactory.getLogger(loggerName);
    }

    private LogHelper() {
    }
}
