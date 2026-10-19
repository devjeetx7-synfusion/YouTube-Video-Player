package com.cineflow.player.database

import android.content.Context
import androidx.room.Room
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.models.PlaylistEntity
import com.cineflow.player.models.VideoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var videoDao: VideoDao
    private lateinit var playlistDao: PlaylistDao

    @Before
    fun createDb() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        videoDao = db.videoDao()
        playlistDao = db.playlistDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndGetVideo() = runBlocking {
        val video = VideoEntity(
            url = "https://www.youtube.com/watch?v=pByvbSa1xUU",
            title = "Test Video",
            source = "YouTube",
            thumbnailUrl = "https://i.ytimg.com/vi/pByvbSa1xUU/hqdefault.jpg",
            duration = 1000L,
            timestamp = System.currentTimeMillis(),
            isFavorite = true
        )
        videoDao.insertVideo(video)

        val fetched = videoDao.findVideo(video.url)
        assertNotNull(fetched)
        assertEquals("Test Video", fetched?.title)
        assertEquals(true, fetched?.isFavorite)

        val favorites = videoDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals(video.url, favorites[0].url)
    }

    @Test
    fun testHistoryOperations() = runBlocking {
        val history = HistoryEntity(
            url = "https://www.youtube.com/watch?v=pByvbSa1xUU",
            title = "Test History",
            source = "YouTube",
            thumbnailUrl = null,
            lastPlaybackPosition = 5000L,
            timestamp = System.currentTimeMillis()
        )
        videoDao.insertHistory(history)

        val found = videoDao.findHistory(history.url)
        assertNotNull(found)
        assertEquals(5000L, found?.lastPlaybackPosition)

        val historyList = videoDao.getHistory().first()
        assertEquals(1, historyList.size)

        videoDao.deleteHistory(history.url)
        val emptyHistory = videoDao.getHistory().first()
        assertEquals(0, emptyHistory.size)
    }

    @Test
    fun testPlaylistOperations() = runBlocking {
        val playlist = PlaylistEntity(
            name = "My Favorites Playlist",
            thumbnailUrl = null,
            timestamp = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(playlist)

        val playlists = playlistDao.getAllPlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals("My Favorites Playlist", playlists[0].name)

        playlistDao.deletePlaylist(playlists[0].id)
        val emptyPlaylists = playlistDao.getAllPlaylists().first()
        assertEquals(0, emptyPlaylists.size)
    }
}
