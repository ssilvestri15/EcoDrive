package com.silvered.ecodrive

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

object ErrorHelper {

    fun showError(context: Context, message: String, isCancellable: Boolean, functionAfterDismiss: () -> Unit) {

        val bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_error)
        bottomSheetDialog.findViewById<MaterialTextView>(R.id.bottomSheet_message)?.text = message
        bottomSheetDialog.setCancelable(isCancellable)
        val button = bottomSheetDialog.findViewById<MaterialButton>(R.id.cancel_button)
        button?.setBackgroundColor(fetchColorPrimary(context))
        button?.setOnClickListener {
            bottomSheetDialog.dismiss()
            functionAfterDismiss()
        }

        bottomSheetDialog.show()
    }

    private fun fetchColorPrimary(context: Context): Int {
        val typedValue = TypedValue()
        val a: TypedArray =
            context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.colorPrimary))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

}