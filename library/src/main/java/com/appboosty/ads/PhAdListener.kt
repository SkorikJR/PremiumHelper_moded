package com.appboosty.ads

abstract class PhAdListener() {

    open fun onAdClosed() {}

    open fun onAdFailedToLoad(error: PhLoadAdError) {}

    open fun onAdOpened() {}

    open fun onAdLoaded() {}

    open fun onAdClicked() {}

    open fun onAdImpression() {}

    companion object{
        const val UNDEFINED_DOMAIN = "undefined"
    }
}