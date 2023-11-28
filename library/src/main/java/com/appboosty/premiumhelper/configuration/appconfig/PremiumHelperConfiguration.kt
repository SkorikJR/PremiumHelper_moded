package com.appboosty.premiumhelper.configuration.appconfig

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.appboosty.ads.config.AdManagerConfiguration
import com.appboosty.premiumhelper.configuration.ConfigRepository
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.ui.rate.RateHelper
import com.appboosty.premiumhelper.util.PHMessagingService
import org.json.JSONObject

@Suppress("ArrayInDataClass")
data class PremiumHelperConfiguration (

    /** Must be defined in app **/
    val mainActivityClass: Class<out Activity>,
    val introActivityClass: Class<out Activity>?,
    val pushMessageListener: PHMessagingService.PushMessageListener?,
    val rateDialogLayout: Int,
    val startLikeProActivityLayout: IntArray,
    val startLikeProTextNoTrial: Int?,
    val startLikeProTextTrial: Int?,
    val relaunchPremiumActivityLayout: IntArray,
    val relaunchOneTimeActivityLayout: IntArray,
    val isDebugMode: Boolean,
    val adManagerTestAds: Boolean,
    val useTestLayouts: Boolean,
    val debugData: Bundle?,
    val configMap: Map<String, String> = HashMap()) {

    fun repository(): ConfigRepository {
        return object : ConfigRepository {

            override fun name(): String = "App Default"

            override fun contains(key: String): Boolean {
                return configMap.containsKey(key)
            }

            @Suppress("unchecked_cast")
            override fun <T> ConfigRepository.getValue(key: String, default: T): T {
                return (when (default) {
                    is String -> configMap[key]
                    is Boolean -> configMap[key]?.toBooleanStrictOrNull()
                    is Long -> configMap[key]?.toLongOrNull()
                    is Double -> configMap[key]?.toDoubleOrNull()
                    else -> error("Unsupported type")
                }  ?: default) as T
            }

            override fun asMap(): Map<String, String> {
                return configMap
            }
        }
    }

    override fun toString(): String {
        return buildString {
            appendLine("MainActivity : ${mainActivityClass.name}")
            appendLine("PushMessageListener : ${pushMessageListener?.javaClass?.name ?: "not set"}")
            appendLine("rateDialogLayout : $rateDialogLayout")
            appendLine("startLikeProActivityLayout : $startLikeProActivityLayout")
            appendLine("startLikeProTextNoTrial : $startLikeProTextNoTrial")
            appendLine("startLikeProTextTrial : $startLikeProTextTrial")
            appendLine("relaunchPremiumActivityLayout : $relaunchPremiumActivityLayout")
            appendLine("relaunchOneTimeActivityLayout : $relaunchOneTimeActivityLayout")
            appendLine("isDebugMode : $isDebugMode")
            appendLine("adManagerTestAds : $adManagerTestAds")
            appendLine("useTestLayouts : $useTestLayouts")
            appendLine("configMap : ")
            appendLine(JSONObject(Gson().toJson(configMap)).toString(4))
        }
    }

    data class Builder(
        val isDebugMode: Boolean,
        private val configMap: HashMap<String, String> = HashMap(),
        private var rateDialogLayout: Int = 0,
        private var startLikeProActivityLayout: IntArray = IntArray(0),
        private var startLikeProTextNoTrial: Int? = null,
        private var startLikeProTextTrial: Int? = null,
        private var relaunchPremiumActivityLayout: IntArray = IntArray(0),
        private var relaunchOneTimeActivityLayout: IntArray = IntArray(0),
        private var mainActivityClass: Class<out Activity>? = null,
        private var introActivityClass: Class<out Activity>? = null,
        private var pushMessageListener: PHMessagingService.PushMessageListener? = null,
        private var adManagerTestAds: Boolean = false,
        private var useTestLayouts: Boolean = true,
        private var debugData: Bundle = Bundle()
        ) {

        constructor(isDebugMode: Boolean) : this(isDebugMode, HashMap(), 0, intArrayOf(0), null, null, intArrayOf(0), intArrayOf(0), null, null)

        /**
         *  Configure custom layout for [rate dialog][com.appboosty.premiumhelper.ui.rate.RateDialog]
         *
         * @param rateDialogLayout dialog layout Id
         *
         * @see <a href="https://github.com/AppBoosty/premium-helper/tree/develop#7-ratedialog">https://github.com/AppBoosty/premium-helper/tree/develop#7-ratedialog</a>
         */
        fun rateDialogLayout(@LayoutRes rateDialogLayout: Int) = apply { this.rateDialogLayout = rateDialogLayout }

        /**
         *  RateUs start from session
         */
        fun rateSessionStart(session: Int) = apply {
            this.configMap[Configuration.RATE_US_SESSION_START.key] = session.toString()
        }

        /**
         * Configure layout for [StartLikeProActivity][com.appboosty.premiumhelper.ui.startlikepro.StartLikeProActivity]
         * Pass multiple layout variants for A/B testing with ONBOARDING_LAYOUT_VARIANT parameter
         *
         * @param startLikeProActivityLayout activity layout Id
         *
         * @see <a href="https://github.com/AppBoosty/premium-helper/tree/develop#4-startlikepro-activity">https://github.com/AppBoosty/premium-helper/tree/develop#4-startlikepro-activity</a>
         */
        fun startLikeProActivityLayout(@LayoutRes vararg startLikeProActivityLayout: Int) = apply { this.startLikeProActivityLayout = startLikeProActivityLayout }

        /**
         *  The startLikeProTextTrial and startLikeProTextNoTrial ids are used to override default strings for purchase button title.
         *  If offer sku has a trial period the startLikeProTextTrial text is displayed on the purchase button.
         *  If offer sku has a no trial period the startLikeProTextNoTrial text is displayed on the purchase button.
         */
        fun startLikeProTextNoTrial(@StringRes startLikeProTextNoTrial:Int) = apply { this.startLikeProTextNoTrial = startLikeProTextNoTrial }

        /**
         *  The startLikeProTextTrial and startLikeProTextNoTrial ids are used to override default strings for purchase button title.
         *  If offer sku has a trial period the startLikeProTextTrial text is displayed on the purchase button.
         *  If offer sku has a no trial period the startLikeProTextNoTrial text is displayed on the purchase button.
         */
        fun startLikeProTextTrial(@StringRes startLikeProTextTrial: Int) = apply { this.startLikeProTextTrial = startLikeProTextTrial }

        /**
         * Configure layout for RelaunchActivity (Main offer).
         * Pass multiple layout variants for A/B testing with RELAUNCH_LAYOUT_VARIANT parameter
         *
         * relaunchPremiumActivityLayout - activity layout Id
         */
        fun relaunchPremiumActivityLayout(@LayoutRes vararg relaunchPremiumActivityLayout: Int) = apply { this.relaunchPremiumActivityLayout = relaunchPremiumActivityLayout }

        /**
         * Configure layout for RelaunchActivity (One-time offer).
         * Pass multiple layout variants for A/B testing with RELAUNCH_ONETIME_LAYOUT_VARIANT parameter
         *
         * relaunchOneTimeActivityLayout - activity layout Id
         */
        fun relaunchOneTimeActivityLayout(@LayoutRes vararg relaunchOneTimeActivityLayout: Int) = apply { this.relaunchOneTimeActivityLayout = relaunchOneTimeActivityLayout }

        /**
         *  Set class name of main application activity that will be started after the splash screen
         */
        fun mainActivityClass(mainActivityClass: Class<out Activity>) = apply { this.mainActivityClass = mainActivityClass }

        /**
         *  Set class name of intro activity that will be started after the splash screen
         */
        fun introActivityClass(introActivityClass: Class<out Activity>) = apply { this.introActivityClass = introActivityClass }

        /**
         *  Listener for receiving push messages.
         *  Optional.
         */
        fun pushMessageListener(pushMessageListener: PHMessagingService.PushMessageListener) = apply { this.pushMessageListener = pushMessageListener }

        /**
         *  Prefix for analytics events.
         *  Optional.
         */
        fun analyticsEventPrefix(analyticsEventPrefix: String) = apply { this.configMap[Configuration.ANALYTICS_PREFIX.key] = analyticsEventPrefix }

        /**
         *  Set default mode for showing rate dialog.
         *  Optional.
         * @see <a href="https://github.com/AppBoosty/premium-helper/tree/develop#7-ratedialog">https://github.com/AppBoosty/premium-helper/tree/develop#7-ratedialog</a>
         */
        fun rateDialogMode(rateDialogMode: RateHelper.RateMode) = apply { this.configMap[Configuration.RATE_US_MODE.key] = rateDialogMode.name }

        /**
         *  Configure remote name for other main offer SKU
         */
        fun configureMainOffer(defaultSku: String) = apply {
            this.configMap[Configuration.MAIN_SKU.key] = defaultSku
        }

        fun setFlurryApiKey(flurryApiKey: String) = apply {
            this.configMap[Configuration.FLURRY_API_KEY.key] = flurryApiKey
        }

        /**
         * Enable/Disable in app update feature
         */
        fun enableInAppUpdates(enabled: Boolean) = apply {
            set(Configuration.IN_APP_UPDATES_ENABLED, enabled)
        }

        /**
         *  Configure remote names for other One-time offer SKUs
         */
        fun configureOneTimeOffer(defaultOneTimeSku: String, defaultOneTimeStrikethroughSku: String) = apply {
            this.configMap[Configuration.ONETIME_OFFER.key] = defaultOneTimeSku
            this.configMap[Configuration.ONETIME_OFFER_STRIKETHROUGH.key] = defaultOneTimeStrikethroughSku
        }

        /**
         *  Ad manager configuration file
         *  Optional.
         */
        fun adManagerConfiguration(
            admobConfiguration: AdManagerConfiguration,
            appLovinConfiguration: AdManagerConfiguration? = null
        ) = apply {
            admobConfiguration(admobConfiguration)
            appLovinConfiguration?.let { applovinConfiguration(appLovinConfiguration) }
        }

        fun adManagerConfiguration(
            admobConfiguration: AdManagerConfiguration
        ) = apply {
            adManagerConfiguration(admobConfiguration, null)
        }

        fun admobConfiguration(configuration: AdManagerConfiguration) = apply {
            this.configMap[Configuration.AD_UNIT_ADMOB_BANNER.key] = configuration.banner ?: ""
            this.configMap[Configuration.AD_UNIT_ADMOB_INTERSTITIAL.key] = configuration.interstitial
            this.configMap[Configuration.AD_UNIT_ADMOB_NATIVE.key] = configuration.native ?: ""
            this.configMap[Configuration.AD_UNIT_ADMOB_REWARDED.key] = configuration.rewarded ?: ""
            this.configMap[Configuration.AD_UNIT_ADMOB_BANNER_EXIT.key] = configuration.exit_banner ?: ""
            this.configMap[Configuration.AD_UNIT_ADMOB_NATIVE_EXIT.key] = configuration.exit_native ?: ""
            configuration.testAdvertisingIds?.let {
                this.debugData.putStringArray("test_advertising_ids", it.toTypedArray())
            }
        }

        fun applovinConfiguration(configuration: AdManagerConfiguration) = apply {
            this.configMap[Configuration.AD_UNIT_APPLOVIN_BANNER.key] = configuration.banner ?: ""
            this.configMap[Configuration.AD_UNIT_APPLOVIN_MREC_BANNER.key] = configuration.bannerMRec ?: ""
            this.configMap[Configuration.AD_UNIT_APPLOVIN_INTERSTITIAL.key] = configuration.interstitial
            this.configMap[Configuration.AD_UNIT_APPLOVIN_NATIVE.key] = configuration.native ?: ""
            this.configMap[Configuration.AD_UNIT_APPLOVIN_REWARDED.key] = configuration.rewarded ?: ""
            this.configMap[Configuration.AD_UNIT_APPLOVIN_BANNER_EXIT.key] = configuration.exit_banner ?: ""
            this.configMap[Configuration.AD_UNIT_APPLOVIN_NATIVE_EXIT.key] = configuration.exit_native ?: ""
            configuration.testAdvertisingIds?.let {
                this.debugData.putStringArray("test_advertising_ids", it.toTypedArray())
            }
        }

        /**
         * Use to show ads on app exit (exit confirmation dialog)
         * Optional.
         */
        fun showExitConfirmationAds(isEnabled: Boolean) = apply { this.configMap[Configuration.SHOW_AD_ON_APP_EXIT.key] = isEnabled.toString() }

        /**
         *  Use test ads
         *  For debug only.
         */
        fun useTestAds(adManagerTestAds: Boolean) = apply { this.adManagerTestAds = adManagerTestAds }

        fun useTestLayouts(useTestLayouts: Boolean) = apply { this.useTestLayouts = useTestLayouts }

        fun showOnboardingInterstitial(showOnboardingInterstitial: Boolean) = apply { this.configMap[Configuration.SHOW_ONBOARDING_INTERSTITIAL.key] = showOnboardingInterstitial.toString() }
        /**
         *  Configure Terms And Conditions URL
         */
        fun termsAndConditionsUrl(url: String) = apply {
            this.configMap[Configuration.TERMS_URL.key] = url
        }

        /**
         *  Configure Privacy Policy URL
         */
        fun privacyPolicyUrl(url: String) = apply {
            this.configMap[Configuration.PRIVACY_URL.key] = url
        }

        /**
         *  Configure Happy Moment Capping
         *
         *  @param seconds capping in seconds
         *  @param type capping type global / session
         */
        @JvmOverloads
        fun setHappyMomentCapping(seconds: Long, type: Configuration.CappingType = Configuration.CappingType.SESSION) = apply {
            set(Configuration.HAPPY_MOMENT_CAPPING_SECONDS, seconds)
            set(Configuration.HAPPY_MOMENT_CAPPING_TYPE, type)
        }

        /**
         *  Configure Happy Moment Skip First
         *
         *  @param skip number of calls to skip
         */
        fun setHappyMomentSkipFirst(skip: Int) = apply {
            set(Configuration.HAPPY_MOMENT_SKIP_FIRST, skip.toLong())
        }

        /**
         *  Configure Interstitial Capping
         *
         *  @param seconds capping in seconds
         *  @param type capping type global / session
         */
        @JvmOverloads
        fun setInterstitialCapping(seconds: Long, type: Configuration.CappingType = Configuration.CappingType.SESSION) = apply {
            set(Configuration.INTERSTITIAL_CAPPING_SECONDS, seconds)
            set(Configuration.INTERSTITIAL_CAPPING_TYPE, type)
        }

        /**
         *  Show trial period on CTA button in purchase screens
         */
        fun showTrialOnCta(show: Boolean) = apply {
            this.configMap[Configuration.SHOW_TRIAL_ON_CTA.key] = show.toString()
        }

        /**
         *  Enable Toto Init call for fetching configuration
         */
        fun setTotoInitEnabled(enabled: Boolean) = apply {
            this.configMap[Configuration.TOTO_ENABLED.key] = enabled.toString()
        }

        /**
         *  Enable premium if any of these apps installed
         *
         *  @param name name(s) of premium packages
         */
        fun setPremiumPackages(vararg name: String) = apply {
            set(Configuration.PREMIUM_PACKAGES, TextUtils.join(",", name))
        }

        /**
         *  Enable premium if any of these apps installed
         *
         *  @param packages names of premium packages
         */
        fun setPremiumPackages(packages: List<String>) = apply {
            set(Configuration.PREMIUM_PACKAGES, TextUtils.join(",", packages))
        }

        /**
         *  Enable Interstitial Ad Fraud protection
         *  Interstitial Ad will be shown if it is already loaded and available.
         */
        fun preventAdFraud(enabled: Boolean) = apply {
            set(Configuration.PREVENT_AD_FRAUD, enabled)
        }

        fun setAdsProvider(adsProvider: Configuration.AdsProvider) = apply {
            set(Configuration.ADS_PROVIDER, adsProvider)
        }

        fun setInterstitialMuted(muted: Boolean) = apply {
            set(Configuration.INTERSTITIAL_MUTED, muted)
        }

        fun <T> set(param: Configuration.ConfigParam<T>, value: T) = apply {
            this.configMap[param.key] = value.toString()
        }

        fun build(): PremiumHelperConfiguration {

            if (mainActivityClass == null) {
                throw IllegalArgumentException("PremiumHelper: Please configure mainActivityClass.")
            }

            if (!useTestLayouts && startLikeProActivityLayout.isEmpty()) {
                throw IllegalArgumentException("PremiumHelper: Please configure layout for StartLikePro activity.")
            }

            if (!useTestLayouts && relaunchPremiumActivityLayout.isEmpty()) {
                throw IllegalArgumentException("PremiumHelper: Please configure layout for RelaunchPremium activity.")
            }

            if (!useTestLayouts && relaunchOneTimeActivityLayout.isEmpty()) {
                throw IllegalArgumentException("PremiumHelper: Please configure layout for RelaunchOneTime activity.")
            }

            if (configMap[Configuration.MAIN_SKU.key].isNullOrEmpty()) {
                throw IllegalArgumentException("PremiumHelper: Please configure default name for main offer SKU.")
            }

            if (configMap[Configuration.ONETIME_OFFER.key]?.isEmpty() == true || configMap[Configuration.ONETIME_OFFER_STRIKETHROUGH.key]?.isEmpty() == true) {
                throw IllegalArgumentException("PremiumHelper: ONE_TIME and ONETIME_OFFER_STRIKETHROUGH cannot be empty")
            }

            if (configMap[Configuration.ONETIME_OFFER.key]?.isNotEmpty() == true && configMap[Configuration.ONETIME_OFFER_STRIKETHROUGH.key].isNullOrEmpty()) {
                throw IllegalArgumentException("PremiumHelper: You must configure both ONE_TIME and ONETIME_OFFER_STRIKETHROUGH sku to show one-time relaunch view.")
            }

            if (!useTestLayouts && configMap[Configuration.ONETIME_OFFER.key] != null && relaunchOneTimeActivityLayout.isEmpty()) {
                throw IllegalArgumentException("PremiumHelper: You must configure relaunchOneTimeActivityLayout to show one-time relaunch view.")
            }

            if (configMap[Configuration.AD_UNIT_ADMOB_BANNER.key].isNullOrEmpty() && configMap[Configuration.AD_UNIT_ADMOB_INTERSTITIAL.key].isNullOrEmpty()) {
                throw IllegalArgumentException("Please provide ads configuration.")
            }

            if (configMap[Configuration.TERMS_URL.key].isNullOrEmpty()) {
                throw IllegalArgumentException("PremiumHelper: You must configure Terms and Conditions url")
            }

            if (configMap[Configuration.PRIVACY_URL.key].isNullOrEmpty()) {
                throw IllegalArgumentException("PremiumHelper: You must configure Privacy url")
            }

            if (configMap[Configuration.ADS_PROVIDER.key] == Configuration.AdsProvider.APPLOVIN.name &&
                configMap[Configuration.AD_UNIT_APPLOVIN_MREC_BANNER.key].isNullOrEmpty()
            ) {
                throw IllegalArgumentException("PremiumHelper: AppLovin MREC unit ID is not defined")
            }

            return PremiumHelperConfiguration(
                   mainActivityClass!!,
                   introActivityClass,
                   pushMessageListener,
                   rateDialogLayout,
                   startLikeProActivityLayout,
                   startLikeProTextNoTrial,
                   startLikeProTextTrial,
                   relaunchPremiumActivityLayout,
                   relaunchOneTimeActivityLayout,
                   isDebugMode,
                   adManagerTestAds,
                   useTestLayouts,
                   debugData,
                   configMap
            )
        }
    }

}