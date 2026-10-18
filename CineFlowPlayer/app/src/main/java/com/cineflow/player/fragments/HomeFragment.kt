package com.cineflow.player.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cineflow.player.R
import com.cineflow.player.adapters.VideoAdapter
import com.cineflow.player.database.AppDatabase
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.viewmodel.HomeViewModel
import com.cineflow.player.viewmodel.HomeViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import com.cineflow.player.models.HistoryEntity

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        HomeViewModelFactory(VideoRepository(database.videoDao(), database.playlistDao()))
    }

    private lateinit var historyAdapter: VideoAdapter
    private lateinit var favoritesAdapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvHistory = view.findViewById<RecyclerView>(R.id.rv_history)
        val rvFavorites = view.findViewById<RecyclerView>(R.id.rv_favorites)
        val fabAddVideo = view.findViewById<FloatingActionButton>(R.id.fab_add_video)

        historyAdapter = VideoAdapter { video -> navigateToPlayer(video.url) }
        favoritesAdapter = VideoAdapter { video -> navigateToPlayer(video.url) }

        rvHistory.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvHistory.adapter = historyAdapter

        rvFavorites.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvFavorites.adapter = favoritesAdapter

        lifecycleScope.launch {
            viewModel.history.collect { historyList ->
                historyAdapter.submitList(historyList)
            }
        }

        lifecycleScope.launch {
            viewModel.favorites.collect { favoritesList ->
                val historyFromFavs = favoritesList.map {
                    HistoryEntity(it.url, it.title, it.source, it.thumbnailUrl, 0L, it.timestamp)
                }
                favoritesAdapter.submitList(historyFromFavs)
            }
        }

        fabAddVideo.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_addVideo)
        }
    }

    private fun navigateToPlayer(url: String) {
        val action = HomeFragmentDirections.actionHomeToVideoPlayer(url)
        findNavController().navigate(action)
    }
}
