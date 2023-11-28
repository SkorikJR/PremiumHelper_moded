package com.appboosty.premiumhelper.ui.preferences.common

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.appboosty.premiumhelper.PremiumHelper

class PrivacyPolicyPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs) {

    init {
        setOnPreferenceClickListener {
            if (context is Activity) {
                PremiumHelper.getInstance().showPrivacyPolicy(context)
            }
            true
        }
    }

}