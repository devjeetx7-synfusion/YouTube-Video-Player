package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.repository.VideoRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: VideoRepository) : ViewModel() {
    val favorites = repository.getFavorites()
    val history = repository.getHistory()

    fun toggleFavorite(url: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(url, !currentStatus)
        }
    }
}
