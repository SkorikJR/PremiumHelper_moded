package com.appboosty.premiumhelper.configuration.toto

import android.content.Context
import com.appboosty.premiumhelper.configuration.ConfigRepository
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.toto.WeightedValueParameter
import kotlinx.coroutines.coroutineScope

class TotoConfigRepository(val context: Context) : ConfigRepository {

    private val sharedPreferences = context.getSharedPreferences("toto_configuration", Context.MODE_PRIVATE)

    override fun name(): String = "Toto Service"

    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    @Suppress("unchecked_cast")
    override fun <T> ConfigRepository.getValue(key: String, default: T): T {
        return (when (default) {
            is String -> sharedPreferences.getString(key, default)
            is Boolean -> sharedPreferences.getString(key, null)?.toBooleanStrictOrNull()
            is Long -> sharedPreferences.getString(key, null)?.toLongOrNull()
            is Double -> sharedPreferences.getString(key, null)?.toDoubleOrNull()
            else -> error("Unsupported type")
        } ?: default) as T
    }

    override fun asMap(): Map<String, String> {
        val map = HashMap<String, String> ()

        sharedPreferences.all
            .filterNot { it.key.endsWith("_hash") }
            .filterNot { it.key.equals("x-country") }
            .forEach { entry ->
                map[entry.key] = entry.value.toString().lowercase()
            }

        return map
    }

    private fun hashKey(key: String) = key + "_hash"

    private fun putTotoParam(key: String, value: String, paramHash: Int) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            putInt(hashKey(key), paramHash)
            apply()
        }
    }

    private fun getHash(key: String): Int? {
        return if (sharedPreferences.contains(hashKey(key))) {
            sharedPreferences.getInt(hashKey(key), -1)
        } else {
            null
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

    /**
     *  Save configuration parameters received from Toto service.
     *  Each parameter is checked for updates by comparing it's hash with stored hash value.
     *  For updated parameter a new random value is selected and saved alongside with hash.
     *
     *  @param config List of received parameters
     *  @return true if configuration was changed
     *
     *          false no updates were made
     */
    fun update(config: List<WeightedValueParameter>, country: String): Boolean {

        val removedKeys = getRemovedKeys(config)
        var updated = removedKeys.isNotEmpty()

        with(sharedPreferences.edit()) {
            putString("x-country", country)

            removedKeys.forEach { key ->
                remove(key)
                remove(hashKey(key))
            }

            apply()
        }

        config.filterNot { it.name.equals(Configuration.TOTO_ENABLED.key, ignoreCase = true) }
            .onEach { param ->
                if (param.hash() != getHash(param.name)) {
                    // Parameter is updated, need to pick a new random value
                    val value = param.pickRandomValue()
                    putTotoParam(param.name, value, param.hash())
                    updated = true
                }
            }

        return updated
    }

    fun getConfigCountry(): String {
        return get("x-country", "")
    }

    private fun getRemovedKeys(config: List<WeightedValueParameter>): List<String> {
        val configKeys = config.map { it.name }.toHashSet()
        return sharedPreferences.all.keys
            .filterNot { it == "x-country" }
            .filterNot { it.endsWith("_hash") }
            .filterNot { configKeys.contains(it) }
            .toList()
    }
}