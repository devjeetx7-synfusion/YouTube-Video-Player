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

    fun resolve(url: String): VideoSource {
        if (url.isBlank()) return VideoSource.Invalid

        val uri = try {
            Uri.parse(url.trim())
        } catch (e: Exception) {
            return VideoSource.Invalid
        }

        // Must be http or https
        val scheme = uri.scheme
        if (scheme != "http" && scheme != "https") {
            return VideoSource.Invalid
        }

        val host = uri.host?.lowercase() ?: return VideoSource.Invalid

        // Check for YouTube
        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            val videoId = extractYouTubeId(uri, host, uri.path)
            if (videoId != null) {
                return VideoSource.YouTube(videoId)
            }
        }

        // Native
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
        if (host.contains("youtu.be")) {
            val segments = uri.pathSegments
            if (segments.isNotEmpty()) {
                return segments[0]
            }
        } else if (host.contains("youtube.com")) {
            if (path == "/watch") {
                return uri.getQueryParameter("v")
            } else if (path != null && path.startsWith("/shorts/")) {
                val segments = uri.pathSegments
                if (segments.size > 1) return segments[1]
            } else if (path != null && path.startsWith("/live/")) {
                val segments = uri.pathSegments
                if (segments.size > 1) return segments[1]
            } else if (path != null && path.startsWith("/embed/")) {
                val segments = uri.pathSegments
                if (segments.size > 1) return segments[1]
            }
        }
        return null
    }
}
