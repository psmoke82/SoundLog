package com.soundlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_status")
data class ServiceStatusEntity(
    @PrimaryKey
    val id: Int = 1,
    val isRunning: Boolean = false,
    val lastRunAt: Long = 0,
    val lastSuccessAt: Long = 0,
    val lastErrorAt: Long = 0,
    val lastArtist: String? = null,
    val lastTitle: String? = null,
    val consecutiveFailureCount: Int = 0,
    val todaySuccessCount: Int = 0,
    val todayFailureCount: Int = 0,
    val telegramConnected: Boolean = false,
    val lastErrorMessage: String? = null
)
