package com.appboosty.premiumhelper.ui.happymoment

import androidx.appcompat.app.AppCompatActivity
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.ui.rate.RateHelper
import com.appboosty.premiumhelper.util.TimeCapping

class HappyMoment(private val rateHelper: RateHelper, private val configuration: Configuration, private val preferences: Preferences) {

    /**
     *  Defines the behavior of the Happy Moment feature
     */
    enum class HappyMomentRateMode {
        /**
         *  Do not show anything on happy moment
         */
        NONE,

        /**
         *  Default behavior: validate intent dialog or in-app review with user intent logic check,
         *  interstitial ad afterwards
         *
         */
        DEFAULT,

        /**
         *  Show in-app review
         */
        IN_APP_REVIEW,

        /**
         *  Show validate intent dialog or in-app review with user intent logic check
         */
        VALIDATE_INTENT,

        /**
         *  Show in-app review followed by interstitial
         */
        IN_APP_REVIEW_WITH_AD,

        /**
         *  Show validate intent dialog followed by interstitial
         */
        VALIDATE_INTENT_WITH_AD
    }

    private val happyMomentCapping: TimeCapping by lazy {
        TimeCapping.ofSeconds(
            capping_seconds = configuration.get(Configuration.HAPPY_MOMENT_CAPPING_SECONDS),
            last_call_time = preferences.get("happy_moment_capping_timestamp", 0L),
            autoUpdate = false
        )
    }

    fun show(activity: AppCompatActivity, theme: Int = -1, callback: (() -> Unit)?) {

        when (val happyMomentRateMode = configuration.get(Configuration.HAPPY_MOMENT_RATE_MODE)) {

            /**
             *  Show default UI.
            */
            HappyMomentRateMode.DEFAULT -> runWithSkipAndCapping(
                {
                    PremiumHelper.getInstance().analytics.onHappyMoment(happyMomentRateMode)
                    showDefaultModeUi(activity, theme, callback)
                },
                { PremiumHelper.getInstance().showInterstitialAd(activity, callback) })

            /**
             *  Show in-app review UI.
             */
            HappyMomentRateMode.IN_APP_REVIEW -> runWithSkipAndCapping(
                {
                    PremiumHelper.getInstance().analytics.onHappyMoment(happyMomentRateMode)
                    rateHelper.showInAppReview(activity, callback)
                },
                { callback?.invoke()})

            /**
             *  Show validate intent dialog or in-app review.
             */
            HappyMomentRateMode.VALIDATE_INTENT -> runWithSkipAndCapping(
                {
                    PremiumHelper.getInstance().analytics.onHappyMoment(happyMomentRateMode)

                    val rateIntent = preferences.get("rate_intent", "")

                    when {
                        rateIntent.isEmpty() -> rateHelper.showRateIntentDialog(activity.supportFragmentManager,
                            theme,
                            false,
                            callback)
                        rateIntent == "positive" -> rateHelper.showInAppReview(activity, callback)
                        else -> callback?.invoke()
                    }
                },
                { callback?.invoke() })

            /**
             *  Show in-app review UI followed by interstitial.
             */
            HappyMomentRateMode.IN_APP_REVIEW_WITH_AD -> runWithSkipAndCapping(
                {
                    PremiumHelper.getInstance().analytics.onHappyMoment(happyMomentRateMode)

                    rateHelper.showInAppReview(activity) {
                        PremiumHelper.getInstance().showInterstitialAd(activity, callback)
                    }
                },
                { PremiumHelper.getInstance().showInterstitialAd(activity, callback) })

            /**
             *  Show validate intent dialog followed by interstitial.
             *  Do not show interstitial if user opened Google Play for review
             */
            HappyMomentRateMode.VALIDATE_INTENT_WITH_AD -> runWithSkipAndCapping(
                {
                    PremiumHelper.getInstance().analytics.onHappyMoment(happyMomentRateMode)

                    val rateIntent = preferences.get("rate_intent", "")

                    when {
                        rateIntent.isEmpty() -> rateHelper.showRateIntentDialog(activity.supportFragmentManager,
                            theme, false,
                            object : RateHelper.OnRateFlowCompleteListener {
                                override fun onRateFlowComplete(
                                    reviewUiShown: RateHelper.RateUi,
                                    negativeIntent: Boolean,
                                ) {
                                    if (reviewUiShown == RateHelper.RateUi.NONE) {
                                        PremiumHelper.getInstance()
                                            .showInterstitialAd(activity, callback)
                                    } else {
                                        callback?.invoke()
                                    }
                                }
                            })

                        rateIntent == "positive" -> rateHelper.showInAppReview(activity) {
                            PremiumHelper.getInstance().showInterstitialAd(activity, callback)
                        }

                        else -> PremiumHelper.getInstance()
                            .showInterstitialAd(activity, callback)
                    }
                },
                { PremiumHelper.getInstance().showInterstitialAd(activity, callback) })

            HappyMomentRateMode.NONE -> callback?.invoke()
        }
    }

    private fun runWithSkipAndCapping(onSuccess: () -> Unit, onCapped: () -> Unit) {

        val happyMomentCounter = preferences.get("happy_moment_counter", 0L)

        if (happyMomentCounter >= configuration.get(Configuration.HAPPY_MOMENT_SKIP_FIRST)) {
            happyMomentCapping.runWithCapping({
                happyMomentCapping.update()
                if (configuration.get(Configuration.HAPPY_MOMENT_CAPPING_TYPE) == Configuration.CappingType.GLOBAL) {
                    preferences.set("happy_moment_capping_timestamp", System.currentTimeMillis())
                }
                onSuccess()
            }, onCapped)
        } else {
            onCapped()
        }

        preferences.set("happy_moment_counter", happyMomentCounter + 1)
    }

    /**
     *  Show validate intent dialog OR in-app review based on the rate-mode
     *  followed by interstitial.
     *  Do not show interstitial if user opened Google Play for review.
     */
    private fun showDefaultModeUi(activity: AppCompatActivity, theme: Int, callback: (() -> Unit)?) {
        val rateUi = when (configuration.get(Configuration.RATE_US_MODE)) {
            RateHelper.RateMode.VALIDATE_INTENT -> {
                val rateIntent = preferences.get("rate_intent", "")
                when {
                    rateIntent.isEmpty() -> RateHelper.RateUi.DIALOG
                    rateIntent == "positive" -> RateHelper.RateUi.IN_APP_REVIEW
                    rateIntent == "negative" -> RateHelper.RateUi.NONE
                    else -> RateHelper.RateUi.NONE
                }
            }
            RateHelper.RateMode.ALL -> RateHelper.RateUi.IN_APP_REVIEW
            RateHelper.RateMode.NONE -> RateHelper.RateUi.NONE
        }

        when (rateUi) {
            RateHelper.RateUi.DIALOG -> rateHelper.showRateIntentDialog(
                activity.supportFragmentManager,
                theme,
                false,
                object : RateHelper.OnRateFlowCompleteListener {
                    override fun onRateFlowComplete(reviewUiShown: RateHelper.RateUi, negativeIntent: Boolean) {
                        if (reviewUiShown == RateHelper.RateUi.NONE) {
                            PremiumHelper.getInstance().showInterstitialAd(activity, callback)
                        } else {
                            callback?.invoke()
                        }
                    }
                })

            RateHelper.RateUi.IN_APP_REVIEW -> rateHelper.showInAppReview(activity) {
                PremiumHelper.getInstance().showInterstitialAd(activity, callback)
            }

            RateHelper.RateUi.NONE -> PremiumHelper.getInstance().showInterstitialAd(activity, callback)
        }
    }

    fun updateHappyMomentCapping() {
        happyMomentCapping.update()
    }


}