package com.soundlog.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soundlog.app.data.local.entity.ExecutionLogEntity
import com.soundlog.app.data.local.entity.ServiceStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceStatusDao {
    @Query("SELECT * FROM service_status WHERE id = 1")
    fun getStatusFlow(): Flow<ServiceStatusEntity?>

    @Query("SELECT * FROM service_status WHERE id = 1")
    suspend fun getStatus(): ServiceStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: ServiceStatusEntity)
}

@Dao
interface ExecutionLogDao {
    @Insert
    suspend fun insert(log: ExecutionLogEntity)

    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 1000): Flow<List<ExecutionLogEntity>>

    // 음악 식별 관련 로그만 (SUCCESS, NO_MATCH, FAILURE, DUPLICATE_SKIP)
    @Query("SELECT * FROM execution_logs WHERE step IN ('SUCCESS', 'NO_MATCH', 'FAILURE', 'DUPLICATE_SKIP') ORDER BY timestamp DESC LIMIT :limit")
    fun getSongRecognitionLogsFlow(limit: Int = 1000): Flow<List<ExecutionLogEntity>>

    // 앱 시스템 작동 로그만 (SERVICE_START, SERVICE_STOP, CYCLE_START, CYCLE_END, TELEGRAM_QUEUE, WATCHDOG_RECOVERY 등)
    @Query("SELECT * FROM execution_logs WHERE step NOT IN ('SUCCESS', 'NO_MATCH', 'FAILURE', 'DUPLICATE_SKIP') ORDER BY timestamp DESC LIMIT :limit")
    fun getSystemLogsFlow(limit: Int = 1000): Flow<List<ExecutionLogEntity>>

    @Query("DELETE FROM execution_logs WHERE id NOT IN (SELECT id FROM execution_logs ORDER BY timestamp DESC LIMIT :maxCount)")
    suspend fun pruneOldLogs(maxCount: Int = 1000)

    @Query("DELETE FROM execution_logs")
    suspend fun clearLogs()
}
