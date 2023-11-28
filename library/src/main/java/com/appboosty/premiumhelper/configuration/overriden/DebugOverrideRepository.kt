package com.appboosty.premiumhelper.configuration.overriden

import com.appboosty.premiumhelper.configuration.ConfigRepository

class DebugOverrideRepository : ConfigRepository {

    private val values = HashMap<String, String>()

    fun put(key: String, value: String) {
        values[key] = value
    }

    override fun name(): String = "Debug Override"

    override fun contains(key: String): Boolean {
        return values.containsKey(key)
    }

    @Suppress("unchecked_cast")
    override fun <T> ConfigRepository.getValue(key: String, default: T): T {
        return (when (default) {
            is String -> values[key]
            is Boolean -> values[key]?.toBooleanStrictOrNull()
            is Long -> values[key]?.toLongOrNull()
            is Double -> values[key]?.toDoubleOrNull()
            else -> error("Unsupported type")
        }  ?: default) as T
    }

    override fun asMap(): Map<String, String> = values

    override fun toString(): String {
        return buildString {
            if (values.isNotEmpty()) {
                appendLine("Debug Override")
                values.entries.forEach { (key, value) ->
                    appendLine("$key : $value")
                }
            }
        }
    }
}