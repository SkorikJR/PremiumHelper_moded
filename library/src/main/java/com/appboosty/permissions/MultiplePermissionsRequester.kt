package com.appboosty.permissions

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.appboosty.premiumhelper.util.ActivityLifecycleCallbacksAdapter
import com.appboosty.premiumhelper.util.ActivityLifecycleListener

/**
 * A permission request helper. Handles multiple permission request and result handling.
 *
 * @param activity Permission requester activity.
 * @param permission Array of permission names
 */
class MultiplePermissionsRequester(activity: AppCompatActivity, private val permissions: Array<String>) : BasePermissionRequester(activity) {

    private var grantedAction: ((requester: MultiplePermissionsRequester) -> Unit)? = null
    private var deniedAction: ((requester: MultiplePermissionsRequester, result: Map<String, Boolean>) -> Unit)? = null
    private var rationaleAction: ((requester: MultiplePermissionsRequester, permissions: List<String>) -> Unit)? = null
    private var permanentlyDeniedAction: ((requester: MultiplePermissionsRequester, result: Map<String, Boolean>, canShowSettingsDialog: Boolean) -> Unit)? = null
    private var relaunchLifecycleListener: ActivityLifecycleListener? = null
    private val launcher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result->
            processPermissionResult(result)
        }
    private var activityClosed = false
    init{
        activity.application
        relaunchLifecycleListener = ActivityLifecycleListener(activity::class.java, object : ActivityLifecycleCallbacksAdapter() {
            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                relaunchLifecycleListener?.let{
                    activityClosed = true
                    activity.application?.unregisterActivityLifecycleCallbacks(it)
                    launcher.unregister()
                }
            }
        })
        activity.application?.registerActivityLifecycleCallbacks(relaunchLifecycleListener)
    }

    private fun processPermissionResult(result: Map<String, Boolean>) {
        if (result.values.all { it }) {
            grantedAction?.invoke(this)
        } else {
            if (PermissionUtils.shouldShowRequestPermissionRationale(
                    activity,
                    result.keys.toTypedArray()
                )
            ) {
                deniedAction?.invoke(this, result)
            } else {
                permanentlyDeniedAction?.invoke(this, result, !rationaleShown)
            }
        }
        rationaleShown = false
    }

    fun hasPermissions(): Boolean {
        permissions.onEach { permission->
            if (!PermissionUtils.hasPermission(activity, permission)) {
                return false
            }
        }

        return true
    }

    override fun request() {
        if(activityClosed || activity.isFinishing) return
        if (hasPermissions()) {
            grantedAction?.invoke(this)
        } else {
            if (PermissionUtils.shouldShowRequestPermissionRationale(activity, permissions) && !rationaleShown && rationaleAction != null) {
                rationaleShown = true
                rationaleAction?.invoke(this, permissions.filter { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) })
            } else {
                launcher.launch(permissions.filterNot {
                    PermissionUtils.hasPermission(
                        activity,
                        it
                    )
                }.toTypedArray())
            }
        }
    }

    override fun getActivityLauncher(): ActivityResultLauncher<*> {
        return launcher
    }

    /**
     * Callback for permission granted result.
     */
    fun onGranted(action: PermissionUtils.Callback<MultiplePermissionsRequester>): MultiplePermissionsRequester {
        return onGranted { action.call(it) }
    }

    /**
     * Callback for permission granted result.
     */
    fun onGranted(action: (requester: MultiplePermissionsRequester) -> Unit): MultiplePermissionsRequester {
        grantedAction = action
        return this
    }

    /**
     * Callback for permission denied result. Called when at least one permission was denied by the user.
     *
     * @param result Map of permission names and permission request result. Value is `true` if permission was granted.
     */
    fun onDenied(action: PermissionUtils.Callback2<MultiplePermissionsRequester, Map<String, Boolean>>): MultiplePermissionsRequester {
        return onDenied { requester, result -> action.call(requester, result) }
    }

    /**
     * Callback for permission denied result. Called when at least one permission was denied by the user.
     *
     * @param result Map of permission names and permission request result. Value is `true` if permission was granted.
     */
    fun onDenied(action: (requester: MultiplePermissionsRequester, result: Map<String, Boolean>) -> Unit): MultiplePermissionsRequester {
        deniedAction = action
        return this
    }

    /**
     * Callback for permission rationale scenario. Called when at least one permission requires showing rationale.
     *
     * @param result List of permissions that require rationale
     */
    fun onRationale(action: PermissionUtils.Callback2<MultiplePermissionsRequester, List<String>>): MultiplePermissionsRequester {
        return onRationale { requester, result -> action.call(requester, result) }
    }

    /**
     * Callback for permission rationale scenario. Called when at least one permission requires showing rationale.
     *
     * @param result List of permissions that require rationale
     */
    fun onRationale(action: (requester: MultiplePermissionsRequester, result: List<String>) -> Unit): MultiplePermissionsRequester {
        rationaleAction = action
        return this
    }

    /**
     * Callback for permission permanently denied scenario. Called when at least one permission request cannot be shown
     * to the user. Need to ask user to open app settings and grant permissions manually.
     */
    fun onPermanentlyDenied(action: PermissionUtils.Callback3<MultiplePermissionsRequester, Map<String, Boolean>, Boolean>): MultiplePermissionsRequester {
        return onPermanentlyDenied { requester, result, canShowSettingsDialog -> action.call(requester, result, canShowSettingsDialog) }
    }

    /**
     * Callback for permission permanently denied scenario. Called when at least one permission request cannot be shown
     * to the user. Need to ask user to open app settings and grant permissions manually.
     */
    fun onPermanentlyDenied(action: (requester: MultiplePermissionsRequester, result: Map<String, Boolean>, canShowSettingsDialog: Boolean) -> Unit): MultiplePermissionsRequester {
        permanentlyDeniedAction = action
        return this
    }

}