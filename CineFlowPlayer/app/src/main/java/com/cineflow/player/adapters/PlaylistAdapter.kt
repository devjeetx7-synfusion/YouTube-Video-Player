package com.cineflow.player.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cineflow.player.R
import com.cineflow.player.models.PlaylistEntity

class PlaylistAdapter(
    private val onPlaylistClick: (PlaylistEntity) -> Unit,
    private val onDeleteClick: (PlaylistEntity) -> Unit
) : ListAdapter<PlaylistEntity, PlaylistAdapter.PlaylistViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tv_playlist_name)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(playlist: PlaylistEntity) {
            name.text = playlist.name

            itemView.setOnClickListener {
                onPlaylistClick(playlist)
            }

            deleteBtn.setOnClickListener {
                onDeleteClick(playlist)
            }
        }
    }
}

class PlaylistDiffCallback : DiffUtil.ItemCallback<PlaylistEntity>() {
    override fun areItemsTheSame(oldItem: PlaylistEntity, newItem: PlaylistEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PlaylistEntity, newItem: PlaylistEntity): Boolean {
        return oldItem == newItem
    }
}
