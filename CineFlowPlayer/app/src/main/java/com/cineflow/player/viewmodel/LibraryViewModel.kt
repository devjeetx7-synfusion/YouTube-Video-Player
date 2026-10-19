package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.models.PlaylistEntity
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

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val playlist = PlaylistEntity(
                name = name.trim(),
                thumbnailUrl = null,
                timestamp = System.currentTimeMillis()
            )
            repository.insertPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
        }
    }
}
