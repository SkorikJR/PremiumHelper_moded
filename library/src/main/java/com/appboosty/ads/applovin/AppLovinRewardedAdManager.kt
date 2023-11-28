package com.appboosty.ads.applovin

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import com.google.android.gms.ads.AdError
import com.appboosty.ads.*
import com.appboosty.ads.PhAdError.Companion.UNDEFINED_DOMAIN
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.performance.AdsLoadingPerformance
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AppLovinRewardedAdManager : RewardedAdManager {

    private val _rewarded = MutableStateFlow<PHResult<MaxRewardedAd>?>(null)
    private val rewarded: StateFlow<PHResult<MaxRewardedAd>?> = _rewarded.asStateFlow()
    private var rewardAdLoader: AppLovinRewardedProvider? = null

    private val log by timber(PremiumHelper.TAG)

    override fun loadRewardedAd(
        activity: Activity,
        adUnitIdProvider: AdUnitIdProvider,
        useTestAds: Boolean,
        listener: PhAdListener?
    ) {
        GlobalScope.launch {
            val start = System.currentTimeMillis()
            val result = try {
                withContext(Dispatchers.Main) {
                    rewardAdLoader = AppLovinRewardedProvider()
                    rewardAdLoader?.load(
                        activity,
                        adUnitIdProvider.getRewardedADUnit(useTestAds)
                    )
                }
            } catch (e: Exception) {
                log.e(e, "AdManager: Failed to load rewarded ad")
                PHResult.Failure(e)
            }
            AdsLoadingPerformance.getInstance()
                .onEndLoadingRewardedAd(System.currentTimeMillis() - start)
            _rewarded.emit(result)

            if (result is PHResult.Success) {
                listener?.onAdLoaded()
            } else {
                listener?.onAdFailedToLoad(
                    PhLoadAdError(
                        -1,
                        (result as PHResult.Failure).error?.message ?: "",
                        PhAdListener.UNDEFINED_DOMAIN,
                        null
                    )
                )
            }
        }
    }

    override fun showRewardedAd(
        application: Application,
        adUnitIdProvider: AdUnitIdProvider,
        useTestAds: Boolean,
        activity: Activity,
        rewardedAdCallback: PhOnUserEarnedRewardListener,
        callback: PhFullScreenContentCallback
    ) {
        if(activity !is LifecycleOwner) return
        (activity as LifecycleOwner).lifecycleScope.launch {
            when (val result = rewarded.filterNotNull().first()) {
                is PHResult.Success -> {
                    result.value.takeIf { it.isReady }?.let { maxRewardAd ->
                        rewardAdLoader?.externalListener = object : MaxRewardedAdListener {
                            override fun onAdLoaded(ad: MaxAd?) {
                                log.e("RewardAd.onAdLoaded()-> Should never be called at this stage")
                            }

                            override fun onAdDisplayed(ad: MaxAd?) {
                                callback.onAdShowedFullScreenContent()
                            }

                            override fun onAdHidden(ad: MaxAd?) {
                                callback.onAdDismissedFullScreenContent()
                            }

                            override fun onAdClicked(ad: MaxAd?) {
                                callback.onAdClicked()
                            }

                            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                                log.e("RewardAd.onAdLoadFailed()-> Should never be called at this stage")
                            }

                            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                                callback.onAdFailedToShowFullScreenContent(
                                    PhAdError(
                                        error?.code ?: 1, error?.message ?: "", UNDEFINED_DOMAIN
                                    )
                                )
                            }

                            override fun onRewardedVideoStarted(ad: MaxAd?) {
                                log.d("onRewardedVideoStarted()-> called")
                            }

                            override fun onRewardedVideoCompleted(ad: MaxAd?) {
                                log.d("onRewardedVideoCompleted()-> called")
                            }

                            override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                                rewardedAdCallback.onUserEarnedReward(reward?.amount ?: 1)
                            }

                        }
                        maxRewardAd.showAd()
                    } ?: log.e("The rewarded ad received but not ready !")
//                    delay(1000)
//                    loadRewardedAd(activity, adUnitIdProvider, useTestAds, null)
                }
                is PHResult.Failure -> {
                    callback.onAdFailedToShowFullScreenContent(
                        PhAdError(
                            -1,
                            result.error?.message ?: "",
                            AdError.UNDEFINED_DOMAIN
                        )
                    )
                }
            }
        }
    }
}