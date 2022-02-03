package com.silvered.ecodrive.util.helpers

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.silvered.ecodrive.R

object ErrorHelper {

    fun showError(context: Context, title:String, message: String, isCancellable: Boolean, functionAfterCancellation: () -> Unit, functionAfterDismiss: () -> Unit) {

        val bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_error)
        bottomSheetDialog.findViewById<MaterialTextView>(R.id.bottomSheet_message)?.text = message
        bottomSheetDialog.setCancelable(isCancellable)

        if (isCancellable) {
            bottomSheetDialog.setOnCancelListener {
                functionAfterCancellation()
            }
        }

        if (title != "") {
            bottomSheetDialog.findViewById<MaterialTextView>(R.id.error_title)?.text = title
        }

        val button = bottomSheetDialog.findViewById<MaterialButton>(R.id.cancel_button)
        val colorPrimary = MaterialColors.getColor(
            context,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(context, R.color.pink)
        )
        button?.setBackgroundColor(colorPrimary)
        button?.setOnClickListener {
            bottomSheetDialog.dismiss()
            functionAfterDismiss()
        }

        bottomSheetDialog.show()
    }

    fun showError(context: Context, message: String, isCancellable: Boolean, functionAfterDismiss: () -> Unit) {

        val bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_error)
        bottomSheetDialog.findViewById<MaterialTextView>(R.id.bottomSheet_message)?.text = message
        bottomSheetDialog.setCancelable(isCancellable)
        val button = bottomSheetDialog.findViewById<MaterialButton>(R.id.cancel_button)
        val colorPrimary = MaterialColors.getColor(
            context,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(context, R.color.pink)
        )
        button?.setBackgroundColor(colorPrimary)
        button?.setOnClickListener {
            bottomSheetDialog.dismiss()
            functionAfterDismiss()
        }

        bottomSheetDialog.show()
    }

}