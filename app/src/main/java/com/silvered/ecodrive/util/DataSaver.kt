package com.silvered.ecodrive.util

import android.content.SharedPreferences
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.silvered.ecodrive.util.helpers.HomeHelper
import com.silvered.ecodrive.util.helpers.ProfileHelper
import com.silvered.ecodrive.util.helpers.RankHelper

class DataSaver {

    companion object {

        fun saveData(
            peso: Float,
            km: Float,
            vel: Float,
            punteggio: Float,
            polyString: String?,
            sp: SharedPreferences,
            onSuccess: () -> Unit
        ) {

            val node: MutableMap<String, Any> = HashMap()
            node["pesoStileDiGuida"] = peso
            node["kmPercorsi"] = km
            node["velMedia"] = vel
            node["punteggio"] = punteggio

            if (polyString != null)
                node["poly"] = polyString

            val firebaseAuth = FirebaseAuth.getInstance()
            val database = FirebaseDatabase.getInstance().reference
            val userInDB = database.child("users/${firebaseAuth.currentUser?.uid}")
            val temp = System.currentTimeMillis().toString()
            val newNode = userInDB.child("routes")
            newNode.child(temp).setValue(node).addOnSuccessListener {

                userInDB.addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        var sum =
                            snapshot.child("sommaPunti").getValue(String::class.java)?.toBigDecimal()
                        var numTot = snapshot.child("numeroViaggi").getValue(Int::class.java)
                        var nazione = snapshot.child("nazione").getValue(String::class.java)
                        var regione = snapshot.child("regione").getValue(String::class.java)

                        val level = snapshot.child("level").getValue(Int::class.java)
                        val fullname = snapshot.child("fullname").getValue(String::class.java)
                        var record = snapshot.child("record").getValue(Int::class.java)
                        var picURL = snapshot.child("picurl").getValue(String::class.java)

                        if (nazione == null || nazione == "" || regione == null || regione == "") {
                            nazione = sp.getString("nazione", "")
                            regione = sp.getString("regione", "")
                            if (nazione == null || nazione == "" || regione == null || regione == "") {
                                //Eccezione
                                return
                            }
                        }

                        if (record == null)
                            record = punteggio.toInt()

                        if (picURL == null)
                            picURL = ""

                        if (sum != null && numTot != null) {

                            sum += punteggio.toBigDecimal()
                            numTot++

                            val media = (sum / numTot.toBigDecimal()).toInt()

                            userInDB.child("sommaPunti").setValue(sum.toString())
                            userInDB.child("numeroViaggi").setValue(numTot)
                            userInDB.child("punteggioMedio").setValue(media)

                            if (record <= punteggio) {
                                record = punteggio.toInt()
                                userInDB.child("record").setValue(record)
                            }

                            database.child("ranking/$nazione/${firebaseAuth.currentUser?.uid}/punteggioMedio")
                                .setValue(media)
                            database.child("ranking/$nazione/${firebaseAuth.currentUser?.uid}/name")
                                .setValue(firebaseAuth.currentUser?.displayName)
                            database.child("ranking/$nazione/${firebaseAuth.currentUser?.uid}/regione")
                                .setValue(regione)
                            database.child("ranking/$nazione/${firebaseAuth.currentUser?.uid}/picurl")
                                .setValue(picURL)
                            database.child("ranking/$regione/${firebaseAuth.currentUser?.uid}/punteggioMedio")
                                .setValue(media)
                            database.child("ranking/$regione/${firebaseAuth.currentUser?.uid}/name")
                                .setValue(firebaseAuth.currentUser?.displayName)
                            database.child("ranking/$regione/${firebaseAuth.currentUser?.uid}/picurl")
                                .setValue(picURL)

                            RankHelper.needToUpdate.value = true
                            HomeHelper.needToUpdate = true
                            ProfileHelper.needToUpdate = true

                            onSuccess()

                        } else {
                            Log.d("SSSS", "Qualcosa non va")
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                        //HANDLE EXCEPTION
                    }
                })
            }

        }

    }

}