package com.silvered.ecodrive.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.silvered.ecodrive.R
import java.util.*


class AnalyticsFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var mycontext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        mycontext = context as Context
        sharedPref = mycontext.getSharedPreferences("info", AppCompatActivity.MODE_PRIVATE)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        var nazione = sharedPref.getString("nazione", "")
        val regione = sharedPref.getString("regione", "")
        if (nazione == null || nazione == "" || regione == null || regione == "") {
            //Eccezione
            return
        }

        for (locale in Locale.getAvailableLocales()) {
            if (locale.getDisplayCountry(Locale.ENGLISH).equals(nazione)) {
                nazione = locale.displayCountry
                break
            }
        }


        val viewPager2 = view.findViewById<ViewPager2>(R.id.pager)
        val adapter = ViewPagerAdapter(this)
        viewPager2.adapter = adapter
        viewPager2.isSaveEnabled = false
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(
            tabLayout, viewPager2
        ) { tab: TabLayout.Tab, position: Int ->
            if (position == 0) tab.text = "$nazione"
            if (position == 1) tab.text = "$regione"
        }.attach()
    }

    private class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment {
            return if (position == 1) RankingFragment.newInstance(false) else RankingFragment.newInstance(
                true
            )
        }

        override fun getItemCount(): Int {
            return 2
        }

    }


}