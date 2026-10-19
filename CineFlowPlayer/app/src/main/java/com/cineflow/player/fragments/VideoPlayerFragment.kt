package com.cineflow.player.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cineflow.player.R
import com.cineflow.player.database.AppDatabase
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.models.VideoEntity
import com.cineflow.player.player.NativeEngine
import com.cineflow.player.player.PlaybackState
import com.cineflow.player.player.PlayerEngine
import com.cineflow.player.player.YouTubeEngine
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.resolver.VideoSource
import com.cineflow.player.resolver.VideoSourceResolver
import com.cineflow.player.viewmodel.PlayerViewModel
import com.cineflow.player.viewmodel.PlayerViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class VideoPlayerFragment : Fragment() {

    private val args: VideoPlayerFragmentArgs by navArgs()

    private val viewModel: PlayerViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        PlayerViewModelFactory(VideoRepository(database.videoDao(), database.playlistDao()))
    }

    private var playerEngine: PlayerEngine? = null
    private lateinit var playerContainer: FrameLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutError: LinearLayout
    private lateinit var tvErrorText: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var btnOpenExternal: MaterialButton
    private lateinit var tvTitle: TextView
    private lateinit var tvSourceBadge: TextView
    private lateinit var btnFavorite: ImageButton

    private var isFavorite = false
    private var playbackErrorShown = false
    private var currentThumbnailUrl: String? = null
    private var isEngineInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerContainer = view.findViewById(R.id.player_container)
        pbLoading = view.findViewById(R.id.pb_loading)
        layoutError = view.findViewById(R.id.layout_error)
        tvErrorText = view.findViewById(R.id.tv_error_text)
        btnRetry = view.findViewById(R.id.btn_retry)
        btnOpenExternal = view.findViewById(R.id.btn_open_external)
        tvTitle = view.findViewById(R.id.tv_video_title)
        tvSourceBadge = view.findViewById(R.id.tv_source_badge)
        btnFavorite = view.findViewById(R.id.btn_favorite)

        val url = args.videoUrl
        val resolved = VideoSourceResolver.resolve(url)

        if (resolved is VideoSource.Invalid) {
            Toast.makeText(context, "Invalid Video URL", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val videoId = if (resolved is VideoSource.YouTube) resolved.videoId else null
        currentThumbnailUrl = if (videoId != null) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else null

        tvSourceBadge.text = if (resolved is VideoSource.YouTube) "YouTube" else "Native"
        tvTitle.text = if (videoId != null) "YouTube Video ($videoId)" else "Direct Media"

        viewModel.loadInitialData(url, videoId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.metadataState.collect { meta ->
                        meta?.let {
                            tvTitle.text = it.title
                            it.thumbnailUrl?.let { thumb -> currentThumbnailUrl = thumb }
                        }
                    }
                }
                launch {
                    viewModel.favorites.collect { favorites ->
                        isFavorite = favorites.any { it.url == url }
                        updateFavoriteIcon()
                    }
                }
                launch {
                    viewModel.savedPosition.collect { pos ->
                        if (pos > 0L) {
                            playerEngine?.seekTo(pos)
                        }
                    }
                }
            }
        }

        setupPlayer(url, resolved)

        btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteIcon()
            viewModel.toggleFavorite(
                VideoEntity(
                    url = url,
                    title = tvTitle.text.toString(),
                    source = if (resolved is VideoSource.YouTube) "YouTube" else "Native",
                    thumbnailUrl = currentThumbnailUrl,
                    duration = playerEngine?.getDuration() ?: 0L,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = isFavorite
                ),
                isFavorite
            )
            Toast.makeText(
                context,
                if (isFavorite) "Added to Favorites" else "Removed from Favorites",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnRetry.setOnClickListener {
            layoutError.visibility = View.GONE
            playbackErrorShown = false
            setupPlayer(url, resolved)
        }

        if (resolved is VideoSource.YouTube) {
            btnOpenExternal.visibility = View.VISIBLE
            btnOpenExternal.setOnClickListener {
                openExternalYouTube(resolved.videoId)
            }
        } else {
            btnOpenExternal.visibility = View.GONE
        }

        saveToHistory(url, resolved, 0L)
    }

    private fun setupPlayer(url: String, resolved: VideoSource) {
        playerEngine?.release()
        playerContainer.removeAllViews()

        playerEngine = if (resolved is VideoSource.YouTube) {
            YouTubeEngine(requireContext())
        } else {
            NativeEngine(requireContext())
        }

        playerEngine?.apply {
            initialize(playerContainer, viewLifecycleOwner)
            setOnPlaybackStateChangeListener { state ->
                if (!isAdded) return@setOnPlaybackStateChangeListener
                when (state) {
                    PlaybackState.BUFFERING -> {
                        pbLoading.visibility = View.VISIBLE
                        layoutError.visibility = View.GONE
                    }
                    PlaybackState.PLAYING -> {
                        pbLoading.visibility = View.GONE
                        layoutError.visibility = View.GONE
                        playbackErrorShown = false
                    }
                    PlaybackState.PAUSED, PlaybackState.ENDED, PlaybackState.IDLE -> {
                        pbLoading.visibility = View.GONE
                    }
                    PlaybackState.ERROR -> {
                        pbLoading.visibility = View.GONE
                        layoutError.visibility = View.VISIBLE
                        if (!playbackErrorShown) {
                            playbackErrorShown = true
                            if (resolved is VideoSource.YouTube) {
                                tvErrorText.text = "This YouTube video cannot play inside the embedded player."
                                showExternalDialog(resolved.videoId)
                            } else {
                                tvErrorText.text = "Error playing direct media file."
                            }
                        }
                    }
                }
            }
            loadVideo(url)
            play()
        }
        isEngineInitialized = true
    }

    private fun showExternalDialog(videoId: String) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("YouTube Playback Notice")
            .setMessage("This video prohibits embedded playback or requires opening directly in YouTube.")
            .setPositiveButton("Watch on YouTube") { _, _ ->
                openExternalYouTube(videoId)
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun openExternalYouTube(videoId: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        )
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to open YouTube link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoriteIcon() {
        val iconRes = if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        btnFavorite.setImageResource(iconRes)
    }

    private fun saveToHistory(url: String, source: VideoSource, positionMs: Long) {
        val sourceStr = if (source is VideoSource.YouTube) "YouTube" else "Native"
        val history = HistoryEntity(
            url = url,
            title = tvTitle.text.toString(),
            source = sourceStr,
            thumbnailUrl = currentThumbnailUrl,
            lastPlaybackPosition = positionMs,
            timestamp = System.currentTimeMillis()
        )
        viewModel.saveToHistory(history)
    }

    override fun onStart() {
        super.onStart()
        playerEngine?.play()
    }

    override fun onStop() {
        super.onStop()
        playerEngine?.pause()

        val url = args.videoUrl
        val pos = playerEngine?.getCurrentPosition() ?: 0L
        val resolved = VideoSourceResolver.resolve(url)
        saveToHistory(url, resolved, pos)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerEngine?.release()
        playerEngine = null
        isEngineInitialized = false
    }
}
