package com.cineflow.player.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.models.PlaylistEntity
import com.cineflow.player.models.VideoEntity

@Database(entities = [VideoEntity::class, PlaylistEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cineflow_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
