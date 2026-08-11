package com.soundlog.app.data.local.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedSettingsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = createSafeSharedPreferences(context)

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
        private const val KEY_MESSAGE_FORMAT = "telegram_message_format"
        private const val KEY_ALBUM_ART_OPTION = "album_art_option"
        private const val KEY_MUSIC_LINK_OPTION = "music_link_option"

        const val ALBUM_ART_NONE = "NONE"
        const val ALBUM_ART_SHAZAM = "SHAZAM"
        const val ALBUM_ART_ITUNES = "ITUNES"

        const val MUSIC_LINK_NONE = "NONE"
        const val MUSIC_LINK_YOUTUBE = "YOUTUBE"

        val DEFAULT_TELEGRAM_FORMAT = """
            🎵 <b>[지금 나오는 음악]</b>

            🎤 <b>아티스트:</b> {artist}
            🎼 <b>곡명:</b> {title}

            #SoundLog #alookat
        """.trimIndent()

        private fun createSafeSharedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                try {
                    context.deleteSharedPreferences(PREF_NAME)
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        context,
                        PREF_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (e2: Exception) {
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    var telegramBotToken: String
        get() = sharedPreferences.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_BOT_TOKEN, value).apply()

    var telegramChatId: String
        get() = sharedPreferences.getString(KEY_CHAT_ID, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_CHAT_ID, value).apply()

    var recognitionIntervalMinutes: Int
        get() = sharedPreferences.getInt(KEY_INTERVAL_MIN, 2)
        set(value) = sharedPreferences.edit().putInt(KEY_INTERVAL_MIN, value).apply()

    var maxTimeoutSeconds: Int
        get() = sharedPreferences.getInt(KEY_MAX_TIMEOUT_SEC, 30)
        set(value) = sharedPreferences.edit().putInt(KEY_MAX_TIMEOUT_SEC, value).apply()

    var deduplicationWindowMinutes: Int
        get() = sharedPreferences.getInt(KEY_DEDUP_WINDOW_MIN, 10)
        set(value) = sharedPreferences.edit().putInt(KEY_DEDUP_WINDOW_MIN, value).apply()

    var maxRetryCount: Int
        get() = sharedPreferences.getInt(KEY_MAX_RETRY, 2)
        set(value) = sharedPreferences.edit().putInt(KEY_MAX_RETRY, value).apply()

    var maxSongLogCount: Int
        get() = sharedPreferences.getInt(KEY_MAX_SONG_LOG_COUNT, 1000)
        set(value) = sharedPreferences.edit().putInt(KEY_MAX_SONG_LOG_COUNT, value).apply()

    var isServiceEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var telegramMessageFormat: String
        get() = sharedPreferences.getString(KEY_MESSAGE_FORMAT, DEFAULT_TELEGRAM_FORMAT) ?: DEFAULT_TELEGRAM_FORMAT
        set(value) = sharedPreferences.edit().putString(KEY_MESSAGE_FORMAT, value).apply()

    var albumArtOption: String
        get() = sharedPreferences.getString(KEY_ALBUM_ART_OPTION, ALBUM_ART_NONE) ?: ALBUM_ART_NONE
        set(value) = sharedPreferences.edit().putString(KEY_ALBUM_ART_OPTION, value).apply()

    var musicLinkOption: String
        get() = sharedPreferences.getString(KEY_MUSIC_LINK_OPTION, MUSIC_LINK_NONE) ?: MUSIC_LINK_NONE
        set(value) = sharedPreferences.edit().putString(KEY_MUSIC_LINK_OPTION, value).apply()
}
