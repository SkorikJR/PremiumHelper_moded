package com.appboosty.ads.applovin

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdLoader

abstract class PhMaxNativeAdListener {

    open fun onNativeAdLoaded(loader: MaxNativeAdLoader, ad: MaxAd?) {}

    open fun onNativeAdLoadFailed(var1: String?, error: MaxError?) {}

    open fun onNativeAdClicked(ad: MaxAd?) {}

    open fun onNativeAdExpired(ad: MaxAd?) {}
}