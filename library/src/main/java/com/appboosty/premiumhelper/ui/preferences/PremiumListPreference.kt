package com.appboosty.premiumhelper.ui.preferences

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.appboosty.premiumhelper.PremiumHelper

class PremiumListPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ListPreference(context, attrs) {

    private val helper = ListPreferenceHelper(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        helper.bindPreferenceViewHolder(holder)
    }

    override fun onClick() {
        if (helper.isUnlocked() || helper.freeEntries?.isNotEmpty() == true) {
            super.onClick()
        } else {
            if (context is Activity) {
                PremiumHelper.getInstance().showPremiumOffering("preference_${key}")
            }
        }
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        if (onPreferenceChange(newValue)) {
            return super.callChangeListener(newValue)
        }
        return false
    }

    private fun onPreferenceChange(newValue: Any?): Boolean {
        val isAllowed = if (helper.isUnlocked()) {
            true
        } else  {
            val index = findIndexOfValue(newValue as String)
            helper.freeEntries?.contains(index) == true
        }

        if (!isAllowed) {
            if (context is Activity) {
                PremiumHelper.getInstance().showPremiumOffering("preference_${key}")
            }
        }

        return isAllowed
    }

    override fun getEntries(): Array<CharSequence> {
        return helper.getEntries(super.getEntries())
    }

}