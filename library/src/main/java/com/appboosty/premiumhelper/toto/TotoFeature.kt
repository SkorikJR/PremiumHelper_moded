package com.appboosty.premiumhelper.toto

import android.app.Application
import android.content.Context
import android.os.Build
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import com.appboosty.premiumhelper.performance.StartupPerformanceTracker
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.error
import com.appboosty.premiumhelper.util.isSuccess
import com.appboosty.premiumhelper.util.onError
import com.appboosty.premiumhelper.util.onSuccess
import com.appboosty.premiumhelper.util.successValue
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.util.*

class TotoFeature(private val context: Context, private val configuration: Configuration, private val preferences: Preferences) {

    private val userAgent: String by lazy {
        context.packageName + "_" + PremiumHelperUtils.getVersionName(context)
    }

    private val serviceConfig: TotoService.ServiceConfig by lazy {
        if (configuration.isDebugMode()) {
            TotoService.ServiceConfig.Staging
        } else {
            TotoService.ServiceConfig.Production
        }
    }

    private val service: TotoService.TotoServiceApi by lazy {
        TotoService.build(serviceConfig, configuration.isDebugMode())
    }

    data class ResponseStats(val code: String, val latency: Long)

    var getConfigResponseStats: ResponseStats? = null

    data class TotoResponse <T> (val result: PHResult<Response<T>>, val responseStats: ResponseStats)

    private suspend fun <T> callApi(call: suspend () -> Response<T>) : TotoResponse<T> {

        val callStart = System.currentTimeMillis()

        return try {

            val response = call()

            val stats = ResponseStats(response.code().toString(), response.raw().receivedResponseAtMillis - response.raw().sentRequestAtMillis)

            if (response.isSuccessful) {
                TotoResponse(PHResult.Success(response), stats)
            } else {
                TotoResponse(PHResult.Failure(HttpException(response)), stats)
            }

        } catch (e : Exception) {

            val stats = if (PremiumHelperUtils.hasInternetConnection(context)) {
                ResponseStats(e.message ?: "Unknown exception", System.currentTimeMillis() - callStart)
            } else {
                ResponseStats("No connection", System.currentTimeMillis() - callStart)
            }

            TotoResponse(PHResult.Failure(e), stats)
        }
    }

    private suspend fun <T> callApiWithRetry(retryCount: Int, call: suspend () -> Response<T>) : TotoResponse<T> {

        repeat(retryCount) {
            with(callApi(call)) {
                if (result.isSuccess) {
                    return this
                }
            }
        }

        return callApi(call)
    }

    suspend fun getConfig(): PHResult<Any> {

        return with (callApiWithRetry(1) { service.getConfig(context.packageName, userAgent) }) {

            getConfigResponseStats = responseStats
            getConfigResponseStats?.let{
                StartupPerformanceTracker.getInstance().setTotoConfigResult(it.code)
            }
            result
                .onSuccess { result ->
                    val body = result.body() 

                    if (body != null) {

                        val xcountry = result.headers().get("x-country") ?: ""

                        if (configuration.updateConfiguration(body.asWeightedParamsList(), xcountry)) {
                            PostConfigWorker.scheduleNow(context)
                        } else if (!preferences.get("post_config_sent", false)) {
                            // Send configuration once if no update received
                            PostConfigWorker.scheduleNow(context)
                        }

                    }
                }
                .onError { e ->
                    Timber.e(e.error)
                    if (!preferences.get("post_config_sent", false)) {
                        // Send configuration once if GET config failed
                        PostConfigWorker.scheduleNow(context)
                    }
                }

            PremiumHelper.getInstance().analytics.onGetConfig(responseStats, result.successValue?.headers()?.get("x-cache") ?: "")

            result
        }

    }

    suspend fun postConfig(): PHResult<Any> {

        val country = PremiumHelper.getInstance().configuration.getTotoConfigurationCountry()
        val config = PremiumHelper.getInstance().configuration.getConfigurationMap()

        val parameters = TotoService.PostConfigParameters(
            PremiumHelperUtils.getInstalledDate(context),
            PremiumHelperUtils.getVersionName(context),
            PremiumHelper.getInstance().appInstanceId.get(),
            country,
            "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android ${Build.VERSION.SDK_INT}",
            Build.VERSION.RELEASE,
            Locale.getDefault().language
        )

        val response = callApi { service.postConfig(context.packageName, userAgent, parameters.asMap(), config) }

        PremiumHelper.getInstance().analytics.onPostConfig(response.responseStats)

        if (response.result.isSuccess) {
            preferences.putBoolean("post_config_sent", true)
        }

        return response.result
    }

    suspend fun registerFcmToken(token: String): Boolean {

        val purchaseInfo = preferences.getActivePurchaseInfo()

        if (purchaseInfo != null) {
            val installTimestamp = PremiumHelperUtils.getInstalledDate(context as Application) / 1000
            val request = TotoService.RegisterRequest(
                context.packageName,
                PremiumHelperUtils.getVersionName(context),
                installTimestamp,
                PremiumHelper.getInstance().appInstanceId.get(),
                purchaseInfo.sku,
                purchaseInfo.purchaseToken,
                token
            )

            val response = callApi { service.register(request, userAgent) }

            PremiumHelper.getInstance().analytics.onTotoRegister(response.responseStats)

            if (!response.result.isSuccess) {
                throw response.result.error ?: error("Empty error")
            }

        } else {
            Timber.e("Register called with no purchase data")
        }

        return true
    }

    fun scheduleRegister(forceRegister: Boolean = false) {
        if (forceRegister || !preferences.isFcmRegistered()) {
            TotoRegisterWorker.schedule(context)
        }
    }

}