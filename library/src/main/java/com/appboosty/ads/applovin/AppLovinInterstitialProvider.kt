package com.appboosty.ads.applovin

import android.app.Activity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class AppLovinInterstitialProvider(private val adUnitId: String) {

    suspend fun load(activity: Activity): PHResult<MaxInterstitialAd> {
        return suspendCancellableCoroutine { cont ->
            try {
                val interstitialAd = MaxInterstitialAd(adUnitId, activity)
                interstitialAd.setRevenueListener { ad->
                    PremiumHelper.getInstance()
                        .analytics.onPaidImpression(AppLovinRevenueHelper.convertParameters(ad))
                }

                interstitialAd.setListener(object : MaxAdListener {
                    override fun onAdLoaded(ad: MaxAd?) {
                        Timber.tag(PremiumHelper.TAG)
                            .d("AppLovinInterstitialProvider: loaded ad ID ${ad?.dspId}")
                        if (cont.isActive) {
                            ad?.let { cont.resume(PHResult.Success(interstitialAd)) } ?: run {
                                cont.resume(PHResult.Failure(IllegalStateException("AppLovinInterstitialProvider: The ad is empty !")))
                            }
                        }
                    }

                    override fun onAdDisplayed(ad: MaxAd?) {
                    }

                    override fun onAdHidden(ad: MaxAd?) {
                    }

                    override fun onAdClicked(ad: MaxAd?) {
                    }

                    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                        Timber.tag(PremiumHelper.TAG)
                            .e("AppLovinInterstitialProvider: Failed to load $error")
                        AdsErrorReporter.reportAdErrorAsync(activity, "interstitial", error?.message)
                        if (cont.isActive) {
                            cont.resume(PHResult.Failure(IllegalStateException("AppLovinInterstitialProvider: Can't load ad. Error code: ${error?.code} Message - ${error?.message}")))
                        }
                    }

                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                        Timber.tag(PremiumHelper.TAG)
                            .e("AppLovinInterstitialProvider.onAdDisplayFailed(): $error")
                    }

                })
                interstitialAd.loadAd()
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }
        }
    }

}