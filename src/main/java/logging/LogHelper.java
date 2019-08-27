package logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

public class LogHelper {
    private static Logger logger;

    public static Logger getLogger() {
        if (logger == null) {
            init();
        }

        return logger;
    }

    private static void init() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "[yyyy/MM/dd HH:mm:ss]");
        System.setProperty(SimpleLogger.LEVEL_IN_BRACKETS_KEY, "true");
        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");

        String loggerName = "NeoGoodies";
        logger = LoggerFactory.getLogger(loggerName);
    }

    private LogHelper() {
    }
}
