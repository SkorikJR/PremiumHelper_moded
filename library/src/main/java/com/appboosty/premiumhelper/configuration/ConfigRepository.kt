package com.appboosty.premiumhelper.configuration

interface ConfigRepository {
    fun name(): String
    fun contains(key: String): Boolean

    fun get(key: String, default: String) = getValue(key, default)
    fun get(key: String, default: Long) = getValue(key, default)
    fun get(key: String, default: Boolean) = getValue(key, default)
    fun get(key: String, default: Double) = getValue(key, default)

    fun <T> ConfigRepository.getValue(key: String, default: T): T

    fun asMap(): Map<String, String>
}