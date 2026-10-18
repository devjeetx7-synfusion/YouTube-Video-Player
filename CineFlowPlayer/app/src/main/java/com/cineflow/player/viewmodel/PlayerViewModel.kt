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

    fun saveVideo(video: com.cineflow.player.models.VideoEntity) {
        viewModelScope.launch { repository.insertVideo(video) }
    }

    fun saveToHistory(history: HistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(history)
        }
    }
}
