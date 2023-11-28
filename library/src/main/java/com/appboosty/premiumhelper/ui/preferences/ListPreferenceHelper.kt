package com.appboosty.premiumhelper.ui.preferences

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.appboosty.premiumhelper.R

class ListPreferenceHelper(val context: Context, attrs: AttributeSet? = null) : PreferenceHelper(context, attrs) {

    internal var freeEntries: List<Int>? = null

    init {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.PremiumPreference)

        try {
            val entries = attr.getString(R.styleable.PremiumPreference_freeEntries)
            freeEntries = entries?.split(",")?.map { it.trim().toInt() }
        } finally {
            attr.recycle()
        }
    }

    override fun updateLockBadge() {
        if (isUnlocked() || freeEntries?.isNotEmpty() == true) {
            clearLockIcon()
        } else {
            setLockIcon()
        }
    }

    fun getEntries(entries: Array<CharSequence>): Array<CharSequence> {
        return if (isUnlocked() || freeEntries?.isEmpty() == true) {
            entries
        } else {
            val icon = if (iconRes != -1) iconRes else R.drawable.ic_preference_lock
            val iconDrawable = ResourcesCompat.getDrawable(context.resources, icon, context.theme) ?: error("Cannot load icon")
            iconDrawable.setBounds(0, 0, 48, 48)
            titleTextView?.let { DrawableCompat.setTint(iconDrawable, iconColor?.defaultColor ?: it.currentTextColor) }
            entries.mapIndexed { index, entry ->
                if (freeEntries?.contains(index) == true) {
                    entry
                } else {
                    val spannable = SpannableString("$entry   ")
                    spannable.setSpan(ImageSpan(iconDrawable, ImageSpan.ALIGN_BASELINE), spannable.length - 2, spannable.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable
                }
            }.toTypedArray()
        }
    }

}