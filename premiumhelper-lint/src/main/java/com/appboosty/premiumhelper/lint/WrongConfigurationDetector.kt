package com.appboosty.premiumhelper.lint

import com.android.tools.lint.detector.api.*
import com.intellij.psi.*
import org.jetbrains.uast.*

@Suppress("UnstableApiUsage")
class WrongConfigurationDetector : Detector(), SourceCodeScanner {

    data class CallContext(val context: JavaContext, val location: Location)

    private var exitAdsEnabled: Boolean = false
    private var adsConfigured: Boolean = false
    private var mainSkuConfigured: Boolean = false
    private var mainSkuEmpty: Boolean = false
    private var bannerAdConfigured: Boolean = false
    private var nativeAdConfigured: Boolean = false
    private var interstitialAdConfigured: Boolean = false
    private var exitBannerAdConfigured: Boolean = false
    private var exitNativeAdConfigured: Boolean = false
    private var termsUrlConfigured: Boolean = false
    private var privacyUrlConfigured: Boolean = false
    private var startLikeProLayoutConfigured: Boolean = false
    private var relaunchLayoutConfigured: Boolean = false
    private var relaunchOneTimeLayoutConfigured: Boolean = false
    private var oneTimeOfferConfigured: Boolean = true

    private var mainActivityClass: PsiClass? = null
    private var exitAdsConfigCall: CallContext? = null
    private var mainOfferConfigCall: CallContext? = null
    private var onetimeOfferConfigCall: CallContext? = null
    private var premiumHelperConfiguration: CallContext? = null

    companion object {

        private val IMPLEMENTATION =
            Implementation(WrongConfigurationDetector::class.java, Scope.JAVA_FILE_SCOPE)

        val INVALID_CONFIGURATION_ISSUE = Issue.create(
            id = "InvalidPremiumHelperConfiguration",
            briefDescription = "Invalid configuration of premium-helper",
            explanation = "Finds invalid configuration for premium-helper library.",
            category = Category.MESSAGES,
            priority = 5,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf(
            "configureMainOffer",
            "configureOneTimeOffer",
            "showExitConfirmationAds",
            "mainActivityClass",
            "adManagerConfiguration",
            "bannerAd",
            "nativeAd",
            "interstitialAd",
            "termsAndConditionsUrl",
            "privacyPolicyUrl",
            "startLikeProActivityLayout",
            "relaunchPremiumActivityLayout",
            "relaunchOneTimeActivityLayout",
            "exitBannerAd",
            "exitNativeAd",
            "build"
        )
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

        if (context.evaluator.isMemberInClass(
                method,
                "com.appboosty.premiumhelper.configuration.appconfig.PremiumHelperConfiguration.Builder"
            )
        ) {
            when (method.name) {

                "mainActivityClass" -> {

                    val arg = node.valueArguments[0]
                    val value = ConstantEvaluator.evaluate(context, arg)

                    val className = if (value is PsiType) {
                        value.canonicalText
                    } else {
                        val name = node.getArgumentForParameter(0)?.asSourceString()!!
                        name.substring(0, name.length - 5)
                    }

                    mainActivityClass = context.evaluator.findClass(className)
                }

                "configureOneTimeOffer" -> {

                    onetimeOfferConfigCall =
                        CallContext(context, context.getLocation(node.sourcePsi))

                    val param1 = ConstantEvaluator.evaluate(context, node.valueArguments[0])
                    val param2 = ConstantEvaluator.evaluate(context, node.valueArguments[1])

                    if ((param1 as String).isEmpty() || (param2 as String).isEmpty()) {
                        oneTimeOfferConfigured = false
                    }

                }

                "showExitConfirmationAds" -> {

                    exitAdsConfigCall = CallContext(context, context.getLocation(node.sourcePsi))

                    val arg = node.valueArguments[0]
                    val value = ConstantEvaluator.evaluate(context, arg)

                    if (value is Boolean) {
                        exitAdsEnabled = value
                    }
                }

                "adManagerConfiguration" -> {
                    adsConfigured = true
                }

                "configureMainOffer" -> {
                    val param1 = ConstantEvaluator.evaluate(context, node.valueArguments[0])
                    mainOfferConfigCall = CallContext(context, context.getLocation(node.sourcePsi))
                    mainSkuEmpty = (param1 as String).isEmpty()
                    mainSkuConfigured = !mainSkuEmpty
                }

                "termsAndConditionsUrl" -> {
                    termsUrlConfigured = true
                }

                "privacyPolicyUrl" -> {
                    privacyUrlConfigured = true
                }

                "startLikeProActivityLayout" -> {
                    startLikeProLayoutConfigured = true
                }

                "relaunchPremiumActivityLayout" -> {
                    relaunchLayoutConfigured = true
                }

                "relaunchOneTimeActivityLayout" -> {
                    relaunchOneTimeLayoutConfigured = true
                }

                "build" -> {
                    premiumHelperConfiguration = CallContext(context, context.getLocation(node))
                }
            }
        } else if (context.evaluator.isMemberInClass(
                method,
                "com.appboosty.ads.config.AdManagerConfiguration.Builder"
            )
        ) {

            when (method.name) {
                "bannerAd" -> {
                    bannerAdConfigured = true
                }

                "nativeAd" -> {
                    nativeAdConfigured = true
                }

                "interstitialAd" -> {
                    interstitialAdConfigured = true
                }

                "exitBannerAd" -> {
                    exitBannerAdConfigured = true
                }

                "exitNativeAd" -> {
                    exitNativeAdConfigured = true
                }
            }
        }
    }

    private fun isOnBackPressedImplemented(): Boolean {
        val methodCall =
            if (mainActivityClass?.containingFile?.name?.endsWith(".kt") != false) "onBackPressedDispatcher.addCallback" else
                "getOnBackPressedDispatcher().addCallback"
        mainActivityClass?.allMethods?.onEach { method ->
            if (method.containingClass?.qualifiedName == mainActivityClass?.qualifiedName) {
                when (method.name) {
                    "onBackPressed" -> return true
                    else -> run {
                        val source = method.sourceElement?.text.toString()
                        source.indexOf(methodCall).takeIf { it != -1 }?.let { index ->
                            if (isStartOfLine(source, index)) return true
                        }
                        source.indexOf("PremiumHelperUtils.addOnMainActivityExitHandler")
                            .takeIf { it != -1 }?.let { index ->
                            if (isStartOfLine(source, index)) return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun isStartOfLine(source: String, index: Int): Boolean {
        var current = index
        while (--current >= 0) {
            val currentChar = source[current]
            if (currentChar == '\n') return true
            if (currentChar != ' ' && currentChar != '\t') return false
        }
        return true
    }

    override fun afterCheckRootProject(context: Context) {
        super.afterCheckRootProject(context)

        if (mainActivityClass == null) {
            reportError(premiumHelperConfiguration!!, "MainActivity class is not configured")
        }

        if (!adsConfigured) {
            reportError(premiumHelperConfiguration!!, "AdManager is not configured")
        }

        if (!mainSkuConfigured) {
            if (mainSkuEmpty) {
                reportError(mainOfferConfigCall!!, "Main offer sku can not be empty")
            } else {
                reportError(premiumHelperConfiguration!!, "Main offer is not configured")
            }
        }

        if (!bannerAdConfigured) {
            reportError(premiumHelperConfiguration!!, "Banner Ad is not configured")
        }

        if (!interstitialAdConfigured) {
            reportError(premiumHelperConfiguration!!, "Interstitial Ad is not configured")
        }

        if (!termsUrlConfigured) {
            reportError(premiumHelperConfiguration!!, "Terms URL is not configured")
        }

        if (!privacyUrlConfigured) {
            reportError(premiumHelperConfiguration!!, "Privacy URL is not configured")
        }

        if (!startLikeProLayoutConfigured) {
            reportError(
                premiumHelperConfiguration!!,
                "StartLikePro activity layout is not configured"
            )
        }

        if (!relaunchLayoutConfigured) {
            reportError(premiumHelperConfiguration!!, "Relaunch activity layout is not configured")
        }

        if (!relaunchOneTimeLayoutConfigured) {
            reportError(
                premiumHelperConfiguration!!,
                "RelaunchOneTime activity layout is not configured"
            )
        }

        if (exitAdsEnabled && (!bannerAdConfigured || !nativeAdConfigured)) {
            reportError(
                exitAdsConfigCall!!,
                "Configure banner and native ad unit ids for exit ads: Banner: $bannerAdConfigured Native: $nativeAdConfigured"
            )
        }

        if (!oneTimeOfferConfigured) {
            reportError(onetimeOfferConfigCall!!, "One time offer skus cannot be empty")
        }

        if (exitAdsEnabled && !isOnBackPressedImplemented()) {
            println("LintCheck()-> 'Back handling' error detected")
            exitAdsConfigCall?.let {
                reportError(
                    it,
                    "onBackPressedMethod or OnBackPressedDispatcher is not implemented in  ${mainActivityClass?.name}"
                )
            }
        } else {
            println("LintCheck()-> 'Back handling' error is NOT detected")
        }
    }

    private fun reportError(callContext: CallContext, message: String) {
        callContext.context.report(
            issue = INVALID_CONFIGURATION_ISSUE,
            location = callContext.location,
            message = message
        )
    }

}