package com.silvered.ecodrive

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import de.hdodenhof.circleimageview.CircleImageView

import coil.Coil
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation


class RankingAdapter(
    private val context: Context,
    private val dataSet: ArrayList<HomeFragment.UserScore>,
    private val myUid: String
) :
    RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.ranking_item_layout, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userScore = dataSet[position]

        if (myUid != userScore.id) {
            holder.cardView.strokeWidth = 0
        }
        val request: ImageRequest = ImageRequest.Builder(context)
            .data(userScore.imageURL)
            .crossfade(true)
            .transformations(CircleCropTransformation())
            .target(holder.circleImageView)
            .build()
        Coil.imageLoader(context).enqueue(request)

        holder.nameTV.text = userScore.name
        holder.positionTV.text = "#${position+1}"
        holder.pointsTV.text = "${userScore.score}"
    }

    override fun getItemCount() = dataSet.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val cardView: MaterialCardView
        val circleImageView: ImageView
        val positionTV: MaterialTextView
        val nameTV: MaterialTextView
        val pointsTV: MaterialTextView

        init {
            cardView = view.findViewById(R.id.rank_card_view)
            circleImageView = view.findViewById(R.id.circle_image_view)
            positionTV = view.findViewById(R.id.rank_pos)
            nameTV = view.findViewById(R.id.rank_name)
            pointsTV = view.findViewById(R.id.rank_point)
        }
    }

}
