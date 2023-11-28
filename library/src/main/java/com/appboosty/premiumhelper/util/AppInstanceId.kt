package com.appboosty.premiumhelper.util

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume

class AppInstanceId(private val context: Context) {

    private val preferences = Preferences(context)

    suspend fun get(): String {
        return withContext(Dispatchers.IO) {
            val appInstanceId = preferences.getAppInstanceId()
            if (appInstanceId.isNullOrEmpty()) {
                suspendCancellableCoroutine { cont ->
                    FirebaseAnalytics.getInstance(context).appInstanceId.addOnCompleteListener {
                        val id = if (it.isSuccessful) {
                            it.result ?: UUID.randomUUID().toString()
                        } else {
                            UUID.randomUUID().toString()
                        }
                        Timber.tag(TAG).i("APPLICATION_INSTANCE_ID = $id")
                        preferences.setAppInstanceId(id)
                        if (cont.isActive) {
                            cont.resume(id)
                        }
                    }
                }
            } else {
                appInstanceId
            }
        }
    }

}