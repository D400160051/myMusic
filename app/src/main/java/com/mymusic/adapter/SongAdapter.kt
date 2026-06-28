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

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
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
            binding.root.setOnClickListener { onSongClick(song, position) }
        }
    }
}
