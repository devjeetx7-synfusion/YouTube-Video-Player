package com.cineflow.player.repository

import com.cineflow.player.database.PlaylistDao
import com.cineflow.player.database.VideoDao
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.models.PlaylistEntity
import com.cineflow.player.models.VideoEntity
import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao
) {

    fun getFavorites(): Flow<List<VideoEntity>> = videoDao.getFavorites()

    suspend fun insertVideo(video: VideoEntity) = videoDao.insertVideo(video)

    suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean) = videoDao.updateFavoriteStatus(url, isFavorite)

    fun getHistory(): Flow<List<HistoryEntity>> = videoDao.getHistory()

    suspend fun insertHistory(history: HistoryEntity) = videoDao.insertHistory(history)

    suspend fun deleteHistory(url: String) = videoDao.deleteHistory(url)

    suspend fun clearHistory() = videoDao.clearHistory()

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun insertPlaylist(playlist: PlaylistEntity) = playlistDao.insertPlaylist(playlist)

    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylist(id)
}
