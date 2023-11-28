package com.appboosty.premiumhelper.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.configuration.Configuration
import timber.log.Timber

internal object UpdateManager {

    fun checkForUpdate(activity: Activity) {

        val premiumHelper = PremiumHelper.getInstance()

        if (!PremiumHelper.getInstance().configuration.get(Configuration.IN_APP_UPDATES_ENABLED)) {
            Timber.tag(TAG).d("UpdateManager: updates disabled")
            return
        }

        val maxUpdateAttempts = premiumHelper.configuration.get(Configuration.MAX_UPDATE_REQUESTS)

        if (maxUpdateAttempts <= 0) {
            Timber.tag(TAG).d("UpdateManager: updates disabled by maxUpdateAttempts")
            return
        }

        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                val lastUpdateVersion = premiumHelper.preferences.getInt("latest_update_version", -1)
                val updateAttempts = premiumHelper.preferences.getInt("update_attempts", 0)

                if (lastUpdateVersion == appUpdateInfo.availableVersionCode() && updateAttempts >= maxUpdateAttempts) {
                    Timber.tag(TAG).d("UpdateManager: max update attempts reached")
                } else {
                    Timber.tag(TAG).d("UpdateManager: starting update flow $appUpdateInfo")

                    appUpdateManager.startUpdateFlow(appUpdateInfo, activity, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
                    premiumHelper.ignoreNextAppStart()

                    if (lastUpdateVersion != appUpdateInfo.availableVersionCode()) {
                        premiumHelper.preferences.putInt("latest_update_version", appUpdateInfo.availableVersionCode())
                        premiumHelper.preferences.putInt("update_attempts", 1)
                    } else {
                        premiumHelper.preferences.putInt("update_attempts", updateAttempts + 1)
                    }

                }

            } else {
                Timber.tag(TAG).d("UpdateManager: no updates available $appUpdateInfo")
            }

        }

        appUpdateInfoTask.addOnFailureListener {
            Timber.tag(TAG).e(it)
        }

    }

    fun resumeUnfinishedUpdate(activity: Activity) {
        if(!PremiumHelper.getInstance().configuration.get(Configuration.IN_APP_UPDATES_ENABLED)) return
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Timber.tag(TAG).d("UpdateManager: resuming update flow $appUpdateInfo")
                appUpdateManager.startUpdateFlow(appUpdateInfo, activity, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
                PremiumHelper.getInstance().ignoreNextAppStart()
            }
        }

        appUpdateInfoTask.addOnFailureListener {
            Timber.tag(TAG).e(it)
        }
    }


}