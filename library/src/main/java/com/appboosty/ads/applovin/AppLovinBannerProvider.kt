package com.appboosty.ads.applovin

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.ads.PhAdListener
import com.appboosty.ads.PhLoadAdError
import com.appboosty.ads.config.PHAdSize
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class AppLovinBannerProvider() {
    companion object {
        private const val TAG = "AppLovin"
        private const val APP_LOVIN_BANNER_HEIGHT = 50
        private const val APP_LOVIN_MREC_BANNER_HEIGHT = 250
    }

    suspend fun load(
        context: Context,
        adUnit: String,
        bannerAdSize: PHAdSize?,
        ad_listener: PhAdListener
    ): PHResult<View> {

        return suspendCancellableCoroutine { cont ->

            try {
                val adSize = mapBannerSize(bannerAdSize)
                with(MaxAdView(adUnit, adSize, context)) {
                    // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
                    setExtraParameter("allow_pause_auto_refresh_immediately", "true")
                    setRevenueListener { ad->
                        PremiumHelper.getInstance()
                            .analytics.onPaidImpression(AppLovinRevenueHelper.convertParameters(ad))
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getBannerHeight(context, bannerAdSize)
                    )

                    setListener(object : MaxAdViewAdListener {
                        override fun onAdLoaded(ad: MaxAd?) {
                            if (cont.isActive) {
                                ad_listener.onAdLoaded()
                                //Premium.analytics.onAdShown(AdManager.AdType.BANNER)
                                cont.resume(PHResult.Success(this@with))
                            }
                        }

                        override fun onAdDisplayed(ad: MaxAd?) {
                            Timber.tag(TAG).d("adDisplayed()-> ${ad?.dspName}")
                        }

                        override fun onAdHidden(ad: MaxAd?) {
                            Timber.tag(TAG).d("adHidden()-> ${ad?.adUnitId}")
                        }

                        override fun onAdClicked(ad: MaxAd?) {
                            ad_listener.onAdClicked()
                            Timber.tag(TAG).d("adClicked()-> ${ad?.dspId}")
                        }

                        override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                            Timber.tag(TAG).e("failedToReceiveAd()-> Error: $error")
                            AdsErrorReporter.reportAdErrorAsync(context, "banner", error?.message)
                            ad_listener.onAdFailedToLoad(
                                PhLoadAdError(
                                    error?.code ?: -1,
                                    "failedToReceiveAd",
                                    "",
                                    ""
                                )
                            )
                            if (cont.isActive)
                                cont.resume(PHResult.Failure(IllegalStateException("Can't load banner. Error: $error")))
                        }

                        override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                            Timber.tag(TAG).e("failedToReceiveAd()-> Error : $error")
                            ad_listener.onAdFailedToLoad(
                                PhLoadAdError(
                                    error?.code ?: -1,
                                    "adFailedToDisplay",
                                    "",
                                    ""
                                )
                            )
                        }

                        override fun onAdExpanded(ad: MaxAd?) {

                        }

                        override fun onAdCollapsed(ad: MaxAd?) {

                        }

                    })
                    id = ViewCompat.generateViewId()
                    loadAd()
                }
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }
        }
    }

    private fun mapBannerSize(bannerAdSize: PHAdSize?): MaxAdFormat {
        return when (bannerAdSize?.sizeType) {
            PHAdSize.SizeType.ADAPTIVE, PHAdSize.SizeType.MEDIUM_RECTANGLE -> MaxAdFormat.MREC
            else -> MaxAdFormat.BANNER
        }
    }

    private fun getBannerHeight(context: Context, bannerAdSize: PHAdSize?): Int {
        return when (bannerAdSize?.sizeType) {
            PHAdSize.SizeType.ADAPTIVE, PHAdSize.SizeType.MEDIUM_RECTANGLE -> AppLovinSdkUtils.dpToPx(
                context,
                APP_LOVIN_MREC_BANNER_HEIGHT
            )
            else -> AppLovinSdkUtils.dpToPx(context, APP_LOVIN_BANNER_HEIGHT)

        }
    }
}