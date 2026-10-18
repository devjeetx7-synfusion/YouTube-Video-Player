package com.cineflow.player.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.cineflow.player.R
import com.cineflow.player.database.AppDatabase
import com.cineflow.player.models.VideoEntity
import com.cineflow.player.repository.VideoRepository
import com.cineflow.player.resolver.VideoSource
import com.cineflow.player.resolver.VideoSourceResolver
import com.cineflow.player.viewmodel.AddVideoViewModel
import com.cineflow.player.viewmodel.AddVideoViewModelFactory
import com.google.android.material.textfield.TextInputEditText

class AddVideoFragment : Fragment() {

    private val viewModel: AddVideoViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        AddVideoViewModelFactory(VideoRepository(database.videoDao(), database.playlistDao()))
    }

    private lateinit var etUrl: TextInputEditText
    private lateinit var btnPlayNow: Button
    private lateinit var btnSave: Button
    private lateinit var tvError: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etUrl = view.findViewById(R.id.et_url)
        btnPlayNow = view.findViewById(R.id.btn_play_now)
        btnSave = view.findViewById(R.id.btn_save)
        tvError = view.findViewById(R.id.tv_error)

        btnPlayNow.isEnabled = false
        btnSave.isEnabled = false

        etUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateUrl(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPlayNow.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (validateUrl(url)) {
                navigateToPlayer(url)
            }
        }

        btnSave.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (validateUrl(url)) {
                saveVideo(url)
            }
        }
    }

    private fun validateUrl(url: String): Boolean {
        if (url.isBlank()) {
            tvError.visibility = View.GONE
            btnPlayNow.isEnabled = false
            btnSave.isEnabled = false
            return false
        }

        val resolved = VideoSourceResolver.resolve(url)
        if (resolved is VideoSource.Invalid) {
            tvError.text = "Invalid or unsupported video URL"
            tvError.visibility = View.VISIBLE
            btnPlayNow.isEnabled = false
            btnSave.isEnabled = false
            return false
        }

        tvError.visibility = View.GONE
        btnPlayNow.isEnabled = true
        btnSave.isEnabled = true
        return true
    }

    private fun saveVideo(url: String) {
        val resolved = VideoSourceResolver.resolve(url)
        val source = if (resolved is VideoSource.YouTube) "YouTube" else "Native"
        val title = if (resolved is VideoSource.YouTube) "YouTube Video (${resolved.videoId})" else "Direct Video"

        val video = VideoEntity(
            url = url,
            title = title,
            source = source,
            thumbnailUrl = if (resolved is VideoSource.YouTube) "https://i.ytimg.com/vi/${resolved.videoId}/hqdefault.jpg" else null,
            duration = 0L,
            timestamp = System.currentTimeMillis(),
            isFavorite = false
        )
        viewModel.saveVideo(video)

        etUrl.text?.clear()
        tvError.text = "Video saved to library!"
        tvError.setTextColor(resources.getColor(R.color.accent, null))
        tvError.visibility = View.VISIBLE
    }

    private fun navigateToPlayer(url: String) {
        val action = AddVideoFragmentDirections.actionAddVideoToVideoPlayer(url)
        findNavController().navigate(action)
    }
}
