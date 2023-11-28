package com.appboosty.premiumhelper

import android.content.Context
import androidx.work.*

internal class AcknowledgePurchaseWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {

        private const val TAG = "AcknowledgePurchaseWorker"

        fun schedule(context: Context) {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<AcknowledgePurchaseWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }

    }

    override suspend fun doWork(): Result {
        PremiumHelper.getInstance().billing.acknowledgeAll()
        return Result.success()
    }
}