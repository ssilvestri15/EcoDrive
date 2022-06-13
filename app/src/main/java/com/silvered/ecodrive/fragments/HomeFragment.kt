package com.silvered.ecodrive.fragments

import android.R
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseError
import com.silvered.ecodrive.activity.SessionActivityNew
import com.silvered.ecodrive.util.CustomObjects
import com.silvered.ecodrive.activity.SessionActivityTest
import com.silvered.ecodrive.adapters.GridHomeAdapter
import com.silvered.ecodrive.databinding.FragmentHomeBinding
import com.silvered.ecodrive.util.helpers.ErrorHelper
import com.silvered.ecodrive.util.helpers.HomeHelper


class HomeFragment : Fragment() {

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

        binding.gridViewHome.layoutManager = GridLayoutManager(context, 2)
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

            if (isGamified(myContext)) {
                binding.gridViewHome.adapter = GridHomeAdapter(setList())
                binding.gridViewHome.layoutManager = GridLayoutManager(context, 2)
            }

            binding.levelBtn.text = HomeHelper.levelName

            val viewPager2 = binding.pager
            val adapter = if (isGamified(myContext)) HomeViewPagerAdapter(this) else HomeViewPagerAdapterNotGamified(this)
            viewPager2.adapter = adapter
            viewPager2.isSaveEnabled = false
            val tabLayout = binding.tabLayout
            TabLayoutMediator(
                tabLayout, viewPager2
            ) { tab: TabLayout.Tab, position: Int ->
                if (position == 0) tab.text = ""
                if (position == 1) tab.text = ""
            }.attach()
            showView(binding.aboveLayout)
            showView(binding.levelBtn)

            binding.levelBtn.setOnClickListener {
                showLevelsBottomSheet()
            }

            if (!isGamified(myContext))
                tabLayout.removeTabAt(0)

        } else {
            showView(binding.noDataLayout)
        }

        showView(binding.startSession)

        binding.startSession.setOnClickListener {
            startActivity(Intent(activity, SessionActivityNew::class.java))
        }

        if (!isGamified(myContext)) {
            binding.levelBtn.visibility = View.GONE
        }

    }

    private fun showLevelsBottomSheet() {
        val bottomSheetDialog =
            BottomSheetDialog(myContext, com.silvered.ecodrive.R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(com.silvered.ecodrive.R.layout.levels_items)
        bottomSheetDialog.show()
    }

    private fun setList(): ArrayList<CustomObjects.GridItem> {

        val puntiCV = CustomObjects.GridItem(
            com.silvered.ecodrive.R.drawable.eco_points,
            HomeHelper.punteggioMedio.toString(),
            "I tuoi\npunti"
        )
        val rankLocalCV = CustomObjects.GridItem(
            com.silvered.ecodrive.R.drawable.rank_local,
            HomeHelper.positionLocal.toString(),
            "Classifica\nregionale"
        )
        val recordCV = CustomObjects.GridItem(
            com.silvered.ecodrive.R.drawable.eco_record,
            HomeHelper.record.toString(),
            "Il tuo\nrecord"
        )
        val rankGlobalCV =
            CustomObjects.GridItem(
                com.silvered.ecodrive.R.drawable.rank_global,
                HomeHelper.positionGlobal.toString(),
                "Classifica\nnazionale"
            )

        val list = ArrayList<CustomObjects.GridItem>()
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

    private fun resetView() {

        hideView(binding.levelBtn)
        hideView(binding.syncingLayout)
        hideView(binding.noDataLayout)
        hideView(binding.aboveLayout)
        hideView(binding.startSession)

    }

    private fun showView(view: View) {
        view.visibility = View.VISIBLE
    }

    private fun hideView(view: View) {
        view.visibility = View.GONE
    }

    private fun isGamified(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("info", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isGamified",false)
    }

    override fun onDetach() {
        HomeHelper.removeListener()
        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class HomeViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment {

            return if (position == 1) HomeCardFragment.newInstance(false) else HomeCardFragment.newInstance(
                true
            )
        }

        override fun getItemCount(): Int {
            return 2
        }

    }

    private class HomeViewPagerAdapterNotGamified(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment {

            return HomeCardFragment.newInstance(false)
        }

        override fun getItemCount(): Int {
            return 1
        }

    }


}