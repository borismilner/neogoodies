package logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.impl.SimpleLogger


object LogHelper {
    val logger: Logger

    init {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "[yyyy/MM/dd HH:mm:ss]")
        System.setProperty(SimpleLogger.LEVEL_IN_BRACKETS_KEY, "true")
        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true")

        val loggerName = "NeoGoodies"
        logger = LoggerFactory.getLogger(loggerName)
    }

}
