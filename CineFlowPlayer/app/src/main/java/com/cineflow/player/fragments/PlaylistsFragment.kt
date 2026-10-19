package com.cineflow.player.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
        val fabAddPlaylist = view.findViewById<FloatingActionButton>(R.id.fab_add_playlist)

        adapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                Toast.makeText(context, "Playlist: ${playlist.name}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { playlist ->
                viewModel.deletePlaylist(playlist)
                Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
            }
        )

        rvPlaylists.layoutManager = LinearLayoutManager(context)
        rvPlaylists.adapter = adapter

        lifecycleScope.launch {
            viewModel.playlists.collect { playlists ->
                adapter.submitList(playlists)
            }
        }

        fabAddPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Playlist Name"
            setPadding(32, 32, 32, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Create New Playlist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createPlaylist(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
