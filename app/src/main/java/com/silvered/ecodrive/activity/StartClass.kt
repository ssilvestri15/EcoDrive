package com.silvered.ecodrive.activity

import android.app.Application
import android.util.Log
import androidx.work.WorkManager
import com.google.android.material.color.DynamicColors
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class StartClass:Application(), OnMapsSdkInitializedCallback {

    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this).cancelAllWork()
        DynamicColors.applyToActivitiesIfAvailable(this)
        MapsInitializer.initialize(applicationContext, Renderer.LATEST, this)
        val sharedPref = getSharedPreferences("info", MODE_PRIVATE)

        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null) {

            Firebase.database.reference.child("users").child(firebaseAuth.currentUser!!.uid).addListenerForSingleValueEvent(object:
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (!dataSnapshot.exists()) {
                        firebaseAuth.signOut()
                        sharedPref.edit().clear().commit()
                        return
                    }

                    val nazione = dataSnapshot.child("nazione").getValue(String::class.java)
                    val regione = dataSnapshot.child("regione").getValue(String::class.java)
                    var isGamified = dataSnapshot.child("isGamified").getValue(Boolean::class.java)

                    if (isGamified == null)
                        isGamified = (firebaseAuth.currentUser!!.uid.filter { it.isDigit() }).toInt()%2 == 0

                    val editor = sharedPref.edit()
                    editor.putString("nazione", nazione)
                    editor.putString("regione",regione)
                    editor.putBoolean("isGamified",isGamified)

                    editor.remove("session")

                    editor.commit()

                }

                override fun onCancelled(error: DatabaseError) {
                    firebaseAuth.signOut()
                    sharedPref.edit().clear().commit()
                }

            })

        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
            Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
        }
    }

}