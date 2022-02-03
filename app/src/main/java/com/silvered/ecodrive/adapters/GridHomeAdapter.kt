package com.silvered.ecodrive.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.textview.MaterialTextView
import com.silvered.ecodrive.util.CustomObjects

class GridHomeAdapter(private val list: ArrayList<CustomObjects.GridItem>) :
    RecyclerView.Adapter<GridHomeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.silvered.ecodrive.R.layout.item_grid_home, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        if (item.icon != -1)
            holder.icon.load(item.icon)

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