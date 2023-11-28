package com.appboosty.ads.applovin

import android.content.Context
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.ads.PhAdListener
import com.appboosty.ads.PhLoadAdError
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AppLovinNativeProvider(private val adUnitId: String) {

    suspend fun load(context: Context, maxNativeAdView: MaxNativeAdView, listener: PhAdListener): PHResult<Unit> {

        return suspendCancellableCoroutine { cont->
            try {
                val nativeAdLoader = MaxNativeAdLoader(adUnitId, context)
                nativeAdLoader.setRevenueListener { ad->
                    PremiumHelper.getInstance()
                        .analytics.onPaidImpression(AppLovinRevenueHelper.convertParameters(ad))
                    listener.onAdImpression()
                }

                nativeAdLoader.setNativeAdListener(object: MaxNativeAdListener() {
                    override fun onNativeAdLoaded(var1: MaxNativeAdView?, var2: MaxAd?) {
                        PremiumHelper.getInstance().analytics.onAdShown(com.appboosty.ads.AdManager.AdType.NATIVE)
                        listener.onAdLoaded()
                    }

                    override fun onNativeAdLoadFailed(adUnitId: String?, error: MaxError?) {
                        AdsErrorReporter.reportAdErrorAsync(context, "native", error?.message)
                        listener.onAdFailedToLoad(PhLoadAdError(error?.code?:-1, error?.message?:"", ""))
                    }

                    override fun onNativeAdClicked(var1: MaxAd?) {
                        listener.onAdClicked()
                    }

                    override fun onNativeAdExpired(var1: MaxAd?) {

                    }
                })
                //nativeAdLoader.loadAd(maxNativeAdView)
                nativeAdLoader.loadAd()

            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }

        }

    }

    suspend fun loadLateBindingAd(context: Context, listener: PhAdListener, nativeListener: PhMaxNativeAdListener, isExitAd: Boolean): PHResult<Unit> {

        return suspendCancellableCoroutine { cont->
            try {
                val nativeAdLoader = MaxNativeAdLoader(adUnitId, context)
                nativeAdLoader.setRevenueListener { ad->
                    if(!isExitAd)
                        PremiumHelper.getInstance().analytics.onAdShown(com.appboosty.ads.AdManager.AdType.NATIVE)
                    PremiumHelper.getInstance()
                        .analytics.onPaidImpression(AppLovinRevenueHelper.convertParameters(ad))
                    listener.onAdImpression()
                }
                nativeAdLoader.setNativeAdListener(object: MaxNativeAdListener() {
                    override fun onNativeAdLoaded(var1: MaxNativeAdView?, ad: MaxAd?) {
                        nativeListener.onNativeAdLoaded(nativeAdLoader, ad)
                        listener.onAdLoaded()
                        if (cont.isActive) {
                            cont.resume(PHResult.Success(Unit))
                        }
                    }

                    override fun onNativeAdLoadFailed(var1: String?, error: MaxError?) {
                        nativeListener.onNativeAdLoadFailed(var1, error)
                        listener.onAdFailedToLoad(PhLoadAdError(error?.code?:-1, error?.message?:"", ""))
                        if (cont.isActive) {
                            cont.resume(PHResult.Failure(IllegalStateException(error?.message)))
                        }
                    }

                    override fun onNativeAdClicked(var1: MaxAd?) {
                        nativeListener.onNativeAdClicked(var1)
                        listener.onAdClicked()
                    }

                    override fun onNativeAdExpired(var1: MaxAd?) {
                        nativeListener.onNativeAdExpired(var1)
                    }
                })
                nativeAdLoader.loadAd()

            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }

        }

    }

}