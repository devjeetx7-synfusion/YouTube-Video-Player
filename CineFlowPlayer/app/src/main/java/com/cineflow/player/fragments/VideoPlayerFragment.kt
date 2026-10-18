package com.cineflow.player.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.cineflow.player.models.VideoEntity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.cineflow.player.R
import com.cineflow.player.database.AppDatabase
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.player.NativeEngine
import com.cineflow.player.player.PlaybackState
import com.cineflow.player.player.PlayerEngine
import com.cineflow.player.player.YouTubeEngine
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.resolver.VideoSource
import com.cineflow.player.resolver.VideoSourceResolver
import com.cineflow.player.viewmodel.PlayerViewModel
import com.cineflow.player.viewmodel.PlayerViewModelFactory

class VideoPlayerFragment : Fragment() {

    private val args: VideoPlayerFragmentArgs by navArgs()

    private val viewModel: PlayerViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        PlayerViewModelFactory(VideoRepository(database.videoDao(), database.playlistDao()))
    }

    private var playerEngine: PlayerEngine? = null
    private lateinit var playerContainer: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSourceBadge: TextView
    private lateinit var btnFavorite: ImageButton

    private var isFavorite = false
    private var playbackErrorShown = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerContainer = view.findViewById(R.id.player_container)
        tvTitle = view.findViewById(R.id.tv_video_title)
        tvSourceBadge = view.findViewById(R.id.tv_source_badge)
        btnFavorite = view.findViewById(R.id.btn_favorite)

        val url = args.videoUrl
        val resolved = VideoSourceResolver.resolve(url)

        when (resolved) {
            is VideoSource.YouTube -> {
                tvTitle.text = "YouTube Video (${resolved.videoId})"
                tvSourceBadge.text = "YouTube"
                playerEngine = YouTubeEngine(requireContext())
            }
            is VideoSource.Native -> {
                tvTitle.text = "Direct Media (${resolved.type.name})"
                tvSourceBadge.text = "Native"
                playerEngine = NativeEngine(requireContext())
            }
            is VideoSource.Invalid -> {
                Toast.makeText(context, "Invalid Video URL", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
                return
            }
        }

        playerEngine?.apply {
            initialize(playerContainer, viewLifecycleOwner)
            setOnPlaybackStateChangeListener { state ->
                when (state) {
                    PlaybackState.ERROR -> {
                        if (!isAdded || playbackErrorShown) return@setOnPlaybackStateChangeListener
                        playbackErrorShown = true
                        if (resolved is VideoSource.YouTube) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("YouTube playback unavailable")
                                .setMessage("This video cannot play in the embedded player. You can try opening it in YouTube.")
                                .setPositiveButton("Open YouTube") { _, _ ->
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://www.youtube.com/watch?v=${resolved.videoId}"))
                                    try { startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
                                        Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        } else Toast.makeText(context, "Playback Error", Toast.LENGTH_SHORT).show()
                    }
                    else -> { /* Handle other states if needed for UI */ }
                }
            }
            loadVideo(url)
            play()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favorites.collect { favorites ->
                isFavorite = favorites.any { it.url == url }
                updateFavoriteIcon()
            }
        }

        btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteIcon()
            viewModel.toggleFavorite(VideoEntity(url, tvTitle.text.toString(), if (resolved is VideoSource.YouTube) "YouTube" else "Native", if (resolved is VideoSource.YouTube) "https://i.ytimg.com/vi/${resolved.videoId}/hqdefault.jpg" else null, 0L, System.currentTimeMillis(), isFavorite), isFavorite)
            Toast.makeText(context, if(isFavorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
        }

        saveToHistory(url, resolved)
    }

    private fun updateFavoriteIcon() {
        val iconRes = if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        btnFavorite.setImageResource(iconRes)
    }

    private fun saveToHistory(url: String, source: VideoSource) {
        val sourceStr = if (source is VideoSource.YouTube) "YouTube" else "Native"
        val history = HistoryEntity(
            url = url,
            title = tvTitle.text.toString(),
            source = sourceStr,
            thumbnailUrl = if (source is VideoSource.YouTube) "https://i.ytimg.com/vi/${source.videoId}/hqdefault.jpg" else null,
            lastPlaybackPosition = 0L,
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

        // Update history with latest position before stopping
        val url = args.videoUrl
        val pos = playerEngine?.getCurrentPosition() ?: 0L
        val resolved = VideoSourceResolver.resolve(url)
        val sourceStr = if (resolved is VideoSource.YouTube) "YouTube" else "Native"

        val history = HistoryEntity(
            url = url,
            title = tvTitle.text.toString(),
            source = sourceStr,
            thumbnailUrl = if (resolved is VideoSource.YouTube) "https://i.ytimg.com/vi/${resolved.videoId}/hqdefault.jpg" else null,
            lastPlaybackPosition = pos,
            timestamp = System.currentTimeMillis()
        )
        viewModel.saveToHistory(history)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerEngine?.release()
        playerEngine = null
    }
}
