package com.appboosty.blytics

import android.app.Application
import android.os.Bundle
import com.appboosty.blytics.model.Session
import com.appboosty.premiumhelper.util.PHResult
import timber.log.Timber

/**
 * Created by Sergey B on 10.05.2018.
 */
abstract class AnalyticsPlatform {

    companion object {
        private val optionalParameters =
            listOf("isForegroundSession", "days_since_install", "occurrence")
    }
    protected var debug = false
    abstract val name: String?
    abstract fun isEnabled(application: Application): Boolean
    open fun initialize(application: Application, debug: Boolean) {
        this.debug = debug
    }

    abstract fun track(event: String, params: Bundle)
    abstract fun onSessionStart(session: Session?)
    abstract fun onSessionFinish(session: Session?)
    abstract fun setUserId(userId: String)
    abstract fun setUserProperty(property: String?, value: String?)
    fun ensureParamsLength(params: Bundle, maxLength: Int): Bundle {
        for (key in params.keySet()) {
            if (params[key] is String) {
                val value = params.getString(key)
                if (value != null && value.length > maxLength) {
                    params.putString(key, value.substring(0, maxLength))
                }
            }
        }
        return params
    }

    fun asMap(params: Bundle): Map<String, String> {
        val result: MutableMap<String, String> = HashMap()
        for (key in params.keySet()) {
            result[key] = params[key].toString()
        }
        return result
    }

    abstract fun getMaximumParametersCount(): Int

    fun ensureParamsCount(
        params: Map<String, String>
    ): PHResult<Map<String, String>> {
        return if (params.size <= getMaximumParametersCount()) PHResult.Success(params)
        else {
            val mutableParams = params.toMutableMap()
            val iterator = getOptionalParametersList().iterator()
            while (mutableParams.size > getMaximumParametersCount() && iterator.hasNext()) {
                mutableParams.remove(iterator.next())
            }

            if (mutableParams.size > getMaximumParametersCount()) {
                Timber.w("$name: Failed to shorten the parameters list by removing optional parameters. Cutting ${mutableParams.size - getMaximumParametersCount()} parameters")
                cutParametersToLimit(mutableParams, params.size)
                if (mutableParams.size > getMaximumParametersCount())
                    PHResult.Failure(java.lang.IllegalArgumentException("The number of parameters still above the limit: ${mutableParams.size} (${getMaximumParametersCount()})"))
                else
                    PHResult.Success(mutableParams)
            } else
                PHResult.Success(mutableParams)
        }
    }

    private fun cutParametersToLimit(params: MutableMap<String, String>, originalCount: Int) {
        val iterator = params.keys.iterator()
        while (iterator.hasNext() && params.size > getMaximumParametersCount() - 1) {
            val key = iterator.next()
            Timber.w("$name: Removing analytics parameter: $key")
            iterator.remove()
        }
        params["limit_exceeded"] = "Limit: ${getMaximumParametersCount()} Params: $originalCount"
    }

    open fun getOptionalParametersList(): List<String> = optionalParameters
}