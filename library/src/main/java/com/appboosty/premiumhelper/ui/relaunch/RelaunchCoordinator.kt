package com.appboosty.premiumhelper.ui.relaunch

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.ProxyBillingActivity
import com.appboosty.ads.PhAdError
import com.appboosty.ads.PhFullScreenContentCallback
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.isAdActivity
import com.appboosty.premiumhelper.isIntroActivity
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.ui.rate.RateHelper
import com.appboosty.premiumhelper.util.getCustomTheme
import com.appboosty.premiumhelper.ui.splash.PHSplashActivity
import com.appboosty.premiumhelper.ui.startlikepro.StartLikeProActivity
import com.appboosty.premiumhelper.update.UpdateManager
import com.appboosty.premiumhelper.util.ActivityLifecycleCallbacksAdapter
import com.appboosty.premiumhelper.util.ActivityLifecycleListener
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import com.appboosty.premiumhelper.util.PremiumHelperUtils.doIfCompat
import com.appboosty.premiumhelper.util.doOnNextNonAdActivityResume

class RelaunchCoordinator(private val application: Application, private val preferences: Preferences, private val configuration: Configuration) {

    private val log by timber(TAG)

    private var isRelaunchComplete = false
    private var premiumOfferingShown: Boolean = false
    private var interstitialAdShown: Boolean = false
    private var rateUiShown: Boolean = false

    interface NoRelaunchActivity

    companion object {
        const val ONE_TIME_OFFER_TIME_MS = DateUtils.DAY_IN_MILLIS
        const val SOURCE_RELAUNCH = "relaunch"
        const val SOURCE_ONBOARDING = "relaunch"

        fun showOffering(activity: Activity, source: String = "", theme: Int = -1) {

            val intent = Intent(activity, RelaunchPremiumActivity::class.java)
                .putExtra(RelaunchPremiumActivity.ARG_SOURCE, source)
                .putExtra(RelaunchPremiumActivity.ARG_THEME, theme)

            activity.startActivity(intent)
        }

        fun showOfferingNewTask(context: Context, source: String = "", theme: Int = -1, flags: Int = -1) {

            val intent = Intent(context, RelaunchPremiumActivity::class.java)
                .putExtra(RelaunchPremiumActivity.ARG_SOURCE, source)
                .putExtra(RelaunchPremiumActivity.ARG_THEME, theme)

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (flags != -1) {
                intent.addFlags(flags)
            }

            context.startActivity(intent)
        }
    }

    private fun isPremiumOfferingAvailable(activity: Activity): Boolean {

        if (preferences.hasActivePurchase()) {
            log.i("Relaunch: app is premium")
            return false
        }

        if (!isRelaunchLayoutDefined()) {
            log.e("Relaunch activity layout is not defined")
            return false
        }

        if (configuration.get(Configuration.DISABLE_RELAUNCH_OFFERING)) {
            log.i("Relaunch: offering is disabled by configuration")
            return false
        }

        return isOneTimeOfferAvailable() || checkRelaunchCapping(activity)
    }

    internal fun isOneTimeOfferAvailable(): Boolean {
        if (preferences.getAppStartCounter() >= configuration.get(Configuration.ONETIME_START_SESSION) &&
            configuration.get(Configuration.ONETIME_OFFER).isNotEmpty()) {
            return !isOneTimeOfferExpired()
        }

        return false
    }

    internal fun onOneTimeOfferShown() {
        if (preferences.getOneTimeOfferStartTime() == 0L) {
            preferences.setOneTimeOfferStartTime(System.currentTimeMillis())
        }
    }

    private fun isOneTimeOfferExpired(): Boolean {
        val start = preferences.getOneTimeOfferStartTime()
        return start > 0 && (start + ONE_TIME_OFFER_TIME_MS) < System.currentTimeMillis()
    }

    private fun checkRelaunchCapping(context: Context): Boolean {

        val counter: Int = preferences.getRelaunchPremiumOfferingCounter()
        val daysFromInstall = PremiumHelperUtils.getDaysSinceInstall(context)

        log.i("Relaunch: checkRelaunchCapping: counter=$counter, daysFromInstall=$daysFromInstall")

        val result = when (daysFromInstall) {
            0 -> {
                // Day 1 - Show three times
                counter < 3
            }
            1 -> {
                // Day 2 - Show two times (3 times from day one plus 2 times)
                counter < 5
            }
            else -> {
                // Show once every three days
                if (daysFromInstall % 3 == 0) {
                    (counter <= daysFromInstall / 3 + 4).also {
                        if (it) {
                            // In case the counter value was smaller
                            // update the counter so that the relaunch is shown only once per day
                            preferences.setRelaunchPremiumOfferingCounter(daysFromInstall / 3 + 4)
                        }
                    }
                } else {
                    false
                }
            }
        }

        if (result) preferences.incrementRelaunchPremiumOfferingCounter()

        log.i("Relaunch: Showing relaunch: $result")

        return result
    }

    private fun isRelaunchLayoutDefined(): Boolean {
        return if (isOneTimeOfferAvailable()) {
            configuration.getRelaunchOneTimeLayout() != 0
        } else {
            configuration.getRelaunchLayout() != 0
        }
    }

    fun onAppOpened() {

        val appStartCounter = if (shouldIncrementAppStartCounter()) {
            preferences.incrementAppStartCounter()
        } else {
            0
        }

        isRelaunchComplete = false
        premiumOfferingShown = false
        interstitialAdShown = false
        rateUiShown = false

        if (!preferences.hasActivePurchase()) {
            if (appStartCounter > 0) {
                if (configuration.get(Configuration.SHOW_RELAUNCH_ON_RESUME)) {
                    handleRelaunchOnResume()
                } else {
                    handleRelaunchOnColdStart()
                }
            } else if (configuration.get(Configuration.SHOW_ONBOARDING_INTERSTITIAL)) {
                showInterstitialAfterOnboarding()
            } else if (configuration.get(Configuration.RATE_US_SESSION_START) == 0L) {
                showRateUi(true)
            } else {
                onRelaunchComplete(afterOnboarding = true)
            }
        } else {
            showRateUi(appStartCounter == 0)
        }

    }

    private fun shouldIncrementAppStartCounter(): Boolean {
        return when {
            !preferences.isOnboardingComplete() -> false
            preferences.getAppStartCounter() > 0 -> true
            !PremiumHelper.getInstance().isIntroComplete() -> false
            else -> true
        }
    }

    private fun showRateUi(afterOnboarding: Boolean) {
        application.registerActivityLifecycleCallbacks(createOnResumeListener { activity, callbacks ->
            if (activity is AppCompatActivity && activity.isAllowedForRelaunch()) {
                if (activity.intent?.getBooleanExtra(PremiumHelper.FLAG_SHOW_RELAUNCH, true) != false) {
                    PremiumHelper.getInstance().rateHelper.showRateUi(activity, activity.getCustomTheme(), true) { result ->
                        rateUiShown = result != RateHelper.RateUi.NONE
                        onRelaunchComplete(activity, afterOnboarding)
                    }
                } else {
                    onRelaunchComplete(activity, afterOnboarding)
                }
            } else {
                onRelaunchComplete(activity)
            }
            application.unregisterActivityLifecycleCallbacks(callbacks)
        })
    }

    private fun showInterstitialAfterOnboarding() {
        application.registerActivityLifecycleCallbacks(createOnResumeListener { activity, callbacks ->
            if (activity.isAllowedForRelaunch()) {
                if (activity is AppCompatActivity) {
                    showInterstitial(activity, SOURCE_ONBOARDING) {
                        PremiumHelper.getInstance().rateHelper.showRateUi(activity, activity.getCustomTheme(), true) { result ->
                            rateUiShown = result != RateHelper.RateUi.NONE
                            onRelaunchComplete(activity, true)
                        }
                    }
                } else {
                    onRelaunchComplete(activity, true)
                    PremiumHelperUtils.errorOrCrash("Please use AppCompatActivity for ${activity.javaClass.name}")
                }
            }
            if(activity !is ProxyBillingActivity)
                application.unregisterActivityLifecycleCallbacks(callbacks)
        })
    }

    private fun handleRelaunchOnColdStart() {
        var relaunchLifecycleListener: ActivityLifecycleListener? = null

        relaunchLifecycleListener = ActivityLifecycleListener(
            configuration.appConfig.mainActivityClass,
            object : ActivityLifecycleCallbacksAdapter() {

                private var handleRelaunch = false

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (savedInstanceState == null) {
                        handleRelaunch = true
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    if (handleRelaunch) {
                        activity.doIfCompat {
                            onRelaunch(it)
                        }
                    }

                    application.unregisterActivityLifecycleCallbacks(relaunchLifecycleListener)
                }
            }
        )

        application.registerActivityLifecycleCallbacks(relaunchLifecycleListener)
    }

    private fun handleRelaunchOnResume() {
        application.registerActivityLifecycleCallbacks(createOnResumeListener { activity, callbacks ->
            if (activity.isAllowedForRelaunch()) {
                if (activity is AppCompatActivity) {
                    onRelaunch(activity)
                } else {
                    onRelaunchComplete(activity)
                    PremiumHelperUtils.errorOrCrash("Please use AppCompatActivity for ${activity.javaClass.name}")
                }
            } else {
                onRelaunchComplete(activity)
            }
            application.unregisterActivityLifecycleCallbacks(callbacks)
        })
    }

    /**
     *  Check if none of the relaunch steps is already running
     *   - Relaunch activity
     *   - Interstitial ad
     *   - Rate dialog is opened
     *
     *   @return true - if relaunch can be started with this activity
     */
    private fun Activity.isAllowedForRelaunch() : Boolean {
        return when {
            this is ProxyBillingActivity -> false
            this is RelaunchPremiumActivity ||
                    isAdActivity() ||
                    this is AppCompatActivity && PremiumHelper.getInstance().rateHelper.isShowing(this) -> false
            else -> true
        }
    }

    fun handleRelaunchClose() {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
            override fun onActivityResumed(activity: Activity) {
                if (!activity.isAdActivity()) {
                    application.unregisterActivityLifecycleCallbacks(this)
                    activity.doIfCompat {
                        when (PremiumHelper.getInstance().rateHelper.shouldShowRateOnAppStart()) {

                            RateHelper.RateUi.DIALOG -> PremiumHelper.getInstance().rateHelper.showRateUi(it, activity.getCustomTheme(), true) { result ->
                                rateUiShown = result != RateHelper.RateUi.NONE
                                onRelaunchComplete(activity)
                            }

                            RateHelper.RateUi.IN_APP_REVIEW, RateHelper.RateUi.NONE ->
                                showInterstitial(activity, SOURCE_RELAUNCH) { onInterstitialComplete(it) }
                        }
                    }
                }
            }
        })

    }

    private fun onInterstitialComplete(activity: AppCompatActivity) {
        PremiumHelper.getInstance().rateHelper.showRateUi(activity, activity.getCustomTheme(), true) { result ->
            rateUiShown = result != RateHelper.RateUi.NONE
            onRelaunchComplete(activity)
        }
    }

    private fun onRelaunchComplete(activity: Activity? = null, afterOnboarding: Boolean = false) {
        if (!isRelaunchComplete) {
            isRelaunchComplete = true

            val result = RelaunchResult(premiumOfferingShown, interstitialAdShown, rateUiShown, afterOnboarding)

            if (activity is OnRelaunchListener) {
                activity.onRelaunchComplete(result)
            } else {
                application.registerActivityLifecycleCallbacks(createOnResumeListener { act, callbacks ->
                    if (act is OnRelaunchListener) {
                        act.onRelaunchComplete(result)
                        application.unregisterActivityLifecycleCallbacks(callbacks)
                    }
                })
            }

            if (activity != null) {
                UpdateManager.checkForUpdate(activity)
            } else {
                application.doOnNextNonAdActivityResume { UpdateManager.checkForUpdate(it) }
            }
        }
    }

    /**
     *  Perform actions on app start.
     *  Premium Helper can show relaunch premium offer, rate dialog or ask application to show interstitial ad.
     */
    private fun onRelaunch(activity: AppCompatActivity) {

        activity.intent?.let { intent ->
            if (intent.hasExtra(PremiumHelper.FLAG_SHOW_RELAUNCH) && !intent.getBooleanExtra(
                    PremiumHelper.FLAG_SHOW_RELAUNCH, true)) {
                onRelaunchComplete(activity)
                return
            }
        }

        log.d("Starting Relaunch")

        // Determine what to show on app start
        // - Check relaunch premium first
        // - Check if rate dialog could be shown
        // - Show interstitial if nothing else was shown
        if (isPremiumOfferingAvailable(activity)) {

            // Showing relaunch premium offering
            showOffering(activity, SOURCE_RELAUNCH, activity.getCustomTheme())
            premiumOfferingShown = true

        } else {
            when (PremiumHelper.getInstance().rateHelper.shouldShowRateOnAppStart()) {
                RateHelper.RateUi.DIALOG -> PremiumHelper.getInstance().rateHelper.showRateUi(activity, activity.getCustomTheme(), true) { result ->
                    PremiumHelper.getInstance().updateHappyMomentCapping()
                    rateUiShown = result != RateHelper.RateUi.NONE
                    onRelaunchComplete(activity)
                }
                RateHelper.RateUi.IN_APP_REVIEW, RateHelper.RateUi.NONE ->
                    showInterstitial(activity, SOURCE_RELAUNCH) { onInterstitialComplete(activity) }
            }
        }
    }

    private fun showInterstitial(activity: Activity, source: String, completeCallback: () -> Unit) {
        if (!preferences.hasActivePurchase()) {

            val isInterstitialReady = PremiumHelper.getInstance().isInterstitialLoaded()

            if (!isInterstitialReady) {
                onRelaunchComplete(activity)
            }

            PremiumHelper.getInstance().showInterstitialAd(
                activity,
                object : PhFullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(error: PhAdError?) {
                        completeCallback.invoke()
                    }

                    override fun onAdClicked() {
                        PremiumHelper.getInstance().analytics.onAdClick(
                            com.appboosty.ads.AdManager.AdType.INTERSTITIAL,
                            source)
                    }

                    override fun onAdShowedFullScreenContent() {
                        interstitialAdShown = true
                        PremiumHelper.getInstance().analytics.onAdShown(com.appboosty.ads.AdManager.AdType.INTERSTITIAL, source)
                    }

                    override fun onAdDismissedFullScreenContent() {
                        completeCallback.invoke()
                    }
                }, !isInterstitialReady, false)
        } else {
            completeCallback.invoke()
        }
    }

    /**
     *  Create onResume lifecycle callback listener
     *  Callback is called only for activities which are NOT:
     *   - Splash
     *   - StartLikePro
     *   - Intro
     *   = NoRelaunchActivity
     */
    private fun createOnResumeListener(action: (activity: Activity, callbacks: Application.ActivityLifecycleCallbacks) -> Unit): Application.ActivityLifecycleCallbacks {
        return object : ActivityLifecycleCallbacksAdapter() {
            override fun onActivityResumed(activity: Activity) {
                if (activity !is PHSplashActivity && activity !is StartLikeProActivity && !activity.isIntroActivity() && activity !is NoRelaunchActivity) {
                    action(activity, this)
                }
            }
        }
    }
}