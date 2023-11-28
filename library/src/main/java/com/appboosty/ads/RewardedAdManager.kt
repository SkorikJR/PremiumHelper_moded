package com.appboosty.ads

import android.app.Activity
import android.app.Application

interface RewardedAdManager {
    fun loadRewardedAd(activity: Activity, adUnitIdProvider: AdUnitIdProvider, useTestAds: Boolean, listener: PhAdListener?)
    fun showRewardedAd(
        application: Application,
        adUnitIdProvider: AdUnitIdProvider,
        useTestAds: Boolean,
        activity: Activity,
        rewardedAdCallback: PhOnUserEarnedRewardListener,
        callback: PhFullScreenContentCallback
    )
}