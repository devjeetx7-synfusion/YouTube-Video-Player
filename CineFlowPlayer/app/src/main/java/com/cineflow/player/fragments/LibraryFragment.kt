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
import com.cineflow.player.models.HistoryEntity
import com.cineflow.player.database.AppDatabase
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.viewmodel.LibraryViewModel
import com.cineflow.player.viewmodel.LibraryViewModelFactory
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private val viewModel: LibraryViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        LibraryViewModelFactory(VideoRepository(database.videoDao(), database.playlistDao()))
    }

    private lateinit var adapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvLibrary = view.findViewById<RecyclerView>(R.id.rv_library)

        adapter = VideoAdapter { video ->
            val action = LibraryFragmentDirections.actionLibraryToVideoPlayer(video.url)
            findNavController().navigate(action)
        }

        rvLibrary.layoutManager = LinearLayoutManager(context)
        rvLibrary.adapter = adapter

        lifecycleScope.launch {
            viewModel.savedVideos.collect { saved ->
                adapter.submitList(saved.map { HistoryEntity(it.url, it.title, it.source, it.thumbnailUrl, 0L, it.timestamp) })
            }
        }
    }
}
