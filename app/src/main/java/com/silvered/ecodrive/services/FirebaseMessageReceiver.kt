package com.silvered.ecodrive.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.silvered.ecodrive.R
import com.silvered.ecodrive.activity.LoginActivity

class FirebaseMessageReceiver: FirebaseMessagingService() {

    private val TAG = "Firebase Messaging Service"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            sendNotification(it)
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }


    override fun onNewToken(token: String) {
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        Firebase.auth.currentUser?.let { Firebase.database.reference.child("users").child(it.uid).child("token").setValue(token) }
    }

    private fun sendNotification(remoteMessage: RemoteMessage.Notification) {
        val notificationManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java) as NotificationManager
        notificationManager.sendNotification(remoteMessage, applicationContext)
    }

    fun NotificationManager.sendNotification(remoteMessage: RemoteMessage.Notification, applicationContext: Context) {


        if (remoteMessage.body == null)
            return

        val builder = NotificationCompat.Builder(
            applicationContext,
            applicationContext.getString(R.string.CHANNEL)
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (remoteMessage.title == null) "EcoDrive" else remoteMessage.title)
            .setContentText(remoteMessage.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Deliver the notification
        notify(0, builder.build())
    }


    fun NotificationManager.cancelNotifications() {
        cancelAll()
    }

}