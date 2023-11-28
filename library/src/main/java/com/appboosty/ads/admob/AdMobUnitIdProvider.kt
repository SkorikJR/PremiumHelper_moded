package com.appboosty.ads.admob

import com.appboosty.ads.AdUnitIdProvider
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration

class AdMobUnitIdProvider : AdUnitIdProvider() {

    companion object {
        private const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/8691691433"
        private const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
        private const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    override fun getBannerADUnit(useTestAds: Boolean): String {
        return if (useTestAds) BANNER_TEST_ID else
            PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_ADMOB_BANNER)
    }

    override fun getMRecBannerADUnit(useTestAds: Boolean): String {
        return getBannerADUnit(useTestAds)
    }

    override fun getNativeADUnit(useTestAds: Boolean): String {
        return if (useTestAds) NATIVE_TEST_ID else
            PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_ADMOB_NATIVE)
    }

    override fun getInterstitialADUnit(useTestAds: Boolean): String {
        return if (useTestAds) INTERSTITIAL_TEST_ID else
            PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_ADMOB_INTERSTITIAL)
    }

    override fun getRewardedADUnit(useTestAds: Boolean): String {
        return if (useTestAds) REWARDED_TEST_ID else
            PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_ADMOB_REWARDED)
    }

    override fun getExitBannerADUnit(useTestAds: Boolean): String {
        return if (useTestAds) BANNER_TEST_ID else
            PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_ADMOB_BANNER_EXIT)
    }

    override fun getExitNativeADUnit(useTestAds: Boolean): String {
        return if (useTestAds) NATIVE_TEST_ID else
            PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_ADMOB_NATIVE_EXIT)
    }
}