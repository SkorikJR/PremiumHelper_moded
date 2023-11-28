package com.appboosty.premiumhelper.ui.rate

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.appboosty.premiumhelper.Analytics
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.configuration.Configuration.Params.RATE_US_MODE
import com.appboosty.premiumhelper.configuration.Configuration.Params.RATE_US_SESSION_START
import com.appboosty.premiumhelper.configuration.Configuration.Params.SHOW_AD_ON_APP_EXIT
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import timber.log.Timber

class RateHelper(private val configuration: Configuration, private val preferences: Preferences) {

    enum class RateMode {
        NONE, ALL, VALIDATE_INTENT
    }

    enum class RateUi {
        NONE, DIALOG, IN_APP_REVIEW
    }

    private val log by timber(TAG)

    interface OnRateFlowCompleteListener {
        fun onRateFlowComplete(reviewUiShown: RateUi, negativeIntent: Boolean)
    }

    /**
     *  Check if rate UI should be shown to the user on this session.
     *  Returns type of rate UI (None, Rate Intent Dialog or In-App review) to show
     */
    internal fun shouldShowRateOnAppStart(): RateUi {

        if (!shouldShowRateThisSession()) {
            return RateUi.NONE
        }

        val rateMode = configuration.get(RATE_US_MODE)
        val appStartCounter = preferences.getAppStartCounter()

        log.i("Rate: shouldShowRateOnAppStart rateMode=$rateMode")

        return when (rateMode) {
            RateMode.VALIDATE_INTENT -> {
                log.i("Rate: shouldShowRateOnAppStart appStartCounter=$appStartCounter")
                val rateIntent = preferences.get("rate_intent", "")
                log.i("Rate: shouldShowRateOnAppStart rateIntent=$rateIntent")
                when {
                    rateIntent.isEmpty() -> {

                        val nextSession = preferences.getRateSessionNumber()

                        log.i("Rate: shouldShowRateOnAppStart nextSession=$nextSession")

                        if (appStartCounter >= nextSession) {
                            // No intent set. Show rate intent dialog to get user intent.
                            RateUi.DIALOG
                        } else {
                            RateUi.NONE
                        }
                    }
                    rateIntent == "positive" -> {
                        // User selected positive choice in rate intent dialog.
                        // Show in-app review now
                        RateUi.IN_APP_REVIEW
                    }
                    rateIntent == "negative" -> {
                        // User selected negative choice in rate intent dialog.
                        // Do not ask for rating again.
                        RateUi.NONE
                    }
                    else -> {
                        RateUi.NONE
                    }
                }
            }
            RateMode.ALL -> {
                RateUi.IN_APP_REVIEW
            }
            RateMode.NONE -> {
                RateUi.NONE
            }
        }
    }

    /**
     *  Check if rate UI should be shown to the user on this session
     */
    private fun shouldShowRateThisSession(): Boolean {
        val startSession = configuration.get(RATE_US_SESSION_START)
        val appStartCounter = preferences.getAppStartCounter()
        log.i("Rate: shouldShowRateThisSession appStartCounter=$appStartCounter, startSession=$startSession")

        return appStartCounter >= startSession
    }

    fun showRateIntentDialog(fm: FragmentManager, theme: Int = -1, fromRelaunch: Boolean = false, completeListener: OnRateFlowCompleteListener?) {
        RateDialog.show(fm, theme, fromRelaunch, completeListener)
    }

    fun showRateIntentDialog(fm: FragmentManager, theme: Int = -1, fromRelaunch: Boolean = false, completeListener: (() -> Unit)? = null) {
        showRateIntentDialog(fm, theme, fromRelaunch, object : OnRateFlowCompleteListener {
            override fun onRateFlowComplete(reviewUiShown: RateUi, negativeIntent: Boolean) {
                completeListener?.invoke()
            }
        })
    }

    internal fun showInAppReview(activity: Activity, completeListener: (() -> Unit)? = null) {
        showInAppReview(activity, object : OnRateFlowCompleteListener {
            override fun onRateFlowComplete(reviewUiShown: RateUi, negativeIntent: Boolean) {
                completeListener?.invoke()
            }
        })
    }

    fun showInAppReview(activity: Activity, completeListener: OnRateFlowCompleteListener? = null) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { response ->
            if (response.isSuccessful) {
                PremiumHelper.getInstance().analytics.onRateUsShown(Analytics.RateUsType.IN_APP_REVIEW)
                val reviewInfo = response.result
                val flowStart = System.currentTimeMillis()

                try {
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        val reviewShown = if ((System.currentTimeMillis() - flowStart) > 2000) {
                            RateUi.IN_APP_REVIEW
                        } else {
                            RateUi.NONE
                        }
                        completeListener?.onRateFlowComplete(reviewUiShown = reviewShown, negativeIntent = false)
                    }
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                    completeListener?.onRateFlowComplete(reviewUiShown = RateUi.NONE, negativeIntent = false)
                }

            } else {
                completeListener?.onRateFlowComplete(reviewUiShown = RateUi.NONE, negativeIntent = false)
            }
        }
   }

    private fun isUserIntentNegative(): Boolean {
        val rateIntent = preferences.get("rate_intent", "")
        return rateIntent == "negative"
    }

    fun showRateUi(activity: AppCompatActivity, theme: Int = -1, fromRelaunch: Boolean = false, completeListener: ((result: RateUi)->Unit)?) {
        showRateUi(activity, theme, fromRelaunch, object : OnRateFlowCompleteListener {
            override fun onRateFlowComplete(reviewUiShown: RateUi, negativeIntent: Boolean) {
                completeListener?.invoke(reviewUiShown)
            }
        })
    }

    private fun showRateUi(activity: AppCompatActivity, theme: Int = -1, fromRelaunch: Boolean = false, completeListener: OnRateFlowCompleteListener?) {

        val rateUi = shouldShowRateOnAppStart()

        log.i("Rate: showRateUi=$rateUi")

        when (rateUi) {
            RateUi.DIALOG -> showRateIntentDialog(activity.supportFragmentManager, theme, fromRelaunch, completeListener)
            RateUi.IN_APP_REVIEW -> showInAppReview(activity, completeListener)
            RateUi.NONE -> { completeListener?.onRateFlowComplete(RateUi.NONE, isUserIntentNegative()) }
        }

        if (rateUi != RateUi.NONE) {
            preferences.setRateSessionNumber(preferences.getAppStartCounter() + 3)
        }
    }

    fun isShowing(activity: Activity): Boolean {
        return if (activity is AppCompatActivity) {
            activity.supportFragmentManager.findFragmentByTag("RATE_DIALOG") != null
        } else {
            PremiumHelperUtils.errorOrCrash("Please use AppCompatActivity for ${activity.javaClass.name}")
            false
        }
    }

    /**
     *  Check conditions for showing In-App Review UI on app exit.
     *  Exit ads must be enabled, rate mode must be ALL or VALIDATE_INTENT,
     *  user intent must be positive.
     *
     *  @return true if review can be shown
     */
    fun canShowInAppReviewOnExit(): Boolean {

        if (configuration.get(SHOW_AD_ON_APP_EXIT)) {
            return when (configuration.get(RATE_US_MODE)) {
                RateMode.ALL -> true
                RateMode.VALIDATE_INTENT -> preferences.get("rate_intent", "") == "positive"
                RateMode.NONE -> false
            }
        }

        return false
    }

}