package com.silvered.ecodrive.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.ActivitySessionTestBinding
import com.silvered.ecodrive.util.Client
import com.silvered.ecodrive.util.CustomObjects
import com.silvered.ecodrive.util.helpers.ErrorHelper
import com.silvered.ecodrive.util.helpers.NsdHelper
import com.silvered.ecodrive.util.helpers.SessionHelper
import kotlinx.parcelize.Parcelize
import java.net.Socket
import java.time.Duration
import java.time.Instant
import kotlin.math.abs


class SessionActivityTest : AppCompatActivity() {

    private val TAG = "SessionActivity"

    private lateinit var sessionHelper: SessionHelper

    private lateinit var client: Client
    private var nsdHelper: NsdHelper? = null

    private lateinit var binding: ActivitySessionTestBinding

    private var colorReallyBad = -1
    private var colorBad = -1
    private var colorGood = -1
    private var colorReallyGood = -1
    private var colorPrimary = -1

    private var fakeUI = false
    private var isUIReady = false

    private lateinit var instantStart: Instant
    private var isCarlaActived = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionHelper = SessionHelper()

        isCarlaActived = getSharedPreferences("info", MODE_PRIVATE).getBoolean("carla", false)
        if (isCarlaActived) {
            nsdHelper = NsdHelper(this)
        } else {

            binding.lottieGPS.addValueCallback(
                KeyPath("**"),
                LottieProperty.COLOR_FILTER
            ) { PorterDuffColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP) }
            binding.lottieGPS.visibility = View.VISIBLE
            binding.limit.visibility = View.INVISIBLE
            isUIReady = true
            checkLocationPermission()
        }

        Log.d(TAG, "CARLA: $isCarlaActived")

        getColors()
    }


    private fun getColors() {
        colorReallyBad = ContextCompat.getColor(this@SessionActivityTest, R.color.really_bad)
        colorBad = ContextCompat.getColor(this@SessionActivityTest, R.color.bad)
        colorGood = ContextCompat.getColor(this@SessionActivityTest, R.color.good)
        colorReallyGood = ContextCompat.getColor(this@SessionActivityTest, R.color.really_good)
        colorPrimary = MaterialColors.getColor(
            this,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(this, R.color.pink)
        )
    }

    override fun onResume() {

        sessionHelper.setListener(object : SessionHelper.SessionHelperCallback {

            override fun onSpeedUpdated(currentSpeed: String) {
                Log.d("AcT", "CS: $currentSpeed")
                updateSpeed(currentSpeed)
            }

            override fun onDataUpdated(
                stileDiGuidaNow: Float,
                stileDiGuida: Float,
                diffspeed: Int,
                points: Int,
                kmPercorsi: Float
            ) {
                updateUI(stileDiGuidaNow, stileDiGuida, diffspeed, points, kmPercorsi)
            }
        })

        if (isCarlaActived) {

            nsdHelper?.initializeNsd(object : NsdHelper.NsdCallback {
                override fun onServerFound(ip: String) {
                    serverFound(ip)
                    nsdHelper?.stopDiscovery()
                }
            })

        }


        setupUI()
        super.onResume()
    }

    private fun setupUI() {

        if (isCarlaActived) {
            binding.layoutConnecting.visibility = View.VISIBLE
            binding.layoutConnected.visibility = View.GONE
        } else {
            binding.layoutConnecting.visibility = View.GONE
            binding.layoutConnected.visibility = View.VISIBLE
        }

        binding.fakeUI.visibility = View.GONE

        binding.endSession.setOnClickListener {
            onBackPressed()
        }

        binding.endSessionFake.setOnClickListener {
            onBackPressed()
        }
    }

    private fun serverFound(ip: String) {

        changeAnimation("satellite.json")

        client = Client(ip, 2005)
        client.connect(object : Client.ClientCallback {

            override fun onMessage(message: String) {
                val json = message.split("}")[0] + "}"
                if (json.contains("{") && json.contains("}")) {
                    val serverMessage =
                        Gson().fromJson(json, CustomObjects.ServerMessage::class.java)
                    updateLimit(serverMessage.limit)
                    sessionHelper.updateData(serverMessage.speed)
                }
            }

            override fun onConnect(socket: Socket) {
                socketConnected()
            }

            override fun onDisconnect(socket: Socket, message: String) {
                socketDisconnected()
            }

            override fun onConnectError(socket: Socket?, message: String) {
                socketError(message)
            }

        })

    }

    private fun updateLimit(limit: String) {

        if (limit == "" && limit.isEmpty())
            return

        if (isCarlaActived.not())
            return

        val myLimit = limit.split(".")[0]

        runOnUiThread {
            binding.limitTv.text = myLimit
        }

    }

    private fun changeAnimation(fileNameAnimation: String) {
        runOnUiThread {
            binding.lottieAnimationView.setAnimation(fileNameAnimation)
            binding.lottieAnimationView.progress = 0F
            binding.lottieAnimationView.playAnimation()
        }
    }

    private fun updateUI(
        stileDiGuidaNow: Float,
        stileDiGuida: Float,
        diffspeed: Int,
        points: Int,
        kmPercorsi: Float
    ) {

        if (isUIReady.not())
            return

        if (fakeUI.not()) {

            runOnUiThread {

                binding.punteggioTv.text = points.toString()

                binding.cvSdgNow.setCardBackgroundColor(getColorSDG(stileDiGuidaNow))
                binding.iconCvSdgNow.setCardBackgroundColor(getColorSDG(stileDiGuidaNow))

                binding.sdgCv.setCardBackgroundColor(getColorSDG(stileDiGuida))
                binding.iconSdgCv.setCardBackgroundColor(getColorSDG(stileDiGuida))

                binding.cvDiff.setCardBackgroundColor(getColorDiff(diffspeed))
                binding.iconCvDiff.setCardBackgroundColor(getColorDiff(diffspeed))

            }
        } else {
            runOnUiThread {
                val km = "%.2f".format(kmPercorsi)
                binding.kmPercorsiFake.text = km
            }
        }

    }

    private fun setTempoCard(diff: Duration) {

        if (diff.seconds.toInt() in 0..59) {
            binding.tempoTv.text = (diff.seconds.toInt()).toString()
            binding.tempoNameTv.text = "secondi"
            return
        }

        if (diff.toMinutes().toInt() in 1..59) {
            binding.tempoTv.text = (diff.toMinutes().toInt()).toString()
            binding.tempoNameTv.text = "minuti"
            return
        }

        if (diff.toHours().toInt() >= 1) {
            binding.tempoTv.text = (diff.toHours().toInt()).toString()
            binding.tempoNameTv.text = "ore"
        }

    }

    private fun updateSpeed(currentSpeed: String) {


        if (isUIReady.not())
            return

        runOnUiThread {
            if (fakeUI.not()) {
                binding.speedTv.text = "$currentSpeed km/h"
                binding.cvVelNow.setCardBackgroundColor(getColorSpeed(currentSpeed.toInt()))
                binding.iconCvVelNow.setCardBackgroundColor(getColorSpeed(currentSpeed.toInt()))
            } else {
                binding.speedFakeTv.text = currentSpeed
                val nowInstant = Instant.now()
                val diff = Duration.between(instantStart, nowInstant)
                setTempoCard(diff)
            }
        }

    }

    private fun getColorDiff(diffspeed: Int): Int {

        val absDiff = abs(diffspeed)

        if (absDiff > 6)
            return colorReallyBad

        val temp = 6 - absDiff
        val hue = (temp * 120) / 6 //1 = green
        return Color.HSVToColor(50, floatArrayOf(hue.toFloat(), 1f, 1f))
    }

    private fun getColorSDG(sdg: Float): Int {

        if (sdg == 0f)
            return colorReallyBad

        if (sdg == 1f)
            return colorReallyGood

        val hue = (sdg * 120) //1 = green
        return Color.HSVToColor(50, floatArrayOf(hue, 1f, 1f))
    }

    private fun getColorSpeed(speed: Int): Int {

        if (speed <= 5)
            return colorReallyBad

        if (speed <= 10)
            return colorBad

        if (speed <= 40)
            return colorReallyGood

        if (speed >= 80)
            return colorReallyBad

        val temp = 20 - ((speed - 40) % 20)
        val hue = (temp * 120) / 20 //20 = green

        return Color.HSVToColor(50, floatArrayOf(hue.toFloat(), 1f, 1f))
    }

    private fun socketConnected() {
        instantStart = Instant.now()
        getFakeUI()
    }

    private fun getFakeUI() {
        FirebaseDatabase.getInstance().getReference("fakeUI")
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValue(Boolean::class.java)
                    if (value != null)
                        fakeUI = value

                    isUIReady = true
                    showUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    fakeUI = true
                    isUIReady = true
                    showUI()

                }
            })
    }

    private fun showUI() {

        if (isUIReady.not())
            return

        runOnUiThread {
            binding.layoutConnecting.visibility = View.GONE
            if (fakeUI.not()) {
                binding.layoutConnected.visibility = View.VISIBLE

                if (isCarlaActived)
                    binding.limit.visibility = View.VISIBLE

            } else {
                binding.limit.visibility = View.GONE
                binding.fakeUI.visibility = View.VISIBLE
                val colorPrimary = MaterialColors.getColor(
                    this@SessionActivityTest,
                    android.R.attr.colorPrimary,
                    ContextCompat.getColor(
                        this@SessionActivityTest,
                        R.color.pink
                    )
                )
                binding.lottieAnimationTripFake.playAnimation()
                binding.lottieAnimationTripFake.addValueCallback(
                    KeyPath("**"),
                    LottieProperty.COLOR_FILTER
                ) {
                    PorterDuffColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP)
                }
            }
        }
    }

    private fun socketDisconnected() {
        showError("Server disconesso", "Il server si è disconnesso", false, {}, { tearDown(true) })
    }

    private fun socketError(message: String) {
        showError("Ops, si è verificato un errore", message, false, {}, { this.finish() })
    }

    private fun showError(
        title: String,
        message: String,
        isCancellable: Boolean,
        functionAfterCancellation: () -> Unit,
        functionAfterDismiss: () -> Unit
    ) {
        runOnUiThread {
            ErrorHelper.showError(
                this@SessionActivityTest,
                title,
                message,
                isCancellable,
                functionAfterCancellation,
                functionAfterDismiss
            )
        }
    }

    override fun onBackPressed() {
        showExitDialog()
    }

    private fun showExitDialog() {

        if (binding.layoutConnected.visibility == View.GONE && binding.fakeUI.visibility == View.GONE) {
            tearDown(false)
            return
        }

        showError("Attenzione", "Sei sicuro di voler uscire?", true, {}, { tearDown(true) })

    }

    private fun tearDown(goToEndSession: Boolean) {

        var results: ArrayList<Float> = arrayListOf(0f, 0f, 0f, 0f)
        if (this::sessionHelper.isInitialized) {
            results = sessionHelper.getResults()
        }

        stopAll()

        if (goToEndSession) {

            val polyMaps: PolylineOptions? = sessionHelper.getTrackMaps()
            val intent = Intent(this@SessionActivityTest, EndSessionActivity::class.java)
            intent.putExtra(EndSessionActivity.PESO_MEDIO, results[0])
            intent.putExtra(EndSessionActivity.KM_PERCORSI, results[1])
            intent.putExtra(EndSessionActivity.VEL_MEDIA, results[2])
            intent.putExtra(EndSessionActivity.PUNTEGGIO, results[3])

            if (polyMaps != null)
                intent.putExtra(EndSessionActivity.POLYLINEOPTIONS, Gson().toJson(polyMaps))

            startActivity(intent)
        }

        finish()

    }

    private fun stopAll() {

        nsdHelper?.stopDiscovery()

        if (this::sessionHelper.isInitialized) {
            sessionHelper.removeListener()
            sessionHelper.stopGPS()
        }

        if (this::client.isInitialized) {
            client.removeClientCallBack()
            client.disconnect()
        }

    }

    override fun onPause() {
        nsdHelper?.stopDiscovery()
        super.onPause()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                requestLocationPermission()
            }
        } else {
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
        } else {
            sessionHelper.setupGPS(this@SessionActivityTest)
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        sessionHelper.setupGPS(this@SessionActivityTest)

                        checkBackgroundLocation()
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", this.packageName, null),
                            ),
                        )
                    }
                }
                return
            }
            MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        sessionHelper.setupGPS(this@SessionActivityTest)

                        Toast.makeText(
                            this,
                            "Granted Background Location Permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return

            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66
    }

}