package com.appboosty.premiumhelper.ui.relaunch

import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.appboosty.premiumhelper.Offer
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.configuration.Configuration
import com.appboosty.premiumhelper.performance.PurchasesPerformanceTracker
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class RelaunchPremiumActivity : AppCompatActivity() {

    companion object {
        const val ARG_SOURCE = "source"
        const val ARG_THEME = "theme"
    }

    private lateinit var timer: CountDownTimer

    private lateinit var progressView: View
    private lateinit var buttonPurchase: TextView
    private lateinit var textPrice: TextView
    private lateinit var buttonClose: View

    private var textTime: TextView? = null
    private var textPriceStrike: TextView? = null

    private lateinit var premiumHelper: PremiumHelper
    private lateinit var offer: Offer
    private lateinit var source: String

    private var oneTimeOfferAvailable: Boolean = false

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

        premiumHelper = PremiumHelper.getInstance()
        oneTimeOfferAvailable = premiumHelper.relaunchCoordinator.isOneTimeOfferAvailable()

        setContentView(getLayoutId())

        supportActionBar?.hide()

        source = intent?.getStringExtra(ARG_SOURCE) ?: RelaunchCoordinator.SOURCE_RELAUNCH

        progressView = findViewById(R.id.relaunch_premium_progress)
        textTime = findViewById(R.id.relaunch_premium_text_time)
        textPrice = findViewById(R.id.relaunch_premium_text_price)
        textPriceStrike = findViewById(R.id.relaunch_premium_text_price_strike)
        buttonPurchase = findViewById(R.id.relaunch_premium_purchase_button)
        buttonClose = findViewById(R.id.relaunch_premium_close_button)

        if (textPriceStrike != null) {
            textPriceStrike!!.paintFlags = textPriceStrike!!.paintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
        }

        buttonClose.setOnClickListener {
            finish()
        }

        buttonPurchase.setOnClickListener {
            if (this::offer.isInitialized) {
                startPurchase()
            }
        }

        progressView.visibility = View.VISIBLE
        buttonPurchase.visibility = View.VISIBLE

        lifecycleScope.launchWhenCreated {

            PurchasesPerformanceTracker.getInstance().onStartLoadOffers()
            PurchasesPerformanceTracker.getInstance().setOfferScreenName("relaunch")
            val offers = if (oneTimeOfferAvailable) {
                PurchasesPerformanceTracker.getInstance().setOneTimeOfferAvailable()
                awaitAll(
                    async { premiumHelper.getOffer(Configuration.ONETIME_OFFER) },
                    async { premiumHelper.getOffer(Configuration.ONETIME_OFFER_STRIKETHROUGH) }
                )
            } else {
                awaitAll(
                    async { premiumHelper.getOffer(Configuration.MAIN_SKU) }
                )
            }


            if (offers.all { it is PHResult.Success }) {

                showOffer(offers.map { (it as PHResult.Success).value })

                if (oneTimeOfferAvailable) {
                    setupTimer()
                }

            } else {
                onOfferLoadError()
            }
        }

        adjustCloseButtonPosition()

    }

    private fun adjustCloseButtonPosition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val activityRootView = window.decorView.findViewById<View>(android.R.id.content)
            activityRootView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    activityRootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    buttonClose.setOnApplyWindowInsetsListener { _, insets ->

                        buttonClose.setOnApplyWindowInsetsListener(null)

                        insets.displayCutout?.let { cutout ->
                            if (cutout.boundingRects.isNotEmpty()) {
                                if (cutout.boundingRects[0].intersects(buttonClose.left, buttonClose.top, buttonClose.right, buttonClose.bottom)) {

                                    val layoutParams = buttonClose.layoutParams as ConstraintLayout.LayoutParams

                                    if (cutout.boundingRects[0].left == 0) {
                                        layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                                        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.UNSET
                                    } else {
                                        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                                        layoutParams.rightToRight = ConstraintLayout.LayoutParams.UNSET
                                    }

                                    buttonClose.layoutParams = layoutParams
                                }
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

    private fun setupTimer() {

        premiumHelper.relaunchCoordinator.onOneTimeOfferShown()

        val countTime = premiumHelper.preferences.getOneTimeOfferStartTime() + RelaunchCoordinator.ONE_TIME_OFFER_TIME_MS - System.currentTimeMillis()

        timer = object: CountDownTimer(countTime, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                textTime?.text = formatElapsedTime(millisUntilFinished)
            }

            override fun onFinish() {
                finish()
            }

        }
        timer.start()

    }

    override fun finish() {
        if (source == RelaunchCoordinator.SOURCE_RELAUNCH) {
            premiumHelper.relaunchCoordinator.handleRelaunchClose()
        }

        super.finish()
    }

    override fun onStop() {

        if (this::timer.isInitialized) {
            timer.cancel()
        }

        super.onStop()
    }

    private fun formatElapsedTime(millis: Long): String {

        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun showOffer(offers: List<Offer>) {

        offer = offers[0]

        if (source == RelaunchCoordinator.SOURCE_RELAUNCH) {
            premiumHelper.analytics.onRelaunch(offer.sku)
        }

        premiumHelper.analytics.onPurchaseImpression(offer.sku, source)

        if (oneTimeOfferAvailable) {
            textPrice.text = offers[0].skuDetails?.originalPrice
            textPriceStrike?.text = offers[1].skuDetails?.originalPrice
            textPriceStrike?.visibility = View.VISIBLE
        } else {
            textPrice.text = PremiumHelperUtils.formatSkuPrice(this, offers[0].skuDetails)
            buttonPurchase.text = PremiumHelperUtils.getCtaButtonText(this, offer)
        }

        progressView.visibility = View.GONE
        textPrice.visibility = View.VISIBLE
        buttonPurchase.visibility = View.VISIBLE
        PurchasesPerformanceTracker.getInstance().onEndLoadOffers()
    }

    private fun onOfferLoadError() {
        val skuName = premiumHelper.configuration.get(Configuration.MAIN_SKU)
        offer = Offer(skuName, null, null)
        PurchasesPerformanceTracker.getInstance().onEndLoadOffers()
    }

    private fun startPurchase() {

        premiumHelper.analytics.onPurchaseStarted(source, offer.sku)

        lifecycleScope.launch {
            PremiumHelper.getInstance().launchBillingFlow(
                this@RelaunchPremiumActivity,
                offer
            )
                .collect { purchaseResult ->
                    if (purchaseResult.isSuccess()) {
                        premiumHelper.analytics.onPurchaseSuccess(offer.sku)
                        finish()
                    } else {
                        Timber.tag(PremiumHelper.TAG).e("Purchase error ${purchaseResult.billingResult.responseCode}")
                    }
                }
        }
    }

    private fun getLayoutId(): Int {
        return if (oneTimeOfferAvailable) {
            premiumHelper.configuration.getRelaunchOneTimeLayout()
        } else {
            premiumHelper.configuration.getRelaunchLayout()
        }
    }

}