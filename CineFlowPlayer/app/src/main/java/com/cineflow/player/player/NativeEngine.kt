package com.cineflow.player.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
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
        playerView = PlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = true
        }

        exoPlayer = ExoPlayer.Builder(context).build().apply {
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
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        playerView?.player = null
        playerView = null
    }

    override fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    override fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    override fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    override fun setOnPlaybackStateChangeListener(listener: (PlaybackState) -> Unit) {
        this.stateListener = listener
    }
}
