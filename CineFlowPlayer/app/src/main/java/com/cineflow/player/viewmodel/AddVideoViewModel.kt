package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineflow.player.models.VideoEntity
import com.cineflow.player.repository.VideoRepository
import kotlinx.coroutines.launch

class AddVideoViewModel(private val repository: VideoRepository) : ViewModel() {

    fun saveVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.insertVideo(video)
        }
    }
}
