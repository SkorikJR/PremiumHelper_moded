package com.appboosty.sampleapp

import android.content.Context
import android.os.Bundle
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.appboosty.ads.config.adManagerConfig
import com.appboosty.blytics.PaidImpressionListener
import com.appboosty.premiumhelper.Premium
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.configuration.appconfig.PremiumHelperConfiguration
import com.appboosty.premiumhelper.ui.rate.RateHelper
import com.appboosty.sample.BuildConfig
import com.appboosty.sample.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SampleApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        val admobConfig = adManagerConfig {
            bannerAd("ca-app-pub-3940256099942544/6300978111")
            interstitialAd("ca-app-pub-3940256099942544/8691691433")
            rewardedAd("ca-app-pub-3940256099942544/5224354917")
            nativeAd("ca-app-pub-3940256099942544/2247696110")
            exitBannerAd("ca-app-pub-3940256099942544/6300978111")
            exitNativeAd("ca-app-pub-3940256099942544/1044960115")
            if(BuildConfig.DEBUG) testAdvertisingIds(arrayListOf("D54F00F4960597447188425203174CD9", "41F32455B7F7D449A816356B76FC4617"))
        }

        val applovinConfig = adManagerConfig {
            bannerAd("ccdf9096d83c0774")
            bannerMRecAd("4d4c8fbe72df4f93")
            interstitialAd("3c729fc9beaa759e")
            rewardedAd("11111111")
            nativeAd("62badc180cd9100e")
            if(BuildConfig.DEBUG) testAdvertisingIds(arrayListOf("dba6e3a2-c91b-4fc6-b2f6-1ce73b953072"))
        }

        Premium.initialize(this@SampleApplication, PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
            .mainActivityClass(MainActivity::class.java)
          //  .introActivityClass(IntroActivity::class.java)
            .rateDialogMode(RateHelper.RateMode.VALIDATE_INTENT)
            .rateSessionStart(2)
            //.set(Configuration.HAPPY_MOMENT_RATE_MODE, RateHelper.HappyMomentRateMode.VALIDATE_INTENT_WITH_AD)
//            .startLikeProActivityLayout(R.layout.activity_start_like_pro_or_try_limited)
            .startLikeProActivityLayout(R.layout.activity_start_like_pro_x_to_close)
            // For A/B testing
//            .startLikeProActivityLayout(R.layout.activity_start_like_pro, R.layout.activity_start_like_pro_a, R.layout.activity_start_like_pro_b)
            .relaunchPremiumActivityLayout(R.layout.activity_relaunch_premium)
            .relaunchOneTimeActivityLayout(R.layout.activity_relaunch_premium_one_time)
            .adManagerConfiguration(admobConfig, applovinConfig)
            .admobConfiguration(admobConfig)
            .applovinConfiguration(applovinConfig)
            .setInterstitialMuted(false)
            .useTestAds(false)
            .showExitConfirmationAds(true)
            .showTrialOnCta(true)
            .termsAndConditionsUrl("https://appboosty.com/Sample/Terms")
            .privacyPolicyUrl("https://appboosty.com/Sample/Privacy")
            .configureMainOffer("test_premium_v1_trial_7d_yearly")
//            .useTestLayouts(true)
//            .configureOneTimeOffer("test_premium_v1_trial_7d_yearly", "test_premium_v1_trial_7d")
            .setInterstitialCapping(20)
            .set(Configuration.DISABLE_ONBOARDING_OFFERING, true)
            .set(Configuration.SHOW_ONBOARDING_INTERSTITIAL, false)
            .set(Configuration.RATE_US_SESSION_START, 3)
            .set(Configuration.TOTO_ENABLED, false)
            .set(Configuration.PREVENT_AD_FRAUD, false)
            .set(Configuration.TOTOLYTICS_ENABLED, true)
            .setHappyMomentSkipFirst(2)
            .setPremiumPackages("ringtone.maker", "ringtone.maker.pro")
            .setAdsProvider(Configuration.AdsProvider.ADMOB)
            .build())

        PremiumHelper.getInstance().setExternalOnPaidImpressionListener(object:
            PaidImpressionListener {
            override fun onPaidImpression(params: Bundle) {

            }
        })

        CoroutineScope(Dispatchers.Default).launch {
            PremiumHelper.getInstance().observePurchaseResult().collect { result->

            }
        }

        Premium.Debug.addMainOffer("test_premium_v1_trial_7d_yearly", "$123")
        //Premium.Debug.addOneTimeOffer("test_premium_v1_trial_7d", "$123", "test_premium_v1_trial_7d", "$456")
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

}