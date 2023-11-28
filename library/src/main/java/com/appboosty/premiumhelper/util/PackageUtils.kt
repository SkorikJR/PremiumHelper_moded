package com.appboosty.premiumhelper.util

import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

object PackageUtils {
    fun getAppVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
            ""
        }
    }
}