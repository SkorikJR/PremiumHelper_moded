package com.appboosty.blytics

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.appboosty.premiumhelper.Premium
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class SessionManager(
    private val application: Application,
    private val configuration: Configuration
) {
    private var currentSession: SessionData? = null
    private var lifecycleObserver: LifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            if (currentSession == null) {
                Timber.d("New session created")
                currentSession = createSession()
                onSessionStartEvent()
                checkAppUpdated()
            }
            cancelCloseSessionTask()
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            if(!Premium.preferences.isNextAppStartIgnored())
                scheduleCloseSessionTask()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Timber.d("onDestroy()-> Application is destroyed")
            closeSessionOnDestroy()
            super.onDestroy(owner)
        }
    }

    private fun onSessionStartEvent() {
        currentSession?.let { session ->
            CoroutineScope(Dispatchers.Default).launch {
                delay(3000)
                PremiumHelper.getInstance().analytics.onSessionOpen(
                    session.sessionId,
                    session.timestamp
                )
            }
        }
    }

    init {
        if (PremiumHelperUtils.isOnMainProcess(application) && isEnabled()) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        }
    }

    private fun checkAppUpdated(){
        currentSession?.let{
            if(PremiumHelperUtils.isFirstStartAfterUpdate(application, PremiumHelper.getInstance().preferences)){
                PremiumHelper.getInstance().analytics.onAppUpdated(it.sessionId)
            }
        }
    }

    private fun createSession(): SessionData {
        return SessionData(UUID.randomUUID().toString(), System.currentTimeMillis())
    }

    private fun scheduleCloseSessionTask() {
        currentSession?.let {
            it.calculateDuration()
            val delayInSeconds = configuration.get(Configuration.SESSION_TIMEOUT_SECONDS)
            val data = Data.Builder()
            data.putString(
                "session",
                Gson().toJson(it.createCloseSessionData())
            )
            val builder = OneTimeWorkRequestBuilder<CloseSessionWorker>()
                .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
                .setInputData(data.build())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                builder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(1))
            
            Timber.d("The close session task will run in $delayInSeconds seconds")
            WorkManager.getInstance(application)
                .enqueueUniqueWork(
                    CloseSessionWorker.TAG,
                    ExistingWorkPolicy.REPLACE,
                    builder.build()
                )
        }
    }

    private fun cancelCloseSessionTask() {
        WorkManager.getInstance(application).cancelUniqueWork(CloseSessionWorker.TAG)
        Timber.d("The close session task cancelled")
    }


    private fun isEnabled(): Boolean {
        return configuration.get(Configuration.TOTOLYTICS_ENABLED)
    }

    private fun closeSessionOnDestroy() {
        cancelCloseSessionTask()
        currentSession?.let {
            currentSession = null
            it.calculateDuration()
            Timber.d("closeSessionOnDestroy()-> called. ID: ${it.sessionId} Session duration: ${it.duration} millis")
            sendAnalytics(it.createCloseSessionData())
        } ?: Timber.d("No active session found !")
    }

    fun sendAnalytics(sessionData: SessionData): Boolean {
        return if (isEnabled()) {
            //Log.d("SessionManager", "--------- UPLOADING SESSION id: ${sessionData.id} Session duration: ${sessionData.value / 1000} seconds --------")
            PremiumHelper.getInstance().analytics.onSessionClose(sessionData.sessionId, sessionData.timestamp, sessionData.duration)
            currentSession = null
            true
        } else true
    }

    @Keep
    data class SessionData(
        @SerializedName("sessionId") val sessionId: String,
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("duration") var duration: Long = 0L
    ) {
        fun calculateDuration() {
            duration = System.currentTimeMillis() - timestamp
        }

        fun createCloseSessionData() = SessionData(
            sessionId,
            System.currentTimeMillis(),
            System.currentTimeMillis() - timestamp
        )
    }

    internal class CloseSessionWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        companion object {
            const val TAG = "CloseSessionWorker"
        }

        override suspend fun doWork(): Result {
            inputData.getString("session")?.let {
                try {
                    val sessionData = Gson().fromJson(it, SessionData::class.java)
                    return if (PremiumHelper.getInstance().sessionManager.sendAnalytics(sessionData))
                        Result.success() else Result.retry()
                } catch (ex: JsonSyntaxException) {
                    Timber.e("Can't upload session data. Parsing failed. ${ex.message}")
                }
            }
            return Result.success()
        }
    }
}