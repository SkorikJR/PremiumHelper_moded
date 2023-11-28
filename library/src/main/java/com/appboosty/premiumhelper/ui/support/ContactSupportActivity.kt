package com.appboosty.premiumhelper.ui.support

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.util.ContactSupport

class ContactSupportActivity : AppCompatActivity() {

    private val toolbar: MaterialToolbar by lazy { findViewById(R.id.toolbar) }
    private val sendButton: View by lazy { findViewById(R.id.button_send) }
    private val editText: EditText by lazy { findViewById(R.id.edit_text) }

    companion object {
        fun show(activity: Activity, email: String, emailVip: String? = null) {
            val intent = Intent(activity, ContactSupportActivity::class.java).apply {
                putExtra("email", email)
                emailVip?.let { putExtra("email_vip", emailVip) }
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_support)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val email = intent.getStringExtra("email") ?: error("No email address!")
        val emailVip = intent.getStringExtra("email_vip")

        val isVip = if (PremiumHelper.getInstance().hasActivePurchase()) {
            emailVip != null || email.contains(".vip", true)
        } else {
            false
        }

        supportActionBar?.title = if (isVip) {
            getString(R.string.contact_vip_support_title)
        } else {
            getString(R.string.contact_support_title)
        }

        editText.doOnTextChanged { text, _, _, _ ->
            sendButton.isEnabled = (text?.trim()?.length ?: 0) >= 20
        }

        sendButton.setOnClickListener {
            ContactSupport.openEmailApp(this,
                email = email,
                emailVip = emailVip,
                message = editText.text.toString())

            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        editText.requestFocus()
    }
}