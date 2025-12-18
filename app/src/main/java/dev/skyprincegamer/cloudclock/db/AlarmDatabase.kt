package dev.skyprincegamer.cloudclock.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.skyprincegamer.cloudclock.models.Alarm

@Database(entities = [Alarm::class], version = 1)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDAO(): AlarmDAO
}