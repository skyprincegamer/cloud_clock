package dev.skyprincegamer.cloudclock.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dev.skyprincegamer.cloudclock.sup.SupabaseRealtimeService

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
//        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
//
//        }
    }
}
private fun createNotificationChannel(context: Context?) {
    if(context == null) return
    val name = "The Alarm Channel"
    val descriptionText = "This is the Cloud Clock alarm channel!"
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel("alarmChannel", name, importance).apply {
        description = descriptionText
    }
    // Register the channel with the system.
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}
