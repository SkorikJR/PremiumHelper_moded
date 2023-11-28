package com.appboosty.premiumhelper.lint

import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UClass

@Suppress("UnstableApiUsage")
class WrongBaseActivityDetector : Detector(), SourceCodeScanner {

    enum class ClassType {
        NON_ACTIVITY, APP_COMPAT_ACTIVITY, ACTIVITY
    }

    companion object {

        private val IMPLEMENTATION = Implementation(WrongBaseActivityDetector::class.java, Scope.JAVA_FILE_SCOPE)

        val INVALID_BASE_ACTIVITY_CLASS_ISSUE = Issue.create(
            id = "InvalidBaseActivityClass",
            briefDescription = "AppCompatActivity must be used as base class",
            explanation = "Finds all activities not inherited from AppCompatActivity",
            category = Category.MESSAGES,
            priority = 5,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )

        private val SUPER_CLASSES = listOf(
            "android.app.Activity",
        )

    }

    override fun applicableSuperClasses(): List<String> {
        return SUPER_CLASSES
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {

        if (getClassType(context, declaration) == ClassType.ACTIVITY) {
            context.report(INVALID_BASE_ACTIVITY_CLASS_ISSUE, context.getNameLocation(declaration), "Must extend AppCompatActivity")
        }
    }

    private fun getClassType(context: JavaContext, declaration: UClass): ClassType {
        return when {
            context.evaluator.getQualifiedName(declaration).equals("androidx.appcompat.app.AppCompatActivity") -> ClassType.APP_COMPAT_ACTIVITY
            context.evaluator.getQualifiedName(declaration).equals("android.app.Activity") -> ClassType.ACTIVITY
            declaration.superClass != null -> getClassType(context, declaration.superClass!!)
            else -> ClassType.NON_ACTIVITY
        }
    }

}