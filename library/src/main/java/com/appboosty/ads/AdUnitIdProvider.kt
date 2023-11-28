package com.appboosty.ads

abstract class AdUnitIdProvider {

    abstract fun getBannerADUnit(useTestAds: Boolean): String
    abstract fun getMRecBannerADUnit(useTestAds: Boolean): String
    abstract fun getNativeADUnit(useTestAds: Boolean): String
    abstract fun getInterstitialADUnit(useTestAds: Boolean): String
    abstract fun getRewardedADUnit(useTestAds: Boolean): String
    abstract fun getExitBannerADUnit(useTestAds: Boolean): String
    abstract fun getExitNativeADUnit(useTestAds: Boolean): String

    fun getAdUnitId(
        adType: com.appboosty.ads.AdManager.AdType,
        isExitAd: Boolean = false,
        useTestAds: Boolean
    ): String {
        val adUnitId = when (adType) {
            com.appboosty.ads.AdManager.AdType.INTERSTITIAL -> getInterstitialADUnit(useTestAds)

            com.appboosty.ads.AdManager.AdType.BANNER -> {
                if (isExitAd) {
                    getExitBannerADUnit(useTestAds).ifEmpty { getBannerADUnit(useTestAds) }
                } else {
                    getBannerADUnit(useTestAds)
                }
            }

            com.appboosty.ads.AdManager.AdType.BANNER_MEDIUM_RECT -> {
                if (isExitAd) {
                    getExitBannerADUnit(useTestAds).ifEmpty { getMRecBannerADUnit(useTestAds) }
                } else {
                    getMRecBannerADUnit(useTestAds)
                }
            }

            com.appboosty.ads.AdManager.AdType.NATIVE -> {
                if (isExitAd) {
                    getExitNativeADUnit(useTestAds).ifEmpty { getNativeADUnit(useTestAds) }
                } else {
                    getNativeADUnit(useTestAds)
                }
            }

            com.appboosty.ads.AdManager.AdType.REWARDED -> getRewardedADUnit(useTestAds)

        }
        return adUnitId
    }
}