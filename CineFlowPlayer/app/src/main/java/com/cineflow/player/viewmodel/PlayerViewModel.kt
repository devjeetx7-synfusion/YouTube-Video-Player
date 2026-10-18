package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.repository.VideoRepository
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: VideoRepository) : ViewModel() {

    val favorites = repository.getFavorites()

    fun setFavorite(url: String, favorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(url, favorite)
        }
    }

    fun toggleFavorite(video: com.cineflow.player.models.VideoEntity, favorite: Boolean) {
        viewModelScope.launch {
            val existing = repository.findVideo(video.url)
            if (existing == null) repository.insertVideo(video.copy(isFavorite = favorite))
            else repository.updateFavoriteStatus(video.url, favorite)
        }
    }

    fun saveToHistory(history: HistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(history)
        }
    }
}
