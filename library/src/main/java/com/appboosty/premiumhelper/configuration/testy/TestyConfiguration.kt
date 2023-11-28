package com.appboosty.premiumhelper.configuration.testy

import android.content.Context
import android.content.pm.Signature
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.appboosty.premiumhelper.configuration.ConfigRepository
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class TestyConfiguration : ConfigRepository {

    private val testyShaKey = "2e1a4db5f9be9d82747e791845a00669205f3c4"

    private val values = HashMap<String, String>()

    override fun name() = "Testy Configuration"

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

    suspend fun init(context: Context) {
        withContext(Dispatchers.IO) {
            if (checkTestyAppSignature(context)) {
                Timber.i("Found Testy app")

                val uri = Uri.parse("content://com.appboosty.testykal.provider.TestyContentProvider/" + context.packageName)
                val cursor = context.contentResolver.query(uri, null, context.packageName, null, null)

                cursor?.let {
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex("ph_params")
                        if (index >= 0) {
                            val type = object:TypeToken<Map<String, String>>(){}.type
                            val params = Gson().fromJson<Map<String, String>>(cursor.getString(index), type)
                            Timber.i("Loaded testy config: $params")
                            values.putAll(params)
                        }
                    }
                    cursor.close()
                }
            }
        }
    }

    private fun checkTestyAppSignature(context: Context): Boolean {

        try {

            val signature = PremiumHelperUtils.getPackageSignature(context, "com.appboosty.testykal")

            signature?.let {
                getCertificate(it)?.let { cert ->
                    val sha = PremiumHelperUtils.sha1(cert.publicKey.toString())
                    return if (testyShaKey == sha) {
                        true
                    } else {
                        Timber.e("Invalid Testy Kal app installed! ($sha)")
                        false
                    }
                }
            }

        } catch (e: Throwable) {
            Timber.e(e)
        }

        return false
    }

    private fun getCertificate(signature: Signature): X509Certificate? {
        return ByteArrayInputStream(signature.toByteArray()).use { stream ->
            val certFactory = CertificateFactory.getInstance("X509")
            certFactory.generateCertificate(stream) as? X509Certificate
        }
    }

}