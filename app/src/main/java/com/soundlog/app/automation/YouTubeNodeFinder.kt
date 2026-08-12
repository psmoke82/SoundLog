package com.soundlog.app.automation

import android.util.Log
import java.net.URLEncoder

object YouTubeNodeFinder {
    private const val TAG = "YouTubeNodeFinder"

    fun buildYouTubeSearchUrl(artist: String, title: String): String {
        val encodedQuery = try {
            URLEncoder.encode("$artist $title official", "UTF-8")
        } catch (e: Exception) {
            "$artist+$title+official"
        }
        val url = "https://www.youtube.com/results?search_query=$encodedQuery"
        Log.i(TAG, "Generated YouTube search URL: $url")
        return url
    }
}
