package com.appboosty.premiumhelper.ui.preferences

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.appboosty.premiumhelper.PremiumHelper

class PremiumSwitchPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : SwitchPreferenceCompat(context, attrs) {

    private val premiumHelper = PreferenceHelper(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        premiumHelper.bindPreferenceViewHolder(holder)
    }

    override fun onClick() {
        if (premiumHelper.isUnlocked()) {
            super.onClick()
        } else {
            if (context is Activity) {
                PremiumHelper.getInstance().showPremiumOffering("preference_${key}")
            }
        }
    }

}