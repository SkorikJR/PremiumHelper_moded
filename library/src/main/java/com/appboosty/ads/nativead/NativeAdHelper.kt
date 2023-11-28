package com.appboosty.ads.nativead

import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.appboosty.premiumhelper.R


/**
 *Created by Dzano Catovic (dzano.catovic@gmail.com) on 03.08.2021.
 */
object NativeAdHelper {

    fun populateView(nativeAd: NativeAd, nativeAdView: NativeAdView?) {
        if(nativeAdView != null) {

            val headlineView: TextView = nativeAdView.findViewById(R.id.native_ad_title)
            headlineView.text = nativeAd.headline
            nativeAdView.headlineView = headlineView

            val secondaryText: TextView = nativeAdView.findViewById(R.id.native_ad_sponsored_label)
            if (adHasOnlyStore(nativeAd)) {
                secondaryText.text = nativeAd.store
                nativeAdView.storeView = secondaryText
            } else if (!TextUtils.isEmpty(nativeAd.advertiser)) {
                secondaryText.text = nativeAd.advertiser
                nativeAdView.advertiserView = secondaryText
            } else
                secondaryText.text = ""

            val bodyView: TextView = nativeAdView.findViewById(R.id.native_ad_body)
            bodyView.text = nativeAd.body
            nativeAdView.bodyView = bodyView


            nativeAdView.findViewById<RatingBar>(R.id.rating_bar)?.let { ratingBar ->
                val rating = nativeAd.starRating?.toFloat() ?: 0f
                if (rating > 0) {
                    ratingBar.visibility = View.VISIBLE
                    ratingBar.rating = rating
                    nativeAdView.starRatingView = ratingBar
                } else {
                    ratingBar.visibility = View.GONE
                }
            }

            nativeAdView.findViewById<MediaView>(R.id.native_ad_media)?.let { mediaView ->
                nativeAdView.mediaView = mediaView
                val iconImage: ImageView = nativeAdView.findViewById(R.id.native_ad_icon)
                if (nativeAd.icon != null) {
                    iconImage.visibility = View.VISIBLE
                    iconImage.setImageDrawable(nativeAd.icon!!.drawable)
                    nativeAdView.iconView = iconImage
                } else
                    iconImage.visibility = View.GONE
            }

            val ctaBtn: Button = nativeAdView.findViewById(R.id.native_ad_call_to_action)
            if (nativeAd.callToAction != null) {
                ctaBtn.visibility = View.VISIBLE
                ctaBtn.text = nativeAd.callToAction
                nativeAdView.callToActionView = ctaBtn
            } else
                ctaBtn.visibility = View.GONE

            nativeAdView.visibility = View.VISIBLE
            nativeAdView.findViewById<ConstraintLayout>(R.id.native_ad_layout).visibility = View.VISIBLE
            nativeAdView.findViewById<FrameLayout>(R.id.progress_layout).visibility = View.GONE
            nativeAdView.setNativeAd(nativeAd)

        }
    }

    fun adHasOnlyStore(nativeAd: NativeAd): Boolean {
        val store: String = nativeAd.store ?: ""
        val advertiser: String = nativeAd.advertiser ?: ""
        return store.isNotEmpty() && advertiser.isEmpty()
    }
}