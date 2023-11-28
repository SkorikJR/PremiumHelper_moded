package com.appboosty.ads.applovin

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.appboosty.ads.nativead.PhNativeAdViewBinder
import com.appboosty.premiumhelper.R

object AppLovinNativeAdHelper {

    fun createAppLovinNativeAdView(binder: PhNativeAdViewBinder): MaxNativeAdView {
        val layoutView = FrameLayout(binder.context)
        val inflater =
            LayoutInflater.from(binder.context).cloneInContext(ContextThemeWrapper(binder.context, R.style.PhNativeAdStyle))
        inflater.inflate(binder.layoutResourceId, layoutView, true)

        val maxBinder: MaxNativeAdViewBinder = MaxNativeAdViewBinder.Builder(layoutView)
            .setTitleTextViewId(binder.titleTextViewId)
            .setBodyTextViewId(binder.bodyTextViewId)
            .setAdvertiserTextViewId(binder.advertiserTextViewId)
            .setIconImageViewId(binder.iconImageViewId)
            .setMediaContentViewGroupId(binder.mediaContentViewGroupId)
            .setOptionsContentViewGroupId(binder.optionsContentViewGroupId)
            .setCallToActionButtonId(binder.callToActionButtonId)
            .build()
        val adView = MaxNativeAdView(maxBinder, binder.context)
        layoutView.findViewById<View?>(binder.ratingBarId)?.visibility = View.GONE
        layoutView.findViewById<View?>(binder.shimmerViewId)?.visibility = View.VISIBLE
        layoutView.findViewById<View?>(binder.adContainerViewId)?.visibility = View.INVISIBLE
        return adView
    }

    fun populateAdView(
        loader: MaxNativeAdLoader, adView: MaxNativeAdView, ad: MaxAd,
        binder: PhNativeAdViewBinder
    ) {
        loader.render(adView, ad)
        adView.findViewById<View?>(binder.shimmerViewId)?.visibility = View.GONE
        adView.findViewById<View?>(binder.adContainerViewId)?.visibility = View.VISIBLE
    }

}