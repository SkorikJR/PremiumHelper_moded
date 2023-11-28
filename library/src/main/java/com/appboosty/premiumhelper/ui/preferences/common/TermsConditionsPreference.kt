package com.appboosty.premiumhelper.ui.preferences.common

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.appboosty.premiumhelper.PremiumHelper

class TermsConditionsPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs) {

    init {
        setOnPreferenceClickListener {
            if (context is Activity) {
                PremiumHelper.getInstance().showTermsAndConditions(context)
            }
            true
        }
    }

}