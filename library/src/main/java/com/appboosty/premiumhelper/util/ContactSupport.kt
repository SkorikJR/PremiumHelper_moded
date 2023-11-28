package com.appboosty.premiumhelper.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.appboosty.premiumhelper.BuildConfig
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.ui.support.ContactSupportActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object ContactSupport {

    @JvmStatic
    fun sendEmail(activity: Activity, email: String, emailVip: String? = null) {
        if (PremiumHelper.getInstance().configuration.get(Configuration.SHOW_CONTACT_SUPPORT_DIALOG)) {
            ContactSupportActivity.show(activity, email, emailVip)
        } else {
            openEmailApp(activity, email, emailVip)
        }
    }

    @JvmStatic
    fun openEmailApp(activity: Activity, email: String, emailVip: String? = null, message: String? = null) {
        if(activity !is LifecycleOwner) return
        (activity as LifecycleOwner).lifecycleScope.launch {

            val appName = PremiumHelperUtils.getApplicationName(activity)
            val appVersion = PremiumHelperUtils.getVersionName(activity)

            val subject = "I have an issue with $appName $appVersion"
            val emailApps = getEmailIntents(activity)
            val attachment = getUriForFile(activity, prepareAttachment(activity))

            val intent = if (emailApps.isNotEmpty()) {
                getEmailIntent(emailApps, getEmail(email, emailVip), subject, message, attachment)
            } else {
                getEmailIntent(getEmail(email, getEmail(email, emailVip)), subject, message, attachment)
            }

            withContext(Dispatchers.Main) {
                try {
                    activity.startActivity(intent)
                    PremiumHelper.getInstance().ignoreNextAppStart()
                } catch (e: ActivityNotFoundException) {
                    showDefaultShareDialog(activity, attachment)
                }
            }
        }
    }

    private fun getEmail(email: String, emailVip: String?): String {
        return if (emailVip != null && PremiumHelper.getInstance().hasActivePurchase()) {
            emailVip
        } else {
            email
        }

    }

    private fun showDefaultShareDialog(context: Context, attachment: Uri) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            setDataAndType(attachment, "application/zip")
        }

        try {
            context.startActivity(intent)
            PremiumHelper.getInstance().ignoreNextAppStart()
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
        }
    }

    private fun getEmailIntent(to: String, subject: String, message: String?, attachment: Uri?): Intent {

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.type = "vnd.android.cursor.dir/email"
//        emailIntent.type = "text/html"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        message?.let { emailIntent.putExtra(Intent.EXTRA_TEXT, message) }

        if (attachment != null) {
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            emailIntent.putExtra(Intent.EXTRA_STREAM, attachment)
        }

        return emailIntent
    }

    private fun getEmailIntent(emailAppsInfo: List<ResolveInfo>, to: String, subject: String, message: String?, attachment: Uri?): Intent? {
        val targetedShareIntents: MutableList<Intent> = ArrayList()
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "vnd.android.cursor.dir/email"
//        shareIntent.type = "text/html"
        for (resolveInfo in emailAppsInfo) {
            val packageName = resolveInfo.activityInfo.packageName
            val targetedShareIntent = Intent(Intent.ACTION_SEND)
            targetedShareIntent.type = "vnd.android.cursor.dir/email"
            targetedShareIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            targetedShareIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            message?.let { targetedShareIntent.putExtra(Intent.EXTRA_TEXT, message) }
            targetedShareIntent.setPackage(packageName)

            if (attachment != null) {
                targetedShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                targetedShareIntent.putExtra(Intent.EXTRA_STREAM, attachment)
            }

            targetedShareIntent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
            targetedShareIntents.add(targetedShareIntent)
        }

        val chooserIntent = Intent.createChooser(targetedShareIntents.removeAt(0), "")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toTypedArray())

        return chooserIntent
    }

    private fun getEmailIntents(context: Context): List<ResolveInfo> {

        val actionSendToApps: List<ResolveInfo>? = getActionSendToApps(context)
        val actionSendApps: List<ResolveInfo>? = getActionSendApps(context)

        return if (!actionSendToApps.isNullOrEmpty() && !actionSendApps.isNullOrEmpty()) {
            mergeResolveInfo(actionSendToApps, actionSendApps)
        } else if (!actionSendApps.isNullOrEmpty()) {
            actionSendApps
        } else if (!actionSendToApps.isNullOrEmpty()) {
            actionSendToApps
        } else {
            emptyList()
        }
    }

    private fun mergeResolveInfo(actionSendToApps: List<ResolveInfo>, actionSendApps: List<ResolveInfo>): List<ResolveInfo> {
        val resolveInfos: MutableList<ResolveInfo> = ArrayList()
        for (actionSendToApp in actionSendToApps) {
            if (isResolveInfoContained(actionSendToApp, actionSendApps)) {
                resolveInfos.add(actionSendToApp)
            }
        }
        return resolveInfos
    }

    private fun isResolveInfoContained(resolveInfoToBeChecked: ResolveInfo, list: List<ResolveInfo>): Boolean {
        for (resolveInfo in list) {
            if (resolveInfoToBeChecked.activityInfo.packageName == resolveInfo.activityInfo.packageName) {
                return true
            }
        }
        return false
    }

    private fun getActionSendToApps(context: Context): List<ResolveInfo>? {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:test@gmail.com") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, "test@gmail.com")
        intent.putExtra(Intent.EXTRA_SUBJECT, "this is a test")
        return try {
            context.packageManager.queryIntentActivities(intent, 0)
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private fun getActionSendApps(context: Context): List<ResolveInfo>? {
        return try {
            context.packageManager.queryIntentActivities(getEmailIntent("test@gmail.com", "Test", null, null), 0)
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private fun getUriForFile(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.com.appboosty.premiumhelper.share", file)
        } else {
            Uri.fromFile(file)
        }
    }

    private suspend fun prepareAttachment(context: Context): File {

        val files = ArrayList<String>()
        files.add(createInfoFile(context).absolutePath)
        context.filesDir.list { _, name -> name.endsWith(".log") }?.onEach { files.add("${context.filesDir}/$it") }

        val toFile = "${context.filesDir}/info.zip"

        Zip.zipFiles(toFile, files)

        return File(toFile)
    }

    private suspend fun createInfoFile(context: Context): File = withContext(Dispatchers.IO) {

        val appName = PremiumHelperUtils.getApplicationName(context)
        val appVersion = PremiumHelperUtils.getVersionName(context)

        val versionFile = File(context.filesDir, "app.txt")

        BufferedWriter(FileWriter(versionFile)).use { out ->

            with(out) {
                write("${Build.MANUFACTURER} ${Build.MODEL}")
                newLine()
                write("Android " + Build.VERSION.RELEASE)
                newLine()
                write("Application: $appName $appVersion")
                newLine()
                write("PremiumHelper ${BuildConfig.VERSION_NAME}")

                if (PremiumHelper.getInstance().isDebugMode()) {
                    newLine()
                    newLine()
                    write("Configuration:")
                    newLine()
                    write(PremiumHelper.getInstance().configuration.allValuesToString())
                    newLine()
                    newLine()
                    write("Preferences:")
                    newLine()
                    write(PremiumHelper.getInstance().preferences.allPreferencesToString())
                }

                newLine()
                newLine()

                write("Purchase: ${PremiumHelper.getInstance().preferences.getActivePurchaseInfo()?.toString() ?: "Not purchased"}")
                flush()
            }

        }

        versionFile
    }
}