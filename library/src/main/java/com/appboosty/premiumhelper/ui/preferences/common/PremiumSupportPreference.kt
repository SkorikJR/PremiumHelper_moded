package com.appboosty.premiumhelper.ui.preferences.common

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.ui.preferences.PremiumPreference
import com.appboosty.premiumhelper.util.ContactSupport

class PremiumSupportPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : PremiumPreference(context, attrs) {

    private val vipEmail: String?

    init {

        with(context.obtainStyledAttributes(attrs, R.styleable.PremiumPreference)) {

            val email = getString(R.styleable.PremiumPreference_support_email) ?: error("You have to set support_email value for Support Preference")
            vipEmail = getString(R.styleable.PremiumPreference_vip_support_email)
            recycle()

            if (vipEmail != null) {
                premiumHelper.showLockIcon = false
            }

            setOnPreferenceClickListener {
                if (context is Activity) {
                    ContactSupport.sendEmail(context, email, vipEmail)
                }
                true
            }
        }

    }

    override fun isPreferenceLocked(): Boolean {
        return (vipEmail == null && super.isPreferenceLocked())
    }
}