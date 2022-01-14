package com.silvered.ecodrive

import android.app.Application
import com.google.android.material.color.DynamicColors

class StartClass:Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}