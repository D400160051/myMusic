package com.mymusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mymusic.adapter.SongAdapter
import com.mymusic.databinding.ActivityMainBinding
import com.mymusic.model.Song
import com.mymusic.player.MusicPlayerManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongAdapter
    private var songs: List<Song> = emptyList()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) loadSongs()
        else Toast.makeText(this, "Izin diperlukan", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SongAdapter(emptyList()) { song, index ->
            MusicPlayerManager.getInstance(this).setSongs(songs, index)
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        binding.rvSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        binding.fabFolder.setOnClickListener {
            MusicPlayerManager.getInstance(this).setSongs(songs, 0)
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                loadSongs()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            loadSongs()
        }
    }

    private fun loadSongs() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, sortOrder
        )

        songs = cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            val list = mutableListOf<Song>()
            while (c.moveToNext()) {
                val data = c.getString(dataCol)
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                list.add(
                    Song(
                        id = c.getLong(idCol),
                        title = c.getString(titleCol) ?: "Unknown",
                        artist = c.getString(artistCol) ?: getString(R.string.unknown_artist),
                        album = c.getString(albumCol) ?: "",
                        durationMs = c.getLong(durCol),
                        uri = uri,
                        data = data
                    )
                )
            }
            list
        } ?: emptyList()

        adapter.updateSongs(songs)
        binding.tvCount.text = "${songs.size} lagu"
    }
}
