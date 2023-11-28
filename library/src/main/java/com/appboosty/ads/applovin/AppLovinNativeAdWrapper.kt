package com.appboosty.ads.applovin

import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader

data class AppLovinNativeAdWrapper(val adLoader: MaxNativeAdLoader, val nativeAd: MaxAd)