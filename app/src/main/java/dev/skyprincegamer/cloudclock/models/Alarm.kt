package dev.skyprincegamer.cloudclock.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Alarm(
    @PrimaryKey val alarm_id:Int? = null,
    val created_at : String? = null,
    val alarm_at : String,
    val user_id : String,
    val active : Boolean
)