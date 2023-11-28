package com.appboosty.premiumhelper

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.gms.ads.AdValue
import com.appboosty.blytics.BLytics
import com.appboosty.blytics.PaidImpressionListener
import com.appboosty.blytics.model.Counter
import com.appboosty.blytics.model.Event
import com.appboosty.premiumhelper.util.ActivePurchaseInfo
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.util.InstallReferrer
import com.appboosty.premiumhelper.util.PackageUtils
import com.appboosty.premiumhelper.toto.TotoFeature
import com.appboosty.premiumhelper.ui.happymoment.HappyMoment
import com.appboosty.premiumhelper.util.ActivityLifecycleCallbacksAdapter
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import com.appboosty.premiumhelper.util.onError
import com.appboosty.premiumhelper.util.onSuccess
import kotlinx.coroutines.*
import java.util.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Analytics(private val application: Application, private val configuration: Configuration, private val preferences: Preferences) {

    private val log by timber()
    
    internal var isInitTimerExpired: Boolean = false
    internal var isAppOpenEventEnabled: Boolean = true

    private var purchaseFlowSource = ""
    private var userId = ""

    private val remoteKeys = HashMap<String, String>()

    private var externalPaidImpressionsListener: PaidImpressionListener? = null

    enum class SilentNotificationType(val value: String) {
        UNKNOWN("unknown"),
        HOLD("hold"),
        RECOVERED("recovered"),
        CANCELLED("cancelled")
    }

    enum class RateUsType(val value: String) {
        DIALOG("dialog"),
        IN_APP_REVIEW("in_app_review")
    }

    internal suspend fun init() {

        if (BLytics.getLogger() == null) {

            BLytics.init(application, configuration.get(Configuration.ANALYTICS_PREFIX), configuration.isDebugMode())

            if (userId.isNotEmpty()) {
                BLytics.getLogger().setUserId(userId)
            }

            withContext(Dispatchers.Main) {
                BLytics.startSessionObserver()
            }
        }
    }

    internal fun setOnPaidImpressionExternalListener(listener: PaidImpressionListener?){
        externalPaidImpressionsListener = listener
    }

    internal fun isFirstSession(): Boolean {
        return BLytics.getLogger()?.sessionNumber ?: 1 == 1
    }

    fun setUserId(id: String) {
        log.d("Analytics User ID: $id")
        userId = id
        try {
            BLytics.getLogger()?.setUserId(userId)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

//    @JvmOverloads
//    fun addRemoteConfigKey(key: String, defaultValue: String = "") {
//        remoteKeys[key] = defaultValue
//    }
//
//    private fun getAllRemoteKeys(): Map<String, String> {
//        val result = HashMap<String, String>()
//
//        remoteKeys.keys.onEach { key ->
//            result[key] = configuration.getString(key, remoteKeys[key]!!)
//        }
//
//        return result
//    }

    fun <T> setProperty(name: String, value: T) {
        try {
            BLytics.getLogger().setProperty(name, value)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    fun <T> setUserProperty(name: String, value: T) {
        try {
            BLytics.getLogger().setUserProperty(name, value)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    fun getUserProperty(name: String): String? {
        return try {
            BLytics.getLogger().getUserProperty(name)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
                null
            }
        }
    }

    fun sendEvent(name: String, vararg params: Bundle?) {
        sendEvent(getEvent(name, *params))
    }


    fun sendEvent(event: Event) {
        try {
            BLytics.getLogger()?.track(event)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    fun sendEventWithoutSession(name: String, vararg params: Bundle?) {
        sendEventWithoutSession(getEvent(name, *params))
    }

    fun sendEventWithoutSession(event: Event) {
        try {
            BLytics.getLogger().trackWithoutSession(event)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    fun scheduleEvent(event: Event, interval: Int) {
        try {
            BLytics.getLogger().track(event, interval)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    fun onFeatureUsed(name: String) {
        sendEvent("FeatureUsed", bundleOf("name" to name))
    }

    private fun getEvent(name: String, vararg extras: Bundle?) = getEvent(name, true, *extras)

    private fun getEvent(name: String, usePrefix: Boolean = true, vararg extras: Bundle?): Event {
        val event = Event(name, usePrefix)
            .setParam("days_since_install", PremiumHelperUtils.getDaysSinceInstall(application))
            .count("occurrence", Counter.GLOBAL)

        extras.forEach {
            event.params.putAll(it ?: Bundle.EMPTY)
        }

        return event
    }

    internal fun onAppInstall(installReferrer: String) {
        val source = installReferrer.ifEmpty { "not_set" }
        sendEvent("Install", bundleOf("source" to source))
    }

    internal fun onAppUpdated(sessionId: String){
        sendEvent(getEvent("App_update", false, bundleOf("session_id" to sessionId)))
    }

    internal fun onAppOpen(launchFrom: String, installReferrer: String, activePurchase: ActivePurchaseInfo? = null) {
        if (isAppOpenEventEnabled) {
            try {
                val event = getEvent("App_open").apply {

                    setParam("source", launchFrom)

                    if (installReferrer.isNotEmpty()) {
                        setParam("referrer", installReferrer)
                    }

                    if (activePurchase != null) {
                        val status = activePurchase.status?.value ?: ""
                        setParam("days_since_purchase", PremiumHelperUtils.getDaysSincePurchase(activePurchase.purchaseTime)
                        )
                        setParam("status", status)
                        setUserProperty("user_status", status)
                    } else {
                        val status = if (preferences.hasHistoryPurchases()) "back_to_free" else "free"
                        setParam("status", status)
                        setUserProperty("user_status", status)
                        checkHistoryPurchases()
                    }
                }

                BLytics.getLogger().track(event)

            } catch (t: Throwable) {
                if (BuildConfig.DEBUG) {
                    throw t
                } else {
                    log.e(t)
                }
            }
        }
    }

    private fun checkHistoryPurchases() {
        GlobalScope.launch {
            PremiumHelper.getInstance().hasHistoryPurchases()
                    .onSuccess { preferences.setHasHistoryPurchases(it) }
                    .onError { log.e(it.error, "Failed to update history purchases") }
        }
    }

    internal fun onOnboarding() {
        GlobalScope.launch {
            delay(1000)
            val initResponseStats = PremiumHelper.getInstance().totoFeature.getConfigResponseStats
            sendEvent(
                "Onboarding", bundleOf(
                    "sku" to configuration.get(Configuration.MAIN_SKU),
                    "timeout" to isInitTimerExpired.toString(),
                    "toto_response_code" to (initResponseStats?.code ?: "not available"),
                    "toto_latency" to (initResponseStats?.latency ?: "not available")
                )
            )
        }
    }

    internal fun onOnboardingComplete(offerLoaded: Boolean) {
        sendEvent(
            "Onboarding_complete", bundleOf(
                "sku" to configuration.get(Configuration.MAIN_SKU),
                "offer_loaded" to offerLoaded
            )
        )
    }

    internal fun onRelaunch(sku: String) {
        sendEvent("Relaunch", bundleOf("sku" to sku))
    }

    internal fun onPurchaseImpression(sku: String, source: String) {
        sendEvent("Purchase_impression", bundleOf(
            "sku" to sku,
            "offer" to source)
        )
    }

    fun onUpgradeInitiated(source: String, sku: String, extras: Bundle? = null) {
        purchaseFlowSource = source

        sendEvent(
            "Upgrade_initiated",
            bundleOf(
                "offer" to source,
                "sku" to sku
            ),
            extras
        )

    }

    internal fun onPurchaseStarted(source: String = purchaseFlowSource, sku: String) {

        purchaseFlowSource = source

        sendEvent(
            "Purchase_started",
            bundleOf(
                "offer" to source,
                "sku" to sku
            )
        )

    }

    internal fun onPurchaseSuccess(sku: String) {
        sendEvent(
            "Purchase_success",
            bundleOf(
                "offer" to purchaseFlowSource,
                "sku" to sku
            )
        )
    }

    @JvmOverloads
    fun onAdShown(type: com.appboosty.ads.AdManager.AdType, source: String? = null) {
        try {
            val event = getEvent("Ad_shown")
                .count("occurrence_${type.name.lowercase(Locale.ROOT)}_shown", Counter.GLOBAL)
                .setParam("type", type.name.lowercase(Locale.ROOT))

            source?.let { event.setParam("source", it) }

            BLytics.getLogger().track(event)

        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    @JvmOverloads
    fun onAdClick(type: com.appboosty.ads.AdManager.AdType, source: String? = null) {
        try {
            val event = getEvent("Ad_clicked")
                .count("occurrence_${type.name.lowercase(Locale.ROOT)}_clicked", Counter.GLOBAL)
                .setParam("type", type.name.lowercase(Locale.ROOT))

            source?.let { event.setParam("source", it) }

            BLytics.getLogger().track(event)

        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    @JvmOverloads
    fun onAdRewarded(type: com.appboosty.ads.AdManager.AdType, offer: String? = null) {
        try {
            val event = getEvent("Ad_rewarded")
                .count("occurrence_${type.name.lowercase(Locale.ROOT)}_rewarded", Counter.GLOBAL)
                .setParam("type", type.name.lowercase(Locale.ROOT))

            if (offer != null) {
                event.setParam("offer", offer)
            }

            BLytics.getLogger().track(event)

        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }


    fun onExitAdShown(type: com.appboosty.ads.AdManager.AdType = com.appboosty.ads.AdManager.AdType.NATIVE, offer: String? = null) {
        try {
            val event = getEvent("ExitAd_shown")
                .count("occurrence_${type.name.lowercase(Locale.ROOT)}_shown", Counter.GLOBAL)

            if (offer != null) {
                event.setParam("offer", offer)
            }

            BLytics.getLogger().track(event)

        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }


    fun onExitAdClick(type: com.appboosty.ads.AdManager.AdType = com.appboosty.ads.AdManager.AdType.NATIVE, offer: String? = null) {
        try {
            val event = getEvent("ExitAd_clicked")
                .count("occurrence_${type.name.lowercase(Locale.ROOT)}_clicked", Counter.GLOBAL)

            if (offer != null) {
                event.setParam("offer", offer)
            }

            BLytics.getLogger().track(event)

        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                throw t
            } else {
                log.e(t)
            }
        }
    }

    fun onRateUsShown(type: RateUsType = RateUsType.DIALOG) {
        sendEvent("Rate_us_shown", bundleOf("type" to type.value))
    }

    fun onRateUsPositive() {
        sendEvent("Rate_us_positive")
    }

    internal fun onSilentPush(type: SilentNotificationType) {

        val params = bundleOf(
            "type" to type.value
        )
        val activePurchaseInfo = preferences.getActivePurchaseInfo()

        if (activePurchaseInfo != null) {
            params.putInt(
                "days_since_purchase",
                PremiumHelperUtils.getDaysSincePurchase(activePurchaseInfo.purchaseTime)
            )
        }

        sendEventWithoutSession("Silent_Notification", params)
   }

    fun onAppOpened(installReferrer: InstallReferrer) {
        if (preferences.isFirstAppStart() && !PremiumHelperUtils.isInstalledFromUpdate(application)) {
            GlobalScope.launch {
                onAppInstall(installReferrer.get())
            }
        }

        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
            override fun onActivityResumed(activity: Activity) {

                val source = activity.intent?.let { intent ->
                    when {
                        intent.getBooleanExtra(PremiumHelper.FLAG_FROM_NOTIFICATION, false) -> "notification"
                        intent.getBooleanExtra(PremiumHelper.FLAG_FROM_WIDGET, false) -> "widget"
                        intent.getBooleanExtra(PremiumHelper.FLAG_FROM_SHORTCUT, false) -> "shortcut"
                        else -> null
                    }
                } ?: "launcher"

                GlobalScope.launch {
                    onAppOpen(source, installReferrer.get(), preferences.getActivePurchaseInfo())
                }

                activity.intent?.run {
                    putExtra(PremiumHelper.FLAG_FROM_NOTIFICATION, false)
                    putExtra(PremiumHelper.FLAG_FROM_WIDGET, false)
                    putExtra(PremiumHelper.FLAG_FROM_SHORTCUT, false)
                }

                application.unregisterActivityLifecycleCallbacks(this)
            }
        })
    }

    fun onGetRemoteConfig(success: Boolean, latency: Long) {
        sendEvent(
            "RemoteGetConfig", bundleOf(
                "success" to success,
                "latency" to latency,
                "has_connection" to PremiumHelperUtils.hasInternetConnection(application)
            )
        )
    }

    fun onGetConfig(responseStats: TotoFeature.ResponseStats, xcache: String) {

        sendEvent(
            "TotoGetConfig", bundleOf(
                "splash_timeout" to isInitTimerExpired.toString(),
                "toto_response_code" to (responseStats.code),
                "toto_latency" to (responseStats.latency),
                "x_cache" to xcache
            )
        )

    }

    fun onPostConfig(responseStats: TotoFeature.ResponseStats) {
        sendEvent(
            "TotoPostConfig", bundleOf(
                "toto_response_code" to (responseStats.code),
                "toto_latency" to (responseStats.latency)
            )
        )

    }

    fun onTotoRegister(responseStats: TotoFeature.ResponseStats) {
        sendEvent(
            "TotoRegister", bundleOf(
                "toto_response_code" to (responseStats.code),
                "toto_latency" to (responseStats.latency)
            )
        )

    }

    fun onPaidImpression(adUnitId: String, adValue: AdValue, mediationAdapter: String?){
        val params = bundleOf(
            "valuemicros" to adValue.valueMicros,
            "value" to (adValue.valueMicros / 1000000f),
            "currency" to adValue.currencyCode,
            "precision" to adValue.precisionType,
            "adunitid" to adUnitId,
            "mediation" to "admob",
            "network" to (mediationAdapter ?: "unknown")
        )
        onPaidImpression(params)
    }
    fun onPaidImpression(params : Bundle) {
        sendEvent(getEvent("paid_ad_impression", false, params))
        CoroutineScope(Dispatchers.Default).launch {
            externalPaidImpressionsListener?.onPaidImpression(params)
        }
    }

    fun onSessionClose(sessionId: String, timestamp: Long, duration: Long) {
        val params = bundleOf(
            "session_id" to sessionId,
            "timestamp" to timestamp,
            "duration" to duration
        )
        sendEvent(getEvent("toto_session_end", false, params))
    }

    fun onSessionOpen(sessionId: String, timestamp: Long){
        val params = bundleOf(
            "session_id" to sessionId,
            "timestamp" to timestamp,
            "application_id" to application.packageName,
            "application_version" to PackageUtils.getAppVersionName(application)
        )
        sendEvent(getEvent("toto_session_start", false, params))
    }

    fun onHappyMoment(happyMomentRateMode: HappyMoment.HappyMomentRateMode) {
        sendEvent("Happy_Moment", bundleOf(
            "happy_moment" to happyMomentRateMode.name
        ))
    }

    fun sendStartupPerformanceData(params: Bundle) {
        sendEvent(getEvent("Performance_initialization", false, params))
    }

    fun sendBannersPerformanceData(params: Bundle) {
        sendEvent(getEvent("Performance_banners", false, params))
    }

    fun sendPurchasesPerformanceData(params: Bundle) {
        sendEvent(getEvent("Performance_offers", false, params))
    }

    fun sendInterstitialPerformanceData(params: Bundle) {
        sendEvent(getEvent("Performance_interstitials", false, params))
    }

    fun sendNativeAdsPerformanceData(params: Bundle) {
        sendEvent(getEvent("Performance_native_ads", false, params))
    }

    fun sendRewardedAdPerformanceData(params: Bundle) {
        sendEvent(getEvent("Performance_rewarded_ads", false, params))
    }

    fun onAdLoadError(params: Bundle) {
        sendEvent(getEvent("Ad_load_error", false, params))
    }

}