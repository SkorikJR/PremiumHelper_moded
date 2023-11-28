package com.appboosty.premiumhelper.ui.preferences.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import com.appboosty.premiumhelper.PremiumHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PersonalizedAdsPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    init {
        setOnPreferenceClickListener {
            if (context is AppCompatActivity) {
                CoroutineScope(Dispatchers.Main).launch {
                    PremiumHelper.getInstance().showConsentDialog(context)
                }
            }
            true
        }
        if (context is AppCompatActivity) {
            context.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    isVisible = PremiumHelper.getInstance().isConsentAvailable()
                }
            })
        }
    }

}