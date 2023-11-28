package com.appboosty.premiumhelper.ui.startlikepro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.marginLeft
import androidx.lifecycle.lifecycleScope
import com.appboosty.premiumhelper.Offer
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.performance.PurchasesPerformanceTracker
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import kotlinx.coroutines.launch
import timber.log.Timber

class StartLikeProActivity : AppCompatActivity() {

    private var offer: Offer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyCustomTheme()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        super.onCreate(savedInstanceState)

        val premiumHelper = PremiumHelper.getInstance()

        setContentView(premiumHelper.configuration.getStartLikeProLayout())

        supportActionBar?.hide()

        with(findViewById<TextView>(R.id.start_like_pro_terms_text)) {

            val termsUrl = premiumHelper.configuration.get(Configuration.TERMS_URL)
            val privacyUrl = premiumHelper.configuration.get(Configuration.PRIVACY_URL)

            text = HtmlCompat.fromHtml(getString(R.string.ph_terms_and_conditions, termsUrl, privacyUrl), HtmlCompat.FROM_HTML_MODE_LEGACY)
            movementMethod = LinkMovementMethod.getInstance()
        }

        premiumHelper.analytics.onOnboarding()

        findViewById<View>(R.id.start_like_pro_try_limited_button)?.setOnClickListener {
            openNextActivity()
        }

        findViewById<View>(R.id.start_like_pro_premium_purchase_button).setOnClickListener {
            offer?.let {
                if (premiumHelper.configuration.isDebugMode() && it.sku.isEmpty()) {
                    openNextActivity()
                    return@setOnClickListener
                }

                premiumHelper.analytics.onPurchaseStarted("onboarding", it.sku)

                lifecycleScope.launch {
                    premiumHelper.launchBillingFlow(this@StartLikeProActivity, it)
                        .collect { purchaseResult ->
                            if (purchaseResult.isSuccess()) {
                                premiumHelper.analytics.onPurchaseSuccess(it.sku)
                                openNextActivity()
                            } else {
                                Timber.tag(PremiumHelper.TAG).e("Purchase failed: ${purchaseResult.billingResult.responseCode}")
                            }
                        }
                }
            }
        }

        val progressView: ProgressBar = findViewById(R.id.start_like_pro_progress)
        progressView.visibility = View.VISIBLE

        findViewById<View>(R.id.start_like_pro_close_button)?.let { buttonClose ->

            buttonClose.setOnClickListener {
                openNextActivity()
            }

            adjustCloseButtonPosition(buttonClose)
        }

        lifecycleScope.launchWhenCreated {
            PurchasesPerformanceTracker.getInstance().onStartLoadOffers()
            PurchasesPerformanceTracker.getInstance().setOfferScreenName("start_like_pro")
            val result = premiumHelper.getOffer(Configuration.MAIN_SKU)

            offer = if (result is PHResult.Success) {
                result.value
            } else {
                val skuName = premiumHelper.configuration.get(Configuration.MAIN_SKU)
                Offer(skuName, null, null)
            }.also {
                PurchasesPerformanceTracker.getInstance().onEndLoadOffers()
                if (result is PHResult.Success) {
                    progressView.visibility = View.GONE
                    findViewById<TextView>(R.id.start_like_pro_price_text).text = PremiumHelperUtils.formatSkuPrice(this@StartLikeProActivity, it.skuDetails)
                }

                findViewById<TextView>(R.id.start_like_pro_premium_purchase_button).text = PremiumHelperUtils.getCtaButtonText(this@StartLikeProActivity, it)
            }
            offer?.let {
                premiumHelper.analytics.onPurchaseImpression(it.sku, "onboarding")
            }
        }

    }

    private fun openNextActivity() {

        with (PremiumHelper.getInstance()) {

            preferences.setOnboardingComplete()
            analytics.onOnboardingComplete(offer != null && offer?.skuDetails != null)

            if (isIntroComplete()) {
                startActivity(Intent(this@StartLikeProActivity, configuration.appConfig.mainActivityClass))
            } else {
                startActivity(Intent(this@StartLikeProActivity, configuration.appConfig.introActivityClass))
            }
        }

        finish()
    }

    private fun adjustCloseButtonPosition(buttonClose: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val activityRootView = window.decorView.findViewById<View>(android.R.id.content)
            activityRootView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    activityRootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    buttonClose.setOnApplyWindowInsetsListener { _, insets ->

                        buttonClose.setOnApplyWindowInsetsListener(null)

                        insets.displayCutout?.let { cutout ->
                            buttonClose.translationX =
                                if (cutout.boundingRects.isNotEmpty()) {
                                    if (cutout.boundingRects[0].intersects(buttonClose.left, buttonClose.top, buttonClose.right, buttonClose.bottom)) {
                                        if (cutout.boundingRects[0].left == 0) {
                                            (activityRootView.width - buttonClose.width - 2 * buttonClose.marginLeft).toFloat()
                                        } else {
                                            -(activityRootView.width - buttonClose.width - 2 * buttonClose.marginLeft).toFloat()
                                        }
                                    } else {
                                        0f
                                    }
                                } else {
                                    0f
                                }.also {
                                    Timber.tag("CUTOUT").i("cutout: ${cutout.boundingRects[0]}")
                                    Timber.tag("CUTOUT").i("close button: left: ${buttonClose.left} right: ${buttonClose.right}")
                                    Timber.tag("CUTOUT").i("applied translation: $it")
                                }
                        }

                        return@setOnApplyWindowInsetsListener insets
                    }
                    buttonClose.requestApplyInsets()
                }
            })
        }
    }

    private fun applyCustomTheme() {
        with (obtainStyledAttributes(R.style.PhPremiumOfferingTheme, intArrayOf(R.attr.premium_offering_style))) {
            val themeId = getResourceId(0, -1)
            if (themeId > 0) {
                setTheme(themeId)
            } else {
                setTheme(R.style.PhPremiumOfferingTheme)
            }
            recycle()
        }
    }

}