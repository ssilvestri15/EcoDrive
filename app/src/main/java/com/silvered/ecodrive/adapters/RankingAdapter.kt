package com.silvered.ecodrive.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.silvered.ecodrive.R
import com.silvered.ecodrive.util.CustomObjects


class RankingAdapter(
    private val context: Context,
    private val dataSet: ArrayList<CustomObjects.UserScore>,
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

        Log.e(
            "INFO",
            "[POSITION: $position] MY ID: $myUid, POSITION ID: ${userScore.id}, SCORE: ${userScore.score}"
        )


        if (myUid != userScore.id) {
            holder.cardView.strokeWidth = 0
        } else {
            holder.cardView.strokeWidth = 12
        }

        val request: ImageRequest = ImageRequest.Builder(context)
            .data(userScore.imageURL)
            .crossfade(true)
            .transformations(CircleCropTransformation())
            .target(holder.circleImageView)
            .build()
        Coil.imageLoader(context).enqueue(request)

        holder.nameTV.text = userScore.name

        if (userScore.position == -1)
            holder.positionTV.text = "#${position + 1}"
        else
            holder.positionTV.text = "#${userScore.position}"


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
