package com.cineflow.player.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val url: String,
    val title: String,
    val source: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val timestamp: Long,
    val isFavorite: Boolean
)
