package com.appboosty.premiumhelper.ui.preferences.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PremiumHelperUtils

class RateUsPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs) {

    init {
        setOnPreferenceClickListener {
            if (context is AppCompatActivity) {
                PremiumHelper.getInstance().showRateDialog(context.supportFragmentManager)
            } else {
                PremiumHelperUtils.errorOrCrash("Please use AppCompatActivity for ${context.javaClass.name}")
            }
            true
        }
    }

}