package com.appboosty.premiumhelper.ui.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceViewHolder
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R

open class PreferenceHelper(context: Context, attrs: AttributeSet?) {

    protected var titleTextView: TextView? = null
    protected var summaryTextView: TextView? = null

    private enum class IconPosition {
        START, END
    }

    protected var iconRes = -1
    private var iconPosition = IconPosition.END
    private var iconSize = -1
    protected var iconColor:ColorStateList? = null
    var showLockIcon = true

    private val premiumTitleText: String?
    private val premiumSummaryText: String?

    init {
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    updateLockBadge()
                    updateTitleText()
                    updateSummaryText()
                }
            })
        }

        with(context.obtainStyledAttributes(attrs, R.styleable.PremiumPreference)) {

            iconRes = getResourceId(R.styleable.PremiumPreference_lock_icon, -1)
            iconSize = getDimensionPixelSize(R.styleable.PremiumPreference_lock_icon_size, -1)
            iconColor = getColorStateList(R.styleable.PremiumPreference_lock_icon_color)
            iconPosition = IconPosition.valueOf((getNonResourceString(R.styleable.PremiumPreference_lock_icon_position) ?: IconPosition.END.name).uppercase())

            premiumTitleText = getString(R.styleable.PremiumPreference_title_premium)
            premiumSummaryText = getString(R.styleable.PremiumPreference_summary_premium)

            recycle()
        }
    }

    fun bindPreferenceViewHolder(holder: PreferenceViewHolder) {

        val titleView = holder.findViewById(android.R.id.title)
        if (titleView is TextView) {
            titleTextView = titleView
            updateLockBadge()
            updateTitleText()
        }

        val summaryView = holder.findViewById(android.R.id.summary)
        if (summaryView is TextView) {
            summaryTextView = summaryView
            updateSummaryText()
        }

    }

    fun isUnlocked(): Boolean {
        return PremiumHelper.getInstance().hasActivePurchase()
    }

    private fun updateTitleText() {
        premiumTitleText?.let { title ->
            if (isUnlocked()) {
                titleTextView?.text = title
            }
        }
    }

    private fun updateSummaryText() {
        premiumSummaryText?.let { summary ->
            if (isUnlocked()) {
                summaryTextView?.text = summary
            }
        }
    }

    open fun updateLockBadge() {
        if (isUnlocked()) {
            clearLockIcon()
        } else {
            setLockIcon()
        }
    }

    protected fun clearLockIcon() {
        titleTextView?.apply {
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    protected fun setLockIcon() {
        if (showLockIcon) {
            titleTextView?.apply {
                compoundDrawablePadding =
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, this.resources.displayMetrics).toInt()

                TextViewCompat.setCompoundDrawableTintList(this, iconColor ?: ColorStateList.valueOf(this.currentTextColor))

                val icon = if (iconRes != -1) iconRes else R.drawable.ic_preference_lock

                if (iconSize != -1) {

                    val iconDrawable = ResourcesCompat.getDrawable(resources, icon, context.theme) ?: error("Failed to load icon")
                    iconDrawable.setBounds(0, 0, iconSize, iconSize)

                    when (iconPosition) {
                        IconPosition.START -> setCompoundDrawables(iconDrawable, null, null, null)
                        IconPosition.END -> setCompoundDrawables(null, null, iconDrawable, null)
                    }

                } else {
                    when (iconPosition) {
                        IconPosition.START -> setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
                        IconPosition.END -> setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
                    }
                }
            }
        }
    }

}