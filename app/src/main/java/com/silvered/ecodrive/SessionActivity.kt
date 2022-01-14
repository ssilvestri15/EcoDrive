package com.silvered.ecodrive

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.silvered.ecodrive.databinding.ActivitySessionBinding
import com.silvered.ecodrive.util.Client
import java.net.Socket
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class SessionActivity : AppCompatActivity() {

    private lateinit var client: Client

    private lateinit var binding: ActivitySessionBinding
    private lateinit var graph: LineChart
    private lateinit var lineDataSet: LineDataSet
    private lateinit var lineData: LineData
    private lateinit var lineList: ArrayList<Entry>


    private var time = 0f
    private var previuspeed = 1f

    private var idRoute: Long = 0

    private var counter = 0 //Scarto per aggiornare il grafico

    private var kmpercosi = 0f
    private var mPrec = 0f

    private var counterV = 1 //Numero delle velocità ricevute
    private var speedS = 0f //Somma delle velocità ricevute
    private var velM = 0f //Velocità media

    private var startTimeViaggio: Instant? = null //Data iniziale viaggio

    private var startTimeAC: Instant? = null //Data iniziale andamento costante
    private var startTimeFermo: Instant? = null //Data iniziale fermo o nel traffico


    private var altitudinePrec: Int = 0
    private var diffAltitudinePrec: Int = 0
    private var startTimeSalita: Instant? = null //Data iniziale salita

    private var punteggio = 0f

    private var secondiAndamentoCostantePrec = 0
    private var secondiAndamentoCostanteTot: Long = 0
    private var secondiAndamentoCostanteTotPrec: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        graph = binding.chart

        lineList = ArrayList()
        lineList.add(Entry(0f, 0f))
        lineDataSet = LineDataSet(lineList, "")
        lineDataSet.fillDrawable = ContextCompat.getDrawable(this, R.drawable.chart_fill)
        lineDataSet.lineWidth = 10f
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineDataSet.color = ContextCompat.getColor(this, R.color.pink)
        lineDataSet.setDrawFilled(true)
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawCircles(false)
        lineData = LineData()
        lineData.addDataSet(lineDataSet)

        graph.description.isEnabled = false
        graph.axisLeft.isEnabled = false
        graph.axisRight.isEnabled = false
        graph.xAxis.isEnabled = false
        graph.legend.isEnabled = false
        graph.setTouchEnabled(false)
        graph.setPinchZoom(false)
        graph.isDragEnabled = false
        graph.setScaleEnabled(false)


        graph.data = lineData

        idRoute = System.currentTimeMillis()

        client = Client("192.168.1.113", 2005)
        client.setClientCallBack(object : Client.ClientCallback {

            override fun onMessage(message: String) {
                runOnUiThread {
                    updateUI(message)
                }
            }

            override fun onConnect(socket: Socket) {
                runOnUiThread {
                    Handler().postDelayed({
                        binding.layoutConnecting.visibility = View.GONE
                        binding.lottieAnimationView.pauseAnimation()
                        binding.layoutConnected.visibility = View.VISIBLE
                        startTimeViaggio = Instant.now()
                    }, 1500)
                }
            }

            override fun onDisconnect(socket: Socket, message: String) {
                runOnUiThread {
                    ErrorHelper.showError(this@SessionActivity, "Il simulatore si è disconnesso",false) { onBackPressed() }
                }
            }

            override fun onConnectError(socket: Socket?, message: String) {
                Log.d("MAS", "Errore: $message")
                runOnUiThread {
                    ErrorHelper.showError(this@SessionActivity,"Non siamo riusciti a connetterci con il simulatore, verfica che sia acceso o che entrambi i devici siano connessi alla stessa rete WiFi",false) { onBackPressed() }
                }
            }
        })

        binding.endSession.setOnClickListener {
            client.disconnect()
            onBackPressed()
        }

        client.connect()


    }

    private fun updateUI(speedString: String) {


        try {

            val temp = speedString.split(".")
            val speed = temp[0] + '.' + temp[1]

            val currentSpeed = speed.toFloat()
            updateVelMedia(currentSpeed)
            binding.speedTv.text = String.format("%.0f", currentSpeed)

            if (counter >= 15) {

                var diffspeed = 0


                if (currentSpeed > previuspeed + 2 || currentSpeed < previuspeed - 2) {

                    if (previuspeed < 1f)
                        previuspeed = 1f

                    if (currentSpeed > 0f && previuspeed > 0f)
                        diffspeed = (((currentSpeed - previuspeed) / previuspeed) * 100).toInt()

                }

                updatePunteggio(currentSpeed, diffspeed)

                previuspeed = currentSpeed
                time += 1f

                updateChart(diffspeed.toFloat())
                counter = 0
            } else {
                counter++
            }

        } catch (e: Exception) {
            Log.d("MAS", "Error: ${e.message}")
        }

    }


    private fun updateChart(currentSpeed: Float) {
        lineList.add(Entry(time, currentSpeed))
        lineDataSet.notifyDataSetChanged()
        lineData.notifyDataChanged()
        graph.notifyDataSetChanged()
        graph.setVisibleXRange(1f, 5f)
        graph.moveViewToX(time)
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

        //(pesoAndamentoMedio * ((pesoAndamentoCorrente * kmpercorsiConLaVelocitaCorrente) + differenzaPercentualeConVelocitàPrecedente)))
        val stileDiGuida = pesoAM * ((pesoAC * (mPercosiAC / 1000) + diff))

        if (secondiAndamentoCostante == (0).toLong())
            secondiAndamentoCostantePrec = 0

        secondiAndamentoCostanteTot += (secondiAndamentoCostante - secondiAndamentoCostantePrec)


        val pesoKmBassi = (pesoKm * kmpercosi) //Il peso dei kilometrti percorsi dall'utente, se è < 1 allora viene penalizzato
        val daLevare = (stileDiGuida + (secondiFermo) + pesoKmBassi + secondiSalita + kmpercosi) //Punti di penalizzazione
        val res = (secondiAndamentoCostanteTot - secondiAndamentoCostanteTotPrec) - daLevare //L'utente guadagna punti in base ai secondi che guida in maniera costante
        secondiAndamentoCostanteTotPrec = secondiAndamentoCostanteTot
        punteggio += res / 100 // effettuo la divisione /100 così per non far uscire valori troppo elevati

    }

    private fun updateVelMedia(currentSpeed: Float) {
        speedS += currentSpeed
        counterV++
        velM = speedS / counterV
    }

    private fun getPeso(speed: Float): Float {

        val v = speed.toInt()

        if (v <= 40)
            return 1f

        if (v >= 80)
            return 0f

        return (1 - ((v / 40f) - 1))

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

    private fun getKmPercorsi(): Float {

        val now = Instant.now()
        val res = Duration.between(startTimeViaggio, now).seconds
        val h = res / 3600f
        return velM * h

    }

    private fun getPesoKm(kmpercosi: Float): Float {

        if (kmpercosi <= 0.5f)
            return 1f

        if (kmpercosi >= 1f)
            return 0f

        return (1 - ((kmpercosi / 0.5) - 1)).toFloat()

    }

    override fun onBackPressed() {
        client.disconnect()
        super.onBackPressed()
    }

}
