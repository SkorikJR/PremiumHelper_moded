package com.appboosty.premiumhelper.ui.preferences.common

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.appboosty.premiumhelper.ui.preferences.PremiumPreference

class RemoveAdsPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : PremiumPreference(context, attrs) {

    init {
        premiumHelper.showLockIcon = false

        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(object : DefaultLifecycleObserver {

                override fun onCreate(owner: LifecycleOwner) {
                    isVisible = isPreferenceLocked()
                }

                override fun onResume(owner: LifecycleOwner) {
                    isVisible = isPreferenceLocked()
                }
            })
        }
    }


}