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
    fun getRecentLogsFlow(limit: Int = 100): Flow<List<ExecutionLogEntity>>

    @Query("DELETE FROM execution_logs")
    suspend fun clearLogs()
}
