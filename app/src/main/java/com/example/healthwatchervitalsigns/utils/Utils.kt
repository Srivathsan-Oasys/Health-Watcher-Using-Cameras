package com.example.healthwatchervitalsigns.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

object Utils {

    fun displayDialog(
        context: Context,
        message: String,
        positiveButtonText: String? = null,
        positiveButtonClickListener: ((dialog: DialogInterface, which: Int) -> Unit)? = null,
        negatiButtonText: String? = null,
        negativeButtonClickListener: ((dialog: DialogInterface, which: Int) -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        positiveButtonText?.let {
            builder.setPositiveButton(positiveButtonText) { dialog, which ->
                if (positiveButtonClickListener != null) {
                    positiveButtonClickListener(dialog, which)
                }
            }
        }
        negatiButtonText?.let {
            builder.setNegativeButton(negatiButtonText) { dialog, which ->
                if (negativeButtonClickListener != null) {
                    negativeButtonClickListener(dialog, which)
                }
            }
        }

        builder.show()
    }
}