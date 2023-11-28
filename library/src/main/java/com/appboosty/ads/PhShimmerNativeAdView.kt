package com.appboosty.ads

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.google.android.gms.ads.nativead.NativeAdView
import com.appboosty.ads.applovin.AppLovinUnitIdProvider
import com.appboosty.ads.nativead.NativeAdHelper
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.util.PHResult
import timber.log.Timber

class PhShimmerNativeAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : PhShimmerBaseAdView(context, attrs, defStyle) {

    enum class NativeAdSize {
        SMALL, MEDIUM
    }

    private val nativeAdLoader: MaxNativeAdLoader? by lazy {
        if(PremiumHelper.getInstance().adManager.currentAdsProvider == Configuration.AdsProvider.APPLOVIN) {
            val provider = AppLovinUnitIdProvider()
            MaxNativeAdLoader(
                provider.getNativeADUnit(false),
                context
            )
        }else null
    }

    private val theme: Int

    var nativeAdSize: NativeAdSize = NativeAdSize.SMALL
        set(value) {
            if (ViewCompat.isAttachedToWindow(this)) {
                setPropertyError()
            } else {
                field = value
            }
        }

    private var nativeAdBackgroundColor: Int?
    private var nativeAdTitleTextColor: Int?
    private var nativeAdLabelTextColor: Int?
    private var nativeAdBodyTextColor: Int?
    private var nativeAdInstallButtonTextColor: Int?
    private var nativeAdInstallButtonColor: Int?

    init {

        with(context.obtainStyledAttributes(attrs, R.styleable.PhShimmerNativeAdView)) {
            nativeAdSize =
                NativeAdSize.values()[getInt(R.styleable.PhShimmerNativeAdView_native_ad_size, NativeAdSize.SMALL.ordinal)]
            recycle()
        }

        with(context.obtainStyledAttributes(attrs, R.styleable.NativeAd)) {
            nativeAdBackgroundColor = loadColorFromAttributes(
                this, R.styleable.NativeAd_native_background_color,
                ContextCompat.getColor(context, R.color.grey_blue_800))

            nativeAdTitleTextColor = loadColorFromAttributes(
                this, R.styleable.NativeAd_native_title_text_color,
                ContextCompat.getColor(context, R.color.ph_text_light))

            nativeAdLabelTextColor = loadColorFromAttributes(
                this, R.styleable.NativeAd_native_label_text_color,
                ContextCompat.getColor(context, R.color.ph_light_grey))

            nativeAdBodyTextColor = loadColorFromAttributes(
                this, R.styleable.NativeAd_native_body_text_color,
                ContextCompat.getColor(context, R.color.ph_black))

            nativeAdInstallButtonTextColor = loadColorFromAttributes(
                this, R.styleable.NativeAd_native_install_button_text_color,
                ContextCompat.getColor(context, R.color.ph_black))

            nativeAdInstallButtonColor = loadColorFromAttributes(
                this, R.styleable.NativeAd_native_install_button_color,
                ContextCompat.getColor(context, R.color.ph_orange_light))

            recycle()
        }

        with(context.obtainStyledAttributes(attrs, R.styleable.View)) {
            theme = getResourceId(R.styleable.View_android_theme, R.style.PhNativeAdStyle)
            recycle()
        }

    }

    private fun loadColorFromAttributes(attrs: TypedArray, index: Int, defColor: Int): Int? {
        val color = attrs.getColor(index, defColor)
        return color.takeIf { color != defColor || PremiumHelper.getInstance().adManager.currentAdsProvider == Configuration.AdsProvider.APPLOVIN }
    }

    override suspend fun createAdView(adLoadingListener: PhAdListener?): View? {
        if (!PremiumHelper.getInstance().adManager.isAdEnabled(com.appboosty.ads.AdManager.AdType.NATIVE, true)) {
            return null
        }
        return when(PremiumHelper.getInstance().adManager.currentAdsProvider){
            Configuration.AdsProvider.ADMOB -> createAdMobView(adLoadingListener)
            Configuration.AdsProvider.APPLOVIN -> createAppLovinView(adLoadingListener)
        }
    }

    private var nativeAd: MaxAd? = null

    private fun createAppLovinView(adLoadingListener: PhAdListener?): View? {
        val viewId = when (nativeAdSize) {
            NativeAdSize.SMALL -> R.layout.ph_applovin_native_small_ad_view
            NativeAdSize.MEDIUM -> R.layout.ph_applovin_native_medium_ad_view
        }

        val binder: MaxNativeAdViewBinder = MaxNativeAdViewBinder.Builder(viewId)
            .setTitleTextViewId(R.id.native_ad_title)
            .setBodyTextViewId(R.id.native_ad_body)
            .setAdvertiserTextViewId(R.id.native_ad_sponsored_label)
            .setIconImageViewId(R.id.native_ad_icon)
            .setMediaContentViewGroupId(R.id.media_view_container)
            .setOptionsContentViewGroupId(R.id.ad_choices_container)
            .setCallToActionButtonId(R.id.native_ad_call_to_action)
            .build()

        val nativeAdView = MaxNativeAdView(binder, context)
        nativeAdLoader?.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                nativeAd?.let {
                    nativeAdLoader?.destroy(nativeAd)
                }
                nativeAd = ad
                adLoadingListener?.onAdLoaded()
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                adLoadingListener?.onAdFailedToLoad(PhLoadAdError(error.code, "", PhAdError.UNDEFINED_DOMAIN, null))
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                adLoadingListener?.onAdClicked()
            }
        })
        nativeAdLoader?.loadAd(nativeAdView)
       // applyAttributesAppLovin(nativeAdView)
        return nativeAdView
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        nativeAd?.let {
            nativeAdLoader?.destroy(nativeAd)
        }
        try {
            MaxNativeAdLoader::class.java.getDeclaredMethod("destroy")
            nativeAdLoader?.destroy()
        }catch (ex: NoSuchMethodException){
            Timber.e("Can't find method destroy() in  MaxNativeAdLoader")
        }
    }

    private suspend fun createAdMobView(adLoadingListener: PhAdListener?): View? {
        val result = PremiumHelper.getInstance().adManager.loadAndGetNativeAd()
        return if (result is PHResult.Success) {
            adLoadingListener?.onAdLoaded()
            val inflater = LayoutInflater.from(context).cloneInContext(ContextThemeWrapper(context, theme))
            val viewId = when (nativeAdSize) {
                NativeAdSize.SMALL -> R.layout.ph_small_native_ad_layout
                NativeAdSize.MEDIUM -> R.layout.ph_medium_native_ad_layout
            }
            val nativeAdView = inflater.inflate(viewId, this, false) as NativeAdView
            NativeAdHelper.populateView(result.value, nativeAdView)
            applyAttributes(nativeAdView)
            nativeAdView.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            nativeAdView
        } else {
            adLoadingListener?.onAdFailedToLoad(PhLoadAdError(-1, "", PhAdError.UNDEFINED_DOMAIN, null))
            null
        }
    }

    private fun applyAttributes(nativeAdView: View) {
        nativeAdBackgroundColor?.let { color ->
            nativeAdView.findViewById<View?>(R.id.native_ad_container)?.let {
                it.setBackgroundColor(color)
            }
        }
        nativeAdTitleTextColor?.let { color ->
            nativeAdView.findViewById<TextView?>(R.id.native_ad_title)?.let {
                it.setTextColor(color)
            }
        }
        nativeAdLabelTextColor?.let { color ->
            nativeAdView.findViewById<TextView>(R.id.native_ad_sponsored_label)?.let {
                it.setTextColor(color)
            }
        }
        nativeAdBodyTextColor?.let { color ->
            nativeAdView.findViewById<TextView?>(R.id.native_ad_body)?.let {
                it.setTextColor(color)
            }
        }
        nativeAdInstallButtonTextColor?.let { color ->
            nativeAdView.findViewById<TextView?>(R.id.native_ad_call_to_action)?.let {
                it.setTextColor(color)
            }
        }
        nativeAdInstallButtonColor?.let { color ->
            nativeAdView.findViewById<Button?>(R.id.native_ad_call_to_action)?.let {
                it.backgroundTintList = ColorStateList.valueOf(color)
            }
        }
    }

    override fun getMinHeight(): Int {
        return when(nativeAdSize) {
            NativeAdSize.SMALL -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics)
            NativeAdSize.MEDIUM -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, resources.displayMetrics)
        }.toInt()
    }

    override fun getAdWidth() = LayoutParams.MATCH_PARENT
}