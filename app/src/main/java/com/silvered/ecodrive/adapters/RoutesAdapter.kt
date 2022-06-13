package com.silvered.ecodrive.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.silvered.ecodrive.R
import com.silvered.ecodrive.fragments.ProfileFragment
import com.silvered.ecodrive.util.CustomObjects

class RoutesAdapter(
    val isGamified: Boolean,
    val list: ArrayList<CustomObjects.Route>,
    val listener: ProfileFragment.RouteAdapterCallback
) :
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

        if (!isGamified)
            holder.ecoPointsLayout.visibility = View.GONE

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
        val ecoPointsLayout: LinearLayout = view.findViewById(R.id.linearLayout3)

    }

}