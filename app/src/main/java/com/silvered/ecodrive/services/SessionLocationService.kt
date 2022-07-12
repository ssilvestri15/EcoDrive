package com.silvered.ecodrive.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.silvered.ecodrive.R
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class SessionLocationService : Service() {

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

    private lateinit var polylineOption: PolylineOptions
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            if (locationResult.lastLocation == null)
                return

            val latlng = LatLng(
                locationResult.lastLocation!!.latitude,
                locationResult.lastLocation!!.longitude
            )
            val speed = (locationResult.lastLocation!!.speed * 3600) / 1000

            addToPoly(latlng)
            updateData(speed.toString())
        }
    }

    private var isFirstTime = true

    override fun onCreate() {
        super.onCreate()
        createNotification()
        requestLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {

            interval = 60
            fastestInterval = 30
            maxWaitTime = 45

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        val permission =
            (fine == PackageManager.PERMISSION_GRANTED) || (coarse == PackageManager.PERMISSION_GRANTED)

        if (permission) {

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

        }
    }

    private fun createNotification() {

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "background_channel"
        val channelName = "backgroud_position"

        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("Posizione in background")
                .setContentText("EcoDrive continuerà ad analizzare la tua posizione in background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
        /*manager.notify(1, builder.build())*/
        startForeground(1, builder.build())
        isFirstTime = false
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (this::fusedLocationProviderClient.isInitialized) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)

            val intent = Intent()

            intent.action = KEY
            intent.putExtra(RESULT_OUTPUT, getResults())
            intent.putExtra(POLY_OUTPUT, getPoly())

            LocalBroadcastManager
                .getInstance(applicationContext)
                .sendBroadcast(intent)

        }
        super.onDestroy()
    }


    private fun getResults(): FloatArray {
        val pesoMedio = getPeso(velM)
        return floatArrayOf(pesoMedio, kmpercosi, velM, punteggio)
    }

    private fun getPoly(): PolylineOptions? {
        return if (this::polylineOption.isInitialized)
            polylineOption
        else
            null
    }

    private fun addToPoly(latLng: LatLng) {

        if (this::polylineOption.isInitialized.not())
            polylineOption = PolylineOptions()

        polylineOption.add(latLng)
    }

    private fun updateData(message: String) {

        Log.d("SERVICELOCATION", "AGGIORNO")

        val intent = Intent()
        intent.action = KEY

        try {

            val temp = message.split(".")
            val speed = temp[0] + '.' + temp[1]

            val currentSpeed = speed.toFloat()

            LocalBroadcastManager
                .getInstance(applicationContext)
                .sendBroadcast(
                    intent.putExtra(
                        SPEED_TEXT,
                        String.format("%.0f", currentSpeed)
                    )
                )

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


            intent.putExtra(
                STILE_DI_GUIDA_NOW,
                getPesoSDG(stileDiGuidaNow, false)
            )
            intent.putExtra(STILE_DI_GUIDA, getPeso(velM))
            intent.putExtra(DIFF_SPEED, diffspeed)
            intent.putExtra(POINTS, punteggio.toInt())
            intent.putExtra(KM_PERCORSI, kmpercosi)

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)


        } catch (e: Exception) {
            Log.e("TRYCATCH ERROR", "Error: ${e.message}")
        }


    }

    private fun getPesoSDG(stileDiGuidaNow: Float, final: Boolean): Float {

        var temp = maxV - minV

        if (final)
            temp = maxVM - minVM

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

        punteggio += res / 100 // effettuo la divisione /100 così per non far uscire valori troppo elevati
        Log.d(
            "SSSSS",
            "PUNTI OTTENUTI: ${(secondiAndamentoCostanteTot - secondiAndamentoCostanteTotPrec)}, DA LEVARE: ${daLevare} -> RES: $res, PUNTRGGIO: $punteggio"
        )
        if (punteggio < 0)
            punteggio = 0f

        if (punteggio > 100)
            punteggio = 100f

        secondiAndamentoCostanteTotPrec = secondiAndamentoCostanteTot

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

    companion object {

        val KEY = "SessionLocationService"

        //OUTPUT
        val SPEED_TEXT = "SPEED_TEXT"
        val STILE_DI_GUIDA_NOW = "STILE_DI_GUIDA_NOW"
        val STILE_DI_GUIDA = "STILE_DI_GUIDA"
        val DIFF_SPEED = "DIFF_SPEED"
        val POINTS = "POINTS"
        val KM_PERCORSI = "KM_PERCORSI"
        val RESULT_OUTPUT = "RESULT_OUTPUT"
        val POLY_OUTPUT = "POLY_OUTPUT"
    }

}