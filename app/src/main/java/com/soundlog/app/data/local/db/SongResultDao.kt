package com.soundlog.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.soundlog.app.data.local.entity.SongResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongResultEntity): Long

    @Update
    suspend fun update(song: SongResultEntity)

    @Query("SELECT * FROM song_results ORDER BY detectedAt DESC")
    fun getAllSongsFlow(): Flow<List<SongResultEntity>>

    @Query("SELECT * FROM song_results ORDER BY detectedAt DESC LIMIT :limit")
    suspend fun getRecentSongs(limit: Int = 20): List<SongResultEntity>

    @Query("SELECT * FROM song_results WHERE telegramStatus = 'PENDING' ORDER BY detectedAt ASC")
    suspend fun getPendingTelegramSongs(): List<SongResultEntity>

    @Query("SELECT * FROM song_results WHERE songKey = :songKey AND detectedAt >= :sinceTimestamp LIMIT 1")
    suspend fun findRecentDuplicate(songKey: String, sinceTimestamp: Long): SongResultEntity?

    @Query("SELECT COUNT(*) FROM song_results WHERE detectedAt >= :startOfDay")
    suspend fun getTodayCount(startOfDay: Long): Int

    @Query("DELETE FROM song_results WHERE id NOT IN (SELECT id FROM song_results ORDER BY detectedAt DESC LIMIT :maxCount)")
    suspend fun pruneOldSongs(maxCount: Int)

    @Query("DELETE FROM song_results")
    suspend fun clearAll()
}
