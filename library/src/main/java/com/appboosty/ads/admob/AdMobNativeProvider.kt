package com.appboosty.ads.admob

import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.ads.PhAdListener
import com.appboosty.ads.PhLoadAdError
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class AdMobNativeProvider(private val adUnitId: String) {

    suspend fun load(context: Context, adCount: Int, listener: PhAdListener,
                     loadListener: NativeAd.OnNativeAdLoadedListener, isExitAd: Boolean): PHResult<Unit> {

        return suspendCancellableCoroutine { cont->
            try {

                val adLoader = AdLoader.Builder(context, adUnitId)
                        .forNativeAd { ad : NativeAd ->
                            Timber.tag(TAG).d("AdMobNative: forNativeAd ${ad.headline}")
                            ad.setOnPaidEventListener { adValue ->
                                if(!isExitAd)
                                    PremiumHelper.getInstance().analytics.onAdShown(com.appboosty.ads.AdManager.AdType.NATIVE)
                                PremiumHelper.getInstance().analytics.onPaidImpression(adUnitId, adValue, ad.responseInfo?.mediationAdapterClassName)
                            }
                            Timber.tag(TAG).d("AdMobNative: loaded ad from ${ad.responseInfo?.mediationAdapterClassName}")
                            loadListener.onNativeAdLoaded(ad)
                        }
                        .withAdListener(object : AdListener() {

                            override fun onAdLoaded() {

                                if (cont.isActive) {
                                    cont.resume(PHResult.Success(Unit))
                                }

                                listener.onAdLoaded()
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {

                                Timber.tag(TAG).e("AdMobNative: Failed to load ${error.code} (${error.message})")
                                AdsErrorReporter.reportAdErrorAsync(context, "native", error.message)
                                if (cont.isActive) {
                                    cont.resume(PHResult.Failure(IllegalStateException(error.message)))
                                }

                                listener.onAdFailedToLoad(PhLoadAdError(error.code, error.message, error.domain, error.cause?.message))
                            }

                            override fun onAdClicked() {
                                listener.onAdClicked()
                            }

                        })
                        .withNativeAdOptions(
                                NativeAdOptions.Builder()
//                                        .setAdChoicesPlacement(if (isRtl()) NativeAdOptions.ADCHOICES_TOP_LEFT else NativeAdOptions.ADCHOICES_TOP_RIGHT)
                                        .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                                        .setRequestMultipleImages(true)
                                        .build())
                        .build()

                adLoader.loadAds(AdRequest.Builder().build(), adCount)

            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }

        }

    }

}