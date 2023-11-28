package com.appboosty.ads.nativead

import android.view.View
import com.appboosty.ads.PhLoadAdError

interface PhNativeAdLoadListener {
    fun onAdFailedToLoad(error: PhLoadAdError)
    fun onAdLoaded(adView: View)
}