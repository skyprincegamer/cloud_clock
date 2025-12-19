package dev.skyprincegamer.cloudclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.skyprincegamer.cloudclock.db.RoomManager

class RebootReAlarm : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == "android.intent.action.BOOT_COMPLETED"){
            val db = RoomManager.getDB(context)
            val alarms = db.alarmDAO().getAll()
            for(a in alarms){
                try{
                    AlarmScheduler(context).scheduleAlarm(a.alarm_id!!, a.alarm_at)
                    Log.i("AlarmSchedulerReboot" , "Alarm set ${a.alarm_id} , at ${a.alarm_at} UTC")

                }
                catch (e : IllegalArgumentException){
                    Log.e("AlarmSchedulerReboot" , "Coudlnt set alarm ${a.alarm_id!!} , at ${a.alarm_at}" , e)
                }
            }
        }
    }
}