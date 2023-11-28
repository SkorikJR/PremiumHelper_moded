package com.appboosty.premiumhelper.util

import android.annotation.SuppressLint
import android.text.format.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


object PhDateTimeUtils {
    fun toHumanReadableDate(millis: Long): String{
        return DateFormat.format("dd-MM-yyyy HH:mm:ss", Date(millis)).toString()
    }

    @SuppressLint("DefaultLocale")
    fun toHumanReadableDuration(millis: Long):String{
        return java.lang.String.format(
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                TimeUnit.MILLISECONDS.toHours(
                    millis
                )
            ),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(
                    millis
                )
            )
        )
    }
}