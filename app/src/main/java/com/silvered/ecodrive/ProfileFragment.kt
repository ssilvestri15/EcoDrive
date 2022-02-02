package com.silvered.ecodrive

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.silvered.ecodrive.databinding.FragmentProfileBinding
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ProfileFragment : Fragment() {

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

        val settingsBtn = binding.settingsBtn

        settingsBtn.setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }

        val user = firebaseAuth.currentUser

        if (user == null) {
            return
        }

        FirebaseDatabase.getInstance().getReference("users/${user.uid}/routes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (!dataSnapshot.exists()) {
                        showMissingUI()
                        return
                    }

                    val list: ArrayList<Route> = ArrayList()

                    for (snapshot in dataSnapshot.children) {

                        val date: String =
                            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(
                                Timestamp(snapshot.key!!.toLong())
                            )
                        val km = snapshot.child("kmPercorsi").getValue(Float::class.java)
                        val poly = snapshot.child("poly").getValue(String::class.java)
                        val sdg = snapshot.child("pesoStileDiGuida").getValue(Float::class.java)
                        val punti = snapshot.child("punteggio").getValue(Float::class.java)
                        val vel = snapshot.child("velMedia").getValue(Float::class.java)

                        if (vel != null && km != null && sdg != null && punti != null)
                            list.add(Route(date, km, vel, sdg, poly, punti))

                    }

                    list.reverse()

                    binding.routesRvProfile.layoutManager = LinearLayoutManager(myContext)
                    binding.routesRvProfile.adapter =
                        RoutesAdapter(list, object : RouteAdapterCallback {
                            override fun onAdapterItemClick(route: Route) {
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

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })

    }

    private fun showMissingUI() {
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class Route(
        val date: String,
        val km: Float,
        val vel: Float,
        val sdg: Float,
        val poly: String?,
        val punti: Float
    )

    private interface RouteAdapterCallback {
        fun onAdapterItemClick(route: Route)
    }

    private class RoutesAdapter(val list: ArrayList<Route>, val listener: RouteAdapterCallback) :
        RecyclerView.Adapter<RoutesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.routes_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val route = list[position]

            holder.date.text = route.date
            holder.points.text = route.punti.toInt().toString()

            holder.card.setOnClickListener {
                listener.onAdapterItemClick(route)
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val date: MaterialTextView = view.findViewById(R.id.dataTV)
            val points: MaterialTextView = view.findViewById(R.id.punteggioTV)
            val card: MaterialCardView = view.findViewById(R.id.card)

        }

    }

}