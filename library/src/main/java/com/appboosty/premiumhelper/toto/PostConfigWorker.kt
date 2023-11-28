package com.appboosty.premiumhelper.toto

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult

class PostConfigWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {

        private const val TAG = "PostConfigWorker"

        /**
         *  Schedule configuration update now.
         */
        fun scheduleNow(context: Context) {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PostConfigWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

    }


    override suspend fun doWork(): Result {

        PremiumHelper.getInstance().waitForInitComplete()

        val result = PremiumHelper.getInstance().totoFeature.postConfig()

        return if (result is PHResult.Failure) {
            Result.retry()
        } else {
            Result.success()
        }
    }

}