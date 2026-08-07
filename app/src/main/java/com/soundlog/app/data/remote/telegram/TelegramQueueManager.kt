package com.soundlog.app.data.remote.telegram

import android.util.Log
import com.soundlog.app.data.local.db.SongResultDao
import com.soundlog.app.data.local.entity.SongResultEntity
import com.soundlog.app.data.local.pref.EncryptedSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TelegramQueueManager(
    private val songResultDao: SongResultDao,
    private val settingsRepository: EncryptedSettingsRepository
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val api: TelegramBotApi = Retrofit.Builder()
        .baseUrl("https://api.telegram.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TelegramBotApi::class.java)

    suspend fun enqueueAndSend(song: SongResultEntity): Boolean = withContext(Dispatchers.IO) {
        val insertedId = songResultDao.insert(song)
        val currentSong = song.copy(id = insertedId)
        return@withContext sendSingleSong(currentSong)
    }

    suspend fun processPendingQueue(): Int = withContext(Dispatchers.IO) {
        val pendingSongs = songResultDao.getPendingTelegramSongs()
        var successCount = 0
        for (song in pendingSongs) {
            if (sendSingleSong(song)) {
                successCount++
            }
        }
        return@withContext successCount
    }

    suspend fun testConnection(rawToken: String, rawChatId: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = sanitizeBotToken(rawToken)
            val chatId = sanitizeChatId(rawChatId)

            if (token.isBlank() || chatId.isBlank()) {
                return@withContext Result.failure(Exception("텔레그램 토큰 또는 Chat ID가 비어있습니다."))
            }

            val url = "https://api.telegram.org/bot$token/sendMessage"
            val testMsg = "<b>[SoundLog]</b> 텔레그램 연동 테스트 메시지입니다. ✅\n시간: ${formatTimestamp(System.currentTimeMillis())}"
            val response = api.sendMessage(url, chatId, testMsg)
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success("텔레그램 전송 성공! (Msg ID: ${response.body()?.result?.messageId})")
            } else {
                val errorMsg = response.body()?.description ?: response.message() ?: "알 수 없는 오류"
                Result.failure(Exception("텔레그램 API 오류: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("텔레그램 전송 실패: ${e.localizedMessage}"))
        }
    }

    private suspend fun sendSingleSong(song: SongResultEntity): Boolean {
        val rawToken = settingsRepository.telegramBotToken
        val rawChatId = settingsRepository.telegramChatId

        val token = sanitizeBotToken(rawToken)
        val chatId = sanitizeChatId(rawChatId)

        if (token.isBlank() || chatId.isBlank()) {
            Log.w(TAG, "Telegram Token or ChatID is not configured.")
            songResultDao.update(
                song.copy(
                    telegramStatus = SongResultEntity.STATUS_PENDING,
                    errorMessage = "Telegram Bot Token 또는 Chat ID 미설정"
                )
            )
            return false
        }

        if (song.retryCount >= settingsRepository.maxRetryCount) {
            songResultDao.update(
                song.copy(
                    telegramStatus = SongResultEntity.STATUS_FAILED,
                    errorMessage = "최대 재시도 횟수(${settingsRepository.maxRetryCount}회) 초과"
                )
            )
            return false
        }

        val messageText = formatSongMessage(song)
        val url = "https://api.telegram.org/bot$token/sendMessage"

        return try {
            val response = api.sendMessage(url, chatId, messageText)
            if (response.isSuccessful && response.body()?.ok == true) {
                val msgId = response.body()?.result?.messageId
                songResultDao.update(
                    song.copy(
                        telegramStatus = SongResultEntity.STATUS_SENT,
                        telegramMessageId = msgId,
                        errorMessage = null
                    )
                )
                true
            } else {
                val errorMsg = response.body()?.description ?: "HTTP ${response.code()}"
                songResultDao.update(
                    song.copy(
                        telegramStatus = SongResultEntity.STATUS_PENDING,
                        retryCount = song.retryCount + 1,
                        errorMessage = errorMsg
                    )
                )
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send telegram message for ${song.title}", e)
            songResultDao.update(
                song.copy(
                    telegramStatus = SongResultEntity.STATUS_PENDING,
                    retryCount = song.retryCount + 1,
                    errorMessage = e.localizedMessage ?: "네트워크 통신 오류"
                )
            )
            false
        }
    }

    private fun sanitizeBotToken(rawToken: String): String {
        var cleaned = rawToken.trim()
        if (cleaned.contains("bot")) {
            val botIndex = cleaned.indexOf("bot")
            cleaned = cleaned.substring(botIndex + 3)
        }
        if (cleaned.contains("/")) {
            cleaned = cleaned.substring(0, cleaned.indexOf("/"))
        }
        return cleaned.trim()
    }

    private fun sanitizeChatId(rawChatId: String): String {
        var cleaned = rawChatId.trim()
        if (cleaned.startsWith("https://t.me/")) {
            cleaned = "@" + cleaned.removePrefix("https://t.me/")
        }
        return cleaned
    }

    private fun formatSongMessage(song: SongResultEntity): String {
        val timeStr = formatTimestamp(song.detectedAt)
        return """
            🎵 <b>[지금 나오는 음악]</b>
            
            🎤 <b>아티스트:</b> ${escapeHtml(song.artist)}
            🎼 <b>곡명:</b> ${escapeHtml(song.title)}
            ⏰ <b>인식시간:</b> $timeStr
            
            #SoundLog #음악식별
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        return sdf.format(Date(timestamp))
    }

    companion object {
        private const val TAG = "TelegramQueueManager"
    }
}
