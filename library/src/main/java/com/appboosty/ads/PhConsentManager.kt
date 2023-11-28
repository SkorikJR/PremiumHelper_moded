package com.appboosty.ads

import android.app.Activity
import android.content.Context
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.ConsentStatus.NOT_REQUIRED
import com.google.android.ump.ConsentInformation.ConsentStatus.OBTAINED
import com.google.android.ump.ConsentInformation.ConsentStatus.REQUIRED
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration.Params.CONSENT_REQUEST_ENABLED
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.lang.Exception

internal class PhConsentManager(context: Context) {

    enum class ConsentResultCodes {
        RESULT_OK,
        ERROR
    }

    companion object {
        private val TAG = PhConsentManager::class.java.simpleName
        private const val CONSENT_FORM_WAITING_TIMEOUT = 5000L

        private const val CONSENT_FORM_WAS_SHOWN = "consent_form_was_shown"
    }

    private val sharedPreferences = context.getSharedPreferences("premium_helper_data", Context.MODE_PRIVATE)

    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null

    var isConsentFormShown: Boolean
        get() = sharedPreferences.getBoolean(CONSENT_FORM_WAS_SHOWN, false)
        private set(value) {
            sharedPreferences.edit().putBoolean(CONSENT_FORM_WAS_SHOWN, value).apply()
        }

    private var requestInProgress = false
    var isConsentAvailable: Boolean = true
        get() {
            return !PremiumHelper.getInstance().hasActivePurchase() &&
                    consentEnabledInConfiguration() &&
                    (consentInformation?.consentStatus == OBTAINED
                            || consentInformation?.consentStatus == REQUIRED)
        }
        private set

    private val currentStatus = MutableStateFlow<ConsentStatus?>(null)

    fun prepareConsentInfoIfNotReady(activity: AppCompatActivity) {
        if (consentForm == null)
            prepareConsentInfo(activity) {}
    }

    @Synchronized
    fun prepareConsentInfo(
        activity: AppCompatActivity, onConsentFormRequired: (() -> Unit)? = null,
        onConsentFormNotRequired: (() -> Unit)? = null
    ) {
        if (requestInProgress) return
        if (!consentEnabledInConfiguration()) {
            onConsentFormNotRequired?.invoke()
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            requestInProgress = true
            currentStatus.emit(null)
            val params = ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)

            if (PremiumHelper.getInstance().isDebugMode()) {
                val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                debugSettingsBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)

                PremiumHelper.getInstance().configuration.appConfig.debugData?.getStringArray("test_advertising_ids")
                    ?.forEach {
                        debugSettingsBuilder.addTestDeviceHashedId(it)
                        Timber.d("Adding test device hash id: $it")
                    }
                params.setConsentDebugSettings(debugSettingsBuilder.build())
            }

            UserMessagingPlatform.getConsentInformation(activity).apply {
                val status = ConsentStatus(null)
                requestConsentInfoUpdate(
                    activity,
                    params.build(),
                    {
                        consentInformation = this
                        if (isConsentFormAvailable) {
                            var notRequiredTask = onConsentFormNotRequired
                            if (consentStatus == OBTAINED || consentStatus == NOT_REQUIRED) {
                                Timber.tag(TAG).d("Current status doesn't require consent: $consentStatus")
                                onConsentFormNotRequired?.invoke()
                                notRequiredTask = null
                            }
                            CoroutineScope(Dispatchers.Main).launch {
                                loadForm(activity, status, onConsentFormRequired, notRequiredTask)
                            }
                        } else {
                            Timber.tag(TAG).d("No consent form available")
                            status.error = ConsentError(errorMessage = "No consent form available")
                            submitStatus(status)
                            requestInProgress = false
                            onConsentFormNotRequired?.invoke()
                        }
                    },
                    { error ->
                        Timber.tag(TAG).e("Consent info request error: ${error.errorCode} -  ${error.message}")
                        status.error = ConsentError(error.message, error)
                        submitStatus(status)
                        requestInProgress = false
                        onConsentFormNotRequired?.invoke()
                    })
            }
        }
    }

    private fun submitStatus(status: ConsentStatus?) {
        CoroutineScope(Dispatchers.Default).launch {
            currentStatus.emit(status)
        }
    }

    private suspend fun waitForConsentForm(): PHResult<Unit> {
        return try {
            coroutineScope {
                val loadFormAction = async {
                    if (currentStatus.value == null) {
                        currentStatus.first { it != null }
                    }
                    true
                }
                withTimeout(CONSENT_FORM_WAITING_TIMEOUT) { awaitAll(loadFormAction) }
                PHResult.Success(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).e("Timeout while waiting for consent form!")
            PHResult.Failure(e)
        }
    }


    @MainThread
    private fun loadForm(
        activity: Activity,
        consentStatus: ConsentStatus,
        onConsentFormRequired: (() -> Unit)?,
        onConsentFormNotRequired: (() -> Unit)?
    ) {
        consentInformation?.let {
            UserMessagingPlatform.loadConsentForm(
                activity,
                { form ->
                    if (it.consentStatus == REQUIRED) {
                        consentForm = form
                        submitStatus(consentStatus)
                        onConsentFormRequired?.invoke()
                    } else {
                        Timber.tag(TAG).d("loadForm()-> Consent form is not required")
                        consentForm = form
                        submitStatus(consentStatus)
                        onConsentFormNotRequired?.invoke()
                    }
                    requestInProgress = false
                },
                {
                    Timber.tag(TAG).e(it.message)
                    consentStatus.error = ConsentError(it.message, it)
                    submitStatus(consentStatus)
                    requestInProgress = false
                }
            )
        } ?: run {
            requestInProgress = false
            Timber.tag(TAG).e("loadForm()-> Consent info is missing. Should never happen")
        }
    }

    private fun isConsentFormCanBeSkipped(): Boolean {
        return PremiumHelper.getInstance().hasActivePurchase() ||
                consentInformation?.consentStatus == OBTAINED ||
                !consentEnabledInConfiguration()
    }

    private fun consentEnabledInConfiguration() = PremiumHelper.getInstance().configuration.get(CONSENT_REQUEST_ENABLED)

    suspend fun askForConsentIfRequired(
        activity: AppCompatActivity,
        forced: Boolean = false,
        onDone: (result: ConsentResult) -> Unit
    ) {
        if (isConsentFormCanBeSkipped() && !forced) {
            Timber.tag(TAG).d("askForConsentIfRequired()-> Consent form can be skipped")
            onDone(ConsentResult(ConsentResultCodes.RESULT_OK))
            return
        }
        waitForConsentForm()

        currentStatus.value?.let { status ->
            if (isConsentFormCanBeSkipped() && !forced) {
                Timber.tag(TAG).d("Consent is not required")
                onDone(ConsentResult(ConsentResultCodes.RESULT_OK))
                return
            }
            if (consentForm == null || consentInformation == null || status.error != null) {
                Timber.tag(TAG).e("Can't show consent dialog. Error: ${status.error}")
                onDone(ConsentResult(ConsentResultCodes.ERROR, status.error?.errorMessage ?: "Unknown error"))
                return
            }
            if (activity.isFinishing) {
                onDone(ConsentResult(ConsentResultCodes.ERROR, "The activity is no longer active"))
                return
            }
            try {
                consentForm?.show(
                    activity
                ) { formError ->
                    formError?.let {
                        Timber.tag(TAG).e("${it.errorCode} - ${formError.message}")
                    }
                    CoroutineScope(Dispatchers.IO).launch { isConsentFormShown = true }
                    if (consentInformation?.consentStatus == OBTAINED) {
                        onDone(ConsentResult(ConsentResultCodes.RESULT_OK))
                    } else {
                        Timber.tag(TAG).e("Consent form cancelled")
                        onDone(
                            ConsentResult(
                                ConsentResultCodes.ERROR,
                                "Consent status: ${consentInformation?.consentStatus}"
                            )
                        )
                    }
                    consentForm = null
                    submitStatus(null) //Prevent showing the same form twice
                    prepareConsentInfo(activity) {}
                } ?: run {
                    Timber.tag(TAG).e("Should never happen. Consent form is missing")
                    onDone(ConsentResult(ConsentResultCodes.ERROR, "Should never happen. Consent form is missing"))
                }
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex)
                onDone(ConsentResult(ConsentResultCodes.ERROR, "Exception thrown when asking for consent: ${ex.message}"))
            }
        }
    }

    private data class ConsentStatus(
        var error: ConsentError? = null,
    )

    private data class ConsentError(val errorMessage: String? = null, val errorForm: FormError? = null) {
        override fun toString(): String {
            return "ConsentError[ message:{$errorMessage} ErrorCode: ${errorForm?.errorCode}]"
        }
    }
    data class ConsentResult(val code: ConsentResultCodes, val errorMessage: String? = null)
}