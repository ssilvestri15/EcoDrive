package com.silvered.ecodrive.activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.ActivityLoginBinding
import com.silvered.ecodrive.util.helpers.ErrorHelper


private sealed class DialogMessage {
    object ESONERO : DialogMessage()
    object GAMIFIED : DialogMessage()
    object NOT_GAMIFIED : DialogMessage()
}

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: SignInClient
    private lateinit var sharedPref: SharedPreferences

    private val getActivityResult =
        registerForActivityResult(StartIntentSenderForResult()) { result: ActivityResult? ->
            if (result != null) {
                this.handleResultGoogle(result)
            } else {
                showError(null)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences("info", MODE_PRIVATE)

        val onboarding = sharedPref.getBoolean("isFirstTime", true)

        if (onboarding) {
            val intent = Intent(this@LoginActivity, OnBoardingActivity::class.java)
            startActivity(intent)
            finish()
        } else {

            firebaseAuth = FirebaseAuth.getInstance()
            if (firebaseAuth.currentUser != null) {

                Firebase.database.reference.child("users").child(firebaseAuth.currentUser!!.uid)
                    .addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                            if (!dataSnapshot.exists()) {
                                firebaseAuth.signOut()
                                sharedPref.edit().clear().commit()
                                binding.dummyIcon.visibility = View.GONE
                                return
                            }

                            val nazione = dataSnapshot.child("nazione").getValue(String::class.java)
                            val regione = dataSnapshot.child("regione").getValue(String::class.java)
                            var isGamified =
                                dataSnapshot.child("isGamified").getValue(Boolean::class.java)

                            if (nazione == null || regione == null) {
                                goToCountryActvity()
                                return
                            }

                            var uid = firebaseAuth.currentUser!!.uid.filter { it.isDigit() }

                            if (uid.length >= 10)
                                uid = uid.substring(0,8)

                            if (isGamified == null)
                                isGamified = uid.toInt() % 2 == 0

                            val editor = sharedPref.edit()
                            editor.putString("nazione", nazione)
                            editor.putString("regione", regione)
                            editor.putBoolean("isGamified", isGamified)
                            editor.commit()
                            goToMainActivity()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            firebaseAuth.signOut()
                            sharedPref.edit().clear().commit()
                            binding.dummyIcon.visibility = View.GONE
                        }

                    })

            } else {
                binding.dummyIcon.visibility = View.GONE
            }

        }


        googleSignInClient = Identity.getSignInClient(this)

        binding.signInGoogle.setOnClickListener {

            MaterialAlertDialogBuilder(this)
                .setTitle("Attenzione")
                .setMessage(getDialogMessage(DialogMessage.ESONERO))
                .setNeutralButton("Annulla") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("Rifiuta") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Accetta") { dialog, _ ->
                    dialog.dismiss()
                    binding.progressLogin.visibility = View.VISIBLE
                    binding.signInGoogle.visibility = View.GONE
                    manageGoogleFlow(true)
                }
                .show()

        }
    }

    private fun goToCountryActvity() {
        val intent = Intent(this@LoginActivity, CountryActivity::class.java)
        intent.putExtra("isLogin", true)
        startActivity(intent)
        finish()
    }

    private fun manageGoogleFlow(isLogin: Boolean) {
        val request = getRequest(isLogin)
        googleSignInClient.beginSignIn(request)
            .addOnSuccessListener(this) { result: BeginSignInResult ->
                val intent = result.pendingIntent.intentSender
                val senderRequest = IntentSenderRequest.Builder(intent).build()
                getActivityResult.launch(senderRequest)
            }
            .addOnFailureListener(this) { e: Exception ->
                if (isLogin) {
                    manageGoogleFlow(false)
                } else {
                    showError(e.localizedMessage)
                }
            }
    }

    private fun getRequest(isLogin: Boolean): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true) // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id)) // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(isLogin)
                    .build()
            ) // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(false)
            .build()
    }

    private fun handleResultGoogle(result: ActivityResult) {
        val data = result.data
        try {
            val credential: SignInCredential =
                googleSignInClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            val name = credential.displayName
            val picurl = credential.profilePictureUri
            if (idToken != null && name != null) {
                firebaseAuthWithGoogle(idToken, name, picurl)
            } else {
                showError(null)
            }

        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> {
                    binding.progressLogin.visibility = View.GONE
                    binding.signInGoogle.visibility = View.VISIBLE
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    showError("Controlla la connessione ad internet e riprova")
                }
                else -> {
                    showError(e.localizedMessage)
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, name: String, picurl: Uri?) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener(this) {
                saveUserToDB(name, picurl)
            }
            .addOnFailureListener(this) { e ->
                showError(e.localizedMessage)
            }
    }

    private fun saveUserToDB(name: String, picurl: Uri?) {


        if (firebaseAuth.currentUser != null) {

            val userId = firebaseAuth.currentUser!!.uid

            val database = FirebaseDatabase.getInstance().reference
            val userInDB = database.child("users/${userId}")

            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val editor = sharedPref.edit()
                    editor.putBoolean("carla", false).commit()

                    if (!dataSnapshot.exists()) {

                        var uid = userId.filter { it.isDigit() }

                        if (uid.length >= 10){
                            uid = uid.substring(0,8)
                        }

                        val isGamified = uid.toInt() % 2 == 0
                        editor.putBoolean("isGamified", isGamified).commit()


                        userInDB.child("sommaPunti").setValue("0")
                        userInDB.child("numeroViaggi").setValue(0)
                        userInDB.child("email").setValue(firebaseAuth.currentUser!!.email)
                        userInDB.child("fullname").setValue(name)
                        userInDB.child("isGamified").setValue(isGamified)
                        userInDB.child("level").setValue(1)

                        if (picurl != null)
                            userInDB.child("picurl").setValue(picurl.toString())

                        MaterialAlertDialogBuilder(this@LoginActivity)
                            .setTitle("Informazioni utili")
                            .setMessage(getDialogMessage(if (isGamified) DialogMessage.GAMIFIED else DialogMessage.NOT_GAMIFIED))
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                goToCountryActvity()
                            }
                            .setCancelable(false)
                            .show()

                    } else {
                        val nazione = dataSnapshot.child("nazione").getValue(String::class.java)
                        val regione = dataSnapshot.child("regione").getValue(String::class.java)
                        var isGamified =
                            dataSnapshot.child("isGamified").getValue(Boolean::class.java)

                        var uid = firebaseAuth.currentUser!!.uid.filter { it.isDigit() }

                        if (uid.length >= 10) {
                            uid = uid.substring(0,8)
                        }

                        if (isGamified == null)
                            isGamified = uid.toInt() % 2 == 0

                        editor.putBoolean("isGamified", isGamified).commit()

                        MaterialAlertDialogBuilder(this@LoginActivity)
                            .setTitle("Informazioni utili")
                            .setMessage(getDialogMessage(if (isGamified) DialogMessage.GAMIFIED else DialogMessage.NOT_GAMIFIED))
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                if (nazione == null || nazione == "" || regione == null || regione == "") {
                                    goToCountryActvity()
                                } else {
                                    editor.putString("nazione", nazione)
                                    editor.putString("regione", regione)
                                    editor.commit()
                                    goToMainActivity()
                                }
                            }
                            .setCancelable(false)
                            .show()

                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    showError(databaseError.message)
                }
            }

            userInDB.addListenerForSingleValueEvent(listener)
        } else {
            showError(null)
        }

    }

    private fun goToMainActivity() {
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String?) {
        if (message != null) {
            ErrorHelper.showError(this@LoginActivity, message, false) {}
        } else {
            ErrorHelper.showError(
                this@LoginActivity,
                "Si è verificato un errore sconosciuto",
                false
            ) {}
        }
        firebaseAuth.signOut()
        runOnUiThread {
            binding.progressLogin.visibility = View.GONE
            binding.signInGoogle.visibility = View.VISIBLE
        }
    }


    private fun getDialogMessage(state: DialogMessage): String {

        return when (state) {
            is DialogMessage.ESONERO -> "Ricorda di prestare attenzione durante la guida!\n\nLo sviluppatore, il team di ricerca e l' Università degli studi di Salerno non si assumono alcuna responsabilità per eventuali danni a persone o cose subiti durante l'utilizzo dell'applicazione.\n\nAccetti queste condizioni?"
            is DialogMessage.GAMIFIED -> "Ad ogni sessione di guida riceverai dei punti detti EcoPoints!\n\nGli EcoPoints sono calcolati sulla base del tuo stile di guida e riceverai dei feedback in tempo reale sul tuo andamento.\n\nRiceverai inoltre una valutazione in tempo reale del tuo stile di guida rappresentato da tre foglioline.\n\nPotrai competere con altre persone, consulta la classifica!\n\nOra goditi l'esperienza di EcoDrive!"
            is DialogMessage.NOT_GAMIFIED -> "Ad ogni sessione di guida riceverai dei feedback in tempo reale sul tuo andamento!\n\nRiceverai inoltre una valutazione in tempo reale del tuo stile di guida rappresentato da tre foglioline.\n\nOra goditi l'esperienza di EcoDrive!"
        }

    }
}