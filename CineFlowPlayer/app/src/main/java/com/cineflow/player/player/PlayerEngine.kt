package com.cineflow.player.player

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner

interface PlayerEngine {
    fun initialize(container: ViewGroup, lifecycleOwner: LifecycleOwner)
    fun loadVideo(url: String)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun release()

    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean

    fun setOnPlaybackStateChangeListener(listener: (PlaybackState) -> Unit)
}

enum class PlaybackState {
    IDLE, BUFFERING, PLAYING, PAUSED, ENDED, ERROR
}
