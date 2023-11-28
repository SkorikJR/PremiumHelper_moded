package com.appboosty.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import timber.log.Timber

object PermissionUtils {

    private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

    /**
     * Determine whether the app have been granted a particular permission.
     *
     * @param permission The name of the permission
     */
    @JvmStatic
    fun hasPermission(context: Context, permission: String): Boolean {

        if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                Timber.w("Do not request WRITE_EXTERNAL_STORAGE on Android ${Build.VERSION.SDK_INT}")
                return true
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !Environment.isExternalStorageLegacy()) {
                return true
            }
        }

        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Show permission rationale dialog with given text
     *
     * @param permissionRequester PermissionRequester used to handle permissions
     * @param titleResId String resource for dialog title
     * @param messageResId String resource for dialog message
     * @param positiveTextResId String resource for dialog button text
     */
    @JvmStatic
    fun showPermissionRationale(
        context: Context,
        permissionRequester: BasePermissionRequester,
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes positiveTextResId: Int
    ) {
        showPermissionRationale(
            context,
            permissionRequester,
            context.getString(titleResId),
            context.getString(messageResId),
            context.getString(positiveTextResId)
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
    @JvmStatic
    fun showPermissionRationale(
        context: Context,
        permissionRequester: BasePermissionRequester,
        title: String,
        message: String,
        positiveButtonText: String
    ) {
        with(AlertDialog.Builder(context)) {
            setTitle(title)
            setMessage(message)
            setPositiveButton(positiveButtonText) { dialog, _ ->
                permissionRequester.request()
                dialog.dismiss()
            }
            show()
        }
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
    @JvmStatic
    fun showOpenSettingsDialog(
        context: Context,
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes positiveTextResId: Int,
        @StringRes negativeTextResId: Int
    ) {

        showOpenSettingsDialog(
            context,
            context.getString(titleResId),
            context.getString(messageResId),
            context.getString(positiveTextResId),
            context.getString(negativeTextResId)
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
    @JvmStatic
    fun showOpenSettingsDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String
    ) {
        with(AlertDialog.Builder(context)) {
            setTitle(title)
            setMessage(message)
            setPositiveButton(positiveButtonText) { _, _ -> PremiumHelperUtils.openApplicationSettings(context) }
            setNegativeButton(negativeButtonText) { dialog, _ -> dialog.dismiss() }
            show()
        }
    }

    /**
     * Show notification settings screen
     */
    @JvmStatic
    fun showNotificationSettings(context: Context, notificationServiceClass: Class<out NotificationListenerService>) {

        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        } else {
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
        }

        val intent = Intent(action)
        val serviceClassName = "${context.packageName}/${notificationServiceClass.name}"
        intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, serviceClassName)
        intent.putExtra(
            EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundleOf(
            EXTRA_FRAGMENT_ARG_KEY to serviceClassName
        ))

        context.startActivity(intent)

        PremiumHelper.getInstance().ignoreNextAppStart()
    }

    /**
     * Show accessibility settings screen
     */
    @JvmStatic
    fun showAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
        PremiumHelper.getInstance().ignoreNextAppStart()
    }

    interface Callback<T> {
        fun call(param: T)
    }

    interface Callback2<T, R> {
        fun call(param1: T, param2: R)
    }

    interface Callback3<T, R, K> {
        fun call(param1: T, param2: R, param3: K)
    }

    /**
     * Check if any of the permissions requires showing rationale
     *
     * @param permissions Array of permission names for checking
     */
    @JvmStatic
    fun shouldShowRequestPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    /**
     * Get the list of permissions for accessing external storage depends on the android version
     * @param mediaType types of media the application needs to access
     * The possible parameters are:
     * MediaPermissionType.AUDIO - Permission to access audio files<br>
     * MediaPermissionType.VIDEO - Permission to access video files<br>
     * MediaPermissionType.IMAGES - Permission to access image files<br>
     * MediaPermissionType.ALL    - Permission to access all types of files<br>
     * @return array of string constants represents the permissions needed to access the requested types
     * of media.
     */
    @JvmStatic
    fun getReadExternalStoragePermissions(mediaType: Int): Array<String> {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else {
            mutableListOf<String>().apply {
                if (mediaType and MediaPermissionType.AUDIO != 0) add(Manifest.permission.READ_MEDIA_AUDIO)
                if (mediaType and MediaPermissionType.VIDEO != 0) add(Manifest.permission.READ_MEDIA_VIDEO)
                if (mediaType and MediaPermissionType.IMAGES != 0) add(Manifest.permission.READ_MEDIA_IMAGES)
            }.toTypedArray()
        }

    }

    object MediaPermissionType {
        const val AUDIO: Int = 0x00000001
        const val VIDEO: Int = 0x00000010
        const val IMAGES: Int = 0x00000100
        const val ALL: Int = 0x00000111
    }

}

