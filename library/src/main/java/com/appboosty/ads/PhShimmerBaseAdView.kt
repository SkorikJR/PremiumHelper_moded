package com.appboosty.ads

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
//import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.appboosty.premiumhelper.BuildConfig
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.performance.AdsLoadingPerformance
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception

abstract class PhShimmerBaseAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ShimmerFrameLayout(context, attrs, defStyle) {

    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private val baseColor: ColorStateList
    private val highlightColor: ColorStateList

    var adLoadingListener: PhAdListener? = null

    init {

        val transition = LayoutTransition()

        with (context.obtainStyledAttributes(attrs, R.styleable.PhShimmerBaseAdView)) {

            baseColor = getColorStateList(R.styleable.PhShimmerBaseAdView_shimmer_base_color) ?: ColorStateList.valueOf(
                Color.WHITE)
            highlightColor = getColorStateList(R.styleable.PhShimmerBaseAdView_shimmer_highlight_color) ?: ColorStateList.valueOf(
                Color.LTGRAY)

            getInteger(R.styleable.PhShimmerBaseAdView_transition_animation_duration, 300).takeIf { it != 0 }?.let { duration ->
                transition.setDuration(duration.toLong())
                layoutTransition = transition
            }

            recycle()
        }

        setShimmer(Shimmer.ColorHighlightBuilder()
            .setBaseColor(baseColor.defaultColor)
            .setHighlightColor(highlightColor.defaultColor)
            .build())

    }

    fun removeAd(){
        onDetachedFromWindow()
    }

    fun addAd(){
        if(childCount == 0) {
            onAttachedToWindow()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if(isInEditMode) return  //Preventing editor crash in Android Studio 2021.3

        doOnLayout {
            if (!PremiumHelper.getInstance().hasActivePurchase()) {
                if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    updateLayoutParams { minimumHeight = getMinHeight().coerceAtLeast(minimumHeight) }
                }
            }
        }

        if (!scope.isActive) {
            scope = CoroutineScope(Job() + Dispatchers.Main)
        }

        scope.launch {
            PremiumHelper.getInstance().observePurchaseStatus().collect { isPurchased->
                isVisible = !isPurchased
                if (isPurchased) {
                    hideAd()
                } else {
                    loadAd()
                }
            }
        }

    }

    private fun loadAd() {
        scope.launch {

            val shimmerView = addShimmerBg()
            val start = System.currentTimeMillis()
            AdsLoadingPerformance.getInstance().onStartLoadingBanner()
            startShimmer()

            createAdView(adLoadingListener)?.let { adView ->
                addView(adView)
                removeView(shimmerView)
                hideShimmer()
            } ?: run {
                isVisible = false
            }

            removeView(shimmerView)
            hideShimmer()
            AdsLoadingPerformance.getInstance().onEndLoadingBanner(System.currentTimeMillis() - start)
        }
    }

    abstract fun getAdWidth(): Int

    private fun addShimmerBg() : View {

        val shimmerView = View(context)

        shimmerView.background = ColorDrawable(baseColor.defaultColor)

        val shimmerLayoutParams = if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            LayoutParams(LayoutParams.MATCH_PARENT, getMinHeight().coerceAtLeast(minimumHeight))
        } else {
            LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        addView(shimmerView, shimmerLayoutParams)

        return shimmerView
    }

    abstract suspend fun createAdView(adLoadingListener: PhAdListener?): View?

    override fun onDetachedFromWindow() {

        scope.cancel()

        hideAd()

        super.onDetachedFromWindow()
    }

    private fun hideAd() {
        hideShimmer()
        try {
            if (childCount > 0) {
                when (val child = getChildAt(0)) {
                    is AdView -> child.destroy()
                    is AdManagerAdView -> child.destroy()
                }
                removeAllViews()
            }
        }catch (ex: Exception){
            Timber.e(ex)
        }
    }

    abstract fun getMinHeight() : Int

    internal fun setPropertyError() {
        if (BuildConfig.DEBUG) {
            error("Banner property is set after banner view is attached to window!")
        } else {
            Timber.e("Banner property is set after banner view is attached to window!")
        }
    }
}