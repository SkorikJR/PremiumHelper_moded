package com.appboosty.ads.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class AdMobInterstitialProvider(private val adUnitId: String) {

    suspend fun load(context: Context) : PHResult<InterstitialAd> {
        return suspendCancellableCoroutine { cont->
            try {
                InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Timber.tag(TAG).d("AdMobInterstitial: loaded ad from ${ad.responseInfo.mediationAdapterClassName}")
                        if (cont.isActive) {
                            ad.onPaidEventListener = OnPaidEventListener { adValue -> PremiumHelper.getInstance().analytics.onPaidImpression(adUnitId, adValue, ad.responseInfo.mediationAdapterClassName) }
                            cont.resume(PHResult.Success(ad))
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {

                        Timber.tag(TAG).e("AdMobInterstitial: Failed to load ${error.code} (${error.message})")
                        AdsErrorReporter.reportAdErrorAsync(context, "interstitial", error.message)
                        if (cont.isActive) {
                            cont.resume(PHResult.Failure(IllegalStateException(error.message)))
                        }
                    }
                })
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }
        }
    }
}