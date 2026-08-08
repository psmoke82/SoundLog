package com.soundlog.app.util

import com.soundlog.app.data.local.entity.SongResultEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramMessageFormatter {

    fun formatSongMessage(template: String, song: SongResultEntity): String {
        return formatMessage(
            template = template,
            title = song.title,
            artist = song.artist,
            timestamp = song.timestamp
        )
    }

    fun formatMessage(
        template: String,
        title: String,
        artist: String,
        timestamp: Long,
        escapeHtml: Boolean = true
    ): String {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        val sdfDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)

        val dateStr = sdfDate.format(Date(timestamp))
        val timeStr = sdfTime.format(Date(timestamp))
        val dateTimeStr = sdfDateTime.format(Date(timestamp))

        val finalTitle = if (escapeHtml) escapeHtml(title) else title
        val finalArtist = if (escapeHtml) escapeHtml(artist) else artist

        return template
            .replace("{title}", finalTitle)
            .replace("{artist}", finalArtist)
            .replace("{date}", dateStr)
            .replace("{time}", timeStr)
            .replace("{datetime}", dateTimeStr)
    }

    fun formatSimulation(template: String): String {
        val sampleTitle = "LOVE DIVE"
        val sampleArtist = "IVE (아이브)"
        val sampleTimestamp = System.currentTimeMillis()

        return formatMessage(
            template = template,
            title = sampleTitle,
            artist = sampleArtist,
            timestamp = sampleTimestamp,
            escapeHtml = true
        )
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
