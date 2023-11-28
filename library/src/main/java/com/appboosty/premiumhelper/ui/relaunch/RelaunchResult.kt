package com.appboosty.premiumhelper.ui.relaunch

data class RelaunchResult(
    val premiumOfferingShown: Boolean = false,
    val interstitialAdShown: Boolean = false,
    val rateUiShown: Boolean = false,
    val isFirstAppStart: Boolean = false,)
