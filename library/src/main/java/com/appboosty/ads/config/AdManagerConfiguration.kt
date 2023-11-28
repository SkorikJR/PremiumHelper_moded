package com.appboosty.ads.config

data class AdManagerConfiguration(
    val banner: String? = null,
    val bannerMRec: String? = null,
    val interstitial: String,
    val native: String? = null,
    val rewarded: String? = null,
    val exit_banner: String? = null,
    val exit_native: String? = null,
    val testAdvertisingIds: List<String>?
) {

    data class Builder(
        private var banner: String? = null,
        private var bannerMRec: String? = null,
        private var interstitial: String? = null,
        private var native: String? = null,
        private var rewarded: String? = null,
        private var exit_banner: String? = null,
        private var exit_native: String? = null,
        private var testAdvertisingIds: List<String>? = null
    ) {

        /**
         * Ad unit id for banners (AdMob/AppLovin)
         */
        fun bannerAd(bannerId: String) = apply { this.banner = bannerId }

        /**
         * This adUnitId is only applicable to AppLovin ads provider. Has no effect on
         * AdMob.
         */
        fun bannerMRecAd(mRecId: String) = apply { this.bannerMRec = mRecId }
        /**
         * Ad unit id for interstitials (AdMob/AppLovin)
         */
        fun interstitialAd(interstitialId: String) = apply { this.interstitial = interstitialId }
        /**
         * Ad unit id for native ads (AdMob/AppLovin)
         */
        fun nativeAd(nativeId: String) = apply { this.native = nativeId }
        /**
         * Ad unit id for rewarded ads (AdMob/AppLovin)
         */
        fun rewardedAd(rewardedId: String) = apply { this.rewarded = rewardedId }
        /**
         * Ad unit id for exit banner (AdMob/AppLovin)
         */
        fun exitBannerAd(bannerId: String) = apply { this.exit_banner = bannerId }
        /**
         * Ad unit id for exit native ad (AdMob/AppLovin)
         */
        fun exitNativeAd(nativeId: String) = apply { this.exit_native = nativeId }

        /**
         * This parameters are for debug purposes only and should not be used in release
         * This list can be used for two different purposes
         * 1) Add test advertisement ids for AppLovin ads provider (Get test ads)
         * 2) Add debug phone hash for testing "Consent management" (See the article in Wiki)
         */
        fun testAdvertisingIds(ids: List<String>) = apply { this.testAdvertisingIds = ids }

        fun build(): AdManagerConfiguration {
            val interstitialId = interstitial ?: error("Interstitial unit Id is mandatory")
            val bannerId = banner ?: error("Banner unit Id is mandatory")
            val nativeID = native ?: error("Native unit Id is mandatory")
            val rewardedId = rewarded?: error("Rewarded unit Id is mandatory")
            return AdManagerConfiguration(bannerId, bannerMRec, interstitialId, nativeID, rewardedId, exit_banner, exit_native, testAdvertisingIds)
        }
    }
}

inline fun adManagerConfig(buildConfig: AdManagerConfiguration.Builder.() -> Unit): AdManagerConfiguration {
    val builder = AdManagerConfiguration.Builder()
    builder.buildConfig()
    return builder.build()
}
