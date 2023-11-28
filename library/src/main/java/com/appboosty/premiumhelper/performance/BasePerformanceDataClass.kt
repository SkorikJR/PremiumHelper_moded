package com.appboosty.premiumhelper.performance

open class BasePerformanceDataClass {

    protected fun calculateDuration(end: Long, start: Long): Long {
        return if (start == 0L || end == 0L) 0L else end - start
    }

    protected fun booleanToString(value: Boolean): String{
        return if(value) "true" else "false"
    }

    protected fun listToCsv(list: List<String>): String{
        return list.joinToString { it }
    }
}