package com.appboosty.premiumhelper.log

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class FirebaseCrashReportTree(private val context: Context) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        getFirebaseCrashlytics()?.log("$tag:$message")

        if (t != null) {
            if (priority == Log.ERROR) {
                getFirebaseCrashlytics()?.recordException(t)
            }
        }
    }

    private fun getFirebaseCrashlytics() = try {
        FirebaseCrashlytics.getInstance()
    } catch (e: IllegalStateException) {
        FirebaseApp.initializeApp(context)
        try {
            FirebaseCrashlytics.getInstance()
        } catch (e: IllegalStateException) {
            null
        }
    }

}