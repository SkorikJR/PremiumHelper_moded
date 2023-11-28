package com.appboosty.ads.admob

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.ads.PhAdListener
import com.appboosty.ads.PhLoadAdError
import com.appboosty.ads.config.PHAdSize
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class AdMobBannerProvider(private val adUnitId: String) {

    suspend fun load(context: Context, bannerAdSize: PHAdSize?, ad_listener: PhAdListener): PHResult<View> {

        return suspendCancellableCoroutine { cont ->

            try {

                with(AdView(context)) {
                    setAdSize(bannerAdSize?.asAdSize(context) ?: AdSize.BANNER)
                    layoutParams = FrameLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    adUnitId = this@AdMobBannerProvider.adUnitId
                    onPaidEventListener = OnPaidEventListener { adValue -> PremiumHelper.getInstance().analytics.onPaidImpression(adUnitId, adValue, responseInfo?.mediationAdapterClassName) }
                    adListener = object : AdListener() {

                        override fun onAdClosed() {
                            ad_listener.onAdClosed()
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Timber.tag(TAG).e("AdMobBanner: Failed to load ${error?.code} (${error?.message})")
                            if (cont.isActive) {
                                val error = PhLoadAdError(error?.code?: -1, error?.message?:"", error?.domain?:"undefined")
                                AdsErrorReporter.reportAdErrorAsync(context, "banner", error.message)
                                ad_listener.onAdFailedToLoad(error)
                                cont.resume(PHResult.Failure(IllegalStateException(error?.message)))
                            }
                        }

                        override fun onAdOpened() {
                            ad_listener.onAdOpened()
                        }

                        override fun onAdLoaded() {
                            Timber.tag(TAG).d("AdMobBanner: loaded ad from ${responseInfo?.mediationAdapterClassName}")
                            if (cont.isActive) {
                                ad_listener.onAdLoaded()
                                cont.resume(PHResult.Success(this@with))
                            }
                        }

                        override fun onAdClicked() {
                            ad_listener.onAdClicked()
                        }

                        override fun onAdImpression() {
                        }
                    }

                    loadAd(AdRequest.Builder().build())
                }

            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }
        }
    }
}