package com.appboosty.premiumhelper.util

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.appboosty.premiumhelper.util.isSuccess
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils.withRetry
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

internal class BillingConnection(context: Context, purchaseUpdateListener: PurchasesUpdatedListener) {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchaseUpdateListener)
        .enablePendingPurchases()
        .build()

    internal suspend fun connect(): BillingClient {

        if (billingClient.isReady) {
            return billingClient
        }

        val result = withRetry(10, maxDelay = 500) { doStartConnection() }

        if (result is PHResult.Failure) {
            error("Connect failure: ${result.error?.message}")
        }

        return billingClient
    }

    private suspend fun doStartConnection(): PHResult<Int> {
        return suspendCancellableCoroutine {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (it.isActive) {
                        if (result.isSuccess()) {
                            it.resume(PHResult.Success(result.responseCode))
                        } else {
                            it.resume(PHResult.Failure(IllegalStateException(result.responseCode.toString())))
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    try {
                        if (it.isActive) {
                            it.resume(PHResult.Failure(IllegalStateException(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED.toString())))
                        }
                    } catch (e: IllegalStateException) {
                        Timber.tag("BillingConnection").e(e)
                    }
                }

            })
        }
    }


}