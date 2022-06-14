package com.leandro.uberclone.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.leandro.uberclone.R
import com.leandro.uberclone.model.DriverInfoModel
import java.lang.StringBuilder

object Common{
    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentUser!!.firstName)
            .append(" ")
            .append(currentUser!!.lastName)
            .toString()
    }

    fun showNotification(context : Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent : PendingIntent? = null
        intent?.let {
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Uber Clone",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Uber Clone"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_baseline_directions_car_24))
            .priority = NotificationCompat.PRIORITY_HIGH
        pendingIntent?.let {
            builder.setContentIntent(pendingIntent)
        }
        val notification = builder.build()
        notificationManager.notify(id, notification)


    }

    val NOTIFICATION_CHANNEL_ID = "leandro_dev_uber_clone"
    val NOTI_BODY: String? = "body"
    val NOTI_TITLE: String? = "title"
    var currentUser: DriverInfoModel? = null
    val DRIVER_INFO_REFERENCE: String = "DriverInfo"
    val DRIVERS_LOCATION_REFERENCE: String = "DriversLocation"
    val TOKEN_REFERENCE: String = "Token"

}