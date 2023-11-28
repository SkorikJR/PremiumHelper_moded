package com.appboosty.premiumhelper.util

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

class InstallReferrer(private val context: Context) {

    private val preferences = Preferences(context)

    suspend fun get(): String {
        return withContext(Dispatchers.IO) {
            preferences.getInstallReferrer() ?: loadInstallReferrer()
        }
    }

    private suspend fun loadInstallReferrer(): String {
        return suspendCancellableCoroutine { cont->
            val client = InstallReferrerClient.newBuilder(context).build()
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    try {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            val referrer = client.installReferrer.installReferrer
                            preferences.setInstallReferrer(referrer)
                            Timber.tag(TAG).d("Install referrer: $referrer")
                            if (cont.isActive) {
                                cont.resume(referrer)
                            }
                        } else {
                            if (cont.isActive) {
                                cont.resume("")
                            }
                        }

                        try { client.endConnection() } catch (e: Throwable) {}

                    } catch (e: RemoteException) {
                        if (cont.isActive) {
                            cont.resume("")
                        }
                    }
                }
                override fun onInstallReferrerServiceDisconnected() {}
            })
        }
    }

}