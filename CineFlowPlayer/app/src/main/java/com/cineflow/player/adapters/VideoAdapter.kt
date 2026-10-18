package com.cineflow.player.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cineflow.player.R
import com.cineflow.player.models.HistoryEntity

class VideoAdapter(private val onVideoClick: (HistoryEntity) -> Unit) :
    ListAdapter<HistoryEntity, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_title)
        private val source: TextView = itemView.findViewById(R.id.tv_source)
        private val thumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)

        fun bind(video: HistoryEntity) {
            title.text = video.title
            source.text = video.source

            val thumbnailUrl = video.thumbnailUrl ?: Regex("(?:youtu\\.be/|[?&]v=|/shorts/|/live/|/embed/)([A-Za-z0-9_-]{11})")
                .find(video.url)?.groupValues?.getOrNull(1)?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
            Glide.with(itemView.context).load(thumbnailUrl).into(thumbnail)

            itemView.setOnClickListener {
                onVideoClick(video)
            }
        }
    }
}

class VideoDiffCallback : DiffUtil.ItemCallback<HistoryEntity>() {
    override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
        return oldItem == newItem
    }
}
