package com.mymusic.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
class BitPerfectAudio(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun isUsbDacConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
    }

    fun getUsbDacInfo(): String? {
        val dac = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE } ?: return null

        val product = dac.productName ?: "USB DAC"
        val sampleRates = dac.sampleRates?.joinToString("/") ?: "?"
        val channelCounts = dac.channelCounts?.joinToString("/") ?: "?"
        return "$product ($sampleRates Hz, $channelCounts ch)"
    }

    fun getPreferredSampleRate(): Int {
        val dac = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
        return dac?.sampleRates?.firstOrNull { it > 48000 } ?: 48000
    }

    companion object {
        const val SAMPLE_RATE_44K = 44100
        const val SAMPLE_RATE_48K = 48000
        const val SAMPLE_RATE_96K = 96000
        const val SAMPLE_RATE_192K = 192000
    }
}
