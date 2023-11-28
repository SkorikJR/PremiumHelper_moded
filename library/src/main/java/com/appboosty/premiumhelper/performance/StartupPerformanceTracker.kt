package com.appboosty.premiumhelper.performance

import android.os.Bundle
import androidx.annotation.Keep
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import com.appboosty.premiumhelper.BuildConfig
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import timber.log.Timber
import java.util.concurrent.TimeUnit

class StartupPerformanceTracker private constructor() : BaseTracker() {
    companion object {
        private const val TAG = "PerformanceTracker"
        private var instance: StartupPerformanceTracker? = null

        fun getInstance(): StartupPerformanceTracker {
            return instance ?: run {
                instance = StartupPerformanceTracker()
                instance!!
            }
        }
    }

    private var startupData: StartupData? = null

    fun onPremiumHelperInitialization() {
        startupData?.phStartTimestamp = System.currentTimeMillis()
    }

    fun onAdManagerInitializationStart() {
        startupData?.adManagerStartTimestamp = System.currentTimeMillis()
    }

    fun onAdManagerInitializationEnd() {
        startupData?.adManagerEndTimeStamp = System.currentTimeMillis()
    }

    fun setAdProvider(provider: String) {
        startupData?.adProvider = provider
    }

    fun onRemoteConfigInitializationStart() {
        startupData?.remoteConfigStartTimestamp = System.currentTimeMillis()
    }

    fun onRemoteConfigInitializationEnd() {
        startupData?.remoteConfigEndTimestamp = System.currentTimeMillis()
    }

    fun onTotoInitializationStart() {
        startupData?.totoConfigStartTimestamp = System.currentTimeMillis()
    }

    fun onTotoInitializationEnd() {
        startupData?.totoConfigEndTimestamp = System.currentTimeMillis()
    }

    fun onApplicationStart() {
        startupData = StartupData().apply {
            applicationStartTimestamp = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun onSplashScreenHide() {
        startupData?.let { data ->
            sendEvent {
                startupData = null
                val params = data.toBundle()
                Timber.tag(TAG).d(params.toString())
                PremiumHelper.getInstance().analytics.sendStartupPerformanceData(params)

            }
        }
    }

    fun onPremiumHelperInitialized() {
        startupData?.isLaunchedByUser = startupData?.isSplashScreenShown?: false
        startupData?.phEndTimestamp = System.currentTimeMillis()
    }

    fun setInterstitialTimeout(interstitialAdTimeout: Long) {
        startupData?.interstitialTimeout = interstitialAdTimeout
    }

    fun setPremiumHelperTimeout(timeout: Long) {
        startupData?.premiumHelperTimeout = timeout
    }

    fun setTotoConfigResult(totoResult: String) {
        if (totoResult.isDigitsOnly()) {
            val code = try {
                val responseCode = Integer.parseInt(totoResult)
                if (responseCode / 100 == 2) "success" else "$responseCode"
            } catch (ex: NumberFormatException) {
                totoResult
            }
            startupData?.totoConfigResult = code
        } else
            startupData?.totoConfigResult = totoResult
    }

    fun setRemoteConfigResult(remoteConfigResult: String) {
        startupData?.remoteConfigResult = remoteConfigResult
    }

    fun onAnalyticsStart() {
        startupData?.analyticsStartTimestamp = System.currentTimeMillis()
    }

    fun onAnalyticsEnd() {
        startupData?.analyticsEndTimestamp = System.currentTimeMillis()
    }

    fun onPurchasesStart() {
        startupData?.purchasesStartTimestamp = System.currentTimeMillis()
    }

    fun onPurchasesEnd() {
        startupData?.purchasesEndTimestamp = System.currentTimeMillis()
    }

    fun onGoogleServiceStart() {
        startupData?.googleServiceStartTimestamp = System.currentTimeMillis()
    }

    fun onGoogleServiceEnd() {
        startupData?.googleServiceEndTimestamp = System.currentTimeMillis()
    }

    fun onTestyStart() {
        startupData?.testyStartTimestamp = System.currentTimeMillis()
    }

    fun onTestyEnd() {
        startupData?.testyEndTimestamp = System.currentTimeMillis()
    }

    fun setTotoConfigCapped(value: Boolean) {
        startupData?.totoConfigCapped = value
        setTotoConfigResult("success")
    }

    fun onSplashScreenCreated() {
        startupData?.isSplashScreenShown = true
    }

    @Keep
    data class StartupData(
        var phStartTimestamp: Long = 0,
        var adManagerStartTimestamp: Long = 0,
        var adManagerEndTimeStamp: Long = 0,
        var remoteConfigStartTimestamp: Long = 0,
        var remoteConfigEndTimestamp: Long = 0,
        var totoConfigStartTimestamp: Long = 0,
        var totoConfigEndTimestamp: Long = 0,
        var adProvider: String = "",
        var applicationStartTimestamp: Long = 0,
        var phEndTimestamp: Long = 0,
        var interstitialTimeout: Long = 0,
        var premiumHelperTimeout: Long = 0,
        var remoteConfigResult: String = "",
        var totoConfigResult: String = "",
        var analyticsStartTimestamp: Long = 0,
        var analyticsEndTimestamp: Long = 0,
        var purchasesStartTimestamp: Long = 0,
        var purchasesEndTimestamp: Long = 0,
        var googleServiceStartTimestamp: Long = 0,
        var googleServiceEndTimestamp: Long = 0,
        var testyStartTimestamp: Long = 0,
        var testyEndTimestamp: Long = 0,
        var totoConfigCapped: Boolean = false,
        var isSplashScreenShown: Boolean = false,
        var isLaunchedByUser: Boolean = false

    ) : BasePerformanceDataClass() {
        fun toBundle(): Bundle {
            val now = System.currentTimeMillis()

             val isAdFraudEnabled =  PremiumHelper.getInstance().configuration.get(
                 Configuration.PREVENT_AD_FRAUD
             )
            return bundleOf(
                //Preloading
                "preloading_time" to calculateDuration(phStartTimestamp, applicationStartTimestamp),

                //Premium helper time
                "premium_helper_time" to calculateDuration(phEndTimestamp, phStartTimestamp),

                //Total
                "total_loading_time" to calculateDuration(
                    now,
                    applicationStartTimestamp
                ),

                "premium_helper_version" to BuildConfig.VERSION_NAME,
                "ads_provider" to adProvider,
                //AdManager
                "ad_manager_time" to calculateDuration(
                    adManagerEndTimeStamp,
                    adManagerStartTimestamp
                ),

                //Remote config
                "remote_config_time" to calculateDuration(
                    remoteConfigEndTimestamp,
                    remoteConfigStartTimestamp
                ),

                //Toto
                "toto_config_time" to calculateDuration(
                    totoConfigEndTimestamp,
                    totoConfigStartTimestamp
                ),
                "toto_config_capped" to booleanToString(totoConfigCapped),

                "premium_helper_timeout" to premiumHelperTimeout,
                "remote_config_result" to remoteConfigResult,
                "toto_config_result" to totoConfigResult,
                //Wait for AD
                "wait_for_ad_time" to if(isAdFraudEnabled) calculateDuration(now, phEndTimestamp) else 0,

                Configuration.AD_FRAUD_PROTECTION_TIMEOUT_SECONDS.key to interstitialTimeout,
                Configuration.PREVENT_AD_FRAUD.key to booleanToString(isAdFraudEnabled),
                "is_debug" to booleanToString(PremiumHelper.getInstance().isDebugMode()),
                //Blytics
                "blytics_time" to calculateDuration(analyticsEndTimestamp, analyticsStartTimestamp),


                //Get active purchase
                "get_active_purchases_time" to calculateDuration(
                    purchasesEndTimestamp,
                    purchasesStartTimestamp
                ),

                //TODO Not relevant after stop supporting SDK < 21
                //Google services
                "googleservices_install_time" to calculateDuration(
                    googleServiceEndTimestamp,
                    googleServiceStartTimestamp
                ),

                //Testy
                "testy_initialization_time" to calculateDuration(
                    testyEndTimestamp,
                    testyStartTimestamp
                ),
                "launched_by_user" to booleanToString(isLaunchedByUser)
            )
        }

        fun isCollectedDataValid(): Boolean {
            if ((totoConfigEndTimestamp - totoConfigStartTimestamp == 0L) && !totoConfigCapped) {
                return false
            }
            if (phStartTimestamp == 0L || phEndTimestamp == 0L || adManagerStartTimestamp == 0L ||
                adManagerEndTimeStamp == 0L || remoteConfigStartTimestamp == 0L ||
                remoteConfigEndTimestamp == 0L || applicationStartTimestamp == 0L ||
                analyticsStartTimestamp == 0L || analyticsEndTimestamp == 0L ||
                purchasesStartTimestamp == 0L || purchasesEndTimestamp == 0L
            ) return false
            if (System.currentTimeMillis() - applicationStartTimestamp > TimeUnit.MINUTES.toMillis(3))
                return false

            return true
        }
    }
}