package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.models.VideoEntity
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.repository.YouTubeMetadataFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VideoMetadataState(
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null
)

class PlayerViewModel(private val repository: VideoRepository) : ViewModel() {

    val favorites = repository.getFavorites()

    private val _metadataState = MutableStateFlow<VideoMetadataState?>(null)
    val metadataState: StateFlow<VideoMetadataState?> = _metadataState.asStateFlow()

    private val _savedPosition = MutableStateFlow<Long>(0L)
    val savedPosition: StateFlow<Long> = _savedPosition.asStateFlow()

    fun loadInitialData(url: String, videoId: String?) {
        viewModelScope.launch {
            // Check existing history or video record for cached title/position
            val existingHistory = repository.findHistory(url)
            val existingVideo = repository.findVideo(url)

            if (existingHistory != null) {
                _savedPosition.value = existingHistory.lastPlaybackPosition
                if (existingHistory.title.isNotBlank() && !existingHistory.title.startsWith("YouTube Video (")) {
                    _metadataState.value = VideoMetadataState(existingHistory.title, thumbnailUrl = existingHistory.thumbnailUrl)
                }
            }

            if (_metadataState.value == null && existingVideo != null) {
                if (existingVideo.title.isNotBlank() && !existingVideo.title.startsWith("YouTube Video (")) {
                    _metadataState.value = VideoMetadataState(existingVideo.title, thumbnailUrl = existingVideo.thumbnailUrl)
                }
            }

            // Fetch fresh metadata from YouTube if videoId is provided
            if (videoId != null) {
                val fetched = YouTubeMetadataFetcher.fetchMetadata(videoId)
                if (fetched != null) {
                    val finalTitle = if (fetched.authorName != null) "${fetched.title} • ${fetched.authorName}" else fetched.title
                    _metadataState.value = VideoMetadataState(
                        title = finalTitle,
                        author = fetched.authorName,
                        thumbnailUrl = fetched.thumbnailUrl
                    )

                    // Update existing records with real title and thumbnail
                    if (existingVideo != null) {
                        repository.insertVideo(
                            existingVideo.copy(title = finalTitle, thumbnailUrl = fetched.thumbnailUrl)
                        )
                    }
                    if (existingHistory != null) {
                        repository.insertHistory(
                            existingHistory.copy(title = finalTitle, thumbnailUrl = fetched.thumbnailUrl)
                        )
                    }
                }
            }
        }
    }

    fun toggleFavorite(video: VideoEntity, favorite: Boolean) {
        viewModelScope.launch {
            val existing = repository.findVideo(video.url)
            if (existing == null) {
                repository.insertVideo(video.copy(isFavorite = favorite))
            } else {
                repository.updateFavoriteStatus(video.url, favorite)
            }
        }
    }

    fun saveToHistory(history: HistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(history)
        }
    }
}
