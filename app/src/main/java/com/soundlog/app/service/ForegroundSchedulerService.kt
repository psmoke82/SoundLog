package com.soundlog.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForegroundSchedulerService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isLoopRunning = false

    private lateinit var powerManagerHelper: PowerManagerHelper
    private lateinit var watchdogManager: WatchdogManager

    inner class LocalBinder : Binder() {
        fun getService(): ForegroundSchedulerService = this@ForegroundSchedulerService
    }

    override fun onCreate() {
        super.onCreate()
        powerManagerHelper = PowerManagerHelper(this)
        watchdogManager = WatchdogManager(this)
        createNotificationChannel()
        Log.i(TAG, "ForegroundSchedulerService Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("SoundLog 24/7 음악 식별 서비스 가동 중")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                    )
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground notification: ${e.message}", e)
        }

        if (!isLoopRunning) {
            isLoopRunning = true
            startSchedulerLoop()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        isLoopRunning = false
        serviceScope.launch(Dispatchers.IO) {
            watchdogManager.recordLog("SERVICE_STOP", "Foreground Service 종료")
        }
        powerManagerHelper.releaseWakeLock()
        Log.i(TAG, "ForegroundSchedulerService Destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SoundLog Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SoundLog 상시 가동 채널"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(statusText: String) {
        val notification = createNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun startSchedulerLoop() {
        serviceScope.launch {
            watchdogManager.recordLog("SERVICE_START", "Foreground Service 가동 시작")

            while (isActive && isLoopRunning) {
                val settings = SoundLogApp.instance.settingsRepository
                val intervalMinutes = settings.recognitionIntervalMinutes
                val cycleStartTime = System.currentTimeMillis()

                if (settings.isServiceEnabled) {
                    executeRecognitionCycle()
                } else {
                    Log.d(TAG, "Service is currently disabled by user setting.")
                }

                // 고정 간격(Fixed-Rate) 대기시간 계산 (인식 소요시간 차감)
                val totalIntervalMs = (intervalMinutes.coerceAtLeast(1) * 60 * 1000L)
                val elapsedTimeMs = System.currentTimeMillis() - cycleStartTime
                val remainingWaitMs = (totalIntervalMs - elapsedTimeMs).coerceAtLeast(1000L)

                Log.d(TAG, "Scheduler cycle elapsed: ${elapsedTimeMs}ms. Waiting remaining ${remainingWaitMs}ms for next cycle (Fixed rate: ${intervalMinutes}m)")
                delay(remainingWaitMs)
            }
        }
    }

    suspend fun executeRecognitionCycle(): Boolean = withContext(Dispatchers.Main) {
        val app = SoundLogApp.instance
        val settings = app.settingsRepository
        val db = app.database

        watchdogManager.recordLog("CYCLE_START", "인식 주기 도달 - WakeLock 획득 및 인식 시작")

        // 1. WakeLock 획득 (화면 ON)
        powerManagerHelper.acquireWakeLockAndTurnScreenOn(timeoutMs = (settings.maxTimeoutSeconds + 15) * 1000L)

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

            // 4. Shazam 인식 동기 대기 (CompletableDeferred)
            val deferredResult = CompletableDeferred<Boolean>()

            accessibilityService.startRecognitionFlow(settings.maxTimeoutSeconds) { result ->
                serviceScope.launch(Dispatchers.IO) {
                    try {
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
                                var albumArtUrl: String? = null
                                var albumArtPath: String? = result.albumArtPath

                                when (settings.albumArtOption) {
                                    com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_ITUNES -> {
                                        albumArtUrl = com.soundlog.app.util.iTunesCoverArtFetcher.fetchCoverArtUrl(artist, title)
                                        albumArtPath = null
                                    }
                                    com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_NONE -> {
                                        albumArtUrl = null
                                        albumArtPath = null
                                    }
                                    com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_SHAZAM -> {
                                        albumArtUrl = null
                                    }
                                }

                                val newSong = SongResultEntity(
                                    artist = artist,
                                    title = title,
                                    songKey = songKey,
                                    albumArtPath = albumArtPath,
                                    albumArtUrl = albumArtUrl
                                )
                                val sendSuccess = app.telegramQueueManager.enqueueAndSend(newSong)
                                if (sendSuccess) {
                                    watchdogManager.recordSuccess(artist, title)
                                    updateNotification("최근 인식 성공: $artist - $title")
                                } else {
                                    watchdogManager.recordLog("TELEGRAM_QUEUE", "텔레그램 전송 실패 -> 대기 큐에 저장: $artist - $title")
                                }
                            }
                            deferredResult.complete(true)
                        } else {
                            val failReason = result.errorMessage ?: "알 수 없는 인식 실패"
                            Log.w(TAG, "Recognition Failed: $failReason")
                            watchdogManager.recordFailure(failReason, isNoMatch = result.isNoMatch)
                            deferredResult.complete(false)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling Shazam result", e)
                        deferredResult.complete(false)
                    }
                }
            }

            // Shazam 서비스 수음이 완전히 끝날 때까지 대기
            deferredResult.await()
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SoundLog 24/7 상시 음악 식별")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ForegroundScheduler"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "soundlog_foreground_channel"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, ForegroundSchedulerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch ForegroundSchedulerService: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundSchedulerService::class.java)
            context.stopService(intent)
        }
    }
}
