package dev.skyprincegamer.cloudclock.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.skyprincegamer.cloudclock.models.Alarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDAO {
    @Query("SELECT * FROM alarm")
    fun getAllFlow(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarm")
    fun getAll(): List<Alarm>

    @Insert
    suspend fun insert(a: Alarm)

    @Upsert
    suspend fun upsert(a : Alarm)
    @Update
    suspend fun update(a: Alarm)

    @Query("DELETE FROM alarm WHERE alarm_id = :old_id")
    suspend fun delete(old_id: Int)
}
