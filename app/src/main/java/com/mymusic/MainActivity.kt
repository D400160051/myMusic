package com.mymusic

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mymusic.adapter.SongAdapter
import com.mymusic.databinding.ActivityMainBinding
import com.mymusic.model.Song
import com.mymusic.player.BitPerfectAudio
import com.mymusic.player.MusicPlayerManager

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongAdapter
    private lateinit var player: MusicPlayerManager
    private lateinit var bitPerfect: BitPerfectAudio
    private var songs: List<Song> = emptyList()
    private lateinit var prefs: android.content.SharedPreferences

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgress = object : Runnable {
        override fun run() {
            if (player.isPlaying()) {
                binding.seekBar.progress = player.getCurrentPosition().toInt()
                binding.tvCurrent.text = formatTime(player.getCurrentPosition())
            }
            handler.postDelayed(this, 200)
        }
    }

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

        prefs = getSharedPreferences("mymusic", Context.MODE_PRIVATE)
        player = MusicPlayerManager.getInstance(this)
        bitPerfect = BitPerfectAudio(this)

        setupUI()
        setupPlayerListeners()

        adapter = SongAdapter(
            songs = emptyList(),
            onSongClick = { song: Song, index: Int ->
                player.setSongs(songs, index)
                adapter.updateSongs(songs)
                adapter.setPlayingId(song.id)
                updateSongInfo()
            }
        )
        binding.rvSongs.layoutManager = LinearLayoutManager(this)
        binding.rvSongs.adapter = adapter

        checkPermission()
        checkDac()
    }

    private fun setupUI() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnPlayPause.setOnClickListener {
            player.playPause()
            updatePlayPauseIcon()
        }
        binding.btnNext.setOnClickListener {
            player.next()
            adapter.setPlayingId(player.getCurrentSong()?.id ?: -1)
        }
        binding.btnPrev.setOnClickListener {
            player.previous()
            adapter.setPlayingId(player.getCurrentSong()?.id ?: -1)
        }
        binding.btnShuffle.setOnClickListener {
            player.toggleShuffle()
            updateShuffleIcon()
        }

        binding.btnEqualizer.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player.seekTo(progress.toLong())
                    binding.tvCurrent.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun setupPlayerListeners() {
        player.onSongChanged = { song ->
            runOnUiThread {
                updateSongInfo()
                binding.seekBar.max = player.getDuration().toInt()
                adapter.setPlayingId(song.id)
            }
        }
        player.onPlaybackStateChanged = { _ ->
            runOnUiThread { updatePlayPauseIcon() }
        }
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
        val selectedFolder = prefs.getString("music_folder", null)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = if (selectedFolder != null) {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        }
        val selectionArgs = if (selectedFolder != null) arrayOf("$selectedFolder/%") else null
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, sortOrder
        )

        songs = cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            val list = mutableListOf<Song>()
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val data = c.getString(dataCol)
                list.add(
                    Song(
                        id = id,
                        title = c.getString(titleCol) ?: "Unknown",
                        artist = c.getString(artistCol) ?: getString(R.string.unknown_artist),
                        album = c.getString(albumCol) ?: "",
                        albumId = c.getLong(albumIdCol),
                        durationMs = c.getLong(durCol),
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        ),
                        data = data
                    )
                )
            }
            list
        } ?: emptyList()

        adapter = SongAdapter(
            songs = songs,
            onSongClick = { song: Song, index: Int ->
                player.setSongs(songs, index)
                adapter.updateSongs(songs)
                adapter.setPlayingId(song.id)
                updateSongInfo()
            },
            currentSongId = player.getCurrentSong()?.id ?: -1
        )
        binding.rvSongs.adapter = adapter
        binding.tvCount.text = "${songs.size} lagu"
        binding.tvEmpty.visibility = if (songs.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        if (songs.isNotEmpty() && player.getCurrentSong() == null) {
            updateSongInfo()
        }
    }

    private fun updateSongInfo() {
        val song = player.getCurrentSong() ?: return
        binding.tvTitle.text = song.title
        binding.tvArtist.text = song.artist
        binding.seekBar.max = player.getDuration().toInt()
        binding.tvTotal.text = formatTime(player.getDuration())
        loadAlbumArt(song)
    }

    private fun loadAlbumArt(song: Song) {
        try {
            if (song.albumId > 0) {
                val uri = song.getAlbumArtUri()
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    binding.ivAlbumArt.setImageBitmap(bitmap)
                } else {
                    binding.ivAlbumArt.setImageResource(R.drawable.ic_audiotrack)
                }
                inputStream?.close()
            } else {
                binding.ivAlbumArt.setImageResource(R.drawable.ic_audiotrack)
            }
        } catch (_: Exception) {
            binding.ivAlbumArt.setImageResource(R.drawable.ic_audiotrack)
        }
    }

    private fun updatePlayPauseIcon() {
        val icon = if (player.isPlaying()) android.R.drawable.ic_media_pause
        else android.R.drawable.ic_media_play
        binding.btnPlayPause.setImageResource(icon)
    }

    private fun updateShuffleIcon() {
        val color = if (player.isShuffling())
            ContextCompat.getColor(this, R.color.primary)
        else
            ContextCompat.getColor(this, R.color.text_secondary)
        binding.btnShuffle.drawable.setTint(color)
        binding.tvShuffleStatus.text = if (player.isShuffling()) "Acak: ON" else "Acak: OFF"
    }

    private fun checkDac() {
        if (bitPerfect.isUsbDacConnected()) {
            binding.tvDacStatus.text = bitPerfect.getUsbDacInfo()
            binding.tvDacStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            binding.tvDacStatus.text = getString(R.string.dac_disconnected)
            binding.tvDacStatus.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary))
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateProgress)
        updatePlayPauseIcon()
        updateShuffleIcon()
        adapter.setPlayingId(player.getCurrentSong()?.id ?: -1)
        if (player.getCurrentSong() != null) updateSongInfo()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateProgress)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgress)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }
}
