package com.cineflow.player.player

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class NativeEngine(private val context: Context) : PlayerEngine {

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var stateListener: ((PlaybackState) -> Unit)? = null
    private var playerListener: Player.Listener? = null
    private var isReleased = false

    override fun initialize(container: ViewGroup, lifecycleOwner: LifecycleOwner) {
        if (exoPlayer != null || playerView != null) {
            release()
        }

        isReleased = false

        playerView = PlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = true
        }

        container.addView(playerView)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (isReleased) return
                val state = when (playbackState) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                    Player.STATE_READY -> if (exoPlayer?.playWhenReady == true) PlaybackState.PLAYING else PlaybackState.PAUSED
                    Player.STATE_ENDED -> PlaybackState.ENDED
                    else -> PlaybackState.IDLE
                }
                stateListener?.invoke(state)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isReleased) return
                val state = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                stateListener?.invoke(state)
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isReleased) return
                Log.e("CineFlowNativeEngine", "ExoPlayer error: ${error.message}", error)
                stateListener?.invoke(PlaybackState.ERROR)
            }
        }

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playerListener?.let { addListener(it) }
            }

        playerView?.player = exoPlayer
    }

    override fun loadVideo(url: String) {
        if (isReleased) return
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
    }

    override fun play() {
        if (isReleased) return
        exoPlayer?.playWhenReady = true
        exoPlayer?.play()
    }

    override fun pause() {
        if (isReleased) return
        exoPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (isReleased) return
        exoPlayer?.seekTo(positionMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (isReleased) return
        if (speed > 0f) {
            exoPlayer?.setPlaybackSpeed(speed)
        }
    }

    override fun release() {
        isReleased = true
        stateListener = null

        playerListener?.let {
            exoPlayer?.removeListener(it)
        }
        playerListener = null

        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null

        playerView?.player = null
        playerView = null
    }

    override fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    override fun getDuration(): Long {
        val duration = exoPlayer?.duration ?: 0L
        return if (duration < 0L) 0L else duration
    }

    override fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    override fun setOnPlaybackStateChangeListener(listener: (PlaybackState) -> Unit) {
        this.stateListener = listener
    }
}
