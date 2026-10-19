package com.cineflow.player.resolver

import android.net.Uri

sealed class VideoSource {
    data class YouTube(val videoId: String) : VideoSource()
    data class Native(val uri: Uri, val type: NativeType) : VideoSource()
    object Invalid : VideoSource()
}

enum class NativeType {
    MP4, HLS, DASH, UNKNOWN
}

object VideoSourceResolver {

    private val YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
        "www.youtu.be"
    )

    private val YOUTUBE_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")

    fun resolve(url: String): VideoSource {
        if (url.isBlank()) return VideoSource.Invalid

        val uri = try {
            Uri.parse(url.trim())
        } catch (e: Exception) {
            return VideoSource.Invalid
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return VideoSource.Invalid
        }

        val host = uri.host?.lowercase() ?: return VideoSource.Invalid

        // Check for YouTube domain
        if (YOUTUBE_HOSTS.any { host == it || host.endsWith(".$it") }) {
            val videoId = extractYouTubeId(uri, host, uri.path)
            return if (videoId != null && YOUTUBE_ID_REGEX.matches(videoId)) {
                VideoSource.YouTube(videoId)
            } else {
                VideoSource.Invalid
            }
        }

        // Direct Native media
        val path = uri.path?.lowercase() ?: ""
        val type = when {
            path.endsWith(".m3u8") -> NativeType.HLS
            path.endsWith(".mpd") -> NativeType.DASH
            path.endsWith(".mp4") -> NativeType.MP4
            else -> NativeType.UNKNOWN
        }

        return VideoSource.Native(uri, type)
    }

    private fun extractYouTubeId(uri: Uri, host: String, path: String?): String? {
        if (host == "youtu.be" || host == "www.youtu.be") {
            val segments = uri.pathSegments
            if (segments.isNotEmpty()) {
                val candidate = segments[0]
                return if (candidate.contains("?")) candidate.substringBefore("?") else candidate
            }
        } else if (host.contains("youtube.com")) {
            val vParam = uri.getQueryParameter("v")
            if (!vParam.isNullOrBlank()) {
                return vParam
            }

            if (path != null) {
                val segments = uri.pathSegments
                if (segments.size >= 2) {
                    when (segments[0].lowercase()) {
                        "shorts", "live", "embed", "v" -> return segments[1]
                    }
                }
            }
        }
        return null
    }
}
