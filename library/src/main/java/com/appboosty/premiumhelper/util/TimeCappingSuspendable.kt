package com.appboosty.premiumhelper.util

import android.text.format.DateUtils
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 *  Execute code with timed capping.
 *
 *  @param capping_ms Capping period in ms
 *  @param last_call_time Last call timestamp in ms
 *  @param autoUpdate Update last call timestamp automatically when checking capping. Disable if you need to control it manually.
 */
class TimeCappingSuspendable(private val capping_ms: Long, private var last_call_time: Long = 0, private val autoUpdate: Boolean = true) {

    companion object {
        @JvmStatic
        fun ofSeconds(capping_seconds: Long, last_call_time: Long = 0, autoUpdate: Boolean = true): TimeCappingSuspendable {
            return TimeCappingSuspendable(capping_seconds  * DateUtils.SECOND_IN_MILLIS, last_call_time, autoUpdate)
        }
        @JvmStatic
        fun ofMinutes(capping_minutes: Long, last_call_time: Long = 0, autoUpdate: Boolean = true): TimeCappingSuspendable {
            return TimeCappingSuspendable(capping_minutes  * DateUtils.MINUTE_IN_MILLIS, last_call_time, autoUpdate)
        }
        @JvmStatic
        fun ofHours(capping_hours: Long, last_call_time: Long = 0, autoUpdate: Boolean = true): TimeCappingSuspendable {
            return TimeCappingSuspendable(capping_hours  * DateUtils.HOUR_IN_MILLIS, last_call_time, autoUpdate)
        }
        @JvmStatic
        fun ofDays(capping_days: Long, last_call_time: Long = 0, autoUpdate: Boolean = true): TimeCappingSuspendable {
            return TimeCappingSuspendable(capping_days  * DateUtils.DAY_IN_MILLIS, last_call_time, autoUpdate)
        }
    }

    private fun check(): Boolean {
        val now = System.currentTimeMillis()

        return when {
            capping_ms == 0L -> true
            now - last_call_time > capping_ms -> {
                if (autoUpdate) update()
                true
            }
            else -> false
        }
    }

    /**
     *  Run code with capping rules applied
     *
     *  @param onSuccess Execute this block when capping is not applied
     *  @param onCapped Execute this block when capping is applied
     */
    suspend fun runWithCapping(onSuccess: suspend () -> Unit, onCapped: suspend () -> Unit) {
        if (check()) {
            onSuccess()
        } else {
            Timber.tag("TimeCapping").i("Skipped due to capping. Next in ${timeToNext()}sec.")
            onCapped()
        }
    }

    /**
     *  Run code with capping rules applied
     *
     *  @param onSuccess Execute this block when capping is not applied
     */
    suspend fun runWithCapping(onSuccess: suspend () -> Unit) {
        runWithCapping(onSuccess) {}
    }

    /**
     *  Update last call timestamp. Use when autoUpdate is FALSE.
     */
    fun update() {
        last_call_time = System.currentTimeMillis()
    }

    fun reset() {
        last_call_time = 0
    }

    fun timeToNext(): Long = TimeUnit.MILLISECONDS.toSeconds((last_call_time + capping_ms - System.currentTimeMillis()))

}