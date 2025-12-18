package dev.skyprincegamer.cloudclock.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AlarmScheduler(private val ctxt : Context) {
    private val manager : AlarmManager = ctxt.getSystemService(ALARM_SERVICE) as AlarmManager
    fun scheduleAlarm(alarm_id : Int , alarm_at: String){
        if (checkIfAlarmExists(alarm_id))
            throw DuplicateInsertionException("Alarm with ID $alarm_id already exists")
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val utcTime = LocalDateTime.parse(alarm_at, format)
        val milis = utcTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        if (milis > System.currentTimeMillis()) {
            val pendingIntent = PendingIntent.getBroadcast(
                ctxt,
                alarm_id,
                Intent(ctxt, AlarmReceiver::class.java).apply {
                    putExtra("ALARM_MSG", "my alarm message")
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                milis,
                pendingIntent
            )
        }
        else
            throw IllegalStateException("cannot set alarm in past")

    }
    fun checkIfAlarmExists(alarm_id : Int) : Boolean {
        return (PendingIntent.getBroadcast(ctxt , alarm_id ,
            Intent(ctxt, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null)
    }
    fun cancelAlarm(alarm_id: Int) {
        if(!checkIfAlarmExists(alarm_id))
            throw NoSuchElementException("No alarm with alarm_id $alarm_id exists")
        manager.cancel(
            PendingIntent.getBroadcast(
                ctxt,
                alarm_id,
                Intent(ctxt, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        )
        PendingIntent.getBroadcast(
            ctxt,
            alarm_id,
            Intent(ctxt, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ).cancel()
    }
}