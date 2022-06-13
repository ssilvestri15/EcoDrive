package com.silvered.ecodrive.util.helpers

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.silvered.ecodrive.util.CustomObjects

object RankHelper {

    var needToUpdate: MutableLiveData<Boolean> = MutableLiveData(true)

    var listGlobal: ArrayList<CustomObjects.UserScore>? = null
    var listLocal: ArrayList<CustomObjects.UserScore>? = null

    fun resetAll() {
        listGlobal = null
        needToUpdate.value = true
        listLocal = null
    }

    fun updateData(user: FirebaseUser, nazione: String, regione: String) {
        FirebaseDatabase.getInstance().getReference("ranking/$nazione")
            .orderByChild("punteggioMedio")
            .limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    listGlobal = ArrayList()
                    listLocal = ArrayList()

                    listGlobal = getList(user,snapshot,false)


                    FirebaseDatabase.getInstance().getReference("ranking/$regione")
                        .orderByChild("punteggioMedio")
                        .limitToLast(10)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                listLocal = getList(user,snapshot, true)
                                needToUpdate.value = false
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d("SSS 2", error.message)
                            }
                        })

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("SSS 1", error.message)
                }
            })

    }

    private fun getList(
        user: FirebaseUser,
        snapshot: DataSnapshot,
        isLocal: Boolean
    ): ArrayList<CustomObjects.UserScore> {

        val list = ArrayList<CustomObjects.UserScore>()

        var found = false
        for (snap in snapshot.children) {
            val punt = snap.child("punteggioMedio").getValue(Int::class.java)
            val picURL = snap.child("picurl").getValue(String::class.java)

            if (snap.key == user.uid)
                found = true

            if (punt != null && picURL != null)
                list.add(
                    CustomObjects.UserScore(
                        -1,
                        picURL,
                        snap.key.toString(),
                        snap.child("name").getValue(String::class.java).toString(),
                        punt
                    )
                )
        }

        if (HomeHelper.positionLocal != null && HomeHelper.positionGlobal != null && HomeHelper.punteggioMedio != null && HomeHelper.picUrl != null && found.not()) {

            if (isLocal)
                list.add(
                    CustomObjects.UserScore(
                        HomeHelper.positionLocal!!.toInt(),
                        HomeHelper.picUrl!!,
                        user.uid,
                        user.displayName.toString(),
                        HomeHelper.punteggioMedio!!.toInt()
                    )
                )
            else
                list.add(
                    CustomObjects.UserScore(
                        HomeHelper.positionGlobal!!.toInt(),
                        HomeHelper.picUrl!!,
                        user.uid,
                        user.displayName.toString(),
                        HomeHelper.punteggioMedio!!.toInt()
                    )
                )
        }

        list.sortByDescending { it.score }

        return list
    }

}