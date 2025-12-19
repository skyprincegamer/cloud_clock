package dev.skyprincegamer.cloudclock.sup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.skyprincegamer.cloudclock.db.AlarmDatabase
import dev.skyprincegamer.cloudclock.db.RoomManager
import dev.skyprincegamer.cloudclock.models.Alarm
import dev.skyprincegamer.cloudclock.alarm.AlarmScheduler
import dev.skyprincegamer.cloudclock.util.DuplicateInsertionException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors


class SupabaseRealtimeService : Service() {
    private lateinit var supabaseClient: SupabaseClient
    private var realtimeChannel: RealtimeChannel? = null
    private lateinit var db : AlarmDatabase

    override fun onCreate() {
        super.onCreate()
        db = RoomManager.getDB(this)
        supabaseClient = SupabaseManager.getClient()
        startForeground(1, createNotification())
        runBlocking{ doInitialSync() }
        setupSupabaseRealtime()
    }

    private suspend fun doInitialSync() {
        try {
            val supabaseAlarms = supabaseClient.from("alarms")
                .select()
                .decodeList<Alarm>()

            val supabaseIds = supabaseAlarms.mapNotNull { it.alarm_id }.toSet()

            for (alarm in supabaseAlarms) {
                try {
                    AlarmScheduler(this@SupabaseRealtimeService).scheduleAlarm(
                        alarm.alarm_id!!,
                        alarm.alarm_at
                    )
                    db.alarmDAO().upsert(alarm)
                } catch (e: Exception) {
                    Log.e("Scheduler", "Failed to schedule alarm ${alarm.alarm_id}", e)
                    supabaseClient.from("alarms").delete {
                        filter {
                            Alarm::alarm_id eq alarm.alarm_id
                        }
                    }
                    db.alarmDAO().delete(alarm.alarm_id!!)
                }
            }

            val localAlarms = db.alarmDAO().getAll().first()

            for (localAlarm in localAlarms) {
                if (localAlarm.alarm_id !in supabaseIds) {
                    db.alarmDAO().delete(localAlarm.alarm_id!!)
                    try {
                        AlarmScheduler(this@SupabaseRealtimeService).cancelAlarm(localAlarm.alarm_id)
                    } catch (e: Exception) {
                        Log.e("Scheduler", "Failed to cancel alarm ${localAlarm.alarm_id}", e)
                    }
                }
            }

            Log.i("SupabaseRealtimeService", "Initial sync completed: ${supabaseAlarms.size} alarms")

        } catch (e: Exception) {
            Log.e("SupabaseRealtimeService", "Initial sync failed", e)
        }
    }


    private fun setupSupabaseRealtime() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                realtimeChannel = supabaseClient.channel("alarms-channel")
                realtimeChannel!!.postgresChangeFlow<PostgresAction>(schema = "public"){
                        table = "alarms"
                    }.onEach { change ->
                        when (change) {
                            is PostgresAction.Insert -> {
                                val newAlarm = change.decodeRecord<Alarm>()
                                Log.d("SupabaseRealtimeService", "new alarm ${newAlarm} for insert")
                                handleNewAlarm(newAlarm)
                            }

                            is PostgresAction.Update -> {
                                val newAlarm = change.decodeRecord<Alarm>()
                                Log.d("SupabaseRealtimeService", "new alarm ${newAlarm} for update")
                                handleUpdatedAlarm(newAlarm)
                            }

                            is PostgresAction.Delete -> {
                                val deletedId = change.oldRecord["alarm_id"].toString().toInt()
                                Log.d("SupabaseRealtimeService", "alarm deleted with id ${deletedId}")
                                handleDeletedAlarm(deletedId)
                            }

                            else -> {}
                        }
                    }.launchIn(this)


                realtimeChannel?.subscribe()

                Log.d("SupabaseRealtimeService", "Connected to Supabase Realtime")
            } catch (e: Exception) {
                Log.e("SupabaseRealtimeService", "Failed to connect: ${e.message}")
            }
        }
    }

    private fun handleNewAlarm(alarm: Alarm) {
        CoroutineScope(Dispatchers.IO).launch {
            db.alarmDAO().insert(alarm)
            try{
                AlarmScheduler(this@SupabaseRealtimeService).scheduleAlarm(alarm.alarm_id!! , alarm.alarm_at)
            }
            catch (e: IllegalArgumentException){
                e.message?.let { Log.e("Scheduler" ,it) }
            }
            catch(e : DuplicateInsertionException){
                e.message?.let { Log.e("Scheduler" , it) }
            }
        }
    }

    private fun handleUpdatedAlarm(alarm: Alarm) {
        CoroutineScope(Dispatchers.IO).launch {
            db.alarmDAO().update(alarm)

            if(!AlarmScheduler(this@SupabaseRealtimeService).checkIfAlarmExists(alarm.alarm_id!!) && alarm.active) {
                try{
                    AlarmScheduler(this@SupabaseRealtimeService).scheduleAlarm(alarm.alarm_id , alarm.alarm_at)
                }
                catch (e: IllegalArgumentException){
                    e.message?.let { Log.e("Scheduler" ,it) }
                }
                }
        }
    }

    private fun handleDeletedAlarm(oldID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            db.alarmDAO().delete(oldID)
            try {
                AlarmScheduler(this@SupabaseRealtimeService).cancelAlarm(oldID)
            }
            catch (_: NoSuchElementException){
                Log.d("Scheduler" , "Attempted to delete non existent alarm")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart if killed
    }

    override fun onDestroy() {
        super.onDestroy()

        runBlocking{
            realtimeChannel?.unsubscribe()
        }
        Log.d("SupabaseRealtimeService", "Disconnected from Supabase Realtime")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "supabase_realtime_service"

        val channel = NotificationChannel(
            channelId,
            "Cloud Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Syncing alarms with cloud"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Cloud Clock")
            .setContentText("Syncing alarms...")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}