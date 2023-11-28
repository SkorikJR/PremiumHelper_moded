package com.appboosty.premiumhelper.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.lifecycleScope
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.performance.StartupPerformanceTracker
import com.appboosty.premiumhelper.ui.startlikepro.StartLikeProActivity
import com.appboosty.premiumhelper.update.UpdateManager
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import com.appboosty.premiumhelper.util.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

open class PHSplashActivity : AppCompatActivity() {

    companion object {
        const val FLAG_FROM_SPLASH = "from_splash"
    }

    private lateinit var premiumHelper: PremiumHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupPerformanceTracker.getInstance().onSplashScreenCreated()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.ph_activity_splash)

        val splashLogo: ImageView? = findViewById(R.id.ph_splash_logo_image)
        val splashTitle: TextView? = findViewById(R.id.ph_splash_title_text)
        val splashProgress: ProgressBar? = findViewById(R.id.ph_splash_progress)
        val root: View? = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)

        val attrs = obtainStyledAttributes(R.styleable.Splash)
        val titleColor = attrs.getColorStateList(R.styleable.Splash_ph_splash_title_color)
        val backgroundColor = attrs.getColorStateList(R.styleable.Splash_ph_splash_background_color)
        attrs.recycle()

        root?.let {
            backgroundColor?.let {
                root.setBackgroundColor(it.defaultColor)
            }
        }

        splashLogo?.setImageResource(PremiumHelperUtils.getApplicationIcon(applicationContext))
        splashTitle?.text = PremiumHelperUtils.getApplicationName(applicationContext)

        titleColor?.let {
            splashTitle?.setTextColor(it)
        }

        val fadeInAnimation = AlphaAnimation(0f, 1f)
        fadeInAnimation.duration = 700
        splashLogo?.startAnimation(fadeInAnimation)
        splashTitle?.startAnimation(fadeInAnimation)

        if (splashProgress != null) {

            runCatching {
                setProgressColor(splashProgress)
            }.onFailure { Timber.e(it) }

            splashProgress.alpha = 0f
            splashProgress.visibility = View.VISIBLE
            splashProgress.animate()
                .apply {
                    alpha(1f)
                    duration = 300
                    startDelay = 5000
                    start()
                }
        }

        premiumHelper = PremiumHelper.getInstance()

        lifecycleScope.launchWhenCreated {
            PremiumHelper.getInstance().adManager.prepareConsentInfo(this@PHSplashActivity, onConsentFormRequired = {
                startFadeAnimation()
            })
            onPremiumHelperInitialized(waitForInitComplete())
        }
    }

    private fun startFadeAnimation() {
        val shaderView = findViewById<View?>(R.id.screen_shader)
        shaderView?.let {
            it.animate().alpha(1f).setDuration(500L).withEndAction {
                CoroutineScope(Dispatchers.Main).launch {
                    PremiumHelper.getInstance().adManager.askForConsentIfRequired(this@PHSplashActivity) {
                        Timber.tag("PhConsentManager").d("PHSplashActivity.onCreate()-> consent done")
                    }
                }
            }.start()
        }?: run{
            CoroutineScope(Dispatchers.Main).launch {
                PremiumHelper.getInstance().adManager.askForConsentIfRequired(this@PHSplashActivity) {
                    Timber.tag("PhConsentManager").d("PHSplashActivity.onCreate()-> consent done")
                }
            }
        }
    }

    private suspend fun waitForInitComplete(): PHResult<Unit> {
        val result = premiumHelper.waitForInitComplete()
        if (!PremiumHelper.getInstance().hasActivePurchase() && result.isSuccess) {
            PremiumHelper.getInstance().adManager.loadInterstitial(this)
            if (PremiumHelper.getInstance().configuration.get(Configuration.PREVENT_AD_FRAUD)) {
                Timber.w("Ad-fraud: Waiting for Interstitial Ad")
                val timeout = getInterstitialAdTimeout()
                if(PremiumHelper.getInstance().adManager.waitForInterstitial(timeout) == false){
                    StartupPerformanceTracker.getInstance().setInterstitialTimeout(timeout)
                }
            }
        }
        return result
    }

    private fun getInterstitialAdTimeout(): Long {
        return TimeUnit.SECONDS.toMillis(
            PremiumHelper.getInstance().
                            configuration.get(Configuration.AD_FRAUD_PROTECTION_TIMEOUT_SECONDS))
    }

    private fun setProgressColor(splashProgress: ProgressBar) {
        val color = ContextCompat.getColor(this, R.color.progress_light)
        splashProgress.indeterminateDrawable.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
    }

    protected open fun onPremiumHelperInitialized(result: PHResult<Unit>) {

        if (result is PHResult.Failure) {
            if (result.error is CancellationException && result.error !is TimeoutCancellationException) {
                StartupPerformanceTracker.getInstance().onSplashScreenHide()
                return
            }
        }
        UpdateManager.resumeUnfinishedUpdate(this)
        if (shouldShowStartLikePro()) {
            startActivity(Intent(this@PHSplashActivity, StartLikeProActivity::class.java))
        } else {
            if (premiumHelper.isIntroComplete()) {
                openMainActivity()
            } else {
                openIntroActivity()
            }
        }
        StartupPerformanceTracker.getInstance().onSplashScreenHide()
        finish()
    }

    protected open fun shouldShowStartLikePro(): Boolean {

        if (premiumHelper.configuration.get(Configuration.DISABLE_ONBOARDING_OFFERING)) {
            premiumHelper.preferences.setOnboardingComplete()
            return false
        }

        return !premiumHelper.preferences.isOnboardingComplete() && !premiumHelper.hasActivePurchase()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun openMainActivity() {
        val intent = Intent(this@PHSplashActivity, premiumHelper.configuration.appConfig.mainActivityClass)
        intent.putExtra(FLAG_FROM_SPLASH, true)
        startActivity(intent)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun openIntroActivity() {
        val intent = Intent(this@PHSplashActivity, premiumHelper.configuration.appConfig.introActivityClass)
        intent.putExtra(FLAG_FROM_SPLASH, true)
        startActivity(intent)
    }

}