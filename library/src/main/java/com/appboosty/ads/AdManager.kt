package com.appboosty.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.nativead.NativeAd
import com.appboosty.ads.admob.*
import com.appboosty.ads.applovin.*
import com.appboosty.ads.config.PHAdSize
import com.appboosty.ads.exitads.ExitAds
import com.appboosty.ads.nativead.PhNativeAdLoadListener
import com.appboosty.ads.nativead.PhNativeAdViewBinder
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.performance.AdsLoadingPerformance
import com.appboosty.premiumhelper.performance.StartupPerformanceTracker
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AdManager(private val application: Application, private val configuration: Configuration) {

    private val log by timber(TAG)


    private var useTestAds = false
    var currentAdsProvider: Configuration.AdsProvider = Configuration.AdsProvider.ADMOB
        private set
    private var interstitialManager: com.appboosty.ads.InterstitialManager? = null
    private lateinit var adUnitIdProvider: com.appboosty.ads.AdUnitIdProvider
    private var rewardedAdManager: com.appboosty.ads.RewardedAdManager? = null


    private var exitAds: ExitAds? = null

    internal val consentManager by lazy { com.appboosty.ads.PhConsentManager(application) }

    private val isInitialized = MutableStateFlow(false)
    private val isPremium = MutableStateFlow<Boolean?>(null)
    private val isConfigurationReady = MutableStateFlow<Boolean?>(null)

    enum class AdType {
        INTERSTITIAL, BANNER, NATIVE, REWARDED, BANNER_MEDIUM_RECT
    }

    companion object {
        const val AD_DISABLED = "disabled"
        private val DEBUG_SCREEN_PROVIDERS = listOf(Configuration.AdsProvider.APPLOVIN)
    }

    suspend fun initialize(testAds: Boolean = false) {
        useTestAds = testAds
        waitForInitComplete()
    }

    fun prepareConsentInfo(
        activity: AppCompatActivity,
        onConsentFormRequired: (() -> Unit)? = null,
        onConsentFormNotRequired: (() -> Unit)? = null
    ) {
        Timber.tag("PhConsentManager").d("AdManager.prepareConsentInfo()-> Start to prepare consent info")
        consentManager.prepareConsentInfo(activity, onConsentFormNotRequired = {
            CoroutineScope(Dispatchers.Main).launch {
                initializeAdSDK()
            }
        }, onConsentFormRequired = onConsentFormRequired)
    }

    internal suspend fun askForConsentIfRequired(activity: AppCompatActivity, onContinue: () -> Unit) {
        waitForPremiumStatus()
        if (PremiumHelper.getInstance().hasActivePurchase()) {
            initializeAdSDK()
            onContinue.invoke()
        } else {
            consentManager.askForConsentIfRequired(activity, onDone = {
                CoroutineScope(Dispatchers.IO).launch {
                    initializeAdSDK()
                }
                onContinue.invoke()
            })
        }
    }

    internal suspend fun setPremiumStatus(isPremium: Boolean) {
        this.isPremium.emit(isPremium)
    }

    internal suspend fun onConfigurationReady() {
        this.isConfigurationReady.emit(true)
    }


    private suspend fun initializeAdSDK() {
        waitForConfiguration()

        StartupPerformanceTracker.getInstance().onAdManagerInitializationStart()
        currentAdsProvider = configuration.get(Configuration.ADS_PROVIDER)
        StartupPerformanceTracker.getInstance().setAdProvider(currentAdsProvider.name)
        initAdsProvider(currentAdsProvider)

        coroutineScope {
            launch(Dispatchers.IO) {
                when (currentAdsProvider) {
                    Configuration.AdsProvider.ADMOB -> {
                        val status = try {
                            withTimeout(PremiumHelper.AD_MANAGER_INITIALIZATION_TIMEOUT) {
                                suspendCancellableCoroutine { cont ->
                                    MobileAds.initialize(application) { status ->
                                        if (cont.isActive) {
                                            cont.resume(status)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            log.e("AdManager: initialize timeout!")
                            InitializationStatus { mutableMapOf() }
                        }
                        StartupPerformanceTracker.getInstance().onAdManagerInitializationEnd()
                        isInitialized.emit(true)
                        log.d("AdManager with AdMob initialized:\n${status.getStatusAsString()}")
                    }

                    Configuration.AdsProvider.APPLOVIN -> {
                        try {
                            withTimeout(PremiumHelper.AD_MANAGER_INITIALIZATION_TIMEOUT) {
                                suspendCancellableCoroutine { cont ->
                                    launch(Dispatchers.Main) {
                                        initAppLovin()
                                        withContext(Dispatchers.IO) {
                                            if (cont.isActive) {
                                                cont.resume(InitializationStatus { mutableMapOf() })
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            log.e("AppLovinManager: initialize timeout!")
                            InitializationStatus { mutableMapOf() }
                        }
                        log.d("AdManager with AppLovin initialized")
                        StartupPerformanceTracker.getInstance().onAdManagerInitializationEnd()
                        isInitialized.emit(true)
                    }
                }
            }
        }
    }

    private fun initAdsProvider(adsProvider: Configuration.AdsProvider) {
        log.d("initAdsProvider()-> Provider: $adsProvider")
        when (adsProvider) {
            Configuration.AdsProvider.ADMOB -> {
                log.d("initAdsProvider()-> initializing ADMOB provider")
                adUnitIdProvider = AdMobUnitIdProvider()
                interstitialManager = AdMobInterstitialManager()
                rewardedAdManager = AdMobRewardedAdManager()
            }

            Configuration.AdsProvider.APPLOVIN -> {
                log.d("initAdsProvider()-> initializing APPLOVIN provider")
                adUnitIdProvider = AppLovinUnitIdProvider()
                interstitialManager = AppLovinInterstitialManager()
                rewardedAdManager = AppLovinRewardedAdManager()
            }
        }
        exitAds = ExitAds(this, application)
        log.d("initAdsProvider()-> Finished")
    }

    private suspend fun initAppLovin(): Boolean {
        return suspendCoroutine { cont ->
            AppLovinPrivacySettings.setHasUserConsent(true, application)
            AppLovinPrivacySettings.setIsAgeRestrictedUser(false, application)

            val settings = AppLovinSdkSettings(application)
            configuration.appConfig.debugData?.getStringArray("test_advertising_ids")?.let {
                settings.testDeviceAdvertisingIds = it.toList()
            }

            val appLovinSdk = AppLovinSdk.getInstance(settings, application)
            appLovinSdk.mediationProvider = AppLovinMediationProvider.MAX
            appLovinSdk.initializeSdk {
                log.d("AppLovin onInitialization complete called")
                cont.resume(true)
            }
        }
    }

    fun onAppOpened() {
        setInterstitialMuteMode()
        //loadInterstitial()
        exitAds?.init()
    }

    private fun setInterstitialMuteMode() {
        runCatching {
            if (PremiumHelper.getInstance().configuration.get(Configuration.INTERSTITIAL_MUTED)) {
                when (currentAdsProvider) {
                    Configuration.AdsProvider.ADMOB -> MobileAds.setAppMuted(true)
                    Configuration.AdsProvider.APPLOVIN -> AppLovinSdk.getInstance(application).settings.isMuted =
                        true
                }
            }
        }
    }

    fun loadInterstitial(activity: Activity) {
        interstitialManager?.loadInterstitial(activity, adUnitIdProvider, useTestAds = useTestAds)
            ?: run {
                log.e("loadInterstitial()-> AdManager is not initialized !")
            }
    }

    fun clearInterstitials() {
        interstitialManager?.clearInterstitials()
    }

    fun loadRewardedAd(activity: Activity, listener: com.appboosty.ads.PhAdListener? = null) {
        rewardedAdManager?.loadRewardedAd(activity, adUnitIdProvider, useTestAds, listener) ?: run {
            log.e("loadRewardedAd()-> AdManager is not initialized !")
        }
    }

    suspend fun loadBanner(
        sizeType: PHAdSize.SizeType,
        size: PHAdSize? = null,
        adListener: com.appboosty.ads.PhAdListener,
        isExitAd: Boolean = false,
        adUnit: String? = null
    ): View? {
        val result = try {
            withContext(Dispatchers.Main) {
                if (!this@AdManager::adUnitIdProvider.isInitialized) {
                    throw java.lang.IllegalArgumentException("AdManager wasn't initialized !")
                }
                when (currentAdsProvider) {
                    Configuration.AdsProvider.ADMOB -> {
                        val unitId = adUnit?: adUnitIdProvider.getAdUnitId(
                            com.appboosty.ads.AdManager.AdType.BANNER,
                            isExitAd,
                            useTestAds = useTestAds
                        )
                        log.d("AdManager: Loading banner ad: ($unitId, $isExitAd)")
                        AdMobBannerProvider(unitId)
                            .load(application, size, adListener)
                    }

                    Configuration.AdsProvider.APPLOVIN -> {
                        val adType = when (sizeType) {
                            PHAdSize.SizeType.ADAPTIVE, PHAdSize.SizeType.MEDIUM_RECTANGLE -> com.appboosty.ads.AdManager.AdType.BANNER_MEDIUM_RECT
                            else -> com.appboosty.ads.AdManager.AdType.BANNER
                        }
                        val unitId = adUnit?: adUnitIdProvider.getAdUnitId(
                            adType,
                            isExitAd,
                            useTestAds = useTestAds
                        )
                        log.d("AdManager: Loading applovin banner ad. AdUnitId: $unitId is Exit: ($isExitAd)")
                        if (unitId.isEmpty())
                            throw java.lang.IllegalArgumentException("Ad unit id is empty. Size: ${adType.name}")
                        else
                            AppLovinBannerProvider().load(application, unitId, size, adListener)
                    }
                }
            }
        } catch (e: Exception) {
            PHResult.Failure(e)
        }

        return when (result) {
            is PHResult.Success -> result.value
            is PHResult.Failure -> {
                log.e(result.error, "AdManager: Failed to load banner ad")
                null
            }
        }
    }

    private val nativeAds = Channel<NativeAd>()

    fun isAdEnabled(adType: com.appboosty.ads.AdManager.AdType, isExitAd: Boolean): Boolean {
        if (!this::adUnitIdProvider.isInitialized) return false
        val adUnitId = adUnitIdProvider.getAdUnitId(adType, isExitAd, useTestAds = useTestAds)
            .takeIf { it.isNotEmpty() } ?: com.appboosty.ads.AdManager.Companion.AD_DISABLED
        return adUnitId != com.appboosty.ads.AdManager.Companion.AD_DISABLED
    }

    suspend fun loadNativeAd(count: Int = 1, isExitAd: Boolean = false): PHResult<Unit> {

        return try {
            val adUnitId =
                adUnitIdProvider.getAdUnitId(com.appboosty.ads.AdManager.AdType.NATIVE, isExitAd, useTestAds = useTestAds)

            log.d("AdManager: Loading native ad: ($adUnitId, $isExitAd)")
            when (currentAdsProvider) {
                Configuration.AdsProvider.ADMOB -> {
                    AdMobNativeProvider(adUnitId)
                        .load(application, count, object : com.appboosty.ads.PhAdListener() {

                            override fun onAdClicked() {
                                if (isExitAd) {
                                    PremiumHelper.getInstance().analytics.onExitAdClick()
                                }
                                PremiumHelper.getInstance().analytics.onAdClick(com.appboosty.ads.AdManager.AdType.NATIVE)

                            }
                        }, { ad ->
                            GlobalScope.launch {
                                nativeAds.send(ad)
                            }
                        }, isExitAd)
                }

                Configuration.AdsProvider.APPLOVIN -> {
                    PHResult.Failure(IllegalArgumentException("Native ads are not supported yet with AppLoving provider"))
                }
            }
        } catch (e: Exception) {
            log.e(e, "AdManager: Failed to load native ad")
            PHResult.Failure(e)
        }
    }

    suspend fun getNativeAd(timeout: Long = Long.MAX_VALUE): NativeAd {
        return withTimeout(timeout) {
            nativeAds.receive()
        }
    }

    suspend fun loadAndGetNativeAd(
        isExitAd: Boolean = false,
        adUnitId: String? = null
    ): PHResult<NativeAd> {
        return try {
            val unitId = adUnitId ?: adUnitIdProvider.getAdUnitId(
                com.appboosty.ads.AdManager.AdType.NATIVE,
                isExitAd,
                useTestAds = useTestAds
            )
            log.d("AdManager: Loading native ad: ($unitId, $isExitAd)")
            suspendCancellableCoroutine { cont ->
                GlobalScope.launch {
                    when (currentAdsProvider) {
                        Configuration.AdsProvider.ADMOB -> {
                            AdMobNativeProvider(unitId)
                                .load(application, 1,
                                    object : com.appboosty.ads.PhAdListener() {
                                        override fun onAdFailedToLoad(error: com.appboosty.ads.PhLoadAdError) {
                                            cont.resume(PHResult.Failure(IllegalStateException(error.message)))
                                        }
                                    }, { ad ->
                                        if (cont.isActive) {
                                            cont.resume(PHResult.Success(ad))
                                        }
                                    }, isExitAd
                                )
                        }

                        Configuration.AdsProvider.APPLOVIN -> {
                            cont.resume(PHResult.Failure(java.lang.IllegalStateException("This function is used to load AdMob native apps only. For AppLovin use loadAndGetAppLovinNativeAd()")))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e, "AdManager: Failed to load native ad")
            PHResult.Failure(e)
        }
    }

    suspend fun loadAndGetAppLovinNativeAd(
        isExitAd: Boolean = false,
        adUnitId: String? = null
    ): PHResult<AppLovinNativeAdWrapper> {
        return try {
            val unitId = adUnitId ?: adUnitIdProvider.getAdUnitId(
                com.appboosty.ads.AdManager.AdType.NATIVE,
                isExitAd,
                useTestAds = useTestAds
            )
            log.d("AdManager: Loading applovin native ad: ($unitId, $isExitAd)")
            suspendCancellableCoroutine { cont ->
                GlobalScope.launch {
                    when (currentAdsProvider) {
                        Configuration.AdsProvider.ADMOB -> {
                            cont.resume(PHResult.Failure(java.lang.IllegalStateException("This function is used to load AppLovin native apps only. For AdMob use loadAndGetNativeAd()")))
                        }

                        Configuration.AdsProvider.APPLOVIN -> {
                            if (unitId.isEmpty()) {
                                cont.resume(PHResult.Failure(IllegalStateException("No ad unitId defined")))
                            } else {
                                AppLovinNativeProvider(unitId).loadLateBindingAd(
                                    application,
                                    object : com.appboosty.ads.PhAdListener() {
                                        override fun onAdFailedToLoad(error: com.appboosty.ads.PhLoadAdError) {
                                            cont.resume(PHResult.Failure(IllegalStateException(error.message)))
                                        }
                                    },
                                    object : PhMaxNativeAdListener() {
                                        override fun onNativeAdLoaded(
                                            loader: MaxNativeAdLoader,
                                            ad: MaxAd?
                                        ) {
                                            if (cont.isActive) {
                                                ad?.let {
                                                    cont.resume(
                                                        PHResult.Success(
                                                            AppLovinNativeAdWrapper(
                                                                loader,
                                                                ad
                                                            )
                                                        )
                                                    )
                                                } ?: cont.resume(
                                                    PHResult.Failure(IllegalStateException("The ad is empty"))
                                                )
                                            }
                                        }
                                    }, isExitAd
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e, "AdManager: Failed to load native ad")
            PHResult.Failure(e)
        }
    }

    suspend fun loadAndGetNativeAppLovinAd(maxNativeAdView: MaxNativeAdView): PHResult<Unit> {
        return try {
            val unitId = adUnitIdProvider.getAdUnitId(com.appboosty.ads.AdManager.AdType.NATIVE, useTestAds = useTestAds)
            log.d("AdManager: Loading AppLovin native ad: ($unitId)")
            suspendCancellableCoroutine { cont ->
                GlobalScope.launch {
                    when (currentAdsProvider) {
                        Configuration.AdsProvider.ADMOB -> {
                            cont.resume(PHResult.Failure(java.lang.IllegalStateException("For AdMobs ads use loadAndGetNativeAd method")))
                        }

                        Configuration.AdsProvider.APPLOVIN -> {
                            // cont.resume(PHResult.Failure(java.lang.IllegalStateException("AppLovin native provider not yet supported")))
                            val provider = AppLovinNativeProvider(
                                adUnitIdProvider.getAdUnitId(
                                    com.appboosty.ads.AdManager.AdType.NATIVE,
                                    true,
                                    useTestAds = useTestAds
                                )
                            )

                            provider.load(application, maxNativeAdView, object : com.appboosty.ads.PhAdListener() {
                                override fun onAdFailedToLoad(error: com.appboosty.ads.PhLoadAdError) {
                                    log.e("AppLovin exit ad failed to load. Error: ${error.message}")
                                    if (cont.isActive) {
                                        cont.resume(PHResult.Failure(IOException("Failed to load AppLovin NativeAd: Error: ${error.message} Code: ${error.code}")))
                                    }
                                }

                                override fun onAdLoaded() {
                                    log.d("AppLovin exit ad Loaded")
                                    if (cont.isActive) {
                                        cont.resume(PHResult.Success(Unit))
                                    }
                                }
                            })
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e, "AdManager: Failed to load AppLovin native ad")
            PHResult.Failure(e)
        }
    }

    fun isInterstitialLoaded(): Boolean {
        return interstitialManager?.isInterstitialLoaded() ?: false
    }

    suspend fun waitForInterstitial(timeout: Long): Boolean? {
        return interstitialManager?.waitForInterstitial(timeout)
    }

    fun showInterstitialAd(
        activity: Activity,
        callback: com.appboosty.ads.PhFullScreenContentCallback?,
        delayed: Boolean = false
    ) {
        interstitialManager?.showInterstitialAd(
            activity,
            callback,
            delayed,
            application,
            adUnitIdProvider,
            useTestAds
        )
    }

    fun showRewardedAd(
        activity: Activity,
        rewardedAdCallback: com.appboosty.ads.PhOnUserEarnedRewardListener,
        callback: com.appboosty.ads.PhFullScreenContentCallback
    ) {
        rewardedAdManager?.showRewardedAd(
            application,
            adUnitIdProvider,
            useTestAds,
            activity,
            rewardedAdCallback,
            callback
        )
    }

    fun destroy() {
        do {
            val ad = nativeAds.tryReceive().getOrNull()?.let {
                log.d("AdManager: Destroying native ad: ${it.headline}")
                it.destroy()
            }
        } while (ad != null)
    }

    @SuppressLint("ClickableViewAccessibility")
    internal fun onMainActivityBackPressed(activity: Activity): Boolean {
        return exitAds?.let {
            if (it.inExitAdShown() || it.isNotEnabled()) {
                it.onActivityClosed()
                true
            } else {
                it.show(activity, useTestAds)
                false
            }
        } ?: true
    }

    fun showDebugScreen() {
        when (currentAdsProvider) {
            Configuration.AdsProvider.APPLOVIN ->
                AppLovinSdk.getInstance(application).showMediationDebugger()

            else ->
                log.e("Current provider doesn't support debug screen. $currentAdsProvider")
        }
    }

    fun isDebugPanelSupported(): Boolean {
        return com.appboosty.ads.AdManager.Companion.DEBUG_SCREEN_PROVIDERS.contains(currentAdsProvider)
    }

    suspend fun loadAndGetNativeAdCommon(
        binder: PhNativeAdViewBinder,
        callback: PhNativeAdLoadListener?,
        isExitAd: Boolean = false,
        adUnitId: String? = null
    ): PHResult<View> {
        return try {
            val unitId = adUnitId ?: adUnitIdProvider.getAdUnitId(
                com.appboosty.ads.AdManager.AdType.NATIVE,
                isExitAd,
                useTestAds = useTestAds
            )
            log.d("AdManager: Loading native ad: ($unitId, $isExitAd)")
            suspendCancellableCoroutine { cont ->
                GlobalScope.launch {
                    when (currentAdsProvider) {
                        Configuration.AdsProvider.ADMOB -> {
                            val adView = AdMobNativeAdHelper.createAdmobNativeView(binder)
                            if (cont.isActive) {
                                cont.resume(
                                    PHResult.Success(adView)
                                )
                            }
                            val start = System.currentTimeMillis()
                            AdsLoadingPerformance.getInstance().onStartNativeAdLoading()
                            AdMobNativeProvider(unitId)
                                .load(application, 1,
                                    object : com.appboosty.ads.PhAdListener() {
                                        override fun onAdFailedToLoad(error: com.appboosty.ads.PhLoadAdError) {
                                            log.e(error.message)
                                            collapseViewOnError(adView)
                                            callback?.onAdFailedToLoad(error)
                                        }
                                    }, { ad ->
                                        AdsLoadingPerformance.getInstance()
                                            .onEndNativeAdLoading(System.currentTimeMillis() - start)
                                        AdMobNativeAdHelper.populateAdmobNativeView(
                                            binder,
                                            adView,
                                            ad
                                        )
                                        callback?.onAdLoaded(adView)
                                    }, isExitAd
                                )
                        }

                        Configuration.AdsProvider.APPLOVIN -> {

                            val adView = AppLovinNativeAdHelper.createAppLovinNativeAdView(binder)
                            if (cont.isActive) {
                                cont.resume(
                                    PHResult.Success(adView)
                                )
                            }
                            val start = System.currentTimeMillis()
                            AdsLoadingPerformance.getInstance().onStartNativeAdLoading()
                            AppLovinNativeProvider(unitId).loadLateBindingAd(
                                application,
                                object : com.appboosty.ads.PhAdListener() {
                                    override fun onAdFailedToLoad(error: com.appboosty.ads.PhLoadAdError) {
                                        log.e(error.message)
                                        collapseViewOnError(adView)
                                        callback?.onAdFailedToLoad(error)
                                    }
                                },
                                object : PhMaxNativeAdListener() {
                                    override fun onNativeAdLoaded(
                                        loader: MaxNativeAdLoader,
                                        ad: MaxAd?
                                    ) {
                                        ad?.let {
                                            AppLovinNativeAdHelper.populateAdView(
                                                loader,
                                                adView,
                                                it, binder
                                            )
                                            callback?.onAdLoaded(adView)
                                            AdsLoadingPerformance.getInstance()
                                                .onEndNativeAdLoading(System.currentTimeMillis() - start)
                                        } ?: run {
                                            log.e("The native ad is empty !")
                                            collapseViewOnError(adView)
                                            callback?.onAdFailedToLoad(
                                                com.appboosty.ads.PhLoadAdError(
                                                    -1,
                                                    "The native ad is empty !",
                                                    ""
                                                )
                                            )
                                        }
                                    }
                                }, isExitAd
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e, "AdManager: Failed to load native ad")
            PHResult.Failure(e)
        }
    }

    private fun collapseViewOnError(view: View) {
        view.visibility = View.GONE
    }

    private suspend fun waitForInitComplete(): PHResult<Unit> {
        return try {
            coroutineScope {
                Timber.tag("PhConsentManager").d("Start to wait for AdManager initialization")
                val initProcess = async {
                    if (!isInitialized.value) {
                        isInitialized.first { it }
                    }
                    Timber.tag("PhConsentManager").d("AdManager initialization wait complete")
                    true
                }
                awaitAll(initProcess)
                PHResult.Success(Unit)
            }
        } catch (e: java.lang.Exception) {
            Timber.tag(TAG).e("Exception while initializing AdManager")
            PHResult.Failure(e)
        }
    }

    private suspend fun waitForPremiumStatus(): PHResult<Unit> {
        return try {
            coroutineScope {
                Timber.tag("PhConsentManager").d("Start to wait for User premium status")
                val initProcess = async {
                    if (isPremium.value == null) {
                        isPremium.first { it != null }
                    }
                    Timber.tag("PhConsentManager").d("Waiting for premium status complete")
                    true
                }
                awaitAll(initProcess)
                PHResult.Success(Unit)
            }
        } catch (e: java.lang.Exception) {
            Timber.tag(TAG).e("Exception while waiting for premium status")
            PHResult.Failure(e)
        }
    }

    private suspend fun waitForConfiguration(): PHResult<Unit> {
        return try {
            coroutineScope {
                Timber.tag("PhConsentManager").d("Start to wait for configuration")
                val initProcess = async {
                    if (isConfigurationReady.value == null) {
                        isConfigurationReady.first { it != null }
                    }
                    Timber.tag("PhConsentManager").d("Waiting for configuration complete")
                    true
                }
                awaitAll(initProcess)
                PHResult.Success(Unit)
            }
        } catch (e: java.lang.Exception) {
            Timber.tag(TAG).e("Exception while waiting for configuration")
            PHResult.Failure(e)
        }
    }
}


fun InitializationStatus.getStatusAsString(): String {

    return with(StringBuilder()) {

        adapterStatusMap.forEach { status ->
            appendLine("${status.key}:${status.value.initializationState}")
        }

        toString()
    }
}
