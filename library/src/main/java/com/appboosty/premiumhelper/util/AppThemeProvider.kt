package com.appboosty.premiumhelper.util

import android.app.Activity

interface AppThemeProvider {
    fun getCustomTheme(): Int
}

fun Activity.getCustomTheme(): Int {
    return if (this is AppThemeProvider) {
        this.getCustomTheme()
    } else {
        -1
    }
}
