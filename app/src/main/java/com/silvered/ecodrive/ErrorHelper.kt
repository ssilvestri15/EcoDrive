package com.silvered.ecodrive

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

object ErrorHelper {

    fun showError(context: Context, message: String, isCancellable: Boolean, functionAfterDismiss: () -> Unit) {

        val bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_error)
        bottomSheetDialog.findViewById<MaterialTextView>(R.id.bottomSheet_message)?.text = message
        bottomSheetDialog.setCancelable(isCancellable)

        bottomSheetDialog.findViewById<MaterialButton>(R.id.cancel_button)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            functionAfterDismiss()
        }

        bottomSheetDialog.show()
    }

}