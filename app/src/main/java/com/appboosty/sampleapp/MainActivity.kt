package com.appboosty.sampleapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.appboosty.ads.PhAdListener
import com.appboosty.ads.PhLoadAdError
import com.appboosty.ads.PhShimmerBaseAdView
import com.appboosty.permissions.MultiplePermissionsRequester
import com.appboosty.permissions.PermissionUtils
import com.appboosty.premiumhelper.Premium
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.log.timber
import com.appboosty.premiumhelper.ui.rate.RateHelper
import com.appboosty.premiumhelper.ui.relaunch.OnRelaunchListener
import com.appboosty.premiumhelper.ui.relaunch.RelaunchResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import com.appboosty.sample.R
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), OnRelaunchListener, RateHelper.OnRateFlowCompleteListener {

    private val log by timber()

    private lateinit var permissionRequester: MultiplePermissionsRequester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        log.d("Starting activity")

        findViewById<View>(R.id.button_show_relaunch).setOnClickListener {
            PremiumHelper.getInstance().showPremiumOffering(this, "")
        }

        findViewById<View>(R.id.button_rate).setOnClickListener {
            Premium.showRateDialog(supportFragmentManager)
        }

        findViewById<View>(R.id.button_interstitial).setOnClickListener {
            Premium.Ads.showInterstitialAd(this, null)
        }

        findViewById<View>(com.appboosty.sample.R.id.button_native).setOnClickListener {
            startActivity(Intent(this, NativeAdsActivity::class.java))
        }

        findViewById<View>(com.appboosty.sample.R.id.button_native_common).setOnClickListener {
            startActivity(Intent(this, CommonNativeAdActivity::class.java))
        }

        if(PremiumHelper.getInstance().getCurrentAdsProvider() == Configuration.AdsProvider.APPLOVIN) {
            findViewById<View>(R.id.button_native_native_applovin).setOnClickListener {
                startActivity(Intent(this, AppLovinNativeActivity::class.java))
            }
        }else
            findViewById<View>(R.id.button_native_native_applovin).visibility = View.GONE
        
//
//        findViewById<View>(com.appboosty.sample.R.id.button_rewarded).setOnClickListener {
//            PremiumHelper.getInstance().showRewardedAd(this,
//                {
//                    Toast.makeText(this@MainActivity, "Reward earned!", Toast.LENGTH_SHORT).show()
//                }, null)
//        }

//        findViewById<View>(com.appboosty.sample.R.id.button_java_sample).setOnClickListener {
//            startActivity(Intent(this, SampleJavaActivity::class.java))
//        }

        findViewById<View>(R.id.button_privacy_policy).setOnClickListener {
            Premium.showPrivacyPolicy(this)
        }

        findViewById<View>(R.id.button_notification).setOnClickListener {
            showNotification()
        }

        findViewById<View>(R.id.button_email).setOnClickListener {
            Premium.Utils.sendEmail(this, "support@appboosty.com", "support.vip@appboosty.com")
        }

        findViewById<SwitchCompat>(R.id.switch_interstitial).setOnCheckedChangeListener { _, testingInterstitialFailure ->

            PremiumHelper.getInstance().adManager.clearInterstitials()

            if (testingInterstitialFailure) {
                Premium.configuration.overrideDebugValue(Configuration.AD_UNIT_ADMOB_INTERSTITIAL, "INVALID_AD_ID")
            } else {
                Premium.configuration.overrideDebugValue(Configuration.AD_UNIT_ADMOB_INTERSTITIAL, "ca-app-pub-3940256099942544/8691691433")
            }

            PremiumHelper.getInstance().adManager.loadInterstitial(this)
        }

        findViewById<View>(R.id.button_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loadRewarded()

        findViewById<PhShimmerBaseAdView>(R.id.banner_container).adLoadingListener =
            object : PhAdListener() {
                override fun onAdLoaded() {
                    log.e("Banner ad loaded")
                }

                override fun onAdFailedToLoad(error: PhLoadAdError) {
                    log.e("Banner ad failed to load!")
                }
            }

        PremiumHelperUtils.addOnMainActivityExitHandler(this)

    /*
        Example of  'smart read external storage' permission. This way the programmer don't need to take care of what SDK is used.
        The only thing is necessary is to choose the media types the host application is going to access.
        Available types are: AUDIO, VIDEO, IMAGES, ALL
    */
        val storagePermissions = PermissionUtils.getReadExternalStoragePermissions(
            PermissionUtils.MediaPermissionType.VIDEO or
                PermissionUtils.MediaPermissionType.IMAGES)

//        val permissionRequester2 =
//            MultiplePermissionsRequester(application, this, storagePermissions)
//
//                .onGranted {
//                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()            }
//                .onDenied { _, result ->
//                    Toast.makeText(this, "Permissions ${result.entries.filterNot { it.value }.map { it.key }.toList()} denied", Toast.LENGTH_SHORT).show()
//                }
//                .onRationale { requester, result ->
//                    requester.showRationale(
//                        "Permission needed",
//                        "This application needs permissions to work correctly", "Ok"
//                    )
//                }
//                .onPermanentlyDenied { requester, result, canShowSettingsDialog ->
//                    if (canShowSettingsDialog) {
//                        requester.showOpenSettingsDialog(
//                            "Permission needed",
//                            "This application needs permissions to work correctly",
//                            "Go to settings",
//                            "Later"
//                        )
//                    }
//                }

        permissionRequester =
            MultiplePermissionsRequester(this, arrayOf(
                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA))

                .onGranted {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()            }
                .onDenied { _, result ->
                    Toast.makeText(this, "Permissions ${result.entries.filterNot { it.value }.map { it.key }.toList()} denied", Toast.LENGTH_SHORT).show()
                }
                .onRationale { requester, result ->
                    requester.showRationale(
                        "Permission needed",
                        "This application needs permissions to work correctly", "Ok"
                    )
                }
                .onPermanentlyDenied { requester, result, canShowSettingsDialog ->
                    if (canShowSettingsDialog) {
                        requester.showOpenSettingsDialog(
                            "Permission needed",
                            "This application needs permissions to work correctly",
                            "Go to settings",
                            "Later"
                        )
                    }
                }
    }

    private fun loadRewarded() {
        lifecycleScope.launch {
            Premium.Ads.loadRewardedAd(this@MainActivity, object : PhAdListener() {
                override fun onAdFailedToLoad(error: PhLoadAdError) {
                    log.e("Failed to load rewarded ad")
                }
                override fun onAdLoaded() {
                    log.i("Rewarded ad loaded")
                }
            })
        }
    }

    override fun onRelaunchComplete(result: RelaunchResult) {
        log.d("ON RELAUNCH COMPLETE (result = $result)")
        permissionRequester.request()
    }

    override fun onRateFlowComplete(reviewUiShown: RateHelper.RateUi, negativeIntent: Boolean) {
        log.d("On Rate Complete (reviewUiShown: $reviewUiShown negativeIntent: $negativeIntent)")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Premium.onActivityNewIntent(this, intent)
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_id)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("PREMIUM_HELPER_CHANNEL", name, importance)
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(PremiumHelper.FLAG_FROM_NOTIFICATION, true)
            putExtra(PremiumHelper.FLAG_SHOW_RELAUNCH, false)
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, "PREMIUM_HELPER_CHANNEL")
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Premium-Helper")
            .setContentText("Tap to open main activity")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(555, builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "Premium-Helper"
            val description = "Premium-Helper channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("PREMIUM_HELPER_CHANNEL", name, importance)
            channel.description = description
            val notificationManager: NotificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

}