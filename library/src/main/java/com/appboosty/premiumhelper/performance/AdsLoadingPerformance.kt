package com.appboosty.premiumhelper.performance

import androidx.core.os.bundleOf
import com.appboosty.premiumhelper.PremiumHelper
import timber.log.Timber

class AdsLoadingPerformance private constructor() : BaseTracker() {
    companion object {
        private const val TAG = "AdsLoadingPerformance"
        private var instance: AdsLoadingPerformance? = null

        fun getInstance(): AdsLoadingPerformance {
            return instance ?: run {
                instance = AdsLoadingPerformance()
                instance!!
            }
        }
    }

    private var bannerCounter = 0
    private var interstitialCounter = 0
    private var nativeAdsCounter = 0
    private var rewardedAdsCounter = 0

    fun onStartLoadingBanner() {
        bannerCounter++
    }

    fun onStartLoadingRewardedAd(){
        rewardedAdsCounter++
    }

    fun onStartInterstitialLoading() {
        interstitialCounter++
    }

    fun onStartNativeAdLoading() {
        nativeAdsCounter++
    }

    fun onEndNativeAdLoading(duration: Long) {
        sendEvent {
            val params = bundleOf(
                "native_ad_loading_time" to duration,
                "native_ads_count" to nativeAdsCounter,
                "ads_provider" to PremiumHelper.getInstance().getCurrentAdsProvider().name
            )
            Timber.tag(TAG).d(params.toString())
            PremiumHelper.getInstance().analytics.sendNativeAdsPerformanceData(params)
        }
    }

    fun onEndInterstitialLoading(duration: Long) {
        sendEvent {
            val params = bundleOf(
                "interstitial_loading_time" to duration,
                "interstitials_count" to interstitialCounter,
                "ads_provider" to PremiumHelper.getInstance().getCurrentAdsProvider().name
            )
            Timber.tag(TAG).d(params.toString())
            PremiumHelper.getInstance().analytics.sendInterstitialPerformanceData(params)
        }
    }

    fun onEndLoadingBanner(duration: Long) {
        sendEvent {
            val params = bundleOf(
                "banner_loading_time" to duration,
                "banner_count" to bannerCounter,
                "ads_provider" to PremiumHelper.getInstance().getCurrentAdsProvider().name
            )
            Timber.tag(TAG).d(params.toString())
            PremiumHelper.getInstance().analytics.sendBannersPerformanceData(params)
        }
    }

    fun onEndLoadingRewardedAd(duration: Long){
        sendEvent {
            val params = bundleOf(
                "rewarded_loading_time" to duration,
                "rewarded_count" to bannerCounter,
                "ads_provider" to PremiumHelper.getInstance().getCurrentAdsProvider().name
            )
            Timber.tag(TAG).d(params.toString())
            PremiumHelper.getInstance().analytics.sendRewardedAdPerformanceData(params)
        }
    }

}