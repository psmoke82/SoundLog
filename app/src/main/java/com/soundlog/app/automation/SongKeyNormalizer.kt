package com.soundlog.app.automation

import com.soundlog.app.data.local.db.SongResultDao

object SongKeyNormalizer {

    fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        return input.lowercase()
            .replace(Regex("[^a-z0-9가-힣ㄱ-ㅎㅏ-ㅣ]"), "") // 특수문자 및 공백 제거
            .trim()
    }

    fun generateSongKey(artist: String, title: String): String {
        val normArtist = normalizeText(artist)
        val normTitle = normalizeText(title)
        return "$normArtist|$normTitle"
    }

    suspend fun isDuplicate(
        songResultDao: SongResultDao,
        artist: String,
        title: String,
        deduplicationWindowMinutes: Int
    ): Boolean {
        val songKey = generateSongKey(artist, title)
        val windowMs = deduplicationWindowMinutes * 60 * 1000L
        val sinceTimestamp = System.currentTimeMillis() - windowMs
        val existing = songResultDao.findRecentDuplicate(songKey, sinceTimestamp)
        return existing != null
    }
}
