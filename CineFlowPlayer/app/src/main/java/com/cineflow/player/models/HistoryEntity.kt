package com.cineflow.player.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val url: String,
    val title: String,
    val source: String,
    val thumbnailUrl: String?,
    val lastPlaybackPosition: Long,
    val timestamp: Long
)
