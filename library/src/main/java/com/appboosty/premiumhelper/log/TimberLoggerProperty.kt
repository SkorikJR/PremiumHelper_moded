package com.appboosty.premiumhelper.log

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T : Any> T.timber(tag: String? = null) = TimberLoggerProperty<T>(tag)

class TimberLoggerProperty<T : Any>(private val tag: String? = null) : ReadOnlyProperty<T, TimberLogger> {

    @Volatile var logger: TimberLogger? = null

    override fun getValue(thisRef: T, property: KProperty<*>): TimberLogger {
        logger?.let { return it }
        logger = TimberLogger(thisRef, tag)
        return logger!!
    }
}
