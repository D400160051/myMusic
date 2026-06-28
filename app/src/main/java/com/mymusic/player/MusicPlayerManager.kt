package com.mymusic.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mymusic.model.Song

@RequiresApi(Build.VERSION_CODES.O)
class MusicPlayerManager private constructor(private val context: Context) {

    private val player: ExoPlayer
    private var songList: List<Song> = emptyList()
    private var currentIndex = -1
    private var equalizer: Equalizer? = null
    private var audioSessionId = 0

    var onSongChanged: ((Song) -> Unit)? = null
    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
        .build()

    init {
        player = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NONE)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> onPlaybackStateChanged?.invoke(true)
                    Player.STATE_ENDED -> next()
                    Player.STATE_BUFFERING -> onPlaybackStateChanged?.invoke(false)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onError?.invoke(error.localizedMessage ?: "Playback error")
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (index in songList.indices && index != currentIndex) {
                    currentIndex = index
                    onSongChanged?.invoke(songList[index])
                    setupEqualizer()
                }
            }
        })

        audioSessionId = player.audioSessionId
    }

    fun setSongs(songs: List<Song>, startIndex: Int = 0) {
        songList = songs
        currentIndex = startIndex
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
    }

    fun playPause() {
        if (player.playWhenReady) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() {
        val nextIndex = if (player.shuffleModeEnabled) {
            player.nextMediaItemIndex
        } else {
            (currentIndex + 1).coerceAtMost(songList.size - 1)
        }
        if (nextIndex in songList.indices) {
            player.seekTo(nextIndex, 0L)
            player.play()
        }
    }

    fun previous() {
        val prevIndex = if (player.currentPosition > 3000) currentIndex
        else (currentIndex - 1).coerceAtLeast(0)
        player.seekTo(prevIndex, 0L)
        player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun isShuffling(): Boolean = player.shuffleModeEnabled

    fun isPlaying(): Boolean = player.isPlaying

    fun getCurrentPosition(): Long = player.currentPosition

    fun getDuration(): Long {
        val d = player.duration
        return if (d < 0) 0L else d
    }

    fun getCurrentSong(): Song? =
        if (currentIndex in songList.indices) songList[currentIndex] else null

    fun getSongList(): List<Song> = songList

    fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    fun setRepeatMode(mode: Int) {
        player.repeatMode = mode
    }

    fun getAudioSessionId(): Int = audioSessionId

    fun setupEqualizer() {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.setEnabled(true)
        } catch (_: Exception) { }
    }

    fun getEqualizer(): Equalizer? = equalizer

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizer?.setEnabled(enabled)
    }

    fun setupBitPerfectMode() {
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
            .build()
        player.setAudioAttributes(attrs, true)
    }

    fun setupNormalMode() {
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(attrs, true)
    }

    fun release() {
        equalizer?.release()
        player.release()
    }

    companion object {
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
