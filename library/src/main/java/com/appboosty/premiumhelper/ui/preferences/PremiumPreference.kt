package com.appboosty.premiumhelper.ui.preferences

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.appboosty.premiumhelper.PremiumHelper

open class PremiumPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs) {

    protected val premiumHelper = PreferenceHelper(context, attrs)
    private var userClickListener: OnPreferenceClickListener? = null

    init {
        super.setOnPreferenceClickListener { preference ->
            if (!isPreferenceLocked()) {
                userClickListener?.onPreferenceClick(preference) ?: false
            } else {
                if (context is Activity) {
                    PremiumHelper.getInstance()
                        .showPremiumOffering("preference_${key}")
                }
                true
            }
        }
    }

    protected open fun isPreferenceLocked(): Boolean {
        return !premiumHelper.isUnlocked()
    }

    override fun setOnPreferenceClickListener(onPreferenceClickListener: OnPreferenceClickListener?) {
        userClickListener = onPreferenceClickListener
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        premiumHelper.bindPreferenceViewHolder(holder)
    }

}