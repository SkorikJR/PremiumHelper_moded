package com.appboosty.ads

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatActivity

interface InterstitialManager {
    fun loadInterstitial(activity: Activity, adUnitIdProvider: AdUnitIdProvider, useTestAds: Boolean)
    fun isInterstitialLoaded(): Boolean
    fun clearInterstitials()
    suspend fun waitForInterstitial(timeout: Long): Boolean
    fun showInterstitialAd(
        activity: Activity,
        callback: PhFullScreenContentCallback?,
        delayed: Boolean = false,
        application: Application,
        adUnitIdProvider: AdUnitIdProvider,
        useTestAds: Boolean
    )

    companion object{
        const val INTERSTITIAL_DELAY = 1000L //millis
    }
}