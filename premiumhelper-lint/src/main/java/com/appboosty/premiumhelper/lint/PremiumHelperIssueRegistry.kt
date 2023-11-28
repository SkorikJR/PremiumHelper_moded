package com.appboosty.premiumhelper.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.Issue

@Suppress("UnstableApiUsage")
class PremiumHelperIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() = listOf(
            WrongConfigurationDetector.INVALID_CONFIGURATION_ISSUE,
            WrongBaseActivityDetector.INVALID_BASE_ACTIVITY_CLASS_ISSUE,
            InvalidPermissionCheckDetector.INVALID_PERMISSION_CHECK_ISSUE,
            ManifestWrongConfigurationDetector.INVALID_MANIFEST_CHECK_ISSUE
        )

    override val vendor: Vendor?
        get() = Vendor("ZipoApps")
}