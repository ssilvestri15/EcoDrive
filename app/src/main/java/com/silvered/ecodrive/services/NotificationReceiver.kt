package com.silvered.ecodrive.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.silvered.ecodrive.R
import java.util.*


class NotificationReceiver: BroadcastReceiver() {

    val notificationID = 66
    val channelID = "INACTIVITY"

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || intent == null)
            return


        val sharedPreferences = context.getSharedPreferences("info",Context.MODE_PRIVATE)
        val lastOpened = sharedPreferences.getString("lastOpen", null) ?: return
        val lastTime = lastOpened.toLong()
        val now = System.currentTimeMillis()

        if ((now - lastTime) < 129600000) {
            Log.e("DIFF",(now-lastTime).toString())
            return
        }

        val name = "Inactivity Channel"
        val desc = "User Inactivity"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc

        val builder = NotificationCompat.Builder(
            context,
            channelID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (intent.getStringExtra("title") == null) "EcoDrive" else intent.getStringExtra("title"))
            .setContentText(if (intent.getStringExtra("body") == null) "Breaking News! Per contribuire alla ricerca devi usare l'app!" else intent.getStringExtra("body"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(notificationID,builder.build())
    }

}