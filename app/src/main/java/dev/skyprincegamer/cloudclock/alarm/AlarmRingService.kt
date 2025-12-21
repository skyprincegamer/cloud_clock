package dev.skyprincegamer.cloudclock.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmRingService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val pIntent = PendingIntent.getActivity(this@AlarmRingService , 23453 , Intent(this@AlarmRingService , AlarmScreen::class.java).apply {
            putExtra("ALARM_AT" , intent?.getStringExtra("ALARM_AT"))
        } ,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this , "alarmChannel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Your alarm is ringing")
            .setContentText("Alarm at ${intent?.getStringExtra("ALARM_AT")}")
            .setOngoing(true)
            .setAutoCancel(false)
            .setUsesChronometer(true)
            .setFullScreenIntent(pIntent, true)
            .build()
        startForeground(1, notification)
        return START_STICKY
    }
    override fun onBind(intent: Intent) = null
}