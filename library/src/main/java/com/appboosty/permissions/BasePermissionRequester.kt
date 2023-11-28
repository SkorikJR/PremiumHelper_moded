package com.appboosty.permissions

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

abstract class BasePermissionRequester(protected val activity: AppCompatActivity) : DefaultLifecycleObserver {

    /**
     * Start permissions request flow
     */
    abstract fun request()

    protected abstract fun getActivityLauncher(): ActivityResultLauncher<*>

    protected var rationaleShown = false

    init {
        activity.lifecycle.addObserver(this)
    }

    /**
     * Show permission rationale dialog with given text
     *
     * @param permissionRequester PermissionRequester used to handle permissions
     * @param titleResId String resource for dialog title
     * @param messageResId String resource for dialog message
     * @param positiveTextResId String resource for dialog button text
     */
    fun showRationale(@StringRes titleResId: Int, @StringRes messageResId: Int, @StringRes positiveTextResId: Int) {
        PermissionUtils.showPermissionRationale(
            activity,
            this,
            titleResId,
            messageResId,
            positiveTextResId
        )
    }

    /**
     * Show permission rationale dialog with given text
     *
     * @param permissionRequester PermissionRequester used to handle permissions
     * @param title String for dialog title
     * @param message String for dialog message
     * @param positiveButtonText String for dialog button text
     */
    fun showRationale(title: String, message: String, positiveButtonText: String) {
        PermissionUtils.showPermissionRationale(activity, this, title, message, positiveButtonText)
    }

    /**
     * Show dialog offering to open application settings page to grant permissions
     * which were permanently denied by the user.
     *
     * @param titleResId String resource for dialog title
     * @param messageResId String resource for dialog message
     * @param positiveTextResId String resource for dialog button text
     * @param negativeTextResId String resource for dialog button text
     */
    fun showOpenSettingsDialog(@StringRes titleResId: Int, @StringRes messageResId: Int, @StringRes positiveTextResId: Int, @StringRes negativeButtonResId: Int) {
        PermissionUtils.showOpenSettingsDialog(
            activity,
            titleResId,
            messageResId,
            positiveTextResId,
            negativeButtonResId
        )
    }

    /**
     * Show dialog offering to open application settings page to grant permissions
     * which were permanently denied by the user.
     *
     * @param title String for dialog title
     * @param message String for dialog message
     * @param positiveButtonText String for dialog button text
     * @param negativeButtonText String for dialog button text
     */
    fun showOpenSettingsDialog(title: String, message: String, positiveButtonText: String, negativeButtonText: String) {
        PermissionUtils.showOpenSettingsDialog(
            activity,
            title,
            message,
            positiveButtonText,
            negativeButtonText
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        getActivityLauncher().unregister()
        owner.lifecycle.removeObserver(this)
    }

}