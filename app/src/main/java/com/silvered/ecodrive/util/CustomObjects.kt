package com.silvered.ecodrive.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class CustomObjects {

    class GridItem(val icon: Int, val text: String, val cardTitle: String)

    class UserScore(
        val position: Int,
        val imageURL: String,
        val id: String,
        val name: String,
        val score: Int
    )

    @Parcelize
    class ServerMessage(val speed: String, val limit: String) : Parcelable

    class Product(val name: String, val imageID: Int)

    class Route(
        val date: String,
        val km: Float,
        val vel: Float,
        val sdg: Float,
        val poly: String?,
        val punti: Float
    )
}