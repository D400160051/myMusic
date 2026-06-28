package com.mymusic

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mymusic.databinding.ActivitySettingsBinding
import com.mymusic.player.BitPerfectAudio
import com.mymusic.player.MusicPlayerManager
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("mymusic", Context.MODE_PRIVATE)

        setupToolbar()
        setupEqualizer()
        setupBitPerfect()
        setupFolder()
        setupVersion()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupEqualizer() {
        binding.cardEqualizer.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }
    }

    private fun setupBitPerfect() {
        val bitPerfect = BitPerfectAudio(this)
        val player = MusicPlayerManager.getInstance(this)

        binding.switchBitPerfect.isChecked = prefs.getBoolean("bit_perfect", false)

        binding.switchBitPerfect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && bitPerfect.isUsbDacConnected()) {
                player.setupBitPerfectMode()
                prefs.edit().putBoolean("bit_perfect", true).apply()
                Toast.makeText(this, "Bit-Perfect: ON", Toast.LENGTH_SHORT).show()
            } else if (isChecked && !bitPerfect.isUsbDacConnected()) {
                binding.switchBitPerfect.isChecked = false
                Toast.makeText(this, "USB DAC tidak terdeteksi", Toast.LENGTH_SHORT).show()
            } else {
                player.setupNormalMode()
                prefs.edit().putBoolean("bit_perfect", false).apply()
                Toast.makeText(this, "Bit-Perfect: OFF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFolder() {
        val savedFolder = prefs.getString("music_folder", null)
        updateFolderLabel(savedFolder)

        binding.cardFolder.setOnClickListener {
            val folders = getMusicFolders()
            if (folders.isEmpty()) {
                Toast.makeText(this, "Tidak ada folder musik ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val items = arrayOf("Semua folder") + folders.toTypedArray()
            val checkedItem = if (savedFolder == null) 0
            else (items.indexOfFirst { savedFolder.startsWith(it) }).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Pilih Folder Musik")
                .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                    val selected = if (which == 0) null else folders[which - 1]
                    prefs.edit().putString("music_folder", selected).apply()
                    updateFolderLabel(selected)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun getMusicFolders(): List<String> {
        val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
        val cursor = contentResolver.query(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )
        val folderSet = mutableSetOf<String>()
        cursor?.use { c ->
            val col = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) {
                val path = c.getString(col)
                val parent = File(path).parent
                if (parent != null && !parent.startsWith("/data/")) {
                    folderSet.add(parent)
                }
            }
        }
        return folderSet.sorted().take(50)
    }

    private fun updateFolderLabel(folder: String?) {
        binding.tvFolderPath.text = folder ?: "Semua folder"
    }

    private fun setupVersion() {
        try {
            val pkg = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "v${pkg.versionName}"
        } catch (_: Exception) {
            binding.tvVersion.text = "v1.0"
        }
    }
}
