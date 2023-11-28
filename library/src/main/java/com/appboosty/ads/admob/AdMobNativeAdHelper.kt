package com.appboosty.ads.admob

import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.appboosty.ads.nativead.NativeAdHelper
import com.appboosty.ads.nativead.PhNativeAdViewBinder
import com.appboosty.premiumhelper.R

object AdMobNativeAdHelper {

    fun createAdmobNativeView(binder: PhNativeAdViewBinder): NativeAdView {
        val nativeAdView = NativeAdView(binder.context)
        val inflater = LayoutInflater.from(binder.context).cloneInContext(ContextThemeWrapper(binder.context, R.style.PhNativeAdStyle))
        inflater.inflate(binder.layoutResourceId, nativeAdView, true)
        nativeAdView.findViewById<View?>(binder.shimmerViewId)?.visibility = View.VISIBLE
        nativeAdView.findViewById<View?>(binder.adContainerViewId)?.visibility = View.INVISIBLE
        return nativeAdView
    }

    fun populateAdmobNativeView(
        binder: PhNativeAdViewBinder,
        nativeAdView: NativeAdView,
        nativeAd: NativeAd
    ) {
        //Headline
        nativeAdView.findViewById<TextView?>(binder.titleTextViewId)?.let {
            it.text = nativeAd.headline
            nativeAdView.headlineView = it
        }
        //Secondary text
        nativeAdView.findViewById<TextView?>(binder.advertiserTextViewId)?.let { secondaryText ->
            if (NativeAdHelper.adHasOnlyStore(nativeAd)) {
                secondaryText.text = nativeAd.store
                nativeAdView.storeView = secondaryText
            } else if (!TextUtils.isEmpty(nativeAd.advertiser)) {
                secondaryText.text = nativeAd.advertiser
                nativeAdView.advertiserView = secondaryText
            } else
                secondaryText.text = ""
        }

        //Body
        nativeAdView.findViewById<TextView?>(binder.bodyTextViewId)?.let { bodyView ->
            bodyView.text = nativeAd.body
            nativeAdView.bodyView = bodyView
        }

        //Rating bar
        nativeAdView.findViewById<RatingBar?>(binder.ratingBarId)?.let { ratingBar ->
            val rating = nativeAd.starRating?.toFloat() ?: 0f
            if (rating > 0) {
                ratingBar.visibility = View.VISIBLE
                ratingBar.rating = rating
                nativeAdView.starRatingView = ratingBar
            } else {
                ratingBar.visibility = View.GONE
            }
        }

        //Icon
        nativeAd.icon?.let { icon ->
            val iconImage: ImageView = nativeAdView.findViewById(binder.iconImageViewId)
            iconImage.visibility = View.VISIBLE
            iconImage.setImageDrawable(icon.drawable)
            nativeAdView.iconView = iconImage
        }

        //MediaView
        nativeAdView.findViewById<ViewGroup?>(binder.mediaContentViewGroupId)?.let {
            val mediaView = com.google.android.gms.ads.nativead.MediaView(binder.context)
            mediaView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            mediaView.foregroundGravity = Gravity.CENTER
            it.addView(mediaView)
            nativeAdView.mediaView = mediaView
        }

        //Button
        val ctaBtn: Button = nativeAdView.findViewById(binder.callToActionButtonId)
        if (nativeAd.callToAction != null) {
            ctaBtn.visibility = View.VISIBLE
            ctaBtn.text = nativeAd.callToAction
            nativeAdView.callToActionView = ctaBtn
        } else
            ctaBtn.visibility = View.GONE


        nativeAdView.findViewById<View?>(binder.adContainerViewId)?.visibility = View.VISIBLE
        nativeAdView.findViewById<View?>(binder.shimmerViewId)?.visibility = View.GONE
        nativeAdView.setNativeAd(nativeAd)
    }
}