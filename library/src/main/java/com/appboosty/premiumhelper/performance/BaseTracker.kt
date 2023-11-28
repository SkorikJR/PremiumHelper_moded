package com.appboosty.premiumhelper.performance

import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration.Params.SEND_PERFORMANCE_EVENTS

open class BaseTracker {

    private fun isEnabled(): Boolean =
        PremiumHelper.getInstance().configuration.get(SEND_PERFORMANCE_EVENTS)

    fun sendEvent(doSendEvent: () -> Unit) {
        if (isEnabled()) doSendEvent()
    }
}