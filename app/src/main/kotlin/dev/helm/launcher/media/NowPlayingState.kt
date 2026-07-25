package dev.helm.launcher.media

import android.graphics.Bitmap

data class NowPlayingState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artwork: Bitmap? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val metadataLyrics: String? = null,
)
