package com.soundlog.app.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object iTunesCoverArtFetcher {

    private const val TAG = "iTunesCoverArtFetcher"

    suspend fun fetchCoverArtUrl(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "$artist $title".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val requestUrl = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=1"

            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "SoundLog-Android/1.0")
            }

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val firstItem = results.getJSONObject(0)
                    val artUrl100 = firstItem.optString("artworkUrl100")
                    if (!artUrl100.isNullOrBlank()) {
                        // 100x100bb.jpg -> 1000x1000bb.jpg 로 고화질 원본 치환
                        val highResUrl = artUrl100.replace("100x100bb", "1000x1000bb")
                        Log.i(TAG, "Successfully fetched iTunes cover art: $highResUrl for [$artist - $title]")
                        return@withContext highResUrl
                    }
                }
            } else {
                Log.w(TAG, "iTunes Search API HTTP error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch iTunes cover art for $artist - $title", e)
        }
        return@withContext null
    }
}
