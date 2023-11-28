package com.appboosty.premiumhelper.util

import android.content.Context
import androidx.annotation.Keep
import androidx.startup.Initializer
import com.appboosty.premiumhelper.performance.StartupPerformanceTracker

@Keep
class ApplicationStartListener : Initializer<Unit> {
    override fun create(context: Context) {
        StartupPerformanceTracker.getInstance().onApplicationStart()
    }
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}