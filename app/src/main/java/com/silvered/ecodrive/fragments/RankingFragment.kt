package com.silvered.ecodrive.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.silvered.ecodrive.databinding.FragmentRankingBinding
import com.silvered.ecodrive.adapters.RankingAdapter
import com.silvered.ecodrive.util.CustomObjects
import com.silvered.ecodrive.util.helpers.HomeHelper
import com.silvered.ecodrive.util.helpers.RankHelper


private const val ARG_PARAM1 = "param1"


class RankingFragment : Fragment() {

    private var param1: Boolean? = null
    private lateinit var myContext: Context
    private var user: FirebaseUser? = null

    private var _binding: FragmentRankingBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = FirebaseAuth.getInstance().currentUser
        arguments?.let {
            param1 = it.getBoolean(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        myContext = context as Context
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData(view)
    }

    private fun getData(view: View) {

        RankHelper.needToUpdate.observe(viewLifecycleOwner) { needToUpdate ->

            if (needToUpdate) {
                getRanking(view)
                return@observe
            }

            if (RankHelper.listGlobal != null && RankHelper.listLocal != null)
                setupUI(view)
        }

    }


    private fun getRanking(view: View) {

        Log.d("SSS", "RICARICO")

        val sharedPreferences = myContext.getSharedPreferences("info", Context.MODE_PRIVATE)
        val nazione =
            sharedPreferences.getString("nazione", null)

        val regione =
            sharedPreferences.getString("regione", null)

        if (user == null || nazione == null || nazione == "" || regione == null || regione == "") {
            return
        }

        RankHelper.updateData(user!!, nazione, regione)

    }

    private fun setupUI(view: View) {

        if ((RankHelper.listGlobal == null && RankHelper.listLocal == null) || user == null) {
            //Eccezione
            return
        }

        binding.barRanking.layoutManager = LinearLayoutManager(myContext)

        val adapter = if (param1!!)
            RankingAdapter(
                myContext,
                RankHelper.listGlobal!!,
                user!!.uid
            )
        else
            RankingAdapter(
                myContext,
                RankHelper.listLocal!!,
                user!!.uid
            )

        Log.d("SSSS", "${adapter.itemCount}")
        binding.barRanking.adapter = adapter
        binding.pbRanking.visibility = View.GONE


    }

    companion object {
        @JvmStatic
        fun newInstance(param1: Boolean) =
            RankingFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_PARAM1, param1)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}