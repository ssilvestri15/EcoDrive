package com.silvered.ecodrive.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.silvered.ecodrive.activity.RoutesActivity
import com.silvered.ecodrive.activity.SettingsActivity
import com.silvered.ecodrive.adapters.RoutesAdapter
import com.silvered.ecodrive.databinding.FragmentProfileBinding
import com.silvered.ecodrive.util.CustomObjects
import com.silvered.ecodrive.util.helpers.HomeHelper
import com.silvered.ecodrive.util.helpers.ProfileHelper
import java.util.*
import kotlin.collections.ArrayList


class ProfileFragment : Fragment() {

    interface RouteAdapterCallback {
        fun onAdapterItemClick(route: CustomObjects.Route)
    }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var myContext: Context
    private var user: FirebaseUser? = null
    private lateinit var sharedPreferences: SharedPreferences

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        firebaseAuth = FirebaseAuth.getInstance()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        myContext = context as Context
        sharedPreferences = myContext.getSharedPreferences("info", AppCompatActivity.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupUI() {

        val isGamified = isGamified(myContext)

        val settingsBtn = binding.settingsBtn

        settingsBtn.setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }

        val user = firebaseAuth.currentUser ?: return

        if (HomeHelper.numViaggi == null || HomeHelper.punteggioMedio == null) {
            binding.linearLayout2.visibility = View.INVISIBLE
        } else {
            binding.numViaggiTV.text = HomeHelper.numViaggi.toString()
            binding.punteggioTV.text = HomeHelper.punteggioMedio.toString()
        }

        if (!isGamified) {
            binding.punteggioSection.visibility = View.GONE
        }

        binding.routesRvProfile.layoutManager = LinearLayoutManager(myContext)

        ProfileHelper.listRoutes.observe(viewLifecycleOwner) { list ->

            if (ProfileHelper.needToUpdate) {
                ProfileHelper.updateData(user)
                return@observe
            }

            binding.progressProfile.visibility = View.GONE
            if (list == null || list.isEmpty()) {
                //Eccezione
                showMissingUI()
                return@observe
            }

            binding.routesRvProfile.adapter =
                RoutesAdapter(
                    isGamified,
                    list,
                    object : RouteAdapterCallback {
                        override fun onAdapterItemClick(route: CustomObjects.Route) {
                            val intent = Intent(myContext, RoutesActivity::class.java)
                            intent.putExtra(RoutesActivity.DATA, route.date)
                            intent.putExtra(RoutesActivity.KM_PERCORSI, route.km)
                            intent.putExtra(RoutesActivity.VEL_MEDIA, route.vel)
                            intent.putExtra(RoutesActivity.PESO_MEDIO, route.sdg)
                            intent.putExtra(RoutesActivity.POLYLINEOPTIONS, route.poly)
                            intent.putExtra(RoutesActivity.PUNTEGGIO, route.punti)
                            startActivity(intent)
                        }
                    })
        }

    }


    private fun showMissingUI() {
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isGamified(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("info", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isGamified", false)
    }

}