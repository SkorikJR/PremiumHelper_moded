package com.appboosty.premiumhelper.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.appboosty.premiumhelper.isAdActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ActivityLifecycleListener(private val activityClass: Class<out Activity>, private val callbacks: ActivityLifecycleCallbacksAdapter) : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity.javaClass == activityClass) {
            callbacks.onActivityCreated(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity.javaClass == activityClass) {
            callbacks.onActivityStarted(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity.javaClass == activityClass) {
            callbacks.onActivityResumed(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity.javaClass == activityClass) {
            callbacks.onActivityPaused(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.javaClass == activityClass) {
            callbacks.onActivityStopped(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (activity.javaClass == activityClass) {
            callbacks.onActivitySaveInstanceState(activity, outState)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity.javaClass == activityClass) {
            callbacks.onActivityDestroyed(activity)
        }
    }

}

abstract class ActivityLifecycleCallbacksAdapter : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

fun Application.doOnNextNonAdActivityResume(action: (activity: Activity) -> Unit) {
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
        override fun onActivityResumed(activity: Activity) {
            if (!activity.isAdActivity()) {
                unregisterActivityLifecycleCallbacks(this)
                action(activity)
            }
        }
    })
}

fun Activity.doOnNextActivityResume(action: (activity: Activity) -> Unit) {
    val currentActivityName = this::class.simpleName
    application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
        override fun onActivityResumed(activity: Activity) {
            if (activity != this@doOnNextActivityResume && activity.javaClass.simpleName != currentActivityName) {
                application.unregisterActivityLifecycleCallbacks(this)
                action(activity)
            }
        }
    })
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Application.onActivityResumed() : Flow<Activity> = callbackFlow {

    val callback = object: ActivityLifecycleCallbacksAdapter() {
        override fun onActivityResumed(activity: Activity) {
            trySendBlocking(activity)
        }
    }

    registerActivityLifecycleCallbacks(callback)

    awaitClose {
        unregisterActivityLifecycleCallbacks(callback)
    }
}