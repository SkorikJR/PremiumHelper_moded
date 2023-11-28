package com.appboosty.premiumhelper.log

import android.content.Context
import android.text.TextUtils
import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.util.FileSize
import org.slf4j.LoggerFactory
import timber.log.Timber

class FileLoggingTree(context: Context, private val isDebugMode: Boolean) : Timber.Tree() {

    companion object {
        private const val FILE_COUNT = 2
        private const val MAX_FILE_SIZE_DEBUG = 1024 * 1024 * 5 // 5M
        private const val MAX_FILE_SIZE_RELEASE = 1024 * 500 // 500K
        private const val LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"
    }

    private val fileLogger = LoggerFactory.getLogger("PremiumHelper")

    init {

        val fileName = context.filesDir.absolutePath + "/premium_helper.log"
        val fileNamePattern = context.filesDir.absolutePath + "/premium_helper.log.%i"

        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.reset()

        val fileAppender = RollingFileAppender<ILoggingEvent>()
        fileAppender.context = loggerContext
        fileAppender.file = fileName

        val rollingPolicy = FixedWindowRollingPolicy()

        with(rollingPolicy) {
            this.context = loggerContext
            this.fileNamePattern = fileNamePattern
            setParent(fileAppender)
            maxIndex = FILE_COUNT
            minIndex = 1
            start()
        }

        val triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>()
        triggeringPolicy.maxFileSize = FileSize.valueOf((if (isDebugMode) MAX_FILE_SIZE_DEBUG else MAX_FILE_SIZE_RELEASE).toString())
        triggeringPolicy.start()

        val encoder = PatternLayoutEncoder()
        encoder.context = loggerContext
        encoder.pattern = LOG_PATTERN
        encoder.start()

        fileAppender.encoder = encoder
        fileAppender.rollingPolicy = rollingPolicy
        fileAppender.triggeringPolicy = triggeringPolicy
        fileAppender.start()

        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.addAppender(fileAppender)
        root.level = Level.DEBUG
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return isDebugMode || (priority != Log.VERBOSE && priority != Log.DEBUG && priority != Log.INFO)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (isLoggable(tag, priority)) {
            val logLine: String = if (TextUtils.isEmpty(tag)) message else "$tag:$message"
            when (priority) {
                Log.ASSERT, Log.DEBUG, Log.VERBOSE -> if (t == null) fileLogger.debug(logLine) else fileLogger.debug(logLine, t)
                Log.ERROR -> if (t == null) fileLogger.error(logLine) else fileLogger.error(logLine, t)
                Log.INFO -> if (t == null) fileLogger.info(logLine) else fileLogger.info(logLine, t)
                Log.WARN -> if (t == null) fileLogger.warn(logLine) else fileLogger.warn(logLine, t)
                else -> if (t == null) fileLogger.debug(logLine) else fileLogger.error(logLine, t)
            }
        }
    }

}