package com.silvered.ecodrive

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.silvered.ecodrive.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: SignInClient

    private val getActivityResult =
        registerForActivityResult(StartIntentSenderForResult()) { result: ActivityResult? ->
            if (result != null) {
                this.handleResultGoogle(result)
            }else{
                showError(null)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null) {
            goToMainActivity()
        }


        googleSignInClient = Identity.getSignInClient(this)

        binding.signInGoogle.setOnClickListener {
            binding.progressLogin.visibility = View.VISIBLE
            binding.signInGoogle.visibility = View.GONE
            manageGoogleFlow(true)
        }
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
                CommonStatusCodes.CANCELED -> {}
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

                    if (!dataSnapshot.exists()) {
                        database.child("users").child(userId).child("email")
                            .setValue(firebaseAuth.currentUser!!.email)
                        database.child("users").child(userId).child("fullname").setValue(name)
                        if (picurl != null)
                            database.child("users").child(userId).child("picurl")
                                .setValue(picurl.toString())
                    }

                    goToMainActivity()

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
}