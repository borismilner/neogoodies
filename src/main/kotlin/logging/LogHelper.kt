package logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.impl.SimpleLogger
import java.util.*


object LogHelper {
    private const val loggerName = "NeoGoodies"
    var logger: Logger

    init {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "[yyyy/MM/dd HH:mm:ss]")
        System.setProperty(SimpleLogger.LEVEL_IN_BRACKETS_KEY, "true")
        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true")
        logger = LoggerFactory.getLogger(loggerName)
    }

    fun setCustomLogger(loggerName: String, systemProperties: Properties?) {
        if (systemProperties != null) {
            System.setProperties(systemProperties)
        }
        logger = LoggerFactory.getLogger(loggerName)
        logger.trace("Logger $loggerName was set.")
    }

}
