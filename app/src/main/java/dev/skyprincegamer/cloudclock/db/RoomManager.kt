package dev.skyprincegamer.cloudclock.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.Executors

object RoomManager {
    private var _database : AlarmDatabase? = null
    fun getDB(ctxt : Context) : AlarmDatabase{
        if(_database == null){
            _database = Room.databaseBuilder(
                ctxt,
                AlarmDatabase::class.java, "alarm"
            ).setQueryCallback(RoomDatabase.QueryCallback { q, bindArgs ->
                Log.d("RoomUpdater", "The query $q, The args $bindArgs")
            }, Executors.newSingleThreadExecutor())
                .enableMultiInstanceInvalidation()
                .build()
        }
        return _database!!
    }
    fun cleanup(){
        _database = null
    }
}