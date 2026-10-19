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

    override fun initialize(container: ViewGroup, lifecycleOwner: LifecycleOwner) {
        if (exoPlayer != null) {
            release()
        }

        playerView = PlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = true
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val state = when (playbackState) {
                            Player.STATE_IDLE -> PlaybackState.IDLE
                            Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                            Player.STATE_READY -> if (playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
                            Player.STATE_ENDED -> PlaybackState.ENDED
                            else -> PlaybackState.IDLE
                        }
                        stateListener?.invoke(state)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val state = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                        stateListener?.invoke(state)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("CineFlowNativeEngine", "ExoPlayer error: ${error.message}", error)
                        stateListener?.invoke(PlaybackState.ERROR)
                    }
                })
            }

        playerView?.player = exoPlayer
        container.addView(playerView)
    }

    override fun loadVideo(url: String) {
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
    }

    override fun play() {
        exoPlayer?.playWhenReady = true
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (speed > 0f) {
            exoPlayer?.setPlaybackSpeed(speed)
        }
    }

    override fun release() {
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
