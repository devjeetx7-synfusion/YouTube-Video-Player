package com.cineflow.player.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject
import org.json.JSONTokener
import com.cineflow.player.resolver.VideoSource
import com.cineflow.player.resolver.VideoSourceResolver

/** YouTube IFrame Player hosted by local HTML/CSS/JS assets. */
class WebViewYouTubeEngine(private val context: Context) : PlayerEngine {

    private var webView: WebView? = null
    private var stateListener: ((PlaybackState) -> Unit)? = null
    private var isReleased = false
    private var pageLoaded = false
    private var playerReady = false
    private var currentVideoId: String? = null
    private var loadedVideoId: String? = null
    private var requestedPlay = false
    private var currentPositionMs = 0L
    private var durationMs = 0L
    private var pendingSeekPositionMs: Long? = null
    private var lastReportedState: PlaybackState? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pollTask: Runnable? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun initialize(container: ViewGroup, lifecycleOwner: LifecycleOwner) {
        release()
        isReleased = false
        pageLoaded = false
        playerReady = false
        currentVideoId = null
        loadedVideoId = null
        requestedPlay = false
        currentPositionMs = 0L
        durationMs = 0L
        lastReportedState = null

        val view = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    if (isReleased) return
                    pageLoaded = true
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return request.url.scheme != "https" || request.url.host != "www.youtube.com"
                }
            }
        }
        webView = view
        container.addView(view)
        view.loadUrl("file:///android_asset/cineflow_youtube/index.html")
        startPolling()
    }

    override fun loadVideo(url: String) {
        if (isReleased) return
        val source = VideoSourceResolver.resolve(url)
        if (source !is VideoSource.YouTube) {
            reportState(PlaybackState.ERROR)
            return
        }
        if (source.videoId == currentVideoId && loadedVideoId == currentVideoId) {
            pendingSeekPositionMs?.let { seekTo(it) }
            return
        }
        currentVideoId = source.videoId
        loadedVideoId = null
        currentPositionMs = 0L
        durationMs = 0L
        pendingSeekPositionMs = null
        playerReady = false
        lastReportedState = null
        reportState(PlaybackState.BUFFERING)
    }

    override fun play() {
        if (isReleased) return
        requestedPlay = true
        if (playerReady && loadedVideoId == currentVideoId) {
            evaluate("window.CineFlowPlayer && window.CineFlowPlayer.play();")
        }
    }

    override fun pause() {
        if (isReleased) return
        requestedPlay = false
        if (playerReady && loadedVideoId == currentVideoId) {
            evaluate("window.CineFlowPlayer && window.CineFlowPlayer.pause();")
        }
    }

    override fun seekTo(positionMs: Long) {
        if (isReleased) return
        val safePosition = positionMs.coerceAtLeast(0L)
        if (playerReady && loadedVideoId == currentVideoId) {
            evaluate("window.CineFlowPlayer && window.CineFlowPlayer.seekTo(${safePosition / 1000.0});")
        } else {
            pendingSeekPositionMs = safePosition
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        // The IFrame Player API does not guarantee arbitrary speed support for every video.
    }

    override fun release() {
        isReleased = true
        stateListener = null
        pollTask?.let { handler.removeCallbacks(it) }
        pollTask = null
        webView?.let { view ->
            view.stopLoading()
            view.webChromeClient = null
            view.webViewClient = WebViewClient()
            view.loadUrl("about:blank")
            (view.parent as? ViewGroup)?.removeView(view)
            view.removeAllViews()
            view.destroy()
        }
        webView = null
        pageLoaded = false
        playerReady = false
        loadedVideoId = null
        currentVideoId = null
        requestedPlay = false
        currentPositionMs = 0L
        durationMs = 0L
        pendingSeekPositionMs = null
        lastReportedState = null
    }

    override fun getCurrentPosition(): Long = currentPositionMs
    override fun getDuration(): Long = durationMs
    override fun isPlaying(): Boolean = lastReportedState == PlaybackState.PLAYING

    override fun setOnPlaybackStateChangeListener(listener: (PlaybackState) -> Unit) {
        stateListener = listener
    }

    private fun startPolling() {
        pollTask?.let { handler.removeCallbacks(it) }
        val task = object : Runnable {
            override fun run() {
                if (isReleased) return
                val view = webView
                if (view != null && pageLoaded) {
                    view.evaluateJavascript(
                        "(function(){return !!(window.CineFlowPlayer && window.CineFlowPlayer.isReady && window.CineFlowPlayer.isReady());})()"
                    ) { readyResult ->
                        if (isReleased) return@evaluateJavascript
                        if (readyResult == "true") {
                            playerReady = true
                            loadPendingVideoIfNeeded()
                            if (loadedVideoId == currentVideoId && currentVideoId != null) {
                                readSnapshot()
                            }
                        }
                    }
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        pollTask = task
        handler.post(task)
    }

    private fun loadPendingVideoIfNeeded() {
        val id = currentVideoId ?: return
        if (!playerReady || loadedVideoId == id) return
        if (!id.matches(Regex("^[A-Za-z0-9_-]{11}$"))) {
            reportState(PlaybackState.ERROR)
            return
        }
        val seekSeconds = (pendingSeekPositionMs ?: 0L) / 1000.0
        val autoplay = requestedPlay
        loadedVideoId = id
        pendingSeekPositionMs = null
        evaluate("window.CineFlowPlayer.loadVideo('$id',$autoplay,$seekSeconds);")
        reportState(PlaybackState.BUFFERING)
    }

    private fun readSnapshot() {
        evaluate("window.CineFlowPlayer.getSnapshot();") { result ->
            if (isReleased || result.isNullOrBlank() || result == "null") return@evaluate
            try {
                val jsonText = JSONTokener(result).nextValue() as? String ?: return@evaluate
                val json = JSONObject(jsonText)
                if (!json.optBoolean("ready", false)) return@evaluate
                currentPositionMs = (json.optDouble("position", 0.0) * 1000.0).toLong().coerceAtLeast(0L)
                durationMs = (json.optDouble("duration", 0.0) * 1000.0).toLong().coerceAtLeast(0L)
                val error = json.optInt("error", 0)
                val state = when {
                    error != 0 -> PlaybackState.ERROR
                    json.optInt("state", -1) == 1 -> PlaybackState.PLAYING
                    json.optInt("state", -1) == 2 -> PlaybackState.PAUSED
                    json.optInt("state", -1) == 0 -> PlaybackState.ENDED
                    json.optInt("state", -1) == 3 -> PlaybackState.BUFFERING
                    else -> PlaybackState.IDLE
                }
                reportState(state)
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse YouTube player snapshot", e)
            }
        }
    }

    private fun evaluate(script: String, callback: ((String?) -> Unit)? = null) {
        if (isReleased) return
        webView?.evaluateJavascript(script) { value -> callback?.invoke(value) }
    }

    private fun reportState(state: PlaybackState) {
        if (lastReportedState == state) return
        lastReportedState = state
        stateListener?.invoke(state)
    }

    companion object {
        private const val TAG = "CineFlowWebYouTube"
        private const val POLL_INTERVAL_MS = 350L
    }
}
