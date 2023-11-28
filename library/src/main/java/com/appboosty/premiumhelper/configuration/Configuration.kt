package com.appboosty.premiumhelper.configuration

import android.content.Context
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.configuration.Configuration.ConfigParam.ConfigBooleanParam
import com.appboosty.premiumhelper.configuration.Configuration.ConfigParam.ConfigEnumParam
import com.appboosty.premiumhelper.configuration.Configuration.ConfigParam.ConfigLongParam
import com.appboosty.premiumhelper.configuration.Configuration.ConfigParam.ConfigStringParam
import com.appboosty.premiumhelper.configuration.appconfig.PremiumHelperConfiguration
import com.appboosty.premiumhelper.configuration.overriden.DebugOverrideRepository
import com.appboosty.premiumhelper.configuration.remoteconfig.RemoteConfig
import com.appboosty.premiumhelper.configuration.testy.TestyConfiguration
import com.appboosty.premiumhelper.configuration.toto.TotoConfigRepository
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.toto.WeightedValueParameter
import com.appboosty.premiumhelper.ui.happymoment.HappyMoment
import com.appboosty.premiumhelper.ui.rate.RateHelper
import timber.log.Timber

class Configuration(
    context: Context,
    private val remoteConfig: RemoteConfig,
    internal val appConfig: PremiumHelperConfiguration,
    private val testyConfiguration: TestyConfiguration,
) : ConfigRepository {

    sealed class ConfigParam <T> constructor (val key: String, val default: T) {

        init { DEFAULT_CONFIGURATION_MAP[key] = default.toString().lowercase() }

        class ConfigStringParam(key: String, default: String = "") : ConfigParam<String>(key, default)
        class ConfigLongParam(key: String, default: Long) : ConfigParam<Long>(key, default)
        class ConfigBooleanParam(key: String, default: Boolean) : ConfigParam<Boolean>(key, default)
        class ConfigEnumParam<E : Enum<E>>(key: String, default: E) : ConfigParam<E>(key, default)
    }

    enum class CappingType {
        SESSION, GLOBAL
    }

    enum class AdsProvider {
        ADMOB, APPLOVIN
    }

    companion object Params {

        private val DEFAULT_CONFIGURATION_MAP = HashMap<String, String>()

        @JvmField val MAIN_SKU                     = ConfigStringParam("main_sku")
        @JvmField val ONETIME_OFFER                = ConfigStringParam("onetime_offer_sku")
        @JvmField val ONETIME_OFFER_STRIKETHROUGH  = ConfigStringParam("onetime_offer_strikethrough_sku")
        @JvmField val AD_UNIT_ADMOB_BANNER         = ConfigStringParam("ad_unit_admob_banner")
        @JvmField val AD_UNIT_ADMOB_INTERSTITIAL   = ConfigStringParam("ad_unit_admob_interstitial")
        @JvmField val AD_UNIT_ADMOB_NATIVE         = ConfigStringParam("ad_unit_admob_native")
        @JvmField val AD_UNIT_ADMOB_REWARDED       = ConfigStringParam("ad_unit_admob_rewarded")
        @JvmField val AD_UNIT_ADMOB_BANNER_EXIT    = ConfigStringParam("ad_unit_admob_banner_exit")
        @JvmField val AD_UNIT_ADMOB_NATIVE_EXIT    = ConfigStringParam("ad_unit_admob_native_exit")
        @JvmField val ANALYTICS_PREFIX             = ConfigStringParam("analytics_prefix")
        @JvmField val ONETIME_START_SESSION        = ConfigLongParam("onetime_start_session", 3)
        @JvmField val RATE_US_SESSION_START        = ConfigLongParam("rateus_session_start", 3)
        @JvmField val RATE_US_MODE                 = ConfigEnumParam("rate_us_mode", RateHelper.RateMode.VALIDATE_INTENT)
        @JvmField val HAPPY_MOMENT_RATE_MODE       = ConfigEnumParam("happy_moment", HappyMoment.HappyMomentRateMode.DEFAULT)
        @JvmField val TERMS_URL                    = ConfigStringParam("terms_url")
        @JvmField val PRIVACY_URL                  = ConfigStringParam("privacy_url")
        @JvmField val SHOW_ONBOARDING_INTERSTITIAL = ConfigBooleanParam("show_interstitial_onboarding_basic", true)
        @JvmField val SHOW_RELAUNCH_ON_RESUME      = ConfigBooleanParam("show_relaunch_on_resume", true)
        @JvmField val SHOW_AD_ON_APP_EXIT          = ConfigBooleanParam("show_ad_on_app_exit", false)
        @JvmField val HAPPY_MOMENT_CAPPING_SECONDS = ConfigLongParam("happy_moment_capping_seconds", 0)
        @JvmField val HAPPY_MOMENT_CAPPING_TYPE    = ConfigEnumParam("happy_moment_capping_type",
            CappingType.SESSION
        )
        @JvmField val HAPPY_MOMENT_SKIP_FIRST      = ConfigLongParam("happy_moment_skip_first", 0)
        @JvmField val INTERSTITIAL_CAPPING_SECONDS = ConfigLongParam("interstitial_capping_seconds", 0)
        @JvmField val INTERSTITIAL_CAPPING_TYPE    = ConfigEnumParam("interstitial_capping_type",
            CappingType.SESSION
        )
        @JvmField val SHOW_TRIAL_ON_CTA            = ConfigBooleanParam("show_trial_on_cta", false)
        @JvmField val TOTO_ENABLED                 = ConfigBooleanParam("toto_enabled", true)
        @JvmField val TOTO_CONFIG_CAPPING_HOURS    = ConfigLongParam("toto_capping_hours", 24L)
        @JvmField val INTERSTITIAL_MUTED           = ConfigBooleanParam("interstitial_muted", false)
        @JvmField val PREMIUM_PACKAGES             = ConfigStringParam("premium_packages")
        @JvmField val DISABLE_RELAUNCH_OFFERING    = ConfigBooleanParam("disable_relaunch_premium_offering", false)
        @JvmField val DISABLE_ONBOARDING_OFFERING  = ConfigBooleanParam("disable_onboarding_premium_offering", false)
        @JvmField val ONBOARDING_LAYOUT_VARIANT    = ConfigLongParam("onboarding_layout_variant", 0)
        @JvmField val RELAUNCH_LAYOUT_VARIANT      = ConfigLongParam("relaunch_layout_variant", 0)
        @JvmField val RELAUNCH_ONETIME_LAYOUT_VARIANT = ConfigLongParam("relaunch_onetime_layout_variant", 0)
        @JvmField val SHOW_CONTACT_SUPPORT_DIALOG  = ConfigBooleanParam("show_contact_support_dialog", true)
        @JvmField val PREVENT_AD_FRAUD             = ConfigBooleanParam("prevent_ad_fraud", false)
        @JvmField val MAX_UPDATE_REQUESTS          = ConfigLongParam("max_update_requests", 2)
        @JvmField val IN_APP_UPDATES_ENABLED       = ConfigBooleanParam("in_app_updates_enabled", false)

        @JvmField val ADS_PROVIDER                          = ConfigEnumParam("ads_provider",
            AdsProvider.ADMOB
        )
        @JvmField val AD_UNIT_APPLOVIN_BANNER               = ConfigStringParam("ad_unit_applovin_banner")
        @JvmField val AD_UNIT_APPLOVIN_MREC_BANNER          = ConfigStringParam("ad_unit_applovin_mrec_banner")
        @JvmField val AD_UNIT_APPLOVIN_INTERSTITIAL         = ConfigStringParam("ad_unit_applovin_interstitial")
        @JvmField val AD_UNIT_APPLOVIN_NATIVE               = ConfigStringParam("ad_unit_applovin_native")
        @JvmField val AD_UNIT_APPLOVIN_REWARDED             = ConfigStringParam("ad_unit_applovin_rewarded")
        @JvmField val AD_UNIT_APPLOVIN_BANNER_EXIT          = ConfigStringParam("ad_unit_applovin_banner_exit")
        @JvmField val AD_UNIT_APPLOVIN_NATIVE_EXIT          = ConfigStringParam("ad_unit_applovin_native_exit")

        @JvmField val TOTOLYTICS_ENABLED                    = ConfigBooleanParam("totolytics_enabled", false)
        @JvmField val SESSION_TIMEOUT_SECONDS               = ConfigLongParam("session_timeout_seconds", 30)
        @JvmField val AD_FRAUD_PROTECTION_TIMEOUT_SECONDS   = ConfigLongParam("prevent_ad_fraud_timeout_seconds", 10)
        @JvmField val SEND_PERFORMANCE_EVENTS               = ConfigBooleanParam("send_performance_events", true)
        @JvmField val FLURRY_API_KEY                        = ConfigStringParam("flurry_api_key", "")
        @JvmField val CONSENT_REQUEST_ENABLED               = ConfigBooleanParam("consent_request_enabled", true)
    }

    private val log by timber(PremiumHelper.TAG)

    private val overridden = DebugOverrideRepository()
    private val totoConfigCache = TotoConfigRepository(context)
    private val appConfigRepository = appConfig.repository()
    private val defaultRepository = DefaultValueRepository()

    override fun name(): String = "Premium Helper"

    internal fun isDebugMode(): Boolean = appConfig.isDebugMode

    fun overrideDebugValue(key: String, value: Any) {
        overridden.put(key, value.toString())
    }

    fun <T> overrideDebugValue(param: ConfigParam<T>, value: T) {
        overridden.put(param.key, value.toString())
    }

    /**
     *  Save configuration parameters received from Toto service.
     *
     *  @param config List of received parameters
     *  @return true if configuration was changed
     *
     *          false no updates were made
     */
    fun updateConfiguration(config: List<WeightedValueParameter>, country: String): Boolean {
        return totoConfigCache.update(config, country)
    }

    override fun contains(key: String): Boolean {
        return getConfigRepository(key) !is DefaultValueRepository
    }

    fun <T> get(param: ConfigParam<T>): T {
        return getValue(param.key, param.default)
    }

    fun <T: Enum<T>> get(param: ConfigEnumParam<T>): T {

        val value = get(param.key, param.default.name)

        return try {
            java.lang.Enum.valueOf(param.default.javaClass, value.uppercase())
        } catch (e: IllegalArgumentException) {
            Timber.e("Invalid remote value for for '${param.javaClass.simpleName}': $value")
            param.default
        }
    }

    override fun <T> ConfigRepository.getValue(key: String, default: T): T {
        val repository = getConfigRepository(key)
        val value = repository.getValue(key, default) ?: default
        log.d("[PH CONFIGURATION] $key = $value from [${repository.name()}]")
        return value
    }

    override fun asMap(): Map<String, String> {
        return DEFAULT_CONFIGURATION_MAP
    }

    private fun getConfigRepository(key: String): ConfigRepository {

        val isRemote = isRemoteValueSupported(key)

        return when {
            isDebugMode() && overridden.contains(key) -> overridden
            testyConfiguration.contains(key) -> testyConfiguration
            isRemote && isTotoEnabled() && totoConfigCache.contains(key) -> totoConfigCache
            isRemote && remoteConfig.contains(key) -> remoteConfig
            appConfigRepository.contains(key) -> appConfigRepository
            else -> defaultRepository
        }
    }

    private fun isRemoteValueSupported(key: String): Boolean {
        return when(key) {
            TOTO_ENABLED.key,
            ANALYTICS_PREFIX.key -> false
            else -> true
        }
    }

    /**
     *  Return value if TOTO_ENABLED parameter,
     *  skipping the toto cache and remote config lookup
     */
    internal fun isTotoEnabled(): Boolean {
        val repository = when {
            isDebugMode() && overridden.contains(TOTO_ENABLED.key) -> overridden
            appConfigRepository.contains(TOTO_ENABLED.key) -> appConfigRepository
            else -> defaultRepository
        }

        return repository.get(TOTO_ENABLED.key, TOTO_ENABLED.default)
    }

    internal suspend fun allValuesToString(): String {
        return buildString {
            appendLine(overridden.toString())
            appendLine()
            appendLine("Preferences")
            appendLine(totoConfigCache.allPreferencesToString())
            appendLine("Remote Config")
            appendLine(remoteConfig.allValuesToString())
            appendLine("Testy")
            appendLine(testyConfiguration.toString())
            appendLine("App Config")
            appendLine(appConfig.toString())
        }
    }

    private class DefaultValueRepository : ConfigRepository {
        override fun name(): String = "DEFAULT"
        override fun contains(key: String): Boolean = true
        override fun <T> ConfigRepository.getValue(key: String, default: T): T = default
        override fun asMap(): Map<String, String> = DEFAULT_CONFIGURATION_MAP
    }

    fun getTotoConfigurationCountry(): String {
        return totoConfigCache.getConfigCountry()
    }

    fun getConfigurationMap(): Map<String, String> {

        val configurationMap = HashMap<String, String>()

        configurationMap.putAll(defaultRepository.asMap())
        configurationMap.putAll(appConfigRepository.asMap())
        configurationMap.putAll(remoteConfig.asMap())
        configurationMap.putAll(totoConfigCache.asMap())

        return configurationMap
    }

    private fun IntArray.get(param: ConfigLongParam) : Int {
        val variant = this@Configuration.get(param).toInt()
        return if (variant < size) {
            get(variant)
        } else {
            get(0)
        }
    }

    fun getStartLikeProLayout(): Int {
        return when {
            appConfig.startLikeProActivityLayout.isNotEmpty() -> appConfig.startLikeProActivityLayout.get(
                ONBOARDING_LAYOUT_VARIANT
            )
            isDebugMode() && appConfig.useTestLayouts -> R.layout.ph_sample_activity_start_like_pro
            else -> error("Start Like Pro layout id is not set in premium-helper configuration!")
        }
    }

    fun getRelaunchLayout(): Int {
        return when {
            appConfig.relaunchPremiumActivityLayout.isNotEmpty() -> appConfig.relaunchPremiumActivityLayout.get(
                RELAUNCH_LAYOUT_VARIANT
            )
            isDebugMode() && appConfig.useTestLayouts -> R.layout.ph_sample_activity_relaunch
            else -> error("Relaunch layout id is not set in premium-helper configuration!")
        }
    }

    fun getRelaunchOneTimeLayout(): Int {
        return when {
            appConfig.relaunchOneTimeActivityLayout.isNotEmpty() -> appConfig.relaunchOneTimeActivityLayout.get(
                RELAUNCH_ONETIME_LAYOUT_VARIANT
            )
            isDebugMode() && appConfig.useTestLayouts -> R.layout.ph_sample_activity_relaunch_one_time
            else -> error("One-time Relaunch layout id is not set in premium-helper configuration!")
        }
    }

}