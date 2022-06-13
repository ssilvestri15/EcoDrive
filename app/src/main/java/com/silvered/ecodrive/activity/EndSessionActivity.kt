package com.silvered.ecodrive.activity

import android.R
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.silvered.ecodrive.databinding.ActivityEndSessionBinding
import com.silvered.ecodrive.util.helpers.ErrorHelper
import com.silvered.ecodrive.util.helpers.HomeHelper
import com.silvered.ecodrive.util.helpers.ProfileHelper
import com.silvered.ecodrive.util.helpers.RankHelper
import kotlin.random.Random.Default.nextInt


class EndSessionActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val PESO_MEDIO = "pesoMedio"
        val KM_PERCORSI = "kmPercorsi"
        val VEL_MEDIA = "velMedia"
        val PUNTEGGIO = "punteggio"
        val POLYLINEOPTIONS = "poly"
    }

    private var polyString: String? = null
    private lateinit var binding: ActivityEndSessionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEndSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent == null) {
            ErrorHelper.showError(this, "OPS", false) { finish() }
            return
        }

        val peso = intent.getFloatExtra(PESO_MEDIO, 0f)
        val km = intent.getFloatExtra(KM_PERCORSI, 0f)
        val vel = intent.getFloatExtra(VEL_MEDIA, 0f)
        val punteggio = intent.getFloatExtra(PUNTEGGIO, 0f)

        polyString = intent.getStringExtra(POLYLINEOPTIONS)

        if (polyString != null) {
            val mapFragment = supportFragmentManager.findFragmentById(com.silvered.ecodrive.R.id.map) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)
        }

        setupUI(peso, km, vel, punteggio, polyString)

    }

    private fun setupUI(
        peso: Float,
        km: Float,
        vel: Float,
        punteggio: Float,
        polyString: String?
    ) {

        val colorPrimary = MaterialColors.getColor(
            this,
            R.attr.colorPrimary,
            ContextCompat.getColor(this, com.silvered.ecodrive.R.color.pink)
        )

        when (peso) {
            in 0f..0.33f -> {
                ImageViewCompat.setImageTintList(binding.foglia1, ColorStateList.valueOf(colorPrimary))
            }
            in 0.34f..0.66f -> {
                ImageViewCompat.setImageTintList(binding.foglia1, ColorStateList.valueOf(colorPrimary))
                ImageViewCompat.setImageTintList(binding.foglia2, ColorStateList.valueOf(colorPrimary))
            }
            else -> {
                ImageViewCompat.setImageTintList(binding.foglia1, ColorStateList.valueOf(colorPrimary))
                ImageViewCompat.setImageTintList(binding.foglia2, ColorStateList.valueOf(colorPrimary))
                ImageViewCompat.setImageTintList(binding.foglia3, ColorStateList.valueOf(colorPrimary))
            }
        }

        binding.kmTvEndSession.text = km.toInt().toString()
        binding.velTvEndSession.text = vel.toInt().toString()
        binding.punteggioTvEndSession.text = punteggio.toInt().toString()

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
                        val sp = getSharedPreferences("info", MODE_PRIVATE)
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

                        binding.syncLayoutEndSession.visibility = View.GONE
                        binding.backHomeEndSession.visibility = View.VISIBLE
                        binding.backHomeEndSession.setOnClickListener {
                            checkMagic(level, fullname)
                        }

                    } else {
                        Log.d("SSSS", "Qualcosa non va")
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    //HANDLE EXCEPTION
                }
            })
        }

        if (!isGamified())
            binding.punteggioLayout.visibility = View.GONE


    }

    private fun checkMagic(level: Int?, fullname: String?) {

        HomeHelper.makeHomeUpdate()

       /* if (level != null && level > 2) {

            val limitList = getLimits(level)

            val limit = (nextInt(0, limitList[0]))
            val user = (nextInt(0, limitList[1]))

            if (user > limit) {
                val intent = Intent(this@EndSessionActivity, WinnerActivity::class.java)
                intent.putExtra(WinnerActivity.LEVEL,level)
                intent.putExtra(WinnerActivity.NAME,fullname)
                startActivity(intent)
            }

        }*/

        finish()
    }

    private fun getLimits(level: Int): ArrayList<Int> {

        var list = ArrayList<Int>()

        if (level <= 3)
            list = arrayListOf(500,255)

        if (level in 4..5)
            list = arrayListOf(500,125)

        if (level > 5)
            list = arrayListOf(500,75)

        return list

    }

    override fun onMapReady(googleMap: GoogleMap) {

        if (polyString == null)
            return

        val polylineOptions: PolylineOptions = Gson().fromJson(polyString, PolylineOptions::class.java)?: return

        binding.messageEndSession.visibility = View.GONE
        binding.mapCardView.visibility = View.VISIBLE

        val polyline = googleMap.addPolyline(polylineOptions)

        val points = polylineOptions.points

        val startPoint = points.first()

        polyline.startCap = RoundCap()
        polyline.endCap = RoundCap()

        polyline.color = Color.BLUE

        val cameraPosition = CameraPosition.Builder()
            .target(startPoint)
            .zoom(17f)
            .bearing(90f)
            .tilt(30f)
            .build()

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

    }

    private fun isGamified(): Boolean {
        val sharedPreferences = getSharedPreferences("info", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isGamified", false)
    }

}