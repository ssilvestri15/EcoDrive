package com.silvered.ecodrive.util.helpers

import com.silvered.ecodrive.util.CustomObjects

object ProfileHelper {

    var needToUpdate: Boolean = true

    var listRoutes: ArrayList<CustomObjects.Route>? = null

    fun resetAll() {
        needToUpdate = true
        listRoutes = null
    }

}