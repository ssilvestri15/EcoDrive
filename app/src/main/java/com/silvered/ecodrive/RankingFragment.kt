package com.silvered.ecodrive

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
import com.silvered.ecodrive.util.RankHelper


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

        if (RankHelper.listGlobal != null && RankHelper.listLocal != null && !RankHelper.needToUpdate) {
            setupUI(view)
            Log.d("SSS", "RIUTILIZZO")
            return
        }

        getRanking(view)
    }


    private fun getRanking(view: View) {

        Log.d("SSS", "RICARICO")

        val sharedPreferences = myContext.getSharedPreferences("info", Context.MODE_PRIVATE)
        val nazione =
            sharedPreferences.getString("nazione", null)

        val regione =
            sharedPreferences.getString("regione", null)

        if (nazione == null || nazione == "" || regione == null || nazione == "") {
            return
        }

        FirebaseDatabase.getInstance().getReference("ranking/$nazione")
            .orderByChild("punteggioMedio")
            .limitToFirst(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    RankHelper.listGlobal = ArrayList()
                    RankHelper.listLocal = ArrayList()

                    RankHelper.listGlobal = getList(snapshot)


                    FirebaseDatabase.getInstance().getReference("ranking/$regione")
                        .orderByChild("punteggioMedio")
                        .limitToFirst(10)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                RankHelper.listLocal = getList(snapshot)
                                RankHelper.needToUpdate = false
                                setupUI(view)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d("SSS 2", error.message)
                            }
                        })

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("SSS 1", error.message)
                }
            })

    }

    private fun getList(snapshot: DataSnapshot): ArrayList<HomeFragment.UserScore> {

        val list = ArrayList<HomeFragment.UserScore>()

        for (snap in snapshot.children) {
            val punt = snap.child("punteggioMedio").getValue(Int::class.java)
            val picURL = snap.child("picurl").getValue(String::class.java)
            Log.d("SSS","PIC: $picURL")
            if (punt != null && picURL != null)
                list.add(
                    HomeFragment.UserScore(
                        picURL,
                        snap.key.toString(),
                        snap.child("name").getValue(String::class.java).toString(),
                        punt
                    )
                )
        }

        list.sortByDescending { it.score }

        return list
    }

    private fun getLocal(): ArrayList<HomeFragment.UserScore>? {
        return null
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

        Log.d("SSSS","${adapter.itemCount}")
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