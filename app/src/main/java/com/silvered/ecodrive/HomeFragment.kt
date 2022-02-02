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
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseError
import com.silvered.ecodrive.databinding.FragmentHomeBinding
import com.silvered.ecodrive.util.ErrorHelper
import com.silvered.ecodrive.util.HomeHelper


class HomeFragment : Fragment() {

    class UserScore(var imageURL: String, var id: String, var name: String, var score: Int)

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var myContext: Context
    private var user: FirebaseUser? = null
    private lateinit var sharedPreferences: SharedPreferences

    private var _binding: FragmentHomeBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth.currentUser
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        myContext = context as Context
        sharedPreferences = myContext.getSharedPreferences("info", MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {

        if (user == null) {
            showError(null) { activity?.finish() }
            return
        }

        val name = user!!.displayName?.split(" ")?.get(0)

        if (name == null)
            binding.nameTextView.text = "Ciao!"
        else
            binding.nameTextView.text = "Ciao $name!"

        binding.gridViewHome.layoutManager = GridLayoutManager(context,2)
        binding.gridViewHome.adapter = GridHomeAdapter(ArrayList())

        val nazione = sharedPreferences.getString("nazione", "")
        val regione = sharedPreferences.getString("regione", "")

        if (nazione == null || nazione == "" || regione == null || regione == "") {
            //Eccezione
            return
        }

        HomeHelper.setupUpdateListner(object : HomeHelper.HomeHelperUpdate {
            override fun onNeedToUpdate() {
                setupUI()
            }
        })

        resetView()

        if (HomeHelper.needToUpdate == null || HomeHelper.needToUpdate!!) {
            showView(binding.syncingLayout)

            val homeHelperListener = object : HomeHelper.HomeHelperCallback {

                override fun onDataReady() {
                    setupUI()
                }

                override fun onError(error: DatabaseError) {
                    showError(error.message) {}
                }
            }

            HomeHelper.getData(user!!.uid, nazione, regione, homeHelperListener)
            return
        }

        if (HomeHelper.listChart == null || HomeHelper.positionGlobal == null || HomeHelper.positionLocal == null) {
            //Eccezione
            return
        }

        resetView()

        if (HomeHelper.listChart!!.isNotEmpty()) {
            binding.gridViewHome.adapter = GridHomeAdapter(setList())
            binding.levelName.text = HomeHelper.levelName
            setupBarChart(myContext)
            hideView(binding.pbPoints)
            showView(binding.aboveLayout)
            showView(binding.pointsCardView)
            showView(binding.barChartHome)
            showView(binding.levelCv)
        } else {
            showView(binding.noDataLayout)
        }

        showView(binding.startSession)

        binding.startSession.setOnClickListener {
            startActivity(Intent(activity, SessionActivityTest::class.java))
        }

    }

    private fun setList(): ArrayList<GridItem> {

        val puntiCV = GridItem(-1, HomeHelper.punteggioMedio.toString(), "I tuoi\npunti")
        val rankLocalCV = GridItem(-1, HomeHelper.positionLocal.toString(), "Classifica\nregionale")
        val recordCV = GridItem(-1, HomeHelper.record.toString(), "Il tuo\nrecord")
        val rankGlobalCV =
            GridItem(-1, HomeHelper.positionGlobal.toString(), "Classifica\nnazionale")

        val list = ArrayList<GridItem>()
        list.add(puntiCV)
        list.add(rankLocalCV)
        list.add(recordCV)
        list.add(rankGlobalCV)

        return list
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

    private fun setupBarChart(context: Context) {

        val entries: ArrayList<Entry> = ArrayList()
        val barChartHome: LineChart = binding.barChartHome

        binding.gridViewHome.layoutManager = GridLayoutManager(context, 2)

        var i = 0f
        for (num in HomeHelper.listChart!!) {
            entries.add(Entry(i, num.toFloat()))
            i++
        }

        val colorPrimary = MaterialColors.getColor(
            context,
            R.attr.colorPrimary,
            ContextCompat.getColor(context, com.silvered.ecodrive.R.color.pink)
        )
        val colorOnPrimary = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnPrimary,
            ContextCompat.getColor(context, com.silvered.ecodrive.R.color.green)
        )

        val barDataSet = LineDataSet(entries, "Punteggio")
        barDataSet.valueTextColor = colorPrimary
        //barDataSet.setGradientColor(colorAccent, colorPrimary)
        //barDataSet.fillDrawable = ContextCompat.getDrawable(myContext, com.silvered.ecodrive.R.drawable.chart_fill)
        barDataSet.lineWidth = 10f
        barDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        barDataSet.color = colorPrimary
        barDataSet.setDrawFilled(false)
        barDataSet.setDrawValues(true)
        barDataSet.setDrawCircles(true)
        barDataSet.setCircleColor(colorPrimary)
        barDataSet.circleHoleColor = colorOnPrimary
        barDataSet.setDrawCircleHole(true)
        barDataSet.circleRadius = 10f
        barDataSet.circleHoleRadius = 5f
        barChartHome.description.isEnabled = false
        barChartHome.axisLeft.isEnabled = false
        barChartHome.axisRight.isEnabled = false
        barChartHome.xAxis.isEnabled = false
        barChartHome.legend.isEnabled = false
        barChartHome.setTouchEnabled(false)
        barChartHome.setPinchZoom(false)
        barChartHome.isDragEnabled = false
        barChartHome.setScaleEnabled(true)

        val data = LineData(barDataSet)
        barChartHome.data = data
        barChartHome.invalidate()

    }

    private fun resetView() {

        hideView(binding.levelCv)
        hideView(binding.syncingLayout)
        hideView(binding.noDataLayout)
        hideView(binding.pointsCardView)
        hideView(binding.barChartHome)
        showView(binding.pbPoints)
        hideView(binding.aboveLayout)
        hideView(binding.startSession)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class GridItem(val icon: Int, val text: String, val cardTitle: String)

    private class GridHomeAdapter(private val list: ArrayList<GridItem>) :
        RecyclerView.Adapter<GridHomeAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(com.silvered.ecodrive.R.layout.item_grid_home, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val item = list[position]

            holder.text.text = item.text
            holder.title.text = item.cardTitle
        }

        override fun getItemCount(): Int {
            return list.size
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            val icon: ImageView = itemView.findViewById(com.silvered.ecodrive.R.id.icon)
            val text: MaterialTextView =
                itemView.findViewById(com.silvered.ecodrive.R.id.reusable_tv)
            val title: MaterialTextView =
                itemView.findViewById(com.silvered.ecodrive.R.id.card_title_tv)

        }

    }

}