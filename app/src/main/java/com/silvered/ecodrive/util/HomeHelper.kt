package com.silvered.ecodrive.util

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.silvered.ecodrive.HomeFragment
import java.math.BigDecimal

object HomeHelper {

    interface HomeHelperCallback {
        fun onDataReady()
        fun onError(error: DatabaseError)
    }

    var needToUpdate: Boolean? = null
    var listChart: ArrayList<BigDecimal>? = null
    var positionLocal: Long? = null
    var positionGlobal: Long? = null
    private var listener: HomeHelperCallback? = null

    private val TAG = "HomeHelper"

    /**
     *
     * Funziona a cascata
     *
     * Inizializza la UI
     * Prende i dati dei viaggi
     * Prende la posizione in classifica locale
     * Prende la posizione in classifica globale
     * Aggiorna la ui
     *
     */
    fun getData(uid: String, nazione: String, listener: HomeHelperCallback?) {

        Thread {

            Log.d(TAG,"Inzio a prendere i dati")

            if (listener != null)
                setListener(listener)

            val database = FirebaseDatabase.getInstance().getReference("users/$uid/routes")
            database.limitToLast(5).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (!dataSnapshot.exists()) {
                        listChart = ArrayList()
                        positionLocal = -1
                        positionGlobal = -1
                        needToUpdate = false
                        listener?.onDataReady()
                        return
                    }

                    handleRoutesData(uid, nazione, dataSnapshot)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    listener?.onError(databaseError)
                }
            })
        }.start()
    }

    private fun handleRoutesData(uid: String, nazione: String, dataSnapshot: DataSnapshot) {

        Log.d(TAG,"cerco i viaggi")

        val list: ArrayList<BigDecimal> = ArrayList()

        for (child in dataSnapshot.children) {
            val punteggioD = child.child("punteggio").getValue(Double::class.java)
            if (punteggioD != null) {
                val punteggio = punteggioD.toBigDecimal()
                list.add(punteggio)
            }
        }

        listChart = list

        getRankingLocal(uid,nazione)
    }

    private fun getRankingLocal(uid: String, nazione: String) {

        Log.d(TAG,"cerco la posizione locale")

        FirebaseDatabase.getInstance().getReference("ranking/$nazione")
            .orderByChild("punteggioMedio")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val list = ArrayList<HomeFragment.UserScore>()

                    for (snap in snapshot.children) {
                        val punt = snap.child("punteggioMedio").getValue(Int::class.java)
                        list.add(
                            HomeFragment.UserScore(
                                snap.key.toString(),
                                snap.child("name").getValue(String::class.java).toString(),
                                punt!!
                            )
                        )
                    }

                    list.sortByDescending { it.score }

                    positionLocal = handleRankDataLocal(list, uid)
                    getRankingGlobal(uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    listener?.onError(error)
                }
            })
    }

    private fun getRankingGlobal(uid: String) {

        Log.d(TAG,"cerco la posizione globale")

        FirebaseDatabase.getInstance().getReference("ranking/global")
            .orderByChild("punteggioMedio")
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    positionGlobal = handleRankDataGlobal(snapshot, uid)
                    needToUpdate = false
                    listener?.onDataReady()
                }

                override fun onCancelled(error: DatabaseError) {
                    listener?.onError(error)
                }
            })
    }


    private fun handleRankDataLocal(list: ArrayList<HomeFragment.UserScore>, uid: String): Long {

        var indexLocal: Long = 1
        var foundP = false
        for (snap in list) {
            if (snap.id == uid) {
                foundP = true
                break
            }
            indexLocal++
        }

        if (!foundP)
            indexLocal = list.size.toLong()

        return indexLocal
    }

    private fun handleRankDataGlobal(snapshot: DataSnapshot, uid: String): Long {

        var indexLocal = snapshot.childrenCount
        var foundP = false
        for (snap in snapshot.children) {
            if (snap.key.equals(uid)) {
                foundP = true
                break
            }
            indexLocal--
        }

        if (!foundP)
            indexLocal = snapshot.childrenCount

        return indexLocal
    }

    private fun setListener(listener: HomeHelperCallback) {
        this.listener = listener
    }

    fun removeListener() {
        if (listener != null)
            listener = null
    }

}