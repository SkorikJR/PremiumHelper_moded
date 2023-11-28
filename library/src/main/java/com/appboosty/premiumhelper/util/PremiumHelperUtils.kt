package com.appboosty.premiumhelper.util

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.appboosty.premiumhelper.Offer
import com.appboosty.premiumhelper.Preferences
import com.appboosty.premiumhelper.Premium
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.configuration.Configuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.*
import timber.log.Timber
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.MessageFormat
import java.util.*
import kotlin.math.roundToInt


object PremiumHelperUtils {

    enum class SubscriptionPeriod {
        NONE, YEARLY, MONTHLY, WEEKLY
    }

    enum class FreeTrialPeriod {
        NONE, THREE_DAYS, SEVEN_DAYS, THIRTY_DAYS
    }

    @JvmStatic
    fun getInstalledDate(context: Context): Long {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: Throwable) {
            System.currentTimeMillis()
        }
    }

    internal fun isInstalledFromUpdate(context: Context): Boolean {
        return try {
            with(context.packageManager.getPackageInfo(context.packageName, 0)) {
                firstInstallTime != lastUpdateTime
            }
        } catch (e: Throwable) {
            false
        }
    }

    @Suppress("DEPRECATION")
    internal fun isFirstStartAfterUpdate(context: Context, preferences: Preferences): Boolean {
        val storageKey = "last_installed_version"
        with(context.packageManager.getPackageInfo(context.packageName, 0)) {
            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                this.longVersionCode
            } else {
                this.versionCode.toLong()
            }

            val lastInstalledVersion = preferences.getLong(storageKey, -1)
            return if (lastInstalledVersion != currentVersionCode) {
                preferences.putLong(storageKey, currentVersionCode)
                lastInstalledVersion != -1L
            } else false
        }
    }

    internal fun reportError(e: Exception) {

        Timber.tag(PremiumHelper.TAG).e(e)

        val crashlyticsEnabled = try {
            Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        if (crashlyticsEnabled) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

    }

    @JvmStatic
    fun getApplicationIcon(context: Context): Int {
        return context.applicationInfo.icon
    }

    @JvmStatic
    fun getApplicationName(context: Context): String {
        return try {
            if (context.applicationInfo.labelRes == 0) {
                context.applicationInfo.nonLocalizedLabel.toString()
            } else {
                context.getString(context.applicationInfo.labelRes)
            }
        } catch (e: Exception) {
            ""
        }
    }

    @JvmStatic
    fun getVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Throwable) {
            ""
        }
    }

    @JvmStatic
    fun isOnMainProcess(context: Context): Boolean {
        val processName = getProcessName(context)
        return processName.isNullOrEmpty() || processName == context.packageName
    }

    @JvmStatic
    fun getProcessName(context: Context): String? {
        runCatching {
            val pid = Process.myPid()
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = manager.runningAppProcesses
            if (processes != null) {
                for (processInfo in processes) {
                    if (processInfo.pid == pid) {
                        return processInfo.processName
                    }
                }
            }
        }

        return null
    }

    @JvmStatic
    fun getDaysSincePurchase(purchase_date: Long): Int {
        val zoneId = DateTimeUtils.toZoneId(TimeZone.getDefault())
        val purchaseDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(purchase_date), zoneId).toLocalDate()
        return Period.between(purchaseDate, LocalDate.now()).days
    }

    @JvmStatic
    fun getDaysSinceInstall(context: Context): Int {
        val diff = System.currentTimeMillis() - getInstalledDate(context)
        return (diff / DateUtils.DAY_IN_MILLIS).toInt()
    }

    internal fun buildSkuDetails(sku: String, skuType: String, price: String): SkuDetails {
        val skuJson = "{\n" +
                "\"title\":\"Debug offer\",\n" +
                "\"price\":\"$price\",\n" +
                "\"type\":\"$skuType\",\n" +
                "\"subscriptionPeriod\":\"P1Y\",\n" +
                "\"freeTrialPeriod\":\"P1W\",\n" +
                "\"description\":\"debug-offer\",\n" +
                "\"price_amount_micros\":890000,\n" +
                "\"price_currency_code\":\"USD\",\n" +
                "\"productId\":\"$sku\"\n" +
                "}"

        return SkuDetails(skuJson)
    }

    internal fun buildDebugOffer(sku: String, price: String): Offer {
        val skuDetails = buildSkuDetails(sku, BillingClient.SkuType.SUBS, price)
        return Offer(sku, BillingClient.SkuType.SUBS, skuDetails)
    }

    internal fun buildDebugPurchase(context: Context, sku: String): Purchase {

        val purchaseJson = "{\n" +
                "\"orderId\":\"DEBUG.OFFER.${UUID.randomUUID()}\",\n" +
                "\"packageName\":\"${context.packageName}\",\n" +
                "\"productId\":\"$sku\",\n" +
                "\"purchaseTime\":${System.currentTimeMillis()},\n" +
                "\"purchaseState\":0,\n" +
                "\"purchaseToken\":\"debugtoken.${UUID.randomUUID()}\",\n" +
                "\"obfuscatedAccountId\":\"debugaccount.${UUID.randomUUID()}\",\n" +
                "\"acknowledged\":true,\n" +
                "\"autoRenewing\":true\n" +
                "}"

        return Purchase(purchaseJson, UUID.randomUUID().toString())
    }

    internal fun validateLayout(context: Context, activityName: String, layoutResId: Int, childIds: List<Int>) {

        val view = LayoutInflater.from(context).inflate(layoutResId, null)

        childIds.forEach {
            if (view.findViewById<View>(it) == null) {
                error("LAYOUT ERROR: $activityName: ${context.resources.getResourceEntryName(it)} not found")
            }
        }
    }

    internal fun formatSkuPrice(@NonNull context: Context, @NonNull skuDetails: SkuDetails?): String {

        if (skuDetails == null || skuDetails.price.isEmpty()) {
            return ""
        }

        val priceString = with(context.resources) {

            val freeTrialPeriod = skuDetails.freeTrialPeriod()

            when (skuDetails.subscriptionPeriod()) {
                SubscriptionPeriod.WEEKLY -> getStringArray(R.array.sku_weekly_prices)[freeTrialPeriod.ordinal]
                SubscriptionPeriod.MONTHLY -> getStringArray(R.array.sku_monthly_prices)[freeTrialPeriod.ordinal]
                SubscriptionPeriod.YEARLY -> getStringArray(R.array.sku_yearly_prices)[freeTrialPeriod.ordinal]
                SubscriptionPeriod.NONE -> getString(R.string.sku_price_onetime)
            }
        }

        return MessageFormat.format(priceString, skuDetails.price)
    }

    internal fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    internal fun hasInternetConnection(context: Context): Boolean {
        return with(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager) {
            activeNetworkInfo?.isConnected ?: false
        }
    }

    @JvmStatic
    fun openUrl(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
            PremiumHelper.getInstance().ignoreNextAppStart()
        }.onFailure { Timber.e(it) }
    }

    internal fun getCtaButtonText(context: Context, offer: Offer) : String {

        return if (offer.skuDetails != null) {

            val configuration = PremiumHelper.getInstance().configuration
            val freeTrialPeriod = offer.skuDetails.freeTrialPeriod()

            if (freeTrialPeriod == FreeTrialPeriod.NONE) {
                // Use custom NO TRIAL title if defined in configuration or 'START PREMIUM'
                context.getString(configuration.appConfig.startLikeProTextNoTrial ?: R.string.ph_start_premium_cta)
            } else {
                when {
                    // Use custom TRIAL title if defined in configuration
                    configuration.appConfig.startLikeProTextTrial != null -> context.getString(configuration.appConfig.startLikeProTextTrial)

                    // Show trial on CTA if enabled
                    configuration.get(Configuration.SHOW_TRIAL_ON_CTA) -> context.resources.getStringArray(R.array.cta_titles)[freeTrialPeriod.ordinal]

                    // Show default 'START FREE TRIAL' title
                    else -> context.getString(R.string.ph_start_trial_cta)
                }
            }
        } else {
            context.getString(R.string.ph_start_trial_cta)
        }

    }

    /**
     *  Get subscription period from sku name
     */
    private fun SkuDetails.subscriptionPeriod(): SubscriptionPeriod {
        return when {
            this.sku.endsWith("_onetime") -> SubscriptionPeriod.NONE
            this.sku.endsWith("_weekly") -> SubscriptionPeriod.WEEKLY
            this.sku.endsWith("_monthly") -> SubscriptionPeriod.MONTHLY
            this.sku.endsWith("_yearly") -> SubscriptionPeriod.YEARLY
            else -> SubscriptionPeriod.NONE
        }
    }

    /**
     *  Get free trial period from sku name
     */
    private fun SkuDetails.freeTrialPeriod(): FreeTrialPeriod {
        return when {
            this.sku.contains("trial_0d") -> FreeTrialPeriod.NONE
            this.sku.contains("trial_3d") -> FreeTrialPeriod.THREE_DAYS
            this.sku.contains("trial_7d") -> FreeTrialPeriod.SEVEN_DAYS
            this.sku.contains("trial_30d") -> FreeTrialPeriod.THIRTY_DAYS
            else -> FreeTrialPeriod.NONE
        }
    }

    internal fun openGooglePlay(activity: Activity, delay: Boolean) {
        if (activity is LifecycleOwner) {
            activity.lifecycleScope.launch {
                if (delay) {
                    delay(500)
                }
                openGooglePlay(activity)
            }
        } else {
            openGooglePlay(activity)
        }
    }

    @JvmStatic
    fun openGooglePlay(context: Context) {
        try {
            context.startActivity(rateIntentForUrl("market://details", context.packageName))
            PremiumHelper.getInstance().ignoreNextAppStart()
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(rateIntentForUrl("https://play.google.com/store/apps/details", context.packageName))
                PremiumHelper.getInstance().ignoreNextAppStart()
            } catch (e: Throwable) {
                Timber.tag(PremiumHelper.TAG).e(e, "Failed to open google play")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun rateIntentForUrl(url: String, packageName: String): Intent {

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, packageName)))

        var flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK

        flags = if (Build.VERSION.SDK_INT >= 21) {
            flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        } else {
            flags or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
        }

        intent.addFlags(flags)

        return intent
    }
    suspend fun <T> withRetry(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> PHResult<T>,
    ): PHResult<T> {

        var currentDelay = initialDelay

        repeat(times) {
            try {

                val result = block()

                if (result.isSuccess)
                    return result

            } catch (e: Exception) {
                Timber.e(e)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }

        return block()

    }

    @JvmStatic
    fun openApplicationSettings(context: Context) {
        kotlin.runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
            PremiumHelper.getInstance().ignoreNextAppStart()
        }
    }

    @JvmStatic
    fun shareMyApp(context: Context) {

        val intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${context.packageName}&referrer=utm_source%3Dshare_my_app")
            type = "text/plain"
        }, null)

        context.startActivity(intent)

        PremiumHelper.getInstance().ignoreNextAppStart()
    }

    //TODO Remove this method after setting min SDK version to 21
    internal fun installGooglePlayServicesProvider(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            kotlin.runCatching {
                ProviderInstaller.installIfNeeded(context)
            }.onFailure {
                Timber.tag("PlayServicesProvider").e(it)
            }
        }
    }

    internal fun isPremiumPackageInstalled(context: Context, packageNames: String): Boolean {
        return if (packageNames.isEmpty()) {
            false
        } else {
            isPackageInstalled(context, packageNames.split(","))
        }
    }

    @JvmStatic
    fun isPackageInstalled(context: Context, packageNames: List<String>): Boolean {
        return packageNames.any { isPackageInstalled(context, it) }
    }

    @JvmStatic
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return getPackageInfo(context, packageName) != null
    }

    fun getPackageSignature(context: Context, packageName: String): Signature? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = getPackageInfo(context, packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            packageInfo?.signingInfo?.apkContentsSigners?.first()
        } else {
            val packageInfo = getPackageInfo(context, packageName, PackageManager.GET_SIGNATURES)
            packageInfo?.signatures?.get(0)
        }
    }

    private fun getPackageInfo(context: Context, packageName: String, flags: Int = 0): PackageInfo? {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(packageName.trim(), flags)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getScreenHeightDp(context: Context, orientation: Int): Int {

        val displayMetrics = context.resources.displayMetrics
        val configuration = context.resources.configuration

        return if (orientation == 0 || configuration.orientation == orientation) {
            (displayMetrics.heightPixels / displayMetrics.density).roundToInt()
        } else {
            (displayMetrics.widthPixels / displayMetrics.density).roundToInt()
        }
    }

    fun getScreenWidthDp(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return (displayMetrics.widthPixels / displayMetrics.density).roundToInt()
    }

    fun setNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun setDayMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun errorOrCrash(message: String) {
        if (PremiumHelper.getInstance().isDebugMode()) {
            error(message)
        } else {
            Timber.e(message)
        }
    }

    fun Activity.doIfCompat(action: (activity: AppCompatActivity) -> Unit) {
        if (this is AppCompatActivity) {
            action(this)
        } else {
            errorOrCrash("Please use AppCompatActivity for ${javaClass.name}")
        }
    }

    fun sha1(string: String): String? {
        return try {
            val digest: MessageDigest = MessageDigest.getInstance("SHA-1")
            digest.update(string.toByteArray(StandardCharsets.UTF_8))
            BigInteger(1, digest.digest()).toString(16)
        } catch (e: NoSuchAlgorithmException) {
            Timber.w(e)
            null
        }
    }

    fun createChooser(context: Context, intent: Intent, titleId: Int) {
        createChooser(context, intent, context.getString(titleId))
    }

    fun createChooser(context: Context, intent: Intent, title: String) {
        if (Build.VERSION.SDK_INT >= 22) {
            val receiver = Intent(context, ChooserReceiver::class.java)
            val flag = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getBroadcast(context, 0, receiver, flag)
            context.startActivity(Intent.createChooser(intent, title, pendingIntent.intentSender))
        } else {
            PremiumHelper.getInstance().ignoreNextAppStart()
            context.startActivity(Intent.createChooser(intent, title))
        }
    }

    @JvmStatic
    fun addOnMainActivityExitHandler(activity: AppCompatActivity) {
        activity.onBackPressedDispatcher.addCallback(
            activity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (Premium.onMainActivityBackPressed(activity)) {
                        activity.finishAffinity()
                    }
                }
            })
    }
}