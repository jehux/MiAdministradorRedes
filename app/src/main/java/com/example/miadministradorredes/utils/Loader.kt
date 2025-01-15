package com.example.miadministradorredes.utils

import android.app.ProgressDialog
import android.content.Context

class Loader(private val context: Context) {
    private var progressDialog: ProgressDialog? = null

    fun showLoader(message: String = "Loading...") {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(context).apply {
                setMessage(message)
                setCancelable(false)
                show()
            }
        }
    }

    fun dismissLoader() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}