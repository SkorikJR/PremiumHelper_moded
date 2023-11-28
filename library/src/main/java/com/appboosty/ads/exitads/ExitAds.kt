package com.appboosty.ads.exitads

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.*
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.google.android.gms.ads.nativead.NativeAdView
import com.appboosty.ads.PhAdListener
import com.appboosty.ads.config.PHAdSize
import com.appboosty.ads.nativead.NativeAdHelper
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.isMainActivity
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.util.ActivityLifecycleCallbacksAdapter
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.error
import kotlinx.coroutines.*
import kotlin.coroutines.resume

class ExitAds(
    private val adManager: com.appboosty.ads.AdManager,
    private val application: Application

) {

    interface ExitAdsActivity

    private val log by timber(PremiumHelper.TAG)

    private var exitAdActivityListener: Application.ActivityLifecycleCallbacks? = null
    private var isAdShowed = false

    fun onActivityClosed(){
        isAdShowed = false
    }

    private fun isEnabled(): Boolean {
        return with(PremiumHelper.getInstance()) {
            !hasActivePurchase() && configuration.get(com.appboosty.premiumhelper.configuration.Configuration.SHOW_AD_ON_APP_EXIT)
        }
    }

    fun isNotEnabled() = !isEnabled()
    fun inExitAdShown() = isAdShowed

    fun init() {

        if (isEnabled()) {
            if (exitAdActivityListener == null) {
                exitAdActivityListener =
                    object : ActivityLifecycleCallbacksAdapter() {
                        override fun onActivityResumed(activity: Activity) {
                            if (isEnabled()) {
                                if (activity.isExitAdActivity())
                                    loadExitAd(activity, false)
                            } else {
                                application.unregisterActivityLifecycleCallbacks(
                                    exitAdActivityListener
                                )
                            }
                        }
                    }

                application.registerActivityLifecycleCallbacks(exitAdActivityListener)
            }

        } else {
            exitAdActivityListener?.let { application.unregisterActivityLifecycleCallbacks(it) }
        }

    }

    private fun reportAdShown(viewContainer: ExitViewContainer) {
        val origin = if (viewContainer.isNative)
            com.appboosty.ads.AdManager.AdType.NATIVE
        else
            com.appboosty.ads.AdManager.AdType.BANNER_MEDIUM_RECT
        PremiumHelper.getInstance().analytics.onAdShown(origin, "exit_ad")
    }

    private fun loadAndShowBannerAsync(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            getBannerView(activity).takeIf { it?.exitView != null }?.let { bannerView ->
                withContext(Dispatchers.Main) { attachView(activity, bannerView) }
                reportAdShown(bannerView)
            }
            return@launch
        }
    }

    fun show(activity: Activity, useTestAds: Boolean) {
        if (!isEnabled() || isAdShowed) return
        isAdShowed = true
        nativeExitView?.let { viewContainer ->
            attachView(activity, viewContainer)
            nativeExitView = null
            reportAdShown(viewContainer)
        } ?: run {
            loadAndShowBannerAsync(activity)
        }
        val adView = activity.findViewById<ViewGroup>(R.id.ph_ad_close_view)

        if (isInPortraitMode(activity) && adView != null && adView.visibility != View.VISIBLE) {

            val backgroundView = activity.findViewById<ViewGroup>(R.id.ph_ad_close_background)

            backgroundView.post {
                backgroundView.alpha = 0f
                backgroundView.visibility = View.VISIBLE
                backgroundView.animate().apply {
                    alpha(1f)
                    duration = 250
                    setListener(null)
                    start()
                }
            }

            adView.post {
                adView.translationY = backgroundView.height.toFloat()
                adView.visibility = View.VISIBLE
                adView.animate().apply {
                    translationY(0f)
                    startDelay = 200
                    duration = 250
                    interpolator = FastOutSlowInInterpolator()
                    setListener(null)
                    start()
                }

            }

            activity.findViewById<TextView>(R.id.confirm_exit_text).setOnClickListener {
                activity.finish()
                val container =
                    activity.findViewById<ViewGroup>(R.id.ph_ad_close_container)
                container.removeAllViews()
                isAdShowed = false
            }

            backgroundView.setOnClickListener {

                backgroundView.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            backgroundView.visibility = View.GONE
                        }
                    })
                    .start()
                isAdShowed = false
                adView.animate()
                    .translationY(backgroundView.height.toFloat())
                    .setStartDelay(200)
                    .setDuration(500)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            val container =
                                activity.findViewById<ViewGroup>(R.id.ph_ad_close_container)
                            container.removeAllViews()
                            adView.visibility = View.GONE
                            container.minimumHeight = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                250f,
                                activity.resources.displayMetrics
                            ).toInt()
                            loadExitAd(activity, useTestAds)
                            activity.findViewById<ViewGroup>(R.id.ph_ad_close_progress).isVisible =
                                true
                        }
                    })
                    .start()
            }
        }
    }

    private fun addBottomSheetActivity(activity: Activity): Boolean {

        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        return if (rootView.findViewById<ViewGroup>(R.id.ph_ad_close_view) == null) {

            val bottomSheetView =
                LayoutInflater.from(activity).inflate(R.layout.ph_ad_close_view, rootView, false)
            val bottomSheetBackground = LayoutInflater.from(activity)
                .inflate(R.layout.ph_ad_close_background, rootView, false)
            rootView.addView(bottomSheetBackground)
            rootView.addView(bottomSheetView)

            ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView) { _, insets ->
                if (insets.hasInsets()) {
                    ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView, null)
                    bottomSheetView.findViewById<View>(R.id.confirm_exit_text)
                        .updateLayoutParams<LinearLayout.LayoutParams> {
                            bottomMargin =
                                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                        }
                }
                insets
            }

            true

        } else {
            rootView.findViewById<ViewGroup>(R.id.ph_ad_close_container).childCount == 0
        }
    }

    private var nativeExitView: ExitViewContainer? = null


    private fun loadExitAd(activity: Activity, useTestAds: Boolean) {
        if(activity !is LifecycleOwner) return
        (activity as LifecycleOwner).lifecycleScope.launchWhenCreated {

            if (addBottomSheetActivity(activity)) {

                // Get ad view (Native, Banner or Error)
                val nativeView: ExitViewContainer? =
                    getExitAdView(activity, useTestAds)?.takeIf { it.exitView != null }
                nativeExitView = nativeView
                val container = activity.findViewById<ViewGroup?>(R.id.ph_ad_close_container)
                (nativeView?.exitView?.layoutParams as FrameLayout.LayoutParams?)?.gravity =
                    Gravity.CENTER
                nativeView?.exitView?.doOnLayout {
                    container?.minimumHeight = it.height
                }

//                // add ad view to the bottom sheet
//                container.addView(view)
//
//                activity.findViewById<ViewGroup>(R.id.ph_ad_close_progress).isVisible = false
            }
        }

    }

    private fun attachView(activity: Activity, adContainer: ExitViewContainer) {
        activity.findViewById<ViewGroup?>(R.id.ph_ad_close_container)?.let { container->
            if(adContainer.exitView?.parent == null)
                container.addView(adContainer.exitView)
            activity.findViewById<ViewGroup>(R.id.ph_ad_close_progress).isVisible = false
        }
    }

    private suspend fun getExitAdView(activity: Activity, useTestAds: Boolean): ExitViewContainer? {
        val adContainer = activity.findViewById<ViewGroup>(R.id.ph_ad_close_container)
        return getNativeAdView(activity, adContainer, useTestAds)
    }

    private suspend fun getNativeAdView(
        context: Context,
        ad_container: ViewGroup,
        useTestAds: Boolean
    ): ExitViewContainer? {
        return try {

            if (!adManager.isAdEnabled(com.appboosty.ads.AdManager.AdType.NATIVE, true)) {
                return null
            }

            if (adManager.currentAdsProvider == Configuration.AdsProvider.APPLOVIN) {
                ExitViewContainer(loadNativeAppLovinExitAd(context, useTestAds), true)
            } else {

                val result = adManager.loadAndGetNativeAd(true)

                if (result is PHResult.Success) {
                    val nativeAdView = LayoutInflater.from(context).inflate(
                        R.layout.ph_exit_native_ad_layout,
                        ad_container,
                        false
                    ) as NativeAdView
                    NativeAdHelper.populateView(result.value, nativeAdView)
                    ExitViewContainer(nativeAdView, true)
                } else {
                    null
                }
            }

        } catch (e: Exception) {
            // Failed to get native ad
            null
        }
    }

    private suspend fun loadNativeAppLovinExitAd(context: Context, useTestAds: Boolean): View? {
        return try {
            suspendCancellableCoroutine { cont ->
                GlobalScope.launch {

                    val result = adManager.loadAndGetAppLovinNativeAd(isExitAd = true)
                    if (result is PHResult.Success) {
                        if (cont.isActive) {
                            val nativeAdView = withContext(Dispatchers.Main){ createExitNativeAdView(context) }
                            result.value.adLoader.render(nativeAdView, result.value.nativeAd)
                            cont.resume(nativeAdView)
                        }
                    } else {
                        log.e("AppLovin exit ad failed to load. Error: ${result.error}")
                        if (cont.isActive) {
                            cont.resume(null)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            log.e(ex)
            null
        }

    }

    private fun createExitNativeAdView(context: Context): MaxNativeAdView {
        val binder: MaxNativeAdViewBinder =
            MaxNativeAdViewBinder.Builder(R.layout.max_exit_ad_native_layout)
                .setTitleTextViewId(R.id.title_text_view)
                .setBodyTextViewId(R.id.body_text_view)
                .setAdvertiserTextViewId(R.id.advertiser_textView)
                .setIconImageViewId(R.id.icon_image_view)
                .setMediaContentViewGroupId(R.id.media_view_container)
                .setOptionsContentViewGroupId(R.id.ad_options_view)
                .setCallToActionButtonId(R.id.cta_button)
                .build()
        return MaxNativeAdView(binder, context)
    }

    private suspend fun getBannerView(activity: Activity): ExitViewContainer? {

        if (!adManager.isAdEnabled(com.appboosty.ads.AdManager.AdType.BANNER, true)) {
            return null
        }

        return ExitViewContainer(
            adManager.loadBanner(
                PHAdSize.SizeType.ADAPTIVE,
                PHAdSize.adaptiveBanner(activity, maxHeight = 250),
                object : PhAdListener() {
                    override fun onAdOpened() {
                        PremiumHelper.getInstance().analytics.onAdClick(
                            com.appboosty.ads.AdManager.AdType.BANNER,
                            "exit_ad"
                        )
                    }

                    override fun onAdLoaded() {
//                    PremiumHelper.getInstance().analytics.onAdShown(
//                        AdManager.AdType.BANNER,
//                        "exit_ad"
//                    )
                    }
                },
                true
            ), false
        )

    }

    private fun getErrorView(context: Context, ad_container: ViewGroup): ExitViewContainer {
        return ExitViewContainer(
            LayoutInflater.from(context)
                .inflate(R.layout.ph_ad_close_error_view, ad_container, false), false
        )
    }

    private fun isInPortraitMode(activity: Activity): Boolean =
        activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    fun Activity.isExitAdActivity(): Boolean {
        return (this is ExitAdsActivity || isMainActivity())
    }

    data class ExitViewContainer(val exitView: View?, val isNative: Boolean)

}