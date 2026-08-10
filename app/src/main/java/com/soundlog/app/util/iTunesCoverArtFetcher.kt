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
        val queries = mutableListOf<String>()
        val rawCombined = "$artist $title".trim()
        if (rawCombined.isNotBlank()) queries.add(rawCombined)

        val cleanArtist = artist.replace(Regex("""[^\w\s\uAC00-\uD7A3]"""), " ").trim()
        val cleanTitle = title.replace(Regex("""[^\w\s\uAC00-\uD7A3]"""), " ").trim()
        val cleanCombined = "$cleanArtist $cleanTitle".trim()
        if (cleanCombined.isNotBlank() && cleanCombined != rawCombined) {
            queries.add(cleanCombined)
        }
        if (title.isNotBlank()) queries.add(title.trim())
        if (cleanTitle.isNotBlank() && cleanTitle != title.trim()) queries.add(cleanTitle)

        for (query in queries.distinct()) {
            val url = searchItunes(query)
            if (!url.isNullOrBlank()) {
                Log.i(TAG, "Successfully fetched iTunes cover art: $url for query [$query]")
                return@withContext url
            }
        }
        Log.w(TAG, "Failed to fetch iTunes cover art for [$artist - $title]")
        return@withContext null
    }

    private fun searchItunes(query: String): String? {
        try {
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
                        return artUrl100.replace("100x100bb", "1000x1000bb")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying iTunes API for query [$query]", e)
        }
        return null
    }
}
