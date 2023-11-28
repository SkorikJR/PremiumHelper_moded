package com.appboosty.ads.applovin

import com.appboosty.ads.AdUnitIdProvider
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration

class AppLovinUnitIdProvider : AdUnitIdProvider() {

    companion object {
    }

    override fun getBannerADUnit(useTestAds: Boolean): String {
        return PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_APPLOVIN_BANNER)
            .takeIf { it.isNotEmpty() } ?: ""
    }

    override fun getMRecBannerADUnit(useTestAds: Boolean): String {
        return PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_APPLOVIN_MREC_BANNER)
            .takeIf { it.isNotEmpty() } ?: ""
    }

    override fun getNativeADUnit(useTestAds: Boolean): String {
        return PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_APPLOVIN_NATIVE)
            .takeIf { it.isNotEmpty() } ?: ""
    }

    override fun getInterstitialADUnit(useTestAds: Boolean): String {
        return PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_APPLOVIN_INTERSTITIAL)
            .takeIf { it.isNotEmpty() } ?: ""
    }

    override fun getRewardedADUnit(useTestAds: Boolean): String {
        return PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_APPLOVIN_REWARDED)
            .takeIf { it.isNotEmpty() } ?: ""
    }

    override fun getExitBannerADUnit(useTestAds: Boolean): String {
        return PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_APPLOVIN_BANNER_EXIT)
            .takeIf { it.isNotEmpty() } ?: ""
    }

    override fun getExitNativeADUnit(useTestAds: Boolean): String {
        return PremiumHelper.getInstance().configuration.get(Configuration.AD_UNIT_APPLOVIN_NATIVE_EXIT)
            .takeIf { it.isNotEmpty() } ?: ""
    }
}