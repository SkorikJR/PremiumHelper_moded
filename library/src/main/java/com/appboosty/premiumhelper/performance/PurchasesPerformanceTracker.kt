package com.appboosty.premiumhelper.performance

import android.os.Bundle
import androidx.core.os.bundleOf
import com.appboosty.premiumhelper.PremiumHelper
import timber.log.Timber
import java.util.*

class PurchasesPerformanceTracker private constructor() : BaseTracker() {
    companion object {
        private const val TAG = "PurchasesTracker"
        private var instance: PurchasesPerformanceTracker? = null

        fun getInstance(): PurchasesPerformanceTracker {
            return instance ?: run {
                instance = PurchasesPerformanceTracker()
                instance!!
            }
        }
    }

    private var offersLoadingData: SkuLoadingData? = null

    fun onStartLoadOffers() {
        offersLoadingData?.let {
            it.offersStartLoadTime = System.currentTimeMillis()
            it.cachePrepared = it.updateOffersCacheEnd != 0L
        }
    }

    fun onEndLoadOffers() {
        offersLoadingData?.offersEndLoadTime = System.currentTimeMillis()
        sendEvent()
    }

    fun onOffersCacheHit() {
        offersLoadingData?.offersCacheHit = true
    }

    fun setOfferScreenName(screenName: String) {
        offersLoadingData?.screenName = screenName
    }

    fun setOneTimeOfferAvailable() {
        offersLoadingData?.isOneTimeOffer = true
    }

    fun onUpdateOffersCacheStart() {
        offersLoadingData?.let {
            it.updateOffersCacheStart = System.currentTimeMillis()
        } ?: run {
            offersLoadingData = SkuLoadingData().apply {
                updateOffersCacheStart = System.currentTimeMillis()
            }
        }
    }

    fun onUpdateOffersCacheEnd() {
        offersLoadingData?.updateOffersCacheEnd = System.currentTimeMillis()
    }

    fun addFailedSku(sku: String) {
        offersLoadingData?.failedSkuList?.add(sku)
    }

    private fun sendEvent() {
        offersLoadingData?.let { data ->
            offersLoadingData = null
            sendEvent {
                val params = data.toBundle()
                Timber.tag(TAG).v(params.toString())
                PremiumHelper.getInstance().analytics.sendPurchasesPerformanceData(params)
            }
        }
    }


    data class SkuLoadingData(
        var offersStartLoadTime: Long = 0,
        var offersEndLoadTime: Long = 0,
        var offersCacheHit: Boolean = false,
        var screenName: String = "",
        var isOneTimeOffer: Boolean = false,
        var updateOffersCacheStart: Long = 0,
        var updateOffersCacheEnd: Long = 0,
        var failedSkuList: LinkedList<String> = LinkedList(),
        var cachePrepared: Boolean = false
    ) : BasePerformanceDataClass() {

        fun toBundle(): Bundle {
            return bundleOf(
                "offers_loading_time" to calculateDuration(offersEndLoadTime, offersStartLoadTime),
                "offers_cache_hit" to booleanToString(offersCacheHit),
                "screen_name" to screenName,
                "update_offers_cache_time" to calculateDuration(
                    updateOffersCacheEnd,
                    updateOffersCacheStart
                ),
                "failed_skus" to listToCsv(failedSkuList),
                "cache_prepared" to booleanToString(cachePrepared)
            )
        }
    }
}