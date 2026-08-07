package com.soundlog.app.data.local.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedSettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var telegramBotToken: String
        get() = sharedPreferences.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_BOT_TOKEN, value).apply()

    var telegramChatId: String
        get() = sharedPreferences.getString(KEY_CHAT_ID, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_CHAT_ID, value).apply()

    var recognitionIntervalMinutes: Int
        get() = sharedPreferences.getInt(KEY_INTERVAL_MIN, 5)
        set(value) = sharedPreferences.edit().putInt(KEY_INTERVAL_MIN, value).apply()

    var maxTimeoutSeconds: Int
        get() = sharedPreferences.getInt(KEY_MAX_TIMEOUT_SEC, 12)
        set(value) = sharedPreferences.edit().putInt(KEY_MAX_TIMEOUT_SEC, value).apply()

    var deduplicationWindowMinutes: Int
        get() = sharedPreferences.getInt(KEY_DEDUP_WINDOW_MIN, 10)
        set(value) = sharedPreferences.edit().putInt(KEY_DEDUP_WINDOW_MIN, value).apply()

    var maxRetryCount: Int
        get() = sharedPreferences.getInt(KEY_MAX_RETRY, 3)
        set(value) = sharedPreferences.edit().putInt(KEY_MAX_RETRY, value).apply()

    var maxSongLogCount: Int
        get() = sharedPreferences.getInt(KEY_MAX_SONG_LOG_COUNT, 1000)
        set(value) = sharedPreferences.edit().putInt(KEY_MAX_SONG_LOG_COUNT, value).apply()

    var isServiceEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    companion object {
        private const val PREF_NAME = "soundlog_encrypted_prefs"
        private const val KEY_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_CHAT_ID = "telegram_chat_id"
        private const val KEY_INTERVAL_MIN = "recognition_interval_min"
        private const val KEY_MAX_TIMEOUT_SEC = "max_timeout_sec"
        private const val KEY_DEDUP_WINDOW_MIN = "dedup_window_min"
        private const val KEY_MAX_RETRY = "max_retry"
        private const val KEY_MAX_SONG_LOG_COUNT = "max_song_log_count"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
    }
}
