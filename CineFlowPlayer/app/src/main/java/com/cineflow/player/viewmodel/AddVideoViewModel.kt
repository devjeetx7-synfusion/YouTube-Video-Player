package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.models.VideoEntity
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.repository.YouTubeMetadataFetcher
import com.cineflow.player.resolver.VideoSource
import com.cineflow.player.resolver.VideoSourceResolver
import kotlinx.coroutines.launch

class AddVideoViewModel(private val repository: VideoRepository) : ViewModel() {

    fun saveVideo(url: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val resolved = VideoSourceResolver.resolve(url)
            val source = if (resolved is VideoSource.YouTube) "YouTube" else "Native"
            var title = if (resolved is VideoSource.YouTube) "YouTube Video (${resolved.videoId})" else "Direct Video"
            var thumbnailUrl = if (resolved is VideoSource.YouTube) "https://i.ytimg.com/vi/${resolved.videoId}/hqdefault.jpg" else null

            if (resolved is VideoSource.YouTube) {
                val metadata = YouTubeMetadataFetcher.fetchMetadata(resolved.videoId)
                if (metadata != null) {
                    title = if (metadata.authorName != null) "${metadata.title} • ${metadata.authorName}" else metadata.title
                    thumbnailUrl = metadata.thumbnailUrl
                }
            }

            val video = VideoEntity(
                url = url,
                title = title,
                source = source,
                thumbnailUrl = thumbnailUrl,
                duration = 0L,
                timestamp = System.currentTimeMillis(),
                isFavorite = false
            )
            repository.insertVideo(video)
            onComplete()
        }
    }
}
