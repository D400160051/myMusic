package com.mymusic

import android.media.audiofx.Equalizer
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.mymusic.databinding.ActivityEqualizerBinding
import com.mymusic.player.MusicPlayerManager

@RequiresApi(Build.VERSION_CODES.O)
class EqualizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEqualizerBinding
    private var equalizer: Equalizer? = null
    private val bandSeekBars = mutableListOf<SeekBar>()
    private val bandLabels = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val player = MusicPlayerManager.getInstance(this)
        equalizer = player.getEqualizer()

        if (equalizer == null) {
            player.setupEqualizer()
            equalizer = player.getEqualizer()
        }

        equalizer?.let { eq ->
            val bands = eq.numberOfBands
            val bandLayout = binding.llBands
            bandLayout.removeAllViews()

            for (i in 0 until bands) {
                val freq = eq.getCenterFreq(i.toShort()) / 1000f
                val min = eq.getBandLevelRange()[0] / 100
                val max = eq.getBandLevelRange()[1] / 100
                val current = eq.getBandLevel(i.toShort()) / 100

                val seekBar = SeekBar(this).apply {
                    max = max - min
                    progress = current - min
                    tag = i
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                            val level = (p + min) * 100
                            eq.setBandLevel(i.toShort(), level.toShort())
                            bandLabels[i].text = "${p + min} dB"
                        }
                        override fun onStartTrackingTouch(sb: SeekBar?) {}
                        override fun onStopTrackingTouch(sb: SeekBar?) {}
                    })
                }

                val label = TextView(this).apply {
                    text = if (freq >= 1000) "%.1f kHz".format(freq / 1000f)
                    else "%.0f Hz".format(freq)
                    textSize = 12f
                }

                val valueLabel = TextView(this).apply {
                    text = "$current dB"
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                }

                bandLayout.addView(label)
                bandLayout.addView(seekBar)
                bandLayout.addView(valueLabel)

                bandSeekBars.add(seekBar)
                bandLabels.add(valueLabel)
            }

            eq.setEnabled(true)
            binding.switchEq.isChecked = true
        }

        binding.switchEq.setOnCheckedChangeListener { _, isChecked ->
            val mplayer = MusicPlayerManager.getInstance(this)
            mplayer.setEqualizerEnabled(isChecked)
        }

        binding.btnReset.setOnClickListener {
            resetEqualizer()
        }
    }

    private fun resetEqualizer() {
        equalizer?.let { eq ->
            val bands = eq.numberOfBands
            for (i in 0 until bands) {
                eq.setBandLevel(i.toShort(), 0)
                val min = eq.getBandLevelRange()[0] / 100
                bandSeekBars[i].progress = -min
                bandLabels[i].text = "0 dB"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
