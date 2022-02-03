package com.silvered.ecodrive.util.helpers

import com.silvered.ecodrive.util.CustomObjects

object RankHelper {

    var needToUpdate: Boolean = true

    var listGlobal: ArrayList<CustomObjects.UserScore>? = null
    var listLocal: ArrayList<CustomObjects.UserScore>? = null

    fun resetAll() {
        listGlobal = null
        needToUpdate = true
        listLocal = null
    }

}