package com.silvered.ecodrive.activity

import android.content.DialogInterface
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.silvered.ecodrive.databinding.ActivityFormBinding

class FormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormBinding

    private var isAlertVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.extras?.getString("URL")
        val type = intent.extras?.getString("TYPE")

        if (isURLValid(url).not()) {
            onBackPressed()
            return
        }

        if (type == null || type.trim().isEmpty()) {
            onBackPressed()
            return
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        val webClient = object: WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript("document.getElementsByClassName('vHW8K').length > 0") {

                    if (url == null || url.contains("/formResponse").not())
                        return@evaluateJavascript

                    val condition = it.toBoolean()

                    if (condition.not())
                        return@evaluateJavascript

                    if (isAlertVisible)
                        return@evaluateJavascript

                    MaterialAlertDialogBuilder(this@FormActivity)
                        .setTitle("Grazie!")
                        .setMessage("Grazie mille per aver compilato il questionario!")
                        .setPositiveButton("OK") { dialog, _ ->
                            setFirebaseStuff(dialog,type)
                        }
                        .setCancelable(false)
                        .show()
                    isAlertVisible = true
                }
            }

        }

        binding.webView.webViewClient = webClient
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.loadUrl(url!!)
    }

    private fun setFirebaseStuff(dialog: DialogInterface, type: String) {

        when (type) {
            "PRE" -> Firebase.database.reference.child("users").child(Firebase.auth.currentUser!!.uid).child("linkPreCompleted").setValue(true)
            "FINAL" -> Firebase.database.reference.child("users").child(Firebase.auth.currentUser!!.uid).child("linkFinalCompleted").setValue(true)
        }

        dialog.dismiss()
        onBackPressed()

    }

    private fun isURLValid(url: String?): Boolean {

        if (url == null)
            return false

        if (url.trim().isEmpty())
            return false

        if (url.contains("http", ignoreCase = true).not())
            return false

        return true
    }

}