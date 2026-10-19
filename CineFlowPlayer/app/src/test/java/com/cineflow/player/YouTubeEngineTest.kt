package com.cineflow.player

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class YouTubeEngineTest {

    @Test
    fun testIFramePlayerOptionsBuilding() {
        val options = IFramePlayerOptions.Builder()
            .controls(1)
            .rel(0)
            .origin("https://www.youtube.com")
            .build()
        assertNotNull(options)
    }
}
