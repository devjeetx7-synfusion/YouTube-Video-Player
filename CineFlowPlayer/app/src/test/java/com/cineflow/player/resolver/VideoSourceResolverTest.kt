package com.cineflow.player.resolver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VideoSourceResolverTest {

    @Test
    fun testYouTubeStandardUrl() {
        val resolved = VideoSourceResolver.resolve("https://www.youtube.com/watch?v=pByvbSa1xUU")
        assertTrue(resolved is VideoSource.YouTube)
        assertEquals("pByvbSa1xUU", (resolved as VideoSource.YouTube).videoId)
    }

    @Test
    fun testYouTubeShortUrl() {
        val resolved = VideoSourceResolver.resolve("https://youtu.be/pByvbSa1xUU")
        assertTrue(resolved is VideoSource.YouTube)
        assertEquals("pByvbSa1xUU", (resolved as VideoSource.YouTube).videoId)
    }

    @Test
    fun testYouTubeShortsUrl() {
        val resolved = VideoSourceResolver.resolve("https://www.youtube.com/shorts/pByvbSa1xUU")
        assertTrue(resolved is VideoSource.YouTube)
        assertEquals("pByvbSa1xUU", (resolved as VideoSource.YouTube).videoId)
    }

    @Test
    fun testYouTubeLiveUrl() {
        val resolved = VideoSourceResolver.resolve("https://www.youtube.com/live/pByvbSa1xUU")
        assertTrue(resolved is VideoSource.YouTube)
        assertEquals("pByvbSa1xUU", (resolved as VideoSource.YouTube).videoId)
    }

    @Test
    fun testYouTubeEmbedUrl() {
        val resolved = VideoSourceResolver.resolve("https://www.youtube.com/embed/pByvbSa1xUU")
        assertTrue(resolved is VideoSource.YouTube)
        assertEquals("pByvbSa1xUU", (resolved as VideoSource.YouTube).videoId)
    }

    @Test
    fun testNativeMp4Url() {
        val resolved = VideoSourceResolver.resolve("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        assertTrue(resolved is VideoSource.Native)
        assertEquals(NativeType.MP4, (resolved as VideoSource.Native).type)
    }

    @Test
    fun testNativeHlsUrl() {
        val resolved = VideoSourceResolver.resolve("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
        assertTrue(resolved is VideoSource.Native)
        assertEquals(NativeType.HLS, (resolved as VideoSource.Native).type)
    }

    @Test
    fun testNativeDashUrl() {
        val resolved = VideoSourceResolver.resolve("https://dash.akamaized.net/envivio/EnvivioDash3/manifest.mpd")
        assertTrue(resolved is VideoSource.Native)
        assertEquals(NativeType.DASH, (resolved as VideoSource.Native).type)
    }

    @Test
    fun testInvalidUrl() {
        val resolved = VideoSourceResolver.resolve("not_a_url")
        assertTrue(resolved is VideoSource.Invalid)
    }

    @Test
    fun testYouTubeInvalidVideoId() {
        val resolved = VideoSourceResolver.resolve("https://www.youtube.com/watch?v=invalid_id")
        assertTrue(resolved is VideoSource.Invalid)
    }
}
