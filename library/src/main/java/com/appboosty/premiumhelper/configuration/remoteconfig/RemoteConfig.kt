package com.appboosty.premiumhelper.configuration.remoteconfig

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.appboosty.premiumhelper.performance.StartupPerformanceTracker
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.configuration.ConfigRepository
import com.appboosty.premiumhelper.log.timber
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Suppress("RedundantVisibilityModifier", "MemberVisibilityCanBePrivate", "unused")
class RemoteConfig : ConfigRepository {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    private val log by timber(TAG)
    private var isDebugMode = false
    private var isInitialized = false

    public suspend fun init(context: Context, isDebugMode: Boolean = false): Boolean {
        this.isDebugMode = isDebugMode
        this.firebaseRemoteConfig = getFirebaseRemoteConfig(context)
        StartupPerformanceTracker.getInstance().onRemoteConfigInitializationStart()
        return suspendCancellableCoroutine { cont ->
            try {

                val settings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (isDebugMode) 0 else 12 * 3600L)
                    .build()

                val startTs = System.currentTimeMillis()

                firebaseRemoteConfig.setConfigSettingsAsync(settings)
                    .continueWithTask {
                        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { fetch ->
                            log.i("RemoteConfig: Fetch success: ${fetch.isSuccessful}")
                            StartupPerformanceTracker.getInstance().setRemoteConfigResult(
                                if (fetch.isSuccessful) "success" else fetch.exception?.message ?: "Fail"
                            )
                            PremiumHelper.getInstance().analytics.onGetRemoteConfig(fetch.isSuccessful, System.currentTimeMillis() - startTs)

                            if (isDebugMode && fetch.isSuccessful) {
                                firebaseRemoteConfig.all.entries.forEach { entry->
                                    log.i("    RemoteConfig: ${entry.key} = ${entry.value.asString()} source: ${entry.value.source}")
                                }
                            }

                            if (cont.isActive) {
                                cont.resume(fetch.isSuccessful)
                            }
                            isInitialized = true
                            StartupPerformanceTracker.getInstance().onRemoteConfigInitializationEnd()
                        }
                    }
            } catch (e: Throwable) {
                StartupPerformanceTracker.getInstance().onRemoteConfigInitializationEnd()
                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
            }
        }
    }

    private fun getFirebaseRemoteConfig(context: Context) = try {
        FirebaseRemoteConfig.getInstance()
    } catch (e: java.lang.IllegalStateException) {
        FirebaseApp.initializeApp(context)
        FirebaseRemoteConfig.getInstance()
    }

    private fun <T> get(key: String, default: T, fetch: (key: String) -> T): T {

        if (!isInitialized) {
            if (isDebugMode) {
                error("!!!!!! RemoteConfig key $key queried before initialization !!!!!!")
            } else {
                log.e("!!!!!! RemoteConfig key $key queried before initialization !!!!!!")
                return default
            }
        }

        if (!this::firebaseRemoteConfig.isInitialized) {
            if (!isDebugMode) {
                log.e("RemoteConfig key $key queried before initialization")
                return default
            }
        }

        return if (firebaseRemoteConfig.getValue(key).source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            fetch(key)
        } else {
            default
        }
    }

    override fun name(): String = "Remote Config"

    @Suppress("unchecked_cast")
    override fun <T> ConfigRepository.getValue(key: String, default: T): T {
        return (get(key, default) {
            with (firebaseRemoteConfig) {
                when (default) {
                    is String -> getString(key)
                    is Boolean -> getBoolean(key)
                    is Long -> getLong(key)
                    is Double -> getDouble(key)
                    else -> error("Unsupported type")
                }
            }
        } ?: default) as T
    }

    override fun asMap(): Map<String, String> {

        val map = HashMap<String, String>()

        firebaseRemoteConfig.all.entries.forEach{ entry->
            map[entry.key] = entry.value.asString().lowercase()
        }

        return map
    }

    suspend fun allValuesToString(): String {
        return coroutineScope {
            val result = StringBuilder()

            firebaseRemoteConfig.all.entries.forEach { entry->
                result.appendLine("${entry.key} = ${entry.value.asString()} source: ${entry.value.source}")
            }

            result.toString()
        }
    }

    override fun contains(key: String): Boolean {

        if (!isInitialized) {
            log.e("!!!!!! RemoteConfig key $key queried (contains) before initialization !!!!!!")
            return false
        }

        if (!this::firebaseRemoteConfig.isInitialized) {
            if (!isDebugMode) {
                log.e("RemoteConfig key $key queried before initialization")
                return false
            }
        }

        return firebaseRemoteConfig.getValue(key).source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC
    }


}
