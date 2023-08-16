package com.radiusnetworks.example.flybuy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.radiusnetworks.example.flybuy.R
import com.radiusnetworks.flybuy.sdk.FlyBuyCore


class FlyBuyFirebaseService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the channel for the notification - IMPORTANCE_LOW does not play sound or vibrate
            val mChannel = NotificationChannel(getString(R.string.default_notification_channel_id),
                getString(R.string.default_notification_channel_name), NotificationManager.IMPORTANCE_HIGH)
            // Set the Notification Channel for the Notification Manager.
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d("Notification", "Refreshed token: $token")
        FlyBuyCore.onNewPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        message.let {
            it.notification?.let { notification ->
                val pm = packageManager
                val intent = pm.getLaunchIntentForPackage(packageName)?.also { intent ->
                    intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    it.data.let { data ->
                        Bundle().also { bundle ->
                            data.toMap<String, String>().forEach { item ->
                                bundle.putString(item.key, item.value)
                            }
                            intent.putExtras(bundle)
                        }
                    }
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
                val notif = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSmallIcon(R.drawable.ic_stat_default)
                    .setContentTitle(notification.title)
                    .setContentText(notification.body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setWhen(System.currentTimeMillis())
                    .build()

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(notification.tag, NOTIFICATION_ID, notif)

            }
                FlyBuyCore.onMessageReceived(it.data, null)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 8775
    }
}