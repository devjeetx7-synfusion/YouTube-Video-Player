package com.cineflow.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cineflow.player.repository.VideoRepository

class AddVideoViewModelFactory(private val repository: VideoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddVideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddVideoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
