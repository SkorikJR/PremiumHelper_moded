package com.appboosty.premiumhelper.lint

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

@Suppress("UnstableApiUsage")
class InvalidPermissionCheckDetector : Detector(), SourceCodeScanner {

    enum class ClassType {
        NON_ACTIVITY, APP_COMPAT_ACTIVITY, ACTIVITY
    }

    companion object {

        private val IMPLEMENTATION = Implementation(InvalidPermissionCheckDetector::class.java, Scope.JAVA_FILE_SCOPE)

        val INVALID_PERMISSION_CHECK_ISSUE = Issue.create(
            id = "InvalidPermissionCheck",
            briefDescription = "Use PermissionUtils.hasPermission()",
            explanation = "Use premium-helper PermissionUtils.hasPermission() method to check permissions to avoid checking the SDK version.",
            category = Category.MESSAGES,
            priority = 5,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }

    override fun getApplicableMethodNames(): List<String>? {
        return listOf("checkSelfPermission")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "androidx.core.content.ContextCompat")) {
            if (method.name == "checkSelfPermission") {
                context.report(INVALID_PERMISSION_CHECK_ISSUE, context.getLocation(node.sourcePsi), "Use PermissionUtils.hasPermission()")
            }
        }
    }

}