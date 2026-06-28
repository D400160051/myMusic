package com.mymusic.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val data: String
) {
    fun getAlbumArtUri(): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"), albumId
        )
    }
}
