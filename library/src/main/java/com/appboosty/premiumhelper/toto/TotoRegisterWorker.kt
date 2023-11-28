package com.appboosty.premiumhelper.toto

import android.content.Context
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessaging
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.HttpException
import java.net.HttpURLConnection
import kotlin.coroutines.resume

internal class TotoRegisterWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val log by timber(TAG)
    private val preferences = Preferences(context)

    companion object {

        private const val TAG = "RegisterWorker"

        @JvmOverloads
        fun schedule(context: Context, fcmToken: String = "") {

            val data = workDataOf("fcm_token" to fcmToken)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<TotoRegisterWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }

    }

    override suspend fun doWork(): Result {

        val token = getFcmToken()

        if (token.isNullOrEmpty()) {
            return Result.retry()
        }

        val result = try {
            PremiumHelper.getInstance().totoFeature.registerFcmToken(token)
        } catch (e: Exception) {
            PremiumHelperUtils.reportError(e)
            if (e is HttpException) {
                when (e.code()) {
                    HttpURLConnection.HTTP_NOT_FOUND -> return Result.retry()
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> return Result.retry()
                    HttpURLConnection.HTTP_BAD_GATEWAY -> return Result.retry()
                    HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> return Result.retry()
                }
            }
            false
        }

        if (!result) {
            return Result.failure()
        }

        preferences.setFcmRegistered(true)

        return Result.success()
    }

    private suspend fun getFcmToken(): String? {

        val token = inputData.getString("fcm_token")

        return if (token.isNullOrEmpty()) {
            suspendCancellableCoroutine { cont ->
                try {
                    log.i("Requesting FCM token")
                    FirebaseMessaging.getInstance().token.addOnCompleteListener {
                        if (it.isSuccessful) {
                            log.i("Got FCM token: ${it.result}")
                            if (cont.isActive) {
                                cont.resume(it.result)
                            }
                        } else {
                            it.exception?.let { e -> PremiumHelperUtils.reportError(e) }
                            if (cont.isActive) {
                                cont.resume(null)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    log.e(e, "Failed to retrieve FCM token")
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
            }
        } else {
            log.i("New FCM token: $token")
            token
        }

    }

}