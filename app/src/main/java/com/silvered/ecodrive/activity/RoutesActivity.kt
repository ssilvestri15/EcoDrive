package com.silvered.ecodrive.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.color.MaterialColors
import com.google.gson.Gson
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.ActivityRoutesBinding

class RoutesActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val PESO_MEDIO = "pesoMedio"
        val KM_PERCORSI = "kmPercorsi"
        val VEL_MEDIA = "velMedia"
        val PUNTEGGIO = "punteggio"
        val POLYLINEOPTIONS = "poly"
        val DATA = "data"
    }

    private interface RouteInterfaceCallback {
        fun onDataReady(polylineOptions: PolylineOptions)
    }

    private lateinit var binding: ActivityRoutesBinding
    private var polylineOptions: PolylineOptions? = null
    private var listener: RouteInterfaceCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val peso = intent.getFloatExtra(PESO_MEDIO, 0f)
        val km = intent.getFloatExtra(KM_PERCORSI, 0f)
        val vel = intent.getFloatExtra(VEL_MEDIA, 0f)
        val punteggio = intent.getFloatExtra(PUNTEGGIO, 0f)
        val data = intent.getStringExtra(DATA)

        val polyString = intent.getStringExtra(POLYLINEOPTIONS)


        if (polyString == null) {
            Log.e("POLY", "NULL")
            goToRoutesWithoutMap(data!!, km, vel, peso, punteggio)
        } else {

            polylineOptions = Gson().fromJson(polyString, PolylineOptions::class.java)

            if (polylineOptions == null) {
                Log.e("POLYOPT", "NULL")
                goToRoutesWithoutMap(data!!, km, vel, peso, punteggio)
            } else {
                Log.e("POLYOPT", "NOT NULL")
                listener?.onDataReady(polylineOptions!!)
            }
        }

        binding = ActivityRoutesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        val colorPrimary = MaterialColors.getColor(
            this,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(this, R.color.pink)
        )

        binding.layoutResoconto.dataResocoto.text = data
        binding.layoutResoconto.kmTvEndSession.text = km.toInt().toString()
        binding.layoutResoconto.punteggioTvEndSession.text = punteggio.toInt().toString()
        binding.layoutResoconto.velTvEndSession.text = "${vel.toInt()} km\\h"

        when (peso) {
            in 0f..0.33f -> {
                ImageViewCompat.setImageTintList(
                    binding.layoutResoconto.foglia1,
                    ColorStateList.valueOf(colorPrimary)
                )
            }
            in 0.34f..0.66f -> {
                ImageViewCompat.setImageTintList(
                    binding.layoutResoconto.foglia1,
                    ColorStateList.valueOf(colorPrimary)
                )
                ImageViewCompat.setImageTintList(
                    binding.layoutResoconto.foglia2,
                    ColorStateList.valueOf(colorPrimary)
                )
            }
            else -> {
                ImageViewCompat.setImageTintList(
                    binding.layoutResoconto.foglia1,
                    ColorStateList.valueOf(colorPrimary)
                )
                ImageViewCompat.setImageTintList(
                    binding.layoutResoconto.foglia2,
                    ColorStateList.valueOf(colorPrimary)
                )
                ImageViewCompat.setImageTintList(
                    binding.layoutResoconto.foglia3,
                    ColorStateList.valueOf(colorPrimary)
                )
            }
        }

        if (!isGamified())
            binding.layoutResoconto.punteggioLayout.visibility = View.GONE

    }

    override fun onMapReady(googleMap: GoogleMap) {

        if (polylineOptions == null) {
            listener = object : RouteInterfaceCallback {
                override fun onDataReady(polylineOptions: PolylineOptions) {
                    onMapReady(googleMap)
                }
            }
            return
        }

        listener = null
        val polyline = googleMap.addPolyline(polylineOptions!!)

        val points = polylineOptions!!.points

        if (points.isEmpty())
            return

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

    private fun goToRoutesWithoutMap(
        data: String,
        km: Float,
        vel: Float,
        peso: Float,
        punteggio: Float
    ) {
        val intent = Intent(this@RoutesActivity, RoutesActivityCarla::class.java)
        intent.putExtra(DATA, data)
        intent.putExtra(KM_PERCORSI, km)
        intent.putExtra(VEL_MEDIA, vel)
        intent.putExtra(PESO_MEDIO, peso)
        intent.putExtra(PUNTEGGIO, punteggio)
        startActivity(intent)
        finish()
    }

    private fun isGamified(): Boolean {
        val sharedPreferences = getSharedPreferences("info", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isGamified", false)
    }
}