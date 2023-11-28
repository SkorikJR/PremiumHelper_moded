package com.appboosty.ads.applovin

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.google.android.gms.ads.AdError
import com.appboosty.ads.*
import com.appboosty.premiumhelper.Premium
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.performance.AdsLoadingPerformance
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.isSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AppLovinInterstitialManager : InterstitialManager {

    private val log by timber("AppLovin")

    private val _interstitial = MutableStateFlow<PHResult<MaxInterstitialAd>?>(null)
    private val interstitial: StateFlow<PHResult<MaxInterstitialAd>?> = _interstitial.asStateFlow()
    private var isLoadInProgress = false

    override fun loadInterstitial(
        activity: Activity,
        adUnitIdProvider: AdUnitIdProvider,
        useTestAds: Boolean
    ) {
        if (isLoadInProgress) return
        isLoadInProgress = true
        GlobalScope.launch {
            if (_interstitial.value != null && _interstitial.value !is PHResult.Success) {
                _interstitial.value = null
            }
            AdsLoadingPerformance.getInstance().onStartInterstitialLoading()
            val start = System.currentTimeMillis()
            val result = try {
                withContext(Dispatchers.Main) {
                    val adUnitId = adUnitIdProvider.getAdUnitId(
                        com.appboosty.ads.AdManager.AdType.INTERSTITIAL,
                        isExitAd = false,
                        useTestAds = useTestAds
                    )
                    log.d("AppLovinInterstitialManager: Loading interstitial ad: ($adUnitId)")
                    AppLovinInterstitialProvider(adUnitId).load(activity)
                }
            } catch (e: Exception) {
                log.e(e, "AppLovinInterstitialManager: Failed to load interstitial ad")
                PHResult.Failure(e)
            } finally {
                isLoadInProgress = false
                AdsLoadingPerformance.getInstance().onEndInterstitialLoading(System.currentTimeMillis() - start)
            }
            log.d("loadInterstitial()-> interstitial loaded")
            _interstitial.emit(result)
        }
    }

    override fun isInterstitialLoaded() =
        _interstitial.value?.let { it is PHResult.Success && it.value.isReady } ?: false


    override fun clearInterstitials() {
        log.d("clearInterstitials()-> called")
        if (isInterstitialLoaded()) {
            GlobalScope.launch {
                _interstitial.first()
            }
        }
    }

    override suspend fun waitForInterstitial(timeout: Long): Boolean {
        return withTimeoutOrNull(timeout) {
            val ad = _interstitial.filterNotNull().first()
            if (ad.isSuccess) {
                log.d("waitForInterstitial()-> Interstitial received")
                _interstitial.value = ad
            }
            return@withTimeoutOrNull true
        } ?: run {
            log.e("waitForInterstitial()-> AppLovinInterstitialManager: Can't load interstitial. Timeout reached")
            false
        }
    }

    override fun showInterstitialAd(
        activity: Activity,
        callback: PhFullScreenContentCallback?,
        delayed: Boolean,
        application: Application,
        adUnitIdProvider: AdUnitIdProvider,
        useTestAds: Boolean
    ) {
        log.d("showInterstitialAd()-> called")
        if (!isInterstitialLoaded()) {
            log.d("showInterstitialAd()-> isInterstitialLoaded = false")
            loadInterstitial(activity, adUnitIdProvider, useTestAds)
        }

        if (!isAllowedByAdFraud(callback)) return
        if(activity !is LifecycleOwner) return
        (activity as LifecycleOwner).lifecycleScope.launch {
            when (val result = interstitial.filterNotNull().first()) {
                is PHResult.Success -> {
                    if(!PremiumHelper.getInstance().interstitialCapping.check()){
                        return@launch
                    }
                    log.d("showInterstitialAd()-> PHResult.Success")
                    result.value.takeIf { it.isReady }?.let { interstitialAd ->
                        interstitialAd.setListener(object : MaxAdListener {
                            override fun onAdLoaded(ad: MaxAd?) {
                            }

                            override fun onAdDisplayed(ad: MaxAd?) {
                                log.d("showInterstitialAd()-> adDisplayed")
                                callback?.onAdShowedFullScreenContent()
                            }

                            override fun onAdHidden(ad: MaxAd?) {
                                log.d("showInterstitialAd()-> adHidden")
                                callback?.onAdDismissedFullScreenContent()
                            }

                            override fun onAdClicked(ad: MaxAd?) {
                                log.d("showInterstitialAd()-> adClicked")
                                callback?.onAdClicked()
                            }

                            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                                callback?.onAdFailedToShowFullScreenContent(
                                    PhAdError(
                                        error?.code ?: 2,
                                        error?.message ?: "",
                                        AdError.UNDEFINED_DOMAIN
                                    )
                                )
                            }

                            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                                callback?.onAdFailedToShowFullScreenContent(
                                    PhAdError(
                                        error?.code ?: 3,
                                        error?.message ?: "",
                                        AdError.UNDEFINED_DOMAIN
                                    )
                                )
                            }

                        })
                        if (delayed) {
                            delay(InterstitialManager.INTERSTITIAL_DELAY)
                        }
                        interstitialAd.showAd()
                        loadInterstitial(activity, adUnitIdProvider, useTestAds)
                    }
                        ?: run {
                            log.e("showInterstitialAd()-> Can't show interstitial ad: ${result.value} is Ready: ${result.value.isReady}")
                            loadInterstitial(activity, adUnitIdProvider, useTestAds)
                        }
                }
                is PHResult.Failure -> {
                    log.e("showInterstitialAd()-> PHResult.Failure")
                    callback?.onAdFailedToShowFullScreenContent(
                        PhAdError(
                            -1,
                            result.error?.message ?: "",
                            AdError.UNDEFINED_DOMAIN
                        )
                    )
                    loadInterstitial(activity, adUnitIdProvider, useTestAds)
                }
            }
        }
    }

    private fun isAllowedByAdFraud(callback: PhFullScreenContentCallback?): Boolean{
        return if (Premium.configuration.get(Configuration.PREVENT_AD_FRAUD)) {
            if (!isInterstitialLoaded()) {
                callback?.onAdFailedToShowFullScreenContent(
                    PhAdError(
                        -1,
                        "Ad-fraud protection",
                        ""
                    )
                )
                log.w("Interstitial Ad skipped due to ad-fraud protection")
                false
            } else true
        }else true
    }
}