package com.appboosty.premiumhelper.util

import android.content.Context
import androidx.core.os.bundleOf
import com.appboosty.premiumhelper.Preferences
import com.facebook.applinks.AppLinkData
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import kotlin.coroutines.resume

class FacebookInstallData(private val context: Context) {

    private val preferences = Preferences(context)

    suspend fun fetchAndReport() {
        coroutineScope {
            launch(Dispatchers.IO) {
                if (!preferences.isFacebookInstallHandled()) {
                    try {
                        reportToFirebase(fetchFromServer())
                    } catch (e: ClassNotFoundException) {
                    } catch (err: NoClassDefFoundError) {
                    }
                    preferences.setFacebookInstallHandled(true)
                }
            }
        }
    }

    private fun reportToFirebase(data: AppLinkData?) {
        if (data != null) {
            FirebaseAnalytics.getInstance(context).logEvent(
                "fb_install", bundleOf(
                    "uri" to data.targetUri.toString(),
                    "promo" to data.promotionCode
                )
            )
        }
    }

    private suspend fun fetchFromServer(): AppLinkData? {
        return try {
            suspendCancellableCoroutine { cont ->
                AppLinkData.fetchDeferredAppLinkData(context) {
                    if (cont.isActive) {
                        cont.resume(it)
                    }
                }
            }
        }catch (ex: Exception){
            Timber.e(ex)
            null
        }
    }

}