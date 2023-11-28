package com.appboosty.ads.applovin

import android.app.Activity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class AppLovinRewardedProvider {
    var externalListener : MaxRewardedAdListener? = null
    suspend fun load(activity: Activity, adUnit: String): PHResult<MaxRewardedAd> {
        return suspendCancellableCoroutine { cont ->
            try {

                val maxRewardedAd = MaxRewardedAd.getInstance(adUnit, activity)
                maxRewardedAd.setRevenueListener { ad ->
                    PremiumHelper.getInstance()
                        .analytics.onPaidImpression(AppLovinRevenueHelper.convertParameters(ad))
                }
                maxRewardedAd.setListener(object : MaxRewardedAdListener {
                    override fun onAdLoaded(ad: MaxAd?) {
                        Timber.tag(PremiumHelper.TAG)
                            .d("AppLovinRewardedProvider: loaded ad ID ${ad?.dspId}")
                        if (cont.isActive)
                            ad?.let { cont.resume(PHResult.Success(maxRewardedAd)) } ?: run {
                                cont.resume(PHResult.Failure(IllegalStateException("AppLovinRewardedProvider: The ad is empty !")))
                            }
                        externalListener?.onAdLoaded(ad)
                    }

                    override fun onAdDisplayed(ad: MaxAd?) {
                        externalListener?.onAdDisplayed(ad)
                    }

                    override fun onAdHidden(ad: MaxAd?) {
                        externalListener?.onAdHidden(ad)
                    }

                    override fun onAdClicked(ad: MaxAd?) {
                        externalListener?.onAdClicked(ad)
                    }

                    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                        Timber.tag(PremiumHelper.TAG)
                            .e("AppLovinRewardedProvider: Failed to load $error")
                        AdsErrorReporter.reportAdErrorAsync(activity, "rewarded", error?.message)
                        if (cont.isActive) {
                            cont.resume(PHResult.Failure(IllegalStateException("AppLovinRewardedProvider: Can't load ad: Error : ${error?.message}")))
                        }
                    }

                    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                        externalListener?.onAdDisplayFailed(ad, error)
                    }

                    override fun onRewardedVideoStarted(ad: MaxAd?) {
                        externalListener?.onRewardedVideoStarted(ad)
                    }

                    override fun onRewardedVideoCompleted(ad: MaxAd?) {
                        externalListener?.onRewardedVideoCompleted(ad)
                    }

                    override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                        externalListener?.onUserRewarded(ad, reward)
                    }

                })
                maxRewardedAd.loadAd()
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }
        }
    }
}