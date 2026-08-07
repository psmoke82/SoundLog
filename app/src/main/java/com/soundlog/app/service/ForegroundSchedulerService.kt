package com.soundlog.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.soundlog.app.R
import com.soundlog.app.SoundLogApp
import com.soundlog.app.automation.SongKeyNormalizer
import com.soundlog.app.data.local.entity.SongResultEntity
import com.soundlog.app.ui.MainActivity
import com.soundlog.app.util.PowerManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForegroundSchedulerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var powerManagerHelper: PowerManagerHelper
    private lateinit var watchdogManager: WatchdogManager

    private var isLoopRunning = false

    override fun onCreate() {
        super.onCreate()
        powerManagerHelper = PowerManagerHelper(this)
        watchdogManager = WatchdogManager(this)
        Log.i(TAG, "ForegroundSchedulerService Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        startForegroundInternal()

        if (!isLoopRunning) {
            isLoopRunning = true
            startSchedulerLoop()
        }

        return START_STICKY
    }

    private fun startForegroundInternal() {
        val notification = createNotification("SoundLog 24시간 가동 중...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startSchedulerLoop() {
        serviceScope.launch {
            watchdogManager.recordLog("SERVICE_START", "Foreground Service 가동 시작")

            while (isActive && isLoopRunning) {
                val settings = SoundLogApp.instance.settingsRepository
                val intervalMinutes = settings.recognitionIntervalMinutes

                if (settings.isServiceEnabled) {
                    executeRecognitionCycle()
                } else {
                    Log.d(TAG, "Service is currently disabled by user setting.")
                }

                // 지정된 주기(분) 만큼 대기 (최소 1분)
                val waitTimeMs = (intervalMinutes.coerceAtLeast(1) * 60 * 1000L)
                Log.d(TAG, "Scheduler waiting for next cycle: ${intervalMinutes}m (${waitTimeMs}ms)")
                delay(waitTimeMs)
            }
        }
    }

    suspend fun executeRecognitionCycle(): Boolean = withContext(Dispatchers.Main) {
        val app = SoundLogApp.instance
        val settings = app.settingsRepository
        val db = app.database

        watchdogManager.recordLog("CYCLE_START", "인식 주기 도달 - WakeLock 획득 및 인식 시작")

        // 1. WakeLock 획득 (화면 ON)
        powerManagerHelper.acquireWakeLockAndTurnScreenOn(timeoutMs = (settings.maxTimeoutSeconds + 10) * 1000L)

        return@withContext try {
            // 2. 텔레그램 오프라인 미전송 큐 처리
            app.telegramQueueManager.processPendingQueue()

            // 3. AccessibilityService 활성화 여부 확인
            val accessibilityService = ShazamAccessibilityService.instance
            if (accessibilityService == null) {
                val errorMsg = "접근성 서비스(AccessibilityService)가 꺼져 있습니다."
                Log.e(TAG, errorMsg)
                watchdogManager.recordFailure(errorMsg)
                return@withContext false
            }

            // 4. Shazam 인식 수행
            var cycleSuccess = false
            accessibilityService.startRecognitionFlow(settings.maxTimeoutSeconds) { result ->
                serviceScope.launch(Dispatchers.IO) {
                    if (result.success && !result.artist.isNullOrBlank() && !result.title.isNullOrBlank()) {
                        val artist = result.artist
                        val title = result.title
                        val songKey = SongKeyNormalizer.generateSongKey(artist, title)

                        // 중복 검사 (예: 10분 이내 동일 곡)
                        val isDup = SongKeyNormalizer.isDuplicate(
                            db.songResultDao(),
                            artist,
                            title,
                            settings.deduplicationWindowMinutes
                        )

                        if (isDup) {
                            Log.i(TAG, "Duplicate song detected ($songKey). Skipping Telegram broadcast.")
                            watchdogManager.recordLog("DUPLICATE_SKIP", "중복 곡 발송 스킵: $artist - $title")
                        } else {
                            val newSong = SongResultEntity(
                                artist = artist,
                                title = title,
                                songKey = songKey
                            )
                            app.telegramQueueManager.enqueueAndSend(newSong)
                            watchdogManager.recordSuccess(artist, title)
                            updateNotification("최근 인식: $artist - $title")
                        }
                        cycleSuccess = true
                    } else {
                        val failReason = result.errorMessage ?: "알 수 없는 인식 실패"
                        Log.w(TAG, "Recognition Failed: $failReason")
                        watchdogManager.recordFailure(failReason, isNoMatch = result.isNoMatch)
                    }
                }
            }

            cycleSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Exception during recognition cycle", e)
            watchdogManager.recordFailure("실행 도중 예외 발생: ${e.localizedMessage}")
            false
        } finally {
            // 5. WakeLock 필수 해제
            powerManagerHelper.releaseWakeLock()
            watchdogManager.recordLog("CYCLE_END", "인식 주기 완료 - WakeLock 해제")
        }
    }

    private fun createNotification(statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SoundLogApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SoundLog 24시간 가동 중")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = createNotification(statusText)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        isLoopRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        powerManagerHelper.releaseWakeLock()
        Log.i(TAG, "ForegroundSchedulerService Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ForegroundScheduler"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.soundlog.app.ACTION_STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, ForegroundSchedulerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundSchedulerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
