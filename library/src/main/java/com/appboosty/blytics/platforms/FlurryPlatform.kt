package com.appboosty.blytics.platforms

import android.app.Application
import android.os.Bundle
import com.flurry.android.FlurryAgent
import com.flurry.android.FlurryPerformance
import com.appboosty.blytics.AnalyticsPlatform
import com.appboosty.blytics.model.Session
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.util.PHResult
import timber.log.Timber

class FlurryPlatform : AnalyticsPlatform() {


    companion object{
        private const val MAXIMUM_PARAMETERS_COUNT = 10
        private const val MAX_PARAM_LENGTH = 100
    }

    private var application: Application? = null
    override val name: String
        get() = "Flurry"

    private var flurryApiKey = ""

    override fun isEnabled(application: Application): Boolean {
        var enabled = false
        try {
            enabled = Class.forName("com.flurry.android.FlurryAgent") != null
        } catch (ignored: ClassNotFoundException) {
            Timber.tag("FlurryPlatform").i("FlurryAnalytics not found!")
        }
        flurryApiKey = PremiumHelper.getInstance().configuration.get(Configuration.FLURRY_API_KEY)
        return enabled && flurryApiKey.isNotEmpty()
    }

    override fun initialize(application: Application, debug: Boolean) {
        super.initialize(application, debug)
        this.application = application
        if(flurryApiKey.isNotEmpty()) {
            FlurryAgent.Builder()
                .withDataSaleOptOut(false) //CCPA - the default value is false
                .withIncludeBackgroundSessionsInMetrics(true)
                .withReportLocation(true)
                .withPerformanceMetrics(FlurryPerformance.ALL)
                .build(application, flurryApiKey)
        }else Timber.tag("FlurryPlatform").wtf(IllegalArgumentException("Flurry API key is empty"))
    }

    override fun track(event: String, params: Bundle) {
        val paramsMap = asMap(ensureParamsLength(params, MAX_PARAM_LENGTH))
        when (val result = ensureParamsCount(paramsMap)) {
            is PHResult.Success ->
                FlurryAgent.logEvent(event, result.value)

            is PHResult.Failure ->
                Timber.tag("FlurryPlatform").e(result.error, "The event: $event")
        }
    }

    override fun onSessionStart(session: Session?) {
        FlurryAgent.onStartSession(application!!)
    }

    override fun onSessionFinish(session: Session?) {
        FlurryAgent.onEndSession(application!!)
    }

    override fun setUserId(userId: String) {
        FlurryAgent.setUserId(userId)
    }

    override fun setUserProperty(property: String?, value: String?) {}
    override fun getMaximumParametersCount(): Int = MAXIMUM_PARAMETERS_COUNT
}