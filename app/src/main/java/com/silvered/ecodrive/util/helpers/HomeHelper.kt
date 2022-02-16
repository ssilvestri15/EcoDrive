package com.silvered.ecodrive.util.helpers

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.silvered.ecodrive.fragments.HomeFragment
import com.silvered.ecodrive.util.CustomObjects
import java.math.BigDecimal

object HomeHelper {

    interface HomeHelperCallback {
        fun onDataReady()
        fun onError(error: DatabaseError)
    }

    interface HomeHelperUpdate {
        fun onNeedToUpdate()
    }

    var record: Int = 0
    var needToUpdate: Boolean? = null

    var listChart: ArrayList<BigDecimal>? = null
    var positionLocal: Long? = null
    var positionGlobal: Long? = null
    var punteggioMedio: Int? = null
    var stileDiGuida: Float? = null
    var numViaggi: Int? = null
    var levelName: String = ""
    var picUrl: String? = null
    private var listener: HomeHelperCallback? = null
    private var listenerUpdate: HomeHelperUpdate? = null
    private val database = FirebaseDatabase.getInstance()

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
    fun getData(uid: String, nazione: String, regione: String, listener: HomeHelperCallback?) {

        Thread {

            Log.d(TAG, "Inzio a prendere i dati")

            if (listener != null)
                setListener(listener)

            getUserData(uid, nazione, regione)


        }.start()
    }

    private fun getUserData(uid: String, nazione: String, regione: String) {

        database.getReference("users/$uid")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (!dataSnapshot.exists()) {
                        //Eccezione
                        return
                    }

                    handleUserData(dataSnapshot, uid, nazione, regione)

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    listener?.onError(databaseError)
                }
            })


    }

    private fun handleUserData(
        dataSnapshot: DataSnapshot,
        uid: String,
        nazione: String,
        regione: String
    ) {

        punteggioMedio = dataSnapshot.child("punteggioMedio").getValue(Int::class.java)
        if (punteggioMedio == null)
            punteggioMedio = 0

        var level = dataSnapshot.child("level").getValue(Int::class.java)

        if (level == null)
            level = 1

        if (punteggioMedio!! <= 10)
            level = 1

        if (punteggioMedio!! in 11..25)
            level = 2

        if (punteggioMedio!! in 26..35)
            level = 3

        if (punteggioMedio!! in 36..50)
            level = 4

        if (punteggioMedio!! in 51..80)
            level = 5

        if (punteggioMedio!! > 80)
            level = 6

        database.getReference("users/$uid").child("level").setValue(level)

        when (level) {
            1 -> levelName = "Principiante"
            2 -> levelName = "Amatore"
            3 -> levelName = "Intermedio"
            4 -> levelName = "Avanzato"
            5 -> levelName = "Pro"
            6 -> levelName = "Campione"
        }

        val recordDB = dataSnapshot.child("record").getValue(Int::class.java)

        if (recordDB != null)
            record = recordDB

        val pic = dataSnapshot.child("picurl").getValue(String::class.java)

        if (pic != null)
            picUrl = pic

        val viaggi = dataSnapshot.child("numeroViaggi").getValue(Int::class.java)

        if (viaggi != null)
            numViaggi = viaggi

        getRoutes(uid, nazione, regione)
    }

    private fun getRoutes(uid: String, nazione: String, regione: String) {
        database.getReference("users/$uid/routes").limitToLast(5)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (!dataSnapshot.exists()) {
                        listChart = ArrayList()
                        positionLocal = -1
                        positionGlobal = -1
                        stileDiGuida = 0f
                        needToUpdate = false
                        listener?.onDataReady()
                        return
                    }

                    handleRoutesData(uid, nazione, regione, dataSnapshot)
                    handleStileDiGuida(dataSnapshot)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    listener?.onError(databaseError)
                }
            })
    }

    private fun handleStileDiGuida(dataSnapshot: DataSnapshot) {

        var punteggio = 0f
        var counter = 0

        for (child in dataSnapshot.children) {
            val sdg = child.child("pesoStileDiGuida").getValue(Long::class.java)
            if (sdg != null) {
                punteggio += sdg
                counter++
            }
        }

        if (punteggio == 0f && counter == 0) {
            stileDiGuida = 0f
            return
        }

        stileDiGuida = (punteggio/counter)

    }

    private fun handleRoutesData(
        uid: String,
        nazione: String,
        regione: String,
        dataSnapshot: DataSnapshot
    ) {

        Log.d(TAG, "cerco i viaggi")

        val list: ArrayList<BigDecimal> = ArrayList()

        for (child in dataSnapshot.children) {
            val punteggioD = child.child("punteggio").getValue(Double::class.java)
            if (punteggioD != null) {
                val punteggio = punteggioD.toBigDecimal()
                list.add(punteggio)
            }
        }

        listChart = list

        getRankingLocal(uid, nazione, regione)
    }

    private fun getRankingLocal(uid: String, nazione: String, regione: String) {

        Log.d(TAG, "cerco la posizione locale")

        FirebaseDatabase.getInstance().getReference("ranking/$regione")
            .orderByChild("punteggioMedio")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val list = ArrayList<CustomObjects.UserScore>()

                    for (snap in snapshot.children) {
                        val punt = snap.child("punteggioMedio").getValue(Int::class.java)
                        val picURL = snap.child("picurl").getValue(String::class.java)
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

                    list.sortByDescending { it.score }

                    positionLocal = handleRankDataLocal(list, uid)
                    getRankingGlobal(uid, nazione)
                }

                override fun onCancelled(error: DatabaseError) {
                    listener?.onError(error)
                }
            })
    }

    private fun getRankingGlobal(uid: String, nazione: String) {

        Log.d(TAG, "cerco la posizione globale")

        FirebaseDatabase.getInstance().getReference("ranking/$nazione")
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


    private fun handleRankDataLocal(list: ArrayList<CustomObjects.UserScore>, uid: String): Long {

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
        HomeHelper.listener = listener
    }

    fun removeListener() {

        if (listener != null)
            listener = null

        if (listenerUpdate != null)
            listener = null
    }

    fun makeHomeUpdate() {
        listenerUpdate?.onNeedToUpdate()
    }

    fun setupUpdateListner(listener: HomeHelperUpdate) {
        listenerUpdate = listener
    }

    fun resetAll() {
        record = 0
        needToUpdate = null

        listChart = null
        positionLocal = null
        positionGlobal = null
        punteggioMedio = null
        numViaggi = null
        levelName = ""
        picUrl = null
        listener = null
        listenerUpdate = null
    }

}