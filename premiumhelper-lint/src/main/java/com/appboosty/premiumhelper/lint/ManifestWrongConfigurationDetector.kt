package com.appboosty.premiumhelper.lint

import com.android.SdkConstants.TAG_APPLICATION
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element


@Suppress("UnstableApiUsage")
class ManifestWrongConfigurationDetector : Detector(), Detector.XmlScanner {

    companion object {
        private val IMPLEMENTATION = Implementation(
            ManifestWrongConfigurationDetector::class.java,
            Scope.MANIFEST_SCOPE
        )

        val INVALID_MANIFEST_CHECK_ISSUE = Issue.create(
            id = "InvalidManifestOption",
            briefDescription = "Do not use enableOnBackInvokedCallback option",
            explanation = "In case this flag is set to true a user can be able to close the interstitials as soon as they appear",
            category = Category.MESSAGES,
            priority = 5,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }

    override fun getApplicableElements(): Collection<String> {
        return listOf(
            TAG_APPLICATION
        )
    }

    override fun visitElement(context: XmlContext, element: Element) {
        element.attributes?.getNamedItem("android:enableOnBackInvokedCallback")?.let {
            if (it.nodeValue == "true") {
                println("LintCheck()-> Element 'enableOnBackInvokedCallback' is set to true")
                context.report(
                    issue = INVALID_MANIFEST_CHECK_ISSUE,
                    location = context.getLocation(element),
                    message = INVALID_MANIFEST_CHECK_ISSUE.getExplanation(TextFormat.TEXT)
                )
            }
        }?: run{
            println("LintCheck()-> enableOnBackInvokedCallback not found")
        }

    }
}