package com.cineflow.player.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cineflow.player.R
import com.cineflow.player.adapters.PlaylistAdapter
import com.cineflow.player.database.AppDatabase
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.viewmodel.LibraryViewModel
import com.cineflow.player.viewmodel.LibraryViewModelFactory
import kotlinx.coroutines.launch

class PlaylistsFragment : Fragment() {

    private val viewModel: LibraryViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LibraryViewModelFactory(VideoRepository(database.videoDao(), database.playlistDao()))
    }

    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playlists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvPlaylists = view.findViewById<RecyclerView>(R.id.rv_playlists)

        adapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                // Navigate or show videos in playlist
            },
            onDeleteClick = { playlist ->
                // Delete logic
            }
        )

        rvPlaylists.layoutManager = LinearLayoutManager(context)
        rvPlaylists.adapter = adapter

        lifecycleScope.launch {
            viewModel.playlists.collect { playlists ->
                adapter.submitList(playlists)
            }
        }
    }
}
