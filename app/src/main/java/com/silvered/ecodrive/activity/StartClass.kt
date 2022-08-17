package com.silvered.ecodrive.activity

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.material.color.DynamicColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.silvered.ecodrive.services.NotificationReceiver
import java.util.*


class StartClass:Application(), OnMapsSdkInitializedCallback {

    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this).cancelAllWork()
        DynamicColors.applyToActivitiesIfAvailable(this)
        MapsInitializer.initialize(applicationContext, Renderer.LATEST, this)
        val sharedPref = getSharedPreferences("info", MODE_PRIVATE)
        val editor = sharedPref.edit()
        val now = System.currentTimeMillis()
        editor.putString("lastOpen", now.toString()).commit()
        scheduleNot()


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

    private fun scheduleNot() {

        val calendar = Calendar.getInstance()

        calendar[Calendar.HOUR_OF_DAY] = 14
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext,NotificationReceiver::class.java)
        intent.putExtra("title","EcoDrive")
        intent.putExtra("body","Ehi! Sembra che non stai utilizzando l'app da un po', fai una sessione di guida ora!")
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(applicationContext,66,intent, pendingFlags)
        alarmManager.setRepeating(AlarmManager.RTC,calendar.timeInMillis,AlarmManager.INTERVAL_DAY,pendingIntent)

    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
            Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
        }
    }

}