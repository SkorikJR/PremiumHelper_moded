package com.appboosty.permissions

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.appboosty.permissions.PermissionUtils.Callback
import com.appboosty.permissions.PermissionUtils.Callback2
import timber.log.Timber

/**
 * A permission request helper. Handles permission request and result handling.
 *
 * @param activity Permission requester activity.
 * @param permission Permission name
 */
class PermissionRequester(activity: AppCompatActivity, val permission: String) : BasePermissionRequester(activity) {

    private var grantedAction: ((requester: PermissionRequester) -> Unit)? = null
    private var deniedAction: ((requester: PermissionRequester) -> Unit)? = null
    private var rationaleAction: ((requester: PermissionRequester) -> Unit)? = null
    private var permanentlyDeniedAction: ((requester: PermissionRequester, canShowSettingsDialog: Boolean) -> Unit)? = null

    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            processPermissionResult(isGranted)
        }

    private fun processPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            grantedAction?.invoke(this)
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                deniedAction?.invoke(this)
            } else {
                permanentlyDeniedAction?.invoke(this, !rationaleShown)
            }
        }

        rationaleShown = false
    }

    fun hasPermission(): Boolean {
        return PermissionUtils.hasPermission(activity, permission)
    }

    override fun request() {
        if (PermissionUtils.hasPermission(activity, permission)) {
            grantedAction?.invoke(this)
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) && !rationaleShown && rationaleAction != null) {
                rationaleShown = true
                rationaleAction?.invoke(this)
            } else {
                try {
                    launcher.launch(permission)
                } catch (e: Throwable) {
                    Timber.e(e)
                    deniedAction?.invoke(this)
                }
            }
        }
    }

    override fun getActivityLauncher(): ActivityResultLauncher<*> {
        return launcher
    }

    /**
     * Callback for permission granted result.
     */
    fun onGranted(action: Callback<PermissionRequester>): PermissionRequester {
        return onGranted { action.call(it) }
    }

    /**
     * Callback for permission granted result.
     */
    fun onGranted(action: (requester: PermissionRequester) -> Unit): PermissionRequester {
        grantedAction = action
        return this
    }

    /**
     * Callback for permission denied result. Called when permission was denied by the user.
     */
    fun onDenied(action: Callback<PermissionRequester>): PermissionRequester {
        return onDenied { action.call(it) }
    }

    /**
     * Callback for permission denied result. Called when permission was denied by the user.
     */
    fun onDenied(action: (requester: PermissionRequester) -> Unit): PermissionRequester {
        deniedAction = action
        return this
    }

    /**
     * Callback for permission rationale scenario. Called when permission requires showing rationale.
     */
    fun onRationale(action: Callback<PermissionRequester>): PermissionRequester {
        return onRationale { action.call(it) }
    }

    /**
     * Callback for permission rationale scenario. Called when permission requires showing rationale.
     */
    fun onRationale(action: (requester: PermissionRequester) -> Unit): PermissionRequester {
        rationaleAction = action
        return this
    }

    /**
     * Callback for permission permanently denied scenario. Called when permission request cannot be shown
     * to the user. Need to ask user to open app settings and grant permissions manually.
     */
    fun onPermanentlyDenied(action: Callback2<PermissionRequester, Boolean>): PermissionRequester {
        return onPermanentlyDenied { requester, canShowSettingsDialog -> action.call(requester, canShowSettingsDialog) }
    }

    /**
     * Callback for permission permanently denied scenario. Called when permission request cannot be shown
     * to the user. Need to ask user to open app settings and grant permissions manually.
     */
    fun onPermanentlyDenied(action: (requester: PermissionRequester, canShowSettingsDialog: Boolean) -> Unit): PermissionRequester {
        permanentlyDeniedAction = action
        return this
    }

}