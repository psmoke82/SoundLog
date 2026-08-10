package com.soundlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_results")
data class SongResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val artist: String,
    val title: String,
    val songKey: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val telegramStatus: String = STATUS_PENDING, // "PENDING", "SENT", "FAILED"
    val retryCount: Int = 0,
    val telegramMessageId: Long? = null,
    val errorMessage: String? = null,
    val albumArtPath: String? = null,
    val albumArtUrl: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
    }
}
