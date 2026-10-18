package com.cineflow.player.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cineflow.player.models.VideoEntity
import com.cineflow.player.models.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteHistory(url: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
