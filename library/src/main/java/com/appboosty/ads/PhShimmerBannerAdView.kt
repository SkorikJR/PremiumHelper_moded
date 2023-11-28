package com.appboosty.ads

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.appboosty.ads.config.PHAdSize
import com.appboosty.ads.config.PHAdSize.SizeType.*
import com.appboosty.premiumhelper.Premium.analytics
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import kotlin.math.roundToInt

class PhShimmerBannerAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : PhShimmerBaseAdView(context, attrs, defStyle) {

    var bannerSize: PHAdSize.SizeType = ADAPTIVE_ANCHORED
        set(value) {
            if (ViewCompat.isAttachedToWindow(this)) {
                setPropertyError()
            } else {
                field = value
            }
        }

    init {

        with(context.obtainStyledAttributes(attrs, R.styleable.PhShimmerBannerAdView)) {
            bannerSize =
                PHAdSize.SizeType.values()[getInt(R.styleable.PhShimmerBannerAdView_banner_size,
                    ADAPTIVE_ANCHORED.ordinal)]
            recycle()
        }
    }

    override suspend fun createAdView(adLoadingListener: PhAdListener?): View? {
        return when (bannerSize) {
            ADAPTIVE -> loadAdaptiveBanner(adLoadingListener)
            ADAPTIVE_ANCHORED -> loadAdaptiveAnchoredBanner(adLoadingListener)
            else -> loadBanner(adLoadingListener)
        }
    }

    private suspend fun loadBanner(adLoadingListener: PhAdListener?): View? {
        return PremiumHelper.getInstance().adManager.loadBanner(
           // PHAdSize.SizeType.BANNER,
            bannerSize,
            size = PHAdSize(bannerSize),
            adListener = object : PhAdListener() {
                override fun onAdOpened() {
                    analytics.onAdClick(com.appboosty.ads.AdManager.AdType.BANNER)
                }

                override fun onAdLoaded() {
                    adLoadingListener?.onAdLoaded()
                    analytics.onAdShown(com.appboosty.ads.AdManager.AdType.BANNER)
                }

                override fun onAdFailedToLoad(e: PhLoadAdError) {
                    adLoadingListener?.onAdFailedToLoad(e)
                }
            })
    }

    private suspend fun loadAdaptiveBanner(adLoadingListener: PhAdListener?): View? {

        val h = when (layoutParams.height) {
            ViewGroup.LayoutParams.WRAP_CONTENT -> 0
            else -> (height / resources.displayMetrics.density).roundToInt()
        }

        val w = (width / resources.displayMetrics.density).roundToInt()

        return PremiumHelper.getInstance().adManager
            .loadBanner(
                ADAPTIVE,
                size = PHAdSize.adaptiveBanner(w, h),
                adListener = object: PhAdListener() {
                    override fun onAdClicked() {
                        analytics.onAdClick(com.appboosty.ads.AdManager.AdType.BANNER)
                        adLoadingListener?.onAdClicked()
                    }

                    override fun onAdClosed() {
                        adLoadingListener?.onAdLoaded()
                    }

                    override fun onAdLoaded() {
                        analytics.onAdShown(com.appboosty.ads.AdManager.AdType.BANNER,"shimmer_banner_view")
                        adLoadingListener?.onAdLoaded()
                    }

                    override fun onAdOpened() {
                        adLoadingListener?.onAdOpened()
                    }
                })
    }

    private suspend fun loadAdaptiveAnchoredBanner(adLoadingListener: PhAdListener?): View? {

        val w = (width / resources.displayMetrics.density).roundToInt()

        return PremiumHelper.getInstance().adManager
            .loadBanner(
                ADAPTIVE_ANCHORED,
                size = PHAdSize.adaptiveAnchoredBanner(w),
                adListener = object: PhAdListener() {
                    override fun onAdClicked() {
                        analytics.onAdClick(com.appboosty.ads.AdManager.AdType.BANNER)
                        adLoadingListener?.onAdClicked()
                    }

                    override fun onAdClosed() {
                        adLoadingListener?.onAdLoaded()
                    }

                    override fun onAdLoaded() {
                        analytics.onAdShown(com.appboosty.ads.AdManager.AdType.BANNER,"shimmer_banner_view")
                        adLoadingListener?.onAdLoaded()
                    }

                    override fun onAdOpened() {
                        adLoadingListener?.onAdOpened()
                    }
                })
    }

    override fun getMinHeight(): Int {
        val w = (width / resources.displayMetrics.density).roundToInt()
        val h = PHAdSize(bannerSize, width = w).asAdSize(context).height.toFloat()
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, h, resources.displayMetrics)
            .toInt()
    }

    override fun getAdWidth() = FrameLayout.LayoutParams.WRAP_CONTENT

}