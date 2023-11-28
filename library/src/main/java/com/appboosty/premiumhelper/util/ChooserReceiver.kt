package com.appboosty.premiumhelper.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appboosty.premiumhelper.PremiumHelper


/**
 *Created by Dzano Catovic (dzano.catovic@gmail.com) on 24.10.2022.
 */
class ChooserReceiver: BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        PremiumHelper.getInstance().ignoreNextAppStart()
    }
}