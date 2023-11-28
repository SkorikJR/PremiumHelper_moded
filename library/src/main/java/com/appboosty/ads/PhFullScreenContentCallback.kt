package com.appboosty.ads


abstract class PhFullScreenContentCallback() {

    open fun onAdFailedToShowFullScreenContent( error: PhAdError?) {}

    open fun onAdShowedFullScreenContent() {}

    open fun onAdDismissedFullScreenContent() {}

    open fun onAdImpression() {}

    open fun onAdClicked() {}
}