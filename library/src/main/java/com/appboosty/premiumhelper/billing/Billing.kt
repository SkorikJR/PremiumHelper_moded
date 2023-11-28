package com.appboosty.premiumhelper.util

import android.app.Activity
import android.app.Application
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.appboosty.premiumhelper.AcknowledgePurchaseWorker
import com.appboosty.premiumhelper.Offer
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.performance.PurchasesPerformanceTracker
import com.appboosty.premiumhelper.util.AppInstanceId
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import com.appboosty.premiumhelper.util.onSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.Period
import java.util.*

class Billing(private val application: Application, private val configuration: Configuration, private val preferences: Preferences, private val appInstanceId: AppInstanceId) : PurchasesUpdatedListener {

    private val log by timber(PremiumHelper.TAG)

    private val billingConnection = BillingConnection(application, this)

    private val _purchaseStatus = MutableStateFlow(preferences.hasActivePurchase())
    internal val purchaseStatus: StateFlow<Boolean> = _purchaseStatus.asStateFlow()

    private val _purchaseResult = MutableSharedFlow<PurchaseResult>()
    internal val purchaseResult: SharedFlow<PurchaseResult> = _purchaseResult.asSharedFlow()

    internal var offerCache = Hashtable<String, Offer>()

    suspend fun getOffer(skuParam: Configuration.ConfigParam.ConfigStringParam): PHResult<Offer> {

        val sku = configuration.get(skuParam)

        if (offerCache.containsKey(sku)) {
            PurchasesPerformanceTracker.getInstance().onOffersCacheHit()
            return PHResult.Success(offerCache[sku]!!)
        }

        var result: PHResult<Offer> = PHResult.Failure(IllegalStateException())

        for (retry in 0..10) {
            result = PHResult.suspendOf { queryOffer(sku) }
            if (result is PHResult.Success) {
                log.i("Offer: ${result.value}")
                offerCache[sku] = result.value
                return result
            } else {
                delay(500)
            }
        }

        return result
    }

    suspend fun getActivePurchases(): PHResult<List<ActivePurchase>> {

        return try {

            if (configuration.isDebugMode()) {

                // Check if we have debug offers purchased
                val activePurchaseInfo = preferences.getActivePurchaseInfo()

                if (activePurchaseInfo != null && activePurchaseInfo.purchaseToken.startsWith("debugtoken")) {
                    val purchases = listOf(ActivePurchase(PremiumHelperUtils.buildDebugPurchase(application, activePurchaseInfo.sku), PremiumHelperUtils.buildSkuDetails(activePurchaseInfo.sku, BillingClient.SkuType.SUBS, ""), PurchaseStatus.PAID))
                    log.i("Purchases: $purchases")
                    return PHResult.Success(purchases)
                }

            }

            with(billingConnection.connect()) {

                coroutineScope {

                    val inapp = async { queryPurchases(this@with, BillingClient.SkuType.INAPP) }
                    val subs = async { queryPurchases(this@with, BillingClient.SkuType.SUBS) }

                    val purchases = inapp.await() + subs.await()
                    val premiumPackageInstalled = PremiumHelperUtils.isPremiumPackageInstalled(
                        application, configuration.get(
                            Configuration.PREMIUM_PACKAGES
                        )
                    )

                    preferences.setHasActivePurchases(!purchases.isNullOrEmpty() || premiumPackageInstalled)
                    _purchaseStatus.value = preferences.hasActivePurchase()

                    launch(Dispatchers.IO) {

                        updateActivePurchaseInfo(purchases)

                        if (purchases.isNotEmpty()) {
                            AcknowledgePurchaseWorker.schedule(application)
                            PremiumHelper.getInstance().totoFeature.scheduleRegister()
                        }
                    }

                    log.i("Purchases: $purchases")

                    PHResult.Success(purchases)
                }
            }

        } catch (e: Exception) {
            PHResult.Failure(e)
        }
    }

    internal suspend fun acknowledgeAll(purchases: List<ActivePurchase>? = null) {

        try {
            with(billingConnection.connect()) {
                val purchasesToAcknowledge = if (purchases.isNullOrEmpty()) {
                    queryActivePurchases(this)
                } else {
                    purchases
                }.filter { !it.purchase.isAcknowledged }

                log.i("Acknowledge purchases $purchasesToAcknowledge")
                purchasesToAcknowledge.onEach { acknowledgePurchase(this, it.purchase.purchaseToken) }

            }
        } catch (e: Exception) {
            log.e(e,"Acknowledge all failed")
        }

    }

    private suspend fun acknowledgePurchase(billingClient: BillingClient, @NonNull token: String): BillingResult {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()

        return billingClient.acknowledgePurchase(params).also {
            log.i("Purchase acknowledged: ${it.isSuccess()}")
        }
    }

    private suspend fun queryPurchases(billingClient: BillingClient, @NonNull @BillingClient.SkuType skuType: String): List<ActivePurchase> {

        val response = billingClient.queryPurchasesAsync(skuType)

        if (response.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            return (response.purchasesList).map { purchase ->
                val skuDetails: SkuDetails = querySkuDetails(billingClient, purchase.skus[0], skuType)
                ActivePurchase(purchase, skuDetails, getPurchaseStatus(purchase, skuDetails))
            }
        }

        throw java.lang.IllegalStateException("Failed to query purchases: ${response.billingResult.responseCode}")
    }

    private suspend fun queryOffer(sku: String): Offer {
        return with(billingConnection.connect()) {
            val skuDetails = querySkuDetails(this, sku)
            Offer(skuDetails.sku, skuDetails.type, skuDetails)
        }

    }

    private suspend fun querySkuDetails(billingClient: BillingClient, @NonNull sku: String): SkuDetails {
        return try {
            querySkuDetails(billingClient, sku, BillingClient.SkuType.SUBS)
        } catch (e: Exception) {
            querySkuDetails(billingClient, sku, BillingClient.SkuType.INAPP)
        }
    }

    private suspend fun querySkuDetails(billingClient: BillingClient, @NonNull sku: String, @BillingClient.SkuType skuType: String?): SkuDetails {
        return if (skuType.isNullOrEmpty()) {
            querySkuDetails(billingClient, sku)
        } else {
            val skuParams: SkuDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(mutableListOf(sku))
                .setType(skuType)
                .build()

            val result = querySkuWithRetries(billingClient, skuParams)

            if (result.isSuccess()) {
                return result.skuDetailsList!![0]
            }

            throw java.lang.IllegalStateException("Failed to get sku $sku ${result.billingResult.responseCode} $skuType")
        }
    }

    private suspend fun querySkuWithRetries(billingClient: BillingClient, skuParams: SkuDetailsParams): SkuDetailsResult {

        var retry = 0
        var result = billingClient.querySkuDetails(skuParams)

        while (retry < 5 && result.shouldRetry()) {
            retry++
            delay(500)
            result = billingClient.querySkuDetails(skuParams)
        }

        return result
    }

    private fun getPurchaseStatus(@NonNull purchase: Purchase, skuDetails: SkuDetails?): PurchaseStatus {
        return if (skuDetails != null) {
            if (skuDetails.type == BillingClient.SkuType.INAPP) {
                PurchaseStatus.PAID
            } else {
                if (isCancelled(purchase)) {
                    if (isTrialExpired(purchase, skuDetails)) {
                        PurchaseStatus.SUBSCRIPTION_CANCELLED
                    } else {
                        PurchaseStatus.TRIAL_CANCELLED
                    }
                } else {
                    if (isTrialExpired(purchase, skuDetails)) {
                        PurchaseStatus.PAID
                    } else {
                        PurchaseStatus.TRIAL
                    }
                }
            }
        } else {
            PurchaseStatus.UNKNOWN
        }
    }

    private fun isTrialExpired(@NonNull purchase: Purchase, skuDetails: SkuDetails): Boolean {
        return try {
            if (skuDetails.freeTrialPeriod.isEmpty()) {
                true
            } else {
                val trialExpire = Instant.ofEpochMilli(purchase.purchaseTime).plus(Period.parse(skuDetails.freeTrialPeriod))
                trialExpire.isBefore(Instant.now())
            }
        } catch (e: Exception) {
            log.e(e, "Trial check failed for ${skuDetails.sku} trial period is: ${skuDetails.freeTrialPeriod}")
            false
        }
    }

    private fun isCancelled(purchase: Purchase): Boolean {
        return !purchase.isAutoRenewing
    }

    private suspend fun handlePurchaseUpdate(purchases: MutableList<Purchase>): List<ActivePurchase> {
        with(billingConnection.connect()) {
            val activePurchases = purchases.onEach {
                if (!it.isAcknowledged) {
                    PHResult.suspendOf { acknowledgePurchase(this, it.purchaseToken) }
                        .onSuccess { response ->
                            if (response.isSuccess()) {
                                log.d("Auto Acknowledge $it result: ${response.responseCode}")
                            } else {
                                log.e("Auto Acknowledge $it failed ${response.responseCode}")
                            }
                        }
                }
            }.map {
                val skuDetails = try {
                    if (it.orderId.startsWith("DEBUG.OFFER")) {
                        PremiumHelperUtils.buildSkuDetails(
                            it.skus[0],
                            BillingClient.SkuType.SUBS,
                            ""
                        )
                    } else {
                        querySkuDetails(this, it.skus[0])
                    }
                } catch (e: Exception) {
                    null
                }

                ActivePurchase(it, skuDetails, getPurchaseStatus(it, skuDetails))
            }

            val premiumPackageInstalled = PremiumHelperUtils.isPremiumPackageInstalled(
                application, configuration.get(
                    Configuration.PREMIUM_PACKAGES
                )
            )

            preferences.setHasActivePurchases(activePurchases.isNotEmpty() || premiumPackageInstalled)
            _purchaseStatus.value = preferences.hasActivePurchase()

            return activePurchases
        }
    }

    private fun updateActivePurchaseInfo(purchases: List<ActivePurchase>) {
        if (purchases.isNotEmpty()) {
            val ap = purchases[0]
            preferences.setActivePurchaseInfo(
                ActivePurchaseInfo(
                    ap.purchase.skus[0],
                    ap.purchase.purchaseToken,
                    ap.purchase.purchaseTime,
                    ap.status
                )
            )
        } else {
            preferences.clearActivePurchaseInfo()
        }
    }

    @DelicateCoroutinesApi
    internal fun updateOfferCache() {
        if (PremiumHelper.getInstance().hasActivePurchase()) return

        GlobalScope.launch {
            coroutineScope {
                launch(Dispatchers.Default) {
                    try {
                        PurchasesPerformanceTracker.getInstance().onUpdateOffersCacheStart()
                        with(billingConnection.connect()) {
                            listOf(Configuration.MAIN_SKU, Configuration.ONETIME_OFFER, Configuration.ONETIME_OFFER_STRIKETHROUGH).onEach { p ->
                                try {
                                    val sku = configuration.get(p)

                                    if (sku.isNotEmpty()) {
                                        val skuDetails = querySkuDetails(this, sku)
                                        val offer = Offer(skuDetails.sku, skuDetails.type, skuDetails)
                                        log.i("Offer cached: $offer")
                                        offerCache[sku] = offer
                                    }

                                } catch (e: Exception) {
                                    log.e(e, "Failed to load offer for sku: $p")
                                    com.appboosty.premiumhelper.performance.PurchasesPerformanceTracker.getInstance().addFailedSku(p.key)
                                }
                            }
                            com.appboosty.premiumhelper.performance.PurchasesPerformanceTracker.getInstance().onUpdateOffersCacheEnd()
                        }
                    } catch (e: java.lang.Exception) {
                        log.e(e, "Offer cache update failed")
                    }
                }
            }
        }
    }

    suspend fun hasHistoryPurchases(): PHResult<Boolean> {
        return try {
            with(billingConnection.connect()) {
                coroutineScope {
                    val inapp = async { hasPurchased(this@with, BillingClient.SkuType.INAPP) }
                    val subs = async { hasPurchased(this@with, BillingClient.SkuType.SUBS) }

                    PHResult.Success(inapp.await() || subs.await())
                }
            }
        } catch (e: Exception) {
            PHResult.Failure(e)
        }
    }

    suspend fun consumeAll(): PHResult<Int> {
        return try {
            with(billingConnection.connect()) {
                val purchases = queryPurchases(this, BillingClient.SkuType.INAPP)
                purchases.onEach {
                    val result = consumePurchase(
                        ConsumeParams.newBuilder().setPurchaseToken(it.purchase.purchaseToken)
                            .build()
                    )
                    log.i("Consume ${it.purchase.skus}: ${result.billingResult.responseCode}")
                }
                PHResult.Success(purchases.size)
            }
        } catch (e: java.lang.Exception) {
            PHResult.Failure(e)
        }
    }

    private suspend fun hasPurchased(billingClient: BillingClient, @NonNull @BillingClient.SkuType skuType: String): Boolean {
        return !queryPurchaseHistory(billingClient, skuType).isNullOrEmpty()
    }

    private suspend fun queryPurchaseHistory(billingClient: BillingClient, @NonNull @BillingClient.SkuType skuType: String): List<PurchaseHistoryRecord> {

        val purchaseHistoryResponse = billingClient.queryPurchaseHistory(skuType)

        val result = if (purchaseHistoryResponse.billingResult.isSuccess() && !purchaseHistoryResponse.purchaseHistoryRecordList.isNullOrEmpty()) {
            purchaseHistoryResponse.purchaseHistoryRecordList!!
        } else {
            emptyList()
        }

        if (configuration.isDebugMode()) {
            result.onEach { log.i("History purchase: $it") }
        }

        return result
    }

    fun launchBillingFlow(@NonNull activity: Activity, @NonNull offer: Offer): Flow<PurchaseResult> {
        if(activity is LifecycleOwner) {
            (activity as LifecycleOwner).lifecycleScope.launch {

                try {
                    if (offer.isDebug()) {
                        launchDebugBillingFlow(activity, offer)
                    } else {
                        with(billingConnection.connect()) {

                            val skuDetails =
                                offer.skuDetails ?: querySkuDetails(this, offer.sku, offer.skuType)

                            val billingParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetails)
                                .setObfuscatedAccountId(appInstanceId.get())
                                .setIsOfferPersonalized(true)
                                .build()

                            log.i("Launching billing flow for offer: $offer")

                            launchBillingFlow(activity, billingParams)
                        }
                    }

                } catch (e: Exception) {
                    log.e(e)
                    _purchaseResult.emit(
                        PurchaseResult(
                            BillingResult.newBuilder().setDebugMessage(e.message ?: "")
                                .setResponseCode(
                                    BillingClient.BillingResponseCode.DEVELOPER_ERROR
                                ).build()
                        )
                    )
                }
            }
        }

        return purchaseResult.distinctUntilChanged()
    }

    private fun launchDebugBillingFlow(activity: Activity, offer: Offer) {
        AlertDialog.Builder(activity)
            .setTitle("Purchase debug offer?")
            .setMessage("You are trying to purchase a DEBUG offer. This purchase is for testing only, Google Play is not updated.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Test Purchase") { _, _ ->
                GlobalScope.launch {

                    val purchases = mutableListOf(
                        PremiumHelperUtils.buildDebugPurchase(
                            application,
                            offer.sku
                        )
                    )

                    val activePurchases = purchases.map {
                        val skuDetails = try {
                            PremiumHelperUtils.buildSkuDetails(
                                it.skus[0],
                                BillingClient.SkuType.SUBS,
                                ""
                            )
                        } catch (e: Exception) {
                            null
                        }

                        ActivePurchase(it, skuDetails, getPurchaseStatus(it, skuDetails))
                    }

                    val premiumPackageInstalled = PremiumHelperUtils.isPremiumPackageInstalled(
                        application, configuration.get(
                            Configuration.PREMIUM_PACKAGES
                        )
                    )

                    preferences.setHasActivePurchases(activePurchases.isNotEmpty() || premiumPackageInstalled)
                    _purchaseStatus.value = preferences.hasActivePurchase()

                    updateActivePurchaseInfo(activePurchases)

                    if (activePurchases.isNotEmpty()) {
                        PremiumHelper.getInstance().totoFeature.scheduleRegister(true)
                        AcknowledgePurchaseWorker.schedule(application)
                    }

                    _purchaseResult.emit(PurchaseResult(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), activePurchases))

                }
            }
            .show()
    }

    private suspend fun queryActivePurchases(billingClient: BillingClient): List<ActivePurchase> {
        return coroutineScope {
            val inapp = async { queryPurchases(billingClient, BillingClient.SkuType.INAPP) }
            val subs = async { queryPurchases(billingClient, BillingClient.SkuType.SUBS) }
            inapp.await() + subs.await()
        }
    }

    override fun onPurchasesUpdated(@NonNull result: BillingResult, purchases: MutableList<Purchase>?) {

        log.i("onPurchaseUpdated: $purchases Result: ${result.responseCode}")

        try {

            GlobalScope.launch {
                if (result.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {

                    val activePurchases = handlePurchaseUpdate(purchases)

                    updateActivePurchaseInfo(activePurchases)

                    if (activePurchases.isNotEmpty()) {
                        PremiumHelper.getInstance().totoFeature.scheduleRegister(true)
                        AcknowledgePurchaseWorker.schedule(application)
                    }

                    _purchaseResult.emit(PurchaseResult(result, activePurchases))

                } else {
                    _purchaseResult.emit(PurchaseResult(result))
                }
            }

        } catch (e: Exception) {
            log.e(e)
        }

    }

}

data class PurchaseResult(
    val billingResult: BillingResult,
    val purchases: List<ActivePurchase>? = null
) {
    fun isSuccess(): Boolean {
        return billingResult.isSuccess()
    }
}

data class ActivePurchaseInfo(val sku: String, val purchaseToken: String, val purchaseTime: Long, val status: PurchaseStatus?)

data class ActivePurchase(
    val purchase: Purchase,
    val skuDetails: SkuDetails? = null,
    val status: PurchaseStatus = PurchaseStatus.UNKNOWN
) {
    override fun toString(): String {
        return "\nActivePurchase: ${status.name}" +
                "\nPurchase JSON:\n${JSONObject(purchase.originalJson).toString(4)}" +
                "\nSkuDetails JSON: \n${JSONObject(skuDetails?.originalJson ?: "null").toString(4)}"
    }
}

enum class PurchaseStatus(val value: String) {
    UNKNOWN(""),
    TRIAL("trial"),
    TRIAL_CANCELLED("trial_cancelled"),
    SUBSCRIPTION_CANCELLED("subscription_cancelled"),
    PAID("paid")
}

fun BillingResult.isSuccess(): Boolean {
    return this.responseCode == BillingClient.BillingResponseCode.OK
}

fun Offer.isDebug(): Boolean {
    return skuDetails?.description == "debug-offer"
}

fun SkuDetailsResult.isSuccess(): Boolean {
    return billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()
}

fun SkuDetailsResult.shouldRetry(): Boolean {
    return !isSuccess() && (
            billingResult.responseCode == BillingClient.BillingResponseCode.OK ||
                    billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
}
