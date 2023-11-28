package com.appboosty.sampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.appboosty.ads.applovin.AppLovinNativeAdWrapper
import com.appboosty.sample.R
import com.appboosty.sample.databinding.ActivityAppLovinNativeBinding
import com.appboosty.sampleapp.models.AppLovinNativeViewModel

class AppLovinNativeActivity : AppCompatActivity() {


    private val viewModel: AppLovinNativeViewModel by viewModels()
    private lateinit var binding: ActivityAppLovinNativeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLovinNativeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initAppLovinNativeAd()
        viewModel.loadAd()
    }

    private fun initAppLovinNativeAd(){
        viewModel.maxAdLiveData.observe(this){ ad->
            binding.progressBar.visibility = View.GONE
            ad?.let {
                showAd(it)
            }
        }
    }

    private fun showAd(nativeAdWrapper: AppLovinNativeAdWrapper) {
        val adView = createNativeAdView()
        nativeAdWrapper.adLoader.render(adView, nativeAdWrapper.nativeAd)
        binding.mainContainer.removeAllViews()
        binding.mainContainer.addView(adView)
    }

    private fun createNativeAdView(): MaxNativeAdView {
        val binder: MaxNativeAdViewBinder = MaxNativeAdViewBinder.Builder(R.layout.native_custom_ad_view)
            .setTitleTextViewId(R.id.title_text_view)
            .setBodyTextViewId(R.id.body_text_view)
            .setAdvertiserTextViewId(R.id.advertiser_textView)
            .setIconImageViewId(R.id.icon_image_view)
            .setMediaContentViewGroupId(R.id.media_view_container)
            .setOptionsContentViewGroupId(R.id.ad_options_view)
            .setCallToActionButtonId(R.id.cta_button)
            .build()
        return MaxNativeAdView(binder, this)
    }
}