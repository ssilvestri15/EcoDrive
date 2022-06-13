package com.silvered.ecodrive.util.helpers

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.silvered.ecodrive.util.CustomObjects
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object ProfileHelper {

    var needToUpdate: Boolean = true

    var listRoutes: MutableLiveData<ArrayList<CustomObjects.Route>?> = MutableLiveData(null)

    fun resetAll() {
        needToUpdate = true
        listRoutes.value = null
    }

    fun updateData(user: FirebaseUser) {
        FirebaseDatabase.getInstance().getReference("users/${user.uid}/routes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (!dataSnapshot.exists()) {
                        listRoutes.value = ArrayList()
                        return
                    }

                    val list: ArrayList<CustomObjects.Route> = ArrayList()

                    for (snapshot in dataSnapshot.children) {

                        val date: String =
                            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(
                                Timestamp(snapshot.key!!.toLong())
                            )
                        val km = snapshot.child("kmPercorsi").getValue(Float::class.java)
                        val poly = snapshot.child("poly").getValue(String::class.java)
                        val sdg = snapshot.child("pesoStileDiGuida").getValue(Float::class.java)
                        val punti = snapshot.child("punteggio").getValue(Float::class.java)
                        val vel = snapshot.child("velMedia").getValue(Float::class.java)

                        if (vel != null && km != null && sdg != null && punti != null)
                            list.add(CustomObjects.Route(date, km, vel, sdg, poly, punti))

                    }

                    list.reverse()

                    listRoutes.value = list
                    needToUpdate = false

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    listRoutes.value = ArrayList()
                }
            })
    }

}