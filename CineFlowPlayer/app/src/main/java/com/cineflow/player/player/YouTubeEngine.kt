package com.cineflow.player.player

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.cineflow.player.resolver.VideoSourceResolver
import com.cineflow.player.resolver.VideoSource

class YouTubeEngine(private val context: Context) : PlayerEngine {

    private var youTubePlayerView: YouTubePlayerView? = null
    private var youTubePlayer: YouTubePlayer? = null
    private var stateListener: ((PlaybackState) -> Unit)? = null

    private var currentVideoId: String? = null
    private var isPlayerReady = false

    private var currentPosition = 0L
    private var duration = 0L
    private var isPlaying = false
    private var requestedPlay = false

    override fun initialize(container: ViewGroup, lifecycleOwner: LifecycleOwner) {
        youTubePlayerView = YouTubePlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        lifecycleOwner.lifecycle.addObserver(youTubePlayerView!!)

        youTubePlayerView?.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@YouTubeEngine.youTubePlayer = youTubePlayer
                isPlayerReady = true
                currentVideoId?.let {
                    if (requestedPlay) youTubePlayer.loadVideo(it, 0f) else youTubePlayer.cueVideo(it, 0f)
                }
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                val playbackState = when (state) {
                    PlayerConstants.PlayerState.UNSTARTED -> PlaybackState.IDLE
                    PlayerConstants.PlayerState.ENDED -> PlaybackState.ENDED
                    PlayerConstants.PlayerState.PLAYING -> {
                        isPlaying = true
                        PlaybackState.PLAYING
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        isPlaying = false
                        PlaybackState.PAUSED
                    }
                    PlayerConstants.PlayerState.BUFFERING -> PlaybackState.BUFFERING
                    PlayerConstants.PlayerState.VIDEO_CUED -> PlaybackState.IDLE
                    else -> PlaybackState.IDLE
                }
                stateListener?.invoke(playbackState)
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                Log.e("CineFlowYouTube", "IFrame player error: $error; videoId=$currentVideoId")
                stateListener?.invoke(PlaybackState.ERROR)
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                currentPosition = (second * 1000).toLong()
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                this@YouTubeEngine.duration = (duration * 1000).toLong()
            }
        })

        container.addView(youTubePlayerView)
    }

    override fun loadVideo(url: String) {
        val resolved = VideoSourceResolver.resolve(url)
        if (resolved is VideoSource.YouTube) {
            currentVideoId = resolved.videoId
            if (isPlayerReady) {
                youTubePlayer?.loadVideo(resolved.videoId, 0f)
            }
        } else {
            stateListener?.invoke(PlaybackState.ERROR)
        }
    }

    override fun play() {
        requestedPlay = true
        youTubePlayer?.play()
    }

    override fun pause() {
        requestedPlay = false
        youTubePlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        youTubePlayer?.seekTo(positionMs / 1000f)
    }

    override fun setPlaybackSpeed(speed: Float) {
    }

    override fun release() {
        youTubePlayerView?.release()
        youTubePlayerView = null
        youTubePlayer = null
    }

    override fun getCurrentPosition(): Long {
        return currentPosition
    }

    override fun getDuration(): Long {
        return duration
    }

    override fun isPlaying(): Boolean {
        return isPlaying
    }

    override fun setOnPlaybackStateChangeListener(listener: (PlaybackState) -> Unit) {
        this.stateListener = listener
    }
}
