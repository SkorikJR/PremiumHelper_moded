package com.appboosty.premiumhelper.ui.rate

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.PremiumHelper.Companion.TAG
import com.appboosty.premiumhelper.R
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import jp.wasabeef.blurry.Blurry
import timber.log.Timber

class RateDialog : AppCompatDialogFragment() {

    private var onRateCompleteListener: RateHelper.OnRateFlowCompleteListener? = null
    var rootLayout: View? = null //BLUR
    private var googlePlayOpened = false
    private var negativeIntent = false

    companion object {
        private const val ARG_THEME = "theme"
        private const val ARG_FROM_RELAUNCH = "from_relaunch"

        fun show(fm: FragmentManager, theme: Int = -1, fromRelaunch: Boolean = false, completeListener: RateHelper.OnRateFlowCompleteListener?) {
            val dialog = RateDialog()

            dialog.onRateCompleteListener = completeListener

            dialog.arguments = bundleOf(
                ARG_THEME to theme,
                ARG_FROM_RELAUNCH to fromRelaunch
            )

            try {
                with(fm.beginTransaction()) {
                    add(dialog, "RATE_DIALOG")
                    commitAllowingStateLoss()
                }
            } catch (e: IllegalStateException) {
                Timber.e(e, "Failed to show rate dialog")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customTheme = arguments?.getInt(ARG_THEME, -1) ?: -1
        if (customTheme != -1) {
            setStyle(DialogFragment.STYLE_NO_TITLE, theme)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val customLayoutId = PremiumHelper.getInstance().configuration.appConfig.rateDialogLayout
        val layoutId = if (customLayoutId == 0) {
            Timber.tag(TAG).e("Using default Rate dialog layout. Please set R.attr.rate_dialog_layout for custom rate dialog.")
            R.layout.fragment_new_rate_dialog
        } else {
            customLayoutId
        }

        val dialogView: View = LayoutInflater.from(activity).inflate(layoutId, null)

        dialogView.findViewById<View>(R.id.rate_dialog_positive_button).setOnClickListener {
            PremiumHelperUtils.openGooglePlay(requireActivity(), arguments?.getBoolean(
                ARG_FROM_RELAUNCH, false) ?: false)
            PremiumHelper.getInstance().preferences.putString("rate_intent", "positive")
            PremiumHelper.getInstance().analytics.onRateUsPositive()
            googlePlayOpened = true
            dismissAllowingStateLoss()
        }

        dialogView.findViewById<View>(R.id.rate_dialog_negative_button).setOnClickListener {
            PremiumHelper.getInstance().preferences.putString("rate_intent", "negative")
            negativeIntent = true
            dismissAllowingStateLoss()
        }

        dialogView.findViewById<View>(R.id.rate_dialog_dismiss_button)?.setOnClickListener {
            dismissAllowingStateLoss()
        }

        PremiumHelper.getInstance().analytics.onRateUsShown()

        val dialog = AlertDialog.Builder(activity).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        rootLayout = activity?.window?.decorView?.rootView
        Blurry.with(context)
            .radius(25)
            .sampling(2)
            //.color(Color.argb(66, 255, 255, 0))
            .async()
            .onto(rootLayout as ViewGroup?);
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val rateUi = if (googlePlayOpened) {
            RateHelper.RateUi.DIALOG
        } else {
            RateHelper.RateUi.NONE
        }
        Blurry.delete(rootLayout as ViewGroup?)
        onRateCompleteListener?.onRateFlowComplete(rateUi, negativeIntent)
    }

}