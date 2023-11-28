package com.appboosty.premiumhelper

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.gms.ads.nativead.NativeAd
import com.appboosty.ads.PhAdListener
import com.appboosty.ads.PhFullScreenContentCallback
import com.appboosty.ads.PhOnUserEarnedRewardListener
import com.appboosty.ads.config.PHAdSize
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.configuration.appconfig.PremiumHelperConfiguration
import com.appboosty.premiumhelper.ui.rate.RateHelper
import com.appboosty.premiumhelper.util.ContactSupport
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import io.reactivex.Single

@Suppress("unused")
object Premium {

    @JvmStatic
    val configuration: Configuration
        get() = PremiumHelper.getInstance().configuration

    @JvmStatic
    val preferences: Preferences
        get() = PremiumHelper.getInstance().preferences

    @JvmStatic
    val analytics: Analytics
        get() = PremiumHelper.getInstance().analytics

    @JvmStatic
    fun initialize(application: Application, appConfiguration: PremiumHelperConfiguration) {
        PremiumHelper.initialize(application, appConfiguration)
    }

    @JvmStatic
    fun hasActivePurchase()= PremiumHelper.getInstance().hasActivePurchase()

    @JvmStatic
    @JvmOverloads
    fun showPremiumOffering(activity: Activity, source: String, theme: Int = -1) {
        PremiumHelper.getInstance().showPremiumOffering(activity, source, theme)
    }

    @JvmStatic
    @JvmOverloads
    fun showPremiumOfferingNewTask(source: String, theme: Int = -1, flags: Int = -1) {
        PremiumHelper.getInstance().showPremiumOffering(source, theme, flags)
    }

    @JvmStatic
    @JvmOverloads
    fun onHappyMoment(activity: AppCompatActivity, theme: Int = -1, delay: Int = 0, callback: (() -> Unit)? = null) {
        PremiumHelper.getInstance().onHappyMoment(activity, theme, delay, callback)
    }

    @JvmStatic
    @JvmOverloads
    fun showHappyMomentOnNextActivity(activity: AppCompatActivity, delay: Int = 0) {
        PremiumHelper.getInstance().showHappyMomentOnNextActivity(activity, delay)
    }

    @JvmStatic
    @JvmOverloads
    fun showRateDialog(fm: FragmentManager, theme: Int = -1, completeListener: RateHelper.OnRateFlowCompleteListener? = null) {
        PremiumHelper.getInstance().showRateDialog(fm, theme, completeListener)
    }

    @JvmStatic
    @JvmOverloads
    fun showInAppReview(activity: AppCompatActivity, completeListener: RateHelper.OnRateFlowCompleteListener? = null) {
        PremiumHelper.getInstance().showInAppReview(activity, completeListener)
    }

    @JvmStatic
    fun showPrivacyPolicy(activity: Activity) {
        PremiumHelper.getInstance().showPrivacyPolicy(activity)
    }

    @JvmStatic
    fun showTermsAndConditions(activity: Activity) {
        PremiumHelper.getInstance().showTermsAndConditions(activity)
    }

    @JvmStatic
    @JvmOverloads
    fun setIntroComplete(value: Boolean = true) = PremiumHelper.getInstance().setIntroComplete(value)

    @JvmStatic
    fun ignoreNextAppStart() {
        PremiumHelper.getInstance().ignoreNextAppStart()
    }

    @JvmStatic
    fun onActivityNewIntent(activity: Activity, newIntent: Intent?) {
        PremiumHelper.onActivityNewIntent(activity, newIntent)
    }

    @JvmStatic
    fun onMainActivityBackPressed(activity: Activity): Boolean {
        return PremiumHelper.getInstance().onMainActivityBackPressed(activity)
    }

    object Debug {
        @JvmStatic
        fun addMainOffer(sku: String, price: String) {
            PremiumHelper.getInstance().addDebugMainOffer(sku, price)
        }

        @JvmStatic
        fun addOneTimeOffer(sku: String, price: String, strike_sku: String, strike_price: String) {
            PremiumHelper.getInstance().addDebugOneTimeOffer(sku, price, strike_sku, strike_price)
        }
    }

    object Ads {

        @JvmStatic
        suspend fun loadBanner(bannerSize: PHAdSize): View? {
            return PremiumHelper.getInstance().loadBanner(bannerSize)
        }

        @JvmStatic
        fun isInterstitialLoaded() = PremiumHelper.getInstance().isInterstitialLoaded()

        @JvmStatic
        @JvmOverloads
        fun showInterstitialAd(activity: Activity, callback: PhFullScreenContentCallback? = null) {
            PremiumHelper.getInstance().showInterstitialAd(activity, callback)
        }

        @JvmStatic
        @JvmOverloads
        fun showInterstitialAdWithoutCapping(activity: Activity, callback: PhFullScreenContentCallback? = null) {
            PremiumHelper.getInstance().showInterstitialAdWithoutCapping(activity, callback)
        }

        @JvmStatic
        fun showInterstitialAdOnNextActivity(activity: Activity) {
            PremiumHelper.getInstance().showInterstitialAdOnNextActivity(activity)
        }

        @JvmStatic
        @JvmOverloads
        fun loadRewardedAd(activity: Activity, listener: PhAdListener? = null) {
            PremiumHelper.getInstance().loadRewardedAd(activity, listener)
        }

        @JvmStatic
        fun showRewardedAd(activity: Activity, rewardedAdCallback: PhOnUserEarnedRewardListener, fullScreenContentCallback: PhFullScreenContentCallback?) {
            PremiumHelper.getInstance()
                .showRewardedAd(activity, rewardedAdCallback, fullScreenContentCallback)
        }

        object Rx {

            @JvmStatic
            fun loadBanner(bannerSize: PHAdSize): Single<PHResult<View>> {
                return PremiumHelper.getInstance().loadBannerRx(bannerSize)
            }

            @JvmStatic
            @JvmOverloads
            fun loadNativeAd(count: Int = 1): Single<PHResult<Unit>> {
                return PremiumHelper.getInstance().loadNativeAdmobAdRx(count)
            }

            @JvmStatic
            fun loadAndGetNativeAd(): Single<PHResult<NativeAd>> {
                return PremiumHelper.getInstance().loadAndGetNativeAdmobAdRx()
            }

        }
    }

    object Utils {

        @JvmStatic
        @JvmOverloads
        fun sendEmail(activity: Activity, email: String, emailVip: String? = null) {
            ContactSupport.sendEmail(activity, email, emailVip)
        }

        @JvmStatic
        fun openUrl(context: Context, url: String) {
            PremiumHelperUtils.openUrl(context, url)
        }

        @JvmStatic
        fun openGooglePlay(context: Context) {
            PremiumHelperUtils.openGooglePlay(context)
        }

        @JvmStatic
        fun openApplicationSettings(context: Context) {
            PremiumHelperUtils.openApplicationSettings(context)
        }

        @JvmStatic
        fun shareMyApp(context: Context) {
            PremiumHelperUtils.shareMyApp(context)
        }

        @JvmStatic
        fun setDayMode() {
            PremiumHelperUtils.setDayMode()
        }

        @JvmStatic
        fun setNightMode() {
            PremiumHelperUtils.setNightMode()
        }

        @JvmStatic
        fun createChooser(context: Context, intent: Intent, titleId: Int) {
            PremiumHelperUtils.createChooser(context, intent, titleId)
        }

        @JvmStatic
        fun createChooser(context: Context, intent: Intent, title: String) {
            PremiumHelperUtils.createChooser(context, intent, title)
        }
    }


}