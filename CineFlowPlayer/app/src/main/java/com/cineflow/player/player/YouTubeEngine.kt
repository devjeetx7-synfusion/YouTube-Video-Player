package com.cineflow.player.player

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.cineflow.player.resolver.VideoSource
import com.cineflow.player.resolver.VideoSourceResolver

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
    private var pendingSeekPositionMs: Long? = null

    override fun initialize(container: ViewGroup, lifecycleOwner: LifecycleOwner) {
        if (youTubePlayerView != null) {
            release()
        }

        youTubePlayerView = YouTubePlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        lifecycleOwner.lifecycle.addObserver(youTubePlayerView!!)

        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                Log.d("CineFlowYouTube", "YouTubePlayer onReady")
                this@YouTubeEngine.youTubePlayer = youTubePlayer
                isPlayerReady = true

                currentVideoId?.let { videoId ->
                    val startSeconds = pendingSeekPositionMs?.let { it / 1000f } ?: 0f
                    pendingSeekPositionMs = null
                    if (requestedPlay) {
                        youTubePlayer.loadVideo(videoId, startSeconds)
                    } else {
                        youTubePlayer.cueVideo(videoId, startSeconds)
                    }
                }
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                Log.d("CineFlowYouTube", "Player state changed: $state")
                val playbackState = when (state) {
                    PlayerConstants.PlayerState.UNSTARTED -> PlaybackState.IDLE
                    PlayerConstants.PlayerState.ENDED -> {
                        isPlaying = false
                        PlaybackState.ENDED
                    }
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
                isPlaying = false
                stateListener?.invoke(PlaybackState.ERROR)
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                currentPosition = (second * 1000).toLong()
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                this@YouTubeEngine.duration = (duration * 1000).toLong()
            }
        }

        val iFramePlayerOptions = IFramePlayerOptions.Builder()
            .controls(1)
            .fullscreen(1)
            .rel(0)
            .origin("https://www.youtube.com")
            .build()

        youTubePlayerView?.initialize(listener, false, iFramePlayerOptions)
        container.addView(youTubePlayerView)
    }

    override fun loadVideo(url: String) {
        val resolved = VideoSourceResolver.resolve(url)
        if (resolved is VideoSource.YouTube) {
            val newVideoId = resolved.videoId
            if (newVideoId == currentVideoId && isPlayerReady) {
                pendingSeekPositionMs?.let {
                    youTubePlayer?.seekTo(it / 1000f)
                    pendingSeekPositionMs = null
                }
                return
            }
            currentVideoId = newVideoId
            if (isPlayerReady) {
                val startSeconds = pendingSeekPositionMs?.let { it / 1000f } ?: 0f
                pendingSeekPositionMs = null
                if (requestedPlay) {
                    youTubePlayer?.loadVideo(newVideoId, startSeconds)
                } else {
                    youTubePlayer?.cueVideo(newVideoId, startSeconds)
                }
            }
        } else {
            stateListener?.invoke(PlaybackState.ERROR)
        }
    }

    override fun play() {
        requestedPlay = true
        if (isPlayerReady) {
            youTubePlayer?.play()
        }
    }

    override fun pause() {
        requestedPlay = false
        if (isPlayerReady) {
            youTubePlayer?.pause()
        }
    }

    override fun seekTo(positionMs: Long) {
        if (isPlayerReady) {
            youTubePlayer?.seekTo(positionMs / 1000f)
        } else {
            pendingSeekPositionMs = positionMs
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        // IFrame Player API playback rate if supported
    }

    override fun release() {
        youTubePlayerView?.release()
        youTubePlayerView = null
        youTubePlayer = null
        isPlayerReady = false
        isPlaying = false
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
