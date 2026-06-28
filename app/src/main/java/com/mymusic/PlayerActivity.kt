package com.mymusic

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mymusic.databinding.ActivityPlayerBinding
import com.mymusic.model.Song
import com.mymusic.player.BitPerfectAudio
import com.mymusic.player.MusicPlayerManager

@RequiresApi(Build.VERSION_CODES.O)
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: MusicPlayerManager
    private lateinit var bitPerfect: BitPerfectAudio
    private var isBitPerfect = false
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        player = MusicPlayerManager.getInstance(this)
        bitPerfect = BitPerfectAudio(this)

        setupUI()
        setupPlayerListeners()
        updateSongInfo()
        checkDac()
    }

    private fun setupUI() {
        binding.btnPlayPause.setOnClickListener {
            player.playPause()
            updatePlayPauseIcon()
        }
        binding.btnNext.setOnClickListener { player.next() }
        binding.btnPrev.setOnClickListener { player.previous() }
        binding.btnShuffle.setOnClickListener {
            player.toggleShuffle()
            updateShuffleIcon()
        }

        binding.btnEqualizer.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }

        binding.btnBitPerfect.setOnClickListener {
            toggleBitPerfect()
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
            }
        }
        player.onPlaybackStateChanged = { playing ->
            runOnUiThread { updatePlayPauseIcon() }
        }
    }

    private fun updateSongInfo() {
        val song = player.getCurrentSong() ?: return
        binding.tvTitle.text = song.title
        binding.tvArtist.text = song.artist
        binding.seekBar.max = player.getDuration().toInt()
        binding.tvTotal.text = formatTime(player.getDuration())
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
            binding.tvDacStatus.setTextColor(
                ContextCompat.getColor(this, R.color.primary)
            )
        } else {
            binding.tvDacStatus.text = getString(R.string.dac_disconnected)
            binding.tvDacStatus.setTextColor(
                ContextCompat.getColor(this, R.color.text_secondary)
            )
        }
    }

    private fun toggleBitPerfect() {
        isBitPerfect = !isBitPerfect
        if (isBitPerfect && bitPerfect.isUsbDacConnected()) {
            player.setupBitPerfectMode()
            binding.btnBitPerfect.setBackgroundColor(
                ContextCompat.getColor(this, R.color.primary)
            )
            Toast.makeText(this, "Bit-Perfect: ON", Toast.LENGTH_SHORT).show()
        } else if (isBitPerfect && !bitPerfect.isUsbDacConnected()) {
            isBitPerfect = false
            Toast.makeText(this, "USB DAC tidak terdeteksi", Toast.LENGTH_SHORT).show()
        } else {
            player.setupNormalMode()
            binding.btnBitPerfect.setBackgroundColor(
                ContextCompat.getColor(this, R.color.surface)
            )
            Toast.makeText(this, "Bit-Perfect: OFF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateProgress)
        updatePlayPauseIcon()
        updateShuffleIcon()
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
