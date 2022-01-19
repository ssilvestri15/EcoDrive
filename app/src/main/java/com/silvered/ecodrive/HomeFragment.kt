package com.silvered.ecodrive

import android.R
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseError
import com.silvered.ecodrive.util.HomeHelper


class HomeFragment : Fragment() {

    class UserScore(var id: String, var name: String, var score: Int)

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var myContext: Context
    private var user: FirebaseUser? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth.currentUser
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(com.silvered.ecodrive.R.layout.fragment_home, container, false)
        myContext = context as Context
        sharedPreferences = myContext.getSharedPreferences("info", MODE_PRIVATE)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
    }

    private fun setupUI(view: View) {

        if (user == null) {
            showError(null) { activity?.finish() }
            return
        }

        val name = user!!.displayName?.split(" ")?.get(0)

        if (name == null)
            getNameTextView(view).text = "Ciao!"
        else
            getNameTextView(view).text = "Ciao $name!"

        val nazione = sharedPreferences.getString("nazione","")

        if (nazione == null || nazione == "") {
            //Eccezione
            return
        }

        resetView(view)

        if (HomeHelper.needToUpdate == null || HomeHelper.needToUpdate!!) {
            showView(getSyncingLayout(view))

            val homeHelperListener = object: HomeHelper.HomeHelperCallback {

                override fun onDataReady() {
                    setupUI(view)
                }

                override fun onError(error: DatabaseError) {
                    showError(error.message) {}
                }
            }

            HomeHelper.getData(user!!.uid,nazione, homeHelperListener)
            return
        }

        if (HomeHelper.listChart == null || HomeHelper.positionGlobal == null || HomeHelper.positionLocal == null) {
            //Eccezione
            return
        }

        resetView(view)

        if (HomeHelper.listChart!!.isNotEmpty()) {
            showView(getPointsCardView(view))
            setupBarChart(view, myContext)
            hideView(getProgressBarChartHome(view))
            showView(getBarChartHome(view))
            setRankViews(HomeHelper.positionLocal!!,getRankLocalTextView(view))
            showView(getRankLocalCardView(view))
            setRankViews(HomeHelper.positionGlobal!!,getRankGlobalTextView(view))
            showView(getRankGlobalCardView(view))
            showView(getCLResoconto(view))
        } else {
            showView(getNoDataLayout(view))
        }

        showView(getStartSession(view))

        getStartSession(view).setOnClickListener {
            startActivity(Intent(activity, SessionActivity::class.java))
        }

    }


    private fun showError(message: String?, function: () -> Unit) {
        if (message != null) {
            ErrorHelper.showError(myContext, message, false, function)
        } else {
            ErrorHelper.showError(
                myContext,
                "Si è verificato un errore sconosciuto",
                false,
                function
            )
        }
    }

    private fun setRankViews(
        positionLong: Long,
        textView: MaterialTextView,
    ) {
        val position = "#${positionLong}"
        textView.text = position
    }

    private fun setupBarChart(view: View, context: Context) {

        val entries: ArrayList<BarEntry> = ArrayList()
        val barChartHome: RoundedBarChart = getBarChartHome(view)

        var i = 0f
        for (num in HomeHelper.listChart!!) {
            entries.add(BarEntry(i, num.toFloat()))
            i++
        }

        val colorPrimary = MaterialColors.getColor(
            context,
            R.attr.colorPrimary,
            ContextCompat.getColor(context, com.silvered.ecodrive.R.color.pink)
        )
        val colorAccent = MaterialColors.getColor(
            context,
            R.attr.colorAccent,
            ContextCompat.getColor(context, com.silvered.ecodrive.R.color.green)
        )

        val barDataSet = BarDataSet(entries, "Punteggio")
        barDataSet.valueTextColor = colorPrimary
        barDataSet.setGradientColor(colorAccent, colorPrimary)
        barChartHome.description.isEnabled = false
        barChartHome.axisLeft.isEnabled = false
        barChartHome.axisRight.isEnabled = false
        barChartHome.xAxis.isEnabled = false
        barChartHome.legend.isEnabled = false
        barChartHome.setTouchEnabled(false)
        barChartHome.setPinchZoom(false)
        barChartHome.isDragEnabled = false

        val data = BarData(barDataSet)
        barChartHome.data = data
        barChartHome.invalidate()

    }

    private fun resetView(view: View) {

        hideView(getSyncingLayout(view))
        hideView(getPointsCardView(view))
        hideView(getBarChartHome(view))
        showView(getProgressBarChartHome(view))
        hideView(getRankLocalCardView(view))
        hideView(getRankGlobalCardView(view))
        hideView(getCLResoconto(view))
        hideView(getStartSession(view))

    }

    private fun getRankLocalTextView(view: View): MaterialTextView {
        return view.findViewById(com.silvered.ecodrive.R.id.rank_local_tv)
    }

    private fun getRankLocalCardView(view: View): MaterialCardView {
        return view.findViewById(com.silvered.ecodrive.R.id.chart_local_cardView)
    }

    private fun getRankGlobalTextView(view: View): MaterialTextView {
        return view.findViewById(com.silvered.ecodrive.R.id.rank_global_tv)
    }

    private fun getRankGlobalCardView(view: View): MaterialCardView {
        return view.findViewById(com.silvered.ecodrive.R.id.chart_global_cardView)
    }

    private fun getNameTextView(view: View): MaterialTextView {
        return view.findViewById(com.silvered.ecodrive.R.id.name_textView)
    }

    private fun getSyncingLayout(view: View): ConstraintLayout {
        return view.findViewById(com.silvered.ecodrive.R.id.syncing_layout)
    }

    private fun getStartSession(view: View): MaterialButton {
        return view.findViewById(com.silvered.ecodrive.R.id.start_session)
    }

    private fun getNoDataLayout(view: View): LinearLayout {
        return view.findViewById(com.silvered.ecodrive.R.id.no_data_layout)
    }

    private fun getCLResoconto(view: View): ConstraintLayout {
        return view.findViewById(com.silvered.ecodrive.R.id.cl_resoconto)
    }

    private fun getPointsCardView(view: View): MaterialCardView {
        return view.findViewById(com.silvered.ecodrive.R.id.points_cardView)
    }

    private fun getBarChartHome(view: View): RoundedBarChart {
        return view.findViewById(com.silvered.ecodrive.R.id.barChartHome)
    }

    private fun getProgressBarChartHome(view: View): ProgressBar {
        return view.findViewById(com.silvered.ecodrive.R.id.pb_points)
    }

    private fun showView(view: View) {
        view.visibility = View.VISIBLE
    }

    private fun hideView(view: View) {
        view.visibility = View.GONE
    }

    override fun onDetach() {
        HomeHelper.removeListener()
        super.onDetach()
    }

}