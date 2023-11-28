package com.appboosty.ads.nativead

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.appboosty.premiumhelper.R

class PHNativeAdView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    private val rootAdView: View

    init {
        inflate(context, R.layout.ph_native_ad_layout, this)
        rootAdView = findViewById(R.id.list_item_layout)
    }

    fun bindView(nativeAd: NativeAd?) {
        val ad = nativeAd ?: return

        findViewById<TextView>(R.id.list_item_nativeAd_title).text = ad.headline
        rootAdView.visibility = View.VISIBLE
    }

}