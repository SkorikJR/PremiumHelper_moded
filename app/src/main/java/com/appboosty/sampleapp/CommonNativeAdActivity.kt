package com.appboosty.sampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.appboosty.ads.nativead.PhNativeAdViewBinder
import com.appboosty.sample.R
import com.appboosty.sample.databinding.ActivityCommonNativeAdBinding
import com.appboosty.sampleapp.models.CommonNativeAdViewModel

class CommonNativeAdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommonNativeAdBinding
    private val viewModel: CommonNativeAdViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonNativeAdBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initLiveData()
        initNativeAd()
    }

    private fun initLiveData() {
        viewModel.nativeAdView.observe(this){ adView->
            binding.progressBar.visibility = View.GONE
            adView?.let{
                binding.mainContainer.removeAllViews()
                binding.mainContainer.addView(it)
            }
        }
    }

    private fun initNativeAd(){

        val binder = PhNativeAdViewBinder.Builder(this)
            .setMainViewResourceId(R.layout.sample_native_ad_layout)
            .setAdContainerViewId(R.id.main_ad_container)
            .setTitleTextViewId(R.id.primary)
            .setAdvertiserTextViewId(R.id.secondary)
            .setBodyTextViewId(R.id.body)
            .setRatingBarViewId(R.id.rating_bar)
            .setIconImageViewId(R.id.icon)
            .setMediaContentViewGroupId(R.id.media_view)
            .setShimmerViewId(R.id.native_ad_shimmer)
            .setCallToActionButtonId(R.id.cta)
            .build()
        viewModel.loadAd(binder)

    }
}