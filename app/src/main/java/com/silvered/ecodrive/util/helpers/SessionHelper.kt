package com.silvered.ecodrive.util.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class SessionHelper {

    interface SessionHelperCallback {
        fun onSpeedUpdated(currentSpeed: String)
        fun onDataUpdated(
            stileDiGuidaNow: Float,
            stileDiGuida: Float,
            diffspeed: Int,
            points: Int,
            kmPercorsi: Float
        ) //Aggiungere parametri
    }

    private var listener: SessionHelperCallback? = null
    private var counter = 0
    private var previuspeed = 0f

    private var kmpercosi = 0f
    private var mPrec = 0f

    private var counterV = 1 //Numero delle velocità ricevute
    private var speedS = 0f //Somma delle velocità ricevute
    private var velM = 0f //Velocità media

    private var startTimeAC: Instant? = null //Data iniziale andamento costante
    private var startTimeFermo: Instant? = null //Data iniziale fermo o nel traffico


    private var altitudinePrec: Int = 0
    private var diffAltitudinePrec: Int = 0
    private var startTimeSalita: Instant? = null //Data iniziale salita

    private var punteggio = 0f

    private var secondiAndamentoCostantePrec = 0
    private var secondiAndamentoCostanteTot: Long = 0
    private var secondiAndamentoCostanteTotPrec: Long = 0

    private var stileDiGuida = 1f
    private var stileDiGuidaNow = 1f

    private var maxV = Int.MIN_VALUE.toFloat()
    private var minV = Int.MAX_VALUE.toFloat()

    private var maxVM = Int.MIN_VALUE.toFloat()
    private var minVM = Int.MAX_VALUE.toFloat()
    private var sdgS = 0f
    private var counterS = 0
    private var sdgM = 0f

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var polylineOption: PolylineOptions

    fun updateData(message: String) {

        try {

            val temp = message.split(".")
            val speed = temp[0] + '.' + temp[1]

            val currentSpeed = speed.toFloat()
            listener?.onSpeedUpdated(String.format("%.0f", currentSpeed))

            updateVelMedia(currentSpeed)


            var diffspeed = 0


            if (currentSpeed > previuspeed + 2 || currentSpeed < previuspeed - 2) {

                if (previuspeed < 1f)
                    previuspeed = 1f

                if (currentSpeed > 0f && previuspeed > 0f)
                    diffspeed = (((currentSpeed - previuspeed) / previuspeed) * 100).toInt()

            }

            updatePunteggio(currentSpeed, diffspeed)

            previuspeed = currentSpeed
            counter = 0

            listener?.onDataUpdated(
                getPesoSDG(stileDiGuidaNow, false),
                getPeso(velM),
                diffspeed,
                punteggio.toInt(),
                kmpercosi
            )


        } catch (e: Exception) {
            Log.e("TRYCATCH ERROR", "Error: ${e.message}")
        }

    }

    private fun getPesoSDG(stileDiGuidaNow: Float, final: Boolean): Float {

        var temp = maxV - minV

        if (final)
            maxVM - minVM

        if (temp == 0f)
            temp = 1f

        var normalized = ((stileDiGuidaNow - minV) / temp) * 10000
        if (final)
            normalized /= 1000

        if (normalized > 1)
            normalized = 1f

        return normalized
    }

    private fun updatePunteggio(currentSpeed: Float, diffAccellPerc: Int) {

        val pesoAM: Float = getPeso(velM) //Peso Andatura media
        val pesoAC: Float = getPeso(currentSpeed) //Peso andatura corrente
        var secondiAndamentoCostante: Long = 0
        var secondiFermo: Long = 0

        if (currentSpeed <= previuspeed + 5 && currentSpeed >= previuspeed - 5) {
            secondiAndamentoCostante = getSecAndamentoCostante(currentSpeed)
        } else {
            startTimeAC = null
        }

        if (currentSpeed <= 5) {
            secondiFermo = getSecFermo()
        } else {
            startTimeFermo = null
        }

        //var secondiSalita = getSecSalita(altitudine)
        var secondiSalita = 0

        val diff = abs(diffAccellPerc)

        val mPercosiAC: Float =
            (currentSpeed / 3.6f) * secondiAndamentoCostante //m percorsi a quell'andatura costante

        if (mPercosiAC == 0f)
            mPrec = 0f

        kmpercosi += (mPercosiAC - mPrec) / 1000
        mPrec = mPercosiAC

        val pesoKm = getPesoKm(kmpercosi)
        stileDiGuidaNow = (pesoAC * ((mPercosiAC / 1000f) + diff))

        if (maxV < stileDiGuidaNow)
            maxV = stileDiGuidaNow

        if (minV > stileDiGuidaNow)
            minV = stileDiGuidaNow

        //(pesoAndamentoMedio * ((pesoAndamentoCorrente * kmpercorsiConLaVelocitaCorrente) + differenzaPercentualeConVelocitàPrecedente)))
        stileDiGuida = pesoAM * (stileDiGuidaNow)
        updateSDG(stileDiGuida)

        if (secondiAndamentoCostante == (0).toLong())
            secondiAndamentoCostantePrec = 0

        secondiAndamentoCostanteTot += (secondiAndamentoCostante - secondiAndamentoCostantePrec)


        val pesoKmBassi =
            (pesoKm * kmpercosi) //Il peso dei kilometrti percorsi dall'utente, se è < 1 allora viene penalizzato
        val daLevare =
            (stileDiGuida + (secondiFermo / 10) + pesoKmBassi + secondiSalita + kmpercosi) //Punti di penalizzazione
        val res =
            (secondiAndamentoCostanteTot - secondiAndamentoCostanteTotPrec) - daLevare //L'utente guadagna punti in base ai secondi che guida in maniera costante
        secondiAndamentoCostanteTotPrec = secondiAndamentoCostanteTot
        punteggio += res / 100 // effettuo la divisione /100 così per non far uscire valori troppo elevati
        if (punteggio < 0)
            punteggio = 0f

    }

    private fun updateSDG(stileDiGuida: Float) {

        sdgS += stileDiGuida
        counterS++
        sdgM = sdgS / counterS

        if (maxVM < sdgM)
            maxVM = sdgM

        if (minVM > sdgM)
            minVM = sdgM

    }

    private fun updateVelMedia(currentSpeed: Float) {
        speedS += currentSpeed
        counterV++
        velM = speedS / counterV
    }

    private fun getPeso(speed: Float): Float {

        if (speed >= 80f)
            return 0f

        val temp = ((speed / 40f) - 1)

        if (temp < 0)
            return (1 + temp)

        return (1 - temp)

    }

    private fun getSecAndamentoCostante(currentSpeed: Float): Long {

        if (currentSpeed <= 5)
            return 0

        //Se null significa che l'andatura non è costante

        if (startTimeAC == null) {
            startTimeAC = Instant.now()
            return 0
        }

        val now = Instant.now()
        val res = Duration.between(startTimeAC, now)

        return res.seconds
    }

    private fun getSecFermo(): Long {

        //Se null significa che non ero fermo
        if (startTimeFermo == null) {
            startTimeFermo = Instant.now()
            return 0
        }

        val now = Instant.now()
        val res = Duration.between(startTimeFermo, now)

        return res.seconds
    }

    private fun getSecSalita(altitudine: Int): Long {

        val diff = (altitudine - altitudinePrec)

        //Minore della differnza precedente significa che sta scendendo
        if (diff < diffAltitudinePrec) {
            diffAltitudinePrec = 0
            startTimeSalita = null
            return 0
        }

        //Continua a salire
        if (startTimeSalita == null) {
            startTimeSalita = Instant.now()
            return 0
        }

        val now = Instant.now()
        val res = Duration.between(startTimeSalita, now)

        diffAltitudinePrec = diff

        return res.seconds
    }

    private fun getPesoKm(kmpercosi: Float): Float {

        if (kmpercosi <= 0.5f)
            return 1f

        if (kmpercosi >= 1f)
            return 0f

        return (1 - ((kmpercosi / 0.5) - 1)).toFloat()

    }

    fun resetData() {

        counter = 0
        previuspeed = 0f
        kmpercosi = 0f
        mPrec = 0f

        counterV = 1
        speedS = 0f
        velM = 0f

        startTimeAC = null
        startTimeFermo = null

        altitudinePrec = 0
        diffAltitudinePrec = 0
        startTimeSalita = null

        punteggio = 0f

        secondiAndamentoCostantePrec = 0
        secondiAndamentoCostanteTot = 0
        secondiAndamentoCostanteTotPrec = 0

    }

    fun setListener(listener: SessionHelperCallback) {
        this.listener = listener
    }

    fun removeListener() {
        listener = null
    }

    fun getResults(): ArrayList<Float> {
        val pesoMedio = getPeso(velM)
        return arrayListOf(pesoMedio, kmpercosi, velM, punteggio)
    }

    fun setupGPS(activity: Activity) {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        locationRequest = LocationRequest.create().apply {

            interval = 60
            fastestInterval = 30
            maxWaitTime = 45

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val latlng = LatLng(
                    locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude
                )
                addToPoly(latlng)
                val speed = (locationResult.lastLocation.speed * 3600) / 1000
                updateData(speed.toString())
            }
        }

        startGPS(activity)
    }

    fun startGPS(activity: Activity) {

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

    }

    private fun addToPoly(latLng: LatLng) {

        if (this::polylineOption.isInitialized.not())
            polylineOption = PolylineOptions()

        polylineOption.add(latLng)
    }

    fun getTrackMaps(): PolylineOptions? {

        if (this::polylineOption.isInitialized.not())
            return null

        return polylineOption
    }

    fun stopGPS() {
        if (this::fusedLocationProviderClient.isInitialized)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    init {
        Log.d("SessionHelper", "Inizializzato")
        resetData()
    }

}
