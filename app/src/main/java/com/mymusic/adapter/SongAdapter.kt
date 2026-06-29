package com.mymusic.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mymusic.databinding.ItemSongBinding
import com.mymusic.model.Song

class SongAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Song, Int) -> Unit,
    private val currentSongId: Long = -1
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var playingId: Long = currentSongId

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    fun setPlayingId(id: Long) {
        playingId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position)
    }

    override fun getItemCount(): Int = songs.size

    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, position: Int) {
            binding.tvTitle.text = song.title
            binding.tvArtist.text = song.artist
            binding.tvDuration.text = formatDuration(song.durationMs)
            binding.root.setOnClickListener { onSongClick(song, position) }

            val isPlaying = song.id == playingId
            binding.tvTitle.setTextColor(
                binding.root.context.getColor(
                    if (isPlaying) com.mymusic.R.color.primary
                    else com.mymusic.R.color.on_surface
                )
            )
        }

        private fun formatDuration(ms: Long): String {
            val totalSec = ms / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%02d:%02d".format(min, sec)
        }
    }
}
