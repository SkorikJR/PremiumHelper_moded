package com.appboosty.ads.admob

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.appboosty.ads.*
import com.appboosty.ads.InterstitialManager.Companion.INTERSTITIAL_DELAY
import com.appboosty.premiumhelper.Premium.configuration
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.performance.AdsLoadingPerformance
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.isSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AdMobInterstitialManager : InterstitialManager {
    private val _interstitial = MutableStateFlow<PHResult<InterstitialAd>?>(null)
    private val interstitial: StateFlow<PHResult<InterstitialAd>?> = _interstitial.asStateFlow()

    private val log by timber(PremiumHelper.TAG)
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
                        useTestAds = useTestAds
                    )
                    log.d("AdManager: Loading interstitial ad: ($adUnitId)")
                    AdMobInterstitialProvider(adUnitId)
                        .load(activity)
                }
            } catch (e: Exception) {
                log.e(e, "AdManager: Failed to load interstitial ad")
                PHResult.Failure(e)
            } finally {
                isLoadInProgress = false
                AdsLoadingPerformance.getInstance().onEndInterstitialLoading(System.currentTimeMillis() - start)
            }
            _interstitial.emit(result)
        }
    }

    override fun clearInterstitials() {
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
                _interstitial.value = ad
            }
            return@withTimeoutOrNull true
        } ?: run {
            log.e("Can't load interstitial. Timeout reached")
            false
        }

    }

    override fun showInterstitialAd(
        activity: Activity, callback: PhFullScreenContentCallback?, delayed: Boolean,
        application: Application,
        adUnitIdProvider: AdUnitIdProvider,
        useTestAds: Boolean
    ) {
        if (!isInterstitialLoaded()) {
            loadInterstitial(activity, adUnitIdProvider, useTestAds)
        }

        if(!isAllowedByAdFraud(callback)) return

        if (activity is LifecycleOwner) {
            (activity as LifecycleOwner).lifecycleScope.launch {
                when (val result = interstitial.filterNotNull().first()) {
                    is PHResult.Success -> {
                        if(!PremiumHelper.getInstance().interstitialCapping.check()){
                            return@launch
                        }

                        val interstitialAd = result.value
                        _interstitial.emit(null)
                        interstitialAd.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                    callback?.onAdFailedToShowFullScreenContent(
                                        PhAdError(
                                            error.code,
                                            error.message,
                                            error.domain
                                        )
                                    )
                                }

                                override fun onAdShowedFullScreenContent() {
                                    callback?.onAdShowedFullScreenContent()
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    callback?.onAdDismissedFullScreenContent()
                                }

                                override fun onAdImpression() {
                                    callback?.onAdImpression()
                                }

                                override fun onAdClicked() {
                                    callback?.onAdClicked()
                                }
                            }

                        if (delayed) {
                            delay(INTERSTITIAL_DELAY)
                        }

                        interstitialAd.show(activity)
                        loadInterstitial(activity, adUnitIdProvider, useTestAds)
                    }
                    is PHResult.Failure -> {
                        _interstitial.emit(null)
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
    }

    override fun isInterstitialLoaded(): Boolean {
        return _interstitial.value?.let { it is PHResult.Success } ?: false
    }

    private fun isAllowedByAdFraud(callback: PhFullScreenContentCallback?): Boolean{
        if (configuration.get(Configuration.PREVENT_AD_FRAUD)) {
            if (!isInterstitialLoaded()) {
                callback?.onAdFailedToShowFullScreenContent(
                    PhAdError(
                        -1,
                        "Ad-fraud protection",
                        ""
                    )
                )
                log.w("Interstitial Ad skipped due to ad-fraud protection")
                return false
            }
        }

        return true
    }
}