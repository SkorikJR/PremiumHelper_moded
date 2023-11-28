package com.appboosty.ads.admob

import android.content.Context
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.appboosty.ads.AdsErrorReporter
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class AdMobRewardedProvider(private val adUnitId: String) {

    suspend fun load(context: Context): PHResult<RewardedAd> {
        return suspendCancellableCoroutine { cont->

            try {

                val adRequest = AdManagerAdRequest.Builder().build()

                RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {

                        Timber.tag(TAG).d("AdMobRewarded: loaded ad from ${ad.responseInfo.mediationAdapterClassName}")

                        if (cont.isActive) {

                            ad.onPaidEventListener =
                                OnPaidEventListener { adValue -> PremiumHelper.getInstance().analytics.onPaidImpression(adUnitId, adValue, ad.responseInfo.mediationAdapterClassName) }

                            cont.resume(PHResult.Success(ad))
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {

                        Timber.tag(TAG).e("AdMobRewarded: Failed to load ${error.code} (${error.message})")
                        AdsErrorReporter.reportAdErrorAsync(context, "rewarded", error.message)
                        if (cont.isActive) {
                            cont.resume(PHResult.Failure(IllegalStateException(error.message)))
                        }
                    }
                })

            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PHResult.Failure(e))
                }
            }
        }
    }
}