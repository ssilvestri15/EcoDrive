package com.silvered.ecodrive.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.ActivitySessionNewBinding
import com.silvered.ecodrive.services.SessionLocationService
import com.silvered.ecodrive.util.DataSaver
import com.silvered.ecodrive.util.helpers.ErrorHelper
import com.silvered.ecodrive.util.helpers.HomeHelper
import com.silvered.ecodrive.util.helpers.ProfileHelper
import com.silvered.ecodrive.util.helpers.RankHelper
import kotlin.math.abs

class SessionActivityNew : AppCompatActivity() {

    private lateinit var binding: ActivitySessionNewBinding

    private var colorReallyBad = -1
    private var colorBad = -1
    private var colorGood = -1
    private var colorReallyGood = -1
    private var colorPrimary = -1
    private var colorOnSurface = -1

    private lateinit var sessionReceiver: BroadcastReceiver
    private lateinit var sessionServiceIntent: Intent

    private var targetAudio = 0.1
    private var isAudioEnabled = true

    private var uiVisible = false
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getColors()
        sharedPreferences = getSharedPreferences("info", Context.MODE_PRIVATE)

        sessionServiceIntent = Intent(this, SessionLocationService::class.java)
        sessionServiceIntent.action = SessionLocationService.KEY
        sessionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val results = intent.getFloatArrayExtra(SessionLocationService.RESULT_OUTPUT)
                val polylineOptions =
                    intent.getParcelableExtra<PolylineOptions>(SessionLocationService.POLY_OUTPUT)

                if (results != null) {
                    val floatList = ArrayList<Float>(results.size)
                    results.forEach {
                        floatList.add(it)
                    }
                    tearDown(floatList, polylineOptions)
                    return
                }

                val speedText = intent.getStringExtra(SessionLocationService.SPEED_TEXT)

                if (speedText != null) {
                    updateSpeed(speedText)
                }

                val stileDiGuidaNow: Float =
                    intent.getFloatExtra(SessionLocationService.STILE_DI_GUIDA_NOW, -1f)
                val stileDiGuida: Float =
                    intent.getFloatExtra(SessionLocationService.STILE_DI_GUIDA, -1f)
                val diffspeed: Int = intent.getIntExtra(SessionLocationService.DIFF_SPEED, -1500)
                val points: Int = intent.getIntExtra(SessionLocationService.POINTS, -1)
                val kmPercorsi: Float =
                    intent.getFloatExtra(SessionLocationService.KM_PERCORSI, -1f)

                if (stileDiGuidaNow != -1f && stileDiGuida != -1f && diffspeed != -1500 && points != -1 && kmPercorsi != -1f) {
                    updateUI(stileDiGuidaNow, stileDiGuida, diffspeed, points, kmPercorsi)
                }

            }
        }

        if (!isGamified())
            binding.layoutEcopoint.visibility = View.GONE

        checkLocationPermission()

    }

    override fun onResume() {
        activityResumed()
        setupUI()
        super.onResume()
    }

    private fun updateSpeed(currentSpeed: String) {

        runOnUiThread {

            binding.speedTv.text = "$currentSpeed km/h"

            val colorSpeed = getColorSpeed(currentSpeed.toInt())

            binding.cvVelNow.setCardBackgroundColor(colorSpeed)

            val contrastColorSpeed = getContrastColor(colorSpeed)
            binding.speedTv.setTextColor(contrastColorSpeed)
            binding.speedIcon.setColorFilter(contrastColorSpeed)

        }

    }

    private fun updateUI(
        stileDiGuidaNow: Float,
        stileDiGuida: Float,
        diffspeed: Int,
        points: Int,
        kmPercorsi: Float
    ) {

        runOnUiThread {


            binding.punteggioTv.text = points.toString()

            val sdgNowColor = getColorSDG(stileDiGuidaNow)
            binding.cvSdgNow.setCardBackgroundColor(sdgNowColor)
            val contrastColorSDGNOW = getContrastColor(sdgNowColor)
            binding.sdgNowTv.setTextColor(contrastColorSDGNOW)
            binding.sdgNowIcon.setColorFilter(contrastColorSDGNOW)

            when (stileDiGuida) {


                in 0f..0.33f -> {
                    ImageViewCompat.setImageTintList(
                        binding.foglia1,
                        ColorStateList.valueOf(colorPrimary)
                    )
                    ImageViewCompat.setImageTintList(
                        binding.foglia2,
                        ColorStateList.valueOf(colorOnSurface)
                    )
                    ImageViewCompat.setImageTintList(
                        binding.foglia3,
                        ColorStateList.valueOf(colorOnSurface)
                    )
                }
                in 0.34f..0.66f -> {
                    ImageViewCompat.setImageTintList(
                        binding.foglia1,
                        ColorStateList.valueOf(colorPrimary)
                    )
                    ImageViewCompat.setImageTintList(
                        binding.foglia2,
                        ColorStateList.valueOf(colorPrimary)
                    )
                    ImageViewCompat.setImageTintList(
                        binding.foglia3,
                        ColorStateList.valueOf(colorOnSurface)
                    )
                }
                else -> {
                    ImageViewCompat.setImageTintList(
                        binding.foglia1,
                        ColorStateList.valueOf(colorPrimary)
                    )
                    ImageViewCompat.setImageTintList(
                        binding.foglia2,
                        ColorStateList.valueOf(colorPrimary)
                    )
                    ImageViewCompat.setImageTintList(
                        binding.foglia3,
                        ColorStateList.valueOf(colorPrimary)
                    )
                }
            }

            val accBruscaColor = getColorDiff(diffspeed)
            binding.cvDiff.setCardBackgroundColor(accBruscaColor)
            val contrastColorACCBRUSCA = getContrastColor(accBruscaColor)
            binding.accBruscaTv.setTextColor(contrastColorACCBRUSCA)
            binding.accBruscaIcon.setColorFilter(contrastColorACCBRUSCA)

            reproduceSound(kmPercorsi, stileDiGuida)

        }

    }

    private fun reproduceSound(kmpercosi: Float, stileDiGuida: Float) {

        if (isAudioEnabled.not())
            return

        if (kmpercosi < targetAudio)
            return

        Thread {

            val mediaPlayer = if (stileDiGuida in 0f..0.5f) MediaPlayer.create(
                this,
                R.raw.alert_error_01
            ) else MediaPlayer.create(this, R.raw.hero_simple_celebration_01)

            mediaPlayer.setOnCompletionListener {
                it.release()
            }

            mediaPlayer.start()
            targetAudio = kmpercosi * 1.8

        }.start()

    }

    private fun setupUI() {

        binding.endSession.setOnClickListener {
            onBackPressed()
        }

        binding.audioFab.setOnClickListener {
            isAudioEnabled = !isAudioEnabled
            when (isAudioEnabled) {
                true -> binding.audioFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.baseline_volume_up_black_24dp
                    )
                )
                false -> binding.audioFab.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.baseline_volume_off_black_24dp
                    )
                )
            }
        }

    }

    override fun onBackPressed() {
        if (uiVisible)
            showExitDialog()
        else
            super.onBackPressed()
    }

    private fun showExitDialog() {
        showError("Attenzione", "Sei sicuro di voler uscire?", true, {}, { requestTearDown() })
    }

    private fun requestTearDown() {
        stopAll()
    }

    private fun tearDown(results: ArrayList<Float>, polyMaps: PolylineOptions?) {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionReceiver)

        if (activityVisible.not()) {

            val polyString = Gson().toJson(polyMaps)
            val sharedPreferences = getSharedPreferences("info", MODE_PRIVATE)

            DataSaver.saveData(
                results[0],
                results[1],
                results[2],
                results[3],
                polyString,
                sharedPreferences
            ) {
            }

            return
        }

        val intent = Intent(this, EndSessionActivity::class.java)
        intent.putExtra(EndSessionActivity.PESO_MEDIO, results[0])
        intent.putExtra(EndSessionActivity.KM_PERCORSI, results[1])
        intent.putExtra(EndSessionActivity.VEL_MEDIA, results[2])
        intent.putExtra(EndSessionActivity.PUNTEGGIO, results[3])

        if (polyMaps != null)
            intent.putExtra(EndSessionActivity.POLYLINEOPTIONS, Gson().toJson(polyMaps))

        startActivity(intent)
        finish()

    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    private fun stopAll() {
        stopService(sessionServiceIntent)
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
                this,
                title,
                message,
                isCancellable,
                functionAfterCancellation,
                functionAfterDismiss
            )
        }
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

        return Color.HSVToColor(200, floatArrayOf(hue.toFloat(), 1f, 1f))
    }

    @ColorInt
    fun getContrastColor(@ColorInt color: Int): Int {
        // Counting the perceptive luminance - human eye favors green color...
        val a =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return if (a < 0.5) Color.BLACK else Color.WHITE
    }

    private fun getColorDiff(diffspeed: Int): Int {

        val absDiff = abs(diffspeed)

        if (absDiff > 6)
            return colorReallyBad

        val temp = 6 - absDiff
        val hue = (temp * 120) / 6 //1 = green
        return Color.HSVToColor(200, floatArrayOf(hue.toFloat(), 1f, 1f))
    }

    private fun getColorSDG(sdg: Float): Int {

        if (sdg == 0f)
            return colorReallyBad

        if (sdg == 1f)
            return colorReallyGood

        val hue = (sdg * 120) //1 = green
        return Color.HSVToColor(200, floatArrayOf(hue, 1f, 1f))
    }

    private fun getColors() {
        colorReallyBad = ContextCompat.getColor(this, R.color.really_bad)
        colorBad = ContextCompat.getColor(this, R.color.bad)
        colorGood = ContextCompat.getColor(this, R.color.good)
        colorReallyGood = ContextCompat.getColor(this, R.color.really_good)
        colorPrimary = MaterialColors.getColor(
            this,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(this, R.color.pink)
        )
        colorOnSurface = MaterialColors.getColor(
            this,
            R.attr.fogliaColor,
            ContextCompat.getColor(this, R.color.light_gray)
        )
    }

    private fun isGamified(): Boolean {
        val sharedPreferences = getSharedPreferences("info", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isGamified", false)
    }

    private fun checkLocationPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            showPermissionDialog {
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

            showPermissionDialog(
                title = "Permesso in background",
                message = "Per abilitare l'analisi dello stile di guida anche quando l'applicazione è chiusa, EcoDrive ha bisogno di accedere alla posizione in backgound",
                positiveButtonClick = {
                    requestBackgroundLocationPermission()
                },
                negativeButtonClick = {
                    showUI()
                }
            )

        } else {
            showUI()
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
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            showUI()
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
                    checkBackgroundLocation()
                } else {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        binding.locationPermission.visibility = View.VISIBLE
                        binding.btnPermessi.setOnClickListener {
                            startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", this.packageName, null)
                                )
                            )
                        }
                    } else {
                        showPermissionDialog {
                            requestLocationPermission()
                        }
                    }
                }

                return
            }

            MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showUI()
                } else {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {

                        showPermissionDialog(
                            title = "Permesso in background",
                            message = "Per abilitare l'analisi dello stile di guida anche quando l'applicazione è chiusa, EcoDrive ha bisogno di accedere alla posizione in backgound",
                            positiveButtonClick = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", this.packageName, null)
                                    )
                                )
                            },
                            negativeButtonClick = {
                                showUI()
                            }
                        )

                    } else {
                        showPermissionDialog(
                            title = "Permesso in background",
                            message = "Per abilitare l'analisi dello stile di guida anche quando l'applicazione è chiusa, EcoDrive ha bisogno di accedere alla posizione in backgound",
                            positiveButtonClick = {
                                requestBackgroundLocationPermission()
                            },
                            negativeButtonClick = {
                                showUI()
                            }
                        )
                    }

                }

                return

            }
        }
    }

    private fun showUI() {

        val intentFilter = IntentFilter()
        intentFilter.addAction(SessionLocationService.KEY)
        LocalBroadcastManager.getInstance(this).registerReceiver(sessionReceiver, intentFilter)

        startForegroundService(sessionServiceIntent)
        uiVisible = true
        binding.permissionOk.visibility = View.VISIBLE
    }

    private fun showPermissionDialog(
        title: String = "Permesso per accedere alla posizione",
        message: String = "Questa applicazione necessita dei permessi della posizione per poter valutare il tuo stile di guida.\n\nAccetti?",
        positiveButtonClick: () -> Unit = {
            requestLocationPermission()
        },
        negativeButtonClick: () -> Unit = {
            binding.locationPermission.visibility = View.VISIBLE
            binding.btnPermessi.setOnClickListener {
                Log.d("LOG", "CLICKED")
                positiveButtonClick()
            }
        }
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Accetta") { dialog, _ ->
                positiveButtonClick()
                dialog.dismiss()
            }
            .setNegativeButton("Rifiuto") { dialog, _ ->
                negativeButtonClick()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onPause() {
        activityPaused()
        super.onPause()
    }


    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66

        fun isActivityVisible(): Boolean {
            return activityVisible
        }

        fun activityResumed() {
            activityVisible = true
        }

        fun activityPaused() {
            activityVisible = false
        }

        private var activityVisible = false

    }

}