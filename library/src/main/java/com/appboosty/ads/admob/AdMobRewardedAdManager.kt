package com.appboosty.ads.admob

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.appboosty.ads.*
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.performance.AdsLoadingPerformance
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdMobRewardedAdManager : RewardedAdManager {

    private val _rewarded = MutableStateFlow<PHResult<RewardedAd>?>(null)
    private val rewarded: StateFlow<PHResult<RewardedAd>?> = _rewarded.asStateFlow()

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
                    val adUnitId = adUnitIdProvider.getAdUnitId(com.appboosty.ads.AdManager.AdType.REWARDED, isExitAd = false, useTestAds = useTestAds)
                    log.d("AdManager: Loading rewarded ad: ($adUnitId)")
                    AdMobRewardedProvider(
                        adUnitIdProvider.getAdUnitId(
                            com.appboosty.ads.AdManager.AdType.REWARDED,
                            isExitAd = false,
                            useTestAds = useTestAds
                        )
                    )
                        .load(activity)
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
                    val rewardedAd = result.value
                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            callback.onAdClicked()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            callback.onAdDismissedFullScreenContent()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            callback.onAdFailedToShowFullScreenContent(PhAdError(error.code, error.message, error.domain))
                        }

                        override fun onAdImpression() {
                            callback.onAdImpression()
                        }

                        override fun onAdShowedFullScreenContent() {
                            callback.onAdShowedFullScreenContent()
                        }
                    }
                    rewardedAd.show(activity) { reward ->
                        rewardedAdCallback.onUserEarnedReward(reward.amount)
                    }
                   // loadRewardedAd(activity, adUnitIdProvider, useTestAds, null)
                }
                is PHResult.Failure -> {
                    callback.onAdFailedToShowFullScreenContent(PhAdError(-1, result.error?.message ?: "", AdError.UNDEFINED_DOMAIN))
                }
            }
        }
    }
}