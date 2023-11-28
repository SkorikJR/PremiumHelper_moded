package com.appboosty.sampleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.sample.R

class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        findViewById<View>(R.id.button_done).setOnClickListener {
            PremiumHelper.getInstance().setIntroComplete()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }
}