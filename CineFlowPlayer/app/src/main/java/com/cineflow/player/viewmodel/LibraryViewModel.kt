package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.repository.VideoRepository
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: VideoRepository) : ViewModel() {
    val savedVideos = repository.getAllVideos()
    val history = repository.getHistory()
    val playlists = repository.getAllPlaylists()

    fun removeFromHistory(url: String) {
        viewModelScope.launch {
            repository.deleteHistory(url)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
