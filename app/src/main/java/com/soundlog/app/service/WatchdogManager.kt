package com.soundlog.app.service

import android.content.Context
import android.util.Log
import com.soundlog.app.data.local.db.AppDatabase
import com.soundlog.app.data.local.entity.ExecutionLogEntity
import com.soundlog.app.data.local.entity.ServiceStatusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WatchdogManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val statusDao = db.serviceStatusDao()
    private val logDao = db.executionLogDao()

    suspend fun recordSuccess(artist: String, title: String) = withContext(Dispatchers.IO) {
        val current = statusDao.getStatus() ?: ServiceStatusEntity()
        val now = System.currentTimeMillis()

        val updated = current.copy(
            isRunning = true,
            lastRunAt = now,
            lastSuccessAt = now,
            lastArtist = artist,
            lastTitle = title,
            consecutiveFailureCount = 0,
            todaySuccessCount = current.todaySuccessCount + 1,
            lastErrorMessage = null
        )
        statusDao.insertOrUpdate(updated)

        logDao.insert(
            ExecutionLogEntity(
                step = "SUCCESS",
                message = "인식 성공: $artist - $title",
                isSuccess = true
            )
        )
    }

    suspend fun recordFailure(reason: String, isNoMatch: Boolean = false) = withContext(Dispatchers.IO) {
        val current = statusDao.getStatus() ?: ServiceStatusEntity()
        val now = System.currentTimeMillis()
        val newFailureCount = if (isNoMatch) current.consecutiveFailureCount else current.consecutiveFailureCount + 1

        val updated = current.copy(
            isRunning = true,
            lastRunAt = now,
            lastErrorAt = now,
            consecutiveFailureCount = newFailureCount,
            todayFailureCount = current.todayFailureCount + 1,
            lastErrorMessage = reason
        )
        statusDao.insertOrUpdate(updated)

        logDao.insert(
            ExecutionLogEntity(
                step = if (isNoMatch) "NO_MATCH" else "FAILURE",
                message = reason,
                isSuccess = false
            )
        )

        // 3회 연속 시스템/자동화 오류 발생 시 복구 수행
        if (newFailureCount >= MAX_CONSECUTIVE_FAILURES) {
            triggerAutoRecovery(newFailureCount)
        }
    }

    suspend fun recordLog(step: String, message: String, isSuccess: Boolean = true) = withContext(Dispatchers.IO) {
        logDao.insert(
            ExecutionLogEntity(
                step = step,
                message = message,
                isSuccess = isSuccess
            )
        )
    }

    private suspend fun triggerAutoRecovery(failureCount: Int) {
        Log.w(TAG, "Watchdog: $failureCount consecutive failures detected. Triggering recovery!")

        logDao.insert(
            ExecutionLogEntity(
                step = "WATCHDOG_RECOVERY",
                message = "$failureCount 회 연속 실패 감지. Shazam 강제 종료 및 복구 시도",
                isSuccess = false
            )
        )

        try {
            // Shazam 강제 종료 명령어 실행 (Shell 권한 범위 내)
            Runtime.getRuntime().exec("am force-stop com.shazam.android")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force-stop Shazam", e)
        }
    }

    companion object {
        private const val TAG = "WatchdogManager"
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
