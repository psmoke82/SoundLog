package com.soundlog.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.soundlog.app.data.local.db.AppDatabase
import com.soundlog.app.data.local.pref.EncryptedSettingsRepository
import com.soundlog.app.data.remote.telegram.TelegramQueueManager

class SoundLogApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: EncryptedSettingsRepository
        private set

    lateinit var telegramQueueManager: TelegramQueueManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        settingsRepository = EncryptedSettingsRepository(this)
        telegramQueueManager = TelegramQueueManager(database.songResultDao(), settingsRepository)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.foreground_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SoundLog 24시간 실시간 스케줄러 뷰어"
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "soundlog_service_channel"
        
        lateinit var instance: SoundLogApp
            private set
    }
}
