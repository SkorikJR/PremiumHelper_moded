package com.appboosty.premiumhelper.toto

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionSpec

import okhttp3.TlsVersion

import android.os.Build
import timber.log.Timber
import java.lang.Exception
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


object TotoService {

    interface TotoServiceApi {

        @GET("/v1/apps/{package}/config")
        suspend fun getConfig(
            @Path("package") packageName: String,
            @Header("User-Agent") userAgent: String) : Response<Map<String, Map<String, Int>>>

        @POST("/v1/apps/{package}/config")
        suspend fun postConfig(
            @Path("package") packageName: String,
            @Header("User-Agent") userAgent: String,
            @QueryMap parameters: Map<String, String>,
            @Body config: Map<String, String>) : Response<Void>

        @Headers("Content-Type: application/json")
        @POST("api/v1/register")
        suspend fun register(@Body request: RegisterRequest, @Header("User-Agent") userAgent: String): Response<Void>

    }

    data class ServiceConfig(val endpoint: String, val secret: String) {
        companion object {
            val Production = ServiceConfig("https://config.appboosty.com/", "")
            val Staging = ServiceConfig("https://staging.config.appboosty.com/", "")
        }
    }

    data class RegisterRequest(
        val packageName: String,
        val version: String,
        val installTimestamp: Long,
        val obfuscatedUserID: String,
        val sku: String,
        val purchaseToken: String,
        val fcmToken: String
    )

    data class PostConfigParameters(
        val installTimestamp: Long,
        val versionName: String,
        val userId: String,
        val country: String = "",
        val deviceModel: String = "",
        val os: String = "Android",
        val osVersion: String = "",
        val lang: String = "") {

        fun asMap(): Map<String, String> {
            return mapOf(
                "installTimestamp" to installTimestamp.toString(),
                "version" to versionName,
                "userId" to userId,
                "country" to country,
                "deviceModel" to deviceModel,
                "os" to os,
                "osVersion" to osVersion,
                "lang" to lang
            ).filterValues { it.isNotEmpty() }
            .mapValues { URLEncoder.encode(it.value, "UTF-8") }
        }
    }

    fun build(config: ServiceConfig, isDebugMode: Boolean): TotoServiceApi {

        val client = with(OkHttpClient.Builder()) {

            if (isDebugMode) {

                val logInterceptor = HttpLoggingInterceptor()

                logInterceptor.apply {
                    logInterceptor.level =
                        if (isDebugMode) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                }

                addInterceptor(logInterceptor)
            }

            connectTimeout(5, TimeUnit.SECONDS)
            readTimeout(5, TimeUnit.SECONDS)
            writeTimeout(5, TimeUnit.SECONDS)

            enableTls12OnPreLollipop()

            build()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(config.endpoint)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(TotoServiceApi::class.java)
    }

    private fun OkHttpClient.Builder.enableTls12OnPreLollipop(): OkHttpClient.Builder {
        if (Build.VERSION.SDK_INT < 22) {
            try {
                val sc = SSLContext.getInstance("TLSv1.2")
                sc.init(null, null, null)
                findX509TrustManager()?.also { trust->
                    sslSocketFactory(Tls12SocketFactory(sc.socketFactory), trust)
                    connectionSpecs(arrayListOf(
                        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .build(),
                        ConnectionSpec.COMPATIBLE_TLS,
                        ConnectionSpec.CLEARTEXT
                    ))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while setting TLS 1.2")
            }
        }

        return this
    }

    private fun findX509TrustManager(): X509TrustManager? {
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
            trustManagers.also { managers ->
                if (managers.isNotEmpty()) {
                    (managers[0] as? X509TrustManager?)?.also { return it }
                }
            }
            return null
        }
    }

}