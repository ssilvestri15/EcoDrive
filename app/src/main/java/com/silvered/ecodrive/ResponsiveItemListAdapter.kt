package com.silvered.ecodrive

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import kotlin.math.abs


class ResponsiveItemListAdapter(
    context: Context,
    mList: ArrayList<HomeFragment.UserScore>,
    rv: RecyclerView,
    rows: Int,
    myId: String
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var context: Context
    var layoutInflater: LayoutInflater
    var mList: ArrayList<HomeFragment.UserScore>
    var recyclerView: RecyclerView
    var numberOfRows: Int
    var rowHeightInPx = 0
    var itemHeightCalculationCompleted = false
    var myId: String

    override fun getItemCount(): Int {
        return if (itemHeightCalculationCompleted) mList.size else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val vh: RecyclerView.ViewHolder
        val view: View = layoutInflater.inflate(
            com.silvered.ecodrive.R.layout.ranking_item_layout_test,
            parent,
            false
        )

        if (rowHeightInPx > 0) {
            val layoutParams = view.layoutParams as RecyclerView.LayoutParams
            layoutParams.height = rowHeightInPx
            layoutParams.width = MATCH_PARENT
            view.layoutParams = layoutParams
        }

        vh = GeneralViewHolder(view)

        return vh
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        Log.d("SSS","$position")

        val userScore = mList[position]
        holder as GeneralViewHolder

        if (myId != userScore.id) {
            holder.getCardView().strokeWidth = 0
        }

        holder.getNameTV().text = userScore.name
        holder.getPositionTV().text = "#${position+1}"
        holder.getPointsTV().text = "${userScore.score}"
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
    }

    inner class GeneralViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var view: View
        private var cardView: MaterialCardView
        private var positionTV: MaterialTextView
        private var nameTV: MaterialTextView
        private var pointsTV: MaterialTextView

        fun getView(): View {
            return view
        }

        fun getCardView(): MaterialCardView {
            return cardView
        }

        fun getPositionTV(): MaterialTextView {
            return positionTV
        }

        fun getNameTV(): MaterialTextView {
            return nameTV
        }

        fun getPointsTV(): MaterialTextView {
            return pointsTV
        }

        init {
            view = itemView
            cardView = itemView.findViewById(R.id.rank_card_view)
            positionTV = itemView.findViewById(R.id.rank_pos)
            nameTV = itemView.findViewById(R.id.rank_name)
            pointsTV = itemView.findViewById(R.id.rank_point)
        }
    }

    init {
        this.context = context
        this.mList = mList
        this.myId = myId
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        recyclerView = rv
        numberOfRows = rows
        if (numberOfRows > 0) {
            recyclerView.post {
                if (recyclerView.measuredHeight > 0) {
                    rowHeightInPx = recyclerView.measuredHeight / numberOfRows
                    itemHeightCalculationCompleted = true
                    notifyDataSetChanged()
                }
            }
        } else {
            itemHeightCalculationCompleted = true
        }
    }
}