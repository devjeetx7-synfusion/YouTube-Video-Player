package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.repository.VideoRepository
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: VideoRepository) : ViewModel() {

    fun saveToHistory(history: HistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(history)
        }
    }
}
