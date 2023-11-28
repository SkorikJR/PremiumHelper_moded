package com.appboosty.premiumhelper

import android.content.Context
import com.google.gson.Gson
import com.appboosty.premiumhelper.configuration.ConfigRepository
import com.appboosty.premiumhelper.util.ActivePurchaseInfo
import kotlinx.coroutines.coroutineScope

class Preferences(context: Context) : ConfigRepository {

    private val sharedPreferences = context.getSharedPreferences("premium_helper_data", Context.MODE_PRIVATE)

    override fun name(): String = "Premium Helper Preferences"

    @Suppress("unchecked_cast")
    override fun <T> ConfigRepository.getValue(key: String, default: T): T {
        return (when (default) {
                is String -> sharedPreferences.getString(key, default)
                is Boolean -> sharedPreferences.getBoolean(key, default)
                is Long -> sharedPreferences.getLong(key, default)
                is Double -> getDouble(key, default)
                else -> error("Unsupported type")
            } ?: default) as T
    }

    @Suppress("unchecked_cast")
    fun <T> set(key: String, default: T) {
        with (sharedPreferences.edit()) {
            when (default) {
                is String -> putString(key, default)
                is Boolean -> putBoolean(key, default)
                is Int -> putLong(key, default.toLong())
                is Long -> putLong(key, default)
                is Double -> putFloat(key, default.toFloat())
                else -> error("Unsupported type")
            }
            apply()
        }
    }

    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    fun getLong(key: String, default: Long): Long {
        return sharedPreferences.getLong(key, default)
    }

    fun getInt(key: String, default: Int): Int {
        return sharedPreferences.getInt(key, default)
    }

    private fun getDouble(key: String, default: Double): Double {
        return if (sharedPreferences.contains(key)) {
            sharedPreferences.getFloat(key, 0f).toDouble()
        } else {
            default
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    fun putString(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    fun putLong(key: String, value: Long) {
        with(sharedPreferences.edit()) {
            putLong(key, value)
            apply()
        }
    }

    fun putInt(key: String, value: Int) {
        with(sharedPreferences.edit()) {
            putInt(key, value)
            apply()
        }
    }

    fun hasActivePurchase(): Boolean {
        return sharedPreferences.getBoolean("has_active_purchase", false)
    }

    fun setHasActivePurchases(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("has_active_purchase", value)
            apply()
        }
    }

    fun getAppStartCounter(): Int {
        return sharedPreferences.getInt("app_start_counter", 0)
    }

    fun incrementAppStartCounter(): Int {
        val counter = sharedPreferences.getInt("app_start_counter", 0)
        with(sharedPreferences.edit()) {
            putInt("app_start_counter", counter + 1)
            apply()
        }
        return counter + 1
    }

    fun isFirstAppStart(): Boolean {
        return sharedPreferences.getInt("app_start_counter", 0) == 0
    }

    fun isOnboardingComplete(): Boolean {
        return sharedPreferences.getBoolean("is_onboarding_complete", false)
    }

    fun setOnboardingComplete() {
        with(sharedPreferences.edit()) {
            putBoolean("is_onboarding_complete", true)
            apply()
        }
    }

    fun incrementRelaunchPremiumOfferingCounter(): Int {
        val counter = sharedPreferences.getInt("relaunch_premium_counter", 0) + 1
        with(sharedPreferences.edit()) {
            putInt("relaunch_premium_counter", counter)
            apply()
        }
        return counter
    }

    fun getRelaunchPremiumOfferingCounter(): Int {
        return sharedPreferences.getInt("relaunch_premium_counter", 0)
    }

    fun setRelaunchPremiumOfferingCounter(value: Int) {
        with(sharedPreferences.edit()) {
            putInt("relaunch_premium_counter", value)
            apply()
        }
    }

    fun hasHistoryPurchases(): Boolean {
        return sharedPreferences.getBoolean("has_history_purchases", false)
    }

    fun setHasHistoryPurchases(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("has_history_purchases", value)
            apply()
        }
    }

    fun getAppInstanceId(): String? {
        return sharedPreferences.getString("app_instance_id", null)
    }

    fun setAppInstanceId(value: String) {
        with(sharedPreferences.edit()) {
            putString("app_instance_id", value)
            apply()
        }
    }

    fun getInstallReferrer(): String? {
        return sharedPreferences.getString("install_referrer", null)
    }

    fun setInstallReferrer(value: String) {
        with(sharedPreferences.edit()) {
            putString("install_referrer", value)
            apply()
        }
    }

    fun setActivePurchaseInfo(value: ActivePurchaseInfo) {
        with(sharedPreferences.edit()) {
            putString("active_purchase_info", Gson().toJson(value))
            apply()
        }
    }

    fun getActivePurchaseInfo(): ActivePurchaseInfo? {

        val info = sharedPreferences.getString("active_purchase_info", "")

        if (info.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(info, ActivePurchaseInfo::class.java)
    }

    fun clearActivePurchaseInfo() {
        with(sharedPreferences.edit()) {
            putString("active_purchase_info", "")
            apply()
        }
    }

    fun isFcmRegistered(): Boolean {
        return sharedPreferences.getBoolean("is_fcm_registered", false)
    }

    fun setFcmRegistered(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("is_fcm_registered", value)
            apply()
        }
    }
    fun getOneTimeOfferStartTime(): Long {
        return sharedPreferences.getLong("one_time_offer_start_time", 0L)
    }

    fun setOneTimeOfferStartTime(value: Long) {
        with(sharedPreferences.edit()) {
            putLong("one_time_offer_start_time", value)
            apply()
        }
    }

    fun isFacebookInstallHandled(): Boolean {
        return sharedPreferences.getBoolean("is_facebook_install_handled", false)
    }

    fun setFacebookInstallHandled(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("is_facebook_install_handled", value)
            apply()
        }
    }

    fun getRateSessionNumber(): Int {
        return sharedPreferences.getInt("rate_session_number", 0)
    }

    fun setRateSessionNumber(number: Int) {
        with(sharedPreferences.edit()) {
            putInt("rate_session_number", number)
            apply()
        }
    }

    fun isNextAppStartIgnored(): Boolean {
        return sharedPreferences.getBoolean("is_next_app_start_ignored", false)
    }

    fun setNextAppStartIgnored(value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("is_next_app_start_ignored", value)
            apply()
        }
    }

    suspend fun allPreferencesToString(): String {

        return coroutineScope {
            val result = StringBuilder()

            sharedPreferences.all.entries.forEach { entry ->
                result.appendLine("${entry.key} : ${entry.value}")
            }

            result.toString()
        }

    }

    override fun asMap(): Map<String, String> {
        val map = HashMap<String, String> ()
        sharedPreferences.all.entries.forEach { entry ->
            map[entry.key] = entry.value.toString().lowercase()
        }
        return map
    }

}