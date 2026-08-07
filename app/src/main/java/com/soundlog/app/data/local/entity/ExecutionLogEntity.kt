package com.soundlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val step: String, // e.g. "SHAZAM_LAUNCH", "NODE_FIND", "SONG_DETECTED", "TELEGRAM_SEND", "FAIL"
    val message: String,
    val isSuccess: Boolean = true
)
