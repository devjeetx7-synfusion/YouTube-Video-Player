package com.cineflow.player.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class YouTubeMetadata(
    val title: String,
    val authorName: String?,
    val thumbnailUrl: String
)

object YouTubeMetadataFetcher {

    suspend fun fetchMetadata(videoId: String): YouTubeMetadata? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doInput = true

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                val title = json.optString("title", "")
                val authorName = json.optString("author_name", "")
                val thumbnailUrl = json.optString("thumbnail_url", "https://i.ytimg.com/vi/$videoId/hqdefault.jpg")

                if (title.isNotBlank()) {
                    return@withContext YouTubeMetadata(
                        title = title,
                        authorName = if (authorName.isNotBlank()) authorName else null,
                        thumbnailUrl = thumbnailUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("YouTubeMetadataFetcher", "Could not fetch metadata for $videoId: ${e.message}")
        }
        return@withContext null
    }
}
