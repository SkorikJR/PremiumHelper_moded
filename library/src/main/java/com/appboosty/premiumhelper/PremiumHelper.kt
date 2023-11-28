package com.appboosty.premiumhelper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.android.billingclient.api.SkuDetails
import com.applovin.adview.AppLovinFullscreenActivity
import com.facebook.ads.AudienceNetworkActivity
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jakewharton.threetenabp.AndroidThreeTen
import com.appboosty.ads.*
import com.appboosty.ads.applovin.AppLovinNativeAdWrapper
import com.appboosty.ads.config.PHAdSize
import com.appboosty.ads.nativead.PhNativeAdLoadListener
import com.appboosty.ads.nativead.PhNativeAdViewBinder
import com.appboosty.premiumhelper.network.NetworkStateMonitor
import com.appboosty.blytics.PaidImpressionListener
import com.appboosty.blytics.SessionManager
import com.appboosty.premiumhelper.util.ActivePurchase
import com.appboosty.premiumhelper.util.Billing
import com.appboosty.premiumhelper.util.PurchaseResult
import com.appboosty.premiumhelper.util.TimeCappingSuspendable
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.configuration.Configuration.Params.INTERSTITIAL_CAPPING_SECONDS
import com.appboosty.premiumhelper.configuration.Configuration.Params.MAIN_SKU
import com.appboosty.premiumhelper.configuration.Configuration.Params.ONETIME_OFFER
import com.appboosty.premiumhelper.configuration.Configuration.Params.ONETIME_OFFER_STRIKETHROUGH
import com.appboosty.premiumhelper.configuration.Configuration.Params.TOTO_CONFIG_CAPPING_HOURS
import com.appboosty.premiumhelper.configuration.appconfig.PremiumHelperConfiguration
import com.appboosty.premiumhelper.configuration.remoteconfig.RemoteConfig
import com.appboosty.premiumhelper.configuration.testy.TestyConfiguration
import com.appboosty.premiumhelper.log.FileLoggingTree
import com.appboosty.premiumhelper.log.FirebaseCrashReportTree
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.util.InstallReferrer
import com.appboosty.premiumhelper.util.ShakeDetector
import com.appboosty.premiumhelper.performance.StartupPerformanceTracker
import com.appboosty.premiumhelper.toto.TotoFeature
import com.appboosty.premiumhelper.ui.happymoment.HappyMoment
import com.appboosty.premiumhelper.ui.rate.RateHelper
import com.appboosty.premiumhelper.util.FacebookInstallData
import com.appboosty.premiumhelper.ui.relaunch.RelaunchCoordinator
import com.appboosty.premiumhelper.ui.relaunch.RelaunchPremiumActivity
import com.appboosty.premiumhelper.util.AppInstanceId
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import com.appboosty.premiumhelper.util.TimeCapping
import com.appboosty.premiumhelper.util.doOnNextActivityResume
import com.appboosty.premiumhelper.util.doOnNextNonAdActivityResume
import com.appboosty.premiumhelper.util.onError
import com.appboosty.premiumhelper.util.onSuccess
import com.appboosty.premiumhelper.util.successValue
import com.appboosty.premiumhelper.util.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.rx2.rxSingle
import timber.log.Timber
import kotlin.collections.set

@Suppress(
    "RedundantVisibilityModifier",
    "MemberVisibilityCanBePrivate",
    "unused",
    "EXPERIMENTAL_API_USAGE"
)
class PremiumHelper private constructor(
    private val application: Application,
    appConfiguration: PremiumHelperConfiguration
) {

    private val log by timber(TAG)

    private val remoteConfig = RemoteConfig()
    private val testyConfiguration = TestyConfiguration()

    public val appInstanceId = AppInstanceId(application)
    public val preferences = Preferences(application)
    public val configuration: Configuration =
        Configuration(application, remoteConfig, appConfiguration, testyConfiguration)
    public val analytics = Analytics(application, configuration, preferences)

    private val installReferrer = InstallReferrer(application)
    val adManager = com.appboosty.ads.AdManager(application, configuration)
    internal val relaunchCoordinator = RelaunchCoordinator(application, preferences, configuration)
    internal val rateHelper = RateHelper(configuration, preferences)
    private val happyMoment = HappyMoment(rateHelper, configuration, preferences)
    internal val totoFeature = TotoFeature(application, configuration, preferences)
    internal val billing = Billing(application, configuration, preferences, appInstanceId)

    private val _isInitialized = MutableStateFlow(false)
    private val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    private var shakeDetector: ShakeDetector? = null
    val sessionManager = SessionManager(application, configuration)
    private val interstitialState = InterstitialState()

    internal val interstitialCapping: TimeCapping by lazy {
        TimeCapping.ofSeconds(
            capping_seconds = configuration.get(INTERSTITIAL_CAPPING_SECONDS),
            last_call_time = preferences.get("interstitial_capping_timestamp", 0L),
            autoUpdate = false
        )
    }

    private val purchaseRefreshCapping = TimeCapping.ofMinutes(5)

    private val totoConfigCapping = TimeCappingSuspendable.ofHours(
        capping_hours = configuration.get(TOTO_CONFIG_CAPPING_HOURS),
        last_call_time = preferences.get("toto_get_config_timestamp", 0L),
        autoUpdate = false
    )

    init {
        try {
            WorkManager.initialize(application, androidx.work.Configuration.Builder().build())
        } catch (e: java.lang.Exception) {
            Timber.i("WorkManager already initialized")
        }
    }

    companion object {
        internal const val TAG = "PremiumHelper"

        const val FLAG_FROM_NOTIFICATION = "notification"
        const val FLAG_FROM_WIDGET = "widget"
        const val FLAG_FROM_SHORTCUT = "shortcut"

        const val FLAG_SHOW_RELAUNCH = "show_relaunch"

        const val FLAG_INTRO_COMPLETE = "intro_complete"

        const val PREMIUM_HELPER_INITIALIZATION_TIMEOUT = 10000L
        const val PREMIUM_HELPER_FIRST_TIME_INITIALIZATION_TIMEOUT = 20000L

        /**
         * This timeout is included in PREMIUM_HELPER_INITIALIZATION_TIMEOUT and it has to be smaller to allow
         * PH initialization to complete without exception even in case ad manager failed to load ads.
         */
        const val AD_MANAGER_INITIALIZATION_TIMEOUT = PREMIUM_HELPER_INITIALIZATION_TIMEOUT - 1000

        @JvmStatic
        private var instance: PremiumHelper? = null

        @JvmStatic
        fun getInstance(): PremiumHelper {
            val i = instance
            if (i != null) {
                return i
            }

            error("Please call getInstance() with context first")
        }

        @JvmStatic
        fun initialize(application: Application, appConfiguration: PremiumHelperConfiguration) {
            val i = instance

            if (i != null) {
                return
            }

            synchronized(this) {
                val i2 = instance

                if (i2 == null) {
                    StartupPerformanceTracker.getInstance().onPremiumHelperInitialization()
                    val created = PremiumHelper(application, appConfiguration)
                    instance = created
                    created.startInitialization()
                }
            }
        }

        @JvmStatic
        fun onActivityNewIntent(activity: Activity, newIntent: Intent?) {
            activity.intent?.run {
                putExtra(
                    FLAG_FROM_NOTIFICATION,
                    newIntent?.getBooleanExtra(FLAG_FROM_NOTIFICATION, false) ?: false
                )
                putExtra(
                    FLAG_FROM_WIDGET,
                    newIntent?.getBooleanExtra(FLAG_FROM_WIDGET, false) ?: false
                )
                putExtra(
                    FLAG_FROM_SHORTCUT,
                    newIntent?.getBooleanExtra(FLAG_FROM_SHORTCUT, false) ?: false
                )
                putExtra(
                    FLAG_SHOW_RELAUNCH,
                    newIntent?.getBooleanExtra(FLAG_SHOW_RELAUNCH, false) ?: false
                )
            }
        }
    }

    fun addDebugMainOffer(sku: String, price: String) {
        addDebugOffer(MAIN_SKU.key, sku, price)
    }

    fun addDebugOneTimeOffer(
        one_time_sku: String,
        one_time_price: String,
        one_time_strike_sku: String,
        one_time_strike_price: String
    ) {
        addDebugOffer(ONETIME_OFFER.key, one_time_sku, one_time_price)
        addDebugOffer(ONETIME_OFFER_STRIKETHROUGH.key, one_time_strike_sku, one_time_strike_price)
    }

    fun addDebugOffer(key: String, sku: String, price: String) {
        if (configuration.isDebugMode()) {
            val debugSku = "debug_$sku"
            configuration.overrideDebugValue(key, debugSku)
            billing.offerCache[debugSku] = PremiumHelperUtils.buildDebugOffer(debugSku, price)
        } else {
            log.e("You are using the debug-only method on the PRODUCTION build. Please make sure you remove all test code!")
        }
    }

    fun setExternalOnPaidImpressionListener(listener: PaidImpressionListener){
        analytics.setOnPaidImpressionExternalListener(listener)
    }

    public suspend fun getOffer(skuParam: Configuration.ConfigParam.ConfigStringParam): PHResult<Offer> {
        return billing.getOffer(skuParam)
    }

    public fun getOfferRx(skuParam: Configuration.ConfigParam.ConfigStringParam): Single<PHResult<Offer>> {
        setRxErrorHandler()
        return rxSingle { getOffer(skuParam) }.observeOn(AndroidSchedulers.mainThread())
    }

    public fun hasActivePurchase(): Boolean {
        return preferences.hasActivePurchase()
    }

    @JvmOverloads
    public fun showPremiumOffering(activity: Activity, source: String, theme: Int = -1) {
        RelaunchCoordinator.showOffering(activity, source, theme)
    }

    @JvmOverloads
    public fun showPremiumOffering(source: String, theme: Int = -1, flags: Int = -1) {
        RelaunchCoordinator.showOfferingNewTask(application, source, theme, flags)
    }

    /**
     *   Ask user to rate the app and optionally show interstitial ad
     *   Behavior of this method depends on the Configuration.HAPPY_MOMENT_RATE_MODE parameter.
     *
     *   @param activity
     *   @param theme custom theme used for showing validate intent dialog
     *   @param delay optional delay before showing rate ui
     *   @param callback called when happy moment flow is complete
     */
    @JvmOverloads
    public fun onHappyMoment(
        activity: AppCompatActivity,
        theme: Int = -1,
        delay: Int = 0,
        callback: (() -> Unit)? = null
    ) {
        activity.lifecycleScope.launch {
            delay(delay.toLong())
            happyMoment.show(activity, theme, callback)
        }
    }

    /**
     *   Ask user to rate the app and optionally show interstitial ad when user switches to next activity
     *   Behavior of this method depends on the Configuration.HAPPY_MOMENT_RATE_MODE parameter.
     *
     *   @param activity
     *   @param delay optional delay before showing rate ui
     */
    fun showHappyMomentOnNextActivity(activity: AppCompatActivity, delay: Int = 0) {
        activity.doOnNextActivityResume {
            if (!it.isAdActivity() && it !is RelaunchPremiumActivity) {
                if (it is AppCompatActivity) {
                    onHappyMoment(it, delay = delay)
                }
            }
        }
    }

    @JvmOverloads
    public fun showRateDialog(
        fm: FragmentManager,
        theme: Int = -1,
        completeListener: RateHelper.OnRateFlowCompleteListener? = null
    ) {
        rateHelper.showRateIntentDialog(fm, theme, completeListener = completeListener)
    }

    @JvmOverloads
    public fun showInAppReview(
        activity: AppCompatActivity,
        completeListener: RateHelper.OnRateFlowCompleteListener? = null
    ) {
        rateHelper.showInAppReview(activity, completeListener)
    }

    public suspend fun loadBanner(bannerSize: PHAdSize): View? {

        if (hasActivePurchase()) {
            return null
        }

        return adManager.loadBanner(PHAdSize.SizeType.BANNER, bannerSize, object : PhAdListener() {
            override fun onAdOpened() {
                analytics.onAdClick(com.appboosty.ads.AdManager.AdType.BANNER)
            }

            override fun onAdLoaded() {
                //analytics.onAdShown(AdManager.AdType.BANNER)
            }
        })
    }

    public fun loadBannerRx(bannerSize: PHAdSize): Single<PHResult<View>> {
        setRxErrorHandler()
        return rxSingle {
            val view = loadBanner(bannerSize)
            if (view != null) PHResult.Success(view) else PHResult.Failure(
                java.lang.IllegalStateException(
                    ""
                )
            )
        }.observeOn(AndroidSchedulers.mainThread())
    }
    @Deprecated("The API is no longer supported",
        ReplaceWith("loadNativeAdsCommonRx()")
    )
    public fun loadNativeAdmobAdRx(count: Int = 1): Single<PHResult<Unit>> {
        setRxErrorHandler()

        if (preferences.hasActivePurchase()) {
            return Single.just(PHResult.Failure(IllegalStateException("App is purchased")))
        }

        return rxSingle {
            adManager.loadNativeAd(count)
        }.observeOn(AndroidSchedulers.mainThread())
    }

    @Deprecated("The API is no longer supported",
        ReplaceWith("loadNativeAdsCommonRx()")
    )
    public fun getNativeAdmobAdRx(): Single<NativeAd> {
        setRxErrorHandler()
        return rxSingle {
            adManager.getNativeAd()
        }.observeOn(AndroidSchedulers.mainThread())
    }

    @Deprecated("The API is no longer supported",
        ReplaceWith("loadNativeAdsCommon()")
    )
    public suspend fun loadAndGetNativeAdmobAd(): PHResult<NativeAd> {
        return adManager.loadAndGetNativeAd()
    }

    @Deprecated("The API is no longer supported",
        ReplaceWith("loadNativeAdsCommonRx()")
    )
    public fun loadAndGetNativeAdmobAdRx(): Single<PHResult<NativeAd>> {
        setRxErrorHandler()

        if (preferences.hasActivePurchase()) {
            return Single.just(PHResult.Failure(IllegalStateException("App is purchased")))
        }

        return rxSingle {
            adManager.loadAndGetNativeAd()
        }.observeOn(AndroidSchedulers.mainThread())
    }

    public suspend fun loadNativeAdsCommon(binder: PhNativeAdViewBinder, callback: PhNativeAdLoadListener? = null): PHResult<View> {
        return adManager.loadAndGetNativeAdCommon(binder, callback)
    }

    public suspend fun loadNativeAdsCommon(binder: PhNativeAdViewBinder): PHResult<View> {
        return loadNativeAdsCommon(binder, null)
    }

    public fun loadNativeAdsCommonRx(binder: PhNativeAdViewBinder, callback: PhNativeAdLoadListener? = null): Single<PHResult<View>>{
        setRxErrorHandler()

        if (preferences.hasActivePurchase()) {
            return Single.just(PHResult.Failure(IllegalStateException("App is purchased")))
        }

        return rxSingle {
            adManager.loadAndGetNativeAdCommon(binder, callback)
        }.observeOn(AndroidSchedulers.mainThread())
    }

    public fun loadNativeAdsCommonRx(binder: PhNativeAdViewBinder): Single<PHResult<View>> {
        return loadNativeAdsCommonRx(binder, null)
    }

    @Deprecated("The API is no longer supported",
        ReplaceWith("loadNativeAdsCommon()")
    )
    public suspend fun loadNativeAppLovinAd(): PHResult<AppLovinNativeAdWrapper> {
        return adManager.loadAndGetAppLovinNativeAd()
    }

    @Deprecated("The API is no longer supported",
        ReplaceWith("loadNativeAdsCommonRx()")
    )
    public fun loadNativeAppLovinAdRx(): Single<PHResult<AppLovinNativeAdWrapper>> {
        setRxErrorHandler()

        if (preferences.hasActivePurchase()) {
            return Single.just(PHResult.Failure(IllegalStateException("App is purchased")))
        }

        return rxSingle {
            adManager.loadAndGetAppLovinNativeAd()
        }.observeOn(AndroidSchedulers.mainThread())
    }

    public fun getCurrentAdsProvider() = adManager.currentAdsProvider

    public suspend fun waitForInitComplete(): PHResult<Unit> {
        return try {
            try {

                coroutineScope {

                    val minSplashTimeout = async {
                        delay(1500)
                        true
                    }

                    val initActions = async {
                        if (!isInitialized.value) {
                            isInitialized.first { it }
                        }
                        true
                    }


                    withTimeout(getMaxTimeout()) { awaitAll(minSplashTimeout, initActions) }
                }

                analytics.isInitTimerExpired = false
                PHResult.Success(Unit)

            } catch (e: TimeoutCancellationException) {
                log.e("Initialization timeout expired: ${e.message}")
                ignoreNextAppStart() // Do not show relaunch this session
                analytics.isInitTimerExpired = true
                StartupPerformanceTracker.getInstance().setPremiumHelperTimeout(getMaxTimeout())
                PHResult.Failure(e)
            }
        } catch (e: Exception) {
            log.e(e)
            PHResult.Failure(e)
        }
    }

    public fun waitForInitCompleteRx(): Single<PHResult<Unit>> {
        setRxErrorHandler()
        return rxSingle { waitForInitComplete() }.observeOn(AndroidSchedulers.mainThread())
    }

    public fun launchBillingFlow(
        @NonNull activity: Activity,
        @NonNull offer: Offer
    ): Flow<PurchaseResult> {
        return billing.launchBillingFlow(activity, offer)
    }

    @SuppressLint("CheckResult")
    public fun launchBillingFlowRx(
        @NonNull activity: Activity,
        @NonNull offer: Offer
    ): Observable<PurchaseResult> {
        setRxErrorHandler()
        return launchBillingFlow(activity, offer).asObservable()
            .observeOn(AndroidSchedulers.mainThread())
    }

    public fun observePurchaseStatus(): Flow<Boolean> {
        return billing.purchaseStatus
    }

    public fun observePurchaseStatusRx(): Observable<Boolean> {
        setRxErrorHandler()
        return billing.purchaseStatus.asObservable().observeOn(AndroidSchedulers.mainThread())
    }

    public fun observePurchaseResult(): Flow<PurchaseResult> {
        return billing.purchaseResult
    }

    public fun observePurchaseResultRx(): Observable<PurchaseResult> {
        return billing.purchaseResult.asObservable().observeOn(AndroidSchedulers.mainThread())
    }

    public suspend fun getActivePurchases(): PHResult<List<ActivePurchase>> {
        return billing.getActivePurchases()
    }

    public fun getActivePurchasesRx(): Single<PHResult<List<ActivePurchase>>> {
        setRxErrorHandler()
        return rxSingle { getActivePurchases() }.observeOn(AndroidSchedulers.mainThread())
    }

    public suspend fun hasHistoryPurchases(): PHResult<Boolean> {
        return billing.hasHistoryPurchases()
    }

    public fun hasHistoryPurchasesRx(): Single<PHResult<Boolean>> {
        setRxErrorHandler()
        return rxSingle { hasHistoryPurchases() }.observeOn(AndroidSchedulers.mainThread())
    }

    public suspend fun consumeAll(): PHResult<Int> {
        return billing.consumeAll()
    }

    public fun consumeAllRx(): Single<PHResult<Int>> {
        setRxErrorHandler()
        return rxSingle { consumeAll() }.observeOn(AndroidSchedulers.mainThread())
    }

    /**
     *  Check if there is an Interstitial Ad ready to be opened.
     *
     *  @return true if there is a loaded interstitial ready to be shown
     */
    public fun isInterstitialLoaded(): Boolean {
        return adManager.isInterstitialLoaded()
    }

    /**
     *  Show Interstitial Ad.
     *  Capping time set by Configuration.INTERSTITIAL_CAPPING parameter is applied to this call.
     *  If interstitial is capped onAdFailedToShowFullScreenContent() callback is called with error code -2
     *
     *  @param activity Activity
     *  @param callback for getting interstitial events
     */
    public fun showInterstitialAd(
        activity: Activity,
        callback: PhFullScreenContentCallback? = null
    ) {
        showInterstitialAd(activity, callback, false)
    }

    /**
     *  Show Interstitial Ad when next activity is resumed.
     *  Capping time set by Configuration.INTERSTITIAL_CAPPING parameter is applied to this call.
     *  If interstitial is capped onAdFailedToShowFullScreenContent() callback is called with error code -2
     *
     *  @param activity Activity
     */
    public fun showInterstitialAdOnNextActivity(activity: Activity) {
        activity.doOnNextActivityResume {
            if (!it.isAdActivity() && it !is RelaunchPremiumActivity) {
                showInterstitialAd(it, null, false)
            }
        }
    }

    internal fun showInterstitialAd(
        activity: Activity,
        callback: PhFullScreenContentCallback? = null,
        delayed: Boolean = false,
        reportShowEvent: Boolean = true
    ) {
        if (!preferences.hasActivePurchase()) {
            interstitialCapping.runWithCapping(
                onSuccess = {
                    showInterstitialAdNowWithRespectToState(activity, callback, delayed, reportShowEvent)
                },
                onCapped = {
                    callback?.onAdFailedToShowFullScreenContent(
                        PhAdError(
                            -2,
                            "CAPPING_SKIP",
                            "CAPPING"
                        )
                    )
                })
        } else {
            callback?.onAdFailedToShowFullScreenContent(PhAdError(-3, "PURCHASED", "PURCHASED"))
        }
    }

    internal fun showInterstitialAd(activity: Activity, callback: (() -> Unit)? = null) {
        showInterstitialAd(activity, object : PhFullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(p0: PhAdError?) {
                callback?.invoke()
            }

            override fun onAdDismissedFullScreenContent() {
                callback?.invoke()
            }
        })
    }

    /**
     *  Show Interstitial Ad without capping applied.
     *
     *  @param activity Activity
     *  @param callback for getting interstitial events
     */
    public fun showInterstitialAdWithoutCapping(
        activity: Activity,
        callback: PhFullScreenContentCallback? = null
    ) {
        if (!preferences.hasActivePurchase()) {
            showInterstitialAdNowWithRespectToState(activity, callback, false)
        }
    }

    private fun showInterstitialAdNowWithRespectToState(
        activity: Activity,
        callback: PhFullScreenContentCallback?,
        delayed: Boolean,
        reportShowEvent: Boolean = true
    ) {
        synchronized(interstitialState) {
            if (!interstitialState.isAllowedToShow()) {
                log.i("Interstitial skipped because the previous one is still open")
                callback?.onAdFailedToShowFullScreenContent(
                    PhAdError(
                        -2,
                        "INTERSTITIAL ALREADY SHOWN",
                        "STATES"
                    )
                )
                return
            }
            interstitialState.onInterstitialRequested()
        }
        doShowInterstitialAdNow(activity, callback, delayed, reportShowEvent)
    }

    private fun doShowInterstitialAdNow(
        activity: Activity,
        callback: PhFullScreenContentCallback?,
        delayed: Boolean,
        reportShowEvent: Boolean = true
    ) {
        adManager.showInterstitialAd(activity, object : PhFullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(error: PhAdError?) {
                interstitialState.onInterstitialHidden()
                callback?.onAdFailedToShowFullScreenContent(
                    error ?: PhAdError(-1, "", PhAdError.UNDEFINED_DOMAIN)
                )
            }

            override fun onAdClicked() {
                analytics.onAdClick(com.appboosty.ads.AdManager.AdType.INTERSTITIAL)
            }

            override fun onAdShowedFullScreenContent() {
                interstitialState.onInterstitialShown()
                if(reportShowEvent)
                    analytics.onAdShown(com.appboosty.ads.AdManager.AdType.INTERSTITIAL)

                callback?.onAdShowedFullScreenContent()

                // Call callback once any activity is resumed to avoid native onDismissed() callback delay for video ads
                application.doOnNextNonAdActivityResume {
                    // Update capping when ad is dismissed
                    log.i("Update interstitial capping time")
                    interstitialCapping.update()
                    interstitialState.onInterstitialHidden() // Just to be sure, in case the callback wasn't called

                    if (configuration.get(Configuration.INTERSTITIAL_CAPPING_TYPE) == Configuration.CappingType.GLOBAL) {
                        preferences.set(
                            "interstitial_capping_timestamp",
                            System.currentTimeMillis()
                        )
                    }

                    callback?.onAdDismissedFullScreenContent()
                }
            }

            override fun onAdDismissedFullScreenContent() {
            }

        }, delayed)
    }

    @JvmOverloads
    public fun loadRewardedAd(activity: Activity, listener: PhAdListener? = null) {
        if (!preferences.hasActivePurchase()) {
            adManager.loadRewardedAd(activity, listener)
        }
    }

    public fun showRewardedAd(
        activity: Activity,
        rewardedAdCallback: PhOnUserEarnedRewardListener,
        fullScreenContentCallback: PhFullScreenContentCallback?
    ) {
        if (!preferences.hasActivePurchase()) {
            adManager.showRewardedAd(
                activity,
                object: PhOnUserEarnedRewardListener{
                    override fun onUserEarnedReward(amount: Int) {
                        interstitialCapping.update()
                        rewardedAdCallback.onUserEarnedReward(amount)
                    }

                },
                object : PhFullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        analytics.onAdShown(com.appboosty.ads.AdManager.AdType.REWARDED)
                        fullScreenContentCallback?.onAdShowedFullScreenContent()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: PhAdError?) {
                        fullScreenContentCallback?.onAdFailedToShowFullScreenContent(
                            error ?: PhAdError(-1, "", PhAdError.UNDEFINED_DOMAIN)
                        )
                    }

                    override fun onAdDismissedFullScreenContent() {
                        fullScreenContentCallback?.onAdDismissedFullScreenContent()
                    }
                })
        }
    }

    /**
     *  Do not show any relaunch UI on next application start.
     */
    public fun ignoreNextAppStart() {
        preferences.setNextAppStartIgnored(true)
    }

    public fun showPrivacyPolicy(activity: Activity) {
        PremiumHelperUtils.openUrl(activity, configuration.get(Configuration.PRIVACY_URL))
    }

    public fun showTermsAndConditions(activity: Activity) {
        PremiumHelperUtils.openUrl(activity, configuration.get(Configuration.TERMS_URL))
    }

    /**
     *  Call this method from the MainActivity onBackPressed() callback
     *  to open the exit ad or in-app review.
     *
     *  @return true - if application can be closed. Call super.onBackPressed().
     *
     *          false - if premium-helper is showing the exit ad. onBackPressed() is consumed.
     */
    fun onMainActivityBackPressed(activity: Activity): Boolean {

        if (rateHelper.canShowInAppReviewOnExit()) {
            rateHelper.showInAppReview(activity, object : RateHelper.OnRateFlowCompleteListener {
                override fun onRateFlowComplete(
                    reviewUiShown: RateHelper.RateUi,
                    negativeIntent: Boolean
                ) {
                    if (reviewUiShown == RateHelper.RateUi.IN_APP_REVIEW) {
                        activity.finish()
                    } else {
                        if (adManager.onMainActivityBackPressed(activity)) {
                            activity.finish()
                        }
                    }
                }
            })

            return false
        }

        return adManager.onMainActivityBackPressed(activity)
    }

    fun setIntroComplete(value: Boolean = true) {
        preferences.set(FLAG_INTRO_COMPLETE, value)
    }

    fun isIntroComplete(): Boolean {
        return configuration.appConfig.introActivityClass == null || preferences.get(
            FLAG_INTRO_COMPLETE,
            false
        )
    }

    /**
     *  PRIVATE Methods
     */
    private fun startInitialization() {
        if (PremiumHelperUtils.isOnMainProcess(application)) {
            initLogger()
            try {

                Firebase.initialize(application)

                GlobalScope.launch {
                    AndroidThreeTen.init(application)
                    doInitialize()
                }
            } catch (e: Exception) {
                log.e(e, "Initialization failed")
            }
        } else {
            log.e(
                "PremiumHelper initialization disabled for process ${
                    PremiumHelperUtils.getProcessName(
                        application
                    )
                }"
            )
        }
    }

    internal fun updateHappyMomentCapping(){
        happyMoment.updateHappyMomentCapping()
    }

    private fun initLogger() {
        if (configuration.isDebugMode()) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(FirebaseCrashReportTree(application))
        }

        Timber.plant(FileLoggingTree(application, configuration.isDebugMode()))
    }

    private suspend fun doInitialize() {
        log.i("PREMIUM HELPER: ${BuildConfig.VERSION_NAME}")
        log.i(configuration.toString())
        NetworkStateMonitor.getInstance(application)
        StartupPerformanceTracker.getInstance().onGoogleServiceStart()
        PremiumHelperUtils.installGooglePlayServicesProvider(application)
        StartupPerformanceTracker.getInstance().onGoogleServiceEnd()
        analytics.setUserId(appInstanceId.get())
        StartupPerformanceTracker.getInstance().onAnalyticsStart()
        analytics.init()
        StartupPerformanceTracker.getInstance().onAnalyticsEnd()
        analytics.setUserProperty(
            "ph_first_open_time",
            PremiumHelperUtils.getInstalledDate(application)
        )
        coroutineScope {
            awaitAll(
                async(Dispatchers.IO) {
                    remoteConfig.init(application, configuration.isDebugMode())
                },

                async(Dispatchers.IO) {
                    if (configuration.isTotoEnabled()) {
                        totoConfigCapping.runWithCapping({
                            StartupPerformanceTracker.getInstance().onTotoInitializationStart()
                            totoFeature.getConfig()
                                .onSuccess {
                                    StartupPerformanceTracker.getInstance().onTotoInitializationEnd()
                                    totoConfigCapping.update()
                                    preferences.set(
                                        "toto_get_config_timestamp",
                                        System.currentTimeMillis()
                                    )
                                   // Log.d("PurchasesTracker","After toto success -> call to update offer cache")
                                   // billing.updateOfferCache()
                                }.onError {
                                    StartupPerformanceTracker.getInstance().onTotoInitializationEnd()
                                }
                        }, {
                            log.d("Toto configuration skipped due to capping")
                            StartupPerformanceTracker.getInstance().setTotoConfigCapped(true)
                        })
                    } else StartupPerformanceTracker.getInstance().setTotoConfigResult("disabled")
                },

                async(Dispatchers.IO) {
                    StartupPerformanceTracker.getInstance().onTestyStart()
                    testyConfiguration.init(application)
                    StartupPerformanceTracker.getInstance().onTestyEnd()
                }
            )

            Timber.tag("PhConsentManager").d("Configuration is ready")
            adManager.onConfigurationReady()

            awaitAll(
                async(Dispatchers.IO) {
                    adManager.initialize(
                        configuration.isDebugMode() && configuration.appConfig.adManagerTestAds
                    )
                },
                async(Dispatchers.IO) {
                    StartupPerformanceTracker.getInstance().onPurchasesStart()
                    with(getActivePurchases()) {
                        adManager.setPremiumStatus(this.successValue?.isNotEmpty() == true)
                        purchaseRefreshCapping.update()
                        com.appboosty.premiumhelper.performance.StartupPerformanceTracker.getInstance().onPurchasesEnd()
                        this is PHResult.Success
                    }
                })
            billing.updateOfferCache()
            WorkManager.getInstance(application).cancelAllWorkByTag("InitWorker")
            FacebookInstallData(application).fetchAndReport()

            withContext(Dispatchers.Main) {
                registerProcessLifecycleCallbacks()
            }

            if (isDebugMode() && adManager.isDebugPanelSupported()) shakeDetector =
                ShakeDetector(application).apply {
                    shakeListener = object : ShakeDetector.ShakeDetectorListener {
                        override fun onShakeDetected() {
                            if (adManager.currentAdsProvider == Configuration.AdsProvider.APPLOVIN)
                                adManager.showDebugScreen()
                        }
                    }
                }
            StartupPerformanceTracker.getInstance().onPremiumHelperInitialized()
            _isInitialized.value = true
        }
    }

    private fun registerProcessLifecycleCallbacks() {

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {

            private var isColdStart = false

            override fun onStart(owner: LifecycleOwner) {
                log.i(" *********** APP IS FOREGROUND: ${preferences.getAppStartCounter()} COLD START: $isColdStart *********** ")

                if (hasActivePurchase()) {
                    // Refresh active purchases with 5 minutes capping
                    // to detect subscription cancellation
                    purchaseRefreshCapping.runWithCapping {
                        GlobalScope.launch {
                            billing.getActivePurchases()
                        }
                    }
                } else {
                    adManager.onAppOpened()
                }

                if (!isColdStart && configuration.isTotoEnabled()) {
                    GlobalScope.launch {
                        totoConfigCapping.runWithCapping {
                            totoFeature.getConfig()
                                .onSuccess {
                                    totoConfigCapping.update()
                                    preferences.set(
                                        "toto_get_config_timestamp",
                                        System.currentTimeMillis()
                                    )
                                    Log.d("PurchasesTracker","onStart()-> call to update offer cache")
                                    billing.updateOfferCache()
                                }
                        }
                    }
                }

                if (configuration.get(Configuration.INTERSTITIAL_CAPPING_TYPE) == Configuration.CappingType.SESSION) {
                    if (!preferences.isNextAppStartIgnored()) {
                        interstitialCapping.reset()
                    }
                }

                if (preferences.isFirstAppStart() && PremiumHelperUtils.isInstalledFromUpdate(
                        application
                    )
                ) {

                    // App is first opened but installed on top of previous version
                    // Skip onboarding, intro and relaunch
                    log.w("App was just updated - skipping onboarding, intro and relaunch!")

                    analytics.onAppOpened(installReferrer)
                    preferences.incrementAppStartCounter()
                    preferences.setOnboardingComplete()
                    preferences.set(FLAG_INTRO_COMPLETE, true)
                } else {
                    if (!preferences.isNextAppStartIgnored()) {
                        analytics.onAppOpened(installReferrer)
                        relaunchCoordinator.onAppOpened()
                    } else {
                        preferences.setNextAppStartIgnored(false)
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                log.i(" *********** APP IS BACKGROUND *********** ")
                isColdStart = false
                adManager.destroy()
            }

            override fun onCreate(owner: LifecycleOwner) {
                isColdStart = true
            }

        })
    }

    private fun getMaxTimeout(): Long {
        return Long.MAX_VALUE
       // return if (preferences.isFirstAppStart()) PREMIUM_HELPER_FIRST_TIME_INITIALIZATION_TIMEOUT else PREMIUM_HELPER_INITIALIZATION_TIMEOUT
    }

    private fun setRxErrorHandler() {
        if (RxJavaPlugins.getErrorHandler() == null) {
            log.i("PremiumHelper set an undelivered exceptions handler")
            RxJavaPlugins.setErrorHandler { log.e(it) }
        }
    }

    fun isDebugMode(): Boolean = configuration.isDebugMode()

    fun showConsentDialog(activity: AppCompatActivity){
        showConsentDialog(activity, null)
    }

    fun showConsentDialog(activity: AppCompatActivity, onDone: (()-> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            adManager.consentManager.prepareConsentInfoIfNotReady(activity)
            adManager.consentManager.askForConsentIfRequired(
                activity, forced = true) {
                Timber.d("On contest done. Code: ${it.code} Message: ${it.errorMessage}")
                onDone?.invoke()
            }
        }
    }

    fun isConsentAvailable() = adManager.consentManager.isConsentAvailable
}

data class Offer(
    val sku: String,
    val skuType: String?,
    val skuDetails: SkuDetails?
)

fun Activity.isMainActivity(): Boolean {
    return javaClass == PremiumHelper.getInstance().configuration.appConfig.mainActivityClass
}

fun Activity.isIntroActivity(): Boolean {
    return javaClass == PremiumHelper.getInstance().configuration.appConfig.introActivityClass
}

fun Activity.isAdActivity(): Boolean {
    val result = when (this) {
        is AdActivity,
        is AppLovinFullscreenActivity,
        is AudienceNetworkActivity -> true
        else -> false
    }
    return result
}
