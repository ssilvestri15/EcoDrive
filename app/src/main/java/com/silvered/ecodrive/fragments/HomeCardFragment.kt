package com.silvered.ecodrive.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.color.MaterialColors
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.FragmentHomeCardBinding
import com.silvered.ecodrive.databinding.FragmentRankingBinding
import com.silvered.ecodrive.util.helpers.HomeHelper

private const val ISECOPOINTS = "isEcoPoints"

class HomeCardFragment : Fragment() {

    private var isEcoPoints: Boolean? = null

    private lateinit var myContext: Context
    private var _binding: FragmentHomeCardBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isEcoPoints = it.getBoolean(ISECOPOINTS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        myContext = context as Context
        _binding = FragmentHomeCardBinding.inflate(inflater, container, false)
        return binding.root



    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isEcoPoints == null)
            isEcoPoints = true

        if (isEcoPoints!!)
            setupBarChart()
        else
            setupSDGChart()
    }

    private fun setupSDGChart() {
        binding.cardTitleTv.text = "EcoDrive"
        binding.barChartHome.visibility = View.GONE

        val colorPrimary = MaterialColors.getColor(
            myContext,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(myContext, R.color.pink)
        )

        when (HomeHelper.stileDiGuida!!) {

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

        binding.pbPoints.visibility = View.GONE
        binding.llFoglie.visibility = View.VISIBLE

    }

    private fun setupBarChart() {

        binding.cardTitleTv.text = "EcoPoints"
        binding.llFoglie.visibility = View.GONE

        val entries: ArrayList<Entry> = ArrayList()
        val barChartHome: LineChart = binding.barChartHome

        var i = 0f
        for (num in HomeHelper.listChart!!) {
            entries.add(Entry(i, num.toFloat()))
            i++
        }

        val colorPrimary = MaterialColors.getColor(
            myContext,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(myContext, R.color.pink)
        )

        val colorOnPrimary = MaterialColors.getColor(
            myContext,
            com.google.android.material.R.attr.colorOnPrimary,
            ContextCompat.getColor(myContext, R.color.green)
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

        binding.pbPoints.visibility = View.GONE
        barChartHome.visibility = View.VISIBLE

    }

    companion object {

        @JvmStatic
        fun newInstance(isEcoPoints: Boolean) =
            HomeCardFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ISECOPOINTS, isEcoPoints)
                }
            }
    }
}